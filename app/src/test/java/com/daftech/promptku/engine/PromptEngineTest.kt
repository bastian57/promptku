package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptEngineTest {

    private fun fullInput(engine: TargetEngine): ShotInput {
        return ShotInput(
            title = "Test Scene",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = engine,
            character = Templates.gokuDna,
            secondCharacter = Templates.cellDna,
            world = Templates.gokuVsCellWorld,
            shotSize = ShotSize.WIDE,
            angle = CameraAngle.LOW,
            lensMm = 35,
            cameraMove = CameraMove.CRANE_UP,
            light = LightKind.GOLDEN_HOUR,
            actionEn = "lands hard in a burst of dust, stands up slowly and faces Cell across the ring",
            poseStart = "standing at the far edge of the arena",
            poseEnd = "standing tall, facing Cell",
            materialEn = "dust explodes outward, ground cracks propagate from his boots",
            audioEn = "desert wind, distant crowd murmur fading, low orchestral drone",
            focusMode = FocusMode.FULL_STABLE,
            hideTactic = HideTactic.DISTANCE
        )
    }

    @Test
    fun flowUsesTimestampAndPositiveLock() {
        val prompt = PromptEngine.build(fullInput(TargetEngine.FLOW))
        assertEquals(TargetEngine.FLOW, prompt.engine)
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.text.contains("[00:05-00:10]"))
        assertTrue(prompt.text.contains("35mm"))
        assertTrue(prompt.text.contains("Goku"))
        assertTrue(prompt.text.contains("Cell"))
        assertTrue(prompt.text.contains("Same face and proportions throughout"))
        assertNull(prompt.negativePrompt)
        assertTrue(prompt.wordCount in 40..200)
    }

    @Test
    fun wanIsShortWithMandarinNegative() {
        val prompt = PromptEngine.build(fullInput(TargetEngine.WAN))
        assertEquals(TargetEngine.WAN, prompt.engine)
        assertEquals(PromptEngine.WAN_NEGATIVE_MANDARIN, prompt.negativePrompt)
        assertFalse(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.wordCount <= 120)
        assertTrue(prompt.wordCount >= 40)
    }

    @Test
    fun klingHasMoodAndAudio() {
        val input = fullInput(TargetEngine.KLING).copy(
            dialogueName = "Goku",
            dialogueLine = "I will finish this."
        )
        val prompt = PromptEngine.build(input)
        assertEquals(TargetEngine.KLING, prompt.engine)
        assertTrue(prompt.text.contains("Mood:"))
        assertTrue(prompt.text.contains("Audio:"))
        assertTrue(prompt.text.contains("Goku (determined):"))
        assertNull(prompt.negativePrompt)
        assertTrue(prompt.wordCount in 40..120)
    }

    @Test
    fun seedanceHasThreeSegmentsAndConstraintsNoNegative() {
        val prompt = PromptEngine.build(fullInput(TargetEngine.SEEDANCE))
        assertEquals(TargetEngine.SEEDANCE, prompt.engine)
        assertTrue(prompt.text.contains("[0-2s]"))
        assertTrue(prompt.text.contains("[2-4s]"))
        assertTrue(prompt.text.contains("[4-6s]"))
        assertTrue(prompt.text.contains("Constraints:"))
        assertNull(prompt.negativePrompt)
        assertTrue(prompt.wordCount in 40..120)
    }

    @Test
    fun fourEnginesProduceDifferentFormulas() {
        val input = fullInput(TargetEngine.FLOW)
        val results = PromptEngine.buildAll(input)
        assertEquals(4, results.size)
        val texts = results.map { it.text }.toSet()
        assertEquals(4, texts.size)
        assertNotEquals(results[0].text, results[1].text)
    }

    @Test
    fun gokuTemplatePassesFlow() {
        val template = Templates.gokuVsCell()
        val prompt = PromptEngine.build(template.input)
        assertTrue(prompt.text.contains("Goku"))
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.wordCount in 40..200)
        assertTrue(prompt.warnings.isNotEmpty())
        assertTrue(prompt.warnings.first().contains("filter IP"))
    }

    @Test
    fun mythWordsNeverSurviveBuild() {
        val dirty = fullInput(TargetEngine.FLOW).copy(
            actionEn = "fights in 8K masterpiece style, ultra detailed, best quality",
            materialEn = "trending on ArtStation, hyperrealistic dust"
        )
        val prompt = PromptEngine.build(dirty)
        val lower = prompt.text.lowercase()
        assertFalse(lower.contains("8k"))
        assertFalse(lower.contains("masterpiece"))
        assertFalse(lower.contains("ultra detailed"))
        assertFalse(lower.contains("best quality"))
        assertFalse(lower.contains("trending on artstation"))
        assertFalse(lower.contains("hyperrealistic"))
    }

    @Test
    fun wanDropsDialogueWithWarning() {
        val input = fullInput(TargetEngine.WAN).copy(
            dialogueName = "Goku",
            dialogueLine = "Hello."
        )
        val prompt = PromptEngine.build(input)
        assertFalse(prompt.text.contains("Hello"))
        assertTrue(prompt.warnings.any { it.contains("tanpa audio") })
    }

    @Test
    fun styleLookDiffersPerStyle() {
        val realistic = PromptEngine.styleLook(VisualStyle.REALISTIC)
        val anime = PromptEngine.styleLook(VisualStyle.ANIME)
        val noir = PromptEngine.styleLook(VisualStyle.NOIR)
        assertNotEquals(realistic.first, anime.first)
        assertNotEquals(realistic.second, noir.second)
        assertTrue(realistic.second.contains("teal-and-orange"))
        assertTrue(anime.first.contains("anime"))
    }
}
