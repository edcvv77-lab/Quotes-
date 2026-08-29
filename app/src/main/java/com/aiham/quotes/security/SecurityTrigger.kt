package com.aiham.quotes.security

object SecurityTrigger {
    private const val SECRET_KEY = "aiham77rk"

    fun checkTrigger(input: String): Boolean {
        if (input.isEmpty()) return false
        return input.trim() == SECRET_KEY
    }
}
