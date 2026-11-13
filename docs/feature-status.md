# Kabox feature status

| Feature | Status in 0.1.0 | Notes |
|---|---|---|
| Magical-academy UI | Implemented | Original branding and visual assets only. |
| Focus timer | Implemented | Local timer with start/pause/resume/reset; restores remaining time. |
| Session history totals | Implemented | Local counters for completed sessions and focus minutes. |
| Daily streak | Implemented | Derived from days with completed focus sessions. |
| Daily quests | Implemented | Three local checkable tasks. |
| Screen-time report | Implemented with permission | Uses Android UsageStats after manual Usage Access grant. |
| App blocking | Planned | Requires AccessibilityService and extensive device testing. |
| Website blocker | Planned | Requires VpnService and an explicit user-controlled VPN connection. |
| Reels/Shorts detection | Not promised | Reliable channel-level blocking is not guaranteed. |
| Study rooms | Planned | Requires identity, real-time backend, room lifecycle, and abuse controls. |
| House Cup / leaderboard | Planned | Requires verified session rules and server-side anti-cheat. |
| Ambient music | Planned | Needs licensed/royalty-safe audio and playback service. |
| Timer finish notification | Not implemented | Current timer state recovers on reopen; no background alarm is claimed. |

## Acceptance checks for v0.1
1. Fresh install opens to Home without asking for special permissions.
2. Focus timer starts, pauses, resumes, and resets without negative time values.
3. Force-stop/reopen restores the timer's remaining time.
4. Completing a session increments the session count and focus totals once.
5. Quest checkboxes retain their state after relaunch.
6. Usage Access screen opens Android settings and reports app usage only after the user grants access.
