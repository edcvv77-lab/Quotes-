package com.aiham.privatespace.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageIsolationPolicyTest {
    @Test
    fun internalGuestStorageIsAccepted() {
        val dataDir = "/data/user/0/com.aiham.quotes"
        val guest = "/data/user/0/com.aiham.quotes/files/secure-space/external-files/blackbox"

        assertTrue(StorageIsolationPolicy.isInternal(dataDir, guest))
        assertFalse(StorageIsolationPolicy.isPublicStorage(guest))
    }

    @Test
    fun androidDataAndSdcardAreRejectedAsPublicStorage() {
        val dataDir = "/data/user/0/com.aiham.quotes"
        val external = "/storage/emulated/0/Android/data/com.aiham.quotes/files/blackbox"

        assertFalse(StorageIsolationPolicy.isInternal(dataDir, external))
        assertTrue(StorageIsolationPolicy.isPublicStorage(external))
        assertTrue(StorageIsolationPolicy.isPublicStorage("/sdcard/Android/data/com.whatsapp"))
    }

    @Test
    fun otherApplicationDataDirIsRejected() {
        assertFalse(
            StorageIsolationPolicy.isInternal(
                "/data/user/0/com.aiham.quotes",
                "/data/user/0/com.whatsapp/files"
            )
        )
    }
}
