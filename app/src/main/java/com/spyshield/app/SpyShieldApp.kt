package com.spyshield.app

import android.app.Application
import com.spyshield.app.data.Store
import com.spyshield.app.data.TrustedApps
import com.spyshield.app.monitor.Notifications

class SpyShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        TrustedApps.init(this)
        Notifications.createChannels(this)
    }
}
