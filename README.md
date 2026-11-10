# Nightstand

Nightstand is an Android screen saver (a `DreamService`) for a phone charging on a bedside table. When you tap the screen saver it shows a clock for a few seconds, but only within a time window you choose and, optionally, only while the phone rests on one of its long edges.

The app's settings screen controls the time window, the clock timeout, the landscape requirement and the clock's appearance.

## Requirements

- Android 12 (API 31) or later
- JDK 21
- Android SDK, with `ANDROID_HOME` set or `sdk.dir` in `local.properties`

## Build and check

Install the debug build on a connected phone:

```powershell
.\gradlew.bat installDebug
```

Run the full check (formatting, Detekt, lint, unit tests and a debug build):

```powershell
.\scripts\verify.ps1
```

## Enable it on the phone

Open Nightstand and tap **Open screen saver settings**. Choose Nightstand as the screen saver and set it to start while charging. The screen saver starts once the screen times out while the phone is charging, not as soon as it is plugged in. Back in the app, make sure **Nightstand mode** is on.

## More

- [TESTING.md](TESTING.md): the manual device checklist and smoke script
- [LICENSE](LICENSE): MIT licence for the app
- [FONTS_LICENSE.md](FONTS_LICENSE.md): SIL Open Font License for the bundled fonts
