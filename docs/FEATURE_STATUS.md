# Kabox 0.3.0 feature status

| Feature | Status in this build | Notes |
|---|---|---|
| Green dashboard / polished navigation | Implemented | Original Kabox UI and components |
| Focus countdown | Implemented | 25/45/60/90-minute presets |
| Pomodoro | Implemented | 25/5; 15-minute break after four focus cycles |
| Stopwatch | Implemented | Pause, resume and save |
| Persistent focus records | Implemented | Local SharedPreferences storage |
| Daily goal / streak / weekly chart | Implemented | Local session data |
| Screen-time analytics | Implemented with permission | Uses Android UsageStatsManager |
| App blocker | Implemented with permission | User-selected apps; Accessibility Service required |
| Per-app daily cap | Implemented with Usage Access | Enforcement occurs when target app comes to foreground |
| Daily reminder | Implemented with notification permission | Inexact repeating alarm |
| Selective Reels / Shorts | Not implemented | Whole-app blocking is available as a practical interim option |
| Website / browser filtering | Not implemented | Needs a separately tested VPN/DNS component |
| Multiplayer / leaderboard | Not implemented | Requires backend and account configuration |
| Ambient audio | Not implemented | Planned |
| Google Play Billing | Not implemented | ₹39/month shown as a planned price only |

## Important limitations

- Android Usage Access and Accessibility are special permissions that users must enable themselves.
- OEM battery management may affect reminder delivery.
- Accessibility app blocking is not tamper-proof. Users can disable the service or uninstall the app; Kabox must not claim otherwise.
- Selective blocking of content inside third-party apps depends on those apps' UI structure and would require ongoing compatibility testing.
