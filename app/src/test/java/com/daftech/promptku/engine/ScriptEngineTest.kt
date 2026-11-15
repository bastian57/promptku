package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes mesin naskah FLAT v1.3 — 1 scene = 1 klip 10 detik, daftar Scene 01..N.
// Menggantikan tes coverage v1.2 (model 2 tingkat Sequence -> Shot DITOLAK
// Putra 19:08 WIB karena "bertele-tele").
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
    fun buildFiveMinuteActionHasThirtyFlatScenes() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        assertEquals(30, draft.scenes.size)
        assertEquals(30, draft.totalClips)
        assertEquals(30, draft.estimatedGenerations)
        // 8 bab (beat) dalam urutan kanonik.
        val beats = draft.scenes.map { it.beatName }.distinct()
        assertEquals(
            listOf(
                "Dunia Biasa", "Pemicu", "Rencana & Taruhan", "Fun & Games",
                "Harga Kemenangan", "Kehancuran", "Kebangkitan", "Dunia Baru"
            ),
            beats
        )
        // Nomor scene 1..30, timecode naik.
        assertEquals(1, draft.scenes.first().sceneNumber)
        assertEquals(30, draft.scenes.last().sceneNumber)
        assertEquals("00:00-00:10", draft.scenes.first().timecode())
        assertEquals("04:50-05:00", draft.scenes.last().timecode())
    }

    @Test
    fun buildTenMinuteHasSixtyScenesThreeMinuteHasEighteen() {
        val ten = ScriptEngine.build(ScriptTemplates.action10Min())
        assertEquals(60, ten.scenes.size)
        assertEquals(8, ten.scenes.map { it.sequenceIndex }.distinct().size)
        val three = ScriptEngine.build(ScriptTemplates.drama3Min())
        assertEquals(18, three.scenes.size)
        assertEquals(5, three.scenes.map { it.sequenceIndex }.distinct().size)
        val one = ScriptEngine.build(ScriptTemplates.horror1Min())
        assertEquals(6, one.scenes.size)
        assertEquals(5, one.scenes.map { it.sequenceIndex }.distinct().size)
    }

    @Test
    fun everySceneHasWantObstacleAndOneAction() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        assertTrue(draft.scenes.all { it.want.isNotBlank() && it.obstacle.isNotBlank() })
        assertTrue(draft.scenes.all { it.actionEn.isNotBlank() })
        assertTrue(draft.scenes.all { it.actionEn.split(Regex("\\s+")).size <= 12 })
        assertTrue(draft.scenes.all { scene ->
            val line = scene.dialogue ?: return@all true
            maxOf(
                line.textEn.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                line.textId.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            ) <= 12
        })
    }

    @Test
    fun noActionRepeatsWithinABeat() {
        listOf(1, 3, 5, 10).forEach { minutes ->
            val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = minutes))
            draft.scenes.groupBy { it.sequenceIndex }.values.forEach { beatScenes ->
                val actions = beatScenes.map { it.actionEn }
                assertEquals(actions.size, actions.distinct().size)
            }
        }
        // Genre generik juga (pool generik 8 aksi per beat).
        val drama = ScriptEngine.build(ScriptTemplates.drama3Min())
        drama.scenes.groupBy { it.sequenceIndex }.values.forEach { beatScenes ->
            val actions = beatScenes.map { it.actionEn }
            assertEquals(actions.size, actions.distinct().size)
        }
    }

    @Test
    fun poseChainAndScreenDirectionConsistent() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val scenes = draft.scenes
        for (i in 1 until scenes.size) {
            assertEquals(scenes[i - 1].poseEnd, scenes[i].poseStart)
        }
        // Arah layar + sisi axis konsisten di dalam satu beat.
        scenes.groupBy { it.sequenceIndex }.values.forEach { beatScenes ->
            assertTrue(beatScenes.all { it.screenDirection == beatScenes.first().screenDirection })
            assertTrue(beatScenes.all { it.axisSide == beatScenes.first().axisSide })
        }
    }

    @Test
    fun dialogueAttachedExactlyOncePerBeat() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        draft.scenes.groupBy { it.sequenceIndex }.values.forEach { beatScenes ->
            assertTrue(beatScenes.count { it.dialogue != null && !it.dialogue.isEmpty() } <= 1)
        }
        // Ada dialog di draft (beat 1-7 punya pool dialog).
        assertTrue(draft.scenes.any { it.dialogue != null && !it.dialogue.isEmpty() })
    }

    @Test
    fun reAnchorEveryFifthClip() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val reAnchors = draft.scenes.filter { it.reAnchor }
        assertTrue(reAnchors.isNotEmpty())
        assertEquals(listOf(5, 10, 15, 20, 25, 30), reAnchors.map { it.sceneNumber })
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
        val shotInput = ScriptEngine.toShotInput(draft, draft.scenes.first(), lock)
        assertEquals("Pantai", shotInput.world?.name)
        assertEquals(LightKind.OVERCAST, shotInput.light)
        assertEquals(VisualStyle.ANIME, shotInput.style)
        assertEquals(24, shotInput.lensMm)
        assertTrue(shotInput.screenDirection.isNotBlank())
    }

    @Test
    fun regenerateSceneChangesOnlyThatSceneAndKeepsChain() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5), seed = 0)
        val rebuilt = ScriptEngine.regenerateScene(draft, 5, seed = 1)
        // Scene lain tidak berubah.
        draft.scenes.filter { it.orderIndex != 5 }.forEach { scene ->
            val twin = rebuilt.scenes.first { it.orderIndex == scene.orderIndex }
            assertEquals(scene.actionEn, twin.actionEn)
        }
        // Scene yang diulang berubah aksinya, pose + dialog tetap.
        val oldScene = draft.scenes.first { it.orderIndex == 5 }
        val newScene = rebuilt.scenes.first { it.orderIndex == 5 }
        assertNotEquals(oldScene.actionEn, newScene.actionEn)
        assertEquals(oldScene.poseStart, newScene.poseStart)
        assertEquals(oldScene.poseEnd, newScene.poseEnd)
        assertEquals(oldScene.dialogue, newScene.dialogue)
        // Rantai pose tetap utuh.
        val scenes = rebuilt.scenes
        for (i in 1 until scenes.size) {
            assertEquals(scenes[i - 1].poseEnd, scenes[i].poseStart)
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
        val dialogScene = draft.scenes.firstOrNull { it.dialogue != null && !it.dialogue.isEmpty() }
        if (dialogScene != null) {
            assertTrue(fountain.contains(dialogScene.dialogue!!.speaker.uppercase()))
            assertTrue(fountain.contains(dialogScene.dialogue!!.textEn))
        }
    }

    @Test
    fun flowPromptCarriesContinuityPhrase() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val shotInput = ScriptEngine.toShotInput(draft, draft.scenes[1])
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
