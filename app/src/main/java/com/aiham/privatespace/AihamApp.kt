package com.aiham.quotes

import android.app.Application
import android.content.Context
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import top.niunaijun.blackbox.BlackBoxCore

class AihamApp : Application() {
    val virtualEngine = BlackBoxVirtualEngine()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        virtualEngine.initialize(this)
    }

    override fun onCreate() {
        super.onCreate()
        BlackBoxCore.get().doCreate()
    }
}
