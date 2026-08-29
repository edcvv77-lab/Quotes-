package com.aiham.quotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteServiceTest {
    @Test
    fun normalQuoteIsSaved() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)

        val result = service.submit("الحياة جميلة")

        assertEquals(QuoteSubmission.Saved("الحياة جميلة"), result)
        assertEquals(listOf("الحياة جميلة"), store.getQuotes())
    }

    @Test
    fun blankQuoteIsRejected() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)

        val result = service.submit("   ")

        assertTrue(result is QuoteSubmission.Empty)
        assertTrue(store.getQuotes().isEmpty())
    }

    @Test
    fun exactTriggerOpensPrivateSpaceAndIsNotSaved() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)

        val result = service.submit("aiham77rk")

        assertTrue(result is QuoteSubmission.OpenPrivateSpace)
        assertTrue(store.getQuotes().isEmpty())
    }

    @Test
    fun similarTriggerDoesNotOpenPrivateSpace() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)

        val result = service.submit("aiham77rk ")

        assertEquals(QuoteSubmission.Saved("aiham77rk"), result)
        assertEquals(listOf("aiham77rk"), store.getQuotes())
    }

    @Test
    fun caseChangedTriggerDoesNotOpenPrivateSpace() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)

        val result = service.submit("Aiham77rk")

        assertEquals(QuoteSubmission.Saved("Aiham77rk"), result)
        assertEquals(listOf("Aiham77rk"), store.getQuotes())
    }

    private class FakeQuoteStore : QuoteStore {
        private val items = mutableListOf<String>()

        override fun getQuotes(): List<String> = items.toList()

        override fun addQuote(quote: String) {
            items.add(quote)
        }
    }
}
