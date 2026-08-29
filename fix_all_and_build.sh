#!/bin/bash
set -e

echo "🔧 [1/3] كتابة إعدادات BlackBox الرسمية بالكامل..."
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

// المتغيرات العامة المطلوبة لمحرك Bcore
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

# تفعيل الـ AndroidX و Jetifier لتوافق المكتبات
cat << 'PROPS' > gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
android.useAndroidX=true
android.enableJetifier=true
PROPS

echo "📤 [2/3] رفع التعديل إلى المستودع..."
git add build.gradle gradle.properties
git commit -m "Fix: Add all global BlackBox ext properties including blackReflection" || true
git push origin main || git push origin master

echo "⏳ [3/3] جاري مراقبة البناء وبدء توليد الـ APK..."
sleep 4
gh run watch
