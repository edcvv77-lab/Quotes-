package com.aiham.quotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun quoteCanBeEditedWithoutChangingOtherItems() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)
        service.submit("الأول")
        service.submit("الثاني")

        val result = service.updateQuote("الأول", "الأول بعد التعديل")

        assertEquals(QuoteEditResult.Updated("الأول بعد التعديل"), result)
        assertEquals(listOf("الثاني", "الأول بعد التعديل"), store.getQuotes())
    }

    @Test
    fun exactSecretCannotBeSavedThroughEdit() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)
        service.submit("نص عادي")

        val result = service.updateQuote("نص عادي", "aiham77rk")

        assertTrue(result is QuoteEditResult.SecretTriggerRejected)
        assertEquals(listOf("نص عادي"), store.getQuotes())
    }

    @Test
    fun quoteCanBeDeleted() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)
        service.submit("سيتم حذفه")

        assertTrue(service.deleteQuote("سيتم حذفه"))
        assertTrue(store.getQuotes().isEmpty())
    }

    @Test
    fun favoriteCanBeToggled() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)
        service.submit("مفضل")

        assertTrue(service.toggleFavorite("مفضل"))
        assertTrue(service.isFavorite("مفضل"))
        assertFalse(service.toggleFavorite("مفضل"))
        assertFalse(service.isFavorite("مفضل"))
    }

    @Test
    fun editingFavoriteMovesFavoriteStateToNewText() {
        val store = FakeQuoteStore()
        val service = QuoteService(store)
        service.submit("قديم")
        service.toggleFavorite("قديم")

        service.updateQuote("قديم", "جديد")

        assertFalse(service.isFavorite("قديم"))
        assertTrue(service.isFavorite("جديد"))
    }

    private class FakeQuoteStore : QuoteStore {
        private val items = mutableListOf<String>()
        private val favorites = mutableSetOf<String>()

        override fun getQuotes(): List<String> = items.toList()

        override fun addQuote(quote: String) {
            items.remove(quote)
            items.add(0, quote)
        }

        override fun removeQuote(quote: String): Boolean {
            favorites.remove(quote)
            return items.remove(quote)
        }

        override fun updateQuote(oldQuote: String, newQuote: String): Boolean {
            val index = items.indexOf(oldQuote)
            if (index < 0) return false

            val wasFavorite = oldQuote in favorites
            items.removeAt(index)
            items.remove(newQuote)
            items.add(index.coerceAtMost(items.size), newQuote)

            favorites.remove(oldQuote)
            if (wasFavorite) favorites.add(newQuote)
            return true
        }

        override fun isFavorite(quote: String): Boolean = quote in favorites

        override fun setFavorite(quote: String, favorite: Boolean) {
            if (favorite) favorites.add(quote) else favorites.remove(quote)
        }
    }
}
