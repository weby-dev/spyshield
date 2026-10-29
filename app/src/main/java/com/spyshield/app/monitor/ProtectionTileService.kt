package com.spyshield.app.monitor

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.spyshield.app.R
import com.spyshield.app.data.Store

/** Quick Settings tile that switches real-time protection on and off. */
class ProtectionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val on = !Store.protectionOn.value
        Store.setProtection(on)
        if (on) SensorMonitorService.start(applicationContext) else SensorMonitorService.stop(applicationContext)
        StatusWidgetProvider.refresh(applicationContext)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val on = Store.protectionOn.value
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "SpyShield"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (on) "Protecting" else "Off"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_shield)
        tile.updateTile()
    }

    companion object {
        /** Ask the system to re-bind the tile so it reflects the current protection state. */
        fun refresh(context: Context) {
            runCatching {
                TileService.requestListeningState(context, ComponentName(context, ProtectionTileService::class.java))
            }
        }
    }
}
