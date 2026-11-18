package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 (riset) — regresi ".." (fix5, film Bima 18x), format dialog
// resmi 2026 tanpa kutip, dan cap karakter ~1.300 (Mode A + Mode B).
// Akar "..": template menempel "." sesudah teks LLM/user yang sudah
// berakhir "." — diperbaiki DI SUMBER (cleanField di worldText/shortDna/
// budgetedSubjectLine/build() + dialog), bukan per-template.
class V2Fix6RisetTest {

    private fun baseInput(engine: TargetEngine): ShotInput = ShotInput(
        title = "Storm Titans",
        genre = Genre.ACTION,
        style = VisualStyle.REALISTIC,
        engine = engine,
        character = CharacterDna(
            "Bima",
            "a warrior with glowing golden eye tattoos, scarred bronze chest, ancient silver gauntlets"
        ),
        secondCharacter = CharacterDna(
            "Rudra",
            "A ruthless immortal titan clad in black obsidian armor who commands the dark tempest clouds and seeks to plunge the world into eternal night."
        ),
        world = WorldLock(
            "Bayangan Langit",
            "Floating volcanic islands drift through churning purple thunderclouds while ancient stone temples crumble under relentless gale-force winds."
        ),
        shotSize = ShotSize.MEDIUM,
        angle = CameraAngle.LOW,
        lensMm = 35,
        cameraMove = CameraMove.DOLLY_IN,
        light = LightKind.GOLDEN_HOUR,
        actionEn = "Bima hurls a glowing plasma disk across the chasm",
        poseStart = "standing still",
        poseEnd = "standing firm",
        materialEn = "dust drifts through the still air",
        audioEn = "thunder rumble, wind howl"
    )

    // --- (a)(c)(d)(e) Titik di ujung: Mode A ---

    @Test
    fun flowWithPeriodEndingAntagonistHasNoDoublePeriod() {
        // Regresi fix5 (Bima): deskripsi antagonis berakhir "." +
        // template "." = "night.." 18x.
        val text = PromptEngine.build(baseInput(TargetEngine.FLOW)).text
        assertFalse(text, text.contains(".."))
        assertTrue(text, text.contains("eternal night. Setting:"))
    }

    @Test
    fun flowWithPeriodEndingWorldHasNoDoublePeriod() {
        val input = baseInput(TargetEngine.FLOW).copy(
            world = WorldLock(
                "Bayangan Langit",
                "Floating volcanic islands drift through purple thunderclouds."
            )
        )
        val text = PromptEngine.build(input).text
        assertFalse(text, text.contains(".."))
        assertTrue(text, text.contains("Setting: Floating volcanic islands drift through purple thunderclouds."))
    }

    @Test
    fun flowWithPeriodEndingPoseHasSinglePeriod() {
        val input = baseInput(TargetEngine.FLOW).copy(poseEnd = "standing firm.")
        val text = PromptEngine.build(input).text
        assertFalse(text, text.contains(".."))
        assertTrue(text, text.contains("the motion settles, ending as standing firm."))
    }

    @Test
    fun flowWithPeriodEndingActionAudioMaterialHasNoDoublePeriod() {
        val input = baseInput(TargetEngine.FLOW).copy(
            actionEn = "Bima hurls a glowing plasma disk across the chasm.",
            audioEn = "thunder rumble, wind howl.",
            materialEn = "dust drifts through the still air."
        )
        val text = PromptEngine.build(input).text
        assertFalse(text, text.contains(".."))
    }

    @Test
    fun wanKlingSeedanceWithShortPeriodEndingDnaHaveNoDoublePeriod() {
        // shortDna: DNA pendek (<= 12 kata) yang berakhir "." + template
        // "${shortDna}. Setting:" = ".." di ketiga builder pendek.
        val input = baseInput(TargetEngine.WAN).copy(
            character = CharacterDna("Riko", "A young courier in a red jacket.")
        )
        for (engine in listOf(TargetEngine.WAN, TargetEngine.KLING, TargetEngine.SEEDANCE)) {
            val text = PromptEngine.build(input.copy(engine = engine)).text
            assertFalse("$engine: $text", text.contains(".."))
        }
    }

    // --- Format dialog tanpa kutip (resmi 2026) ---

    @Test
    fun modeADialogueHasNoQuotes() {
        val input = baseInput(TargetEngine.FLOW).copy(
            dialogue = DialogueLine(
                speaker = "Bima",
                tone = "a low, steady voice",
                textEn = "My turn!",
                language = DialogueLang.EN
            )
        )
        val text = PromptEngine.build(input).text
        assertTrue(text, text.contains("Bima says, in a low, steady voice: My turn!"))
        assertFalse(text, text.contains("\"My turn!\""))
    }

    @Test
    fun modeADialogueLegacyLineHasNoQuotes() {
        val input = baseInput(TargetEngine.FLOW).copy(
            dialogueName = "Bima",
            dialogueLine = "My turn!"
        )
        val text = PromptEngine.build(input).text
        assertTrue(text, text.contains("Bima says: My turn!"))
        assertFalse(text, text.contains("\"My turn!\""))
    }

    // --- Cap karakter ~1.300 (Mode A) ---

    @Test
    fun flowPromptStaysWithinCharCapIncludingAspect() {
        // v2.2 — cap Flow: kata 180 -> 300, karakter 1.300 -> 2.000.
        // Kata-kata cukup panjang sehingga cap KATA efektif (300 - 3 aspek
        // = 297) lolos (terukur 280 kata) dan cap KARAKTER efektif
        // (2.000 - 22 aspek = 1.978) ditegakkan (terukur 1.970 karakter).
        // Catatan jujur: dengan cap baru, trim kata biasanya mengikat
        // duluan di input yang lebih panjang (kalimat 13-18 kata membuat
        // trim kata mendarat di ~1.900 karakter) — kasus ini menguji
        // batasnya di titik nyaris-cap, bukan jalur trim-karakter murni.
        val longAction = ("Bima hurls a blazing plasma wave across the stormy chasm, " +
            "lightning crackling around his gauntlets as thunder echoes. ").repeat(5)
        val longWorld = ("Floating volcanic islands drift through churning purple " +
            "thunderclouds while ancient stone temples crumble. ").repeat(5)
        val input = baseInput(TargetEngine.FLOW).copy(
            actionEn = longAction.trim(),
            world = WorldLock("Bayangan Langit", longWorld.trim()),
            aspectWords = 3,
            aspectChars = 22,
            dialogue = DialogueLine(
                speaker = "Bima",
                textEn = "My turn!",
                language = DialogueLang.EN
            )
        )
        val prompt = PromptEngine.build(input)
        assertTrue("length=${prompt.text.length}", prompt.text.length <= 1978)
        assertTrue(prompt.text, prompt.text.contains("Bima says: My turn!"))
        assertTrue(prompt.text, prompt.text.contains("plasma"))
        assertFalse(prompt.text, prompt.text.contains(".."))
        // Input yang melampaui kedua cap tetap kembali di bawah batas karakter.
        val overInput = input.copy(
            actionEn = longAction.repeat(2).trim(),
            world = WorldLock("Bayangan Langit", longWorld.repeat(2).trim())
        )
        val overPrompt = PromptEngine.build(overInput)
        assertTrue("length=${overPrompt.text.length}", overPrompt.text.length <= 1978)
        assertTrue(overPrompt.text, overPrompt.text.contains("Bima says: My turn!"))
        assertTrue(overPrompt.text, overPrompt.text.contains("plasma"))
    }

    // --- Mode B: dialog + fit-trim ---

    private fun bible(): StoryBible = StoryBible(
        title = "Storm Titans",
        logline = "Kaito battles Golden Frieza.",
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
        worldDescEn = "Lightning splits the bruised sky over jagged rocks as thunder rolls.",
        lightEn = "Harsh, flickering lightning.",
        ambienceEn = "A roaring wind howls.",
        beats = listOf(
            V2Beat(0, "Pembuka", ""),
            V2Beat(1, "Pemicu", ""),
            V2Beat(2, "Eskalasi", ""),
            V2Beat(3, "Klimaks", ""),
            V2Beat(4, "Penutup", "")
        ),
        episodes = listOf(V2Episode(1, "Storm Titans", "Kaito vs Golden Frieza.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun plan(
        num: Int,
        action: String,
        dialogueEn: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
        lensMm = 35, shotSize = "wide", angle = "low",
        dialogueId = "", dialogueEn = dialogueEn,
        audioEn = "thunder, wind", effectId = "",
        wantEn = "win", obstacleEn = "the tyrant", transitionEn = "",
        narrationId = "Narasi test.", dialogueSpeaker = "Kaito"
    )

    @Test
    fun modeBDialogueHasNoQuotesAndNoDoublePeriod() {
        val p = plan(1, "Kaito cups both hands", dialogueEn = "My turn!.")
        val text = ModeRender.renderReference(bible(), p, ModeRender.effectiveShot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(text, text.contains("Kaito says: My turn!"))
        assertFalse(text, text.contains(".."))
        assertFalse(text, text.contains("Dialogue ("))
    }

    @Test
    fun modeBFitTrimKeepsWorldAndDialogue() {
        // Mode B dulu TIDAK PUNYA trim (S17 Kaito 1.453 karakter / 211 kata
        // lolos). v2.2 — aksi 300 kata (repeat 20) dipotong ekornya ke cap
        // Flow 300 kata / 2.000 karakter; dunia + dialog utuh.
        val action = ("Kaito hurls a blazing plasma wave at Golden Frieza, the energy " +
            "crackling through the storm. ").repeat(20).trim()
        val p = plan(1, action, dialogueEn = "My turn!")
        val text = ModeRender.renderReference(bible(), p, ModeRender.effectiveShot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue("words=${MythFilter.countWords(text)}", MythFilter.countWords(text) <= 300)
        assertTrue("chars=${text.length}", text.length <= 2000)
        assertTrue(text, text.contains("Lightning splits the bruised sky"))
        assertTrue(text, text.contains("Kaito says: My turn!"))
        assertTrue(text, text.contains("Kaito hurls a blazing plasma wave"))
    }
}
