package com.spyshield.app.monitor

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.spyshield.app.data.AccessEvent
import com.spyshield.app.data.AppScanner
import com.spyshield.app.data.AutoBlockPrefs
import com.spyshield.app.data.LiveUse
import com.spyshield.app.data.Sensor
import com.spyshield.app.data.Store
import com.spyshield.app.data.TrustedApps
import com.spyshield.app.data.appLabel

/**
 * Foreground service that listens for the camera becoming unavailable and for audio
 * recordings starting, then figures out who is responsible and raises alerts.
 */
class SensorMonitorService : Service() {

    private class Session(val eventId: Long, val start: Long, val dropIfShort: Boolean) {
        var alert: Runnable? = null
        var alarm: Runnable? = null
        var ignored = false
    }

    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager

    private val busyCameras = mutableSetOf<String>()
    private var torchOn = false
    @Volatile private var destroyed = false
    private var camSession: Session? = null
    private var micSession: Session? = null
    private val recorders = DetectionRules.RecorderTracker()
    private var installWatcher: InstallWatcher? = null
    private var lastOngoingText: String? = null
    // Set when the widget skipped an update while the screen was off; only touched on the handler thread.
    private var widgetStale = false

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Also picks up the midnight reset of "alerts today".
            widgetStale = false
            StatusWidgetProvider.refresh(this@SensorMonitorService)
        }
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            busyCameras.remove(cameraId)
            if (busyCameras.isEmpty()) endSession(Sensor.CAMERA)
        }

        override fun onCameraUnavailable(cameraId: String) {
            busyCameras.add(cameraId)
            if (camSession == null) startSession(Sensor.CAMERA)
        }
    }

    // The flashlight makes the camera "unavailable" on many phones; don't report it.
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchOn = enabled
            val s = camSession ?: return
            if (enabled && !s.ignored && System.currentTimeMillis() - s.start < 2_000) {
                s.ignored = true
                s.alert?.let { handler.removeCallbacks(it) }
                Store.removeEvent(s.eventId)
                Store.setLive(Sensor.CAMERA, null)
                refreshOngoing()
            }
        }
    }

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) = onMicConfigs(configs)
    }

    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread("spyshield-monitor").apply { start() }
        handler = Handler(thread.looper)
        cameraManager = getSystemService(CameraManager::class.java)
        audioManager = getSystemService(AudioManager::class.java)

        val notification = Notifications.monitor(this, IDLE_TEXT)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifications.ID_MONITOR, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_MONITOR, notification)
        }
        lastOngoingText = IDLE_TEXT

        cameraManager.registerAvailabilityCallback(cameraCallback, handler)
        cameraManager.registerTorchCallback(torchCallback, handler)
        audioManager.registerAudioRecordingCallback(audioCallback, handler)
        installWatcher = InstallWatcher(this).also { it.register(handler) }
        // Screen-on is a protected system broadcast, so only the system can send it.
        ContextCompat.registerReceiver(
            this, screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), null, handler, ContextCompat.RECEIVER_EXPORTED,
        )
        handler.post { onMicConfigs(audioManager.activeRecordingConfigurations) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        cameraManager.unregisterTorchCallback(torchCallback)
        audioManager.unregisterAudioRecordingCallback(audioCallback)
        installWatcher?.unregister()
        installWatcher = null
        runCatching { unregisterReceiver(screenOnReceiver) }
        handler.post {
            destroyed = true
            endSession(Sensor.CAMERA)
            endSession(Sensor.MIC)
            if (widgetStale) StatusWidgetProvider.refresh(this)
            Store.flush()
            thread.quitSafely()
            // Inside the post so it lands after any update() this thread already queued.
            PrivacyDotOverlay.hide(this)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onMicConfigs(configs: List<AudioRecordingConfiguration>) {
        // Hotword, playback capture and FM tuner configs aren't the user's microphone.
        @Suppress("NAME_SHADOWING")
        val configs = DetectionRules.micConfigs(configs)
        val active = configs.isNotEmpty()
        val joined = recorders.update(configs)
        val current = micSession
        if (active && current != null && joined == true && Store.event(current.eventId)?.suspicious == false) {
            // Android silenced a recorder that joined a mic in legit use (call, recorder app); report it separately.
            endSession(Sensor.MIC)
            startSession(Sensor.MIC, true)
            return
        }
        if (active && micSession == null) {
            val silenced = Build.VERSION.SDK_INT >= 29 && configs.all { it.isClientSilenced }
            startSession(Sensor.MIC, silenced)
        } else if (!active && micSession != null) {
            endSession(Sensor.MIC)
        }
    }

    private fun startSession(sensor: Sensor, silenced: Boolean = false) {
        val now = System.currentTimeMillis()
        val id = now * 10 + sensor.ordinal

        if (sensor == Sensor.CAMERA && torchOn) {
            camSession = Session(id, now, false).apply { ignored = true }
            return
        }

        val a = Attributor.attribute(this, sensor, silenced)
        // A blocked app is blamed only when it was on screen or confidently identified, never when it's
        // just the top guess (say, during face unlock).
        val blockedPkg = a.packageName?.takeIf { Store.isBlocked(it) && (!a.suspicious || a.confident) }
        val event = AccessEvent(
            id = id,
            sensor = sensor,
            packageName = a.packageName,
            appLabel = a.label,
            start = now,
            end = null,
            suspicious = a.suspicious || blockedPkg != null,
            reason = if (blockedPkg != null) "Blocked app! ${a.reason}" else a.reason,
            suspects = a.suspects,
            blocked = blockedPkg != null,
            confident = a.confident,
        )
        Store.addEvent(event)

        val session = Session(id, now, a.lockedCamera)
        val allTrusted = event.packageName != null &&
            (listOf(event.packageName) + event.suspects).all { TrustedApps.isTrusted(it) }
        if (event.suspicious && (event.blocked || !allTrusted)) {
            val r = Runnable {
                DetectionRules.confirmAlert(this, sensor, id, recheck = !a.lockedCamera)?.let {
                    if (shouldAutoBlock(it)) autoBlock(session, it) else Notifications.alert(this, it)
                }
                refreshOngoing()
            }
            session.alert = r
            val delay = when {
                a.lockedCamera -> 5_000L
                event.blocked -> 0L
                else -> 1_500L
            }
            handler.postDelayed(r, delay)
        }
        if (sensor == Sensor.CAMERA) camSession = session else micSession = session
        Store.setLive(sensor, LiveUse(id, event.appLabel, event.packageName, event.suspicious, now))
        refreshOngoing()
    }

    private fun endSession(sensor: Sensor) {
        val s = (if (sensor == Sensor.CAMERA) camSession else micSession) ?: return
        if (sensor == Sensor.CAMERA) camSession = null else micSession = null
        if (s.ignored) return

        s.alert?.let { handler.removeCallbacks(it) }
        s.alarm?.let { handler.removeCallbacks(it) }
        val now = System.currentTimeMillis()
        if (s.dropIfShort && now - s.start < 5_000) {
            Store.removeEvent(s.eventId)
        } else {
            Store.updateEvent(s.eventId) { it.copy(end = now) }
        }
        Store.setLive(sensor, null)
        refreshOngoing()
    }

    /** Only a confident pick of an untrusted, user-installed app is auto-blocked; other apps are never touched. */
    private fun shouldAutoBlock(e: AccessEvent): Boolean {
        val pkg = e.packageName ?: return false
        if (!AutoBlockPrefs.isEnabled(this) || !e.suspicious || !e.confident || e.blocked) return false
        if (pkg == packageName || TrustedApps.isTrusted(pkg)) return false
        if (pkg in DetectionRules.defaultRoleApps(this) || DetectionRules.inCallMode(this)) return false
        return runCatching { !AppScanner.isSystem(packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(false)
    }

    /** Blocklists the app and re-sounds the alarm every 30 s (5 repeats) while the session lasts. */
    private fun autoBlock(session: Session, e: AccessEvent) {
        val pkg = e.packageName ?: return
        Store.setBlocked(pkg, true)
        Store.updateEvent(e.id) { it.copy(blocked = true, autoBlocked = true) }
        var repeats = 0
        val alarm = object : Runnable {
            override fun run() {
                // "Not a spy" unblocks the app, which ends the alarm.
                if (!Store.isBlocked(pkg)) return
                Store.event(e.id)?.let { Notifications.spyAlarm(this@SensorMonitorService, it) }
                if (repeats++ < 5) handler.postDelayed(this, 30_000L)
            }
        }
        session.alarm = alarm
        alarm.run()
    }

    private fun refreshOngoing() {
        // Widget first so it also catches the final "everything idle" update on shutdown.
        // Nobody sees it with the screen off, so that update waits for screen-on.
        if (destroyed || getSystemService(PowerManager::class.java).isInteractive) {
            widgetStale = false
            StatusWidgetProvider.refresh(this)
        } else {
            widgetStale = true
        }
        if (destroyed) return
        PrivacyDotOverlay.update(this, Store.live.value)
        val live = Store.live.value
        fun line(sensor: Sensor) = live.of(sensor)?.let { "${sensor.title}: ${it.label}" } ?: "${sensor.title}: idle"
        val text = if (live.camera == null && live.mic == null) {
            IDLE_TEXT
        } else {
            line(Sensor.CAMERA) + "  •  " + line(Sensor.MIC)
        }
        // Re-posting the same text would only make SystemUI redraw it.
        if (text == lastOngoingText) return
        lastOngoingText = text
        getSystemService(NotificationManager::class.java)
            .notify(Notifications.ID_MONITOR, Notifications.monitor(this, text))
    }

    companion object {
        private const val IDLE_TEXT = "Watching your camera and microphone"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, SensorMonitorService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorMonitorService::class.java))
        }
    }
}
