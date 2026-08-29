#!/bin/bash
set -e

echo "🔧 [1/3] تسجيل جميع وحدات ومحركات BlackBox التابعة في settings.gradle..."

cat << 'SETTINGS' > settings.gradle
rootProject.name = "Quotes"
include ':app'

// دالة ذكية للبحث عن مسارات وحدات BlackBox وربطها
def findModuleDir(String path) {
    def f1 = new File(rootDir, "BlackBox/" + path)
    if (f1.exists()) return f1
    def f2 = new File(rootDir, path)
    if (f2.exists()) return f2
    return null
}

def modules = [
    ':Bcore': 'Bcore',
    ':Bcore:black-fake': 'Bcore/black-fake',
    ':Bcore:black-hook': 'Bcore/black-hook',
    ':Bcore:pine-xposed': 'Bcore/pine-xposed',
    ':Bcore:pine-core': 'Bcore/pine-core',
    ':Bcore:pine-xposed-res': 'Bcore/pine-xposed-res',
    ':android-mirror': 'android-mirror'
]

modules.each { name, path ->
    def dir = findModuleDir(path)
    if (dir != null) {
        include name
        project(name).projectDir = dir
    }
}
SETTINGS

echo "📤 [2/3] رفع الإعدادات الجديدة إلى المستودع..."
git add settings.gradle
git commit -m "Fix: Include all nested BlackBox submodules (black-fake, black-hook, android-mirror)" || true
git push origin main || git push origin master

echo "⏳ [3/3] جاري مراقبة البناء وبدء تجميع التطبيق..."
sleep 4
gh run watch
