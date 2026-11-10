# Nightstand device testing

The manual checklist for the Samsung Galaxy S26 Ultra (SM-S948B, serial `<device serial>`, 1080x2340 portrait). It is run first against the debug build and finally against the signed release. Every row needs a passing Result against the release APK before v1 is done.

Items marked **[hands-on]** need the phone physically handled (moved, unlocked, looked at in a dark room). Everything else can be driven over adb with the phone plugged in over USB, which also keeps it charging.

Until the real wording is confirmed on the phone, the One UI menu path is the provisional "Settings > Display > Screen saver" and the start option is the provisional "While charging". Update both here once confirmed.

## Build and install

### Debug build

```powershell
.\gradlew.bat installDebug
adb shell am start -n dev.lewisscoging.nightstand/.MainActivity
```

### Signed release build

1. The owner creates a keystore outside the repo with the JDK's `keytool` and writes the gitignored `keystore.properties` at the repo root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without that file `assembleRelease` produces an unsigned APK.
2. Build and check the signature:

   ```powershell
   .\gradlew.bat assembleRelease
   & "$env:ANDROID_HOME\build-tools\37.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
   ```

3. The debug and release builds have different signatures. Uninstall the debug build first (its settings are lost), then install the release:

   ```powershell
   adb uninstall dev.lewisscoging.nightstand
   adb install app\build\outputs\apk\release\app-release.apk
   ```

### Smoke script

`.\scripts\device-smoke.ps1` installs the debug build, selects and enables the dream through `adb shell settings put secure ...`, starts the dream, taps the screen centre, saves a screenshot to `build\smoke\smoke-<timestamp>.png` and exits the dream with the power key. `.\scripts\device-smoke.ps1 -WhatIf` prints the commands without touching adb.

### Useful adb commands

| Purpose | Command |
|---|---|
| Start the dream now | `adb shell cmd dreams start` (fallback `adb shell am start -n com.android.systemui/.Somnambulator`; note here which one works) |
| Tap the screen centre | `adb shell input tap 540 1170` |
| Exit the dream | `adb shell input keyevent KEYCODE_POWER` |
| Screenshot | `adb shell screencap -p /sdcard/ns.png` then `adb pull /sdcard/ns.png <local path>` (don't redirect `adb exec-out` in PowerShell 5.1) |
| App process id | `adb shell pidof dev.lewisscoging.nightstand` |
| Light / dark mode | `adb shell cmd uimode night no` / `adb shell cmd uimode night yes` |

## Checklist

Fill in Result (`Pass` / `Fail`), Date and Notes as each item is run. Note which build (debug or release) was tested.

### Setup

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 1.1 | Open the launcher. | A "Nightstand" icon (amber crescent and star) is listed; the themed-icon mode shows the monochrome layer. | | | |
| 1.2 | Open Nightstand from the launcher. | The settings screen opens with the "Set up the screen saver" card at the top, showing the menu path and option label. | | | |
| 1.3 | Tap "Open screen saver settings" on the card. | The One UI screen saver page opens (or Display settings if that page is missing). | | | |
| 1.4 | **[hands-on]** Follow the menu path on the screen saver page, select Nightstand, and set it to start "While charging". | Nightstand is listed and can be selected; the option saves. | | | |
| 1.5 | **[hands-on]** Tap the gear icon next to Nightstand on the screen saver page. | The Nightstand settings screen opens. | | | |

### Activation

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 2.1 | Plug in the charger, leave the phone untouched, wait for the system screen timeout. | The dream starts after the timeout (not on plug-in), showing the background only, with no clock. | | | |
| 2.2 | Unplug the charger and wait for the screen timeout. | The dream does not start; the screen turns off normally. | | | |
| 2.3 | Start the dream with the adb start command while charging. | The dream starts immediately, background only. | | | |

### Clock

Defaults: 15 s timeout, inside the window, phone on its stand.

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 3.1 | **[hands-on]** With the dream showing the background, tap once. | The clock fades in (about 300 ms) showing the current time, and ticks each second. | | | |
| 3.2 | Wait without touching. | The clock fades out 15 s after the tap; the background stays. | | | |
| 3.3 | Tap, wait about 10 s, tap again while the clock is Visible. | The timer restarts: the clock hides 15 s after the second tap, not the first. | | | |
| 3.4 | Send 20 rapid taps (`adb shell input tap 540 1170` in a loop). | The clock shows and hides once, one timeout after the last tap; logcat shows no duplicate tickers, errors or crashes. | | | |
| 3.5 | Start and stop the dream 3 times, then check `adb logcat -d *:W` for the app's PID. | No crash, no "ViewTreeLifecycleOwner not found", no app warnings or errors. | | | |

### Exits

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 4.1 | With the dream running, press the power button (or `adb shell input keyevent KEYCODE_POWER`). | The dream ends and the screen turns off; pressing again shows the lock screen. | | | |
| 4.2 | **[hands-on]** With the dream running, unlock the phone (fingerprint, face or swipe). | The dream ends and the phone returns to normal use. | | | |

### Gates

Taps must be ignored whenever a gate is closed, and a Visible clock must hide immediately when a gate closes. The dream never ends by itself.

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 5.1 | Turn "Nightstand mode" off in settings, start the dream, tap. | The background shows; taps are ignored. Turn it back on afterwards. | | | |
| 5.2 | Set a time window that excludes now, start the dream, tap. | Taps are ignored. Restore the window afterwards. | | | |
| 5.3 | Set a window that crosses midnight and includes now (for example start an hour ago, end before it). | Taps show the clock. | | | |
| 5.4 | Set the start and end times equal, start the dream, tap. | Taps show the clock at any time of day (always active). | | | |
| 5.5 | **[hands-on]** Phone reclined on a stand, resting on a long edge; tap. | The clock shows. | | | |
| 5.6 | **[hands-on]** Show the clock, then lay the phone flat on its back. | Within about 2 s the clock hides and taps are ignored. | | | |
| 5.7 | **[hands-on]** Show the clock, lay the phone face-down, wait 3 s, take an adb screenshot. | The screenshot shows no clock. | | | |
| 5.8 | **[hands-on]** Hold the phone in portrait, upright; tap. | Taps are ignored. | | | |
| 5.9 | **[hands-on]** Put the phone back on the stand; wait about 2 s; tap. | Taps work again. | | | |
| 5.10 | Turn "Require landscape" off; hold the phone in portrait (**[hands-on]**); tap. | The clock shows. Turn it back on afterwards. | | | |
| 5.11 | Exit the dream, then run `adb shell dumpsys sensorservice`. | No active sensor registration for `dev.lewisscoging.nightstand`. | | | |

### Settings

Change each setting in the app, start the dream over adb, tap, and confirm by screenshot or by looking.

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 6.1 | Screenshot the settings screen with `cmd uimode night no`. | Light mode looks right: readable text, no clipped rows. | | | |
| 6.2 | Screenshot the settings screen with `cmd uimode night yes`. | Dark mode looks right. | | | |
| 6.3 | Change the start and end times with the time picker. | The rows show the new times; the window behaves as in Gates. | | | |
| 6.4 | Set the clock timeout to 5 s, then to 120 s. | The clock hides after 5 s, then after 120 s. | | | |
| 6.5 | Set the clock format to 12h, then 24h, then System (try System with the phone set to both 12- and 24-hour). | 12h shows `h:mm` with no AM/PM; 24h shows `HH:mm`; System follows the phone's setting. | | | |
| 6.6 | Select each clock font: System, Digital, Bubble. | Each renders the time correctly and legibly; the date line stays in the system font. | | | |
| 6.7 | Select each text colour: White, Warm white, Red, Amber, Green, Cyan. | Each shows in the dream and is legible on true black. | | | |
| 6.8 | Select each background: True black, Dark grey, Midnight blue, Deep red. | Each shows in the dream; the clock stays legible on each. | | | |
| 6.9 | Set clock brightness to 5%, then 100%. | 5% is dim but readable; 100% is full colour. Non-black backgrounds dim with it. | | | |
| 6.10 | **[hands-on]** In a dark room, view the clock at 5% brightness. | Legible without being glaring. Record the result in Notes. | | | |
| 6.11 | Turn "Show date" on, then off. | A small date line appears under the clock, then disappears. | | | |
| 6.12 | Change several settings while the dream is running (for example colour and timeout). | The dream picks up the change live without restarting. | | | |
| 6.13 | Change every setting away from its default, force-stop the app (`adb shell am force-stop dev.lewisscoging.nightstand`) and relaunch it. | Every value persisted. | | | |

### Burn-in

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 7.1 | With "Burn-in protection" on and the timeout at 120 s, show the clock and screenshot just before and just after a minute boundary. | The clock is offset by a few pixels (at most 16 px) and the offset changes each minute. | | | |
| 7.2 | Turn "Burn-in protection" off and repeat. | The clock stays exactly centred. | | | |

### Timezone

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 8.1 | Turn automatic time zone off, change to a different zone, start the dream, tap. | The clock shows the new zone's time. Restore automatic time zone afterwards. | | | |
| 8.2 | Change the zone while the clock is Visible. | The clock switches to the new zone's time on the next second. | | | |

### Battery

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 9.1 | With the clock Hidden, read `adb shell dumpsys gfxinfo dev.lewisscoging.nightstand` twice, 2 minutes apart. | The total frame count does not increase. | | | |
| 9.2 | With the dream running, run `adb shell dumpsys power`. | No wake locks held by `dev.lewisscoging.nightstand`. | | | |
| 9.3 | Run `adb shell dumpsys batterystats --reset`, dream Hidden for 30 minutes, then `adb shell dumpsys batterystats dev.lewisscoging.nightstand`. | The app's CPU time is negligible. | | | |
| 9.4 | **[hands-on]** Overnight run: window covering the night, phone on its stand on charge, tapped during the night, taken off the stand in the morning. Then `adb shell dumpsys dropbox --print data_app_crash` and `--print data_app_anr`. | No crash or ANR from the package, no stuck clock, no battery anomaly under Settings > Battery. | | | |

### Offline

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 10.1 | Run `& "$env:ANDROID_HOME\build-tools\37.0.0\aapt2.exe" dump permissions <apk>` on the APK under test. | No `INTERNET` permission; the only permission is AndroidX's `dev.lewisscoging.nightstand.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. | | | |

### Upgrade

| # | Steps | Expected | Result | Date | Notes |
|---|---|---|---|---|---|
| 11.1 | Change several settings, then install the release APK over the installed release (`adb install -r app\build\outputs\apk\release\app-release.apk`). | The install succeeds without uninstalling, and every setting survives. | | | |
