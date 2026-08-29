#!/bin/bash
set -e

echo "[1/6] إنشاء مجلدات المشروع..."
mkdir -p .github/workflows
mkdir -p app/src/main/java/com/aiham/quotes/security
mkdir -p app/src/main/java/com/aiham/quotes/ui
mkdir -p app/src/main/java/com/aiham/quotes/private_space
mkdir -p app/src/main/res/drawable
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/mipmap-anydpi-v26

echo "[2/6] كتابة ملفات إعدادات Gradle..."
cat << 'EOF' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
rootProject.name = "QuotesApp"
include(":app")
if (file("Bcore").exists()) {
    include(":Bcore")
}
EOF

cat << 'EOF' > build.gradle.kts
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
EOF

cat << 'EOF' > gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
EOF

cat << 'EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aiham.quotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aiham.quotes"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    if (findProject(":Bcore") != null) {
        implementation(project(":Bcore"))
    } else {
        implementation("com.github.FBlackBox:BlackBox:2.1.0")
    }
}
EOF

echo "[3/6] كتابة بيان التطبيق والموارد والأيقونة..."
cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".App"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.AihamQuotes"
        android:requestLegacyExternalStorage="true"
        tools:targetApi="34">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.AihamQuotes">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".private_space.PrivateSpaceActivity"
            android:exported="false"
            android:label="Aiham Private Space"
            android:theme="@style/Theme.AihamQuotes" />

        <provider
            android:name="top.niunaijun.blackbox.core.system.provider.BlackBoxProvider"
            android:authorities="${applicationId}.blackbox.provider"
            android:exported="false" />

        <service
            android:name="top.niunaijun.blackbox.core.system.service.BPService"
            android:process=":blackbox_daemon" />

        <service
            android:name="top.niunaijun.blackbox.core.system.service.BFService"
            android:process=":blackbox_server" />

        <activity
            android:name="top.niunaijun.blackbox.core.system.proxy.ProxyActivity$P0"
            android:process=":p0"
            android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
            android:hardwareAccelerated="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />

        <service
            android:name="top.niunaijun.blackbox.core.system.proxy.ProxyService$P0"
            android:process=":p0" />

        <provider
            android:name="top.niunaijun.blackbox.core.system.proxy.ProxyContentProvider$P0"
            android:authorities="${applicationId}.proxy_content_provider_0"
            android:process=":p0"
            android:exported="false" />
    </application>
</manifest>
EOF

cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">اقتباسات</string>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/colors.xml
<resources>
    <color name="ic_launcher_background">#0F172A</color>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/themes.xml
<resources>
    <style name="Theme.AihamQuotes" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#1E293B</item>
        <item name="android:navigationBarColor">#0F172A</item>
    </style>
</resources>
EOF

cat << 'EOF' > app/src/main/res/drawable/ic_launcher_foreground.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#1E293B"
        android:pathData="M0,0h108v108h-108z"/>
    <path
        android:fillColor="#F59E0B"
        android:pathData="M32,40 c-6.6,0 -12,5.4 -12,12 c0,6.6 5.4,12 12,12 c6.6,0 12,-5.4 12,-12 c0,-8.8 -7.2,-16 -16,-16 Z M32,56 c-2.2,0 -4,-1.8 -4,-4 c0,-2.2 1.8,-4 4,-4 c2.2,0 4,1.8 4,4 c0,2.2 -1.8,4 -4,4 Z"/>
    <path
        android:fillColor="#F59E0B"
        android:pathData="M64,40 c-6.6,0 -12,5.4 -12,12 c0,6.6 5.4,12 12,12 c6.6,0 12,-5.4 12,-12 c0,-8.8 -7.2,-16 -16,-16 Z M64,56 c-2.2,0 -4,-1.8 -4,-4 c0,-2.2 1.8,-4 4,-4 c2.2,0 4,1.8 4,4 c0,2.2 -1.8,4 -4,4 Z"/>
</vector>
EOF

cat << 'EOF' > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF

echo "[4/6] كتابة كود التطبيق وواجهة الاقتباسات والمحرك..."
cat << 'EOF' > app/src/main/java/com/aiham/quotes/App.kt
package com.aiham.quotes

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instance = this
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return base.packageName
                }
                override fun isHideRoot(): Boolean {
                    return true
                }
                override fun isHideXposed(): Boolean {
                    return true
                }
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            BlackBoxCore.get().doCreate()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/aiham/quotes/security/SecurityTrigger.kt
package com.aiham.quotes.security

object SecurityTrigger {
    private const val SECRET_KEY = "aiham77rk"

    fun checkTrigger(input: String): Boolean {
        if (input.isEmpty()) return false
        return input.trim() == SECRET_KEY
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/aiham/quotes/ui/MainActivity.kt
package com.aiham.quotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiham.quotes.private_space.PrivateSpaceActivity
import com.aiham.quotes.security.SecurityTrigger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF59E0B),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFF1E293B)
                )
            ) {
                QuotesAppScreen(
                    onTriggerUnlocked = {
                        val intent = Intent(this, PrivateSpaceActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesAppScreen(onTriggerUnlocked: () -> Unit) {
    var quotesList by remember {
        mutableStateOf(
            listOf(
                "العقل مثل المظلة، لا يعمل إلا إذا كان مفتوحاً.",
                "لا تنتظر الفرصة، بل اصنعها بنفسك.",
                "أفضل طريقة للتنبؤ بالمستقبل هي أن تخترعه.",
                "البساطة هي قمة التطور والذكاء."
            )
        )
    }

    var isDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اقتباسات",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isDialogVisible = true },
                containerColor = Color(0xFFF59E0B),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة اقتباس")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(quotesList) { quote ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            text = "“ $quote ”",
                            modifier = Modifier.padding(20.dp),
                            color = Color(0xFFE2E8F0),
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        if (isDialogVisible) {
            AddQuoteModalDialog(
                onDismiss = { isDialogVisible = false },
                onQuoteSubmitted = { enteredText ->
                    isDialogVisible = false
                    if (SecurityTrigger.checkTrigger(enteredText)) {
                        onTriggerUnlocked()
                    } else if (enteredText.isNotBlank()) {
                        quotesList = quotesList + enteredText.trim()
                    }
                }
            )
        }
    }
}

@Composable
fun AddQuoteModalDialog(
    onDismiss: () -> Unit,
    onQuoteSubmitted: (String) -> Unit
) {
    var quoteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "أضف اقتباسك",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = quoteInput,
                onValueChange = { quoteInput = it },
                placeholder = { Text("اكتب الاقتباس هنا...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4
            )
        },
        confirmButton = {
            Button(
                onClick = { onQuoteSubmitted(quoteInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}
EOF

cat << 'EOF' > app/src/main/java/com/aiham/quotes/private_space/PrivateSpaceActivity.kt
package com.aiham.quotes.private_space

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.niunaijun.blackbox.BlackBoxCore

class PrivateSpaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF020617)
                )
            ) {
                PrivateSpaceDashboard(
                    onLaunch = { pkg -> runVirtualPackage(pkg) },
                    onInstall = { pkg -> cloneLocalPackage(pkg) }
                )
            }
        }
    }

    private fun runVirtualPackage(packageName: String) {
        try {
            val launched = BlackBoxCore.get().launchApk(packageName, 0)
            if (!launched) {
                Toast.makeText(this, "يرجى نسخ التطبيق إلى البيئة المعزولة أولاً", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ أثناء التشغيل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cloneLocalPackage(packageName: String) {
        try {
            val result = BlackBoxCore.get().installPackageAsUser(packageName, 0)
            if (result.success) {
                Toast.makeText(this, "تم استنساخ التطبيق في المساحة المعزولة بنجاح", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "فشل الاستنساخ: ${result.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceDashboard(
    onLaunch: (String) -> Unit,
    onInstall: (String) -> Unit
) {
    val managedApps = remember {
        listOf(
            Triple("WhatsApp", "com.whatsapp", "تطبيق المراسلة المعزول"),
            Triple("Telegram", "org.telegram.messenger", "المحادثات الآمنة"),
            Triple("WhatsApp Business", "com.whatsapp.w4b", "نسخة الأعمال المعزولة")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aiham Private Space",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617))
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "البيئة الافتراضية نشطة. التطبيقات هنا تعمل في وضع عزل كامل للذاكرة وقواعد البيانات دون تداخل مع النظام الخارجي.",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(managedApps) { (appName, pkgName, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onInstall(pkgName) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("استنساخ", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onLaunch(pkgName) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تشغيل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "[5/6] كتابة ملف البناء التلقائي GitHub Actions..."
cat << 'EOF' > .github/workflows/build.yml
name: Build Aiham Quotes & Private Space APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - name: Checkout Repository
      uses: actions/checkout@v4
      with:
        submodules: recursive
        fetch-depth: 0

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      with:
        cmdline-tools-version: 11076708

    - name: Ensure Gradle Wrapper
      run: |
        if [ ! -f "gradlew" ]; then
          gradle wrapper
        fi
        chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug --stacktrace

    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: AihamQuotes-Debug-APK
        path: app/build/outputs/apk/debug/*.apk
        retention-days: 7
EOF

echo "[6/6] حفظ التعديلات والرفع إلى GitHub..."
git add .
git commit -m "Auto: Complete setup for Quotes App with BlackBox and GitHub Actions" || true

BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")
if [ "$BRANCH_NAME" = "HEAD" ] || [ -z "$BRANCH_NAME" ]; then
    BRANCH_NAME="main"
fi

git push origin "$BRANCH_NAME" || git push origin main || git push origin master

echo "=================================================="
echo "تم إنشاء جميع الملفات والرفع بنجاح!"
echo "افتح GitHub -> Actions لمتابعة البناء وتحميل الـ APK."
echo "=================================================="
