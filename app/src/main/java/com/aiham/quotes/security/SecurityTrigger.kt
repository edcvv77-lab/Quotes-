package com.aiham.quotes.security

import java.security.MessageDigest

object SecurityTrigger {
    private const val SECRET_HASH = "1fe4beb5ba37284958745a8f36824d2cc794e7a4eef3d55215cb66d9a7c30a84"

    fun checkTrigger(input: String): Boolean = sha256(input.trim()) == SECRET_HASH

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
