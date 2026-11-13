@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\bootstrap-gradle.ps1" %*
exit /b %errorlevel%
