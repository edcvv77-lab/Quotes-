package com.aiham.quotes

import android.content.SharedPreferences

interface QuoteStore {
    fun getQuotes(): List<String>
    fun addQuote(quote: String)
}

class SharedPreferencesQuoteStore(
    private val preferences: SharedPreferences
) : QuoteStore {
    override fun getQuotes(): List<String> =
        preferences.getStringSet(KEY_ITEMS, emptySet()).orEmpty().toList()

    override fun addQuote(quote: String) {
        val items = preferences.getStringSet(KEY_ITEMS, emptySet()).orEmpty().toMutableSet()
        items.add(quote)
        preferences.edit().putStringSet(KEY_ITEMS, items).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
    }
}
