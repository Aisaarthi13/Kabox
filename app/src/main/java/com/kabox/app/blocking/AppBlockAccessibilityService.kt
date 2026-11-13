package com.kabox.app.blocking

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

/**
 * Optional, user-enabled app blocker.
 * It observes only the foreground package name; it does not retrieve window content.
 */
class AppBlockAccessibilityService : AccessibilityService() {
    private var lastInterruptionAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage == packageName || targetPackage == "android") return

        val prefs = getSharedPreferences("kabox_local", Context.MODE_PRIVATE)
        val selected = prefs.getStringSet("blocked_apps", emptySet())?.contains(targetPackage) == true
        val focusActive = prefs.getBoolean("timer_running", false) || prefs.getLong("stopwatch_started_at", 0L) > 0L
        val isPomodoroBreak = prefs.getBoolean("timer_is_break", false)
        val alwaysOn = prefs.getBoolean("blocker_always_on", false)
        val blockDuringFocus = prefs.getBoolean("block_during_focus", true)
        val snoozeUntil = prefs.getLong("snooze_until_$targetPackage", 0L)

        if (snoozeUntil > System.currentTimeMillis()) return
        val selectedAppShouldBlock = selected && (alwaysOn || (focusActive && blockDuringFocus && !isPomodoroBreak))
        val dailyLimitShouldBlock = hasExceededDailyLimit(targetPackage, prefs)
        if (!selectedAppShouldBlock && !dailyLimitShouldBlock) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastInterruptionAt < 1800L) return
        lastInterruptionAt = now

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPackage, 0)).toString()
        } catch (_: Exception) { "This app" }

        performGlobalAction(GLOBAL_ACTION_HOME)
        val interruption = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockedActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, targetPackage)
        }
        try { startActivity(interruption) } catch (_: Exception) { /* Android may defer background activity launches. */ }
    }

    private fun hasExceededDailyLimit(targetPackage: String, prefs: android.content.SharedPreferences): Boolean {
        if (!prefs.getBoolean("limit_enabled_$targetPackage", false)) return false
        val minutes = prefs.getInt("limit_minutes_$targetPackage", 60).coerceAtLeast(1)
        return try {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val used = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis())
                ?.filter { it.packageName == targetPackage }
                ?.sumOf { it.totalTimeInForeground } ?: 0L
            used >= minutes * 60_000L
        } catch (_: Exception) { false }
    }

    override fun onInterrupt() = Unit
}
