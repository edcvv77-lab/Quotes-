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

    private fun normalize(path: String): String {
        var normalized = path.replace('\\', '/').trimEnd('/')

        // Android 11 may expose the same credential-encrypted app directory
        // using either /data/user/0/<pkg> or the legacy /data/data/<pkg> alias.
        if (normalized.startsWith("/data/user/0/")) {
            normalized = "/data/data/" + normalized.removePrefix("/data/user/0/")
        }

        // Some devices expose emulated storage through /mnt/user/0/emulated/0
        // while framework APIs return /storage/emulated/0 for the same volume.
        if (normalized.startsWith("/mnt/user/0/emulated/0")) {
            normalized = "/storage/emulated/0" +
                normalized.removePrefix("/mnt/user/0/emulated/0")
        }

        return normalized
    }
}
