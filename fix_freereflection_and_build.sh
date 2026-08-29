#!/bin/bash
set -e

echo "🔍 [1/3] استبدال رابط المكتبة القديمة المغلقة برابط JitPack الحي..."
# استبدال المكتبة في جميع ملفات build.gradle في المشروع
find . -name "build.gradle" -exec sed -i 's|me.weishu:free_reflection:3.0.1|com.github.tiann:FreeReflection:3.1.0|g' {} +

# التأكد من وجود مستودع JitPack في build.gradle الرئيسي
cat << 'GRADLE' > build.gradle
// Top-level build file
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
git add .
git commit -m "Fix: Replace deprecated JCenter free_reflection with JitPack FreeReflection:3.1.0" || true
git push origin main || git push origin master

echo "⏳ [3/3] جاري مراقبة البناء وبدء تجميع الـ APK..."
sleep 4
gh run watch
