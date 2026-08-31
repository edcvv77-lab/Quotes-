package com.aiham.privatespace.storage

import android.content.Context
import android.content.ContextWrapper
import java.io.File

class PrivateStorageContext(base: Context) : ContextWrapper(base) {
    private fun privateRoot(): File =
        File(filesDir, PRIVATE_ROOT).apply { mkdirs() }

    private fun privateDir(kind: String, type: String? = null): File {
        val root = File(privateRoot(), kind)
        val target = if (type.isNullOrBlank()) root else File(root, sanitize(type))
        target.mkdirs()
        return target
    }

    override fun getExternalFilesDir(type: String?): File =
        privateDir("external-files", type)

    override fun getExternalFilesDirs(type: String?): Array<File> =
        arrayOf(getExternalFilesDir(type))

    override fun getExternalCacheDir(): File =
        privateDir("external-cache")

    override fun getExternalCacheDirs(): Array<File> =
        arrayOf(getExternalCacheDir())

    override fun getExternalMediaDirs(): Array<File> =
        arrayOf(privateDir("external-media"))

    override fun getObbDir(): File =
        privateDir("obb")

    override fun getObbDirs(): Array<File> =
        arrayOf(getObbDir())

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        const val PRIVATE_ROOT = "secure-space"
    }
}
