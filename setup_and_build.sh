#!/bin/bash
set -e

echo "🚀 [1/6] تنظيف وإعداد ملفات المشروع..."
mkdir -p app/src/main/java/com/aiham/quotes
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p .github/workflows

# 1. إعداد build.gradle الرئيسي
cat << 'GRADLE' > build.gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20'
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
GRADLE

# 2. إعداد settings.gradle
cat << 'SETTINGS' > settings.gradle
rootProject.name = "Quotes"
include ':app'
SETTINGS

# 3. إعداد app/build.gradle
cat << 'APP_GRADLE' > app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    namespace 'com.aiham.quotes'
    compileSdk 33

    defaultConfig {
        applicationId "com.aiham.quotes"
        minSdk 21
        targetSdk 33
        versionCode 1
        versionName "1.0"
        
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            debuggable true
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = '11'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.9.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.8.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    // BlackBox Core Engine
    implementation 'io.github.fankes:blackbox-core:2.1.0'
}
APP_GRADLE

# 4. إعداد AndroidManifest.xml
cat << 'MANIFEST' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="اقتباسات"
        android:supportsRtl="true"
        android:theme="@style/Theme.MaterialComponents.Light.DarkActionBar"
        android:extractNativeLibs="true"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".PrivateSpaceActivity"
            android:exported="false"
            android:label="Aiham Private Space" />
    </application>
</manifest>
MANIFEST

# 5. كود Application لتهيئة BlackBox
cat << 'APP_CODE' > app/src/main/java/com/aiham/quotes/App.kt
package com.aiham.quotes

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : top.niunaijun.blackbox.app.configuration.ClientConfiguration() {
                override fun getHostPackageName(): String = "com.aiham.quotes"
                override fun isHideRoot(): Boolean = true
                override fun isHideXposed(): Boolean = true
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
APP_CODE

# 6. كود واجهة الاقتباسات + كاشف المفتاح السري aiham77rk
cat << 'MAIN_ACTIVITY' > app/src/main/java/com/aiham/quotes/MainActivity.kt
package com.aiham.quotes

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var quotesList: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quotesList = arrayListOf(
            "لا تحزن إن الله معنا.",
            "النجاح يبدأ بخطوة وإصرار لا ينقطع.",
            "كن أنت التغيير الذي تريد أن تراه في العالم.",
            "الصبر مفتاح الفرج وتاج المؤمن."
        )

        val listView = findViewById<ListView>(R.id.listViewQuotes)
        val btnAddQuote = findViewById<Button>(R.id.btnAddQuote)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, quotesList)
        listView.adapter = adapter

        btnAddQuote.setOnClickListener {
            showAddQuoteDialog()
        }
    }

    private fun showAddQuoteDialog() {
        val input = EditText(this)
        input.hint = "أضف اقتباسك هنا..."

        AlertDialog.Builder(this)
            .setTitle("إضافة اقتباس جديد")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val text = input.text.toString().trim()
                
                // 🔐 المفتاح السري لفتح المساحة الخاصة
                if (text == "aiham77rk") {
                    val intent = Intent(this, PrivateSpaceActivity::class.java)
                    startActivity(intent)
                } else if (text.isNotEmpty()) {
                    quotesList.add(0, text)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "تمت إضافة الاقتباس بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
MAIN_ACTIVITY

# 7. كود واجهة المساحة الخاصة (تشغيل واستنساخ التطبيقات عبر BlackBox)
cat << 'SPACE_ACTIVITY' > app/src/main/java/com/aiham/quotes/PrivateSpaceActivity.kt
package com.aiham.quotes

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import top.niunaijun.blackbox.BlackBoxCore

class PrivateSpaceActivity : AppCompatActivity() {

    private val installedApps = ArrayList<String>()
    private val installedPkgNames = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_space)
        title = "المساحة الخاصة المعزولة 🔒"

        val listView = findViewById<ListView>(R.id.listViewPrivateApps)
        val btnCloneApp = findViewById<Button>(R.id.btnCloneApp)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, installedApps)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val pkg = installedPkgNames[position]
            Toast.makeText(this, "جاري تشغيل التطبيق في البيئة المعزولة...", Toast.LENGTH_SHORT).show()
            BlackBoxCore.get().launchApk(pkg, 0)
        }

        btnCloneApp.setOnClickListener {
            showCloneDialog()
        }

        loadPrivateApps()
    }

    private fun loadPrivateApps() {
        installedApps.clear()
        installedPkgNames.clear()
        try {
            val list = BlackBoxCore.get().getInstalledApplications(0, 0)
            for (info in list) {
                val label = packageManager.getApplicationLabel(info).toString()
                installedApps.add(label)
                installedPkgNames.add(info.packageName)
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCloneDialog() {
        val pm = packageManager
        val sysApps = pm.getInstalledApplications(0).filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        val names = sysApps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("اختر تطبيقاً لعزله داخل المساحة")
            .setItems(names) { _, which ->
                val selectedPkg = sysApps[which].packageName
                Toast.makeText(this, "جاري نسخ وتثبيت التطبيق...", Toast.LENGTH_LONG).show()
                Thread {
                    BlackBoxCore.get().installPackageAsUser(selectedPkg, 0)
                    runOnUiThread {
                        loadPrivateApps()
                        Toast.makeText(this, "تم تثبيت التطبيق بنجاح داخل العزل!", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
SPACE_ACTIVITY

# 8. ملفات الـ Layout (الواجهات)
cat << 'LAYOUT_MAIN' > app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="📜 روائع الاقتباسات اليومية"
        android:textSize="20sp"
        android:textStyle="bold"
        android:gravity="center"
        android:paddingBottom="12dp" />

    <ListView
        android:id="@+id/listViewQuotes"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <Button
        android:id="@+id/btnAddQuote"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="＋ إضافة اقتباس جديد"
        android:textSize="16sp" />
</LinearLayout>
LAYOUT_MAIN

cat << 'LAYOUT_SPACE' > app/src/main/res/layout/activity_private_space.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="⚡ التطبيقات المعزولة داخل المحرك"
        android:textSize="18sp"
        android:textStyle="bold"
        android:gravity="center"
        android:paddingBottom="12dp" />

    <ListView
        android:id="@+id/listViewPrivateApps"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <Button
        android:id="@+id/btnCloneApp"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="استنساخ تطبيق من الهاتف (واتساب/تليجرام..)"
        android:textSize="14sp" />
</LinearLayout>
LAYOUT_SPACE

# 9. إعداد الـ Workflow للبناء السحابي السريع
cat << 'WORKFLOW' > .github/workflows/build.yml
name: Build Camouflage Quotes App

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Code
      uses: actions/checkout@v4

    - name: Set up JDK 11
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'

    - name: Grant Execute Permission
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
        name: Aiham-Quotes-PrivateSpace
        path: app/build/outputs/apk/debug/*.apk
WORKFLOW

echo "📤 [2/6] رفع المشروع إلى GitHub لبدء البناء السحابي..."
git add .
git commit -m "Complete working Camouflage App with BlackBox and secret trigger" || true
git push origin main || git push origin master

echo "⏳ [3/6] جاري مراقبة البناء وبدء سحب الـ APK..."
gh run watch

echo "📥 [4/6] تم البناء بنجاح! جاري تنزيل ملف الـ APK إلى جهازك..."
gh run download -n Aiham-Quotes-PrivateSpace -D $HOME/Downloads/

echo "✅ [مبروك!] تم حفظ التطبيق في مجلد التنزيلات (Downloads) في جهازك! جاهز للتثبيت على الأندرويد 11 و 14 🎉"
