package com.aiham.quotes

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
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
    private lateinit var tvQuoteCount: TextView
    private lateinit var tvListStatus: TextView
    private lateinit var etQuoteSearch: EditText
    private lateinit var quotesContainer: LinearLayout
    private lateinit var btnFavoriteFilter: Button
    private lateinit var tvPrivateStatus: TextView
    private lateinit var appsContainer: GridLayout

    private val prefs by lazy { getSharedPreferences("quotes", MODE_PRIVATE) }
    private val quoteService by lazy { QuoteService(SharedPreferencesQuoteStore(prefs)) }
    private val virtualEngine: BlackBoxVirtualEngine by lazy { (application as AihamApp).virtualEngine }
    private val guestPermissionManager by lazy { GuestPermissionManager(this) }
    private val guestApkInspector by lazy { GuestApkInspector(this) }
    private val guestAppCatalog by lazy { GuestAppCatalog(this) }
    private val installedAppCloneManager by lazy { InstalledAppCloneManager(this) }

    private var pendingGuestApk: File? = null
    private var pendingInstalledClonePackage: String? = null
    private var inPrivateSpace = false
    private var favoritesOnly = false

    private val defaultQuotes = listOf(
        "الأفعال الصغيرة المتكررة تصنع نتائج كبيرة.",
        "لا تنتظر الوقت المثالي؛ اصنعه.",
        "كل خطوة واضحة أفضل من عشر خطوات متخيلة."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingGuestApk = savedInstanceState
            ?.getString(STATE_PENDING_APK)
            ?.let(::File)
            ?.takeIf { it.isFile }
        pendingInstalledClonePackage =
            savedInstanceState?.getString(STATE_PENDING_INSTALLED_CLONE)

        if (savedInstanceState?.getBoolean(STATE_PRIVATE_SPACE) == true) {
            enterPrivateSpace()
        } else {
            showQuotesHome()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PRIVATE_SPACE, inPrivateSpace)
        pendingGuestApk?.absolutePath?.let { outState.putString(STATE_PENDING_APK, it) }
        pendingInstalledClonePackage?.let {
            outState.putString(STATE_PENDING_INSTALLED_CLONE, it)
        }
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
        favoritesOnly = false
        setContentView(R.layout.activity_main)

        tvQuoteCount = findViewById(R.id.tvQuoteCount)
        tvListStatus = findViewById(R.id.tvListStatus)
        etQuoteSearch = findViewById(R.id.etQuoteSearch)
        quotesContainer = findViewById(R.id.quotesContainer)
        btnFavoriteFilter = findViewById(R.id.btnFavoriteFilter)

        findViewById<Button>(R.id.btnAddQuote).setOnClickListener { showAddQuoteDialog() }
        btnFavoriteFilter.setOnClickListener {
            favoritesOnly = !favoritesOnly
            btnFavoriteFilter.text = if (favoritesOnly) "★ عرض الكل" else "☆ المفضلة"
            renderQuotes()
        }

        etQuoteSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderQuotes()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        renderQuotes()
    }

    private fun showAddQuoteDialog() {
        val input = createQuoteEditor()

        AlertDialog.Builder(this)
            .setTitle("إضافة اقتباس")
            .setMessage("أضف كلمة أو فكرة تريد الاحتفاظ بها.")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ -> handleSubmittedText(input.text.toString()) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun createQuoteEditor(initialText: String = ""): EditText {
        return EditText(this).apply {
            hint = "اكتب اقتباسك هنا"
            minLines = 3
            maxLines = 7
            setText(initialText)
            setSelection(text.length)
            setPadding(36, 28, 36, 28)
            textDirection = View.TEXT_DIRECTION_LOCALE
        }
    }

    private fun handleSubmittedText(raw: String) {
        when (val result = quoteService.submit(raw)) {
            QuoteSubmission.Empty ->
                Toast.makeText(this, "اكتب اقتباسًا أولًا.", Toast.LENGTH_SHORT).show()
            QuoteSubmission.OpenPrivateSpace -> enterPrivateSpace()
            is QuoteSubmission.Saved -> {
                etQuoteSearch.setText("")
                favoritesOnly = false
                btnFavoriteFilter.text = "☆ المفضلة"
                renderQuotes()
                Toast.makeText(this, "تمت إضافة الاقتباس.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderQuotes() {
        if (!::quotesContainer.isInitialized || inPrivateSpace) return

        val query = etQuoteSearch.text?.toString().orEmpty().trim()
        val items = buildList {
            defaultQuotes.forEach { add(QuoteUiItem(it, isUserQuote = false)) }
            quoteService.getQuotes().forEach { add(QuoteUiItem(it, isUserQuote = true)) }
        }

        val visible = items.filter { item ->
            val matchesSearch = query.isEmpty() || item.text.contains(query, ignoreCase = true)
            val matchesFavorite = !favoritesOnly || quoteService.isFavorite(item.text)
            matchesSearch && matchesFavorite
        }

        tvQuoteCount.text = "عدد الاقتباسات: " + items.size
        tvListStatus.text = when {
            favoritesOnly && query.isNotEmpty() -> "نتائج البحث في المفضلة • " + visible.size
            favoritesOnly -> "المفضلة • " + visible.size
            query.isNotEmpty() -> "نتائج البحث • " + visible.size
            else -> "كل الاقتباسات • " + visible.size
        }

        quotesContainer.removeAllViews()

        if (visible.isEmpty()) {
            val message = TextView(this).apply {
                text = if (favoritesOnly) {
                    "لا توجد اقتباسات مفضلة تطابق العرض الحالي."
                } else {
                    "لا توجد اقتباسات تطابق البحث."
                }
                gravity = android.view.Gravity.CENTER
                textSize = 16f
                setTextColor(getColorCompat(R.color.quotes_text_secondary))
                setPadding(24, 64, 24, 64)
            }
            quotesContainer.addView(message)
            return
        }

        val inflater = LayoutInflater.from(this)
        visible.forEach { item ->
            val card = inflater.inflate(R.layout.item_quote_card, quotesContainer, false)
            bindQuoteCard(card, item)
            quotesContainer.addView(card)
        }
    }

    private fun bindQuoteCard(card: View, item: QuoteUiItem) {
        val favorite = quoteService.isFavorite(item.text)

        card.findViewById<TextView>(R.id.tvQuoteText).text = "“" + item.text + "”"
        card.findViewById<TextView>(R.id.tvQuoteBadge).text =
            if (item.isUserQuote) "من اقتباساتي" else "مختار"

        card.findViewById<TextView>(R.id.tvFavoriteMark).visibility =
            if (favorite) View.VISIBLE else View.GONE

        val favoriteAction = card.findViewById<TextView>(R.id.actionFavorite)
        favoriteAction.text = if (favorite) "★ محفوظ" else "☆ حفظ"
        favoriteAction.setOnClickListener {
            val nowFavorite = quoteService.toggleFavorite(item.text)
            Toast.makeText(
                this,
                if (nowFavorite) "تمت الإضافة إلى المفضلة." else "تمت الإزالة من المفضلة.",
                Toast.LENGTH_SHORT
            ).show()
            renderQuotes()
        }

        card.findViewById<TextView>(R.id.actionCopy).setOnClickListener {
            copyQuote(item.text)
        }

        card.findViewById<TextView>(R.id.actionShare).setOnClickListener {
            shareQuote(item.text)
        }

        val edit = card.findViewById<TextView>(R.id.actionEdit)
        val delete = card.findViewById<TextView>(R.id.actionDelete)

        if (item.isUserQuote) {
            edit.setOnClickListener { showEditQuoteDialog(item.text) }
            delete.setOnClickListener { confirmDeleteQuote(item.text) }
        } else {
            edit.visibility = View.GONE
            delete.visibility = View.GONE
        }
    }

    private fun showEditQuoteDialog(original: String) {
        val input = createQuoteEditor(original)

        AlertDialog.Builder(this)
            .setTitle("تعديل الاقتباس")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                when (quoteService.updateQuote(original, input.text.toString())) {
                    QuoteEditResult.Empty ->
                        Toast.makeText(this, "لا يمكن حفظ اقتباس فارغ.", Toast.LENGTH_SHORT).show()
                    QuoteEditResult.SecretTriggerRejected ->
                        Toast.makeText(this, "تعذر حفظ هذا النص.", Toast.LENGTH_SHORT).show()
                    QuoteEditResult.NotFound ->
                        Toast.makeText(this, "تعذر العثور على الاقتباس.", Toast.LENGTH_SHORT).show()
                    is QuoteEditResult.Updated -> {
                        renderQuotes()
                        Toast.makeText(this, "تم تعديل الاقتباس.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteQuote(quote: String) {
        AlertDialog.Builder(this)
            .setTitle("حذف الاقتباس")
            .setMessage("هل تريد حذف هذا الاقتباس نهائيًا؟")
            .setPositiveButton("حذف") { _, _ ->
                if (quoteService.deleteQuote(quote)) {
                    renderQuotes()
                    Toast.makeText(this, "تم حذف الاقتباس.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun copyQuote(quote: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("quote", quote))
        Toast.makeText(this, "تم نسخ الاقتباس.", Toast.LENGTH_SHORT).show()
    }

    private fun shareQuote(quote: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, quote)
        }
        startActivity(Intent.createChooser(intent, "مشاركة الاقتباس"))
    }

    private fun getColorCompat(colorRes: Int): Int =
        androidx.core.content.ContextCompat.getColor(this, colorRes)

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
                            app.label + "\n" + app.packageName
                        }.toTypedArray()

                        AlertDialog.Builder(this)
                            .setTitle("اختر تطبيقًا لنسخه")
                            .setItems(labels) { _, which ->
                                cloneInstalledApp(apps[which])
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

    private fun cloneInstalledApp(app: InstalledAppCandidate) {
        if (!inPrivateSpace) return

        tvPrivateStatus.text = "جاري تجهيز نسخة مستقلة من " + app.label + "..."

        Thread {
            val baseApk = File(app.baseApkPath)
            val plan = guestPermissionManager.buildPlan(baseApk).getOrElse { error ->
                Log.e(
                    SPACE_TAG,
                    "Unable to build installed-app permission plan package=" + app.packageName,
                    error
                )
                runOnUiThread {
                    if (inPrivateSpace) {
                        tvPrivateStatus.text = "تعذر فحص أذونات التطبيق."
                    }
                }
                return@Thread
            }

            runOnUiThread {
                if (!inPrivateSpace) return@runOnUiThread

                if (plan.missingRuntimePermissions.isEmpty()) {
                    installInstalledGuest(app, permissionsDenied = false)
                } else {
                    pendingGuestApk = null
                    pendingInstalledClonePackage = app.packageName
                    tvPrivateStatus.text =
                        "يحتاج " + app.label + " إلى بعض الأذونات قبل التشغيل."
                    guestPermissionManager.requestMissingRuntimePermissions(
                        plan,
                        REQUEST_GUEST_PERMISSIONS
                    )
                }
            }
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

        val denied = permissions.indices.any { index ->
            grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
        }

        if (denied) {
            Log.w(SPACE_TAG, "One or more guest runtime permissions were denied")
        }

        val installedClonePackage = pendingInstalledClonePackage
        if (installedClonePackage != null) {
            pendingInstalledClonePackage = null
            pendingGuestApk = null

            val app = installedAppCloneManager.findCloneCandidate(installedClonePackage)
                .getOrElse { error ->
                    Log.e(
                        SPACE_TAG,
                        "Pending installed app could not be resolved package=" +
                            installedClonePackage,
                        error
                    )
                    if (inPrivateSpace) {
                        tvPrivateStatus.text = "تعذر متابعة نسخ التطبيق المثبت."
                    }
                    return
                }

            installInstalledGuest(app, permissionsDenied = denied)
            return
        }

        val apk = pendingGuestApk
        pendingGuestApk = null

        if (apk == null || !apk.isFile) {
            Log.w(SPACE_TAG, "Permission result received without pending guest APK")
            if (inPrivateSpace) tvPrivateStatus.text = "تعذر متابعة تثبيت التطبيق."
            return
        }

        val metadata = guestApkInspector.inspectFile(apk).getOrElse { error ->
            Log.e(SPACE_TAG, "Pending APK could not be re-read", error)
            apk.delete()
            if (inPrivateSpace) tvPrivateStatus.text = "تعذر إعادة قراءة ملف التطبيق."
            return
        }

        installImportedGuest(metadata, permissionsDenied = denied)
    }

    private fun installInstalledGuest(
        app: InstalledAppCandidate,
        permissionsDenied: Boolean
    ) {
        runPrivateAction(
            progressText = "جاري نسخ " + app.label + " داخل المساحة...",
            action = {
                val result = virtualEngine.installHostPackage(app.packageName)
                if (!result.success) {
                    return@runPrivateAction result.message
                }

                guestAppCatalog.save(
                    packageName = app.packageName,
                    label = app.label,
                    icon = installedAppCloneManager.loadIcon(app.packageName)
                )

                buildString {
                    append("تم نسخ ")
                    append(app.label)
                    append(" داخل المساحة.")

                    if (permissionsDenied) {
                        append("\nتم رفض بعض الأذونات؛ قد لا تعمل بعض الوظائف.")
                    }
                }
            },
            after = { refreshVirtualApps() }
        )
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
                                "المساحة جاهزة • التخزين معزول داخليًا • لا توجد تطبيقات."
                            } else {
                                "المساحة جاهزة • التخزين معزول داخليًا • التطبيقات: " + packages.size
                            }
                    },
                    onFailure = { error ->
                        Log.e(SPACE_TAG, "Unable to refresh virtual app list", error)
                        appsContainer.removeAllViews()
                        tvPrivateStatus.text =
                            "تعذر قراءة التطبيقات داخل المساحة. حالة المحرك: " +
                                virtualEngine.getEngineStatus()
                    }
                )
            }
        }.start()
    }

    private fun renderVirtualApps(packages: List<String>) {
        appsContainer.removeAllViews()

        if (packages.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد تطبيقات داخل المساحة بعد.\nأضف APK أو انسخ تطبيقًا مثبتًا من الجهاز."
                textSize = 15f
                gravity = android.view.Gravity.CENTER
                setTextColor(0xFF94A3B8.toInt())
                setPadding(20, 48, 20, 48)
            }
            val emptyParams = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                columnSpec = GridLayout.spec(0, appsContainer.columnCount)
            }
            appsContainer.addView(empty, emptyParams)
            return
        }

        val inflater = LayoutInflater.from(this)
        val columns = appsContainer.columnCount.coerceAtLeast(1)
        val margin = (8 * resources.displayMetrics.density).toInt()

        packages.forEach { packageName ->
            val record = guestAppCatalog.get(packageName)
            val tile = inflater.inflate(R.layout.item_guest_app, appsContainer, false)

            tile.findViewById<TextView>(R.id.tvGuestLabel).text = record.label

            val iconView = tile.findViewById<ImageView>(R.id.ivGuestIcon)
            val icon = record.iconPath?.let(Drawable::createFromPath)
            if (icon != null) {
                iconView.setImageDrawable(icon)
            } else {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            tile.setOnClickListener {
                launchVirtualApp(record.packageName, record.label)
            }

            tile.setOnLongClickListener {
                showGuestActions(record.packageName, record.label)
                true
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                setMargins(margin, margin, margin, margin)
            }

            appsContainer.addView(tile, params)
        }
    }

    private fun showGuestActions(packageName: String, label: String) {
        AlertDialog.Builder(this)
            .setTitle(label)
            .setMessage(packageName)
            .setItems(arrayOf("تشغيل", "إزالة من المساحة")) { _, which ->
                when (which) {
                    0 -> launchVirtualApp(packageName, label)
                    1 -> confirmRemoveVirtualApp(packageName, label)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
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

    private data class QuoteUiItem(
        val text: String,
        val isUserQuote: Boolean
    )

    private companion object {
        const val SPACE_TAG = "AIHAM_SPACE"
        const val GUEST_TAG = "AIHAM_GUEST"

        const val REQUEST_PICK_APK = 7700
        const val REQUEST_GUEST_PERMISSIONS = 7701

        const val STATE_PRIVATE_SPACE = "state_private_space"
        const val STATE_PENDING_APK = "state_pending_apk"
        const val STATE_PENDING_INSTALLED_CLONE = "state_pending_installed_clone"
    }
}
