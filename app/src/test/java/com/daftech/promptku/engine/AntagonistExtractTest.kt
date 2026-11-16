package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v1.4.1 — cacat #5 lapangan 16 Nov: isi kalimat hilang.
// "Bejo melawan raksasa 2,8 meter di langit kota yang sudah hancur"
// harus menghasilkan antagonis Raksasa + dunia langit + kota hancur,
// bukan cerita kurir + chip dari pool generik.
class AntagonistExtractTest {

    @Test
    fun bejoSentenceExtractsGiantWithSize() {
        val inference = StoryInfer.infer(
            "Bejo melawan raksasa 2,8 meter di langit kota yang sudah hancur"
        )
        assertEquals(Genre.ACTION, inference.genre)
        assertEquals("Raksasa", inference.antagonist)
        assertTrue(inference.antagonistDescEn.contains("2.8-meter-tall giant"))
        assertTrue(inference.worldDescEn.contains("in the sky"))
        assertTrue(inference.worldDescEn.contains("ruined"))
    }

    @Test
    fun giantWithoutSizeHasNoSizePhrase() {
        val (name, desc) = StoryInfer.extractAntagonist(" raksasa besar menghadang jalan ")
        assertEquals("Raksasa", name)
        assertTrue(desc.startsWith("giant,"))
    }

    @Test
    fun kuntilanakBecomesAntagonist() {
        val inference = StoryInfer.infer(
            "Kuntilanak di kebun pisang menakuti anak yang pulang malam"
        )
        assertEquals("Kuntilanak", inference.antagonist)
        assertTrue(inference.antagonistDescEn.contains("ghost woman"))
    }

    @Test
    fun noAntagonistSentenceStaysEmpty() {
        val inference = StoryInfer.infer(
            "Riko si tukang tambal ban ternyata CEO perusahaan menara itu"
        )
        assertEquals("", inference.antagonist)
        assertEquals("", inference.antagonistDescEn)
    }

    @Test
    fun toScriptInputCarriesAntagonistToSecondCharacter() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang sudah hancur"
        val inference = StoryInfer.infer(sentence)
        val input = StoryInfer.toScriptInput(sentence, "Bejo", 5, inference)
        assertEquals("Raksasa", input.antagonist)
        assertTrue(input.worldDescEn.contains("in the sky"))
        val draft = ScriptEngine.build(input)
        assertEquals(30, draft.scenes.size)
        val shot = ScriptEngine.toShotInput(draft, draft.scenes[0])
        assertEquals("Raksasa", shot.secondCharacter?.name)
        assertTrue(shot.secondCharacter!!.descriptionEn.contains("2.8-meter-tall giant"))
        // Prompt Flow scene pertama menyebut raksasa (bukan cerita kurir).
        val prompt = PromptEngine.build(shot.copy(engine = TargetEngine.FLOW))
        assertTrue(prompt.text.contains("Raksasa"))
    }
}
