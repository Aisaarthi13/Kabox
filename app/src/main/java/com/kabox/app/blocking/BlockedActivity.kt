package com.kabox.app.blocking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class BlockedActivity : Activity() {
    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    private val green = Color.rgb(18, 122, 82)
    private val page = Color.rgb(245, 248, 245)
    private val ink = Color.rgb(25, 39, 31)
    private val muted = Color.rgb(112, 128, 119)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
            setBackgroundColor(page)
        }
        val mark = TextView(this).apply {
            text = "K"
            textSize = 36f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(green, 24)
        }
        root.addView(mark, LinearLayout.LayoutParams(dp(76), dp(76)))
        root.addView(TextView(this).apply {
            text = "Take a moment."
            textSize = 28f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(0, dp(23), 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "$appName is paused by your Kabox focus settings."
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Your attention is valuable. Return to the task you chose."
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(25))
        })
        root.addView(action("RETURN TO KABOX", true) {
            startActivity(Intent(this, com.kabox.app.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }, LinearLayout.LayoutParams(-1, dp(50)))
        root.addView(action("ALLOW 5 MINUTES", false) {
            if (targetPackage.isNotBlank()) {
                getSharedPreferences("kabox_local", Context.MODE_PRIVATE).edit()
                    .putLong("snooze_until_$targetPackage", System.currentTimeMillis() + 5 * 60_000L).apply()
            }
            Toast.makeText(this, "A 5-minute pause is allowed.", Toast.LENGTH_SHORT).show()
            finish()
        }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun action(label: String, primary: Boolean, click: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 10f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(if (primary) Color.WHITE else green)
        background = rounded(if (primary) green else Color.rgb(226, 245, 233), 14)
        setOnClickListener { click() }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
