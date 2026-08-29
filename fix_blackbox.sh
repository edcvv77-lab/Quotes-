#!/bin/bash
set -e

echo "🔍 جاري فحص وتجهيز مكتبة BlackBox المحلية..."
git submodule update --init --recursive || true

# تحديد مسار المجلد الداخلي لـ BlackBox
BCORE_DIR=$(find . -maxdepth 3 -type d -name "Bcore" | head -n 1)
if [ -z "$BCORE_DIR" ]; then
    BCORE_DIR="BlackBox/Bcore"
fi

echo "✅ تم ربط المحرك من المسار المحلي: $BCORE_DIR"

# 1. تحديث settings.gradle للربط بالمحرك المحلي
cat << SETTINGS > settings.gradle
rootProject.name = "Quotes"
include ':app'
include ':Bcore'
project(':Bcore').projectDir = new File(rootDir, '${BCORE_DIR#./}')
SETTINGS

# 2. تحديث app/build.gradle ليعتمد على المشروع المحلي بدلاً من السيرفر الخارجي
sed -i "s|implementation 'io.github.fankes:blackbox-core:2.1.0'|implementation project(':Bcore')|g" app/build.gradle

# 3. التأكد من إعدادات سحب الـ Submodule في GitHub Actions
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
    - name: Checkout Code with Submodules
      uses: actions/checkout@v4
      with:
        submodules: recursive
        fetch-depth: 0

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

echo "📤 جاري رفع التعديل وبدء البناء..."
git add .
git commit -m "Fix: Link local BlackBox submodule (:Bcore) directly" || true
git push origin main || git push origin master

echo "⏳ جاري مراقبة البناء..."
sleep 4
gh run watch
