package com.aiham.privatespace.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

internal data class InstalledAppPolicyInput(
    val ownPackage: Boolean,
    val hasLauncher: Boolean,
    val systemApp: Boolean,
    val updatedSystemApp: Boolean
)

internal object InstalledAppPolicy {
    fun shouldShow(input: InstalledAppPolicyInput): Boolean {
        if (input.ownPackage || !input.hasLauncher) return false
        return !input.systemApp || input.updatedSystemApp
    }
}

data class InstalledAppCandidate(
    val packageName: String,
    val label: String,
    val baseApkPath: String,
    val splitApkPaths: List<String>
) {
    val usesSplitApks: Boolean
        get() = splitApkPaths.isNotEmpty()
}

class InstalledAppCloneManager(
    private val context: Context
) {
    fun listCloneCandidates(): Result<List<InstalledAppCandidate>> = runCatching {
        val packageManager = context.packageManager

        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { info ->
                InstalledAppPolicy.shouldShow(
                    InstalledAppPolicyInput(
                        ownPackage = info.packageName == context.packageName,
                        hasLauncher = packageManager.getLaunchIntentForPackage(info.packageName) != null,
                        systemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        updatedSystemApp = info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                    )
                )
            }
            .mapNotNull { info -> toCandidate(info, packageManager) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }.onFailure {
        Log.e(TAG, "Unable to enumerate installed applications", it)
    }

    fun findCloneCandidate(packageName: String): Result<InstalledAppCandidate> = runCatching {
        require(packageName.isNotBlank()) { "Installed application package name is missing" }
        val packageManager = context.packageManager
        val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        toCandidate(info, packageManager)
            ?: error("Installed application metadata is incomplete")
    }.onFailure {
        Log.e(TAG, "Unable to resolve installed application package=" + packageName, it)
    }

    fun loadIcon(packageName: String): Drawable? {
        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.onFailure {
            Log.w(TAG, "Unable to load installed application icon package=" + packageName, it)
        }.getOrNull()
    }

    private fun toCandidate(
        info: ApplicationInfo,
        packageManager: PackageManager
    ): InstalledAppCandidate? {
        val sourceDir = info.sourceDir?.takeIf { it.isNotBlank() }
            ?: return null

        val label = runCatching {
            info.loadLabel(packageManager)?.toString()
        }.getOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: info.packageName

        return InstalledAppCandidate(
            packageName = info.packageName,
            label = label,
            baseApkPath = sourceDir,
            splitApkPaths = info.splitSourceDirs.orEmpty()
                .filter { it.isNotBlank() }
                .distinct()
        )
    }

    private companion object {
        const val TAG = "AIHAM_SPACE"
    }
}
