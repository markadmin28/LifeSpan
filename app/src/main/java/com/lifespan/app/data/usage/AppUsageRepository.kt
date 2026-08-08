package com.lifespan.app.data.usage

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.UsageRanker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides a per-app "battery usage" ranking derived from foreground time
 * (via [UsageStatsManager]) over the last 24h — the closest signal available to
 * a non-privileged app — plus a best-effort background-kill helper.
 */
class AppUsageRepository(private val context: Context) {

    private val ownPackage = context.packageName

    /** Whether the user has granted the special "Usage access" permission. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ownPackage,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ownPackage,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Aggregate foreground time per package over the last [windowMillis] and
     * return the top [limit] apps, resolving display labels via PackageManager.
     * Returns an empty list when usage access is not granted.
     */
    suspend fun topBatteryApps(
        limit: Int = 20,
        windowMillis: Long = 24 * 60 * 60 * 1000L,
    ): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - windowMillis

        val totals = HashMap<String, Long>()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            start,
            end,
        ) ?: emptyList()
        for (stat in stats) {
            if (stat.totalTimeInForeground <= 0L) continue
            if (stat.packageName == ownPackage) continue
            totals[stat.packageName] =
                (totals[stat.packageName] ?: 0L) + stat.totalTimeInForeground
        }

        val pm = context.packageManager
        val apps = totals.mapNotNull { (pkg, millis) ->
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull() ?: return@mapNotNull null
            AppUsage(packageName = pkg, label = label, foregroundMillis = millis)
        }
        UsageRanker.rank(apps, limit)
    }

    /**
     * Best-effort request to stop a package's background processes. On modern
     * Android this is limited (a true force-stop is privileged), so the UI also
     * routes the user to the app's system settings.
     */
    fun killBackground(packageName: String) {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
        }
    }
}
