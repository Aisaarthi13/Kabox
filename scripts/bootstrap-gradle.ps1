param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Properties = Join-Path $Root "gradle\wrapper\gradle-wrapper.properties"
$Line = Get-Content $Properties | Where-Object { $_ -like "distributionUrl=*" } | Select-Object -First 1
$Url = ($Line -replace "^distributionUrl=", "") -replace "\\:", ":"
$FileName = [System.IO.Path]::GetFileName($Url)
if ($FileName -notmatch '^gradle-([0-9.]+)-bin\.zip$') { throw "Could not read Gradle distribution version" }
$Version = $Matches[1]
$CacheRoot = Join-Path $env:USERPROFILE ".gradle\kabox-bootstrap"
$Distribution = Join-Path $CacheRoot "gradle-$Version"
$Gradle = Join-Path $Distribution "bin\gradle.bat"
if (-not (Test-Path $Gradle)) {
    New-Item -ItemType Directory -Force -Path $CacheRoot | Out-Null
    $Archive = Join-Path $CacheRoot "gradle-$Version-bin.zip"
    Write-Host "Downloading Gradle $Version on first run..."
    Invoke-WebRequest -Uri $Url -OutFile $Archive
    Expand-Archive -Path $Archive -DestinationPath $CacheRoot -Force
    Remove-Item $Archive -Force
}
Push-Location $Root
try {
    & $Gradle @GradleArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
