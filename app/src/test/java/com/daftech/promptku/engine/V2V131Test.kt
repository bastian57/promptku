package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v1.3.1 — adapter pensiun Veo 3.1 (gas 20:22 WIB 19 Nov 2026):
// Veo 3.1 Fast/Quality pensiun dari Flow 5 Des 2026; profil FLOW
// retarget ke Omni Flash 1.2 (default), Veo 3.1 Lite jadi fallback
// (maks 8 dtk, 720p) di catatan export. Cap kata TIDAK berubah
// (tidak ada batas resmi Flow; riset 18 Nov) — yang berubah label,
// teks prompter/director, catatan export, dan timestamp Mode A
// 8 dtk -> 10 dtk (batas 8 dtk adalah batas Veo, bukan batas scene;
// scene = 10 dtk per model flat; Mode B sudah 10 dtk).
class V2V131Test {

    // ---------- fixture (disalin dari V2EnginePickerTest) ----------

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

    private fun oneSceneDraft(): ScriptDraft {
        val scene = DraftScene(
            sceneNumber = 1,
            orderIndex = 0,
            sequenceIndex = 0,
            beatName = "Pembuka",
            slugline = "INT. ARENA — SIANG",
            want = "menang",
            obstacle = "musuh",
            actionEn = "Aang lunges forward.",
            dialogue = null,
            shotSize = ShotSize.MEDIUM,
            angle = CameraAngle.EYE,
            lensMm = 35,
            cameraMove = CameraMove.STATIC,
            light = LightKind.OVERCAST,
            poseStart = "standing still",
            poseEnd = "standing tall",
            screenDirection = "",
            axisSide = "",
            transition = "",
            directorNote = "",
            reAnchor = false
        )
        return ScriptDraft(
            input = ScriptInput(title = "Tes", durationMinutes = 1),
            scenes = listOf(scene),
            totalClips = 1,
            estimatedGenerations = 1,
            estimatedCostId = "",
            estimatedHoursId = ""
        )
    }

    // ---------- 1. label + cap ----------

    @Test
    fun flowLabelIsOmni12() {
        assertEquals("Flow / Omni 1.2", TargetEngine.FLOW.labelId)
        assertTrue(TargetEngine.FLOW.labelId.contains("Omni"))
        assertFalse(TargetEngine.FLOW.labelId.contains("Veo"))
    }

    @Test
    fun flowCapsUnchanged() {
        // v1.3.1: cap 300 + min 40 + char cap 2000 TIDAK berubah —
        // tidak ada batas resmi Flow yang ditemukan (riset 18 Nov).
        assertEquals(300, TargetEngine.FLOW.maxWords)
        assertEquals(40, TargetEngine.FLOW.minWords)
        assertEquals(2000, PromptEngine.FLOW_CHAR_CAP)
    }

    @Test
    fun flowProfileUnchanged() {
        val flow = EnginePromptProfile.forEngine(TargetEngine.FLOW)
        assertEquals(50, flow.actionWordsStandard)
        assertEquals(150, flow.actionWordsMax)
        assertTrue(flow.audioRule.isBlank())
    }

    // ---------- 2. teks prompter + director ----------

    @Test
    fun flowRuleTextNamesOmni12() {
        val rule = EnginePromptProfile.forEngine(TargetEngine.FLOW).ruleText()
        assertTrue(rule.contains("Omni Flash 1.2"))
        assertTrue(rule.contains("300 words"))
        assertTrue(rule.contains("50 words"))
        assertTrue(rule.contains("150 words"))
        assertFalse(rule.contains("Veo"))
    }

    @Test
    fun directorUserNamesOmni12() {
        val text = DirectorPrompts.directorUser(
            "Ide tes", 1, emptyList(), false, targetEngine = TargetEngine.FLOW
        )
        assertTrue(text.contains("Flow / Omni 1.2"))
        assertFalse(text.contains("Flow / Veo"))
    }

    @Test
    fun prompterUserNamesOmni12() {
        val text = DirectorPrompts.prompterUser(
            bima18Bible(), listOf(1), "", engine = TargetEngine.FLOW
        )
        assertTrue(text.contains("Omni Flash 1.2"))
        assertTrue(text.contains("300 words"))
    }

    // ---------- 3. timestamp Mode A 10 detik ----------

    @Test
    fun flowRenderUsesTenSecondSegments() {
        val prompt = PromptEngine.build(Templates.gokuVsCell().input)
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.text.contains("[00:05-00:10]"))
        assertFalse(prompt.text.contains("[00:00-00:04]"))
        assertFalse(prompt.text.contains("[00:04-00:08]"))
    }

    @Test
    fun promptCoreSlotsUseTenSecondSegments() {
        val slots = PromptCore.slotsFor(Templates.gokuVsCell().input, TargetEngine.FLOW)
        assertTrue(slots.action.contains("[00:00-00:05]"))
        assertTrue(slots.action.contains("[00:05-00:10]"))
        assertFalse(slots.action.contains("[00:00-00:04]"))
        assertFalse(slots.action.contains("[00:04-00:08]"))
    }

    // ---------- 4. tips export ----------

    @Test
    fun exportTextTipsNameOmni12AndLite() {
        val prompt = PromptEngine.build(Templates.gokuVsCell().input)
        val text = ExportText.buildProjectText("Tes", listOf("Scene 1" to prompt))
        assertTrue(text.contains("Flow (Omni Flash 1.2)"))
        assertTrue(text.contains("10 detik per klip"))
        assertTrue(text.contains("Veo 3.1 Lite"))
        assertTrue(text.contains("8 detik"))
        assertFalse(text.contains("Flow/Veo"))
    }

    @Test
    fun fountainTipsNameOmni12AndLite() {
        val draft = oneSceneDraft()
        val prompt = PromptEngine.build(Templates.gokuVsCell().input)
        val text = FountainExport.buildScriptText(
            draft, listOf(draft.scenes.first() to prompt)
        )
        assertTrue(text.contains("Flow (Omni Flash 1.2)"))
        assertTrue(text.contains("10 detik per klip"))
        assertTrue(text.contains("Veo 3.1 Lite"))
        assertTrue(text.contains("8 detik"))
        assertFalse(text.contains("Flow/Veo"))
    }
}
