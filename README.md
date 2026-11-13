# Kabox — The Academy of Focus

Kabox is an offline-first Android focus companion with a dark-academia visual style.

## Included in v0.2.0

- Focus timer with 25, 50 and 90 minute presets.
- Start, pause and reset controls.
- Timer end-time persistence, so a running session can be restored after the app process is recreated.
- Local focus-minute, session and House Point totals.
- A daily focus goal and first-session quest.
- Android Usage Access screen-time total (requires the user to enable Usage Access in Android Settings).
- Home, Focus and Insights screens.
- GitHub Actions workflow that builds and uploads a debug APK.

## Not implemented yet

App blocking, website blocking, per-app usage charts, multiplayer study rooms, cloud sync, subscriptions and ambient audio are not included in this MVP. The dashboard labels planned modules as in development instead of presenting them as working features.

## Build configuration

- Android Gradle Plugin: 8.10.0
- Gradle: 8.11.1
- Kotlin: 2.2.10
- JDK: 17
- Compile SDK: Android 16 / API 36
- Target SDK: 35
- Minimum SDK: 26

The build workflow installs `platforms;android-36` and `build-tools;35.0.0`. It does not request the unavailable `platforms;android-37` package and does not install the removed legacy `tools` package.

## Build the APK with GitHub Actions

1. Upload this project to the `main` branch of a GitHub repository.
2. Open **Actions**.
3. Select **Build Kabox Android APK**.
4. Open the latest successful run.
5. Download the `Kabox-debug-apk` artifact.

The workflow uses a pinned Gradle version and verifies that the APK file exists before uploading it.

## Publish this ZIP to an existing repository from Termux

After downloading and extracting the ZIP in Termux, run:

```bash
bash scripts/publish-to-github.sh Aisaarthi13/Kabox
```

The script clones the chosen repository, replaces its tracked project files with this project, commits and pushes the result. It requires `gh auth login` to have been completed first.

## Permissions

Kabox declares `PACKAGE_USAGE_STATS`, but Android treats Usage Access as a special app access rather than an ordinary runtime permission. The user must explicitly enable it from the Insights screen. The app does not request Accessibility, VPN, overlay or device-admin permissions in this version.

## Build verification note

The source ZIP is checked for archive integrity, XML well-formedness, required SDK settings and workflow consistency before delivery. A real Android Gradle build must still run on GitHub Actions or Android Studio; the artifact cannot honestly be called device-tested until that build and an installation test succeed.
