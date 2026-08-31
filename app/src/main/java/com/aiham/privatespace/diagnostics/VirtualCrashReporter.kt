package com.aiham.privatespace.diagnostics

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.BActivityThread

object VirtualCrashReporter {
    private const val TAG = "AIHAM_CRASH"
    private const val REPORT_FILE = "last_virtual_crash.txt"
    private const val TARGET_FILE = "last_virtual_launch.txt"
    private const val MAX_STACK_CHARS = 28_000

    fun install(context: Context) {
        runCatching {
            BlackBoxCore.get().setExceptionHandler { thread, throwable ->
                record(context.applicationContext, thread, throwable)
            }
            Log.i(TAG, "Virtual crash capture installed")
        }.onFailure {
            Log.e(TAG, "Unable to install virtual crash capture", it)
        }
    }

    fun markLaunch(context: Context, packageName: String, label: String) {
        runCatching {
            reportFile(context).delete()
            targetFile(context).writeText(
                buildString {
                    appendLine("package=$packageName")
                    appendLine("label=$label")
                    appendLine("time=${System.currentTimeMillis()}")
                }
            )
        }.onFailure {
            Log.w(TAG, "Unable to record launch target", it)
        }
    }

    fun readLastReport(context: Context): String? =
        runCatching {
            reportFile(context)
                .takeIf { it.isFile && it.length() > 0L }
                ?.readText()
        }.getOrNull()

    private fun record(context: Context, thread: Thread, throwable: Throwable) {
        runCatching {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            val stack = writer.toString().take(MAX_STACK_CHARS)
            val guestPackage = runCatching { BActivityThread.getAppPackageName() }.getOrNull()
            val guestProcess = runCatching { BActivityThread.getAppProcessName() }.getOrNull()
            val processName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Application.getProcessName()
                } else {
                    "pid:${Process.myPid()}"
                }
            val launchTarget = runCatching {
                targetFile(context).takeIf { it.isFile }?.readText()?.trim()
            }.getOrNull().orEmpty()
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS Z",
                Locale.US
            ).format(Date())

            reportFile(context).writeText(
                buildString {
                    appendLine("Aiham Quotes virtual-runtime crash")
                    appendLine("timestamp=$timestamp")
                    appendLine("sdk=${Build.VERSION.SDK_INT}")
                    appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("hostPackage=${context.packageName}")
                    appendLine("process=$processName")
                    appendLine("pid=${Process.myPid()}")
                    appendLine("thread=${thread.name}")
                    appendLine("guestPackage=${guestPackage.orEmpty()}")
                    appendLine("guestProcess=${guestProcess.orEmpty()}")
                    if (launchTarget.isNotBlank()) {
                        appendLine("--- requested launch ---")
                        appendLine(launchTarget)
                    }
                    appendLine("--- exception ---")
                    appendLine(stack)
                }
            )
        }.onFailure {
            Log.e(TAG, "Unable to persist virtual crash report", it)
        }
    }

    private fun reportFile(context: Context): File =
        File(context.filesDir, REPORT_FILE)

    private fun targetFile(context: Context): File =
        File(context.filesDir, TARGET_FILE)
}
