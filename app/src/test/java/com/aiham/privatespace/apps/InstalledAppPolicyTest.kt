package com.aiham.privatespace.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppPolicyTest {
    @Test
    fun userLaunchableAppIsShown() {
        assertTrue(
            InstalledAppPolicy.shouldShow(
                InstalledAppPolicyInput(
                    ownPackage = false,
                    hasLauncher = true,
                    systemApp = false,
                    updatedSystemApp = false
                )
            )
        )
    }

    @Test
    fun hostAndNonLaunchableAppsAreHidden() {
        assertFalse(
            InstalledAppPolicy.shouldShow(
                InstalledAppPolicyInput(
                    ownPackage = true,
                    hasLauncher = true,
                    systemApp = false,
                    updatedSystemApp = false
                )
            )
        )

        assertFalse(
            InstalledAppPolicy.shouldShow(
                InstalledAppPolicyInput(
                    ownPackage = false,
                    hasLauncher = false,
                    systemApp = false,
                    updatedSystemApp = false
                )
            )
        )
    }

    @Test
    fun systemAppsAreHiddenUnlessUpdatedUserFacingApps() {
        assertFalse(
            InstalledAppPolicy.shouldShow(
                InstalledAppPolicyInput(
                    ownPackage = false,
                    hasLauncher = true,
                    systemApp = true,
                    updatedSystemApp = false
                )
            )
        )

        assertTrue(
            InstalledAppPolicy.shouldShow(
                InstalledAppPolicyInput(
                    ownPackage = false,
                    hasLauncher = true,
                    systemApp = true,
                    updatedSystemApp = true
                )
            )
        )
    }
}
