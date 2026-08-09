package com.lifespan.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lifespan.app.R
import com.lifespan.app.data.BatteryReader
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.ui.MainActivity
import java.util.Locale

/** Home-screen widget showing live battery level, temperature and power. */
class LifeSpanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val snapshot = BatteryReader.sample(context)
        val views = buildViews(context, snapshot)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    companion object {
        /** Push a fresh [snapshot] to all placed widgets (called by the service). */
        fun update(context: Context, snapshot: BatterySnapshot?) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LifeSpanWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, buildViews(context, snapshot))
        }

        private fun buildViews(context: Context, snapshot: BatterySnapshot?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_lifespan)
            if (snapshot == null) {
                views.setTextViewText(R.id.widget_level, "—")
                views.setTextViewText(R.id.widget_status, "Not monitoring")
                views.setTextViewText(R.id.widget_detail, "Tap to open")
            } else {
                views.setTextViewText(R.id.widget_level, "${snapshot.level}%")
                views.setTextViewText(
                    R.id.widget_status,
                    if (snapshot.isCharging) "Charging · ${snapshot.plugType.label}" else "On battery",
                )
                views.setTextViewText(
                    R.id.widget_detail,
                    String.format(
                        Locale.US,
                        "%.1f°C · %.2f W",
                        snapshot.temperatureCelsius,
                        snapshot.powerWatts,
                    ),
                )
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }
    }
}
