package com.aiham.privatespace.engine

import android.content.Context
import java.io.File

data class EngineResult(
    val success: Boolean,
    val message: String,
    val packageName: String? = null
)

interface VirtualEngine {
    fun initialize(context: Context): EngineResult
    fun onApplicationCreate(): EngineResult
    fun installApk(apkFile: File): EngineResult
    fun uninstallApp(packageName: String): EngineResult
    fun listInstalledApps(): Result<List<String>>
    fun launchApp(packageName: String, context: Context): EngineResult
    fun isInstalled(packageName: String): Boolean
    fun isVirtualProcessRunning(packageName: String): Boolean
    fun isHostPackageInstalled(packageName: String, context: Context): Boolean
    fun verifyStorageIsolation(context: Context): EngineResult
    fun getEngineStatus(): String
}
