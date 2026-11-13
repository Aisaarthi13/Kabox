# Architecture — Kabox 0.1

## Shape
- UI: Jetpack Compose + Material 3, single-activity navigation state.
- Local persistence: `LocalStore` backed by Android SharedPreferences.
- Timer clock: `SystemClock.elapsedRealtime()` deadline, with wall-clock fallback for reboot recovery.
- Usage data: `UsageStatsManager`, accessed only after checking `OPSTR_GET_USAGE_STATS`.
- Backend: none in v0.1; all focus and quest records remain local.

## Why local-first
It avoids requiring a server account for the basic focus workflow and avoids transmitting personal usage data. Multiplayer and cloud sync will be separate opt-in modules.

## Next architecture milestones
1. Move focus sessions and quests to Room with explicit schema migrations.
2. Add a foreground timer service and user-visible completion notification, following current Android background execution limits.
3. Add an optional accessibility-based app shield with prominent disclosure, explicit consent, a visible off switch, and a clear explanation of limitations.
4. Add a user-controlled domain-blocking VpnService only after a privacy/security review.
5. Add Firebase only for opt-in accounts, rooms, sync, and server-verified points.
