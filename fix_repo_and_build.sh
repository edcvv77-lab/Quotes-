#!/bin/bash
set -e

echo "🔧 [1/3] إضافة مستودع jcenter لجلب مكتبة free_reflection..."

cat << 'GRADLE' > build.gradle
// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jcenter.bintray.com' }
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
        maven { url 'https://jcenter.bintray.com' }
        maven { url 'https://jitpack.io' }
    }
}

ext {
    compileSdkVersion = 33
    buildToolsVersion = "30.0.3"
    minSdkVersion = 21
    targetSdkVersion = 30
    versionCode = 1
    versionName = "1.0"
    xVersion = "1.1.0"
    blackReflection = "1.1.2"
}
GRADLE

echo "📤 [2/3] رفع التعديل إلى المستودع..."
git add build.gradle
git commit -m "Fix: Add JCenter repository for me.weishu:free_reflection dependency" || true
git push origin main || git push origin master

echo "⏳ [3/3] جاري مراقبة البناء وبدء تجميع التطبيق..."
sleep 4
gh run watch
