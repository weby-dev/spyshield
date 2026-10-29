# SpyShield — Google Play listing & Play Console declarations

Copy-paste source for the store listing and the policy forms in Play Console.
Character limits are Play's; counts below were checked when this file was written.

---

## App title (max 30)

```
SpyShield: Camera & Mic Guard
```
(29 characters)

## Short description (max 80)

```
See which apps use your camera & mic. Get alerts and shut spying apps out fast.
```
(79 characters)

## Full description (max 4000)

```
Is an app watching or listening when you're not looking? SpyShield tells you.

SpyShield runs quietly in the background and alerts you the moment your camera or microphone switches on. It works out which app is most likely responsible, flags anything that looks wrong, and gives you one-tap ways to shut it out.

REAL-TIME CAMERA & MIC MONITORING
• Get notified whenever the camera or microphone turns on, with a live status in your notification shade.
• Flashlight use and short face-unlock camera use are filtered out, so you only see what matters.

CATCH SUSPICIOUS ACCESS
SpyShield flags use that doesn't match what you're doing:
• Camera or mic used while the screen is off or the phone is locked
• Use while you're on the home screen
• Use while you're in an app that doesn't even have that permission
• On Android 10 and newer: background apps that Android itself silenced while they tried to record

SCAN EVERY APP
See every installed app that can reach your camera or microphone, with a spy-risk score that explains itself: sideloaded from outside a trusted store, starts at boot, can keep the camera/mic running in the background, hasn't been opened in 30+ days but still has access, past suspicious events, and more.

BLOCK LIST
Mark an app as blocked and SpyShield raises an instant, high-priority alarm the second it touches your camera or mic — with one-tap shortcuts to revoke its access, force stop it or uninstall it. Suspicious-access alerts that name an app include a "Block app" button.

ACTIVITY TIMELINE & PRIVACY SCORE
• A full history of every camera and mic session: which app, when, for how long, and why it was flagged.
• A privacy score on your dashboard, with a live shield that reacts as sensors switch on and off.
• Optional privacy dot: a small on-screen indicator while the camera or mic is in use, even on older Android versions.
• Quick Settings tile and home-screen widget to check and toggle protection.
• Share a plain-text privacy report when you choose to.

PRIVATE BY DESIGN
SpyShield has no internet permission. Everything it records stays on your phone. No accounts, no ads, no analytics, no tracking.

HONEST LIMITATIONS
We'd rather tell you up front:
• No root needed — and no root used. Without root, no app can silently switch off another app's permissions. "Block" means an instant alarm plus a one-tap jump to that app's permission page, where you choose "Don't allow" (or uninstall it).
• Android deliberately hides exactly which app is using the camera or mic from regular apps. When the app on screen holds the permission, SpyShield names it confidently; for background use it shows its best guess as "Possible: …" with the most likely apps ranked.
• Naming apps requires Usage Access, which you grant in Android settings.
• Some phones (e.g. Samsung, Xiaomi, Oppo, Vivo) aggressively close background apps. Set SpyShield's battery usage to "Unrestricted" so protection keeps running.
• For a hard kill switch, Android 12+ includes Camera access and Microphone access tiles in Quick Settings — SpyShield works alongside them.

Turn on Real-time protection, grant Usage Access, allow notifications, and SpyShield takes it from there.
```

## Feature bullets (for promo text, website, release notes)

- Real-time alerts when the camera or microphone switches on
- Flags use while the screen is off, the phone is locked, or from apps not on screen
- Detects background recording that Android silenced (Android 10+)
- Spy-risk score for every app with camera/mic access, with reasons
- Block list: instant alarm plus one-tap Revoke access / Force stop / Uninstall
- Activity timeline and privacy score dashboard
- Optional on-screen privacy dot, Quick Settings tile and home-screen widget
- Shareable plain-text privacy report
- No internet permission, no account, no ads, no analytics — all data stays on the device

## Suggested screenshots (phone, 1080×2400 or 1080×1920, 4–8 images)

| # | Screen | Caption |
|---|--------|---------|
| 1 | Dashboard with the shield orb and privacy score | Know when your camera & mic are on |
| 2 | Suspicious-access alert notification (with Block app button) | Instant alerts for suspicious access |
| 3 | Dashboard while camera is live, showing the app name | See who's using your camera right now |
| 4 | Apps screen sorted by risk score | Every app with camera or mic access, risk-scored |
| 5 | App detail sheet with reasons and Revoke / Force stop / Uninstall | One tap to shut a spying app out |
| 6 | Activity timeline with a flagged event ("Accessed while the screen was off") | A full history of every camera & mic session |
| 7 | Privacy dot visible over another app + Quick Settings tile | Privacy dot, tile and widget |
| 8 | Onboarding / "Everything stays on your phone" | No internet permission. Nothing leaves your phone. |

Capture with demo data only; don't show real contacts, notifications or personal app names you would not
want public. Feature graphic (1024×500): shield orb on the dark SpyShield background, tagline
"Catch apps spying through your camera & mic".

## Category & tags

- **App category:** Tools
- **Tags (pick up to 5):** Security, Privacy, Antivirus & security, Device monitoring, Utilities
- **Contact email:** `<support email>`  **Website:** `<website>`  **Privacy policy URL:** host `PRIVACY_POLICY.md` publicly (e.g. GitHub Pages) and paste its URL

## Content rating (IARC questionnaire)

- Category: **Utility, Productivity, Communication, or Other**
- Violence, sexuality, language, controlled substances, gambling: **No**
- Users can interact or exchange content with each other: **No**
- Shares the user's current physical location with other users: **No**
- Allows users to purchase digital goods: **No**
- Expected result: **Everyone / PEGI 3 / USK 0**

## Target audience & ads

- Target age group: **18+** (simplest; a security tool not designed for children)
- Contains ads: **No**
- In-app purchases: **No**

---

## Play Console declarations

### QUERY_ALL_PACKAGES (Permissions declaration form)

**Core use case:** Antivirus / security / privacy monitoring.

> SpyShield is a privacy-protection app whose core purpose is to show the user which installed apps can access
> the camera and microphone and to alert them when one of those apps uses either sensor. To do this it must
> enumerate all installed packages and read each app's requested and granted permissions
> (PackageManager.getInstalledPackages with GET_PERMISSIONS). It uses this to (1) list every app that requests
> CAMERA or RECORD_AUDIO with a risk score, and (2) when a sensor turns on in the background, rank which
> installed apps holding that permission are the likely cause. There is no narrower alternative: the set of
> apps that may hold camera/mic access is arbitrary and cannot be declared with <queries> elements in advance.
> The package list is processed only on the device; the app has no INTERNET permission and transmits nothing.

### PACKAGE_USAGE_STATS (Usage Access)

Not a Play permissions-declaration permission, but reviewers may ask; the in-app prompt explains it before
sending the user to Settings.

> Usage Access is used to identify which app is in the foreground at the moment the camera or microphone
> switches on (UsageStatsManager.queryEvents) and which apps were used recently
> (queryAndAggregateUsageStats), so SpyShield can tell the user whether the sensor was used by the app they
> were looking at or by something in the background. It also powers the "not opened in 30+ days but still has
> access" risk signal. The user grants it manually in system settings and the app works (without naming apps)
> if it is denied. Usage data is read on demand, never stored in bulk, and never leaves the device.

### FOREGROUND_SERVICE_SPECIAL_USE (Foreground service declaration)

Manifest subtype (already declared in `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`):
`Watches camera and microphone availability to alert the user when another app uses them`

**Declaration text:**

> SpyShield runs one foreground service (SensorMonitorService, type specialUse) while the user has turned on
> "Real-time protection". The service registers CameraManager.AvailabilityCallback and
> AudioManager.AudioRecordingCallback so it is notified the instant any app starts using the camera or
> microphone, then alerts the user if the use looks suspicious. These callbacks are only delivered while the
> process is alive, and the alert has to fire within seconds of the event, so the work cannot be deferred to
> WorkManager or a job. None of the other foreground service types fit: the app does not itself use the camera
> or microphone (so "camera"/"microphone" types would be incorrect), and it is not media, location, data sync,
> health, or connected-device work. The service shows a persistent notification with live camera/mic status,
> is started and stopped only by the user (toggle in the app, Quick Settings tile), and is restarted after
> reboot only if the user left protection on.

**User impact if the service is deferred or interrupted:** camera or microphone use by another app goes
undetected and the user is not alerted.

**Video:** record a ~30 s screen capture: enable protection → persistent notification appears → open another
app's camera in the background (e.g. via a test app) → SpyShield alert fires. Upload unlisted and paste the link.

### SYSTEM_ALERT_WINDOW (privacy dot)

Not a Play-restricted permission, but describe it in the listing/review notes:

> Optional and off by default. When the user enables "Privacy dot", SpyShield draws a small, non-touchable
> indicator in the top corner of the screen (TYPE_APPLICATION_OVERLAY, FLAG_NOT_TOUCHABLE) while the camera or
> microphone is in use, similar to the indicators built into Android 12+. It is shown only while a sensor is
> active, never intercepts input, and the user grants "Display over other apps" from system settings.

Note: the code checks `Settings.canDrawOverlays` and opens `ACTION_MANAGE_OVERLAY_PERMISSION`. The manifest
declares `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`; keep it, otherwise
SpyShield never appears in the "Display over other apps" list.

### REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

Play allows this only when battery optimization would break the app's core function; reviewers decide case by
case. Justification:

> SpyShield's core function is real-time detection of camera and microphone use by other apps. Detection relies
> on system callbacks that only fire while SpyShield's monitoring service is running. On many devices, battery
> optimization (Doze, app standby and OEM battery managers) stops this service, which silently disables
> protection — exactly when the user expects to be alerted. The exemption is requested only from an onboarding
> step the user taps, with an explanation, and the app falls back to opening the system battery-optimization
> list if the direct request is unavailable. The app performs no network or periodic background work, so the
> exemption does not increase battery use beyond the idle callbacks.

If Play rejects it: drop the permission and keep only the fallback
(`Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), which needs no permission.

### Other permissions (no form, for reviewer notes)

| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE` | Required to run the monitoring service above |
| `POST_NOTIFICATIONS` | Persistent status notification and camera/mic alerts (Android 13+ runtime prompt) |
| `RECEIVE_BOOT_COMPLETED` | Restart protection after reboot/update, only if the user left it on |
| `REQUEST_DELETE_PACKAGES` | "Uninstall" button opens the system uninstall dialog; the user confirms there |

SpyShield does **not** request CAMERA, RECORD_AUDIO, INTERNET, location, contacts, SMS, accessibility or
device-admin access.

---

## Data safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | N/A — nothing is transmitted (no INTERNET permission) |
| Do you provide a way for users to request that their data is deleted? | N/A — nothing is collected. (In-app "clear history" and uninstalling remove all local data.) |

Why "No" is accurate under Play's definitions:

- **Collected** means transmitted off the device. SpyShield has no `INTERNET` permission and no SDKs, so it
  cannot transmit anything. Data processed only on the device is not "collected".
- **Installed apps / app activity** (package list, permissions, usage stats, sensor event history) are read and
  stored on-device only, in app-private storage (`allowBackup="false"`, so not copied to cloud backups).
- **Shared privacy report:** the text report is sent only when the user taps Share and picks an app in the
  Android share sheet. Play treats user-initiated transfers like this as not "sharing".
- No ads, no analytics, no crash reporting, no account.

Security practices section: data is not transmitted, so "encrypted in transit" does not apply; leave
"independent security review" unchecked unless one is done.
