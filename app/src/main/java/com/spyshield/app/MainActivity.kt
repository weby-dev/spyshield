package com.spyshield.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.spyshield.app.data.Store
import com.spyshield.app.monitor.Notifications
import com.spyshield.app.monitor.SensorMonitorService
import com.spyshield.app.ui.AppRoot
import com.spyshield.app.ui.SpyShieldTheme
import com.spyshield.app.ui.Tab

class MainActivity : ComponentActivity() {

    private val requestedTab = mutableStateOf<Tab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        handleIntent(intent)
        if (Store.protectionOn.value) SensorMonitorService.start(this)

        setContent {
            SpyShieldTheme {
                AppRoot(requestedTab.value, onTabConsumed = { requestedTab.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(Notifications.EXTRA_TAB) == "activity") requestedTab.value = Tab.ACTIVITY
    }
}
