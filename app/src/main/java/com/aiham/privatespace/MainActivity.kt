package com.aiham.privatespace

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvEngineStatus: TextView
    private lateinit var tvLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEngineStatus = findViewById(R.id.tvEngineStatus)
        tvLogs = findViewById(R.id.tvLogs)

        val engine = (application as AihamApp).virtualEngine
        tvEngineStatus.text = "Engine Status: ${engine.getEngineStatus()}"

        findViewById<Button>(R.id.btnInstallApk).setOnClickListener {
            val apkFile = extractTestApk()
            if (apkFile != null) {
                log("Installing ${apkFile.absolutePath}...")
                val success = engine.installApk(apkFile)
                log("Install result: $success")
            } else {
                log("Failed to extract test APK.")
            }
        }

        findViewById<Button>(R.id.btnListApps).setOnClickListener {
            val apps = engine.listInstalledApps()
            log("Installed virtual apps:")
            if (apps.isEmpty()) log(" (none)")
            apps.forEach { log(" - $it") }
        }

        findViewById<Button>(R.id.btnLaunchApp).setOnClickListener {
            val pkg = "com.aiham.virtualtest"
            log("Launching $pkg...")
            val success = engine.launchApp(pkg, this)
            log("Launch result: $success")
        }

        findViewById<Button>(R.id.btnUninstallApp).setOnClickListener {
            val pkg = "com.aiham.virtualtest"
            log("Uninstalling $pkg...")
            val success = engine.uninstallApp(pkg)
            log("Uninstall result: $success")
        }
    }

    private fun log(message: String) {
        tvLogs.append("$message\n")
    }

    private fun extractTestApk(): File? {
        // In a real scenario, you might pick an APK from storage via ACTION_GET_CONTENT.
        // For this automated MVP test, we assume the test APK is placed in assets.
        // We will copy it to the cache directory and return the File.
        try {
            val fileName = "AihamVirtualTest-debug.apk"
            val outFile = File(cacheDir, fileName)
            if (outFile.exists()) return outFile

            val inputStream = assets.open(fileName)
            val outputStream = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            return outFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
