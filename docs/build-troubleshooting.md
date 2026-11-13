# Kabox build troubleshooting

## `Failed to find package 'tools'` during Android SDK setup

This is an SDK setup workflow failure, not an app-source compilation error. Google no longer publishes the standalone `tools` SDK package. Kabox's workflow now uses `android-actions/setup-android@v4.0.2` with `packages: 'platform-tools'` and installs the specific Android platform/build-tools packages needed by this project.

After pushing the updated workflow, open **Actions → Build Kabox Android APK**. If the run fails again, open **Build Kabox debug APK → Build debug APK** and inspect the first Gradle `FAILURE` / `What went wrong` lines. A failure after SDK setup is a different issue and should be diagnosed from those exact lines.

## What this workflow verifies

The workflow installs JDK 17, the Android 37 platform and Android build-tools 36.0.0, then runs `clean assembleDebug`. On success it uploads `Kabox-debug-apk` as a workflow artifact. This produces a debug APK for device testing; it is not a signed Play Store release.
