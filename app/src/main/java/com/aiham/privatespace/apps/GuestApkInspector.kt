package com.aiham.privatespace.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class GuestApkMetadata(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val apkFile: File
)

class GuestApkInspector(
    private val context: Context
) {
    fun copyAndInspect(uri: Uri): Result<GuestApkMetadata> = runCatching {
        val destination = File.createTempFile("guest-import-", ".apk", context.cacheDir)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination, false).use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open selected APK")

        require(destination.length() > 0L) { "Selected APK is empty" }

        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(
            destination.absolutePath,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
        ) ?: error("Selected file is not a readable APK")

        val applicationInfo = packageInfo.applicationInfo
            ?: error("APK has no application metadata")

        applicationInfo.sourceDir = destination.absolutePath
        applicationInfo.publicSourceDir = destination.absolutePath

        val packageName = packageInfo.packageName
            ?.takeIf { it.isNotBlank() }
            ?: error("APK package name is missing")

        val label = runCatching {
            applicationInfo.loadLabel(packageManager)?.toString()
        }.getOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: packageName

        val icon = runCatching {
            applicationInfo.loadIcon(packageManager)
        }.onFailure {
            Log.w(TAG, "Unable to load imported APK icon package=" + packageName, it)
        }.getOrNull()

        GuestApkMetadata(
            packageName = packageName,
            label = label,
            icon = icon,
            apkFile = destination
        )
    }.onFailure {
        Log.e(TAG, "Failed to inspect selected APK", it)
    }

    private companion object {
        const val TAG = "AIHAM_SPACE"
    }
}
