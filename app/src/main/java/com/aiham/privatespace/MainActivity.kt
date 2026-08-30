package com.aiham.quotes

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aiham.privatespace.apps.GuestApkInspector
import com.aiham.privatespace.apps.GuestApkMetadata
import com.aiham.privatespace.apps.GuestAppCatalog
import com.aiham.privatespace.apps.InstalledAppCandidate
import com.aiham.privatespace.apps.InstalledAppCloneManager
import com.aiham.privatespace.engine.BlackBoxVirtualEngine
import com.aiham.privatespace.permissions.GuestPermissionManager
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var tvQuotes: TextView
    private lateinit var tvPrivateStatus: TextView
    private lateinit var appsContainer: LinearLayout

    private val prefs by lazy { getSharedPreferences("quotes", MODE_PRIVATE) }
    private val quoteService by lazy { QuoteService(SharedPreferencesQuoteStore(prefs)) }
    private val virtualEngine: BlackBoxVirtualEngine by lazy { (application as AihamApp).virtualEngine }
    private val guestPermissionManager by lazy { GuestPermissionManager(this) }
    private val guestApkInspector by lazy { GuestApkInspector(this) }
    private val guestAppCatalog by lazy { GuestAppCatalog(this) }
    private val installedAppCloneManager by lazy { InstalledAppCloneManager(this) }

    private var pendingGuestApk: File? = null
    private var inPrivateSpace = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingGuestApk = savedInstanceState
            ?.getString(STATE_PENDING_APK)
            ?.let(::File)
            ?.takeIf { it.isFile }

        if (savedInstanceState?.getBoolean(STATE_PRIVATE_SPACE) == true) {
            enterPrivateSpace()
        } else {
            showQuotesHome()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PRIVATE_SPACE, inPrivateSpace)
        pendingGuestApk?.absolutePath?.let { outState.putString(STATE_PENDING_APK, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (inPrivateSpace) {
            showQuotesHome()
        } else {
            super.onBackPressed()
        }
    }

    private fun showQuotesHome() {
        inPrivateSpace = false
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

        tvQuotes.text = (defaults + quoteService.getQuotes())
            .joinToString("\n\n") { "“" + it + "”" }
    }

    private fun enterPrivateSpace() {
        inPrivateSpace = true
        setContentView(R.layout.activity_private_space)

        tvPrivateStatus = findViewById(R.id.tvPrivateStatus)
        appsContainer = findViewById(R.id.appsContainer)

        findViewById<Button>(R.id.btnPickApk).setOnClickListener { openApkPicker() }
        findViewById<Button>(R.id.btnCloneInstalledApp).setOnClickListener { showInstalledAppPicker() }
        findViewById<Button>(R.id.btnRefreshApps).setOnClickListener { refreshVirtualApps() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { showQuotesHome() }

        refreshPrivateStatus()
        refreshVirtualApps()
    }

    private fun openApkPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        try {
            startActivityForResult(intent, REQUEST_PICK_APK)
        } catch (t: Throwable) {
            Log.e(SPACE_TAG, "Unable to open document picker", t)
            tvPrivateStatus.text = "تعذر فتح مدير الملفات."
        }
    }

    @Deprecated("Legacy result API retained for Android 11 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_PICK_APK || resultCode != RESULT_OK) return

        val uri = data?.data ?: run {
            tvPrivateStatus.text = "لم يتم اختيار ملف."
            return
        }

        prepareImportedApk(uri)
    }

    private fun prepareImportedApk(uri: Uri) {
        tvPrivateStatus.text = "جاري فحص ملف APK..."

        Thread {
            val metadata = guestApkInspector.copyAndInspect(uri).getOrElse { error ->
                Log.e(SPACE_TAG, "Selected file is not a usable APK", error)
                runOnUiThread {
                    if (inPrivateSpace) tvPrivateStatus.text = "الملف المختار ليس APK صالحًا."
                }
                return@Thread
            }

            prepareGuestForInstall(metadata)
        }.start()
    }

    private fun showInstalledAppPicker() {
        if (!inPrivateSpace) return

        tvPrivateStatus.text = "جاري قراءة التطبيقات المثبتة..."

        Thread {
            val result = installedAppCloneManager.listCloneCandidates()

            runOnUiThread {
                if (!inPrivateSpace) return@runOnUiThread

                result.fold(
                    onSuccess = { apps ->
                        if (apps.isEmpty()) {
                            tvPrivateStatus.text = "لم يتم العثور على تطبيقات قابلة للنسخ."
                            return@fold
                        }

                        val labels = apps.map { app ->
                            buildString {
                                append(app.label)
                                append("\n")
                                append(app.packageName)
                                if (app.usesSplitApks) {
                                    append("  •  Split APK")
                                }
                            }
                        }.toTypedArray()

                        AlertDialog.Builder(this)
                            .setTitle("اختر تطبيقًا لنسخه")
                            .setItems(labels) { _, which ->
                                val selected = apps[which]
                                if (selected.usesSplitApks) {
                                    showSplitApkUnsupported(selected)
                                } else {
                                    cloneInstalledApp(selected)
                                }
                            }
                            .setNegativeButton("إلغاء", null)
                            .show()

                        tvPrivateStatus.text = "اختر التطبيق الذي تريد نسخه إلى المساحة."
                    },
                    onFailure = { error ->
                        Log.e(SPACE_TAG, "Unable to enumerate installed apps", error)
                        tvPrivateStatus.text = "تعذر قراءة التطبيقات المثبتة على الجهاز."
                    }
                )
            }
        }.start()
    }

    private fun showSplitApkUnsupported(app: InstalledAppCandidate) {
        Log.w(
            SPACE_TAG,
            "Clone blocked for split APK package=" + app.packageName +
                " splits=" + app.splitApkPaths.size
        )

        AlertDialog.Builder(this)
            .setTitle("هذا التطبيق يستخدم Split APK")
            .setMessage(
                app.label + " يعتمد على " + app.splitApkPaths.size +
                    " جزء إضافي إلى جانب base.apk. إصدار BlackBox الحالي لا يحمّل هذه الأجزاء " +
                    "بشكل صحيح، لذلك تم إيقاف النسخ بدل تثبيت نسخة ناقصة."
            )
            .setPositiveButton("حسنًا", null)
            .show()

        tvPrivateStatus.text = "تعذر نسخ " + app.label + " بأمان لأنه يستخدم Split APK."
    }

    private fun cloneInstalledApp(app: InstalledAppCandidate) {
        if (!inPrivateSpace) return

        tvPrivateStatus.text = "جاري تجهيز نسخة مستقلة من " + app.label + "..."

        Thread {
            val copiedApk = installedAppCloneManager.copyBaseApkForClone(app).getOrElse { error ->
                Log.e(SPACE_TAG, "Installed app clone copy failed package=" + app.packageName, error)
                runOnUiThread {
                    if (inPrivateSpace) {
                        tvPrivateStatus.text = "تعذر نسخ ملف التطبيق من الجهاز."
                    }
                }
                return@Thread
            }

            val metadata = guestApkInspector.inspectFile(copiedApk).getOrElse { error ->
                Log.e(SPACE_TAG, "Copied installed app APK could not be inspected", error)
                copiedApk.delete()
                runOnUiThread {
                    if (inPrivateSpace) {
                        tvPrivateStatus.text = "تعذر قراءة نسخة التطبيق المجهزة."
                    }
                }
                return@Thread
            }

            prepareGuestForInstall(metadata)
        }.start()
    }

    private fun prepareGuestForInstall(metadata: GuestApkMetadata) {
        if (virtualEngine.isInstalled(metadata.packageName)) {
            metadata.apkFile.delete()
            runOnUiThread {
                if (inPrivateSpace) {
                    tvPrivateStatus.text =
                        "التطبيق موجود داخل المساحة بالفعل. احذفه أولًا إذا أردت تثبيت نسخة أخرى."
                }
            }
            return
        }

        val plan = guestPermissionManager.buildPlan(metadata.apkFile).getOrElse { error ->
            Log.e(SPACE_TAG, "Unable to build guest permission plan", error)
            metadata.apkFile.delete()
            runOnUiThread {
                if (inPrivateSpace) tvPrivateStatus.text = "تعذر فحص أذونات التطبيق."
            }
            return
        }

        runOnUiThread {
            if (!inPrivateSpace) {
                metadata.apkFile.delete()
                return@runOnUiThread
            }

            if (plan.missingRuntimePermissions.isEmpty()) {
                installImportedGuest(metadata, permissionsDenied = false)
            } else {
                pendingGuestApk = metadata.apkFile
                tvPrivateStatus.text =
                    "يحتاج " + metadata.label + " إلى بعض الأذونات قبل التشغيل."
                guestPermissionManager.requestMissingRuntimePermissions(
                    plan,
                    REQUEST_GUEST_PERMISSIONS
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_GUEST_PERMISSIONS) return

        val apk = pendingGuestApk
        pendingGuestApk = null

        if (apk == null || !apk.isFile) {
            Log.w(SPACE_TAG, "Permission result received without pending guest APK")
            if (inPrivateSpace) tvPrivateStatus.text = "تعذر متابعة تثبيت التطبيق."
            return
        }

        val denied = permissions.indices.any { index ->
            grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
        }

        if (denied) {
            Log.w(SPACE_TAG, "One or more guest runtime permissions were denied")
        }

        val metadata = guestApkInspector.inspectFile(apk).getOrElse { error ->
            Log.e(SPACE_TAG, "Pending APK could not be re-read", error)
            apk.delete()
            if (inPrivateSpace) tvPrivateStatus.text = "تعذر إعادة قراءة ملف التطبيق."
            return
        }

        installImportedGuest(metadata, permissionsDenied = denied)
    }

    private fun installImportedGuest(
        metadata: GuestApkMetadata,
        permissionsDenied: Boolean
    ) {
        runPrivateAction(
            progressText = "جاري تثبيت " + metadata.label + " داخل المساحة...",
            action = {
                val hostWasInstalled = virtualEngine.isHostPackageInstalled(
                    metadata.packageName,
                    this
                )

                val result = virtualEngine.installApk(metadata.apkFile)
                if (!result.success) {
                    return@runPrivateAction result.message
                }

                guestAppCatalog.save(
                    packageName = metadata.packageName,
                    label = metadata.label,
                    icon = metadata.icon
                )

                val hostVisibleAfter = virtualEngine.isHostPackageInstalled(
                    metadata.packageName,
                    this
                )

                Log.i(
                    GUEST_TAG,
                    "Post-install host visibility package=" + metadata.packageName +
                        " before=" + hostWasInstalled +
                        " after=" + hostVisibleAfter
                )

                buildString {
                    append("تم تثبيت ")
                    append(metadata.label)
                    append(" داخل المساحة.")

                    if (permissionsDenied) {
                        append("\nتم رفض بعض الأذونات؛ قد لا تعمل بعض الوظائف.")
                    }

                    when {
                        hostWasInstalled ->
                            append("\nيوجد تطبيق نظامي من نفس الحزمة مسبقًا؛ النسخة داخل المساحة مستقلة عنه.")
                        hostVisibleAfter ->
                            append("\nتحذير: ظهرت الحزمة لمدير حزم النظام بعد التثبيت.")
                        else ->
                            append("\nلم يتم تثبيت الحزمة كتطبيق نظام مستقل.")
                    }
                }
            },
            after = {
                metadata.apkFile.delete()
                refreshVirtualApps()
            }
        )
    }

    private fun refreshVirtualApps() {
        if (!inPrivateSpace) return

        tvPrivateStatus.text = "جاري تحديث التطبيقات..."

        Thread {
            val result = virtualEngine.listInstalledApps()

            runOnUiThread {
                if (!inPrivateSpace) return@runOnUiThread

                result.fold(
                    onSuccess = { packages ->
                        renderVirtualApps(packages)
                        tvPrivateStatus.text =
                            if (packages.isEmpty()) {
                                "المساحة جاهزة. لا توجد تطبيقات مثبتة."
                            } else {
                                "المساحة جاهزة. التطبيقات: " + packages.size
                            }
                    },
                    onFailure = { error ->
                        Log.e(SPACE_TAG, "Unable to refresh virtual app list", error)
                        appsContainer.removeAllViews()
                        tvPrivateStatus.text = "تعذر قراءة التطبيقات داخل المساحة."
                    }
                )
            }
        }.start()
    }

    private fun renderVirtualApps(packages: List<String>) {
        appsContainer.removeAllViews()

        if (packages.isEmpty()) {
            val empty = TextView(this).apply {
                text = "أضف APK أو انسخ تطبيقًا مثبتًا من الجهاز إلى المساحة الخاصة."
                textSize = 15f
                setTextColor(0xFF94A3B8.toInt())
                setPadding(12, 32, 12, 32)
            }
            appsContainer.addView(empty)
            return
        }

        val inflater = LayoutInflater.from(this)

        packages.forEach { packageName ->
            val record = guestAppCatalog.get(packageName)
            val row = inflater.inflate(R.layout.item_guest_app, appsContainer, false)

            row.findViewById<TextView>(R.id.tvGuestLabel).text = record.label
            row.findViewById<TextView>(R.id.tvGuestPackage).text = record.packageName

            val iconView = row.findViewById<ImageView>(R.id.ivGuestIcon)
            val icon = record.iconPath?.let(Drawable::createFromPath)
            if (icon != null) {
                iconView.setImageDrawable(icon)
            } else {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            row.findViewById<Button>(R.id.btnGuestLaunch).setOnClickListener {
                launchVirtualApp(record.packageName, record.label)
            }

            row.findViewById<Button>(R.id.btnGuestRemove).setOnClickListener {
                confirmRemoveVirtualApp(record.packageName, record.label)
            }

            appsContainer.addView(row)
        }
    }

    private fun launchVirtualApp(packageName: String, label: String) {
        runPrivateAction(
            progressText = "جاري تشغيل " + label + "...",
            action = {
                virtualEngine.launchApp(packageName, this).message
            }
        )
    }

    private fun confirmRemoveVirtualApp(packageName: String, label: String) {
        AlertDialog.Builder(this)
            .setTitle("إزالة التطبيق")
            .setMessage("هل تريد إزالة " + label + " من المساحة الخاصة؟")
            .setPositiveButton("إزالة") { _, _ ->
                removeVirtualApp(packageName, label)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun removeVirtualApp(packageName: String, label: String) {
        runPrivateAction(
            progressText = "جاري إزالة " + label + "...",
            action = {
                val result = virtualEngine.uninstallApp(packageName)
                if (result.success) {
                    guestAppCatalog.remove(packageName)
                }
                result.message
            },
            after = { refreshVirtualApps() }
        )
    }

    private fun refreshPrivateStatus() {
        tvPrivateStatus.text = "حالة المحرك: " + virtualEngine.getEngineStatus()
    }

    private fun runPrivateAction(
        progressText: String,
        action: () -> String,
        after: (() -> Unit)? = null
    ) {
        if (!inPrivateSpace) return

        tvPrivateStatus.text = progressText

        Thread {
            val message = try {
                action()
            } catch (t: Throwable) {
                Log.e(SPACE_TAG, "Private-space action failed", t)
                "تعذر إكمال العملية."
            }

            runOnUiThread {
                if (!inPrivateSpace) return@runOnUiThread
                tvPrivateStatus.text = message
                after?.invoke()
            }
        }.start()
    }

    private companion object {
        const val SPACE_TAG = "AIHAM_SPACE"
        const val GUEST_TAG = "AIHAM_GUEST"

        const val REQUEST_PICK_APK = 7700
        const val REQUEST_GUEST_PERMISSIONS = 7701

        const val STATE_PRIVATE_SPACE = "state_private_space"
        const val STATE_PENDING_APK = "state_pending_apk"
    }
}
