# Build configuration decision

The failed run requested `platforms;android-37` and stopped with `Failed to find package 'platforms;android-37'`. This project pins the stable Android 16 API 36 platform instead.

The version set is intentionally conservative and aligned with the official compatibility tables:

- AGP 8.10.0 supports compile SDK API 36.
- AGP 8.10 uses Gradle 8.11.1 and JDK 17.
- Kotlin Gradle Plugin 2.2.10 supports AGP up to 8.10.0 and Gradle 8.14, so Gradle 8.11.1 is within its supported range.
- Build Tools 35.0.0 is the AGP 8.10 default; no experimental API 37 packages are requested.

The CI workflow checks for the SDK directories before running Gradle and checks that `app-debug.apk` exists before uploading it.
