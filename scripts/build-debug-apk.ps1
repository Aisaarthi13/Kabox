$ErrorActionPreference = "Stop"
if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    throw "Gradle 9.6.0 is required. Install Gradle 9.6.0 and JDK 17 first."
}
gradle --no-daemon assembleDebug
if ($LASTEXITCODE -ne 0) { throw "APK build failed" }
Write-Host "APK: app/build/outputs/apk/debug/app-debug.apk"
