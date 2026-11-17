package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix5 — tindak lanjut audit lapangan "Storm Titans: Clash of
// Legends" (Kaito vs Golden Frieza, 17 Nov) + 2 syarat Putra:
//  1. template dunia/cahaya: "Setting: ..."/"Lighting: ..." (bukan "In
//     $world,"/"lit with $light") — dunia fix4 adalah KALIMAT, "In
//     Lightning splits..." 18x tidak gramatikal (Mode B Flow + Mode A
//     Flow/Wan/Kling).
//  2. G4: stopword ("with") tidak lagi dihitung sebagai token deskriptif.
//  3. Dialog WAJIB bahasa pilihan user: prompter diberi tahu pilihan
//     (bible.dialogueLang — sebelumnya prompter TIDAK PERNAH diberi tahu),
//     gerbang G5 gagal kalau dialog cuma ada di bahasa lain.
//  4. Prompter: ESCALATION per-beat + jurus bernama SEKALI per film +
//     FACIAL PERFORMANCE (mulut terbuka harus TERTULIS di aksi).
//  5. Export (UI, bukan engine — diverifikasi CI + baca kode): narasi
//     terpisah dari blok prompt (hanya NARASI LENGKAP di akhir), dialog
//     yang ditampilkan = bahasa pilihan, reviewReason take(1000).
class V2Fix5Test {

    private fun stormBible(dialogueLang: String = "EN"): StoryBible = StoryBible(
        title = "Storm Titans: Clash of Legends",
        logline = "Kaito battles Golden Frieza in a stormbound wasteland.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Kaito",
                listOf("orange gi with blue sash", "spiky black hair", "glowing orange aura")
            )
        ),
        antagonistName = "Golden Frieza",
        antagonistDescEn = "a golden-armored tyrant with a violet energy field",
        worldName = "Stormbound Wasteland",
        worldDescEn = "Lightning splits the bruised sky over jagged rocks as thunder rolls, " +
            "while ancient stone monoliths crumble under the relentless wind, and the earth " +
            "trembles with each clash of titanic forces.",
        lightEn = "Harsh, flickering lightning casts stark shadows, highlighting the metallic " +
            "sheen of the antagonist and the orange glow of the hero's aura.",
        ambienceEn = "A roaring wind howls through the cracked terrain, punctuated by distant " +
            "rumblings of thunder and the crackle of raw energy.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the tyrant descends from the storm clouds"),
            V2Beat(1, "Eskalasi", "the first exchange of blows"),
            V2Beat(2, "Klimaks", "the biggest clash detonates"),
            V2Beat(3, "Penutup", "silence returns to the wasteland")
        ),
        episodes = listOf(V2Episode(1, "Storm Titans", "Kaito vs Golden Frieza.", 18)),
        durationMinutes = 3,
        dialogueLang = dialogueLang,
        aspectRatio = "9:16"
    )

    private fun plan(
        num: Int,
        action: String,
        dialogueId: String = "",
        dialogueEn: String = "",
        narrationId: String = "Narasi test satu kalimat."
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
        lensMm = 35, shotSize = "wide", angle = "low",
        dialogueId = dialogueId, dialogueEn = dialogueEn,
        audioEn = "thunderclap, cracking stone", effectId = "",
        wantEn = "defeat the tyrant", obstacleEn = "the tyrant", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = ""
    )

    private fun shot(p: V2ScenePlan): ModeRender.EffectiveShot =
        ModeRender.effectiveShot(p)

    private fun scenePrompt(num: Int, p: V2ScenePlan): V2ScenePrompt =
        V2ScenePrompt(
            sceneNumber = num, plan = p,
            flowText = "x", klingText = "x", seedanceText = "x", wanText = "x"
        )

    // --- 1. Template dunia/cahaya (Mode B) ---

    @Test
    fun flowReferenceUsesSettingAndLightingForSentenceWorld() {
        val bible = stormBible()
        val p = plan(1, "Golden Frieza descends rapidly from the dark clouds")
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(text, text.contains("Setting: Lightning splits the bruised sky"))
        assertTrue(text, text.contains("Lighting: Harsh, flickering lightning casts stark shadows"))
        assertFalse(text, text.contains("In Lightning splits"))
        assertFalse(text, text.contains("lit with Harsh"))
    }

    @Test
    fun flowReferenceUsesSettingForPhraseWorldToo() {
        // Dunia frasa lama (pra-fix4) tetap terbaca sebagai deskripsi setting.
        val bible = stormBible().copy(
            worldDescEn = "a frozen Antarctic ocean at night, cracked ice, jagged cliffs",
            lightEn = "storm light, white lightning glare on wet ice"
        )
        val p = plan(1, "Kaito channels a blinding Kamehameha")
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(text, text.contains("Setting: a frozen Antarctic ocean"))
        assertFalse(text, text.contains("In a frozen Antarctic"))
    }

    // --- 1b. Template dunia/cahaya (Mode A) ---

    @Test
    fun modeAFlowUsesSettingNotIn() {
        val bible = stormBible()
        val rendered = ModeRender.renderScene(
            bible, plan(1, "Kaito channels a blinding Kamehameha"), V2Mode.TEXT_ONLY
        )
        assertTrue(rendered.flowText, rendered.flowText.contains("Setting: Lightning splits the bruised sky"))
        assertFalse(rendered.flowText, rendered.flowText.contains(" in Lightning splits"))
    }

    @Test
    fun modeAKlingUsesSettingNotIn() {
        val bible = stormBible()
        val rendered = ModeRender.renderScene(
            bible, plan(1, "Kaito channels a blinding Kamehameha"), V2Mode.TEXT_ONLY
        )
        assertTrue(rendered.klingText, rendered.klingText.contains("Setting: Lightning splits the bruised sky"))
        assertFalse(rendered.klingText, rendered.klingText.contains(", in Lightning"))
    }

    // --- 2. G4 stopword ---

    @Test
    fun g4StopwordWithDoesNotCountAsDescriptiveToken() {
        // Audit Storm Titans S17: flag "(orange, with)" — "with" dari anchor
        // "orange gi with blue sash" dihitung sebagai token deskriptif.
        // Sesudah fix5, aksi yang cuma berbagi {orange} + "with" LOLOS.
        val bible = stormBible()
        val rendered = ModeRender.renderScene(
            bible,
            plan(
                1,
                "Kaito raises his fists in triumph, his hair whipping in the gentle breeze, " +
                    "orange light washing over the battlefield with the storm receding"
            ),
            V2Mode.REFERENCE, imageCount = 3
        )
        val g4 = ValidationGates.runGates(bible, listOf(rendered), true).first { it.id == "G4" }
        assertTrue(g4.failures.toString(), g4.passed)
    }

    @Test
    fun g4RealParaphraseStillFails() {
        // Token deskriptif ASLI yang tumpang tindih >= 2 tetap gagal.
        val bible = stormBible()
        val rendered = ModeRender.renderScene(
            bible,
            plan(
                1,
                "Kaito sweeps his leg in a low spin kick, dust billowing around his blue sash, " +
                    "his orange gi rippling"
            ),
            V2Mode.REFERENCE, imageCount = 3
        )
        val g4 = ValidationGates.runGates(bible, listOf(rendered), true).first { it.id == "G4" }
        assertFalse(g4.passed)
        assertTrue(g4.failures.any { it.contains("parafrasa") })
    }

    // --- 3. Dialog bahasa pilihan (G5) ---

    @Test
    fun g5FailsDialogueOnlyInOtherLanguageEn() {
        val bible = stormBible(dialogueLang = "EN")
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, plan(1, "Kaito cups both hands", dialogueId = "Aku siap"))), false
        ).first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.any { it.contains("bukan bahasa pilihan (EN)") })
    }

    @Test
    fun g5PassesDialogueInChosenLanguageEn() {
        val bible = stormBible(dialogueLang = "EN")
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, plan(1, "Kaito cups both hands", dialogueEn = "I am ready"))), false
        ).first { it.id == "G5" }
        assertTrue(g5.failures.toString(), g5.passed)
    }

    @Test
    fun g5FailsDialogueOnlyInOtherLanguageId() {
        val bible = stormBible(dialogueLang = "ID")
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, plan(1, "Kaito cups both hands", dialogueEn = "I am ready"))), false
        ).first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.any { it.contains("bukan bahasa pilihan (ID)") })
    }

    @Test
    fun g5PassesDialogueInChosenLanguageId() {
        val bible = stormBible(dialogueLang = "ID")
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, plan(1, "Kaito cups both hands", dialogueId = "Aku siap"))), false
        ).first { it.id == "G5" }
        assertTrue(g5.failures.toString(), g5.passed)
    }

    // --- 4. Prompter: bahasa + eskalasi + jurus-sekali + facial ---

    @Test
    fun prompterNamesChosenLanguageEn() {
        val prompt = DirectorPrompts.prompterUser(stormBible(dialogueLang = "EN"), listOf(1), "")
        assertTrue(prompt, prompt.contains("DIALOGUE LANGUAGE"))
        assertTrue(prompt, prompt.contains("dialogue_en is the primary line"))
        assertTrue(prompt, prompt.contains("ESCALATION"))
        assertTrue(prompt, prompt.contains("AT MOST ONE scene"))
        assertTrue(prompt, prompt.contains("does NOT count"))
    }

    @Test
    fun prompterNamesChosenLanguageId() {
        val prompt = DirectorPrompts.prompterUser(stormBible(dialogueLang = "ID"), listOf(1), "")
        assertTrue(prompt, prompt.contains("DIALOGUE LANGUAGE"))
        assertTrue(prompt, prompt.contains("dialogue_id is the primary line"))
        assertTrue(prompt, prompt.contains("ESCALATION"))
    }
}
