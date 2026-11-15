package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptEngineTest {

    @Test
    fun clipCountMatchesDuration() {
        assertEquals(6, ScriptEngine.clipCount(1))
        assertEquals(18, ScriptEngine.clipCount(3))
        assertEquals(30, ScriptEngine.clipCount(5))
        assertEquals(60, ScriptEngine.clipCount(10))
    }

    @Test
    fun allocationSumsToClipCount() {
        listOf(1, 3, 5, 10).forEach { minutes ->
            val allocation = ScriptEngine.allocateClips(minutes)
            assertEquals(ScriptEngine.clipCount(minutes), allocation.sum())
        }
        assertEquals(8, ScriptEngine.allocateClips(5).size)
        assertEquals(5, ScriptEngine.allocateClips(3).size)
        assertEquals(5, ScriptEngine.allocateClips(1).size)
    }

    @Test
    fun buildFiveMinuteActionHasEightScenesThirtyShots() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        assertEquals(8, draft.scenes.size)
        assertEquals(30, draft.shots.size)
        assertEquals(30, draft.totalClips)
        assertEquals(90, draft.estimatedGenerations)
        // Beat checkpoint: sequence 2 = Pemicu, sequence 4 = Fun & Games.
        assertEquals("Pemicu", draft.scenes[1].beatName)
        assertEquals("Fun & Games", draft.scenes[3].beatName)
        assertEquals("Kehancuran", draft.scenes[5].beatName)
    }

    @Test
    fun buildTenMinuteHasSixtyShotsThreeMinuteHasEighteen() {
        val ten = ScriptEngine.build(ScriptTemplates.action10Min())
        assertEquals(60, ten.shots.size)
        assertEquals(8, ten.scenes.size)
        val three = ScriptEngine.build(ScriptTemplates.drama3Min())
        assertEquals(18, three.shots.size)
        assertEquals(5, three.scenes.size)
        val one = ScriptEngine.build(ScriptTemplates.horror1Min())
        assertEquals(6, one.shots.size)
        assertEquals(5, one.scenes.size)
    }

    @Test
    fun everySceneHasWantObstacleAndEveryShotOneAction() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        assertTrue(draft.scenes.all { it.want.isNotBlank() && it.obstacle.isNotBlank() })
        assertTrue(draft.shots.all { it.actionEn.isNotBlank() })
        assertTrue(draft.shots.all { it.actionEn.split(Regex("\\s+")).size <= 12 })
        assertTrue(draft.scenes.all { it.actionEn.split(Regex("\\s+")).size <= 40 })
        assertTrue(draft.scenes.all { it.dialogueLine.isBlank() || it.dialogueLine.split(Regex("\\s+")).size <= 12 })
    }

    @Test
    fun poseChainAndScreenDirectionConsistent() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val shots = draft.shots
        for (i in 1 until shots.size) {
            assertEquals(shots[i - 1].poseEnd, shots[i].poseStart)
        }
        // Arah layar + sisi axis konsisten di dalam satu scene.
        draft.scenes.forEach { scene ->
            val sceneShots = shots.filter { it.sceneOrder == scene.orderIndex }
            assertTrue(sceneShots.all { it.screenDirection == scene.screenDirection })
            assertTrue(sceneShots.all { it.axisSide == scene.axisSide })
        }
    }

    @Test
    fun coverageHasPriorityAShotsAndReAnchorMarkers() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        draft.scenes.forEach { scene ->
            val sceneShots = draft.shots.filter { it.sceneOrder == scene.orderIndex }
            assertTrue(sceneShots.any { it.priority == ShotPriority.A })
            assertEquals(ShotPriority.A, sceneShots.last().priority)
        }
        // Re-anchor tiap 5 klip (klip ke-5, 10, 15, ...).
        val reAnchors = draft.shots.filter { it.reAnchor }
        assertTrue(reAnchors.isNotEmpty())
        assertEquals(5, draft.shots.indexOf(reAnchors.first()) + 1)
    }

    @Test
    fun lockWinsInMassGenerate() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val lock = GlobalLock(
            world = WorldLock("Pantai", "a sunny beach"),
            light = LightKind.OVERCAST,
            style = VisualStyle.ANIME,
            lensMm = 24
        )
        val shotInput = ScriptEngine.toShotInput(draft, draft.shots.first(), lock)
        assertEquals("Pantai", shotInput.world?.name)
        assertEquals(LightKind.OVERCAST, shotInput.light)
        assertEquals(VisualStyle.ANIME, shotInput.style)
        assertEquals(24, shotInput.lensMm)
        assertTrue(shotInput.screenDirection.isNotBlank())
    }

    @Test
    fun regenerateSequenceChangesOnlyThatSequenceAndKeepsChain() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5), seed = 0)
        val rebuilt = ScriptEngine.regenerateSequence(draft, 2, seed = 1)
        // Sequence lain tidak berubah.
        draft.scenes.filter { it.orderIndex != 2 }.forEach { scene ->
            val twin = rebuilt.scenes.first { it.orderIndex == scene.orderIndex }
            assertEquals(scene.actionEn, twin.actionEn)
        }
        // Sequence yang diulang berubah aksinya.
        val oldScene = draft.scenes.first { it.orderIndex == 2 }
        val newScene = rebuilt.scenes.first { it.orderIndex == 2 }
        assertNotEquals(oldScene.actionEn, newScene.actionEn)
        // Rantai pose tetap utuh di semua shot.
        val shots = rebuilt.shots
        for (i in 1 until shots.size) {
            assertEquals(shots[i - 1].poseEnd, shots[i].poseStart)
        }
    }

    @Test
    fun naskahScoreTenForFiveMinuteActionExample() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val result = ScriptScore.score(draft)
        assertEquals(10, result.score)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun fountainExportHasStandardFormat() {
        val draft = ScriptEngine.build(ScriptTemplates.drama3Min())
        val fountain = FountainExport.buildFountain(draft)
        assertTrue(fountain.contains("Title: Surat Terakhir"))
        assertTrue(fountain.contains("FADE IN:"))
        assertTrue(fountain.contains("FADE OUT."))
        assertTrue(fountain.contains("INT.") || fountain.contains("EXT."))
        // Nama pengucap dialog ditulis caps.
        val dialogScene = draft.scenes.firstOrNull { it.dialogueLine.isNotBlank() }
        if (dialogScene != null) {
            assertTrue(fountain.contains(dialogScene.dialogueName.uppercase()))
            assertTrue(fountain.contains(dialogScene.dialogueLine))
        }
    }

    @Test
    fun flowPromptCarriesContinuityPhrase() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val shotInput = ScriptEngine.toShotInput(draft, draft.shots[1])
        val prompt = PromptEngine.build(shotInput.copy(engine = TargetEngine.FLOW))
        assertTrue(prompt.text.contains("Screen direction:"))
        assertTrue(prompt.text.contains("side of the axis"))
    }

    @Test
    fun promptScoreV3CatchesConsecutiveIdenticalShotSize() {
        val base = ShotInput(
            genre = Genre.ACTION,
            shotSize = ShotSize.MEDIUM,
            actionEn = "walks forward slowly",
            dialogueLine = "Hello there."
        )
        val same = base.copy(title = "Shot 2")
        val result = PromptScore.scoreV3(same, base)
        assertFalse(result.items.first { it.id == "shot_size_varies" }.passed)
        val different = base.copy(shotSize = ShotSize.CLOSEUP)
        val result2 = PromptScore.scoreV3(different, base)
        assertTrue(result2.items.first { it.id == "shot_size_varies" }.passed)
    }

    @Test
    fun promptScoreV3CatchesFastDialogue() {
        val longDialogue = (1..30).joinToString(" ") { "word" }
        val input = ShotInput(actionEn = "stands still", dialogueLine = longDialogue)
        val result = PromptScore.scoreV3(input)
        assertFalse(result.items.first { it.id == "dialogue_speed" }.passed)
    }
}
