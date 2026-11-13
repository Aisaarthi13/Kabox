# Permissions and user trust

Kabox 0.1 declares the special Android Usage Access capability. The user must enable it manually in Android Settings. Without it, the screen-time ledger stays empty and the rest of the app continues to work.

This build does **not** declare or request:
- Accessibility Service access
- VPN permission
- Display-over-other-apps permission
- Notification permission
- Contacts, location, camera, microphone, or storage access

Future blockers must have a prominent in-app explanation, affirmative user consent, transparent data handling, a working disable path, and policy review before release. Kabox must not claim it can make itself impossible to uninstall or prevent users from revoking permissions.
