package com.aiham.privatespace.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class GuestPermissionPolicyTest {
    @Test
    fun onlyMissingDangerousPermissionsDeclaredByHostAreRequested() {
        val result = GuestPermissionPolicy.missingRuntimePermissions(
            listOf(
                PermissionCandidate("camera", dangerous = true, declaredByHost = true, granted = false),
                PermissionCandidate("microphone", dangerous = true, declaredByHost = true, granted = true),
                PermissionCandidate("custom", dangerous = true, declaredByHost = false, granted = false),
                PermissionCandidate("internet", dangerous = false, declaredByHost = true, granted = false)
            )
        )

        assertEquals(listOf("camera"), result)
    }

    @Test
    fun duplicatePermissionsAreRequestedOnlyOnce() {
        val result = GuestPermissionPolicy.missingRuntimePermissions(
            listOf(
                PermissionCandidate("camera", dangerous = true, declaredByHost = true, granted = false),
                PermissionCandidate("camera", dangerous = true, declaredByHost = true, granted = false)
            )
        )

        assertEquals(listOf("camera"), result)
    }
}
