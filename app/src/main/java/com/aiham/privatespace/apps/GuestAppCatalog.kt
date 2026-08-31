package com.aiham.privatespace.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class GuestAppRecord(
    val packageName: String,
    val label: String,
    val iconPath: String?
)

class GuestAppCatalog(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val iconDir = File(context.filesDir, ICON_DIR_NAME)

    fun save(packageName: String, label: String, icon: Drawable?) {
        val safeLabel = label.trim().ifEmpty { packageName }
        val iconPath = icon?.let { saveIcon(packageName, it) }

        prefs.edit()
            .putString(labelKey(packageName), safeLabel)
            .apply {
                if (iconPath != null) putString(iconKey(packageName), iconPath)
                else remove(iconKey(packageName))
            }
            .apply()
    }

    fun get(packageName: String): GuestAppRecord {
        val label = prefs.getString(labelKey(packageName), null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: packageName
        val iconPath = prefs.getString(iconKey(packageName), null)
            ?.takeIf { File(it).isFile }

        return GuestAppRecord(packageName, label, iconPath)
    }

    fun remove(packageName: String) {
        prefs.getString(iconKey(packageName), null)?.let { path ->
            runCatching { File(path).delete() }
        }

        prefs.edit()
            .remove(labelKey(packageName))
            .remove(iconKey(packageName))
            .apply()
    }

    private fun saveIcon(packageName: String, drawable: Drawable): String? {
        return runCatching {
            if (!iconDir.exists() && !iconDir.mkdirs()) {
                error("Unable to create icon directory")
            }

            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            val file = File(iconDir, packageName + ".png")
            FileOutputStream(file, false).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Unable to encode icon"
                }
            }
            bitmap.recycle()
            file.absolutePath
        }.onFailure {
            Log.w(TAG, "Unable to persist guest icon package=" + packageName, it)
        }.getOrNull()
    }

    private fun labelKey(packageName: String) = "label:" + packageName
    private fun iconKey(packageName: String) = "icon:" + packageName

    private companion object {
        const val TAG = "AIHAM_SPACE"
        const val PREFS_NAME = "private_guest_catalog"
        const val ICON_DIR_NAME = "guest-icons"
        const val DEFAULT_ICON_SIZE = 128
    }
}
