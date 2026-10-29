package com.spyshield.app.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.spyshield.app.monitor.ForegroundAppDetector

private const val DAY = 86_400_000L

private val TRUSTED_STORES = setOf(
    "com.android.vending",
    "com.google.android.feedback",
    "com.sec.android.app.samsungapps",
    "com.huawei.appmarket",
    "com.xiaomi.market",
    "com.xiaomi.mipicks",
    "com.heytap.market",
    "com.oppo.market",
    "com.bbk.appstore",
    "com.amazon.venezia",
)


object AppScanner {

    fun installedPackages(context: Context): List<PackageInfo> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
    }

    /** Manifest facts that only change when a package is installed, updated or removed. */
    private class PkgManifest(val system: Boolean, val requested: Set<String>, val sharedUid: Boolean)

    // Guarded by this. Grants are never cached: they change without any broadcast.
    private var manifests: Map<String, PkgManifest>? = null
    private var homes: Set<String>? = null
    private val serviceTypes = HashMap<String, Pair<Int, Int>>()
    private val labels = HashMap<String, String>()
    private var labelLocale = ""
    private var changeSeq = -1

    /** Drops cached package data whenever the system reports an install, update or removal. */
    @Synchronized
    private fun sync(pm: PackageManager) {
        val changed = try {
            pm.getChangedPackages(maxOf(changeSeq, 0))
        } catch (e: RuntimeException) {
            // Can't tell what changed, so fall back to today's uncached lookups.
            changeSeq = -1
            invalidateAll()
            return
        }
        if (changed == null) {
            if (changeSeq < 0) changeSeq = 0
            return
        }
        changeSeq = changed.sequenceNumber
        invalidateAll()
    }

    /** Forgets all cached package data. [com.spyshield.app.monitor.InstallWatcher] calls this too. */
    @Synchronized
    fun invalidateAll() {
        manifests = null
        homes = null
        serviceTypes.clear()
        labels.clear()
    }

    /** Installed packages in system order. Any change reloads the whole list, so the order matches a fresh query. */
    @Synchronized
    @Suppress("DEPRECATION") // sharedUserId
    private fun manifests(context: Context): Map<String, PkgManifest> {
        sync(context.packageManager)
        manifests?.let { return it }
        val loaded = LinkedHashMap<String, PkgManifest>()
        installedPackages(context).forEach { pi ->
            loaded[pi.packageName] = PkgManifest(
                system = pi.applicationInfo?.let { isSystem(it) } ?: true,
                requested = pi.requestedPermissions?.toSet().orEmpty(),
                sharedUid = pi.sharedUserId != null,
            )
        }
        manifests = loaded
        return loaded
    }

    /** Packages (other than us) that currently hold [permission]. */
    fun packagesWithGranted(context: Context, permission: String): List<String> {
        val pm = context.packageManager
        // Only apps that ask for it, or share a UID that might, can hold it. The grant itself is read live.
        return manifests(context)
            .filter { (pkg, m) -> pkg != context.packageName && (permission in m.requested || m.sharedUid) }
            .keys
            .filter { pm.checkPermission(permission, it) == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Whether [pkg] is a system app; one that can't be looked up counts as system. Reads the list
     * [packagesWithGranted] just synced, so ranking suspects makes no extra binder calls.
     */
    @Synchronized
    fun isSystemPackage(context: Context, pkg: String): Boolean =
        (manifests ?: manifests(context))[pkg]?.system
            ?: runCatching { isSystem(context.packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(true)

    /** Packages that can act as the home screen. */
    @Synchronized
    fun homePackages(context: Context): Set<String> {
        sync(context.packageManager)
        homes?.let { return it }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
            .also { homes = it }
    }

    /** [pkg]'s targetSdk and the OR of its declared foreground service types, or null if it can't be read. */
    @RequiresApi(29)
    @Synchronized
    fun foregroundServiceTypes(context: Context, pkg: String): Pair<Int, Int>? {
        sync(context.packageManager)
        serviceTypes[pkg]?.let { return it }
        return runCatching {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(pkg, PackageManager.GET_SERVICES)
            info.applicationInfo!!.targetSdkVersion to
                info.services.orEmpty().fold(0) { acc, s -> acc or s.foregroundServiceType }
        }.getOrNull()?.also { serviceTypes[pkg] = it }
    }

    /** Display name of [pkg]. Only labels that resolved are cached, so a not-yet-visible app still shows its package name. */
    @Synchronized
    fun label(context: Context, pkg: String): String {
        sync(context.packageManager)
        val locale = context.resources.configuration.locales.toLanguageTags()
        if (locale != labelLocale) {
            labels.clear()
            labelLocale = locale
        }
        labels[pkg]?.let { return it }
        val pm = context.packageManager
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
            .onSuccess {
                if (labels.size >= 64) labels.clear()
                labels[pkg] = it
            }
            .getOrDefault(pkg)
    }

    fun isSystem(ai: ApplicationInfo) = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0

    fun installerOf(context: Context, pkg: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            context.packageManager.getInstallSourceInfo(pkg).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(pkg)
        }
    }.getOrNull()

    /** Label and installer only change with an update (or, for the label, the language), so scans reuse them. */
    private class ScanMemo(val updated: Long, val locale: String, val label: String, val installer: String?)

    // Only touched by scan(), under scanLock.
    private val scanLock = Any()
    private val scanMemo = HashMap<String, ScanMemo>()
    private var usedCache: Map<String, Long>? = null
    private var usedFullAt = 0L
    private var usedAt = 0L

    /**
     * Last-used times over 60 days. Later scans only ask for the recent past and keep the newest time
     * per app, instead of making the system re-read 60 days of usage history on every resume.
     */
    private fun lastUsed60(context: Context, now: Long, canSeeUsage: Boolean): Map<String, Long> {
        val cached = usedCache
        if (!canSeeUsage) {
            usedCache = null
            return emptyMap()
        }
        val merged = if (cached == null || now - usedFullAt > DAY || now < usedAt) {
            usedFullAt = now
            ForegroundAppDetector.lastUsedTimes(context, 60 * DAY)
        } else {
            val recent = ForegroundAppDetector.lastUsedTimes(context, now - usedAt + DAY)
            HashMap(cached).apply { recent.forEach { (pkg, t) -> this[pkg] = maxOf(this[pkg] ?: 0L, t) } }
        }
        usedCache = merged
        usedAt = now
        return merged
    }

    /** Every app that asks for camera or mic access, scored by how risky that access looks. */
    fun scan(context: Context, events: List<AccessEvent>): List<AppPrivacyInfo> = synchronized(scanLock) {
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val canSeeUsage = ForegroundAppDetector.hasUsageAccess(context)
        val lastUsed = lastUsed60(context, now, canSeeUsage)
        val eventsByPkg = events.groupBy { it.packageName }
        val locale = context.resources.configuration.locales.toLanguageTags()
        val seen = HashSet<String>()

        val apps = installedPackages(context).mapNotNull { pi ->
            if (pi.packageName == context.packageName) return@mapNotNull null
            val ai = pi.applicationInfo ?: return@mapNotNull null
            val requested = pi.requestedPermissions ?: return@mapNotNull null
            val flags = pi.requestedPermissionsFlags ?: IntArray(requested.size)
            fun granted(p: String): Boolean {
                val i = requested.indexOf(p)
                return i >= 0 && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            }

            val camReq = Manifest.permission.CAMERA in requested
            val micReq = Manifest.permission.RECORD_AUDIO in requested
            if (!camReq && !micReq) return@mapNotNull null
            val camGranted = granted(Manifest.permission.CAMERA)
            val micGranted = granted(Manifest.permission.RECORD_AUDIO)
            val system = isSystem(ai)
            seen += pi.packageName
            val memo = scanMemo[pi.packageName]?.takeIf { it.updated == pi.lastUpdateTime && it.locale == locale }
                ?: ScanMemo(
                    pi.lastUpdateTime, locale, pm.getApplicationLabel(ai).toString(),
                    // System apps never count as sideloaded, so their installer is never needed.
                    if (system) null else installerOf(context, pi.packageName),
                ).also { scanMemo[pi.packageName] = it }
            val installer = memo.installer
            val sideloaded = !system && (installer == null || installer !in TRUSTED_STORES)
            val used = lastUsed[pi.packageName] ?: 0L
            val appEvents = eventsByPkg[pi.packageName].orEmpty()
            val suspiciousUses = appEvents.count { it.suspicious }

            var score = 0
            val reasons = mutableListOf<String>()
            if (camGranted) { score += 20; reasons += "Can use your camera" }
            if (micGranted) { score += 20; reasons += "Can record with your microphone" }
            if (camGranted && micGranted) { score += 10; reasons += "Has both camera and mic — can capture video with sound" }
            if (sideloaded) { score += 20; reasons += "Installed from outside a trusted app store" }
            if ("android.permission.FOREGROUND_SERVICE_MICROPHONE" in requested ||
                "android.permission.FOREGROUND_SERVICE_CAMERA" in requested
            ) { score += 10; reasons += "Can keep using the camera/mic while running in the background" }
            if (Manifest.permission.RECEIVE_BOOT_COMPLETED in requested) { score += 5; reasons += "Starts itself automatically when the phone boots" }
            if (Manifest.permission.SYSTEM_ALERT_WINDOW in requested) { score += 5; reasons += "Can draw over other apps" }
            if (canSeeUsage && (camGranted || micGranted) && now - used > 30 * DAY) {
                score += 10; reasons += "Not opened in 30+ days but still has access"
            }
            if (suspiciousUses > 0) {
                score += (suspiciousUses * 15).coerceAtMost(30)
                reasons += "Flagged $suspiciousUses time(s) for background camera/mic use"
            }
            val preGranted = system && (camGranted || micGranted)
            if (preGranted) reasons += "Pre-installed — camera/mic access may have been granted by the phone maker"
            if (system) score /= 2
            if (!camGranted && !micGranted) reasons.add(0, "Asked for access, but you haven't allowed it")

            AppPrivacyInfo(
                packageName = pi.packageName,
                label = memo.label,
                isSystem = system,
                cameraRequested = camReq,
                cameraGranted = camGranted,
                micRequested = micReq,
                micGranted = micGranted,
                sideloaded = sideloaded,
                lastUsed = used,
                riskScore = score.coerceIn(0, 100),
                reasons = reasons,
                uses = appEvents.size,
                suspiciousUses = suspiciousUses,
                preGranted = preGranted,
            )
        }
        // Forget uninstalled apps.
        scanMemo.keys.retainAll(seen)
        apps
    }
}

fun Context.appLabel(pkg: String): String = AppScanner.label(this, pkg)

fun appDetailsIntent(pkg: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun uninstallIntent(pkg: String): Intent =
    Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
