# Kabox 0.3.0 — Focus & Screen Time

Kabox is an original, green-themed Android focus companion. Its product direction is inspired by the clarity and usefulness of modern digital-wellbeing apps, but it uses its own brand, layout, components and interaction design. Kabox is not affiliated with Regain or EpowerX Labs.

## What works in this release

- Refined green-and-white interface with cards, progress rings, weekly bars, animated screen transitions and touch feedback.
- Focus countdown timer with 25 / 45 / 60 / 90 minute presets.
- Pomodoro mode with 25-minute focus intervals, 5-minute short breaks and a 15-minute break after every fourth completed focus interval.
- Stopwatch mode with pause, resume and save.
- Focus timer state persists across activity/process recreation using a stored end time.
- Local daily focus minutes, completed sessions, streak calculation and daily goal.
- Weekly focus activity chart based on locally stored sessions.
- Android Usage Access integration and top-app usage for today (requires the user to grant Usage Access in Settings).
- Optional accessibility-based app blocker. It can pause apps selected by the user during a focus session or all the time, and can enforce per-app daily usage limits.
- Prominent in-app disclosure before opening Android Accessibility settings. The service observes the foreground package only and does not retrieve window content.
- Daily focus reminder using Android's inexact repeating alarm and notifications; the reminder schedule is restored after device reboot (notification permission may be required on Android 13+).
- Kabox Plus price placeholder showing ₹39/month. Billing is intentionally not active in this debug build.
- GitHub Actions workflow that builds and uploads a debug APK.

## Not yet implemented

These features are deliberately not presented as working in this build:

- Per-screen YouTube Shorts / Instagram Reels blocking. The current blocker pauses a whole selected app. Selective in-app content blocking depends on changing app interfaces and needs separate device/version testing.
- Website and browser-domain blocking. This requires a separately tested VPN/DNS filtering module and user consent.
- Multiplayer rooms, online leaderboards and cloud sync. These require a configured backend, authentication, security rules and server-side tests.
- Ambient music library, widgets, and active Google Play subscriptions. Subscription billing needs a Play Console product/base plan and a real purchase/restore test.
- Uninstall prevention or claims that blocking cannot be bypassed. Android always leaves the device owner in control of device permissions.

## Requirements and build configuration

- JDK 17
- Android Gradle Plugin 8.10.0
- Gradle 8.11.1
- Kotlin 2.2.10
- Compile SDK 36 (Android 16)
- Target SDK 35
- Minimum SDK 26 (Android 8.0)

The CI workflow installs `platforms;android-36` and `build-tools;35.0.0`. It does not request the unavailable API 37 package or the removed legacy SDK `tools` package.

## First-run setup on Android

1. Install Kabox.
2. Open **Stats** and enable **Usage Access** if you want screen-time reports and daily app limits.
3. Open **Block**, read the accessibility disclosure, then choose **Enable app blocker** and explicitly enable Kabox in Android Accessibility settings.
4. Select the apps you want paused. Choose a daily limit from the three-dot menu at the end of an app row if required.
5. In **Focus**, enable **Block distractions** to apply the selected-app block while a focus timer/stopwatch is active.
6. In **Settings**, choose a daily reminder time. Allow notifications on Android 13+.

Accessibility service behavior is local to the device. Kabox uses the service only to observe the foreground app package and intervene for selected apps; the service is not configured to read window contents.

## Build an APK with GitHub Actions

1. Push this project to the `main` branch of your GitHub repository.
2. Open **Actions**.
3. Select **Build Kabox Android APK**.
4. Open the latest green workflow run.
5. Download the `Kabox-debug-apk` artifact and extract the APK.

The workflow validates that the APK file exists before uploading it. A green workflow confirms compilation and artifact creation; an actual install and feature test on the target phone is still required.

## Publish this source to the existing repository from Termux

After downloading this ZIP into the phone's Downloads folder, run:

```bash
unzip -o ~/storage/shared/Download/Kabox-Android-Source-v0.3.0.zip -d ~/kabox-v030 && bash ~/kabox-v030/Kabox/scripts/publish-to-github.sh Aisaarthi13/Kabox
```

The script clones the repository, replaces its project files, commits the change and pushes it. It requires `gh auth login` to have already been completed.
