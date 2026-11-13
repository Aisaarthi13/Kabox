# Permissions and privacy

## Usage Access

Kabox requests Android Usage Access only to show device screen-time summaries and enforce optional user-set daily limits. The user enables or disables this special access in Android Settings. The app does not upload usage history.

## Accessibility Service

The optional blocker uses Android Accessibility Service to read the foreground package name from window-state events. It does not request access to window content, text, passwords, messages or form fields. When the package belongs to an app selected by the user and a focus/all-day/daily-limit rule applies, Kabox sends the user to its interruption screen. No accessibility event data is transmitted to a server.

The app shows an in-app disclosure and requires the user to continue to Android Accessibility settings to enable the service. The user may turn the service off at any time.

## Notifications

The daily reminder creates a local notification. Android 13 and newer may require runtime notification permission.

## Permissions intentionally not requested

This build does not request device administrator, uninstall protection, overlay, VPN, contacts, microphone, camera or location permissions.
