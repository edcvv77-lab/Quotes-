package com.aiham.quotes

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : top.niunaijun.blackbox.app.configuration.ClientConfiguration() {
                override fun getHostPackageName(): String = "com.aiham.quotes"
                override fun isHideRoot(): Boolean = true
                override fun isHideXposed(): Boolean = true
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
