package com.kabox.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

 data class AppUsage(val label: String, val packageName: String, val minutes: Long)

object UsageRepository {
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openPermissionSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Suppress("DEPRECATION")
    fun loadLast24Hours(context: Context): List<AppUsage> {
        if (!hasPermission(context)) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 24L * 60L * 60L * 1000L
        val packageManager = context.packageManager
        return manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .asSequence()
            .filter { it.totalTimeInForeground > 0L && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .map { (packageName, records) ->
                val totalMs = records.sumOf { it.totalTimeInForeground }
                val label = try {
                    val info = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(info).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    packageName.substringAfterLast('.')
                }
                AppUsage(label, packageName, totalMs / 60_000L)
            }
            .filter { it.minutes > 0L }
            .sortedByDescending { it.minutes }
            .take(12)
            .toList()
    }
}
