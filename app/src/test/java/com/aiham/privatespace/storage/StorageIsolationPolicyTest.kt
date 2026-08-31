package com.aiham.privatespace.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageIsolationPolicyTest {
    @Test
    fun internalBlackBoxRootIsAccepted() {
        val dataDir = "/data/user/0/com.aiham.quotes"
        val blackBoxRoot = "/data/user/0/com.aiham.quotes/blackbox"

        assertTrue(StorageIsolationPolicy.isInside(dataDir, blackBoxRoot))
    }

    @Test
    fun appScopedExternalBlackBoxRootIsAccepted() {
        val appExternal = "/storage/emulated/0/Android/data/com.aiham.quotes/files"
        val guestRoot = "/storage/emulated/0/Android/data/com.aiham.quotes/files/blackbox"

        assertTrue(StorageIsolationPolicy.isInside(appExternal, guestRoot))
        assertFalse(StorageIsolationPolicy.isSharedPublicPath(guestRoot))
    }

    @Test
    fun sharedStorageOutsideHostAppScopeIsRejected() {
        assertTrue(
            StorageIsolationPolicy.isSharedPublicPath(
                "/storage/emulated/0/Download/com.whatsapp"
            )
        )
        assertTrue(
            StorageIsolationPolicy.isSharedPublicPath(
                "/sdcard/Pictures/com.whatsapp"
            )
        )
    }

    @Test
    fun android11DataAliasesAreTreatedAsSamePrivateDirectory() {
        assertTrue(
            StorageIsolationPolicy.isInside(
                "/data/user/0/com.aiham.quotes",
                "/data/data/com.aiham.quotes/blackbox"
            )
        )
    }

    @Test
    fun android11EmulatedStorageAliasesAreTreatedAsSameAppScope() {
        assertTrue(
            StorageIsolationPolicy.isInside(
                "/storage/emulated/0/Android/data/com.aiham.quotes/files",
                "/mnt/user/0/emulated/0/Android/data/com.aiham.quotes/files/blackbox"
            )
        )
    }

    @Test
    fun anotherAppsPrivateTreeIsNotInsideQuotesScope() {
        assertFalse(
            StorageIsolationPolicy.isInside(
                "/storage/emulated/0/Android/data/com.aiham.quotes/files",
                "/storage/emulated/0/Android/data/com.whatsapp/files"
            )
        )
    }
}
