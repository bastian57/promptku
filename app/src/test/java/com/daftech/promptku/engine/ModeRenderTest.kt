package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeRenderTest {

    private fun bible(): StoryBible = StoryBible(
        title = "Bejo vs Raksasa",
        logline = "Bejo melawan raksasa batu di kota yang runtuh.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character("Bejo", listOf("torn denim jacket", "determined eyes", "black hair"))
        ),
        antagonistName = "Raksasa",
        antagonistDescEn = "a 2.8-meter-tall giant, cracked stone skin",
        worldName = "Kota Runtuh",
        worldDescEn = "a ruined metropolitan city at night, broken towers, smoke rising in the distance",
        lightEn = "cold moonlight",
        ambienceEn = "distant sirens, wind through broken glass",
        beats = listOf(
            V2Beat(0, "Pembuka", "Bejo walks into the ruined city"),
            V2Beat(1, "Pemicu", "the giant appears"),
            V2Beat(2, "Eskalasi", "the fight begins"),
            V2Beat(3, "Klimaks", "Bejo lands the final blow"),
            V2Beat(4, "Penutup", "silence returns")
        ),
        episodes = listOf(V2Episode(1, "Bejo vs Raksasa", "Bejo melawan raksasa.", 6)),
        durationMinutes = 1
    )

    private fun plan(num: Int, action: String, dialogueId: String = ""): V2ScenePlan =
        V2ScenePlan(
            sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
            lensMm = 50, shotSize = "medium", angle = "eye",
            dialogueId = dialogueId, dialogueEn = "",
            audioEn = "stone cracking", effectId = "",
            wantEn = "survive", obstacleEn = "the giant", transitionEn = ""
        )

    @Test
    fun modeAContainsFullSubjectAndWorld() {
        val rendered = ModeRender.renderScene(bible(), plan(1, "raises his fist and steps forward"), V2Mode.TEXT_ONLY)
        assertTrue(rendered.flowText.contains("Bejo"))
        assertTrue(rendered.flowText.contains("torn denim jacket"))
        assertTrue(rendered.flowText.contains("ruined metropolitan city at night"))
        assertTrue(rendered.flowText.contains("50mm"))
    }

    @Test
    fun modeBPointsAtImageAndNeverRedescribesSubject() {
        val rendered = ModeRender.renderScene(bible(), plan(1, "raises his fist and steps forward"), V2Mode.REFERENCE)
        assertTrue(rendered.flowText.contains("reference image"))
        assertFalse(rendered.flowText.contains("torn denim jacket"))
        assertFalse(rendered.flowText.contains("determined eyes"))
        assertFalse(rendered.flowText.contains("black hair"))
        assertTrue(rendered.flowText.contains("Bejo"))
        assertTrue(rendered.flowText.contains("ruined metropolitan city at night"))
    }

    @Test
    fun modeBEngineVariantsDiffer() {
        val rendered = ModeRender.renderScene(bible(), plan(1, "dodges under the giant arm"), V2Mode.REFERENCE)
        assertFalse(rendered.flowText == rendered.klingText)
        assertTrue(rendered.seedanceText.contains("Reference: Image 1"))
        assertTrue(rendered.wanText.length < rendered.flowText.length)
    }

    @Test
    fun assignEffectsKeepsFightMovesUniqueUntilPoolRunsOut() {
        val plans = (1..18).map { plan(it, "fight move number $it") }
        val assigned = ModeRender.assignEffects(plans, isFight = true, totalBeats = 5)
        val fightIds = assigned.filter { it.effectId.isNotBlank() && it.effectId != "finisher" }
            .map { it.effectId }
        // Pool jurus = 7 (di luar finisher); 7 scene berantem pertama
        // harus beda semua — keunikan aksi se-film tetap dijamin LLM
        // lewat actionEn yang berbeda (gerbang G2), bukan lewat id efek.
        assertEquals(7, fightIds.take(7).toSet().size)
        assertTrue(fightIds.all { id -> EffectLibrary.FIGHT_MOVES_ALL.any { it.id == id } })
        assertEquals(1, assigned.count { it.effectId == "finisher" })
        assertEquals("", assigned.maxByOrNull { it.sceneNumber }?.effectId)
        // Finisher di scene pertama klimaks (beat 3, 18 scene / 5 beat = 4 scene/beat -> scene 13).
        assertEquals("finisher", assigned.first { it.sceneNumber == 13 }.effectId)
    }

    @Test
    fun assignEffectsLeavesNonFightFilmsWithoutFightMoves() {
        val plans = (1..6).map { plan(it, "walks slowly number $it") }
        val assigned = ModeRender.assignEffects(plans, isFight = false, totalBeats = 5)
        assertFalse(assigned.any { it.effectId == "finisher" })
        assertTrue(assigned.none { EffectLibrary.FIGHT_MOVES_ALL.any { m -> m.id == it.effectId } })
    }

    @Test
    fun effectCameraAndLensOverridePlan() {
        val grapple = plan(1, "grapples the giant").copy(effectId = "grapple")
        val shot = ModeRender.effectiveShot(grapple)
        assertEquals(CameraMove.RACK_FOCUS, shot.cameraMove)
        assertEquals(85, shot.lensMm)
        val rendered = ModeRender.renderScene(bible(), grapple, V2Mode.TEXT_ONLY)
        assertTrue(rendered.flowText.contains("85mm"))
        assertTrue(rendered.flowText.contains("rack focus"))
    }

    @Test
    fun effectFragmentReplacesTheGiantWithAntagonist() {
        val frag = ModeRender.effectFragment(
            EffectLibrary.ALL.first { it.id == "grapple" }, bible()
        )
        assertFalse(frag.contains("the giant"))
    }
}
