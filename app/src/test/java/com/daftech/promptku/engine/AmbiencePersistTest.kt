package com.daftech.promptku.engine

import com.daftech.promptku.data.toScriptInput
import com.daftech.promptku.data.toScriptItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v1.4.1 — cacat #6 lapangan 16 Nov: ambience Mode Cepat hilang
// saat naskah dimuat ulang dari DB (ambienceEn hanya in-memory).
// Room v4 -> v5 menyimpan ambienceEn di tabel scripts.
class AmbiencePersistTest {

    @Test
    fun ambienceSurvivesScriptItemRoundTrip() {
        val input = ScriptTemplates.action10Min().copy(
            ambienceEn = "distant traffic, wind, low city hum"
        )
        val item = input.toScriptItem()
        assertEquals("distant traffic, wind, low city hum", item.ambienceEn)
        val restored = item.toScriptInput()
        assertEquals("distant traffic, wind, low city hum", restored.ambienceEn)
    }

    @Test
    fun ambienceFromQuickModeReachesPromptAfterReload() {
        // Simulasi alur lapangan: Mode Cepat -> simpan -> muat ulang
        // dari DB -> generate prompt. Ambience harus tetap ada.
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang sudah hancur"
        val inference = StoryInfer.infer(sentence)
        val input = StoryInfer.toScriptInput(sentence, "Bejo", 5, inference)
        val reloaded = input.toScriptItem().toScriptInput()
        val draft = ScriptEngine.build(reloaded)
        val prompt = PromptEngine.build(
            ScriptEngine.toShotInput(draft, draft.scenes[0]).copy(engine = TargetEngine.FLOW)
        )
        assertTrue(prompt.text.contains("distant traffic, wind, low city hum"))
        assertEquals(false, prompt.text.contains("Audio: wind, quiet ambience"))
    }
}
