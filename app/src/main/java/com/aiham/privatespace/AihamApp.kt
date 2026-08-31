package com.aiham.quotes

import android.app.Application
import android.content.Context
import android.util.Log
import com.aiham.privatespace.engine.BlackBoxVirtualEngine

class AihamApp : Application() {
    val virtualEngine = BlackBoxVirtualEngine()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val result = virtualEngine.initialize(this)
        if (!result.success) {
            Log.e(TAG, "BlackBox attach failed: " + result.message)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val result = virtualEngine.onApplicationCreate()
        if (!result.success) {
            Log.e(TAG, "BlackBox create failed: " + result.message)
        }
    }

    private companion object {
        const val TAG = "AIHAM_ENGINE"
    }
}
