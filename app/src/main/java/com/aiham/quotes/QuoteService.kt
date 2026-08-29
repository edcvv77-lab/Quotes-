package com.aiham.quotes

import com.aiham.quotes.security.SecurityTrigger

sealed class QuoteSubmission {
    object Empty : QuoteSubmission()
    object OpenPrivateSpace : QuoteSubmission()
    data class Saved(val quote: String) : QuoteSubmission()
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

    fun getQuotes(): List<String> = store.getQuotes()
}
