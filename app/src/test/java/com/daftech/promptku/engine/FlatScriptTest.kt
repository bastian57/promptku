package com.daftech.promptku.engine

import com.daftech.promptku.data.toDraftScene
import com.daftech.promptku.data.toScriptInput
import com.daftech.promptku.data.toScriptItem
import com.daftech.promptku.data.toScriptSceneItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes invarian model FLAT v1.3 + round-trip Room v4 (kolom baru dialog & kartu).
class FlatScriptTest {

    @Test
    fun sceneNumbersAndTimecodesAreSequential() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        draft.scenes.forEachIndexed { index, scene ->
            assertEquals(index + 1, scene.sceneNumber)
            assertEquals(index, scene.orderIndex)
            val start = index * 10
            val end = start + 10
            val expected = "%02d:%02d-%02d:%02d".format(start / 60, start % 60, end / 60, end % 60)
            assertEquals(expected, scene.timecode())
        }
        assertEquals("00:00-00:10", ScriptEngine.timecodeFor(0))
        assertEquals("04:50-05:00", ScriptEngine.timecodeFor(29))
    }

    @Test
    fun directorNoteMentionsBeatAndTension() {
        val draft = ScriptEngine.build(ScriptTemplates.drama3Min())
        assertTrue(draft.scenes.all { it.directorNote.isNotBlank() })
        assertTrue(draft.scenes.all { it.directorNote.contains("Beat") })
        assertTrue(draft.scenes.all { it.directorNote.contains("ketegangan") })
    }

    @Test
    fun toShotInputCarriesDialogueExactlyOnce() {
        val draft = ScriptEngine.build(ScriptTemplates.action10Min().copy(durationMinutes = 5))
        val dialogScene = draft.scenes.first { it.dialogue != null && !it.dialogue.isEmpty() }
        val prompt = PromptEngine.build(
            ScriptEngine.toShotInput(draft, dialogScene).copy(engine = TargetEngine.FLOW)
        )
        val line = dialogScene.dialogue!!.textEn
        assertTrue(prompt.text.contains(line))
        // Kalimat dialog muncul tepat 1x di prompt (tidak diulang per shot).
        assertEquals(1, prompt.text.split(line).size - 1)
        // Scene tanpa dialog -> input dialog kosong.
        val plainScene = draft.scenes.first { it.dialogue == null || it.dialogue.isEmpty() }
        val plainInput = ScriptEngine.toShotInput(draft, plainScene)
        assertTrue(plainInput.dialogue == null || plainInput.dialogue.isEmpty())
    }

    @Test
    fun scriptItemRoundTripKeepsNewFields() {
        val input = ScriptTemplates.action10Min().copy(
            dialogueLang = DialogueLang.ID,
            characterCard = CharacterCard("calm confident look", "hand to hair", "torn denim jacket", "grease-smudged face"),
            antagonistDescEn = "a wealthy collector in a dark suit, cold eyes, calm voice"
        )
        val item = input.toScriptItem("DRAFT")
        assertEquals("ID", item.dialogueLang)
        assertEquals("calm confident look", item.cardTrait)
        assertEquals("hand to hair", item.cardGesture)
        assertEquals("torn denim jacket", item.cardObject)
        assertEquals("grease-smudged face", item.cardFace)
        assertEquals("a wealthy collector in a dark suit, cold eyes, calm voice", item.antagonistDescEn)
        val back = item.toScriptInput()
        assertEquals(DialogueLang.ID, back.dialogueLang)
        assertEquals("torn denim jacket", back.characterCard?.objectProp)
        assertEquals("a wealthy collector in a dark suit, cold eyes, calm voice", back.antagonistDescEn)
    }

    @Test
    fun scriptSceneItemRoundTripKeepsDialogueAndShot() {
        val draft = ScriptEngine.build(ScriptTemplates.drama3Min())
        val scene = draft.scenes.first { it.dialogue != null && !it.dialogue.isEmpty() }
        val item = scene.toScriptSceneItem(1L)
        assertEquals(scene.sceneNumber, item.sceneNumber)
        assertEquals(scene.dialogue!!.textEn, item.dialogueTextEn)
        assertEquals(scene.dialogue!!.speaker, item.dialogueName)
        assertEquals(scene.shotSize.name, item.shotSize)
        assertEquals(scene.lensMm, item.lensMm)
        assertEquals(scene.reAnchor, item.reAnchor)
        val back = item.toDraftScene()
        assertEquals(scene.actionEn, back.actionEn)
        assertEquals(scene.dialogue?.textEn, back.dialogue?.textEn)
        assertEquals(scene.shotSize, back.shotSize)
        assertEquals(scene.sceneNumber, back.sceneNumber)
        assertEquals(scene.poseStart, back.poseStart)
        assertEquals(scene.poseEnd, back.poseEnd)
    }

    @Test
    fun exportScriptTextHasFlatScenesAndMeter() {
        val draft = ScriptEngine.build(ScriptTemplates.drama3Min())
        val prompts = draft.scenes.map { it to PromptEngine.build(ScriptEngine.toShotInput(draft, it)) }
        val text = FountainExport.buildScriptText(draft, prompts)
        assertTrue(text.contains("v1.3.0"))
        assertTrue(text.contains("SCENE 1 -"))
        assertTrue(text.contains("SCENE 18 -"))
        assertTrue(text.contains("Meter:"))
        assertTrue(text.contains("Kenapa shot ini:"))
    }

    @Test
    fun wanExportHasAudioSheet() {
        val draft = ScriptEngine.build(ScriptTemplates.drama3Min())
        val prompts = draft.scenes.map { it to PromptEngine.build(ScriptEngine.toShotInput(draft, it).copy(engine = TargetEngine.WAN)) }
        val text = FountainExport.buildScriptText(draft, prompts)
        // Setidaknya 1 scene punya dialog -> lembar suara muncul.
        assertTrue(text.contains("LEMBAR SUARA"))
    }
}
