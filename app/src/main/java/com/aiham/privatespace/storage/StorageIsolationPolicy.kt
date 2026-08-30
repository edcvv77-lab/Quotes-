package com.aiham.privatespace.storage

internal object StorageIsolationPolicy {
    fun isInternal(dataDir: String, candidatePath: String): Boolean {
        val normalizedData = normalize(dataDir)
        val normalizedCandidate = normalize(candidatePath)
        return normalizedCandidate == normalizedData ||
            normalizedCandidate.startsWith(normalizedData + "/")
    }

    fun isPublicStorage(candidatePath: String): Boolean {
        val normalized = normalize(candidatePath)
        return normalized == "/sdcard" ||
            normalized.startsWith("/sdcard/") ||
            normalized == "/storage" ||
            normalized.startsWith("/storage/")
    }

    private fun normalize(path: String): String =
        path.replace('\\', '/').trimEnd('/')
}
