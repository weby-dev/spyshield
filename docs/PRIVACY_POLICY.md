# SpyShield Privacy Policy

**Effective date:** `<YYYY-MM-DD>`
**Developer:** `<developer or company name>`
**Contact:** `<privacy contact email>`

SpyShield helps you find out which apps use your phone's camera and microphone. This policy explains what
the app handles and where it goes. The short version: **nothing leaves your device.**

## 1. We don't collect your data

- SpyShield does not request the Internet permission, so it **cannot send data anywhere**.
- There are no accounts, ads, analytics, crash reporters or third-party SDKs.
- We (the developer) never receive, see or store any of your information.

## 2. What is stored on your device

SpyShield keeps a small amount of data in its private app storage on your phone. Other apps cannot read it,
and it is excluded from Android cloud backups.

| Data | What it contains | Why |
|---|---|---|
| Sensor event history | For each camera/mic session (up to the most recent 400): which sensor, the app name and package SpyShield attributed it to (or "Unknown"), up to three possible apps for background use, start and end time, whether it was flagged and why, and whether the app was on your block list | Activity timeline, privacy score, risk scoring and alerts |
| Block list | Package names of apps you marked as blocked | Raise an alarm when those apps use the camera or mic |
| Trusted list | Package names of apps you marked as trusted | Avoid alerting you about apps you've approved |
| Permission snapshot | For each app scanned: its package name and whether it held camera and microphone access at the last scan | Tell you when an app newly gains or loses camera/mic access |
| Settings | Whether protection is on, whether the privacy dot is on, whether onboarding is finished | Remember your choices |

You can clear the event history from inside the app at any time. Uninstalling SpyShield, or using
**Settings → Apps → SpyShield → Storage → Clear data**, deletes all of it.

## 3. Information the app reads but does not store

To do its job SpyShield reads, on the device and only when needed:

- **The list of installed apps and their permissions**, to show which apps can access the camera or microphone
  and to score their risk (only the camera/mic access snapshot described above is kept).
- **App usage information** (with Usage Access you grant), to see which app was on screen when a sensor turned on
  and when apps were last used.
- **Camera and microphone on/off status** from Android. SpyShield **never** opens the camera, records audio or
  sees any image or sound — it only learns *that* a sensor is busy. It does not request camera or microphone
  permission.
- **Screen and lock state**, to tell whether a sensor was used while the phone was off or locked.

## 4. Permissions and why we ask

| Permission | Purpose |
|---|---|
| Usage Access (`PACKAGE_USAGE_STATS`) | Identify which app is in front when the camera or mic turns on |
| See all apps (`QUERY_ALL_PACKAGES`) | Find every app that can access the camera or mic |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Keep monitoring running while protection is on, with a visible notification |
| Notifications (`POST_NOTIFICATIONS`) | Show the protection status and camera/mic alerts |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Restart protection after a reboot, only if you left it on |
| Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Ask you to stop the system from closing the monitor in the background |
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | Optional privacy dot shown while the camera or mic is in use; off by default |
| Request uninstall (`REQUEST_DELETE_PACKAGES`) | Open Android's uninstall dialog when you tap "Uninstall" on an app; you confirm it there |

Usage Access, notifications, battery and overlay permissions are all optional and can be revoked in Android
settings. Some features stop working without them; the app tells you which.

## 5. Sharing a report

SpyShield can create a plain-text privacy report. It is only sent if **you** tap Share and choose an app
(for example email or a messenger) in Android's share sheet. What happens next is governed by the app you
choose. SpyShield does not send it anywhere on its own.

## 6. Children

SpyShield is a general-audience security tool and is not directed at children under 13. It collects no data
from anyone.

## 7. Changes

If this policy changes, the new version will be posted at this address with a new effective date. Because
SpyShield collects no data, changes will not affect information we hold about you — we hold none.

## 8. Contact

Questions about this policy: `<privacy contact email>`
