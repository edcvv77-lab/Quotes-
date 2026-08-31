package com.aiham.privatespace.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.aiham.privatespace.storage.StorageIsolationPolicy
import java.io.File
import java.util.UUID
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import top.niunaijun.blackbox.core.env.BEnvironment

class BlackBoxVirtualEngine : VirtualEngine {
    @Volatile private var attached = false
    @Volatile private var created = false
    @Volatile private var storageSafe = false

    override fun initialize(context: Context): EngineResult {
        if (attached) return EngineResult(true, "BlackBox attach already completed")
        return try {
            Log.i(
                TAG,
                "Attaching BlackBox with native Android context package=" + context.packageName +
                    " externalFiles=" + (context.getExternalFilesDir(null)?.absolutePath ?: "unavailable")
            )
            BlackBoxCore.get().doAttachBaseContext(context, object : ClientConfiguration() {
                override fun getHostPackageName(): String = context.packageName
                override fun isEnableDaemonService(): Boolean = false
            })
            attached = true
            EngineResult(true, "BlackBox attach completed")
        } catch (t: Throwable) {
            Log.e(TAG, "BlackBox attach failed", t)
            EngineResult(false, "تعذر تهيئة محرك التطبيقات الافتراضية.")
        }
    }

    override fun onApplicationCreate(): EngineResult {
        if (!attached) return EngineResult(false, "لم يكتمل ربط محرك التطبيقات الافتراضية.")
        if (created) return EngineResult(true, "BlackBox create already completed")
        return try {
            Log.i(TAG, "Completing BlackBox application lifecycle")
            BlackBoxCore.get().doCreate()
            created = true

            val storageResult = verifyStorageIsolation(BlackBoxCore.getContext())
            storageSafe = storageResult.success
            if (!storageSafe) {
                Log.w(
                    TAG,
                    "BlackBox storage safety verification could not prove path equivalence; " +
                        "continuing because FBlackBox uses host app-scoped directories"
                )
            }

            if (BlackBoxCore.get().isMainProcess) {
                val packages = BlackBoxCore.get().getInstalledPackages(0, USER_ID)
                Log.i(TAG, "BlackBox ready in main process; virtual package count=" + packages.size)
            } else {
                Log.i(TAG, "BlackBox lifecycle completed in auxiliary process")
            }
            EngineResult(true, "محرك التطبيقات الافتراضية جاهز.")
        } catch (t: Throwable) {
            created = false
            Log.e(TAG, "BlackBox create/service probe failed", t)
            EngineResult(false, "تعذر تشغيل خدمات التطبيقات الافتراضية.")
        }
    }

    override fun installApk(apkFile: File): EngineResult {
        if (!isReady()) return EngineResult(false, "محرك التطبيقات الافتراضية غير جاهز.")
        if (!apkFile.isFile || apkFile.length() <= 0L) return EngineResult(false, "ملف التطبيق غير صالح.")
        return try {
            val archiveInfo = BlackBoxCore.getPackageManager().getPackageArchiveInfo(apkFile.absolutePath, 0)
                ?: return EngineResult(false, "تعذر قراءة حزمة التطبيق.")
            val expectedPackage = archiveInfo.packageName
            Log.i(GUEST_TAG, "Installing guest package=" + expectedPackage + " bytes=" + apkFile.length())
            val result = BlackBoxCore.get().installPackageAsUser(apkFile, USER_ID)
            val installed = BlackBoxCore.get().isInstalled(expectedPackage, USER_ID)
            if (result != null && result.success && installed) {
                Log.i(GUEST_TAG, "Guest install verified package=" + expectedPackage)
                EngineResult(true, "تم تثبيت التطبيق داخل المساحة.", expectedPackage)
            } else {
                val detail = result?.msg ?: "BlackBox returned no error detail"
                Log.e(GUEST_TAG, "Guest install not verified package=" + expectedPackage + " detail=" + detail + " installed=" + installed)
                EngineResult(false, "تعذر تثبيت التطبيق داخل المساحة.", expectedPackage)
            }
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Guest install failed path=" + apkFile.absolutePath, t)
            EngineResult(false, "تعذر تثبيت التطبيق داخل المساحة.")
        }
    }

    override fun installHostPackage(packageName: String): EngineResult {
        if (!isReady()) {
            return EngineResult(false, "محرك التطبيقات الافتراضية غير جاهز.", packageName)
        }
        if (packageName.isBlank()) {
            return EngineResult(false, "تعذر تحديد التطبيق المثبت.")
        }

        var clusterDir: File? = null
        return try {
            val packageManager = BlackBoxCore.getPackageManager()
            val hostInfo = packageManager.getPackageInfo(packageName, 0)
            val hostApplication = hostInfo.applicationInfo
                ?: return EngineResult(false, "تعذر قراءة ملفات التطبيق المثبت.", packageName)

            val baseSource = File(hostApplication.sourceDir.orEmpty())
            if (!baseSource.isFile) {
                return EngineResult(false, "ملف التطبيق الأساسي غير متاح.", packageName)
            }

            val splitSources = hostApplication.splitSourceDirs.orEmpty()
                .map(::File)
                .filter { it.isFile }

            val declaredSplitCount = hostApplication.splitSourceDirs?.size ?: 0
            if (splitSources.size != declaredSplitCount) {
                Log.e(
                    GUEST_TAG,
                    "Installed package has missing split files package=" + packageName +
                        " declared=" + declaredSplitCount +
                        " readable=" + splitSources.size
                )
                return EngineResult(false, "تعذر قراءة جميع أجزاء التطبيق المثبت.", packageName)
            }

            clusterDir = File(
                BlackBoxCore.getContext().cacheDir,
                "private-import-" + UUID.randomUUID().toString()
            )
            check(clusterDir.mkdirs()) { "Unable to create private import staging directory" }

            baseSource.copyTo(File(clusterDir, "base.apk"), overwrite = true)
            splitSources.forEachIndexed { index, source ->
                val safeName = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                source.copyTo(
                    File(clusterDir, "split-" + index.toString().padStart(3, '0') + "-" + safeName),
                    overwrite = true
                )
            }

            Log.i(
                GUEST_TAG,
                "Importing installed package by private copy package=" + packageName +
                    " splitCount=" + declaredSplitCount +
                    " staging=" + clusterDir.absolutePath
            )

            val result = BlackBoxCore.get().installPackageAsUser(clusterDir, USER_ID)
            val installed = BlackBoxCore.get().isInstalled(packageName, USER_ID)
            if (result == null || !result.success || !installed) {
                val detail = result?.msg ?: "BlackBox returned no error detail"
                Log.e(
                    GUEST_TAG,
                    "Private package import failed package=" + packageName +
                        " detail=" + detail +
                        " installed=" + installed
                )
                return EngineResult(false, "تعذر نسخ التطبيق داخل المساحة.", packageName)
            }

            val virtualInfo = BlackBoxCore.getBPackageManager()
                .getApplicationInfo(packageName, 0, USER_ID)
                ?: run {
                    BlackBoxCore.get().uninstallPackageAsUser(packageName, USER_ID)
                    return EngineResult(false, "تعذر التحقق من النسخة داخل المساحة.", packageName)
                }

            val privateAppRoot = BEnvironment.getAppDir(packageName).canonicalFile
            val virtualBase = File(virtualInfo.sourceDir).canonicalFile
            val virtualSplits = virtualInfo.splitSourceDirs.orEmpty()
                .map { File(it).canonicalFile }

            val baseIsPrivate = isInside(privateAppRoot, virtualBase) && virtualBase.isFile
            val splitsArePrivate =
                virtualSplits.size == declaredSplitCount &&
                    virtualSplits.all { split -> isInside(privateAppRoot, split) && split.isFile }

            if (!baseIsPrivate || !splitsArePrivate) {
                Log.e(
                    GUEST_TAG,
                    "Private copy verification failed package=" + packageName +
                        " basePrivate=" + baseIsPrivate +
                        " expectedSplits=" + declaredSplitCount +
                        " actualSplits=" + virtualSplits.size
                )
                BlackBoxCore.get().uninstallPackageAsUser(packageName, USER_ID)
                return EngineResult(
                    false,
                    "تعذر تأمين نسخة التطبيق داخل التخزين الخاص.",
                    packageName
                )
            }

            Log.i(
                GUEST_TAG,
                "Private package import verified package=" + packageName +
                    " splitCount=" + virtualSplits.size +
                    " virtualBase=" + virtualBase.absolutePath
            )
            EngineResult(true, "تم نسخ التطبيق داخل المساحة.", packageName)
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Private installed-package import failed package=" + packageName, t)
            EngineResult(false, "تعذر نسخ التطبيق داخل المساحة.", packageName)
        } finally {
            clusterDir?.deleteRecursively()
        }
    }

    private fun isInside(root: File, candidate: File): Boolean {
        val rootPath = root.path.trimEnd(File.separatorChar)
        val candidatePath = candidate.path
        return candidatePath == rootPath ||
            candidatePath.startsWith(rootPath + File.separator)
    }

    override fun uninstallApp(packageName: String): EngineResult {
        if (!isReady()) return EngineResult(false, "محرك التطبيقات الافتراضية غير جاهز.", packageName)
        return try {
            if (!BlackBoxCore.get().isInstalled(packageName, USER_ID))
                return EngineResult(false, "التطبيق غير مثبت داخل المساحة.", packageName)
            Log.i(GUEST_TAG, "Uninstalling virtual package=" + packageName)
            BlackBoxCore.get().uninstallPackageAsUser(packageName, USER_ID)
            val stillInstalled = BlackBoxCore.get().isInstalled(packageName, USER_ID)
            if (!stillInstalled) {
                Log.i(GUEST_TAG, "Guest uninstall verified package=" + packageName)
                EngineResult(true, "تمت إزالة التطبيق من المساحة.", packageName)
            } else {
                Log.e(GUEST_TAG, "Guest uninstall could not be verified package=" + packageName)
                EngineResult(false, "تعذر إزالة التطبيق من المساحة.", packageName)
            }
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Guest uninstall failed package=" + packageName, t)
            EngineResult(false, "تعذر إزالة التطبيق من المساحة.", packageName)
        }
    }

    override fun listInstalledApps(): Result<List<String>> {
        if (!isReady()) return Result.failure(IllegalStateException("BlackBox is not ready"))
        return runCatching {
            val packages = BlackBoxCore.get().getInstalledPackages(0, USER_ID).map { it.packageName }.distinct().sorted()
            Log.i(TAG, "Virtual packages=" + packages.joinToString())
            packages
        }.onFailure { Log.e(TAG, "Failed to query virtual packages", it) }
    }

    override fun launchApp(packageName: String, context: Context): EngineResult {
        if (!isReady()) return EngineResult(false, "محرك التطبيقات الافتراضية غير جاهز.", packageName)

        return try {
            if (!BlackBoxCore.get().isInstalled(packageName, USER_ID)) {
                return EngineResult(false, "التطبيق غير مثبت داخل المساحة.", packageName)
            }

            val resolvedIntent = BlackBoxCore.getBPackageManager()
                .getLaunchIntentForPackage(packageName, USER_ID)
                ?: return EngineResult(false, "لا توجد شاشة تشغيل للتطبيق.", packageName)

            val launchIntent = Intent(resolvedIntent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Log.i(
                GUEST_TAG,
                "Launching virtual package=" + packageName +
                    " component=" + launchIntent.component +
                    " sdk=" + Build.VERSION.SDK_INT
            )

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                Log.i(GUEST_TAG, "Using direct BlackBox activity manager path for Android 11 or older")
                BlackBoxCore.getBActivityManager().startActivity(launchIntent, USER_ID)
            } else {
                val requested = BlackBoxCore.get().launchApk(packageName, USER_ID)
                if (!requested) {
                    return EngineResult(false, "تعذر إرسال طلب تشغيل التطبيق الافتراضي.", packageName)
                }

                if (!waitForVirtualProcess(packageName, PRIMARY_LAUNCH_WAIT_MS)) {
                    Log.w(
                        GUEST_TAG,
                        "LauncherActivity path did not start guest process; trying direct fallback package=" +
                            packageName
                    )
                    BlackBoxCore.getBActivityManager().startActivity(launchIntent, USER_ID)
                }
            }

            if (waitForVirtualProcess(packageName, FINAL_LAUNCH_WAIT_MS)) {
                Log.i(GUEST_TAG, "Guest process verified running package=" + packageName)
                EngineResult(true, "بدأت عملية التطبيق داخل المحرك.", packageName)
            } else {
                Log.e(GUEST_TAG, "Guest process did not start package=" + packageName)
                EngineResult(false, "لم يبدأ التطبيق داخل المحرك.", packageName)
            }
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Guest launch failed package=" + packageName, t)
            EngineResult(false, "تعذر تشغيل التطبيق الافتراضي.", packageName)
        }
    }

    override fun isInstalled(packageName: String): Boolean {
        if (!isReady()) return false
        return try { BlackBoxCore.get().isInstalled(packageName, USER_ID) }
        catch (t: Throwable) { Log.e(TAG, "Virtual install check failed package=" + packageName, t); false }
    }

    override fun isVirtualProcessRunning(packageName: String): Boolean {
        if (!isReady()) return false
        return try {
            val info = BlackBoxCore.getBActivityManager()
                .getRunningAppProcesses(packageName, USER_ID)
            info?.mAppProcessInfoList?.isNotEmpty() == true
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Virtual process check failed package=" + packageName, t)
            false
        }
    }

    private fun waitForVirtualProcess(packageName: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            if (isVirtualProcessRunning(packageName)) return true
            Thread.sleep(PROCESS_POLL_MS)
        } while (System.currentTimeMillis() < deadline)
        return isVirtualProcessRunning(packageName)
    }

    override fun isHostPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (t: Throwable) {
            Log.e(TAG, "Host package visibility check failed package=" + packageName, t)
            false
        }
    }

    override fun verifyStorageIsolation(context: Context): EngineResult {
        return try {
            val dataDir = context.applicationInfo.dataDir
            val appExternalFiles = context.getExternalFilesDir(null)?.canonicalPath
                ?: return EngineResult(false, "تعذر الوصول إلى تخزين التطبيق الخاص.")
            val virtualRoot = BEnvironment.getVirtualRoot().canonicalPath
            val externalRoot = BEnvironment.getExternalVirtualRoot().canonicalPath

            val virtualInternal = StorageIsolationPolicy.isInside(dataDir, virtualRoot)
            val externalAppScoped = StorageIsolationPolicy.isInside(appExternalFiles, externalRoot)

            Log.i(
                TAG,
                "Storage safety dataDir=" + dataDir +
                    " appExternalFiles=" + appExternalFiles +
                    " virtualRoot=" + virtualRoot +
                    " externalVirtualRoot=" + externalRoot +
                    " internalRoot=" + virtualInternal +
                    " appScopedExternal=" + externalAppScoped
            )

            if (virtualInternal && externalAppScoped) {
                EngineResult(true, "تخزين المساحة محصور داخل نطاق تطبيق اقتباسات.")
            } else {
                EngineResult(false, "تعذر تأمين نطاق تخزين المساحة الخاصة.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Storage safety verification failed", t)
            EngineResult(false, "تعذر التحقق من نطاق التخزين.")
        }
    }

    override fun getEngineStatus(): String {
        if (!attached) return "غير مهيأ"
        if (!created) return "التهيئة غير مكتملة"
        return try {
            BlackBoxCore.get().getInstalledPackages(0, USER_ID)
            "جاهز"
        } catch (t: Throwable) {
            Log.e(TAG, "Engine health probe failed", t)
            "غير متاح"
        }
    }

    private fun isReady(): Boolean = attached && created

    private companion object {
        const val TAG = "AIHAM_ENGINE"
        const val GUEST_TAG = "AIHAM_GUEST"
        const val USER_ID = 0
        const val PROCESS_POLL_MS = 100L
        const val PRIMARY_LAUNCH_WAIT_MS = 1200L
        const val FINAL_LAUNCH_WAIT_MS = 3000L
    }
}
