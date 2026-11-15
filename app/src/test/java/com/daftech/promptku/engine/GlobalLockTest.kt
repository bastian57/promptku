package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalLockTest {

    private fun fullLock(): GlobalLock = GlobalLock(
        world = Templates.gokuVsCellWorld,
        light = LightKind.GOLDEN_HOUR,
        style = VisualStyle.REALISTIC,
        lensMm = 35,
        character = Templates.gokuDna,
        secondCharacter = Templates.cellDna
    )

    @Test
    fun applyToOverridesOnlySetFields() {
        val input = ShotInput(
            genre = Genre.ACTION,
            shotSize = ShotSize.WIDE,
            actionEn = "runs across the ruins"
        )
        val lock = GlobalLock(light = LightKind.NEON, lensMm = 85)
        val result = lock.applyTo(input)
        assertEquals(LightKind.NEON, result.light)
        assertEquals(85, result.lensMm)
        assertEquals(Genre.ACTION, result.genre)
        assertEquals(ShotSize.WIDE, result.shotSize)
        assertEquals("runs across the ruins", result.actionEn)
        assertEquals(null, result.world)
    }

    @Test
    fun emptyLockLeavesInputUnchanged() {
        val input = ShotInput(
            genre = Genre.DRAMA,
            world = WorldLock("Kota", "a quiet town at dusk"),
            light = LightKind.MOONLIGHT,
            lensMm = 50
        )
        val result = GlobalLock().applyTo(input)
        assertEquals(input, result)
        assertTrue(GlobalLock().isEmpty())
    }

    @Test
    fun lockWinsOverDifferingSceneInput() {
        val input = ShotInput(
            world = WorldLock("Pantai", "a sunny beach"),
            light = LightKind.OVERCAST,
            style = VisualStyle.ANIME,
            lensMm = 24,
            character = CharacterDna("Rani", "a woman in her thirties")
        )
        val result = fullLock().applyTo(input)
        assertEquals(Templates.gokuVsCellWorld, result.world)
        assertEquals(LightKind.GOLDEN_HOUR, result.light)
        assertEquals(VisualStyle.REALISTIC, result.style)
        assertEquals(35, result.lensMm)
        assertEquals(Templates.gokuDna, result.character)
        assertEquals(Templates.cellDna, result.secondCharacter)
    }

    @Test
    fun continuityPoseStartTrimsAndKeepsText() {
        assertEquals("standing tall, facing Cell", continuityPoseStart("  standing tall, facing Cell  "))
        assertEquals("", continuityPoseStart(""))
        assertEquals("", continuityPoseStart("   "))
    }

    @Test
    fun summaryIdListsLockedFields() {
        val summary = fullLock().summaryId()
        assertTrue(summary.contains("Arena Cell Games"))
        assertTrue(summary.contains("Golden Hour"))
        assertTrue(summary.contains("Goku"))
        assertEquals("Belum dikunci", GlobalLock().summaryId())
    }

    @Test
    fun exportHeaderContainsLock() {
        val scenes = listOf(
            "Scene 01" to PromptEngine.build(
                ShotInput(title = "Scene 01", genre = Genre.ACTION, engine = TargetEngine.FLOW)
            )
        )
        val text = ExportText.buildProjectText("Film Tes", scenes, fullLock())
        assertTrue(text.contains("GLOBAL LOCK"))
        assertTrue(text.contains("Arena Cell Games"))
        assertTrue(text.contains("Goku"))
        assertFalse(ExportText.buildProjectText("Film Tes", scenes).contains("GLOBAL LOCK"))
    }
}
