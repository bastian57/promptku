package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix8 v2 — fragmen fight CEPAT + efek peran + kamera cepat.
// Akar keluhan lapangan (Olympus S1–S5, 17 Nov 2026): bahasa slow-mo ada
// DI KODE — fragmen "the impact freezes for a heartbeat" + "sweat sprays
// in slow motion" + kamera "slow dolly in" di semua jurus. Tes ini mengunci
// bahwa jurus biasa TIDAK mengandung kata slow-mo dan kameranya cepat.
class V2FightFragmentsTest {

    private val CONTACT_VERBS = listOf("punch", "kick", "slam", "strike", "hit", "attack", "clash")

    @Test
    fun fastFightMovesContainNoSlowMotion() {
        // 7 jurus biasa (FIGHT_MOVES): tidak satu pun boleh mengandung
        // momen slow-mo — slow-mo hanya di fight_impact + finisher.
        for (effect in EffectLibrary.FIGHT_MOVES) {
            assertFalse(
                "${effect.id}: ${effect.fragmentEn}",
                ArcLexicon.isSlowMoMoment(effect.fragmentEn)
            )
        }
        assertEquals(7, EffectLibrary.FIGHT_MOVES.size)
    }

    @Test
    fun finisherKeepsSlowMotion() {
        val finisher = EffectLibrary.byId("finisher")!!
        assertTrue(ArcLexicon.isSlowMoMoment(finisher.fragmentEn))
        assertEquals(CameraMove.CRANE_UP, finisher.cameraMove)
    }

    @Test
    fun impactFragmentIsSpeedRampWithSnapBack() {
        val impact = EffectLibrary.byId("fight_impact")!!
        assertTrue(impact.fragmentEn.contains("slow motion"))
        assertTrue(impact.fragmentEn.contains("snaps back to full speed"))
        assertTrue(impact.fragmentEn.contains("face"))
        assertTrue(impact.fragmentEn.contains("eyes wide with pain"))
        assertEquals(CameraMove.PUSH_IN, impact.cameraMove)
        assertTrue(impact.roleOnly)
    }

    @Test
    fun heroHurtFragmentShowsHeroHurt() {
        val hurt = EffectLibrary.byId("fight_hero_hurt")!!
        assertTrue(hurt.fragmentEn.contains("staggers back"))
        assertTrue(hurt.fragmentEn.contains("blood on"))
        assertTrue(hurt.fragmentEn.contains("knees buckling"))
        assertTrue(hurt.roleOnly)
        // Detektor gerbang mengenali fragmen ini sebagai hero terluka
        // (dengan nama hero di depan — seperti aksi LLM yang benar).
        assertTrue(ArcLexicon.isHeroHurt("Ariadne ${hurt.fragmentEn}", "Ariadne"))
    }

    @Test
    fun tensionFragmentsHaveNoContact() {
        assertEquals(3, EffectLibrary.TENSION_MOVES.size)
        for (effect in EffectLibrary.TENSION_MOVES) {
            assertTrue(effect.roleOnly)
            val lower = effect.fragmentEn.lowercase()
            for (verb in CONTACT_VERBS) {
                assertFalse("${effect.id} contains $verb", lower.contains(verb))
            }
            assertFalse(effect.fragmentEn, ArcLexicon.isSlowMoMoment(effect.fragmentEn))
        }
    }

    @Test
    fun roleEffectsNeverEnterTheGenericCycle() {
        assertTrue(EffectLibrary.FIGHT_MOVES.none { it.roleOnly })
        assertTrue(EffectLibrary.FIGHT_MOVES_ALL.none { it.roleOnly })
        assertTrue(EffectLibrary.TENSION_MOVES.all { it.roleOnly })
    }

    @Test
    fun fastCameraMovesAreFast() {
        assertTrue(CameraMove.WHIP_PAN.phraseEn.contains("whip pan"))
        assertTrue(CameraMove.WHIP_PAN.phraseEn.contains("fast"))
        assertTrue(CameraMove.PUSH_IN.phraseEn.contains("fast push-in"))
        assertTrue(CameraMove.HANDHELD_SHAKE.phraseEn.contains("shake"))
        for (move in listOf(CameraMove.WHIP_PAN, CameraMove.PUSH_IN, CameraMove.HANDHELD_SHAKE)) {
            assertFalse(move.phraseEn, move.phraseEn.contains("slow"))
        }
        // Efek "Whip-Pan" akhirnya benar-benar whip pan (dulu PAN_LEFT
        // alias "slow pan left" — namanya bohong).
        assertEquals(CameraMove.WHIP_PAN, EffectLibrary.byId("camera_whip")!!.cameraMove)
    }

    @Test
    fun cameraMoveFromStringKnowsFastMoves() {
        assertEquals(CameraMove.WHIP_PAN, ModeRender.cameraMoveFromString("whip pan"))
        assertEquals(CameraMove.WHIP_PAN, ModeRender.cameraMoveFromString("whip pan left to right"))
        assertEquals(CameraMove.PUSH_IN, ModeRender.cameraMoveFromString("fast push-in"))
        assertEquals(CameraMove.HANDHELD_SHAKE, ModeRender.cameraMoveFromString("handheld shake"))
        assertEquals(CameraMove.DOLLY_IN, ModeRender.cameraMoveFromString("slow dolly in"))
        assertEquals(CameraMove.TRACKING, ModeRender.cameraMoveFromString("tracking shot"))
    }

    @Test
    fun rewrittenFightMovesUseFastCameras() {
        assertEquals(CameraMove.WHIP_PAN, EffectLibrary.byId("punch_straight")!!.cameraMove)
        assertEquals(CameraMove.TRACKING, EffectLibrary.byId("punch_uppercut")!!.cameraMove)
        assertEquals(CameraMove.WHIP_PAN, EffectLibrary.byId("kick_roundhouse")!!.cameraMove)
        assertEquals(CameraMove.TRACKING, EffectLibrary.byId("dodge")!!.cameraMove)
        assertEquals(CameraMove.HANDHELD_SHAKE, EffectLibrary.byId("block")!!.cameraMove)
        assertEquals(CameraMove.TRACKING, EffectLibrary.byId("slam")!!.cameraMove)
        // "asphalt" (Yunani kuno) sudah diganti "ground" di fix8.
        assertFalse(EffectLibrary.byId("slam")!!.fragmentEn.contains("asphalt"))
        assertFalse(EffectLibrary.byId("kick_roundhouse")!!.fragmentEn.contains("wet ground"))
    }
}
