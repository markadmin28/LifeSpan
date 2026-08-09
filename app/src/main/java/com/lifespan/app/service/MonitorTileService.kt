package com.lifespan.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.lifespan.app.LifeSpanApp

/** Quick Settings tile that toggles the battery monitoring service. */
class MonitorTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile(monitoring())
    }

    override fun onClick() {
        super.onClick()
        val running = monitoring()
        if (running) {
            BatteryMonitorService.stop(this)
        } else {
            BatteryMonitorService.start(this)
        }
        refreshTile(!running)
    }

    private fun monitoring(): Boolean =
        (applicationContext as LifeSpanApp).container.batteryRepository.monitoring.value

    private fun refreshTile(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(com.lifespan.app.R.string.tile_label)
        tile.updateTile()
    }
}
