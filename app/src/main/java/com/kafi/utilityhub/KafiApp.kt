package com.kafi.utilityhub

import android.app.Application
import com.kafi.utilityhub.util.NotificationHelper

class KafiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
