package com.aiham.privatespace.storage

internal object StorageIsolationPolicy {
    fun isInside(allowedRoot: String, candidatePath: String): Boolean {
        val root = normalize(allowedRoot)
        val candidate = normalize(candidatePath)
        return candidate == root || candidate.startsWith(root + "/")
    }

    fun isSharedPublicPath(candidatePath: String): Boolean {
        val path = normalize(candidatePath)
        if (!(path == "/sdcard" || path.startsWith("/sdcard/") ||
                path == "/storage" || path.startsWith("/storage/"))) {
            return false
        }

        return !path.contains("/Android/data/com.aiham.quotes/")
    }

    private fun normalize(path: String): String =
        path.replace('\\', '/').trimEnd('/')
}
