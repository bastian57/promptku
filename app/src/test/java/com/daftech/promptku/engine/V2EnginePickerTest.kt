package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.2 — picker engine (paparan 18 Nov 2026, gas 21:47 WIB):
// 1. cap riset detail-vs-panjang (Flow 300, Seedance 250, WanGP 200,
//    Kling tetap 100; FLOW_CHAR_CAP 1300 -> 2000).
// 2. EnginePromptProfile sebagai DATA + aturan per engine di prompter
//    dan baris engine di director.
// 3. StoryBible.targetEngine (JSON round-trip + default Flow untuk
//    bible lama tanpa target_engine).
// 4. lead KLING kenal marker beat "[00:" (lipatan fix13 kandidat 5):
//    aksi yang diawali marker + nama hero tidak ditempel nama dobel.
class V2EnginePickerTest {

    // ---------- fixture (disalin dari V2Fix12Test: fixture tes itu
    // private fun per file, tidak bisa dipakai lintas file) ----------

    private fun bima18Bible(): StoryBible = StoryBible(
        title = "Storm Titans: Clash of Legends",
        logline = "Bima battles Rudra.",
        genre = Genre.ACTION,
        characters = listOf(V2Character("Bima", listOf("golden eye tattoos"))),
        antagonistName = "Rudra",
        antagonistDescEn = "A ruthless immortal titan.",
        worldName = "Bayangan Langit",
        worldDescEn = "Floating volcanic islands drift through purple thunderclouds.",
        lightEn = "Blinding violet lightning.",
        ambienceEn = "Roaring thunder.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Storm Titans", "Bima vs Rudra.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun fieldBible(): StoryBible = StoryBible(
        title = "Final Battle Aang vs Fire Emperor",
        logline = "Aang battles the Fire Emperor.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Aang",
                listOf("Aang", "bald head with a blue arrow tattoo", "mustard-yellow tunic")
            )
        ),
        antagonistName = "Fire Emperor",
        antagonistDescEn = "A ruthless fire tyrant in golden armor.",
        worldName = "Volcanic Wasteland",
        worldDescEn = "Towering black rock pillars jut from the molten earth while rivers of liquid fire carve glowing pathways through the jagged canyons.",
        lightEn = "Fiery orange and crimson glow casting dramatic shadows across the battlefield.",
        ambienceEn = "Roaring flames, crackling embers, and the fierce howling wind.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow")
        ),
        episodes = listOf(V2Episode(1, "Final Battle", "Aang vs Fire Emperor.", 6)),
        durationMinutes = 1,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun microBeatPlan(action: String): V2ScenePlan = V2ScenePlan(
        sceneNumber = 1, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "close-up", angle = "eye",
        dialogueId = "", dialogueEn = "",
        audioEn = "Bone-shattering impact and roaring wind.",
        effectId = "",
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Aang"
    )

    // ---------- 1. cap riset ----------

    @Test
    fun engineCapsFollowResearch() {
        assertEquals(300, TargetEngine.FLOW.maxWords)
        assertEquals(100, TargetEngine.KLING.maxWords)
        assertEquals(250, TargetEngine.SEEDANCE.maxWords)
        assertEquals(200, TargetEngine.WAN.maxWords)
        assertEquals(2000, PromptEngine.FLOW_CHAR_CAP)
    }

    // ---------- 2. profil per engine ----------

    @Test
    fun engineProfilesCarryMeasuredBudgets() {
        val flow = EnginePromptProfile.forEngine(TargetEngine.FLOW)
        assertEquals(50, flow.actionWordsStandard)
        assertEquals(150, flow.actionWordsMax)
        val kling = EnginePromptProfile.forEngine(TargetEngine.KLING)
        assertEquals(20, kling.actionWordsStandard)
        assertEquals(25, kling.actionWordsMax)
        val seedance = EnginePromptProfile.forEngine(TargetEngine.SEEDANCE)
        assertEquals(60, seedance.actionWordsStandard)
        assertEquals(120, seedance.actionWordsMax)
        val wan = EnginePromptProfile.forEngine(TargetEngine.WAN)
        assertEquals(60, wan.actionWordsStandard)
        assertEquals(120, wan.actionWordsMax)
        assertTrue(wan.audioRule.contains("silent video"))
    }

    @Test
    fun engineRuleTextNamesEngineAndBudget() {
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.FLOW).ruleText().contains("Flow"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.FLOW).ruleText().contains("300 words"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.KLING).ruleText().contains("Kling"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.KLING).ruleText().contains("100 words"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.SEEDANCE).ruleText().contains("Seedance"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.SEEDANCE).ruleText().contains("250 words"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.WAN).ruleText().contains("WanGP"))
        assertTrue(EnginePromptProfile.forEngine(TargetEngine.WAN).ruleText().contains("200 words"))
    }

    @Test
    fun prompterUserCarriesEngineRule() {
        val kling = DirectorPrompts.prompterUser(
            bima18Bible(), listOf(1), "", engine = TargetEngine.KLING
        )
        assertTrue(kling.contains("Kling"))
        assertTrue(kling.contains("100 words"))
        val flow = DirectorPrompts.prompterUser(
            bima18Bible(), listOf(1), "", engine = TargetEngine.FLOW
        )
        assertTrue(flow.contains("Flow"))
        assertTrue(flow.contains("300 words"))
        // Default = Flow (pemanggil lama tanpa argumen engine).
        val def = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(def.contains("300 words"))
    }

    @Test
    fun directorUserNamesTargetEngine() {
        val seedance = DirectorPrompts.directorUser(
            "Aang vs Fire Emperor", 1, emptyList(), false,
            targetEngine = TargetEngine.SEEDANCE
        )
        assertTrue(seedance.contains("Seedance"))
        val def = DirectorPrompts.directorUser("Aang vs Fire Emperor", 1, emptyList(), false)
        assertTrue(def.contains("Flow"))
    }

    // ---------- 3. bible.targetEngine ----------

    @Test
    fun bibleTargetEngineRoundTripsThroughJson() {
        val bible = bima18Bible().copy(targetEngine = TargetEngine.SEEDANCE)
        val back = V2Models.bibleFromJson(V2Models.bibleToJson(bible))!!
        assertEquals(TargetEngine.SEEDANCE, back.targetEngine)
    }

    @Test
    fun bibleWithoutTargetEngineDefaultsToFlow() {
        val obj = Json.objOf(
            "title" to Json.strOf("Film Lama"),
            "world_desc_en" to Json.strOf("A quiet valley under grey skies.")
        )
        val back = V2Models.bibleFromJson(obj)!!
        assertEquals(TargetEngine.FLOW, back.targetEngine)
    }

    @Test
    fun bibleDefaultTargetEngineIsFlow() {
        assertEquals(TargetEngine.FLOW, bima18Bible().targetEngine)
    }

    // ---------- 4. lead KLING kenal marker beat ----------

    @Test
    fun klingLeadDoesNotDoubleNameAfterTimecodeMarker() {
        // Aksi Mode B verbatim gaya Clash S6: marker beat + nama hero.
        val action = "[00:00-00:03] Aang punches, shattering a floating stone shard; " +
            "[00:03-00:06] Aang drives a palm thrust; " +
            "[00:06-00:10] Aang lands firm, fists raised"
        val rendered = ModeRender.renderAll(
            fieldBible(), listOf(microBeatPlan(action)), V2Mode.REFERENCE, isFight = false
        )
        val kling = rendered.first().klingText
        assertFalse(kling, kling.contains("Aang [00:00-00:03] Aang"))
        assertTrue(kling, kling.contains("[00:00-00:03] Aang punches"))
    }

    @Test
    fun klingLeadStillStripsPlainLeadingName() {
        // Kontrol: aksi tanpa marker yang diawali nama tetap tidak dobel.
        val action = "Aang punches, shattering a floating stone shard, then lands firm, fists raised"
        val rendered = ModeRender.renderAll(
            fieldBible(), listOf(microBeatPlan(action)), V2Mode.REFERENCE, isFight = false
        )
        val kling = rendered.first().klingText
        assertFalse(kling, kling.contains("Aang Aang punches"))
        assertTrue(kling, kling.contains("Aang punches"))
    }
}
