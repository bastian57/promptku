package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MythFilterTest {

    @Test
    fun removesBannedPhrases() {
        val input = "A fight scene in 8K, masterpiece, ultra detailed, best quality, trending on ArtStation"
        val out = MythFilter.filterMyths(input)
        val lower = out.lowercase()
        assertFalse(lower.contains("8k"))
        assertFalse(lower.contains("masterpiece"))
        assertFalse(lower.contains("ultra detailed"))
        assertFalse(lower.contains("best quality"))
        assertFalse(lower.contains("trending on artstation"))
        assertTrue(out.contains("fight scene"))
    }

    @Test
    fun removes4kAndHyperrealistic() {
        val out = MythFilter.filterMyths("Shot in 4K, hyperrealistic skin, hyper-realistic eyes")
        val lower = out.lowercase()
        assertFalse(lower.contains("4k"))
        assertFalse(lower.contains("hyperrealistic"))
        assertFalse(lower.contains("hyper-realistic"))
    }

    @Test
    fun collapsesRepeatedCinematicToOne() {
        val out = MythFilter.collapseCinematic("cinematic shot, cinematic light, cinematic grade")
        val count = Regex("(?i)\\bcinematic\\b").findAll(out).count()
        assertEquals(1, count)
    }

    @Test
    fun countWordsHandlesBlank() {
        assertEquals(0, MythFilter.countWords(""))
        assertEquals(0, MythFilter.countWords("   "))
        assertEquals(3, MythFilter.countWords("one two three"))
    }

    @Test
    fun trimKeepsFirstSentence() {
        val text = "Goku stands in the arena. Dust drifts slowly. Birds sing far away. " +
            "The crowd murmurs. The sun sets slowly over the hills."
        val trimmed = MythFilter.trimToWords(text, 10)
        assertTrue(trimmed.startsWith("Goku stands in the arena."))
        assertTrue(MythFilter.countWords(trimmed) <= 10)
    }

    @Test
    fun detectsLicensedNames() {
        val found = MythFilter.detectLicensed("Goku fights Cell with a Kamehameha")
        assertTrue(found.contains("goku"))
        assertTrue(found.contains("cell"))
        assertTrue(found.contains("kamehameha"))
        assertTrue(MythFilter.detectLicensed("A baker makes bread").isEmpty())
    }

    @Test
    fun genericSuggestionsAreSane() {
        assertEquals("the hero", MythFilter.genericSuggestion("Goku"))
        assertEquals("the bio-android villain", MythFilter.genericSuggestion("Cell"))
        assertEquals("blue energy beam", MythFilter.genericSuggestion("Kamehameha"))
    }
}
