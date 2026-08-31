package com.aiham.privatespace.permissions

import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

internal data class PermissionCandidate(
    val name: String,
    val dangerous: Boolean,
    val declaredByHost: Boolean,
    val granted: Boolean
)

internal object GuestPermissionPolicy {
    fun missingRuntimePermissions(candidates: List<PermissionCandidate>): List<String> =
        candidates.asSequence()
            .filter { it.dangerous && it.declaredByHost && !it.granted }
            .map { it.name }
            .distinct()
            .sorted()
            .toList()
}

data class GuestPermissionPlan(
    val requestedPermissions: List<String>,
    val missingRuntimePermissions: List<String>
)

class GuestPermissionManager(
    private val activity: Activity
) {
    fun buildPlan(apkFile: File): Result<GuestPermissionPlan> = runCatching {
        require(apkFile.isFile && apkFile.length() > 0L) { "Guest APK is missing" }

        val packageManager = activity.packageManager
        val archiveInfo = packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_PERMISSIONS
        ) ?: error("Unable to read guest APK permissions")

        val guestRequested = archiveInfo.requestedPermissions.orEmpty()
            .distinct()
            .sorted()

        val hostDeclared = packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty().toSet()

        val candidates = guestRequested.map { permission ->
            PermissionCandidate(
                name = permission,
                dangerous = isDangerousPermission(packageManager, permission),
                declaredByHost = permission in hostDeclared,
                granted = ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val missing = GuestPermissionPolicy.missingRuntimePermissions(candidates)

        Log.i(
            TAG,
            "Guest permission plan package=" + archiveInfo.packageName +
                " requested=" + guestRequested.size +
                " missingRuntime=" + missing.size
        )

        GuestPermissionPlan(
            requestedPermissions = guestRequested,
            missingRuntimePermissions = missing
        )
    }.onFailure {
        Log.e(TAG, "Failed to inspect guest permissions", it)
    }

    fun requestMissingRuntimePermissions(plan: GuestPermissionPlan, requestCode: Int) {
        if (plan.missingRuntimePermissions.isEmpty()) return

        Log.i(TAG, "Requesting guest runtime permissions count=" + plan.missingRuntimePermissions.size)
        ActivityCompat.requestPermissions(
            activity,
            plan.missingRuntimePermissions.toTypedArray(),
            requestCode
        )
    }

    private fun isDangerousPermission(
        packageManager: PackageManager,
        permission: String
    ): Boolean {
        return try {
            val info = packageManager.getPermissionInfo(permission, 0)
            val baseProtection = info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            baseProtection == PermissionInfo.PROTECTION_DANGEROUS
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private companion object {
        const val TAG = "AIHAM_SPACE"
    }
}
