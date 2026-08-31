package com.aiham.quotes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlackBoxRuntimeInstrumentedTest {
    private lateinit var context: Context
    private lateinit var engine: BlackBoxVirtualEngine
    private lateinit var installedTestPackage: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = (context.applicationContext as AihamApp).virtualEngine
        installedTestPackage = InstrumentationRegistry.getInstrumentation().context.packageName
        if (engine.isInstalled(installedTestPackage)) {
            engine.uninstallApp(installedTestPackage)
        }
        if (engine.isInstalled(TEST_PACKAGE)) {
            engine.uninstallApp(TEST_PACKAGE)
        }
    }

    @After
    fun cleanUp() {
        if (engine.isInstalled(installedTestPackage)) {
            engine.uninstallApp(installedTestPackage)
        }
        if (engine.isInstalled(TEST_PACKAGE)) {
            engine.uninstallApp(TEST_PACKAGE)
        }
    }


    @Test
    fun installedHostPackageIsCopiedIntoBlackBoxPrivateStorage() {
        assertTrue("BlackBox must be ready", engine.getEngineStatus() == "جاهز")
        assertTrue(
            "Instrumentation package must be visible to the Android package manager",
            engine.isHostPackageInstalled(installedTestPackage, context)
        )

        val hostInfo = context.packageManager.getApplicationInfo(installedTestPackage, 0)
        val install = engine.installHostPackage(installedTestPackage)
        assertTrue("Installed-package import failed: " + install.message, install.success)
        assertTrue(
            "Imported host package missing from BlackBox",
            engine.isInstalled(installedTestPackage)
        )

        val virtualInfo = top.niunaijun.blackbox.BlackBoxCore.getBPackageManager()
            .getApplicationInfo(installedTestPackage, 0, 0)
        assertTrue("Virtual application info missing", virtualInfo != null)

        val privateAppRoot = BEnvironment.getAppDir(installedTestPackage).canonicalFile
        val virtualBase = File(virtualInfo.sourceDir).canonicalFile
        assertTrue(
            "Imported base APK must live inside BlackBox private app storage: " + virtualBase,
            virtualBase.path == privateAppRoot.path ||
                virtualBase.path.startsWith(privateAppRoot.path + File.separator)
        )
        assertTrue("Private base APK must exist", virtualBase.isFile)
        assertNotEquals(
            "Virtual guest must not depend on the normally-installed source APK",
            File(hostInfo.sourceDir).canonicalPath,
            virtualBase.canonicalPath
        )

        val uninstall = engine.uninstallApp(installedTestPackage)
        assertTrue("Imported host package uninstall failed: " + uninstall.message, uninstall.success)
        assertFalse(
            "Imported host package still present after uninstall",
            engine.isInstalled(installedTestPackage)
        )
    }

    @Test
    fun installedSplitHostPackageRetainsPrivateSplitFilesWhenAvailable() {
        assertTrue("BlackBox must be ready", engine.getEngineStatus() == "جاهز")

        val packageManager = context.packageManager
        val candidate = packageManager.getInstalledApplications(0)
            .firstOrNull { app ->
                app.packageName != context.packageName &&
                    app.packageName != installedTestPackage &&
                    !app.splitSourceDirs.isNullOrEmpty() &&
                    packageManager.getLaunchIntentForPackage(app.packageName) != null
            }

        assumeTrue("Device has no launchable split APK package to exercise", candidate != null)
        val hostApp = candidate!!
        val hostSplits = hostApp.splitSourceDirs.orEmpty()
        assertTrue("Split candidate must contain at least one split", hostSplits.isNotEmpty())

        if (engine.isInstalled(hostApp.packageName)) {
            engine.uninstallApp(hostApp.packageName)
        }

        val install = engine.installHostPackage(hostApp.packageName)
        assertTrue(
            "Split host package import failed for " + hostApp.packageName + ": " + install.message,
            install.success
        )

        val virtualInfo = top.niunaijun.blackbox.BlackBoxCore.getBPackageManager()
            .getApplicationInfo(hostApp.packageName, 0, 0)
        assertTrue("Virtual split application info missing", virtualInfo != null)

        val privateAppRoot = BEnvironment.getAppDir(hostApp.packageName).canonicalFile
        val virtualSplits = virtualInfo.splitSourceDirs.orEmpty().map { File(it).canonicalFile }
        assertEquals(
            "Every host split must be copied into the private virtual package",
            hostSplits.size,
            virtualSplits.size
        )
        virtualSplits.forEach { split ->
            assertTrue("Private split APK missing: " + split, split.isFile)
            assertTrue(
                "Split APK escaped BlackBox private app storage: " + split,
                split.path.startsWith(privateAppRoot.path + File.separator)
            )
        }

        val uninstall = engine.uninstallApp(hostApp.packageName)
        assertTrue("Split package uninstall failed: " + uninstall.message, uninstall.success)
    }

    @Test
    fun guestLifecycleUsesBlackBoxAndNotHostPackageManager() {
        assertTrue("BlackBox must be ready", engine.getEngineStatus() == "جاهز")

        val storageIsolation = engine.verifyStorageIsolation(context)
        assertTrue(
            "Guest storage must be private: " + storageIsolation.message,
            storageIsolation.success
        )
        val hostDataDir = File(context.applicationInfo.dataDir).canonicalPath
        val virtualRoot = BEnvironment.getVirtualRoot().canonicalPath
        assertTrue(
            "BlackBox internal root escaped host app data: " + virtualRoot,
            virtualRoot == hostDataDir ||
                virtualRoot.startsWith(hostDataDir + File.separator)
        )

        val appExternalFiles = context.getExternalFilesDir(null)!!.canonicalPath
        val virtualExternalRoot = BEnvironment.getExternalVirtualRoot().canonicalPath
        assertTrue(
            "Virtual external storage escaped Quotes app-scoped storage: " + virtualExternalRoot,
            virtualExternalRoot == appExternalFiles ||
                virtualExternalRoot.startsWith(appExternalFiles + File.separator)
        )

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
        assertTrue("Virtual launch failed: " + launch.message, launch.success)
        assertTrue(
            "Guest process is not running after launch",
            engine.isVirtualProcessRunning(TEST_PACKAGE)
        )

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
