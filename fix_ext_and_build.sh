#!/bin/bash
set -e

echo "🔧 [1/3] تحديث build.gradle بإعدادات BlackBox المطلوبة..."
cat << 'GRADLE' > build.gradle
buildscript {
    ext {
        compileSdkVersion = 33
        buildToolsVersion = "30.0.3"
        minSdkVersion = 21
        targetSdkVersion = 33
        versionCode = 1
        versionName = "1.0.0"
        kotlin_version = '1.8.20'
    }
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    ext {
        compileSdkVersion = 33
        buildToolsVersion = "30.0.3"
        minSdkVersion = 21
        targetSdkVersion = 33
        versionCode = 1
        versionName = "1.0.0"
    }
}
GRADLE

echo "📤 [2/3] رفع التحديث إلى GitHub..."
git add build.gradle
git commit -m "Fix: Define targetSdkVersion and compilation properties for Bcore" || true
git push origin main || git push origin master

echo "⏳ [3/3] جاري تشغيل ومراقبة البناء السحابي..."
sleep 4
gh run watch
