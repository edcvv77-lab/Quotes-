package com.aiham.quotes

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var tvQuotes: TextView
    private lateinit var tvPrivateStatus: TextView
    private val prefs by lazy { getSharedPreferences("quotes", MODE_PRIVATE) }
    private val quoteService by lazy { QuoteService(SharedPreferencesQuoteStore(prefs)) }
    private val virtualEngine: BlackBoxVirtualEngine by lazy { (application as AihamApp).virtualEngine }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showQuotesHome()
    }

    private fun showQuotesHome() {
        setContentView(R.layout.activity_main)
        tvQuotes = findViewById(R.id.tvQuotes)
        findViewById<Button>(R.id.btnAddQuote).setOnClickListener { showAddQuoteDialog() }
        renderQuotes()
    }

    private fun showAddQuoteDialog() {
        val input = EditText(this).apply {
            hint = "اكتب اقتباسك هنا"
            minLines = 3
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("إضافة اقتباس")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ -> handleSubmittedText(input.text.toString()) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun handleSubmittedText(raw: String) {
        when (val result = quoteService.submit(raw)) {
            QuoteSubmission.Empty -> Unit
            QuoteSubmission.OpenPrivateSpace -> enterPrivateSpace()
            is QuoteSubmission.Saved -> {
                renderQuotes()
                Toast.makeText(this, "تمت إضافة الاقتباس", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderQuotes() {
        val defaults = listOf(
            "الأفعال الصغيرة المتكررة تصنع نتائج كبيرة.",
            "لا تنتظر الوقت المثالي؛ اصنعه.",
            "كل خطوة واضحة أفضل من عشر خطوات متخيلة."
        )
        tvQuotes.text = (defaults + quoteService.getQuotes()).joinToString("\n\n") { "“$it”" }
    }

    private fun enterPrivateSpace() {
        setContentView(R.layout.activity_private_space)
        tvPrivateStatus = findViewById(R.id.tvPrivateStatus)
        refreshPrivateStatus()

        findViewById<Button>(R.id.btnInstallApk).setOnClickListener {
            runPrivateAction("جاري فحص وتثبيت التطبيق...") {
                val apk = extractTestApk()
                    ?: return@runPrivateAction "تعذر تجهيز ملف التطبيق التجريبي."
                val result = virtualEngine.installApk(apk)
                if (result.success) {
                    val pkg = result.packageName ?: TEST_PACKAGE
                    val hostVisible = virtualEngine.isHostPackageInstalled(pkg, this)
                    Log.i(GUEST_TAG, "Post-install host PackageManager visibility package=$pkg visible=$hostVisible")
                    result.message + if (hostVisible) "\nتحذير: الحزمة ظاهرة لمدير حزم النظام." else "\nالحزمة غير مثبتة كحزمة نظام مستقلة."
                } else result.message
            }
        }

        findViewById<Button>(R.id.btnListApps).setOnClickListener {
            runPrivateAction("جاري تحديث القائمة...") {
                virtualEngine.listInstalledApps().fold(
                    onSuccess = { apps -> if (apps.isEmpty()) "لا توجد تطبيقات افتراضية مثبتة." else apps.joinToString("\n") },
                    onFailure = { "تعذر قراءة قائمة التطبيقات الافتراضية." }
                )
            }
        }

        findViewById<Button>(R.id.btnLaunchApp).setOnClickListener {
            runPrivateAction("جاري طلب تشغيل التطبيق...") {
                virtualEngine.launchApp(TEST_PACKAGE, this).message
            }
        }

        findViewById<Button>(R.id.btnUninstallApp).setOnClickListener {
            runPrivateAction("جاري إزالة التطبيق...") {
                virtualEngine.uninstallApp(TEST_PACKAGE).message
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { showQuotesHome() }
    }

    private fun refreshPrivateStatus() {
        tvPrivateStatus.text = "حالة المحرك: ${virtualEngine.getEngineStatus()}"
    }

    private fun runPrivateAction(progressText: String, action: () -> String) {
        tvPrivateStatus.text = progressText
        Thread {
            val message = try {
                action()
            } catch (t: Throwable) {
                Log.e(SPACE_TAG, "Private-space action failed", t)
                "تعذر إكمال العملية."
            }
            runOnUiThread {
                tvPrivateStatus.text = message
            }
        }.start()
    }

    private fun extractTestApk(): File? {
        return try {
            val file = File(cacheDir, TEST_APK_NAME)
            assets.open(TEST_APK_NAME).use { input ->
                FileOutputStream(file, false).use { output -> input.copyTo(output) }
            }
            if (file.length() <= 0L) {
                Log.e(GUEST_TAG, "Extracted guest APK is empty")
                null
            } else {
                Log.i(GUEST_TAG, "Guest APK extracted path=${file.absolutePath} bytes=${file.length()}")
                file
            }
        } catch (t: Throwable) {
            Log.e(GUEST_TAG, "Guest APK extraction failed", t)
            null
        }
    }

    private companion object {
        const val SPACE_TAG = "AIHAM_SPACE"
        const val GUEST_TAG = "AIHAM_GUEST"
        const val TEST_PACKAGE = "com.aiham.virtualtest"
        const val TEST_APK_NAME = "AihamVirtualTest-debug.apk"
    }
}
