package com.kabox.app

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 14, 25)
    private val panel = Color.rgb(19, 27, 43)
    private val panel2 = Color.rgb(24, 34, 52)
    private val gold = Color.rgb(217, 184, 120)
    private val ivory = Color.rgb(242, 237, 225)
    private val muted = Color.rgb(164, 171, 184)
    private val green = Color.rgb(134, 181, 147)
    private val prefs by lazy { getSharedPreferences("kabox_local", Context.MODE_PRIVATE) }

    private lateinit var root: LinearLayout
    private var currentTab = "home"
    private var selectedMinutes = 25
    private var remainingMillis = 25L * 60_000L
    private var finishAtMillis = 0L
    private var timerRunning = false
    private var ticker: CountDownTimer? = null
    private var timerLabel: TextView? = null
    private var timerStatus: TextView? = null
    private var timerProgress: ProgressBar? = null
    private var built = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        restoreTimerState()
        render()
        built = true
    }

    override fun onResume() {
        super.onResume()
        if (built) render()
    }

    override fun onPause() {
        saveTimerState()
        super.onPause()
    }

    override fun onDestroy() {
        ticker?.cancel()
        ticker = null
        super.onDestroy()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        val mark = TextView(this).apply {
            text = "✦"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = rounded(gold, 16)
        }
        header.addView(mark, LinearLayout.LayoutParams(dp(46), dp(46)))
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(label("KABOX", 16f, gold, true))
        brand.addView(label("THE ACADEMY OF FOCUS", 9f, muted, false))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("✧  YOUR SPACE", 10f, muted, true))
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        when (currentTab) {
            "focus" -> buildFocus(content)
            "insights" -> buildInsights(content)
            else -> buildHome(content)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildNavigation())
        setContentView(root)
        updateTimerViews()
    }

    private fun buildHome(parent: LinearLayout) {
        parent.addView(label("MAKE ROOM FOR\nYOUR NEXT CHAPTER.", 29f, ivory, true).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(8))
        })
        parent.addView(label("Small, focused sessions. A stronger mind, one day at a time.", 13f, muted, false))
        spacer(parent, 18)

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard("${todayFocusMinutes()}", "FOCUS MINUTES", "today"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(spaceWidth(10))
        stats.addView(statCard("${todaySessions()}", "SESSIONS", "completed"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(spaceWidth(10))
        stats.addView(statCard("${housePoints()}", "HOUSE POINTS", "earned"), LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(stats)
        spacer(parent, 16)

        val goalCard = card()
        goalCard.addView(label("TODAY'S INTENTION", 10f, gold, true))
        spacer(goalCard, 8)
        goalCard.addView(label("Give your attention somewhere meaningful.", 18f, ivory, true).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
        })
        spacer(goalCard, 6)
        goalCard.addView(label("Daily focus goal · 2 hours", 12f, muted, false))
        spacer(goalCard, 12)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = min(1000, todayFocusMinutes() * 1000 / 120)
            progressTintList = android.content.res.ColorStateList.valueOf(gold)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(48, 56, 70))
        }
        goalCard.addView(progress, LinearLayout.LayoutParams(-1, dp(7)))
        spacer(goalCard, 8)
        goalCard.addView(label("${todayFocusMinutes()} / 120 minutes completed", 11f, muted, false))
        spacer(goalCard, 14)
        goalCard.addView(actionButton("BEGIN A FOCUS SESSION  →", true) { currentTab = "focus"; render() })
        parent.addView(goalCard)
        spacer(parent, 16)

        val quest = card()
        quest.addView(label("DAILY QUEST", 10f, gold, true))
        spacer(quest, 8)
        val done = todaySessions() > 0
        quest.addView(label(if (done) "✓  First focus session complete" else "◌  Complete your first focus session", 15f, ivory, true))
        spacer(quest, 4)
        quest.addView(label(if (done) "Your first focus session is on the board." else "One completed session earns 25 house points.", 12f, muted, false))
        parent.addView(quest)
        spacer(parent, 16)
        parent.addView(label("THE ACADEMY WING", 10f, gold, true))
        spacer(parent, 8)
        parent.addView(featureRow("⌛", "Focus chamber", "Timer and personal focus sessions", "AVAILABLE"))
        spacer(parent, 8)
        parent.addView(featureRow("◈", "Usage observatory", "See today's device usage", "AVAILABLE"))
        spacer(parent, 8)
        parent.addView(featureRow("✧", "Distraction wards", "App and website blocking", "IN DEVELOPMENT"))
        spacer(parent, 8)
        parent.addView(featureRow("♜", "Study commons", "Shared study rooms and House Cup", "IN DEVELOPMENT"))
        spacer(parent, 12)
        parent.addView(label("Kabox keeps your focus records on this device. No account required.", 11f, muted, false))
        spacer(parent, 18)
    }

    private fun buildFocus(parent: LinearLayout) {
        parent.addView(label("THE FOCUS CHAMBER", 11f, gold, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        parent.addView(label("One thing at a time.", 28f, ivory, true).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
        })
        parent.addView(label("Choose a session length and settle into your work.", 13f, muted, false))
        spacer(parent, 18)

        val timerCard = card().apply { gravity = Gravity.CENTER_HORIZONTAL }
        timerCard.addView(label("✧  YOUR HOURGLASS  ✧", 10f, gold, true))
        spacer(timerCard, 20)
        timerLabel = label(formatClock(remainingMillis), 56f, ivory, true).apply {
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        timerCard.addView(timerLabel)
        spacer(timerCard, 8)
        timerStatus = label(if (timerRunning) "YOUR SESSION IS IN PROGRESS" else "READY WHEN YOU ARE", 10f, muted, true)
        timerCard.addView(timerStatus)
        spacer(timerCard, 22)
        timerProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(gold)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(48, 56, 70))
        }
        timerCard.addView(timerProgress, LinearLayout.LayoutParams(-1, dp(7)))
        spacer(timerCard, 18)

        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(25, 50, 90).forEachIndexed { index, minutes ->
            val mode = actionButton("${minutes}m", minutes == selectedMinutes) {
                if (!timerRunning) {
                    selectedMinutes = minutes
                    remainingMillis = minutes * 60_000L
                    saveTimerState()
                    render()
                }
            }
            modeRow.addView(mode, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                if (index != 2) marginEnd = dp(8)
            })
        }
        timerCard.addView(modeRow)
        spacer(timerCard, 12)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mainAction = actionButton(if (timerRunning) "PAUSE SESSION" else "START FOCUS", true) {
            if (timerRunning) pauseTimer() else startTimer()
        }
        controls.addView(mainAction, LinearLayout.LayoutParams(0, dp(50), 1f))
        controls.addView(spaceWidth(8))
        controls.addView(actionButton("RESET", false) { resetTimer() }, LinearLayout.LayoutParams(dp(92), dp(50)))
        timerCard.addView(controls)
        parent.addView(timerCard)
        spacer(parent, 14)

        val note = card()
        note.addView(label("A QUIET REMINDER", 10f, gold, true))
        spacer(note, 6)
        note.addView(label("Keep Kabox open or return when your session ends. Your timer is saved if the app is closed.", 12f, muted, false))
        parent.addView(note)
        spacer(parent, 20)
    }

    private fun buildInsights(parent: LinearLayout) {
        parent.addView(label("YOUR OBSERVATORY", 11f, gold, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        parent.addView(label("Notice where your\ntime is going.", 29f, ivory, true).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
        })
        parent.addView(label("Kabox reads Android's Usage Access report. Your usage data stays on this device.", 13f, muted, false))
        spacer(parent, 18)
        val usageCard = card()
        usageCard.addView(label("SCREEN TIME · TODAY", 10f, gold, true))
        spacer(usageCard, 10)
        if (hasUsageAccess()) {
            usageCard.addView(label(formatDuration(todayDeviceUsageMillis()), 34f, ivory, true).apply {
                typeface = Typeface.create("serif", Typeface.BOLD)
            })
            spacer(usageCard, 4)
            usageCard.addView(label("Total foreground app usage reported by Android.", 12f, muted, false))
        } else {
            usageCard.addView(label("Usage access is not enabled yet.", 17f, ivory, true))
            spacer(usageCard, 6)
            usageCard.addView(label("Allow Kabox in Android settings to read today's device usage total.", 12f, muted, false))
            spacer(usageCard, 12)
            usageCard.addView(actionButton("OPEN USAGE ACCESS SETTINGS", true) { openUsageSettings() })
        }
        parent.addView(usageCard)
        spacer(parent, 14)

        val focusCard = card()
        focusCard.addView(label("YOUR FOCUS RECORD", 10f, gold, true))
        spacer(focusCard, 10)
        focusCard.addView(insightLine("Focus minutes today", "${todayFocusMinutes()} min"))
        spacer(focusCard, 10)
        focusCard.addView(insightLine("Sessions completed today", "${todaySessions()}"))
        spacer(focusCard, 10)
        focusCard.addView(insightLine("House points earned", "${housePoints()}"))
        parent.addView(focusCard)
        spacer(parent, 14)

        val privacy = card()
        privacy.addView(label("PRIVACY BY DESIGN", 10f, gold, true))
        spacer(privacy, 6)
        privacy.addView(label("Focus totals and points are stored locally with Android SharedPreferences. Kabox does not send this data to a server in this version.", 12f, muted, false))
        parent.addView(privacy)
        spacer(parent, 20)
    }

    private fun buildNavigation(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            background = rounded(panel, 18)
        }
        addNavItem(nav, "✦", "HOME", "home")
        addNavItem(nav, "⌛", "FOCUS", "focus")
        addNavItem(nav, "◈", "INSIGHTS", "insights")
        return nav
    }

    private fun addNavItem(nav: LinearLayout, icon: String, title: String, tab: String) {
        val active = currentTab == tab
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { currentTab = tab; render() }
            isClickable = true
            isFocusable = true
        }
        item.addView(label(icon, 18f, if (active) gold else muted, true))
        item.addView(label(title, 9f, if (active) gold else muted, true))
        nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun statCard(value: String, title: String, subtitle: String): LinearLayout {
        val box = card().apply { setPadding(dp(11), dp(14), dp(11), dp(14)) }
        box.addView(label(value, 24f, ivory, true).apply { typeface = Typeface.create("serif", Typeface.BOLD) })
        spacer(box, 4)
        box.addView(label(title, 8f, gold, true))
        box.addView(label(subtitle, 9f, muted, false))
        return box
    }

    private fun featureRow(icon: String, title: String, description: String, status: String): LinearLayout {
        val row = card().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        row.addView(label(icon, 23f, gold, true), LinearLayout.LayoutParams(dp(38), -2))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label(title, 14f, ivory, true))
        copy.addView(label(description, 10f, muted, false))
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(status, 8f, if (status == "AVAILABLE") green else muted, true))
        return row
    }

    private fun insightLine(title: String, value: String): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(label(title, 12f, muted, false), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(value, 13f, ivory, true))
        return row
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(17), dp(17), dp(17))
        background = rounded(panel, 20, Color.rgb(43, 52, 68))
    }

    private fun actionButton(text: String, primary: Boolean, click: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(if (primary) bg else gold)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(if (primary) gold else panel2, 14, if (primary) gold else Color.rgb(82, 74, 60))
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            includeFontPadding = true
        }
    }

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun spacer(parent: LinearLayout, heightDp: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(heightDp)))
    }

    private fun spaceWidth(widthDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), 1)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun startTimer() {
        if (timerRunning) return
        if (remainingMillis <= 0L) remainingMillis = selectedMinutes * 60_000L
        timerRunning = true
        finishAtMillis = System.currentTimeMillis() + remainingMillis
        saveTimerState()
        startTicker()
        updateTimerViews()
    }

    private fun pauseTimer() {
        if (!timerRunning) return
        val end = finishAtMillis
        val left = max(0L, end - System.currentTimeMillis())
        if (left == 0L) {
            completeFocusSession()
            return
        }
        remainingMillis = left
        timerRunning = false
        finishAtMillis = 0L
        ticker?.cancel()
        ticker = null
        saveTimerState()
        render()
    }

    private fun resetTimer() {
        ticker?.cancel()
        ticker = null
        timerRunning = false
        finishAtMillis = 0L
        remainingMillis = selectedMinutes * 60_000L
        saveTimerState()
        render()
    }

    private fun startTicker() {
        ticker?.cancel()
        val duration = max(1_000L, finishAtMillis - System.currentTimeMillis())
        ticker = object : CountDownTimer(duration, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = max(0L, finishAtMillis - System.currentTimeMillis())
                updateTimerViews()
            }

            override fun onFinish() {
                remainingMillis = 0L
                completeFocusSession()
            }
        }.start()
    }

    private fun completeFocusSession() {
        if (!timerRunning) return
        ticker?.cancel()
        ticker = null
        timerRunning = false
        finishAtMillis = 0L
        remainingMillis = 0L
        val key = todayKey()
        val newMinutes = todayFocusMinutes() + selectedMinutes
        val newSessions = todaySessions() + 1
        val newPoints = housePoints() + 25
        prefs.edit()
            .putBoolean("timer_running", false)
            .putLong("timer_finish_at", 0L)
            .putLong("timer_remaining", 0L)
            .putInt("timer_selected_minutes", selectedMinutes)
            .putInt("focus_minutes_$key", newMinutes)
            .putInt("focus_sessions_$key", newSessions)
            .putInt("house_points", newPoints)
            .apply()
        Toast.makeText(this, "Focus session complete · +25 house points", Toast.LENGTH_LONG).show()
        if (built) render()
    }

    private fun saveTimerState() {
        prefs.edit()
            .putInt("timer_selected_minutes", selectedMinutes)
            .putLong("timer_remaining", remainingMillis)
            .putLong("timer_finish_at", if (timerRunning) finishAtMillis else 0L)
            .putBoolean("timer_running", timerRunning)
            .apply()
    }

    private fun restoreTimerState() {
        selectedMinutes = prefs.getInt("timer_selected_minutes", 25).coerceIn(1, 180)
        timerRunning = prefs.getBoolean("timer_running", false)
        if (timerRunning) {
            finishAtMillis = prefs.getLong("timer_finish_at", 0L)
            remainingMillis = max(0L, finishAtMillis - System.currentTimeMillis())
            if (finishAtMillis <= System.currentTimeMillis()) {
                completeExpiredSessionOnRestore()
            } else {
                startTicker()
            }
        } else {
            finishAtMillis = 0L
            remainingMillis = prefs.getLong("timer_remaining", selectedMinutes * 60_000L)
                .coerceIn(0L, selectedMinutes * 60_000L)
        }
    }

    private fun completeExpiredSessionOnRestore() {
        timerRunning = true
        ticker?.cancel()
        timerRunning = false
        remainingMillis = 0L
        finishAtMillis = 0L
        val key = todayKey()
        prefs.edit()
            .putBoolean("timer_running", false)
            .putLong("timer_finish_at", 0L)
            .putLong("timer_remaining", 0L)
            .putInt("focus_minutes_$key", todayFocusMinutes() + selectedMinutes)
            .putInt("focus_sessions_$key", todaySessions() + 1)
            .putInt("house_points", housePoints() + 25)
            .apply()
    }

    private fun updateTimerViews() {
        timerLabel?.text = formatClock(remainingMillis)
        timerStatus?.text = if (timerRunning) "YOUR SESSION IS IN PROGRESS" else if (remainingMillis == 0L) "SESSION COMPLETE · WELL DONE" else "READY WHEN YOU ARE"
        val total = selectedMinutes * 60_000L
        val progress = if (total <= 0L) 0 else (((total - remainingMillis).toDouble() / total) * 1000).toInt().coerceIn(0, 1000)
        timerProgress?.progress = progress
    }

    private fun formatClock(millis: Long): String {
        val seconds = max(0L, millis) / 1000L
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun todayFocusMinutes(): Int = prefs.getInt("focus_minutes_${todayKey()}", 0)
    private fun todaySessions(): Int = prefs.getInt("focus_sessions_${todayKey()}", 0)
    private fun housePoints(): Int = prefs.getInt("house_points", 0)

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun openUsageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Open Android Settings → Special app access → Usage access.", Toast.LENGTH_LONG).show()
        }
    }

    private fun todayDeviceUsageMillis(): Long {
        if (!hasUsageAccess()) return 0L
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val results = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())
        return results?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
