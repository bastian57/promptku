package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PromptScoreTest {

    @Test
    fun emptyInputScoresLowWithNamedMissing() {
        val result = PromptScore.score(ShotInput())
        assertTrue(result.score < 5)
        assertTrue(result.missing.isNotEmpty())
        val ids = result.missing.map { it.id }
        assertTrue(ids.contains("shot_lens"))
        assertTrue(ids.contains("dna_verbatim"))
        assertTrue(ids.contains("audio"))
    }

    @Test
    fun fullInputScoresTen() {
        val input = Templates.gokuVsCell().input
        val result = PromptScore.score(input)
        assertEquals(10, result.score)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun allTemplatesScoreTen() {
        assertTrue(Templates.all.size >= 12)
        for (template in Templates.all) {
            val result = PromptScore.score(template.input)
            assertEquals(
                "Template ${template.id} harus skor 10, dapat ${result.score}: " +
                    result.missing.joinToString { it.labelId },
                10,
                result.score
            )
        }
    }

    @Test
    fun allTemplatesProduceCleanPrompts() {
        val banned = listOf("8k", "4k", "masterpiece", "ultra detailed", "best quality", "trending on artstation", "hyperrealistic")
        for (template in Templates.all) {
            val prompt = PromptEngine.build(template.input)
            val lower = prompt.text.lowercase()
            for (word in banned) {
                assertTrue("Template ${template.id} mengandung kata mitos: $word", !lower.contains(word))
            }
        }
    }

    @Test
    fun diceRollIsCompleteAndScorable() {
        val rolled = Dice.roll(Random(42))
        val result = PromptScore.score(rolled)
        assertEquals(10, result.score)
        assertTrue(rolled.genre != null)
        assertTrue(rolled.style != null)
        assertTrue(rolled.engine != null)
        assertTrue(rolled.actionEn.isNotBlank())
    }

    @Test
    fun exportTextContainsAllScenes() {
        val scenes = Templates.all.take(2).map { template ->
            template.titleId to PromptEngine.build(template.input)
        }
        val text = ExportText.buildProjectText("Film Test", scenes)
        assertTrue(text.contains("Film Test"))
        assertTrue(text.contains("SCENE 1"))
        assertTrue(text.contains("SCENE 2"))
        assertTrue(text.contains("Total scene: 2"))
    }

    @Test
    fun genreAndStyleCountsAreLocked() {
        assertEquals(10, Genre.values().size)
        assertEquals(6, VisualStyle.values().size)
        assertEquals(4, TargetEngine.values().size)
    }
}
