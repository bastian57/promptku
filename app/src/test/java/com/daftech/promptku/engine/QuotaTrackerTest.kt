package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotaTrackerTest {

    @Test
    fun remainingDecreasesWithCalls() {
        var counts: QuotaCounts = emptyMap()
        assertEquals(20, QuotaTracker.remaining(V2Provider.GEMINI_FLASH, counts))
        counts = QuotaTracker.record(counts, V2Provider.GEMINI_FLASH)
        assertEquals(19, QuotaTracker.remaining(V2Provider.GEMINI_FLASH, counts))
        assertTrue(QuotaTracker.canCall(V2Provider.GEMINI_FLASH, counts))
    }

    @Test
    fun exhaustedProviderCannotBeCalled() {
        var counts: QuotaCounts = emptyMap()
        repeat(20) { counts = QuotaTracker.record(counts, V2Provider.GEMINI_FLASH) }
        assertEquals(0, QuotaTracker.remaining(V2Provider.GEMINI_FLASH, counts))
        assertFalse(QuotaTracker.canCall(V2Provider.GEMINI_FLASH, counts))
        assertEquals(
            V2Provider.GEMINI_FLASH_LITE,
            QuotaTracker.nextAvailable(V2Provider.directorChain(), counts)
        )
    }

    @Test
    fun filmsEstimateUsesSevenCallsPerFilm() {
        // Kuota penuh: vision 1000/2 = 500 film, director (20+500+1000)/5 = 304 film.
        assertEquals(304, QuotaTracker.filmsEstimate(emptyMap()))
        // Gemini Flash habis (0) + Lite habis (0) + Groq 1000 -> (0+0+1000)/5 = 200.
        var counts: QuotaCounts = emptyMap()
        repeat(20) { counts = QuotaTracker.record(counts, V2Provider.GEMINI_FLASH) }
        repeat(500) { counts = QuotaTracker.record(counts, V2Provider.GEMINI_FLASH_LITE) }
        assertEquals(200, QuotaTracker.filmsEstimate(counts))
        // Vision habis -> 0 film walau director masih ada.
        repeat(1000) { counts = QuotaTracker.record(counts, V2Provider.GROQ_VISION) }
        assertEquals(0, QuotaTracker.filmsEstimate(counts))
    }

    @Test
    fun directorChainOrderIsFlashThenLiteThenGroq() {
        assertEquals(
            listOf(V2Provider.GEMINI_FLASH, V2Provider.GEMINI_FLASH_LITE, V2Provider.GROQ_GPT_OSS),
            V2Provider.directorChain()
        )
        assertEquals(
            listOf(V2Provider.GROQ_VISION, V2Provider.GEMINI_FLASH),
            V2Provider.visionChain()
        )
    }
}
