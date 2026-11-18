package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes DialogueLine ID/EN + render per engine + Prompt Score v4.
class DialogueLineTest {

    @Test
    fun textForFallsBackToOtherLanguage() {
        val line = DialogueLine(
            speaker = "Riko",
            textId = "Saya tidak mengerti.",
            textEn = "I don't understand.",
            language = DialogueLang.ID
        )
        assertEquals("Saya tidak mengerti.", line.textFor(DialogueLang.ID))
        assertEquals("I don't understand.", line.textFor(DialogueLang.EN))
        val enOnly = DialogueLine(textEn = "Wait for me.", language = DialogueLang.ID)
        assertEquals("Wait for me.", enOnly.textFor(DialogueLang.ID))
        val idOnly = DialogueLine(textId = "Tunggu aku.", language = DialogueLang.EN)
        assertEquals("Tunggu aku.", idOnly.textFor(DialogueLang.EN))
    }

    @Test
    fun activeTextFollowsLanguage() {
        val line = DialogueLine(textId = "Tunggu aku.", textEn = "Wait for me.", language = DialogueLang.EN)
        assertEquals("Wait for me.", line.activeText())
        assertEquals("Tunggu aku.", line.copy(language = DialogueLang.ID).activeText())
    }

    @Test
    fun flowRendersToneBeforeLine() {
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            dialogue = DialogueLine(
                speaker = "Riko",
                tone = "a low, steady voice",
                textEn = "I don't understand.",
                language = DialogueLang.EN
            )
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("Riko says, in a low, steady voice: I don't understand."))
    }

    @Test
    fun flowRendersLegacyDialogueWithoutTone() {
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            dialogueName = "Riko",
            dialogueLine = "I don't understand."
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("Riko says: I don't understand."))
    }

    @Test
    fun seedanceIdRendersLanguagePrefix() {
        val input = ShotInput(
            engine = TargetEngine.SEEDANCE,
            dialogue = DialogueLine(
                speaker = "Riko",
                tone = "a calm voice",
                textId = "Saya tidak mengerti.",
                textEn = "I don't understand.",
                language = DialogueLang.ID
            )
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("Language: Indonesian."))
        assertTrue(prompt.text.contains("Saya tidak mengerti."))
    }

    @Test
    fun klingRendersParenTone() {
        val input = ShotInput(
            engine = TargetEngine.KLING,
            dialogue = DialogueLine(
                speaker = "Riko",
                tone = "an urgent voice",
                textEn = "Wait for me.",
                language = DialogueLang.EN
            )
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("Riko (an urgent voice): Wait for me."))
    }

    @Test
    fun wanDropsDialogueLineObjectWithWarning() {
        val input = ShotInput(
            engine = TargetEngine.WAN,
            dialogue = DialogueLine(speaker = "Riko", textEn = "Hello.")
        )
        val prompt = PromptEngine.build(input)
        assertFalse(prompt.text.contains("Hello"))
        assertTrue(prompt.warnings.any { it.contains("tanpa audio") })
    }

    @Test
    fun scoreV4CatchesDialogueTooFast() {
        val longText = (1..30).joinToString(" ") { "word" }
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            actionEn = "stands still",
            dialogue = DialogueLine(speaker = "Riko", textEn = longText)
        )
        val result = PromptScore.scoreV4(input)
        assertFalse(result.items.first { it.id == "dialogue_timing" }.passed)
    }

    @Test
    fun scoreV4FlagsWanDialogueAsSilent() {
        val input = ShotInput(
            engine = TargetEngine.WAN,
            actionEn = "stands still",
            dialogue = DialogueLine(speaker = "Riko", textEn = "Hello.")
        )
        val result = PromptScore.scoreV4(input)
        assertFalse(result.items.first { it.id == "dialogue_language" }.passed)
    }

    @Test
    fun scoreV4FlagsTrimmedPrompt() {
        // DNA panjang tanpa kartu = prompt terpotong -> trim_report + slot_budget gagal.
        // v2.2 — DNA 129 kata kini muat utuh di bawah cap Flow 300 (terukur
        // 224 kata, tanpa trim). Jalur flag tetap diuji dengan DNA digandakan
        // (258 kata -> dipangkas ke 204 kata, kedua item gagal).
        val dna = DnaBudgetTest.RIKO_DNA_LONG + " " + DnaBudgetTest.RIKO_DNA_LONG
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            character = CharacterDna("Riko", dna),
            world = WorldLock("Bengkel", "a small roadside tire repair shop at dusk"),
            actionEn = "kneels and begins fixing the tire"
        )
        val prompt = PromptEngine.build(input)
        val result = PromptScore.scoreV4(input, prompt)
        assertFalse(result.items.first { it.id == "trim_report" }.passed)
        assertFalse(result.items.first { it.id == "slot_budget" }.passed)
    }

    @Test
    fun scoreV4PassesCleanCardPrompt() {
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            character = CharacterDna("Riko", DnaBudgetTest.RIKO_DNA_LONG),
            characterCard = CharacterCard(
                trait = "calm confident look",
                gesture = "hand to hair",
                objectProp = "torn denim jacket",
                faceAnchor = "grease-smudged face"
            ),
            world = WorldLock("Bengkel", "a small roadside tire repair shop at dusk"),
            actionEn = "kneels and begins fixing the tire",
            dialogue = DialogueLine(
                speaker = "Riko",
                tone = "a low, steady voice",
                textEn = "I don't understand.",
                language = DialogueLang.EN
            )
        )
        val prompt = PromptEngine.build(input)
        val result = PromptScore.scoreV4(input, prompt)
        assertEquals(5, result.score)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun scoreV4FlagsTwoSpeakersInOneClip() {
        // Dua pengucap tertulis dalam satu dialog legacy = gagal speaker_rule.
        val input = ShotInput(
            engine = TargetEngine.FLOW,
            actionEn = "stands still",
            dialogueName = "Riko",
            dialogueLine = "Pak Darma says: \"Listen.\" Then Riko says: \"No.\""
        )
        val result = PromptScore.scoreV4(input, clipSeconds = 8f)
        assertFalse(result.items.first { it.id == "speaker_rule" }.passed)
    }
}
