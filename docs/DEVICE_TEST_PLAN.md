# Kabox 0.3.0 device test checklist

Run this checklist after installing the Actions artifact on the target Android phone.

1. Launch Kabox and verify the Home, Focus, Block and Stats tabs open without crashes.
2. In Focus → Timer, start a session, pause it, leave Kabox, reopen it and verify the remaining time is retained.
3. Start a Pomodoro focus cycle. Verify the session is counted when the focus countdown ends, the app switches to a 5-minute break, and the app-blocker pauses are suspended during the break.
4. Start Stopwatch, pause, resume and save. Verify whole elapsed minutes are added to today's focus total.
5. In Stats, grant Usage Access and verify total screen time and top apps appear. Revoke the access and verify the app shows the permission prompt instead of crashing.
6. In Block, read the disclosure, enable Kabox in Android Accessibility settings, select a test app and enable Block distractions in Focus. Launch the selected app during a focus session and verify Kabox opens the interruption screen.
7. On the interruption screen, test Return to Kabox and Allow 5 minutes. Verify the five-minute snooze applies only to that selected app.
8. Set a daily app limit and verify it is enforced once Android usage data reaches the selected threshold.
9. Set a daily reminder, allow notifications, and verify that the local notification appears at the selected time (Android may deliver inexact alarms a little later).
10. Check portrait and landscape layouts, font scaling, back navigation, app restart and process recreation.

Record phone model, Android version, build commit and any failing step before enabling more advanced blocking modules or preparing a Play Store release.
