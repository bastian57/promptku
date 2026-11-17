package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix8 v2 — render: peran fight -> efek peran (assignEffects),
// rantai kamera (chainCameras), frasa "continuing ..." (renderAll +
// PromptEngine). Inilah yang bikin 5 scene berantem berasa 1 take.
class V2FightRenderTest {

    private fun bible(): StoryBible = StoryBible(
        title = "Fall of Olympus",
        logline = "Ariadne fights Kallistos in the Aegean ruins.",
        genre = Genre.ACTION,
        characters = listOf(V2Character("Ariadne", listOf("scarred shield", "stormy eyes"))),
        antagonistName = "Kallistos",
        antagonistDescEn = "a ruthless commander",
        worldName = "Aegean Ruins",
        worldDescEn = "The shattered marble streets of a once-glorious Greek citadel.",
        lightEn = "Harsh sunlight",
        ambienceEn = "Echoing rumble",
        beats = listOf(
            V2Beat(0, "Pembuka", "the threat arrives"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the decisive clash"),
            V2Beat(4, "Penutup", "silence returns")
        ),
        episodes = listOf(V2Episode(1, "Fall of Olympus", "Ariadne vs Kallistos.", 18)),
        durationMinutes = 3
    )

    private fun plan(num: Int): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = "Ariadne trades blows with Kallistos number $num",
        cameraMove = "tracking shot", lensMm = 35, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = "wind, steel", effectId = "",
        wantEn = "win", obstacleEn = "Kallistos", transitionEn = "",
        narrationId = "", dialogueSpeaker = ""
    )

    private fun roles(): Map<Int, FightRole> {
        val b = bible()
        return BeatMap.fightRoles(BeatMap.build(b.totalScenes, b.beats), b.beats, true)
    }

    @Test
    fun assignEffectsUsesRoleEffects() {
        val plans = (1..18).map { plan(it) }
        val assigned = ModeRender.assignEffects(plans, isFight = true, totalBeats = 5, fightRoles = roles())
        assertEquals("fight_impact", assigned.first { it.sceneNumber == 10 }.effectId)
        assertEquals("fight_hero_hurt", assigned.first { it.sceneNumber == 12 }.effectId)
        assertEquals("finisher", assigned.first { it.sceneNumber == 13 }.effectId)
        // TENSION: 3 scene, 3 fragmen berbeda (tidak ada yang berulang).
        val tension = listOf(2, 5, 9).map { n -> assigned.first { it.sceneNumber == n }.effectId }
        assertEquals(3, tension.toSet().size)
        assertTrue(tension.all { it.startsWith("fight_tension_") })
        // Jurus siklus tidak mengandung efek peran.
        val cycle = assigned.map { it.effectId }
            .filter { it.isNotBlank() && it != "finisher" && !it.startsWith("fight_") }
        assertTrue(cycle.all { id -> EffectLibrary.FIGHT_MOVES.any { it.id == id } })
    }

    @Test
    fun assignEffectsWithoutRolesBehavesAsBefore() {
        // Penelepon lama (tanpa roles) tetap jalan: finisher di 13, siklus
        // 7 jurus — kompatibilitas mundur assignEffects.
        val plans = (1..18).map { plan(it) }
        val assigned = ModeRender.assignEffects(plans, isFight = true, totalBeats = 5)
        assertEquals("finisher", assigned.first { it.sceneNumber == 13 }.effectId)
        assertTrue(assigned.none { it.effectId.startsWith("fight_") })
    }

    @Test
    fun cameraChainLinksConsecutiveExchanges() {
        val plans = (1..18).map { plan(it) }
        val assigned = ModeRender.assignEffects(plans, isFight = true, totalBeats = 5, fightRoles = roles())
        val chained = ModeRender.chainCameras(assigned, roles())
        // S4/S7/S8/S15 EXCHANGE berurutan -> melanjutkan kamera scene sebelumnya.
        assertEquals(Pair(CameraMove.TRACKING, true), chained[4])
        assertEquals(Pair(CameraMove.TRACKING, true), chained[7])
        assertEquals(Pair(CameraMove.TRACKING, true), chained[8])
        assertEquals(Pair(CameraMove.WHIP_PAN, true), chained[15])
        // Rantai putus setelah TENSION (S2/S5/S9), IMPACT (S10),
        // HERO_HURT (S12), FINISHER (S13).
        assertEquals(Pair<CameraMove?, Boolean>(null, false), chained[3])
        assertEquals(Pair<CameraMove?, Boolean>(null, false), chained[6])
        assertEquals(Pair<CameraMove?, Boolean>(null, false), chained[11])
        assertEquals(Pair<CameraMove?, Boolean>(null, false), chained[14])
        assertEquals(Pair<CameraMove?, Boolean>(null, false), chained[10])
        // Tanpa peran -> tidak ada rantai.
        assertTrue(ModeRender.chainCameras(assigned, emptyMap()).isEmpty())
    }

    @Test
    fun renderAllWritesContinuingPhraseForChainedScenes() {
        val plans = (1..18).map { plan(it) }
        val rendered = ModeRender.renderAll(bible(), plans, V2Mode.TEXT_ONLY, isFight = true)
        val s7 = rendered.first { it.sceneNumber == 7 }
        assertTrue(s7.flowText, s7.flowText.contains("continuing tracking shot, following the subject from the previous shot"))
        val s6 = rendered.first { it.sceneNumber == 6 }
        assertFalse(s6.flowText, s6.flowText.contains("continuing"))
        val s10 = rendered.first { it.sceneNumber == 10 }
        assertFalse(s10.flowText, s10.flowText.contains("continuing"))
        // Mode B (renderReference) juga menulis frasa yang sama.
        val renderedB = ModeRender.renderAll(bible(), plans, V2Mode.REFERENCE, isFight = true)
        val s7b = renderedB.first { it.sceneNumber == 7 }
        assertTrue(s7b.flowText, s7b.flowText.contains("continuing tracking shot, following the subject from the previous shot"))
    }

    @Test
    fun cameraPhraseContinuesWhenChained() {
        val input = ShotInput(
            title = "Test",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            world = WorldLock("Ruins", "The shattered marble streets."),
            shotSize = ShotSize.MEDIUM,
            angle = CameraAngle.EYE,
            lensMm = 35,
            cameraMove = CameraMove.WHIP_PAN,
            light = LightKind.OVERCAST,
            actionEn = "trades a rapid flurry",
            poseEnd = "standing tall",
            audioEn = "wind",
            cameraContinues = true
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text, prompt.text.contains("continuing whip pan, fast motion blur, snapping across the action from the previous shot"))
        val plain = PromptEngine.build(input.copy(cameraContinues = false))
        assertFalse(plain.text, plain.text.contains("continuing"))
    }
}
