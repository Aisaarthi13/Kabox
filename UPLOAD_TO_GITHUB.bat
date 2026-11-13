@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\publish-to-github.ps1"
if errorlevel 1 (
  echo.
  echo GitHub upload did not finish. Read the message above.
  pause
  exit /b 1
)
echo.
echo Kabox has been pushed to GitHub.
pause
