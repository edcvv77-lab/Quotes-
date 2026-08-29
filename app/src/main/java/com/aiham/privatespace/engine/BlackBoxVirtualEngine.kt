package com.aiham.privatespace.engine

import android.content.Context
import java.io.File
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

class BlackBoxVirtualEngine : VirtualEngine {
    private var isInitialized = false

    override fun initialize(context: Context) {
        if (!isInitialized) {
            BlackBoxCore.get().doAttachBaseContext(context, object : ClientConfiguration() {
                override fun getHostPackageName(): String = context.packageName
            })
            isInitialized = true
        }
    }

    override fun installApk(apkFile: File): Boolean {
        if (!isInitialized) return false
        val result = BlackBoxCore.get().installPackageAsUser(apkFile, 0)
        return result.success
    }

    override fun uninstallApp(packageName: String): Boolean {
        if (!isInitialized) return false
        BlackBoxCore.get().uninstallPackageAsUser(packageName, 0)
        return true
    }

    override fun listInstalledApps(): List<String> {
        if (!isInitialized) return emptyList()
        return BlackBoxCore.get().getInstalledPackages(0, 0).map { it.packageName }
    }

    override fun launchApp(packageName: String, context: Context): Boolean {
        if (!isInitialized) return false
        return BlackBoxCore.get().launchApk(packageName, 0)
    }

    override fun isInstalled(packageName: String): Boolean {
        return listInstalledApps().contains(packageName)
    }

    override fun getEngineStatus(): String {
        return if (isInitialized) "Initialized (FBlackBox)" else "Not Initialized"
    }
}
