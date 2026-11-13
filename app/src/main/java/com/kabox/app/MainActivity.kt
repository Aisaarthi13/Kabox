package com.kabox.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.TimePickerDialog
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Kabox 0.3.0 — local-first focus and screen-time companion.
 * All focus records and blocker preferences are stored on-device.
 */
class MainActivity : Activity() {
    private val page = Color.rgb(245, 248, 245)
    private val surface = Color.WHITE
    private val green = Color.rgb(18, 122, 82)
    private val greenDark = Color.rgb(11, 77, 54)
    private val greenSoft = Color.rgb(226, 245, 233)
    private val lime = Color.rgb(184, 241, 202)
    private val ink = Color.rgb(25, 39, 31)
    private val muted = Color.rgb(112, 128, 119)
    private val border = Color.rgb(226, 235, 228)
    private val orange = Color.rgb(226, 145, 82)

    private val prefs by lazy { getSharedPreferences("kabox_local", Context.MODE_PRIVATE) }
    private lateinit var shell: LinearLayout
    private var currentTab = "home"

    // Countdown and Pomodoro state.
    private var mode = "timer" // timer, pomodoro, stopwatch
    private var selectedMinutes = 25
    private var remainingMillis = 25L * 60_000L
    private var intervalDurationMillis = 25L * 60_000L
    private var finishAtMillis = 0L
    private var timerRunning = false
    private var isBreak = false
    private var pomodoroCycles = 0
    private var stopwatchAccumulated = 0L
    private var stopwatchStartedAt = 0L
    private var tickerStarted = false

    private var timerValue: TextView? = null
    private var timerCaption: TextView? = null
    private var timerRing: RingProgressView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateTimerWidgets()
            if (timerRunning || (mode == "stopwatch" && stopwatchStartedAt > 0L)) {
                handler.postDelayed(this, 250L)
            } else {
                tickerStarted = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = surface
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        restoreTimerState()
        render()
    }

    override fun onResume() {
        super.onResume()
        reconcileTimerAfterResume()
        ensureTicker()
        if (::shell.isInitialized) render()
    }

    override fun onPause() {
        saveTimerState()
        handler.removeCallbacks(tick)
        tickerStarted = false
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        tickerStarted = false
        super.onDestroy()
    }

    // ---------------------------- App shell ----------------------------

    private fun render() {
        timerValue = null
        timerCaption = null
        timerRing = null

        shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        shell.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(66)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(0, 0, 0, dp(12))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(22))
        }
        when (currentTab) {
            "focus" -> buildFocus(content)
            "block" -> buildBlock(content)
            "stats" -> buildStats(content)
            "profile" -> buildProfile(content)
            else -> buildHome(content)
        }
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        shell.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, dp(68)))
        setContentView(shell)
        shell.alpha = 0f
        shell.animate().alpha(1f).setDuration(180L).start()
        updateTimerWidgets()
        ensureTicker()
    }

    private fun buildHeader(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(7), dp(18), dp(5))
            setBackgroundColor(page)
        }
        val logo = TextView(this).apply {
            text = "K"
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(green, 15)
        }
        bar.addView(logo, LinearLayout.LayoutParams(dp(42), dp(42)))
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        brand.addView(text("Kabox", 19f, ink, true))
        brand.addView(text("FOCUS, WITH INTENTION", 8.5f, muted, true))
        bar.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val profile = TextView(this).apply {
            text = "•••"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(greenDark)
            background = rounded(greenSoft, 22)
            setOnClickListener { currentTab = "profile"; render() }
        }
        bar.addView(profile, LinearLayout.LayoutParams(dp(42), dp(42)))
        return bar
    }

    private fun buildBottomNav(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(7))
            setBackgroundColor(surface)
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadii = floatArrayOf(dp(18).toFloat(), dp(18).toFloat(), dp(18).toFloat(), dp(18).toFloat(), 0f, 0f, 0f, 0f)
                setStroke(dp(1), border)
            }
        }
        addNavItem(nav, "⌂", "Home", "home")
        addNavItem(nav, "◷", "Focus", "focus")
        addNavItem(nav, "▣", "Block", "block")
        addNavItem(nav, "▥", "Stats", "stats")
        return nav
    }

    private fun addNavItem(nav: LinearLayout, icon: String, title: String, tab: String) {
        val active = currentTab == tab
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener { currentTab = tab; render() }
        }
        val iconView = TextView(this).apply {
            text = icon
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(if (active) green else muted)
        }
        item.addView(iconView)
        item.addView(text(title, 10f, if (active) green else muted, active))
        nav.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }

    // ---------------------------- Home ----------------------------

    private fun buildHome(parent: LinearLayout) {
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        parent.addView(text(greeting, 13f, muted, true))
        parent.addView(text("Make today\ncount.", 34f, ink, true).apply {
            setLineSpacing(dp(1).toFloat(), 1f)
            setPadding(0, dp(2), 0, dp(3))
        })
        parent.addView(text("Build focus one session at a time.", 13f, muted, false))
        gap(parent, 18)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(19), dp(20), dp(18))
            background = gradientCard(greenDark, green, 24)
            elevation = dp(2).toFloat()
        }
        val heroTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heroTitleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heroTitleBox.addView(text("YOUR NEXT SESSION", 9f, Color.rgb(196, 235, 211), true))
        heroTitleBox.addView(text("Ready when you are.", 19f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, 0) })
        heroTop.addView(heroTitleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val badge = TextView(this).apply {
            text = "●  ON TRACK"
            textSize = 8.5f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = rounded(Color.argb(38, 255, 255, 255), 20)
        }
        heroTop.addView(badge)
        hero.addView(heroTop)
        gap(hero, 17)
        hero.addView(text("A little progress, repeated daily, becomes a big change.", 12f, Color.rgb(221, 243, 229), false))
        gap(hero, 17)
        val heroAction = button("START FOCUS  →", true, greenDark) {
            if (mode == "stopwatch" && stopwatchStartedAt == 0L) mode = "timer"
            currentTab = "focus"
            render()
        }
        heroAction.background = rounded(Color.WHITE, 13)
        heroAction.setTextColor(greenDark)
        hero.addView(heroAction, LinearLayout.LayoutParams(-1, dp(48)))
        parent.addView(hero)
        gap(parent, 16)

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(metricCard("${todayFocusMinutes()}", "FOCUS MIN", green), LinearLayout.LayoutParams(0, dp(92), 1f))
        stats.addView(spaceW(9))
        stats.addView(metricCard("${todaySessions()}", "SESSIONS", orange), LinearLayout.LayoutParams(0, dp(92), 1f))
        stats.addView(spaceW(9))
        stats.addView(metricCard("${currentStreak()}", "DAY STREAK", greenDark), LinearLayout.LayoutParams(0, dp(92), 1f))
        parent.addView(stats)
        gap(parent, 15)

        val goal = card()
        val goalHead = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        goalHead.addView(text("Daily focus goal", 15f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        goalHead.addView(text("${todayFocusMinutes()} / ${dailyGoalMinutes()} min", 11f, green, true))
        goal.addView(goalHead)
        gap(goal, 11)
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (todayFocusMinutes() * 1000 / max(1, dailyGoalMinutes())).coerceIn(0, 1000)
            progressTintList = android.content.res.ColorStateList.valueOf(green)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(greenSoft)
        }
        goal.addView(pb, LinearLayout.LayoutParams(-1, dp(8)))
        gap(goal, 8)
        goal.addView(text(if (todayFocusMinutes() >= dailyGoalMinutes()) "Goal reached. Great work today." else "${max(0, dailyGoalMinutes() - todayFocusMinutes())} minutes to reach today's goal.", 11f, muted, false))
        parent.addView(goal)
        gap(parent, 15)

        val weekCard = card()
        sectionTitle(weekCard, "THIS WEEK", "Your focus rhythm")
        gap(weekCard, 6)
        weekCard.addView(WeeklyFocusChart(this, green, border, weeklyFocusValues()), LinearLayout.LayoutParams(-1, dp(120)))
        parent.addView(weekCard)
        gap(parent, 15)

        val quickHead = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        quickHead.addView(text("Quick tools", 17f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        quickHead.addView(text("VIEW ALL", 9f, green, true).apply { setOnClickListener { currentTab = "block"; render() } })
        parent.addView(quickHead)
        gap(parent, 9)
        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quick.addView(quickCard("App blocker", "Choose distractions", "▣") { currentTab = "block"; render() }, LinearLayout.LayoutParams(0, dp(104), 1f))
        quick.addView(spaceW(10))
        quick.addView(quickCard("Screen time", "See your usage", "◷") { currentTab = "stats"; render() }, LinearLayout.LayoutParams(0, dp(104), 1f))
        parent.addView(quick)
        gap(parent, 14)
        parent.addView(text("Your focus data stays on this device.", 10f, muted, false).apply { gravity = Gravity.CENTER })
    }

    private fun metricCard(value: String, title: String, accent: Int): View {
        val box = card().apply { setPadding(dp(12), dp(13), dp(10), dp(10)) }
        box.addView(text(value, 23f, ink, true))
        gap(box, 5)
        box.addView(text(title, 8.5f, accent, true))
        return box
    }

    private fun quickCard(title: String, subtitle: String, icon: String, click: () -> Unit): View {
        val box = card().apply {
            setPadding(dp(13), dp(12), dp(10), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        val badge = TextView(this).apply {
            text = icon
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(green)
            background = rounded(greenSoft, 11)
        }
        box.addView(badge, LinearLayout.LayoutParams(dp(33), dp(33)))
        gap(box, 6)
        box.addView(text(title, 13f, ink, true))
        box.addView(text(subtitle, 10f, muted, false))
        return box
    }

    // ---------------------------- Focus modes ----------------------------

    private fun buildFocus(parent: LinearLayout) {
        parent.addView(text("FOCUS SESSION", 10f, green, true))
        parent.addView(text("Find your flow.", 29f, ink, true).apply { setPadding(0, dp(2), 0, dp(2)) })
        parent.addView(text("Choose how you want to work today.", 12f, muted, false))
        gap(parent, 16)

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Color.rgb(233, 240, 235), 15)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        addModeChip(modeRow, "Timer", "timer")
        addModeChip(modeRow, "Pomodoro", "pomodoro")
        addModeChip(modeRow, "Stopwatch", "stopwatch")
        parent.addView(modeRow, LinearLayout.LayoutParams(-1, dp(45)))
        gap(parent, 14)

        val timerCard = card().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        val status = when {
            timerRunning && isBreak -> "BREAK IN PROGRESS"
            timerRunning -> "FOCUS IN PROGRESS"
            mode == "pomodoro" && isBreak -> "READY FOR YOUR BREAK"
            else -> "READY WHEN YOU ARE"
        }
        timerCaption = text(status, 9f, green, true)
        timerCard.addView(timerCaption)
        gap(timerCard, 14)

        val ring = RingProgressView(this, green, greenSoft).apply {
            setProgress(timerProgressValue())
        }
        timerRing = ring
        val ringBox = FrameLayoutCompat(this)
        ringBox.addView(ring, FrameLayoutCompat.LayoutParams(dp(218), dp(218), Gravity.CENTER))
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        timerValue = text(currentTimerText(), 43f, ink, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = -0.03f
        }
        inner.addView(timerValue)
        inner.addView(text(if (mode == "stopwatch") "ELAPSED" else if (isBreak) "BREAK TIME" else "REMAINING", 9f, muted, true))
        ringBox.addView(inner, FrameLayoutCompat.LayoutParams(dp(170), dp(170), Gravity.CENTER))
        timerCard.addView(ringBox, LinearLayout.LayoutParams(-1, dp(218)))
        gap(timerCard, 9)
        val sub = when (mode) {
            "pomodoro" -> if (isBreak) "Cycle ${max(1, pomodoroCycles)} · take a short reset" else "Cycle ${pomodoroCycles + 1} · focus for ${selectedMinutes} minutes"
            "stopwatch" -> "Let the timer measure your deep-work session"
            else -> "A focused ${selectedMinutes}-minute session"
        }
        timerCard.addView(text(sub, 11f, muted, false))
        gap(timerCard, 16)

        if (mode != "stopwatch") {
            val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            listOf(25, 45, 60, 90).forEachIndexed { i, mins ->
                val chip = smallChip("${mins}m", selectedMinutes == mins) {
                    if (!timerRunning) {
                        selectedMinutes = mins
                        remainingMillis = (if (mode == "pomodoro" && isBreak) 5 else mins) * 60_000L
                        intervalDurationMillis = remainingMillis
                        saveTimerState()
                        render()
                    }
                }
                presets.addView(chip, LinearLayout.LayoutParams(0, dp(40), 1f).apply { if (i != 3) marginEnd = dp(7) })
            }
            timerCard.addView(presets)
            gap(timerCard, 13)
        }

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val startLabel = when {
            timerRunning -> "PAUSE"
            mode == "stopwatch" && stopwatchStartedAt > 0L -> "PAUSE"
            mode == "stopwatch" && stopwatchAccumulated > 0L -> "RESUME"
            mode == "pomodoro" && isBreak -> "START BREAK"
            else -> "START FOCUS"
        }
        actions.addView(button(startLabel, true) {
            if (timerRunning || (mode == "stopwatch" && stopwatchStartedAt > 0L)) pauseFocus() else startFocus()
        }, LinearLayout.LayoutParams(0, dp(50), 1f))
        actions.addView(spaceW(9))
        actions.addView(button(if (mode == "stopwatch") "SAVE" else "RESET", false) { if (mode == "stopwatch") finishStopwatch() else resetFocus() }, LinearLayout.LayoutParams(dp(94), dp(50)))
        timerCard.addView(actions)
        parent.addView(timerCard)
        gap(parent, 13)

        val blockCard = card()
        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(text("Block distractions", 14f, ink, true))
        titleBox.addView(text("Pause selected apps while this session runs.", 10f, muted, false))
        titleRow.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val focusSwitch = Switch(this).apply {
            isChecked = prefs.getBoolean("block_during_focus", true)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("block_during_focus", checked).apply() }
        }
        titleRow.addView(focusSwitch)
        blockCard.addView(titleRow)
        parent.addView(blockCard)
        gap(parent, 14)
        parent.addView(text("Your timer is saved automatically when you leave Kabox.", 10f, muted, false).apply { gravity = Gravity.CENTER })
    }

    private fun addModeChip(row: LinearLayout, label: String, value: String) {
        val selected = mode == value
        val chip = TextView(this).apply {
            text = label
            textSize = 11f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", if (selected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (selected) greenDark else muted)
            background = if (selected) rounded(Color.WHITE, 12) else rounded(Color.TRANSPARENT, 12)
            setOnClickListener {
                if (!timerRunning && !(mode == "stopwatch" && stopwatchStartedAt > 0L)) {
                    mode = value
                    isBreak = false
                    if (mode == "stopwatch") {
                        remainingMillis = 0L
                    } else {
                        remainingMillis = selectedMinutes * 60_000L
                        intervalDurationMillis = remainingMillis
                    }
                    saveTimerState()
                    render()
                }
            }
        }
        row.addView(chip, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun startFocus() {
        if (mode == "stopwatch") {
            stopwatchStartedAt = System.currentTimeMillis()
            prefs.edit().putLong("stopwatch_started_at", stopwatchStartedAt).apply()
            ensureTicker()
            Toast.makeText(this, "Stopwatch started", Toast.LENGTH_SHORT).show()
        } else {
            if (remainingMillis <= 0L) { remainingMillis = selectedMinutes * 60_000L; intervalDurationMillis = remainingMillis }
            timerRunning = true
            finishAtMillis = System.currentTimeMillis() + remainingMillis
            saveTimerState()
            ensureTicker()
            Toast.makeText(this, if (isBreak) "Break started" else "Focus started", Toast.LENGTH_SHORT).show()
        }
        render()
    }

    private fun pauseFocus() {
        if (mode == "stopwatch") {
            if (stopwatchStartedAt > 0L) {
                stopwatchAccumulated += max(0L, System.currentTimeMillis() - stopwatchStartedAt)
                stopwatchStartedAt = 0L
                prefs.edit().putLong("stopwatch_accumulated", stopwatchAccumulated).putLong("stopwatch_started_at", 0L).apply()
            }
        } else if (timerRunning) {
            remainingMillis = max(0L, finishAtMillis - System.currentTimeMillis())
            timerRunning = false
            finishAtMillis = 0L
            saveTimerState()
            if (remainingMillis == 0L) completeCountdown()
        }
        render()
    }

    private fun resetFocus() {
        timerRunning = false
        finishAtMillis = 0L
        isBreak = false
        if (mode == "stopwatch") {
            stopwatchAccumulated = 0L
            stopwatchStartedAt = 0L
            prefs.edit().putLong("stopwatch_accumulated", 0L).putLong("stopwatch_started_at", 0L).apply()
        } else {
            remainingMillis = selectedMinutes * 60_000L
            intervalDurationMillis = remainingMillis
        }
        saveTimerState()
        render()
    }

    private fun completeCountdown() {
        timerRunning = false
        finishAtMillis = 0L
        remainingMillis = 0L
        if (mode == "pomodoro") {
            if (!isBreak) {
                recordFocusSession(selectedMinutes)
                pomodoroCycles += 1
                isBreak = true
                val breakMinutes = if (pomodoroCycles % 4 == 0) 15 else 5
                remainingMillis = breakMinutes * 60_000L
                intervalDurationMillis = remainingMillis
                Toast.makeText(this, "Focus cycle complete. Take a ${breakMinutes}-minute break.", Toast.LENGTH_LONG).show()
            } else {
                isBreak = false
                remainingMillis = selectedMinutes * 60_000L
                intervalDurationMillis = remainingMillis
                Toast.makeText(this, "Break complete. Ready for another focus cycle?", Toast.LENGTH_LONG).show()
            }
        } else {
            recordFocusSession(selectedMinutes)
            Toast.makeText(this, "Focus session complete. Well done!", Toast.LENGTH_LONG).show()
        }
        saveTimerState()
        if (::shell.isInitialized) render()
    }

    private fun finishStopwatch() {
        if (stopwatchStartedAt > 0L) stopwatchAccumulated += max(0L, System.currentTimeMillis() - stopwatchStartedAt)
        stopwatchStartedAt = 0L
        val minutes = (stopwatchAccumulated / 60_000L).toInt()
        if (minutes > 0) recordFocusSession(minutes)
        stopwatchAccumulated = 0L
        prefs.edit().putLong("stopwatch_accumulated", 0L).putLong("stopwatch_started_at", 0L).apply()
        Toast.makeText(this, if (minutes > 0) "${minutes} focus minutes saved" else "Session was shorter than one minute", Toast.LENGTH_LONG).show()
        render()
    }

    private fun currentTimerText(): String {
        return if (mode == "stopwatch") {
            val elapsed = stopwatchAccumulated + if (stopwatchStartedAt > 0L) max(0L, System.currentTimeMillis() - stopwatchStartedAt) else 0L
            formatClock(elapsed)
        } else formatClock(remainingMillis)
    }

    private fun timerProgressValue(): Float {
        if (mode == "stopwatch") return 0f
        val total = max(1L, intervalDurationMillis).toFloat()
        if (total <= 0f) return 0f
        return ((total - remainingMillis.toFloat()).coerceAtLeast(0f) / total).coerceIn(0f, 1f)
    }

    private fun updateTimerWidgets() {
        if (timerRunning && mode != "stopwatch") {
            remainingMillis = max(0L, finishAtMillis - System.currentTimeMillis())
            if (remainingMillis <= 0L) { completeCountdown(); return }
        }
        timerValue?.text = currentTimerText()
        timerCaption?.text = when {
            timerRunning && isBreak -> "BREAK IN PROGRESS"
            timerRunning -> "FOCUS IN PROGRESS"
            mode == "pomodoro" && isBreak -> "READY FOR YOUR BREAK"
            mode == "stopwatch" && stopwatchStartedAt > 0L -> "STOPWATCH RUNNING"
            else -> "READY WHEN YOU ARE"
        }
        timerRing?.setProgress(timerProgressValue())
    }

    private fun ensureTicker() {
        if (!tickerStarted && (timerRunning || (mode == "stopwatch" && stopwatchStartedAt > 0L))) {
            tickerStarted = true
            handler.removeCallbacks(tick)
            handler.post(tick)
        }
    }

    private fun saveTimerState() {
        prefs.edit()
            .putString("timer_mode", mode)
            .putInt("timer_selected_minutes", selectedMinutes)
            .putLong("timer_remaining", remainingMillis)
            .putLong("timer_interval_duration", intervalDurationMillis)
            .putLong("timer_finish_at", if (timerRunning) finishAtMillis else 0L)
            .putBoolean("timer_running", timerRunning)
            .putBoolean("timer_is_break", isBreak)
            .putInt("pomodoro_cycles", pomodoroCycles)
            .apply()
    }

    private fun restoreTimerState() {
        mode = prefs.getString("timer_mode", "timer") ?: "timer"
        if (mode !in setOf("timer", "pomodoro", "stopwatch")) mode = "timer"
        selectedMinutes = prefs.getInt("timer_selected_minutes", 25).coerceIn(1, 180)
        remainingMillis = prefs.getLong("timer_remaining", selectedMinutes * 60_000L).coerceAtLeast(0L)
        intervalDurationMillis = prefs.getLong("timer_interval_duration", selectedMinutes * 60_000L).coerceAtLeast(1L)
        finishAtMillis = prefs.getLong("timer_finish_at", 0L)
        timerRunning = prefs.getBoolean("timer_running", false)
        isBreak = prefs.getBoolean("timer_is_break", false)
        pomodoroCycles = prefs.getInt("pomodoro_cycles", 0)
        stopwatchAccumulated = prefs.getLong("stopwatch_accumulated", 0L)
        stopwatchStartedAt = prefs.getLong("stopwatch_started_at", 0L)
    }

    private fun reconcileTimerAfterResume() {
        if (timerRunning) {
            val left = finishAtMillis - System.currentTimeMillis()
            if (left <= 0L) {
                remainingMillis = 0L
                completeCountdown()
            } else remainingMillis = left
        }
        if (mode == "stopwatch" && stopwatchStartedAt > 0L) {
            // Wall-clock start time is retained so the stopwatch continues across process recreation.
        }
    }

    // ---------------------------- App blocker ----------------------------

    private fun buildBlock(parent: LinearLayout) {
        parent.addView(text("DIGITAL WELLBEING", 10f, green, true))
        parent.addView(text("Choose what gets\nyour attention.", 29f, ink, true).apply { setPadding(0, dp(3), 0, dp(4)) })
        parent.addView(text("Select distracting apps. Kabox can pause them during focus or after a daily limit.", 12f, muted, false))
        gap(parent, 15)

        val statusCard = card()
        val enabled = isAccessibilityEnabled()
        val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val statusText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusText.addView(text("App blocker", 16f, ink, true))
        statusText.addView(text(if (enabled) "Accessibility service connected" else "Needs one-time Android permission", 10f, if (enabled) green else orange, false))
        statusRow.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        val statusPill = TextView(this).apply {
            text = if (enabled) "READY" else "SETUP"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (enabled) greenDark else orange)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = rounded(if (enabled) greenSoft else Color.rgb(255, 243, 230), 20)
        }
        statusRow.addView(statusPill)
        statusCard.addView(statusRow)
        gap(statusCard, 12)
        val master = Switch(this).apply {
            text = "Block selected apps all the time"
            textSize = 12f
            setTextColor(ink)
            isChecked = prefs.getBoolean("blocker_always_on", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("blocker_always_on", checked).apply()
                if (checked && !isAccessibilityEnabled()) showAccessibilityDisclosure()
            }
        }
        statusCard.addView(master)
        gap(statusCard, 6)
        statusCard.addView(button(if (enabled) "OPEN ACCESSIBILITY SETTINGS" else "ENABLE APP BLOCKER", true) { showAccessibilityDisclosure() })
        parent.addView(statusCard)
        gap(parent, 12)

        val disclosure = card().apply { setPadding(dp(14), dp(13), dp(14), dp(13)) }
        disclosure.addView(text("YOUR PRIVACY", 9f, green, true))
        gap(disclosure, 5)
        disclosure.addView(text("Kabox uses Accessibility only to detect the foreground app you selected and show a block screen. It does not inspect screen text, passwords or messages, and does not send accessibility data to a server. You can turn the service off in Android Settings at any time. Daily app caps also need Usage Access.", 10.5f, muted, false))
        parent.addView(disclosure)
        gap(parent, 15)

        val listHead = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listHead.addView(text("Your app list", 17f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        val blockedCount = selectedBlockedApps().size
        listHead.addView(text("$blockedCount selected", 10f, green, true))
        parent.addView(listHead)
        gap(parent, 5)
        parent.addView(text("Selected apps are paused during focus when the blocker service is enabled.", 10f, muted, false))
        gap(parent, 9)

        val apps = installedApps()
        if (apps.isEmpty()) {
            parent.addView(card().apply { addView(text("No launchable apps found.", 12f, muted, false)) })
        } else {
            apps.forEach { app ->
                parent.addView(appBlockRow(app))
                gap(parent, 7)
            }
        }

        val websiteCard = card()
        websiteCard.addView(text("Website and Reels filters", 14f, ink, true))
        gap(websiteCard, 5)
        websiteCard.addView(text("Not enabled in this release. For now, selecting Instagram or YouTube above blocks the whole app during focus; selective Reels/Shorts and website filtering are planned for a later tested release.", 10.5f, muted, false))
        parent.addView(websiteCard)
        gap(parent, 16)
    }

    private data class AppRowData(val packageName: String, val label: String, val icon: android.graphics.drawable.Drawable, val isSystem: Boolean)

    private fun installedApps(): List<AppRowData> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { info ->
                    val activityInfo = info.activityInfo ?: return@mapNotNull null
                    val pkg = activityInfo.packageName
                    if (pkg == packageName || pkg == "android") return@mapNotNull null
                    try {
                        val appInfo = packageManager.getApplicationInfo(pkg, 0)
                        AppRowData(pkg, packageManager.getApplicationLabel(appInfo).toString(), packageManager.getApplicationIcon(appInfo), (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                    } catch (_: Exception) { null }
                }
                .distinctBy { it.packageName }
                .sortedWith(compareBy<AppRowData> { it.isSystem }.thenBy { it.label.lowercase(Locale.getDefault()) })
        } catch (_: Exception) { emptyList() }
    }

    private fun appBlockRow(app: AppRowData): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(10), dp(10))
            background = rounded(surface, 15, border)
        }
        val icon = ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER }
        row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        details.addView(text(app.label, 12f, ink, true))
        val limit = if (prefs.getBoolean("limit_enabled_${app.packageName}", false)) "Limit: ${prefs.getInt("limit_minutes_${app.packageName}", 60)} min/day" else "No daily limit"
        details.addView(text(limit, 9f, muted, false))
        row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))

        val selected = selectedBlockedApps().contains(app.packageName)
        val sw = Switch(this).apply {
            isChecked = selected
            scaleX = 0.88f
            scaleY = 0.88f
            setOnCheckedChangeListener { _, checked ->
                val set = selectedBlockedApps().toMutableSet()
                if (checked) set.add(app.packageName) else set.remove(app.packageName)
                prefs.edit().putStringSet("blocked_apps", set).apply()
            }
        }
        row.addView(sw)
        row.setOnClickListener { sw.isChecked = !sw.isChecked }
        val limitButton = TextView(this).apply {
            text = "⋮"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(dp(5), 0, dp(2), 0)
            setOnClickListener { showAppLimitDialog(app) }
        }
        row.addView(limitButton, LinearLayout.LayoutParams(dp(28), dp(40)))
        return row
    }

    private fun showAppLimitDialog(app: AppRowData) {
        val options = arrayOf("No daily limit", "30 minutes/day", "60 minutes/day", "90 minutes/day", "120 minutes/day", "180 minutes/day")
        AlertDialog.Builder(this)
            .setTitle("Daily limit · ${app.label}")
            .setItems(options) { _, which ->
                if (which == 0) {
                    prefs.edit().putBoolean("limit_enabled_${app.packageName}", false).apply()
                } else {
                    val minutes = listOf(0, 30, 60, 90, 120, 180)[which]
                    prefs.edit().putBoolean("limit_enabled_${app.packageName}", true)
                        .putInt("limit_minutes_${app.packageName}", minutes).apply()
                }
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAccessibilityDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Allow Kabox to pause selected apps?")
            .setMessage("Kabox uses Android Accessibility Service to detect when an app you selected comes to the foreground. When a focus session is active, or an always-on block/daily limit applies, Kabox sends you to a brief interruption screen. It does not read screen content or send accessibility data away from your device. You can disable the service at any time in Android Settings.")
            .setPositiveButton("Continue to settings") { _, _ ->
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                catch (_: Exception) { Toast.makeText(this, "Open Android Settings → Accessibility → Downloaded apps → Kabox.", Toast.LENGTH_LONG).show() }
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(this, com.kabox.app.blocking.AppBlockAccessibilityService::class.java).flattenToString()
        return try {
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == com.kabox.app.blocking.AppBlockAccessibilityService::class.java.name }
                || (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(expected, ignoreCase = true)
        } catch (_: Exception) { false }
    }

    private fun selectedBlockedApps(): Set<String> = prefs.getStringSet("blocked_apps", emptySet())?.toSet() ?: emptySet()

    // ---------------------------- Insights ----------------------------

    private data class UsageRow(val packageName: String, val label: String, val millis: Long, val icon: android.graphics.drawable.Drawable?)

    private fun buildStats(parent: LinearLayout) {
        parent.addView(text("YOUR DEVICE", 10f, green, true))
        parent.addView(text("Understand your\nscreen time.", 29f, ink, true).apply { setPadding(0, dp(2), 0, dp(4)) })
        parent.addView(text("A clearer picture of where your time goes.", 12f, muted, false))
        gap(parent, 15)

        val allowed = hasUsageAccess()
        val total = if (allowed) todayUsageMillis() else 0L
        val screenCard = card()
        screenCard.addView(text("SCREEN TIME · TODAY", 9f, green, true))
        gap(screenCard, 6)
        screenCard.addView(text(if (allowed) formatDuration(total) else "Not connected", 33f, ink, true))
        screenCard.addView(text(if (allowed) "Total foreground time from Android Usage Access." else "Enable Usage Access so Kabox can read app usage on this device.", 10.5f, muted, false))
        if (!allowed) {
            gap(screenCard, 12)
            screenCard.addView(button("OPEN USAGE ACCESS", true) { openUsageSettings() })
        }
        parent.addView(screenCard)
        gap(parent, 12)

        val focusCard = card()
        sectionTitle(focusCard, "YOUR FOCUS", "Today's progress")
        gap(focusCard, 10)
        focusCard.addView(statLine("Focus time", "${todayFocusMinutes()} min"))
        gap(focusCard, 8)
        focusCard.addView(statLine("Completed sessions", "${todaySessions()}"))
        gap(focusCard, 8)
        focusCard.addView(statLine("Current streak", "${currentStreak()} days"))
        parent.addView(focusCard)
        gap(parent, 12)

        val chartCard = card()
        sectionTitle(chartCard, "LAST 7 DAYS", "Focus activity")
        gap(chartCard, 8)
        chartCard.addView(WeeklyFocusChart(this, green, border, weeklyFocusValues()), LinearLayout.LayoutParams(-1, dp(120)))
        parent.addView(chartCard)
        gap(parent, 12)

        val appCard = card()
        sectionTitle(appCard, "MOST USED APPS", "Today")
        gap(appCard, 8)
        if (!allowed) {
            appCard.addView(text("App usage will appear here after Usage Access is enabled.", 11f, muted, false))
        } else {
            val rows = topUsageRows()
            if (rows.isEmpty()) appCard.addView(text("No app usage data is available yet. Try again after using your phone for a little while.", 11f, muted, false))
            else rows.take(6).forEachIndexed { index, row ->
                appCard.addView(usageRow(row))
                if (index != min(5, rows.lastIndex)) gap(appCard, 10)
            }
        }
        parent.addView(appCard)
        gap(parent, 16)
        parent.addView(text("Usage reports are provided by Android. Data is processed locally by Kabox.", 10f, muted, false).apply { gravity = Gravity.CENTER })
    }

    private fun usageRow(row: UsageRow): View {
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icon = ImageView(this).apply {
            if (row.icon != null) setImageDrawable(row.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        line.addView(icon, LinearLayout.LayoutParams(dp(29), dp(29)))
        val labelBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), 0, 0, 0) }
        labelBox.addView(text(row.label, 11f, ink, true))
        val maxValue = max(1L, todayUsageMillis())
        val prog = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (row.millis * 1000L / maxValue).toInt().coerceIn(8, 1000)
            progressTintList = android.content.res.ColorStateList.valueOf(green)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(greenSoft)
        }
        labelBox.addView(prog, LinearLayout.LayoutParams(dp(130), dp(5)).apply { topMargin = dp(5) })
        line.addView(labelBox, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(text(formatDuration(row.millis), 10f, muted, true))
        return line
    }

    private fun topUsageRows(): List<UsageRow> {
        if (!hasUsageAccess()) return emptyList()
        val start = startOfDayMillis()
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis()) ?: return emptyList()
        val launchers = installedApps().associateBy { it.packageName }
        return stats.groupBy { it.packageName }
            .mapNotNull { (pkg, list) ->
                if (pkg == packageName) return@mapNotNull null
                val app = launchers[pkg]
                val duration = list.sumOf { it.totalTimeInForeground }
                if (duration <= 0L) return@mapNotNull null
                val label = app?.label ?: try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { return@mapNotNull null }
                UsageRow(pkg, label, duration, app?.icon)
            }
            .sortedByDescending { it.millis }
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun openUsageSettings() {
        try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        catch (_: Exception) { Toast.makeText(this, "Settings → Apps → Special app access → Usage access → Kabox.", Toast.LENGTH_LONG).show() }
    }

    private fun todayUsageMillis(): Long {
        if (!hasUsageAccess()) return 0L
        return try {
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDayMillis(), System.currentTimeMillis())
                ?.filter { it.packageName != packageName }
                ?.sumOf { it.totalTimeInForeground } ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun startOfDayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ---------------------------- Profile / settings ----------------------------

    private fun buildProfile(parent: LinearLayout) {
        parent.addView(text("KABOX SETTINGS", 10f, green, true))
        parent.addView(text("Your space.\nYour rhythm.", 29f, ink, true).apply { setPadding(0, dp(3), 0, dp(4)) })
        parent.addView(text("Personalise how Kabox helps you focus.", 12f, muted, false))
        gap(parent, 15)

        val goalCard = card()
        goalCard.addView(text("DAILY FOCUS GOAL", 9f, green, true))
        gap(goalCard, 7)
        val goalRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        goalRow.addView(text("${dailyGoalMinutes()} minutes", 22f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        goalRow.addView(button("CHANGE", false) { showGoalDialog() }, LinearLayout.LayoutParams(dp(94), dp(38)))
        goalCard.addView(goalRow)
        parent.addView(goalCard)
        gap(parent, 12)

        val schedule = card()
        schedule.addView(text("DAILY FOCUS REMINDER", 9f, green, true))
        gap(schedule, 7)
        schedule.addView(text("A gentle nudge to begin your planned session.", 11f, muted, false))
        gap(schedule, 10)
        schedule.addView(button("SET REMINDER TIME", true) { showScheduleTimePicker() })
        parent.addView(schedule)
        gap(parent, 12)

        val premium = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = gradientCard(greenDark, green, 20)
        }
        premium.addView(text("KABOX PLUS", 9f, Color.rgb(200, 240, 214), true))
        gap(premium, 6)
        premium.addView(text("More control.\nMore intentional days.", 20f, Color.WHITE, true))
        gap(premium, 8)
        premium.addView(text("Planned price: ₹39 / month", 13f, Color.WHITE, true))
        gap(premium, 4)
        premium.addView(text("Subscriptions are not active in this test APK. No payment can be taken yet.", 10f, Color.rgb(222, 243, 229), false))
        parent.addView(premium)
        gap(parent, 12)

        val privacy = card()
        privacy.addView(text("PRIVACY & PERMISSIONS", 9f, green, true))
        gap(privacy, 7)
        privacy.addView(text("Usage Access", 13f, ink, true))
        privacy.addView(text(if (hasUsageAccess()) "Connected" else "Not enabled", 10f, muted, false))
        gap(privacy, 9)
        privacy.addView(button("MANAGE USAGE ACCESS", false) { openUsageSettings() })
        gap(privacy, 8)
        privacy.addView(text("Accessibility is only used for the app-blocking feature you choose to enable. You can disable it from Android Settings.", 10.5f, muted, false))
        parent.addView(privacy)
        gap(parent, 12)

        val about = card()
        about.addView(text("ABOUT KABOX", 9f, green, true))
        gap(about, 6)
        about.addView(text("Version 0.3.0 · Offline-first focus companion", 11f, ink, true))
        gap(about, 3)
        about.addView(text("Kabox is an independent product and is not affiliated with Regain or EpowerX Labs.", 10f, muted, false))
        parent.addView(about)
    }

    private fun showGoalDialog() {
        val options = arrayOf("30 minutes", "60 minutes", "90 minutes", "120 minutes", "180 minutes", "240 minutes")
        val values = intArrayOf(30, 60, 90, 120, 180, 240)
        AlertDialog.Builder(this).setTitle("Daily focus goal")
            .setItems(options) { _, which -> prefs.edit().putInt("daily_goal_minutes", values[which]).apply(); render() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showScheduleTimePicker() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 430)
        }
        TimePickerDialog(this, { _, hour, minute ->
            prefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).putBoolean("reminder_enabled", true).apply()
            scheduleReminder(hour, minute)
            val label = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            Toast.makeText(this, "Daily reminder set for $label", Toast.LENGTH_LONG).show()
        }, prefs.getInt("reminder_hour", 18), prefs.getInt("reminder_minute", 0), false).show()
    }

    private fun scheduleReminder(hour: Int, minute: Int) {
        val intent = Intent(this, com.kabox.app.reminders.FocusReminderReceiver::class.java)
        val pending = android.app.PendingIntent.getBroadcast(this, 431, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarm = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarm.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, android.app.AlarmManager.INTERVAL_DAY, pending)
    }

    // ---------------------------- Records / helpers ----------------------------

    private fun recordFocusSession(minutes: Int) {
        if (minutes <= 0) return
        val key = dateKey()
        prefs.edit()
            .putInt("focus_minutes_$key", todayFocusMinutes() + minutes)
            .putInt("focus_sessions_$key", todaySessions() + 1)
            .putInt("focus_points", prefs.getInt("focus_points", 0) + 10)
            .apply()
    }

    private fun todayFocusMinutes(): Int = prefs.getInt("focus_minutes_${dateKey()}", 0)
    private fun todaySessions(): Int = prefs.getInt("focus_sessions_${dateKey()}", 0)
    private fun dailyGoalMinutes(): Int = prefs.getInt("daily_goal_minutes", 120)
    private fun dateKey(daysAgo: Int = 0): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    private fun weeklyFocusValues(): List<Int> = (6 downTo 0).map { prefs.getInt("focus_minutes_${dateKey(it)}", 0) }

    private fun currentStreak(): Int {
        var offset = if (prefs.getInt("focus_minutes_${dateKey(0)}", 0) > 0) 0 else 1
        var count = 0
        while (offset <= 60 && prefs.getInt("focus_minutes_${dateKey(offset)}", 0) > 0) {
            count++
            offset++
        }
        return count
    }

    private fun formatClock(millis: Long): String {
        val seconds = max(0L, millis) / 1000L
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun formatDuration(millis: Long): String {
        val mins = millis / 60_000L
        val hours = mins / 60L
        val remaining = mins % 60L
        return if (hours > 0) "${hours}h ${remaining}m" else "${remaining}m"
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(surface, 18, border)
        elevation = dp(1).toFloat()
    }

    private fun sectionTitle(parent: LinearLayout, eyebrow: String, title: String) {
        parent.addView(text(eyebrow, 8.5f, green, true))
        gap(parent, 4)
        parent.addView(text(title, 16f, ink, true))
    }

    private fun statLine(title: String, value: String): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(text(title, 11f, muted, false), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(value, 12f, ink, true))
        return row
    }

    private fun button(label: String, primary: Boolean, custom: Int? = null, click: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 10f
        gravity = Gravity.CENTER
        letterSpacing = 0.04f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(if (primary) Color.WHITE else green)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(custom ?: if (primary) green else greenSoft, 13, if (primary) null else border)
        isClickable = true
        isFocusable = true
        setOnClickListener { animate().scaleX(0.98f).scaleY(0.98f).setDuration(70).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(80).start(); click()
        }.start() }
    }

    private fun smallChip(label: String, selected: Boolean, click: () -> Unit): View = TextView(this).apply {
        text = label
        textSize = 10.5f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", if (selected) Typeface.BOLD else Typeface.NORMAL)
        setTextColor(if (selected) Color.WHITE else muted)
        background = rounded(if (selected) green else Color.rgb(244, 247, 244), 11, if (selected) null else border)
        setOnClickListener { click() }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun gradientCard(start: Int, end: Int, radius: Int): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end)
    ).apply { cornerRadius = dp(radius).toFloat() }

    private fun gap(parent: LinearLayout, height: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun spaceW(width: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private class FrameLayoutCompat(context: Context) : android.widget.FrameLayout(context) {
        class LayoutParams(width: Int, height: Int, gravity: Int) : android.widget.FrameLayout.LayoutParams(width, height, gravity)
    }
}
