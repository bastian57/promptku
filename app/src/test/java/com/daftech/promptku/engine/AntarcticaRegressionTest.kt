package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes regresi sinopsis Antartika persis (audit 16 Nov 2026, 15:52 WIB).
// Dulu: jaket sobek 18/18, fist raised 18/18, dunia Pantai 18/18,
// S1–S6 motor/paket/chip (kurir). Sekarang harus NOL semua itu.
class AntarcticaRegressionTest {

    private val synopsis =
        "Pertempuran hebat super humanoid di atas lautan Antartika. " +
            "kedua humanoid melayang di atas lautan sambil bertarung hebat"

    @Test
    fun notACourierStory() {
        assertFalse(ScriptPools.isCourierStory(synopsis))
        assertFalse(V2Pipeline.isCourierHint(synopsis))
    }

    @Test
    fun worldIsAntarcticIceNotBeach() {
        val inference = StoryInfer.infer(synopsis)
        assertEquals("Lautan Es Antartika", inference.worldName)
        assertTrue(inference.worldDescEn.contains("ice", ignoreCase = true))
        assertFalse(inference.worldDescEn.contains("beach", ignoreCase = true))
        assertFalse(inference.worldDescEn.contains("benches", ignoreCase = true))
    }

    @Test
    fun characterCardHasNoTornJacketOrFist() {
        val inference = StoryInfer.infer(synopsis)
        val cardText = inference.characterCard.toDescription()
        assertFalse(cardText.contains("torn jacket", ignoreCase = true))
        assertFalse(cardText.contains("fist raised", ignoreCase = true))
        assertTrue(cardText.contains("metallic armor", ignoreCase = true))
    }

    @Test
    fun antagonistIsSecondHumanoid() {
        val inference = StoryInfer.infer(synopsis)
        assertEquals("Humanoid Kedua", inference.antagonist)
        assertTrue(inference.antagonistDescEn.contains("humanoid", ignoreCase = true))
    }

    @Test
    fun offlineDraftHasZeroCourierAndBeachContent() {
        val inference = StoryInfer.infer(synopsis)
        val input = StoryInfer.toScriptInput(synopsis, "Hantu Laut", 3, inference)
        assertFalse(input.protagonistDescEn.contains("torn jacket", ignoreCase = true))
        val draft = ScriptEngine.build(input)
        assertEquals(18, draft.scenes.size)
        val allText = draft.scenes.joinToString(" ") { it.actionEn } +
            " " + draft.scenes.joinToString(" ") { it.slugline }
        assertFalse(allText.contains("motorcycle", ignoreCase = true))
        assertFalse(allText.contains("parcel", ignoreCase = true))
        assertFalse(allText.contains("chip", ignoreCase = true))
        assertFalse(allText.contains("torn jacket", ignoreCase = true))
        assertFalse(allText.contains("fist raised", ignoreCase = true))
    }

    @Test
    fun renderedOfflinePromptsPassWorldGate() {
        val inference = StoryInfer.infer(synopsis)
        val input = StoryInfer.toScriptInput(synopsis, "Hantu Laut", 3, inference)
        val draft = ScriptEngine.build(input)
        val bible = StoryBible(
            title = input.title,
            logline = synopsis,
            genre = inference.genre,
            characters = listOf(
                V2Character("Hantu Laut", listOf("smooth pale skin", "glowing eyes", "metallic armor"))
            ),
            antagonistName = inference.antagonist,
            antagonistDescEn = inference.antagonistDescEn,
            worldName = inference.worldName,
            worldDescEn = inference.worldDescEn,
            lightEn = "pale cold light",
            ambienceEn = inference.ambienceEn,
            beats = listOf(
                V2Beat(0, "Pembuka", "two humanoids hover above the frozen ocean"),
                V2Beat(1, "Pemicu", "the battle begins"),
                V2Beat(2, "Eskalasi", "the fight escalates"),
                V2Beat(3, "Klimaks", "the second humanoid falls"),
                V2Beat(4, "Penutup", "silence returns over the ice")
            ),
            episodes = listOf(V2Episode(1, input.title, synopsis, 18)),
            durationMinutes = 3
        )
        val plans = draft.scenes.map { scene ->
            V2ScenePlan(
                sceneNumber = scene.sceneNumber,
                actionEn = scene.actionEn,
                cameraMove = scene.cameraMove.phraseEn,
                lensMm = scene.lensMm,
                shotSize = scene.shotSize.name,
                angle = scene.angle.name,
                dialogueId = scene.dialogue?.textId.orEmpty(),
                dialogueEn = scene.dialogue?.textEn.orEmpty(),
                audioEn = "",
                effectId = scene.effectId,
                wantEn = scene.want,
                obstacleEn = scene.obstacle,
                transitionEn = scene.transition
            )
        }
        val rendered = ModeRender.renderAll(bible, plans, V2Mode.TEXT_ONLY, isFight = true)
        assertEquals(18, rendered.size)
        val allFlow = rendered.joinToString(" ") { it.flowText }
        assertFalse(allFlow.contains("torn jacket", ignoreCase = true))
        assertFalse(allFlow.contains("fist raised", ignoreCase = true))
        assertFalse(allFlow.contains("beach", ignoreCase = true))
        assertTrue(allFlow.contains("ice", ignoreCase = true))
        val g1 = ValidationGates.runGates(bible, rendered, hasImages = false)
            .first { it.id == "G1" }
        assertTrue("G1 gagal: ${g1.failures}", g1.passed)
    }
}
