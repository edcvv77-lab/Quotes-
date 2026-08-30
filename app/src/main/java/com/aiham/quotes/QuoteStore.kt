package com.aiham.quotes

import android.content.SharedPreferences
import org.json.JSONArray

interface QuoteStore {
    fun getQuotes(): List<String>
    fun addQuote(quote: String)
    fun removeQuote(quote: String): Boolean
    fun updateQuote(oldQuote: String, newQuote: String): Boolean
    fun isFavorite(quote: String): Boolean
    fun setFavorite(quote: String, favorite: Boolean)
}

class SharedPreferencesQuoteStore(
    private val preferences: SharedPreferences
) : QuoteStore {
    override fun getQuotes(): List<String> {
        val json = preferences.getString(KEY_ITEMS_JSON, null)
        if (json != null) {
            return decodeQuotes(json)
        }

        val legacy = preferences.getStringSet(KEY_ITEMS_LEGACY, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }

        if (legacy.isNotEmpty()) {
            saveQuotes(legacy)
        }
        return legacy
    }

    override fun addQuote(quote: String) {
        val items = getQuotes().toMutableList()
        items.remove(quote)
        items.add(0, quote)
        saveQuotes(items)
    }

    override fun removeQuote(quote: String): Boolean {
        val items = getQuotes().toMutableList()
        val removed = items.remove(quote)
        if (!removed) return false

        saveQuotes(items)
        setFavorite(quote, false)
        return true
    }

    override fun updateQuote(oldQuote: String, newQuote: String): Boolean {
        val items = getQuotes().toMutableList()
        val index = items.indexOf(oldQuote)
        if (index < 0) return false

        val wasFavorite = isFavorite(oldQuote)
        items.removeAll { it == newQuote }
        val targetIndex = index.coerceAtMost(items.size)
        items[targetIndex] = newQuote
        saveQuotes(items)

        if (oldQuote != newQuote) {
            setFavorite(oldQuote, false)
            if (wasFavorite) setFavorite(newQuote, true)
        }
        return true
    }

    override fun isFavorite(quote: String): Boolean =
        quote in preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()

    override fun setFavorite(quote: String, favorite: Boolean) {
        val items = preferences.getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .toMutableSet()

        if (favorite) items.add(quote) else items.remove(quote)
        preferences.edit().putStringSet(KEY_FAVORITES, items).apply()
    }

    private fun saveQuotes(items: Collection<String>) {
        val array = JSONArray()
        items.distinct().forEach(array::put)
        preferences.edit()
            .putString(KEY_ITEMS_JSON, array.toString())
            .remove(KEY_ITEMS_LEGACY)
            .apply()
    }

    private fun decodeQuotes(json: String): List<String> {
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotEmpty() && value !in this) add(value)
                }
            }
        }.getOrElse { emptyList() }
    }

    private companion object {
        const val KEY_ITEMS_JSON = "items_json_v2"
        const val KEY_ITEMS_LEGACY = "items"
        const val KEY_FAVORITES = "favorite_quotes"
    }
}
