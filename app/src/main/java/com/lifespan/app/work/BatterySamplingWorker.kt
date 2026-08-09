package com.lifespan.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.lifespan.app.LifeSpanApp
import com.lifespan.app.data.BatteryReader
import com.lifespan.app.widget.LifeSpanWidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Periodically samples battery state and logs telemetry even when the
 * foreground service is not running, so long-term history stays continuous.
 */
class BatterySamplingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LifeSpanApp ?: return Result.success()
        // Foreground service is the live writer; skip to avoid duplicate
        // telemetry rows and charge-session open/close races.
        if (app.container.batteryRepository.monitoring.value) {
            return Result.success()
        }
        val snapshot = BatteryReader.sample(applicationContext) ?: return Result.success()
        app.container.batteryRepository.record(snapshot)
        LifeSpanWidgetProvider.update(applicationContext, snapshot)
        return Result.success()
    }
}

object SamplingScheduler {
    private const val WORK_NAME = "lifespan_battery_sampling"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BatterySamplingWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
