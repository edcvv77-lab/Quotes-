package com.aiham.privatespace

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private lateinit var tvQuotes: TextView
    private lateinit var tvPrivateStatus: TextView
    private val prefs by lazy { getSharedPreferences("quotes", MODE_PRIVATE) }

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
        val text = raw.trim()
        if (text.isEmpty()) return

        if (sha256(text) == SECRET_HASH) {
            enterPrivateSpace()
            return
        }

        val items = prefs.getStringSet("items", emptySet())!!.toMutableSet()
        items.add(text)
        prefs.edit().putStringSet("items", items).apply()
        renderQuotes()
        Toast.makeText(this, "تمت إضافة الاقتباس", Toast.LENGTH_SHORT).show()
    }

    private fun renderQuotes() {
        val defaults = listOf(
            "الأفعال الصغيرة المتكررة تصنع نتائج كبيرة.",
            "لا تنتظر الوقت المثالي؛ اصنعه.",
            "كل خطوة واضحة أفضل من عشر خطوات متخيلة."
        )
        val custom = prefs.getStringSet("items", emptySet())!!.toList()
        tvQuotes.text = (defaults + custom).joinToString("\n\n") { "“$it”" }
    }

    private fun enterPrivateSpace() {
        setContentView(R.layout.activity_private_space)
        tvPrivateStatus = findViewById(R.id.tvPrivateStatus)
        val engine = (application as AihamApp).virtualEngine
        tvPrivateStatus.text = "الحالة: ${engine.getEngineStatus()}"

        findViewById<Button>(R.id.btnInstallApk).setOnClickListener {
            val apk = extractTestApk()
            val ok = apk != null && engine.installApk(apk)
            tvPrivateStatus.text = if (ok) "تم تثبيت التطبيق داخل المساحة" else "فشل التثبيت"
        }
        findViewById<Button>(R.id.btnListApps).setOnClickListener {
            val apps = engine.listInstalledApps()
            tvPrivateStatus.text = if (apps.isEmpty()) "لا توجد تطبيقات افتراضية مثبتة" else apps.joinToString("\n")
        }
        findViewById<Button>(R.id.btnLaunchApp).setOnClickListener {
            val ok = engine.launchApp("com.aiham.virtualtest", this)
            tvPrivateStatus.text = if (ok) "تم تشغيل التطبيق داخل المساحة" else "تعذر تشغيل التطبيق"
        }
        findViewById<Button>(R.id.btnUninstallApp).setOnClickListener {
            val ok = engine.uninstallApp("com.aiham.virtualtest")
            tvPrivateStatus.text = if (ok) "تم إلغاء تثبيت التطبيق الافتراضي" else "فشل إلغاء التثبيت"
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener { showQuotesHome() }
    }

    private fun extractTestApk(): File? = try {
        val file = File(cacheDir, "AihamVirtualTest-debug.apk")
        if (file.exists() && file.length() > 0) return file
        assets.open("AihamVirtualTest-debug.apk").use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.takeIf { it.length() > 0 }
    } catch (_: Exception) { null }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        // SHA-256("aiham77rk"). This is a trigger, not a cryptographic access-control system.
        private const val SECRET_HASH = "1fe4beb5ba37284958745a8f36824d2cc794e7a4eef3d55215cb66d9a7c30a84"
    }
}
