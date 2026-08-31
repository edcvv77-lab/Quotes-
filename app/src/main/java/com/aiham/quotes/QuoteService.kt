package com.aiham.quotes

import com.aiham.quotes.security.SecurityTrigger

sealed class QuoteSubmission {
    object Empty : QuoteSubmission()
    object OpenPrivateSpace : QuoteSubmission()
    data class Saved(val quote: String) : QuoteSubmission()
}

sealed class QuoteEditResult {
    object Empty : QuoteEditResult()
    object SecretTriggerRejected : QuoteEditResult()
    data class Updated(val quote: String) : QuoteEditResult()
    object NotFound : QuoteEditResult()
}

class QuoteService(
    private val store: QuoteStore
) {
    fun submit(rawInput: String): QuoteSubmission {
        if (rawInput.isBlank()) {
            return QuoteSubmission.Empty
        }

        if (SecurityTrigger.checkTrigger(rawInput)) {
            return QuoteSubmission.OpenPrivateSpace
        }

        val quote = rawInput.trim()
        if (quote.isEmpty()) {
            return QuoteSubmission.Empty
        }

        store.addQuote(quote)
        return QuoteSubmission.Saved(quote)
    }

    fun updateQuote(oldQuote: String, rawInput: String): QuoteEditResult {
        if (rawInput.isBlank()) return QuoteEditResult.Empty
        if (SecurityTrigger.checkTrigger(rawInput)) {
            return QuoteEditResult.SecretTriggerRejected
        }

        val updated = rawInput.trim()
        if (updated.isEmpty()) return QuoteEditResult.Empty

        return if (store.updateQuote(oldQuote, updated)) {
            QuoteEditResult.Updated(updated)
        } else {
            QuoteEditResult.NotFound
        }
    }

    fun deleteQuote(quote: String): Boolean = store.removeQuote(quote)

    fun toggleFavorite(quote: String): Boolean {
        val next = !store.isFavorite(quote)
        store.setFavorite(quote, next)
        return next
    }

    fun isFavorite(quote: String): Boolean = store.isFavorite(quote)

    fun getQuotes(): List<String> = store.getQuotes()
}
