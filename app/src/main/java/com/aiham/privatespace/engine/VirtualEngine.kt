package com.aiham.privatespace.engine

import android.content.Context
import java.io.File

interface VirtualEngine {
    fun initialize(context: Context)
    fun installApk(apkFile: File): Boolean
    fun uninstallApp(packageName: String): Boolean
    fun listInstalledApps(): List<String>
    fun launchApp(packageName: String, context: Context): Boolean
    fun isInstalled(packageName: String): Boolean
    fun getEngineStatus(): String
}
