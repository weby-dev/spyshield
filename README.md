# SpyShield — catch apps spying through your camera & mic

An Android app (Kotlin + Jetpack Compose, Android 8.0+) that:

- **Watches your camera and microphone in real time** from a background service and shows who is using them.
- **Flags anomalies** — sensor use while the screen is off, while the phone is locked, while you're on the home
  screen, or while you're in an app that doesn't even have that permission. On Android 10+ it also catches
  background apps that Android silently muted.
- **Scans every installed app** that can reach your camera/mic and gives each one a spy-risk score
  (sideloaded, auto-start at boot, background camera/mic services, unused for 30+ days but still has access,
  past suspicious events…).
- **Blocks apps**: a blocked app sets off an instant high-priority alarm the moment it touches the camera or mic,
  with one-tap **Revoke access**, **Force stop** and **Uninstall**. Alerts for unknown apps have a **Block app** button.
- Keeps a full **activity timeline** and a **privacy score** on a dashboard with a live animated shield.
- **Privacy dot**: an optional green/orange dot drawn over every app while the camera/mic is on.
- **Quick Settings tile** and **home-screen widget** for protection status at a glance.
- **New-app alerts** when a freshly installed/updated app can use the camera or mic, and a dashboard card when an
  existing app *gains* camera/mic access.
- **Trusted apps** to silence false alarms, and a **shareable privacy report**.
- Fewer false alarms: phone/VoIP calls, "Hey Google" hotword, voice typing, internal-audio screen recorders,
  flashlight and face unlock are recognised; alerts are re-checked before firing with a 60 s cooldown.
- First-run **onboarding** that walks through Usage Access, notifications and battery-optimisation exemption.

Store listing, privacy policy and release checklist are in [docs/](docs/).

## Build & run

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. **File → Open** and pick this `SpyShield` folder. Let Gradle sync (it downloads Gradle 8.9 and the SDK 35 platform).
3. Plug in a phone with USB debugging on (or start an emulator) and press **Run ▶**.
4. In the app: turn on **Real-time protection**, tap **Grant access** to allow Usage Access, and allow notifications.

To build an APK from the command line after the first sync: `./gradlew assembleDebug`
(output: `app/build/outputs/apk/debug/app-debug.apk`).

## How detection works

Android deliberately hides from regular apps *which* app holds the camera or mic. SpyShield infers it:

| Signal | API |
|---|---|
| Camera switched on/off | `CameraManager.AvailabilityCallback` (flashlight filtered out via `TorchCallback`) |
| Mic recording started/stopped, silenced background recording | `AudioManager.AudioRecordingCallback`, `isClientSilenced` |
| App on screen at that moment | `UsageStatsManager` events (needs Usage Access) |
| Screen off / locked | `PowerManager`, `KeyguardManager` |
| Likely culprits for background use | apps holding the permission, ranked by user-installed + most recently used |

If the app on screen holds the permission, the use is treated as normal. Otherwise it's flagged, and the most
likely apps are listed. Short camera use on the lock screen (face unlock) is ignored.

## Honest limitations

- **No app can silently revoke another app's permissions without root.** "Block" = instant alarm + one-tap jump to
  that app's permission page to switch Camera/Microphone to *Don't allow* (or uninstall it).
  For a hard kill switch, use Android 12+'s Quick Settings **Camera access / Microphone access** tiles.
- Background culprit identification is a best guess (shown as "Possible: …"), because Android doesn't expose the
  exact package to non-system apps.
- Phones with aggressive battery savers (Xiaomi, Oppo, Vivo, Samsung) may kill the service — set SpyShield's battery
  usage to **Unrestricted**.
- `QUERY_ALL_PACKAGES` and the `specialUse` foreground service need a justification if you publish on Google Play.
