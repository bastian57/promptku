package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix4 — audit film cloud "Broly vs Golden Demon: Antarctic Fury"
// (17 Nov) + 3 layer ChatGPT "UES Cinematic" (Reference Assignment,
// Secondary Motion, Facial Performance). Delapan item:
//  1. dunia = kalimat mengalir (bukan tumpukan kata benda)
//  2. keyword ACTION/FIGHT: bentuk kata benda + Inggris ("battle",
//     "pertarungan"...) — "Final Battle" tidak lagi jatuh ke DRAMA
//  3. sanitizer: aktor dipertahankan (pembalikan fix6), "and" awal dibuang
//  4. template a/an ("an extreme close-up")
//  5. narasi wajib tiap scene + gerbang G5 (cloud saja; offline dibebaskan)
//  6. Reference Assignment: peran tiap gambar di daftar referensi
//  7. Secondary Motion + 8. Facial Performance di prompt prompter +
//     dialogue_speaker di rencana + render.
class V2Fix4Test {

    private fun brolyBible(): StoryBible = StoryBible(
        title = "Broly vs Golden Demon",
        logline = "Final battle on a frozen Antarctic ocean.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character("Broly", listOf("red fur over whole body", "silver hair", "glowing seven-star Dragon Ball in chest"))
        ),
        antagonistName = "Golden Demon",
        antagonistDescEn = "a golden-armored demon with massive feathered wings",
        worldName = "Antarctic Abyss",
        worldDescEn = "a frozen Antarctic ocean at night, cracked ice, jagged cliffs, storm clouds, lightning",
        lightEn = "storm light, white lightning glare on wet ice",
        ambienceEn = "howling wind echoing across the frozen expanse",
        beats = listOf(
            V2Beat(0, "Pembuka", "the demon dives from the storm clouds"),
            V2Beat(1, "Klimaks", "the clash detonates"),
            V2Beat(2, "Penutup", "silence returns to the ice")
        ),
        episodes = listOf(V2Episode(1, "Broly vs Golden Demon", "Final battle.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun plan(
        num: Int,
        action: String,
        dialogueEn: String = "",
        dialogueSpeaker: String = "",
        narrationId: String = "Narasi test satu kalimat.",
        shotSize: String = "wide"
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
        lensMm = 35, shotSize = shotSize, angle = "low",
        dialogueId = "", dialogueEn = dialogueEn,
        audioEn = "thunderclap, cracking ice", effectId = "",
        wantEn = "destroy the demon", obstacleEn = "the demon", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = dialogueSpeaker
    )

    private fun shot(p: V2ScenePlan): ModeRender.EffectiveShot =
        ModeRender.effectiveShot(p)

    // --- 2. Keyword genre + fight ---

    @Test
    fun finalBattleInfersActionNotDrama() {
        val genre = StoryInfer.infer(
            "Final Battle antara Broly dan Golden Demon yang sangat dahsyat di atas lautan Antartika"
        ).genre
        assertEquals(Genre.ACTION, genre)
    }

    @Test
    fun pertempuranHebatInfersAction() {
        val genre = StoryInfer.infer(
            "Pertempuran hebat super humanoid di atas lautan Antartika"
        ).genre
        assertEquals(Genre.ACTION, genre)
    }

    @Test
    fun medanPerangStaysSejarah() {
        // Penjaga: "perang" (bare) SENGAJA tidak masuk keyword ACTION —
        // ACTION dicek duluan di RULES dan akan mencuri "medan perang".
        val genre = StoryInfer.infer(
            "Pangeran Diponegoro mengangkat keris di medan perang"
        ).genre
        assertEquals(Genre.SEJARAH, genre)
    }

    @Test
    fun finalBattleDetectsFight() {
        assertTrue(
            EffectLibrary.detect(
                "Final Battle antara Broly dan Golden Demon yang sangat dahsyat"
            ).fight
        )
    }

    // --- 3. Sanitizer: aktor dipertahankan ---

    @Test
    fun sanitizeActionKeepsNamedActor() {
        val bible = brolyBible()
        val clean = ModeRender.sanitizeAction(
            "The Golden Demon spreads its massive golden feathered wings and dives toward Broly",
            bible
        )
        assertTrue(clean, clean.startsWith("The Golden Demon spreads"))
    }

    @Test
    fun sanitizeActionStripsLeadingAndButKeepsActor() {
        val bible = brolyBible()
        val clean = ModeRender.sanitizeAction(
            "and the Golden Demon slam together, generating a shockwave",
            bible
        )
        assertFalse(clean, clean.lowercase().startsWith("and "))
        assertTrue(clean, clean.startsWith("the Golden Demon"))
    }

    // --- 4. a/an ---

    @Test
    fun aOrAnPicksAnForVowels() {
        assertEquals("an", ModeRender.aOrAn("extreme close-up"))
        assertEquals("an", ModeRender.aOrAn("over-the-shoulder shot"))
        assertEquals("a", ModeRender.aOrAn("wide shot"))
        assertEquals("a", ModeRender.aOrAn("medium shot"))
    }

    @Test
    fun flowExtremeCloseUpUsesAn() {
        val bible = brolyBible()
        val p = plan(1, "Broly hurls a plasma wave", shotSize = "extreme close-up")
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(text, text.contains("create an extreme close-up"))
        assertFalse(text, text.contains("create a extreme"))
    }

    // --- 6. Reference Assignment ---

    @Test
    fun referenceListAssignsRoles() {
        val bible = brolyBible()
        val p = plan(1, "Broly hurls a plasma wave")
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(
            text,
            text.contains("Broly (hero), Golden Demon (antagonist), Antarctic Abyss (environment)")
        )
    }

    // --- 3+6. Aktor di render Mode B ---

    @Test
    fun flowKeepsActorAfterPointerComma() {
        val bible = brolyBible()
        val p = plan(
            1,
            "The Golden Demon spreads its massive golden feathered wings and dives toward Broly"
        )
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        // "The" kapital: aksi ditulis LLM sebagai awal kalimat sendiri —
        // render mempertahankannya apa adanya (bukan cacat).
        assertTrue(
            text,
            text.contains("in the reference images, The Golden Demon spreads")
        )
    }

    @Test
    fun klingDoesNotDoubleHeroName() {
        val bible = brolyBible()
        val named = plan(1, "Broly hurls a plasma wave at the Golden Demon")
        val namedText = ModeRender.renderReference(bible, named, shot(named), TargetEngine.KLING, imageCount = 3)
        assertFalse(namedText, namedText.contains("Broly Broly"))
        assertTrue(namedText, namedText.contains("Broly hurls a plasma wave"))
        // Aksi tanpa pelaku tetap ditempel hero (perilaku lama yang benar).
        val bare = plan(1, "hurls a plasma wave at the Golden Demon")
        val bareText = ModeRender.renderReference(bible, bare, shot(bare), TargetEngine.KLING, imageCount = 3)
        assertTrue(bareText, bareText.contains("Broly hurls a plasma wave"))
    }

    @Test
    fun wanStripsHeroNameButKeepsAntagonist() {
        val bible = brolyBible()
        val hero = plan(1, "Broly hurls a plasma wave at the Golden Demon")
        val heroText = ModeRender.renderReference(bible, hero, shot(hero), TargetEngine.WAN, imageCount = 1)
        assertTrue(heroText, heroText.contains("Using the reference image of Broly, hurls a plasma wave"))
        assertFalse(heroText, heroText.contains("Broly, Broly"))
        val demon = plan(1, "The Golden Demon spreads its wings")
        val demonText = ModeRender.renderReference(bible, demon, shot(demon), TargetEngine.WAN, imageCount = 1)
        assertTrue(demonText, demonText.contains("The Golden Demon spreads its wings"))
    }

    // --- 8. Dialogue speaker di render ---

    @Test
    fun dialogueNamesSpeakerWhenKnown() {
        val bible = brolyBible()
        val p = plan(1, "Broly cups both hands", dialogueEn = "My turn!", dialogueSpeaker = "Broly")
        val text = ModeRender.renderReference(bible, p, shot(p), TargetEngine.FLOW, imageCount = 3)
        assertTrue(text, text.contains("Dialogue (Broly): \"My turn!\""))
        val unknown = plan(1, "Broly cups both hands", dialogueEn = "My turn!")
        val unknownText = ModeRender.renderReference(bible, unknown, shot(unknown), TargetEngine.FLOW, imageCount = 3)
        assertTrue(unknownText, unknownText.contains("Dialogue: \"My turn!\""))
        assertFalse(unknownText, unknownText.contains("Dialogue ("))
    }

    // --- 5. Narasi wajib (G5) ---

    private fun scenePrompt(num: Int, narrationId: String): V2ScenePrompt =
        V2ScenePrompt(
            sceneNumber = num,
            plan = plan(num, "Broly hurls a plasma wave", narrationId = narrationId),
            flowText = "x", klingText = "x", seedanceText = "x", wanText = "x"
        )

    @Test
    fun g5FailsOnBlankNarrationForCloud() {
        val bible = brolyBible()
        val g5 = ValidationGates.runGates(bible, listOf(scenePrompt(1, "")), false)
            .first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.any { it.contains("narasi kosong") })
    }

    @Test
    fun g5PassesWhenNarrationFilled() {
        val bible = brolyBible()
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, "Broly mengumpulkan tenaga untuk serangan balasan.")), false
        ).first { it.id == "G5" }
        assertTrue(g5.failures.toString(), g5.passed)
    }

    @Test
    fun g5ExemptsOfflineDraftsFromNarration() {
        val bible = brolyBible()
        val g5 = ValidationGates.runGates(
            bible, listOf(scenePrompt(1, "")), false, requireNarration = false
        ).first { it.id == "G5" }
        assertTrue(g5.failures.toString(), g5.passed)
    }

    // --- JSON round-trip dialogue_speaker ---

    @Test
    fun scenePlanJsonRoundTripsDialogueSpeaker() {
        val original = plan(1, "Broly hurls a plasma wave", dialogueEn = "My turn!", dialogueSpeaker = "Broly")
        val back = V2Models.scenePlansFromJson(
            Json.Arr(listOf(V2Models.scenePlanToJson(original)))
        )
        assertEquals(1, back.size)
        assertEquals("Broly", back[0].dialogueSpeaker)
        assertEquals("My turn!", back[0].dialogueEn)
    }

    // --- Prompt LLM membawa 8 layer fix4 ---

    @Test
    fun prompterPromptCarriesFix4Rules() {
        val prompt = DirectorPrompts.prompterUser(brolyBible(), listOf(1), "Opening: the demon dives")
        assertTrue(prompt, prompt.contains("dialogue_speaker"))
        assertTrue(prompt, prompt.contains("REQUIRED for every scene"))
        assertTrue(prompt, prompt.contains("SECONDARY MOTION"))
        assertTrue(prompt, prompt.contains("FACIAL PERFORMANCE"))
        assertTrue(prompt, prompt.contains("never invent a third character"))
        assertTrue(prompt, prompt.contains("name its actor explicitly"))
    }

    @Test
    fun directorPromptAsksFlowingWorldSentenceAndEscalation() {
        val prompt = DirectorPrompts.directorUser(
            "Final Battle antara Broly dan Golden Demon di atas lautan Antartika",
            3, emptyList(), false
        )
        assertTrue(prompt, prompt.contains("flowing sentence"))
        assertTrue(prompt, prompt.contains("ESCALATE"))
    }
}
