package com.aiham.privatespace.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
            .mapNotNull { info ->
                val sourceDir = info.sourceDir?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val label = runCatching {
                    info.loadLabel(packageManager)?.toString()
                }.getOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: info.packageName

                InstalledAppCandidate(
                    packageName = info.packageName,
                    label = label,
                    baseApkPath = sourceDir,
                    splitApkPaths = info.splitSourceDirs.orEmpty()
                        .filter { it.isNotBlank() }
                        .distinct()
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }.onFailure {
        Log.e(TAG, "Unable to enumerate installed applications", it)
    }

    fun copyBaseApkForClone(candidate: InstalledAppCandidate): Result<File> = runCatching {
        require(!candidate.usesSplitApks) {
            "Split APK applications are not supported by the pinned BlackBox engine"
        }

        val source = File(candidate.baseApkPath)
        require(source.isFile && source.length() > 0L) {
            "Installed application base APK is unavailable"
        }

        val copy = File.createTempFile("installed-clone-", ".apk", context.cacheDir)
        FileInputStream(source).use { input ->
            FileOutputStream(copy, false).use { output ->
                input.copyTo(output)
            }
        }

        require(copy.length() == source.length()) {
            "Installed application APK copy is incomplete"
        }

        Log.i(
            TAG,
            "Prepared installed app clone package=" + candidate.packageName +
                " bytes=" + copy.length()
        )

        copy
    }.onFailure {
        Log.e(TAG, "Unable to prepare installed app clone package=" + candidate.packageName, it)
    }

    private companion object {
        const val TAG = "AIHAM_SPACE"
    }
}
