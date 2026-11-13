# Kabox — The Academy of Focus

Kabox is an original wizard-academy-inspired productivity app for Android. It uses a dark, quiet, antique-gold visual language; it does not use Harry Potter characters, Hogwarts branding, or franchise artwork.

## Current build: 0.1.1 (CI workflow fix)

### Working in this build
- Focus timer with 25, 45, and 60 minute presets.
- Start, pause, resume, and reset controls.
- Timer state survives app process closure and can recover its remaining time after reopening.
- Completed focus sessions, total focus minutes, daily focus minutes, and streak count are stored locally.
- Three editable daily quest checkboxes are persisted locally.
- Screen-time ledger reads Android UsageStats after the user manually grants Usage Access in Android Settings.
- Dark-academia Kabox UI with five navigation sections.

### Not implemented yet
- Blocking other apps (requires a dedicated AccessibilityService design, user disclosure/consent, and device testing).
- Domain-based website filtering (requires a VpnService, explicit consent, and network testing).
- Reliable Reels/Shorts channel-level blocking (not promised; encrypted traffic and app UI changes limit what can be detected).
- Multiplayer rooms, cloud sync, leaderboards, subscriptions, and ambient audio.
- Background timer completion notifications / exact alarms.

These items are marked as planned in the app. The UI does not claim that these features already work.

## Build requirements
- Android Studio compatible with Android Gradle Plugin 9.4.0.
- JDK 17.
- Android SDK Platform 37 and Build Tools 36.0.0.
- Internet access for Gradle and Maven dependency downloads.

The project uses Gradle 9.6.0, AGP 9.4.0, AGP built-in Kotlin 2.2.10 with its matching Compose compiler plugin, and Compose BOM 2026.09.00. AGP 9.4.0 requires JDK 17 and Gradle 9.6.0.

## Build an APK
Run the included Gradle bootstrap launcher from the project root after installing JDK 17 and Android SDK Platform 37:

```bash
./gradlew assembleDebug
```

On Windows, run `gradlew.bat assembleDebug`. The first run downloads Gradle 9.6.0 and caches it; later runs reuse that copy. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. You can also use the GitHub Actions workflow included in this ZIP; it builds the debug APK automatically after the project is pushed.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Publish all project files to GitHub
This ZIP contains `scripts/publish-to-github.sh` and `scripts/publish-to-github.ps1`. They commit the extracted project files and create a GitHub repository named `Kabox` (or push to the existing repository) using GitHub CLI.

You need a GitHub account, Git, and GitHub CLI (`gh`) installed. Sign in once with:

```bash
gh auth login
```

Then run the matching script from the extracted project root. See `docs/github-upload.md` for the quickest instructions.

## Privacy notes
- Kabox v0.1 has no analytics SDK and no cloud backend.
- Timer and quest records are saved locally in Android SharedPreferences.
- Screen-time details are queried on-device after the user grants Usage Access. They are not uploaded by this build.
- No Accessibility, VPN, overlay, contacts, location, or notification permission is requested in this build.

## Project layout

```text
app/src/main/java/com/kabox/app/
  MainActivity.kt             Compose app and screens
  data/LocalStore.kt          Local timer, session, streak, and quest persistence
  usage/UsageRepository.kt    Android Usage Access integration

docs/                         Architecture, permissions, and release notes
scripts/                       One-command GitHub publishing helpers
.github/workflows/             GitHub Actions Android build workflow
```

## Important
This is a functional first milestone, not a finished Regain-equivalent product. App blocking, website filtering, multiplayer, and production release require separate implementation and testing. Do not publish claims for a feature until it has been built and verified on real devices.


## Updating the existing GitHub repository from Termux

After downloading the updated source ZIP and extracting it, run `bash scripts/update-existing-github.sh` from the `Kabox` project folder. The script uses your existing `gh auth login` session, clones your existing `Kabox` repository, copies the project (including hidden workflow files), commits the changes, and pushes to `main`. It does not print or ask for your GitHub token.

The GitHub Actions workflow uses `android-actions/setup-android@v4.0.2` and explicitly requests only `platform-tools`; it does not request the retired standalone SDK `tools` package.
