package com.aiham.quotes

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instance = this
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return base.packageName
                }
                override fun isHideRoot(): Boolean {
                    return true
                }
                override fun isHideXposed(): Boolean {
                    return true
                }
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            BlackBoxCore.get().doCreate()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
