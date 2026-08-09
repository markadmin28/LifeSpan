package com.lifespan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifespan.app.LifeSpanApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Restarts monitoring after device reboot when the user has opted in. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as LifeSpanApp
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = app.container.settingsRepository.appSettings.first()
                if (settings.autoStartEnabled) {
                    withContext(Dispatchers.Main) {
                        runCatching { BatteryMonitorService.start(context) }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
