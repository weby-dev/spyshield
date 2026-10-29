# SpyShield — Release checklist

Steps to go from this repo to a signed `.aab` on Google Play. Commands are for Windows PowerShell from the
`SpyShield` folder; on macOS/Linux use `./gradlew` and `$JAVA_HOME/bin/keytool`.

## 1. Signing

### 1.1 Generate an upload keystore (once)

Use the JDK that builds the project (Android Studio's `jbr`, or the bundled JDK 17):

```powershell
$jdk = "C:\path\to\jdk-17"   # or "C:\Program Files\Android\Android Studio\jbr"
& "$jdk\bin\keytool.exe" -genkeypair -v `
    -keystore "$HOME\keys\spyshield-upload.jks" `
    -alias spyshield-upload `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -dname "CN=<your name>, O=<org>, C=<country code>"
```

keytool prompts for the store and key passwords. Then:

- [ ] Keep the `.jks` **outside** the repo and back it up (password manager / encrypted storage). Losing it means
      asking Google to reset the upload key.
- [ ] Enroll in **Play App Signing** (default for new apps). Google holds the app signing key; this keystore is only
      the upload key.

### 1.2 `keystore.properties` (never committed)

Create `SpyShield/keystore.properties`:

```properties
storeFile=C:/Users/<you>/keys/spyshield-upload.jks
storePassword=<store password>
keyAlias=spyshield-upload
keyPassword=<key password>
```

- [ ] Add `keystore.properties` and `*.jks` to `.gitignore`.

### 1.3 `signingConfigs` in `app/build.gradle.kts`

```kotlin
import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    // ...
    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

The `exists()` guard keeps debug builds and CI without the file working; `bundleRelease` then fails at the
signing step, which is what you want.

## 2. Versioning

In `app/build.gradle.kts` → `defaultConfig`:

- [ ] `versionCode` — integer, **must increase** for every upload to Play (1, 2, 3 …). Play rejects reused codes,
      even from a discarded draft.
- [ ] `versionName` — user-visible, e.g. `1.0.0`, `1.1.0`.
- [ ] `targetSdk` — currently 35. Check Play's current target API requirement before uploading
      (Play raises it every August for new apps and updates); bump and retest if needed.
- [ ] Write release notes (≤ 500 characters per language).

## 3. Minify / R8

`isMinifyEnabled` is `false` today. Turning it on is safe and shrinks the app substantially:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

- Compose, AndroidX and Kotlin ship their own consumer R8 rules — **no extra keep rules needed**.
- Manifest-declared components (activity, `SensorMonitorService`, `ProtectionTileService`, receivers,
  `StatusWidgetProvider`) are kept automatically by AAPT-generated rules.
- No reflection or serialization libraries: `org.json` comes from the platform and `AccessEvent` JSON uses
  explicit string keys, so field renaming doesn't break stored history.
- `proguard-rules.pro` can stay as is.
- [ ] After enabling, smoke-test the **release** build on a device (not just debug): protection toggle,
      an alert, the Apps scan, the widget and the tile.
- [ ] Keep `app/build/outputs/mapping/release/mapping.txt` for each release (Play also stores it with the
      bundle) to deobfuscate crash traces in Play Console.

## 4. Build the bundle

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
.\gradlew.bat clean testDebugUnitTest bundleRelease
```

Output: `app\build\outputs\bundle\release\app-release.aab`

- [ ] Unit tests pass.
- [ ] Verify the bundle is signed with the upload key:
      `& "$env:JAVA_HOME\bin\jarsigner.exe" -verify -verbose:summary app\build\outputs\bundle\release\app-release.aab`
- [ ] Optional on-device check of the exact release artifact with
      [bundletool](https://github.com/google/bundletool): `build-apks --connected-device` then `install-apks`.

## 5. Pre-release code checks

- [ ] `SYSTEM_ALERT_WINDOW` is still declared in `AndroidManifest.xml` (needed for the privacy dot to appear in
      "Display over other apps").
- [ ] No `INTERNET` permission crept in through a dependency (check the merged manifest:
      `app\build\intermediates\merged_manifests\release\...\AndroidManifest.xml`). The privacy policy and Data
      safety answers depend on it.
- [ ] `allowBackup="false"` still set.
- [ ] No debug logging of package names or events.

## 6. Testing matrix

Test the **release** build. For each row: fresh install → onboarding → enable protection → grant Usage Access →
trigger camera and mic from another app in the foreground (should be "normal") and in the background / screen
off (should alert) → block an app and confirm the alarm → reboot and confirm protection restarts.

| Android | API | Focus |
|---|---|---|
| 8.0 / 8.1 | 26 / 27 | minSdk; `checkOpNoThrow` usage-access path; `getInstalledPackages(int)`; notification channels; overlay dot (no system indicator on this version) |
| 10 | 29 | `isClientSilenced` — background recording silenced by Android is reported; scoped usage events |
| 12 | 31 | System privacy indicators and Quick Settings Camera/Mic access toggles coexist with SpyShield; background FGS-start restrictions (boot receiver and user-tapped tile are exempt); face unlock on lock screen not flagged |
| 13 | 33 | `POST_NOTIFICATIONS` runtime prompt; `PackageInfoFlags` overload of `getInstalledPackages` |
| 14 | 34 | `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` passed to `startForeground` (crash if missing); user can dismiss FGS notification; widget + tile |
| 15 | 35 | Edge-to-edge enforced (check insets on every screen); BOOT_COMPLETED start of the `specialUse` service still allowed; 16 KB page-size devices/emulator |

Also check on each: flashlight toggle does **not** create an event; Clear history; Share report; dark/light
system theme; font scale 1.3+; small (360 dp) screen.

### OEM battery managers

The service must survive overnight with the screen off. Test after 1+ hour idle and after swiping SpyShield away
from Recents.

| OEM | Settings to verify / document for users |
|---|---|
| Samsung (One UI) | Settings → Apps → SpyShield → Battery → **Unrestricted**; Battery → Background usage limits → make sure SpyShield is not in **Sleeping / Deep sleeping apps** |
| Xiaomi / Redmi / POCO (MIUI / HyperOS) | App info → **Autostart** on; Battery saver → **No restrictions**; lock the app in Recents (long-press → lock) |
| Oppo / Realme / OnePlus (ColorOS) | Battery → **Allow background activity**, **Allow auto launch** |
| Vivo (Funtouch / OriginOS) | Background power consumption → **Allow**; Autostart on |
| Pixel / stock | Battery → **Unrestricted** (the onboarding battery step should cover this) |

- [ ] If the service is killed on a device, note it in the listing's "Honest limitations" / FAQ.

## 7. Play Console

- [ ] Store listing, screenshots, feature graphic — see `docs/PLAY_STORE_LISTING.md`.
- [ ] Privacy policy hosted at a public URL — `docs/PRIVACY_POLICY.md`.
- [ ] App content: Data safety, Content rating, Target audience, Ads = No, Permissions declaration
      (`QUERY_ALL_PACKAGES`), Foreground service declaration (`specialUse`, with demo video).
- [ ] Upload to **Internal testing** first; install from Play on at least one physical device.
- [ ] New personal developer accounts: closed test with the required number of testers for the required period
      before production access is granted.
- [ ] Promote to Production with a staged rollout (e.g. 20 %), watch Android vitals (ANRs, crashes) for a few
      days, then 100 %.
