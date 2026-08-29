package com.aiham.quotes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlackBoxRuntimeInstrumentedTest {
    private lateinit var context: Context
    private lateinit var engine: BlackBoxVirtualEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = (context.applicationContext as AihamApp).virtualEngine
        if (engine.isInstalled(TEST_PACKAGE)) {
            engine.uninstallApp(TEST_PACKAGE)
        }
    }

    @After
    fun cleanUp() {
        if (engine.isInstalled(TEST_PACKAGE)) {
            engine.uninstallApp(TEST_PACKAGE)
        }
    }

    @Test
    fun guestLifecycleUsesBlackBoxAndNotHostPackageManager() {
        assertTrue("BlackBox must be ready", engine.getEngineStatus() == "جاهز")

        val guestApk = extractBundledGuest()
        assertTrue("Bundled guest APK must exist", guestApk.isFile && guestApk.length() > 0L)

        val install = engine.installApk(guestApk)
        assertTrue("Virtual install failed: " + install.message, install.success)
        assertTrue("Guest missing from BlackBox after install", engine.isInstalled(TEST_PACKAGE))

        val packages = engine.listInstalledApps().getOrThrow()
        assertTrue("Guest missing from virtual package list", packages.contains(TEST_PACKAGE))

        assertFalse(
            "Guest must not be installed as an independent host package",
            engine.isHostPackageInstalled(TEST_PACKAGE, context)
        )

        val launch = engine.launchApp(TEST_PACKAGE, context)
        assertTrue("Virtual launch request failed: " + launch.message, launch.success)

        val uninstall = engine.uninstallApp(TEST_PACKAGE)
        assertTrue("Virtual uninstall failed: " + uninstall.message, uninstall.success)
        assertFalse("Guest still present after uninstall", engine.isInstalled(TEST_PACKAGE))
    }

    private fun extractBundledGuest(): File {
        val out = File(context.cacheDir, TEST_APK_NAME)
        context.assets.open(TEST_APK_NAME).use { input ->
            FileOutputStream(out, false).use { output -> input.copyTo(output) }
        }
        return out
    }

    private companion object {
        const val TEST_PACKAGE = "com.aiham.virtualtest"
        const val TEST_APK_NAME = "AihamVirtualTest-debug.apk"
    }
}
