package com.aiham.quotes

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment

@RunWith(AndroidJUnit4::class)
class SplitPackagePrivateCopyInstrumentedTest {
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
    fun splitFixtureSurvivesRemovalOfNormallyInstalledSource() {
        assertTrue("BlackBox must be ready", engine.getEngineStatus() == "جاهز")

        val hostInfo = context.packageManager.getApplicationInfo(TEST_PACKAGE, 0)
        val hostSplits = hostInfo.splitSourceDirs.orEmpty()
        assertTrue(
            "Fixture must be installed by bundletool with at least one split APK",
            hostSplits.isNotEmpty()
        )

        val install = engine.installHostPackage(TEST_PACKAGE)
        assertTrue("Private split import failed: " + install.message, install.success)
        assertTrue("Virtual split fixture missing after import", engine.isInstalled(TEST_PACKAGE))

        val beforeUninstall = BlackBoxCore.getBPackageManager()
            .getApplicationInfo(TEST_PACKAGE, 0, 0)
        assertTrue("Virtual application info missing before source uninstall", beforeUninstall != null)
        assertPrivateCopy(beforeUninstall.sourceDir, beforeUninstall.splitSourceDirs.orEmpty(), hostSplits.size)

        val uninstallOutput = shell("pm uninstall " + TEST_PACKAGE)
        assertTrue(
            "Android shell failed to uninstall normal source package: " + uninstallOutput,
            uninstallOutput.contains("Success")
        )

        val deadline = System.currentTimeMillis() + 5000L
        while (engine.isHostPackageInstalled(TEST_PACKAGE, context) &&
            System.currentTimeMillis() < deadline) {
            Thread.sleep(100L)
        }

        assertFalse(
            "Normally installed source must disappear from Android PackageManager",
            engine.isHostPackageInstalled(TEST_PACKAGE, context)
        )
        assertTrue(
            "Virtual copy must survive removal of the normal source package",
            engine.isInstalled(TEST_PACKAGE)
        )

        val afterUninstall = BlackBoxCore.getBPackageManager()
            .getApplicationInfo(TEST_PACKAGE, 0, 0)
        assertTrue("Virtual application info missing after source uninstall", afterUninstall != null)
        assertPrivateCopy(afterUninstall.sourceDir, afterUninstall.splitSourceDirs.orEmpty(), hostSplits.size)

        val launch = engine.launchApp(TEST_PACKAGE, context)
        assertTrue("Private split fixture failed to launch: " + launch.message, launch.success)
        assertTrue(
            "Private split fixture process is not running after host source removal",
            engine.isVirtualProcessRunning(TEST_PACKAGE)
        )
    }

    private fun assertPrivateCopy(basePath: String, splitPaths: Array<out String>, expectedSplits: Int) {
        val privateRoot = BEnvironment.getAppDir(TEST_PACKAGE).canonicalFile
        val base = File(basePath).canonicalFile
        assertTrue("Private base APK missing: " + base, base.isFile)
        assertTrue(
            "Base APK escaped BlackBox private storage: " + base,
            base.path.startsWith(privateRoot.path + File.separator)
        )

        assertEquals("Unexpected private split count", expectedSplits, splitPaths.size)
        splitPaths.forEach { path ->
            val split = File(path).canonicalFile
            assertTrue("Private split APK missing: " + split, split.isFile)
            assertTrue(
                "Split APK escaped BlackBox private storage: " + split,
                split.path.startsWith(privateRoot.path + File.separator)
            )
        }
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    private companion object {
        const val TEST_PACKAGE = "com.aiham.virtualtest"
    }
}
