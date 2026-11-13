package com.kabox.app.data

import android.content.Context
import android.os.SystemClock
import java.time.LocalDate

/** Local-first persistence for Kabox's v0.1 focus and daily quest features. */
class LocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kabox_local", Context.MODE_PRIVATE)

    fun selectedMinutes(): Int = prefs.getInt(KEY_DURATION, 25)
    fun setSelectedMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DURATION, minutes.coerceIn(1, 180)).apply()
    }

    fun isTimerRunning(): Boolean = prefs.getBoolean(KEY_RUNNING, false)

    fun remainingSeconds(): Long {
        if (!isTimerRunning()) return prefs.getLong(KEY_REMAINING, selectedMinutes() * 60L).coerceAtLeast(0L)
        val elapsedDeadline = prefs.getLong(KEY_END_ELAPSED, 0L)
        val elapsedRemaining = elapsedDeadline - SystemClock.elapsedRealtime()
        val remainingMs = if (elapsedRemaining >= 0L) {
            elapsedRemaining
        } else {
            // Fallback for a device reboot, when elapsedRealtime starts from zero again.
            prefs.getLong(KEY_END_WALL, 0L) - System.currentTimeMillis()
        }
        return ((remainingMs.coerceAtLeast(0L) + 999L) / 1000L)
    }

    fun startNew(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 180)
        val seconds = safeMinutes * 60L
        prefs.edit()
            .putInt(KEY_DURATION, safeMinutes)
            .putLong(KEY_REMAINING, seconds)
            .putLong(KEY_SESSION_LENGTH, seconds)
            .putLong(KEY_END_ELAPSED, SystemClock.elapsedRealtime() + seconds * 1000L)
            .putLong(KEY_END_WALL, System.currentTimeMillis() + seconds * 1000L)
            .putBoolean(KEY_RUNNING, true)
            .apply()
    }

    fun resume() {
        val seconds = remainingSeconds()
        if (seconds <= 0L) return
        prefs.edit()
            .putLong(KEY_END_ELAPSED, SystemClock.elapsedRealtime() + seconds * 1000L)
            .putLong(KEY_END_WALL, System.currentTimeMillis() + seconds * 1000L)
            .putBoolean(KEY_RUNNING, true)
            .apply()
    }

    fun pause() {
        val seconds = remainingSeconds()
        prefs.edit()
            .putLong(KEY_REMAINING, seconds)
            .putBoolean(KEY_RUNNING, false)
            .apply()
    }

    fun resetTimer() {
        prefs.edit()
            .putLong(KEY_REMAINING, selectedMinutes() * 60L)
            .putBoolean(KEY_RUNNING, false)
            .putLong(KEY_END_ELAPSED, 0L)
            .putLong(KEY_END_WALL, 0L)
            .apply()
    }

    /** Returns true exactly once when an active session reaches zero. */
    fun finishIfDue(): Boolean {
        if (!isTimerRunning() || remainingSeconds() > 0L) return false
        val sessionSeconds = prefs.getLong(KEY_SESSION_LENGTH, selectedMinutes() * 60L)
        prefs.edit()
            .putBoolean(KEY_RUNNING, false)
            .putLong(KEY_REMAINING, 0L)
            .putLong(KEY_END_ELAPSED, 0L)
            .putLong(KEY_END_WALL, 0L)
            .apply()
        addCompletedSession(sessionSeconds)
        return true
    }

    private fun addCompletedSession(seconds: Long) {
        val today = LocalDate.now().toString()
        val oldDate = prefs.getString(KEY_STUDY_DAY, "") ?: ""
        val dailySeconds = if (oldDate == today) prefs.getLong(KEY_DAILY_SECONDS, 0L) else 0L
        val previousTotal = prefs.getLong(KEY_TOTAL_SECONDS, 0L)
        val previousSessions = prefs.getInt(KEY_SESSIONS, 0)
        val lastActive = prefs.getString(KEY_LAST_ACTIVE, "") ?: ""
        val oldStreak = prefs.getInt(KEY_STREAK, 0)
        val yesterday = LocalDate.now().minusDays(1).toString()
        val newStreak = when (lastActive) {
            today -> oldStreak.coerceAtLeast(1)
            yesterday -> oldStreak + 1
            else -> 1
        }
        prefs.edit()
            .putString(KEY_STUDY_DAY, today)
            .putLong(KEY_DAILY_SECONDS, dailySeconds + seconds)
            .putLong(KEY_TOTAL_SECONDS, previousTotal + seconds)
            .putInt(KEY_SESSIONS, previousSessions + 1)
            .putString(KEY_LAST_ACTIVE, today)
            .putInt(KEY_STREAK, newStreak)
            .apply()
    }

    fun dailyFocusMinutes(): Int {
        val today = LocalDate.now().toString()
        if ((prefs.getString(KEY_STUDY_DAY, "") ?: "") != today) return 0
        return (prefs.getLong(KEY_DAILY_SECONDS, 0L) / 60L).toInt()
    }

    fun totalFocusMinutes(): Int = (prefs.getLong(KEY_TOTAL_SECONDS, 0L) / 60L).toInt()
    fun completedSessions(): Int = prefs.getInt(KEY_SESSIONS, 0)
    fun streakDays(): Int = if ((prefs.getString(KEY_LAST_ACTIVE, "") ?: "") == LocalDate.now().toString() || (prefs.getString(KEY_LAST_ACTIVE, "") ?: "") == LocalDate.now().minusDays(1).toString()) prefs.getInt(KEY_STREAK, 0) else 0

    fun isQuestDone(index: Int): Boolean = prefs.getBoolean("quest_$index", false)
    fun setQuestDone(index: Int, done: Boolean) {
        prefs.edit().putBoolean("quest_$index", done).apply()
    }

    companion object {
        private const val KEY_DURATION = "timer_duration"
        private const val KEY_RUNNING = "timer_running"
        private const val KEY_REMAINING = "timer_remaining"
        private const val KEY_END_ELAPSED = "timer_end_elapsed"
        private const val KEY_END_WALL = "timer_end_wall"
        private const val KEY_SESSION_LENGTH = "timer_session_length"
        private const val KEY_STUDY_DAY = "study_day"
        private const val KEY_DAILY_SECONDS = "daily_seconds"
        private const val KEY_TOTAL_SECONDS = "total_seconds"
        private const val KEY_SESSIONS = "completed_sessions"
        private const val KEY_STREAK = "streak_days"
        private const val KEY_LAST_ACTIVE = "last_active_day"
    }
}
