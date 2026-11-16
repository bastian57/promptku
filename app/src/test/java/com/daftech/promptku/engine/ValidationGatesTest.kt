package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationGatesTest {

    private fun bible(world: String = "a frozen Antarctic ocean, endless ice fields"): StoryBible =
        StoryBible(
            title = "T",
            logline = "L",
            genre = Genre.ACTION,
            characters = listOf(
                V2Character("Hantu Laut", listOf("smooth pale skin", "glowing eyes", "metallic armor"))
            ),
            antagonistName = "Humanoid Kedua",
            antagonistDescEn = "a second humanoid",
            worldName = "Lautan Es Antartika",
            worldDescEn = world,
            lightEn = "pale cold light",
            ambienceEn = "cold wind, cracking ice",
            beats = listOf(V2Beat(0, "Pembuka", "a"), V2Beat(1, "Klimaks", "b")),
            episodes = listOf(V2Episode(1, "T", "L", 3)),
            durationMinutes = 1
        )

    private fun scene(
        num: Int,
        action: String,
        flow: String,
        effectId: String = "",
        dialogueId: String = "",
        dialogueEn: String = ""
    ): V2ScenePrompt = V2ScenePrompt(
        sceneNumber = num,
        plan = V2ScenePlan(
            sceneNumber = num, actionEn = action, cameraMove = "static",
            lensMm = 35, shotSize = "medium", angle = "eye",
            dialogueId = dialogueId, dialogueEn = dialogueEn,
            audioEn = "", effectId = effectId, wantEn = "", obstacleEn = "", transitionEn = ""
        ),
        flowText = flow, klingText = flow, seedanceText = flow, wanText = flow
    )

    private val worldSig = "a frozen antarctic ocean, endless ice fields"

    @Test
    fun g1PassesWhenWorldVerbatim() {
        val scenes = listOf(
            scene(1, "hovers above the ice", "wide shot in $worldSig, hero hovers."),
            scene(2, "dives toward the ice", "medium shot in $worldSig, hero dives."),
            scene(3, "stands still on the ice", "close-up in $worldSig, hero stands.")
        )
        val g1 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G1" }
        assertTrue(g1.failures.toString(), g1.passed)
    }

    @Test
    fun g1FailsWhenWorldMissing() {
        val scenes = listOf(
            scene(1, "hovers", "wide shot on a beach at sunset, hero hovers.")
        )
        val g1 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G1" }
        assertFalse(g1.passed)
    }

    @Test
    fun g2FailsOnRepeatedActionAndClosingFight() {
        val scenes = listOf(
            scene(1, "punches the giant", "in $worldSig punches."),
            scene(2, "punches the giant", "in $worldSig punches again."),
            scene(3, "punches the giant", "in $worldSig punches after climax.")
        )
        val g2 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G2" }
        assertFalse(g2.passed)
        assertTrue(g2.failures.any { it.contains("sama dengan scene 1") })
        assertTrue(g2.failures.any { it.contains("Penutup") })
    }

    @Test
    fun g2FailsOnTwoFinishers() {
        val scenes = listOf(
            scene(1, "charges forward", "in $worldSig charges.", effectId = "finisher"),
            scene(2, "lands the final blow", "in $worldSig final blow.", effectId = "finisher"),
            scene(3, "stands still", "in $worldSig stands.")
        )
        val g2 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G2" }
        assertFalse(g2.passed)
    }

    @Test
    fun g3FailsWhenEffectLensMissingFromText() {
        // grapple = rack focus 85mm di EffectLibrary.
        val scenes = listOf(
            scene(1, "grapples the giant", "in $worldSig grapples, slow pan left, 35mm lens.", effectId = "grapple"),
            scene(2, "stands still", "in $worldSig stands."),
            scene(3, "walks away", "in $worldSig walks.")
        )
        val g3 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G3" }
        assertFalse(g3.passed)
    }

    @Test
    fun g4ModeBRequiresPointerAndNoFullDescription() {
        val good = listOf(
            scene(1, "hovers", "Using the provided images for Hantu Laut, create a wide shot of Hantu Laut in the reference images hovering above the ice. Setting: Lautan Es Antartika."),
            scene(2, "dives", "Using the provided images for Hantu Laut, create a medium shot of Hantu Laut in the reference images diving. Setting: Lautan Es Antartika."),
            scene(3, "stands", "Using the provided images for Hantu Laut, create a close-up of Hantu Laut in the reference images standing. Setting: Lautan Es Antartika.")
        )
        val g4good = ValidationGates.runGates(bible(), good, true).first { it.id == "G4" }
        assertTrue(g4good.failures.toString(), g4good.passed)

        val bad = listOf(
            scene(1, "hovers", "wide shot of Hantu Laut, smooth pale skin, glowing eyes, metallic armor, hovering. Setting: Lautan Es Antartika.")
        )
        val g4bad = ValidationGates.runGates(bible(), bad, true).first { it.id == "G4" }
        assertFalse(g4bad.passed)
    }

    @Test
    fun g5FailsOnLongDialogue() {
        val scenes = listOf(
            scene(1, "stands", "in $worldSig stands.", dialogueId = "Ini kalimat yang sangat panjang sekali melebihi delapan kata"),
            scene(2, "walks", "in $worldSig walks."),
            scene(3, "sits", "in $worldSig sits.")
        )
        val g5 = ValidationGates.runGates(bible(), scenes, false).first { it.id == "G5" }
        assertFalse(g5.passed)
    }

    @Test
    fun g6PassesOnlyWhenFailuresAreFlagged() {
        val unflagged = listOf(
            scene(1, "hovers", "wide shot on a beach, hero hovers.")
        )
        val g6bad = ValidationGates.runGates(bible(), unflagged, false).first { it.id == "G6" }
        assertFalse(g6bad.passed)

        val flagged = listOf(
            scene(1, "hovers", "wide shot on a beach, hero hovers.").copy(
                needsReview = true, reviewReason = "G1"
            )
        )
        val g6good = ValidationGates.runGates(bible(), flagged, false).first { it.id == "G6" }
        assertTrue(g6good.passed)
    }
}
