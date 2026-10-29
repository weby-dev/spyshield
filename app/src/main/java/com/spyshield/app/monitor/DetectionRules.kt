package com.spyshield.app.monitor

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.spyshield.app.data.AccessEvent
import com.spyshield.app.data.Sensor
import com.spyshield.app.data.Store
import com.spyshield.app.data.appLabel

/**
 * Extra checks that keep ordinary phone behaviour (calls, hotword, voice typing, internal
 * audio capture) from looking like spying, and keep repeat alerts quiet.
 */
object DetectionRules {

    // Hidden MediaRecorder.AudioSource values only the system can use.
    private const val ECHO_REFERENCE = 1997
    private const val RADIO_TUNER = 1998
    private const val HOTWORD = 1999
    private val NOT_MIC = setOf(MediaRecorder.AudioSource.REMOTE_SUBMIX, ECHO_REFERENCE, RADIO_TUNER, HOTWORD)

    private const val ALERT_COOLDOWN = 60_000L
    private val lastAlert = HashMap<String, Long>()

    /** Drops recordings that don't capture the microphone: always-on hotword, playback capture, radio. */
    fun micConfigs(configs: List<AudioRecordingConfiguration>): List<AudioRecordingConfiguration> =
        configs.filter { it.clientAudioSource !in NOT_MIC }

    /** Foreground app, looking further back when an app has stayed open for longer than the usual window. */
    fun foreground(locked: Boolean, snapshot: ForegroundAppDetector.Snapshot): String? =
        snapshot.foreground
            ?: if (!locked) snapshot.foregroundLong else null

    /** Phone and VoIP calls keep the mic (and for video calls, the camera) busy with the screen off. */
    fun duringCall(
        context: Context, sensor: Sensor, silenced: Boolean,
        snapshot: () -> ForegroundAppDetector.Snapshot = { ForegroundAppDetector.snapshot(context) },
    ): Attribution? {
        if (silenced) return null
        val am = context.getSystemService(AudioManager::class.java)
        val mode = am.mode
        // MODE_IN_CALL needs MODIFY_PHONE_STATE, so only the telephony stack can set it.
        val cellular = mode == AudioManager.MODE_IN_CALL ||
            (Build.VERSION.SDK_INT >= 30 && mode == AudioManager.MODE_CALL_SCREENING) ||
            (Build.VERSION.SDK_INT >= 33 && mode == AudioManager.MODE_CALL_REDIRECT)
        val voip = mode == AudioManager.MODE_IN_COMMUNICATION ||
            (Build.VERSION.SDK_INT >= 33 && mode == AudioManager.MODE_COMMUNICATION_REDIRECT)
        // Cellular call audio stays in the modem and VoIP apps record with VOICE_COMMUNICATION, so any
        // other recording during a call (a call recorder, stalkerware) isn't part of the call.
        val configs = micConfigs(am.activeRecordingConfigurations)
        val callAudio = configs.isNotEmpty() &&
            configs.all { it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION }
        val inCall = when (sensor) {
            Sensor.MIC -> (cellular || voip) && callAudio
            // A video call also has the mic open; communication mode alone isn't enough for the camera.
            Sensor.CAMERA -> voip && callAudio
        }
        if (!inCall) return null

        val pm = context.packageManager
        val screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        val fg = if (screenOn) snapshot().foreground else null
        val pkg = fg?.takeIf {
            it != context.packageName && it != "com.android.systemui" &&
                pm.checkPermission(sensor.permission, it) == PackageManager.PERMISSION_GRANTED
        } ?: if (cellular) {
            runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
        } else null
        return Attribution(pkg, pkg?.let { context.appLabel(it) } ?: "Call app", false, "Used during a call", emptyList(), false)
    }

    /** Voice typing records from the keyboard, which never shows up as the foreground app. */
    fun voiceTyping(context: Context, sensor: Sensor, fg: String?): Attribution? {
        if (sensor != Sensor.MIC) return null
        val am = context.getSystemService(AudioManager::class.java)
        // Every recording must be recognition, so a concurrent background recorder isn't hidden behind the keyboard.
        val recognizing = micConfigs(am.activeRecordingConfigurations)
            .let { c -> c.isNotEmpty() && c.all { it.clientAudioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION } }
        if (!recognizing) return null
        val ime = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.let { ComponentName.unflattenFromString(it)?.packageName }
        }.getOrNull() ?: return null
        if (context.packageManager.checkPermission(sensor.permission, ime) != PackageManager.PERMISSION_GRANTED) return null
        val where = fg?.let { " while using ${context.appLabel(it)}" }.orEmpty()
        return Attribution(ime, context.appLabel(ime), false, "Voice typing with your keyboard$where", emptyList(), false)
    }

    /**
     * Runs right before an alert fires. The usage event for an app that was just opened often lands
     * after the camera/mic turns on, so attribute again and stay quiet if it now looks legit.
     * Also skips repeat alerts for the same app and sensor within a minute.
     */
    fun confirmAlert(context: Context, sensor: Sensor, eventId: Long, recheck: Boolean = true): AccessEvent? {
        val e = Store.event(eventId) ?: return null
        if (e.end != null) return null
        // Unlocking after a lock-screen session started says nothing about who opened the camera.
        if (recheck && !e.blocked) {
            val am = context.getSystemService(AudioManager::class.java)
            // Any silenced client means something was blocked from recording, so never downgrade then.
            val silenced = sensor == Sensor.MIC && Build.VERSION.SDK_INT >= 29 &&
                micConfigs(am.activeRecordingConfigurations).any { it.isClientSilenced }
            val a = Attributor.attribute(context, sensor, silenced)
            if (!a.suspicious && !Store.isBlocked(a.packageName)) {
                Store.updateEvent(eventId) {
                    it.copy(packageName = a.packageName, appLabel = a.label, suspicious = false, reason = a.reason, suspects = emptyList())
                }
                Store.live.value.of(sensor)?.takeIf { it.eventId == eventId }?.let {
                    Store.setLive(sensor, it.copy(label = a.label, packageName = a.packageName, suspicious = false))
                }
                return null
            }
            // Auto-block only acts when both attributions name the same app with confidence.
            Store.updateEvent(eventId) { it.copy(confident = it.confident && a.confident && a.packageName == it.packageName) }
        }
        // Blocked state is part of the key so blocking an app never swallows its first blocked alarm.
        val key = "${sensor.name}:${e.packageName}:${e.blocked}"
        val now = System.currentTimeMillis()
        val last = lastAlert.put(key, now)
        if (last != null && now - last < ALERT_COOLDOWN) return null
        return Store.event(eventId)
    }

    /** Keyboard, assistant and dialer use the mic in normal use, so auto-block leaves them alone. */
    fun defaultRoleApps(context: Context): Set<String> {
        val cr = context.contentResolver
        fun component(key: String) = runCatching {
            Settings.Secure.getString(cr, key)?.let { ComponentName.unflattenFromString(it)?.packageName }
        }.getOrNull()
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
        return setOfNotNull(component(Settings.Secure.DEFAULT_INPUT_METHOD), component("assistant"), component("voice_interaction_service"), dialer)
    }

    /** Any call or VoIP audio mode, whoever is recording. */
    fun inCallMode(context: Context): Boolean {
        val mode = context.getSystemService(AudioManager::class.java).mode
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION ||
            (Build.VERSION.SDK_INT >= 30 && mode == AudioManager.MODE_CALL_SCREENING) ||
            (Build.VERSION.SDK_INT >= 33 && (mode == AudioManager.MODE_CALL_REDIRECT || mode == AudioManager.MODE_COMMUNICATION_REDIRECT))
    }

    /** Notices a new recording joining a mic that was already busy. */
    class RecorderTracker {
        private var known = emptySet<Int>()

        /** Null when nobody new joined, otherwise whether every newcomer was silenced by Android. */
        fun update(configs: List<AudioRecordingConfiguration>): Boolean? {
            val fresh = configs.filter { it.clientAudioSessionId !in known }
            val hadAny = known.isNotEmpty()
            known = configs.map { it.clientAudioSessionId }.toSet()
            if (!hadAny || fresh.isEmpty()) return null
            return Build.VERSION.SDK_INT >= 29 && fresh.all { it.isClientSilenced }
        }
    }
}
