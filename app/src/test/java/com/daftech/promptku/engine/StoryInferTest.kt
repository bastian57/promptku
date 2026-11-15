package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes Mode Cepat v1.4: StoryInfer menebak genre/dunia dari 1 kalimat
// (rule-based offline), lokasi menimpa dunia default, dan hasil tebakan
// bisa dibangun jadi naskah flat 1 menit (6 scene).
class StoryInferTest {

    @Test
    fun bejoExampleInfersActionAndCity() {
        val inference = StoryInfer.infer("Bejo melawan raksasa 2,8 meter di langit kota")
        assertEquals(Genre.ACTION, inference.genre)
        assertEquals("Kota", inference.worldName)
        assertEquals(VisualStyle.REALISTIC, inference.style)
        assertTrue(inference.ambienceEn.isNotBlank())
    }

    @Test
    fun rikoExampleInfersDramaAndBengkel() {
        val inference = StoryInfer.infer(
            "Riko si tukang tambal ban ternyata CEO perusahaan menara itu"
        )
        // "tukang" BUKAN kata kunci dokumenter — cerita ini drama.
        assertEquals(Genre.DRAMA, inference.genre)
        assertEquals("Bengkel", inference.worldName)
    }

    @Test
    fun kuntilanakExampleInfersHorrorAndGrove() {
        val inference = StoryInfer.infer(
            "Kuntilanak di kebun pisang menakuti anak yang pulang malam"
        )
        assertEquals(Genre.HOROR, inference.genre)
        assertEquals("Hutan", inference.worldName)
        assertEquals(VisualStyle.NOIR, inference.style)
    }

    @Test
    fun eachGenreKeywordMapsToGenre() {
        val cases = mapOf(
            "Dia jatuh cinta pada gadis di seberang jalan" to Genre.ROMANSA,
            "Tukang ojek itu lucu sekali, penumpangnya ngakak" to Genre.KOMEDI,
            "Robot itu membuka matanya di laboratorium" to Genre.SCI_FI,
            "Naga itu turun dari awan dan menatap gadis kecil" to Genre.FANTASI,
            "Detektif itu mengikuti bayangan di gang hujan" to Genre.THRILLER,
            "Nelayan itu berangkat sebelum subuh" to Genre.DOKUMENTER,
            "Pangeran Diponegoro mengangkat keris di medan perang" to Genre.SEJARAH,
            "Ayah pulang terlambat dan ibu menunggunya di beranda" to Genre.DRAMA
        )
        cases.forEach { (sentence, genre) ->
            assertEquals(sentence, genre, StoryInfer.infer(sentence).genre)
        }
    }

    @Test
    fun locationOverridesGenreWorld() {
        val inference = StoryInfer.infer("Hantu itu muncul di pantai saat senja")
        assertEquals(Genre.HOROR, inference.genre)
        assertEquals("Pantai", inference.worldName)
    }

    @Test
    fun styleAndAmbienceKnownForEveryGenre() {
        Genre.values().forEach { genre ->
            assertTrue(StoryInfer.styleForGenre(genre).toString().isNotBlank())
            assertTrue(StoryInfer.ambienceForGenre(genre).isNotBlank())
        }
    }

    @Test
    fun toScriptInputBuildsFlatSixScenesForOneMinute() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang penuh gedung"
        val inference = StoryInfer.infer(sentence)
        val input = StoryInfer.toScriptInput(sentence, "Bejo", 1, inference)
        assertEquals(Genre.ACTION, input.genre)
        assertEquals("Kota", input.worldName)
        assertEquals(1, input.durationMinutes)
        assertTrue(input.isReady())
        val draft = ScriptEngine.build(input)
        assertEquals(6, draft.scenes.size)
        assertEquals(6, draft.totalClips)
    }

    @Test
    fun ambienceFromQuickModeReachesPrompt() {
        val sentence = "Kuntilanak di kebun pisang menakuti anak yang pulang malam hari"
        val inference = StoryInfer.infer(sentence)
        val input = StoryInfer.toScriptInput(sentence, "Joko", 1, inference)
            .copy(ambienceEn = "crickets and night wind")
        val draft = ScriptEngine.build(input)
        val prompt = PromptEngine.build(
            ScriptEngine.toShotInput(draft, draft.scenes.first())
                .copy(engine = TargetEngine.FLOW)
        )
        assertTrue(prompt.text.contains("crickets and night wind"))
    }

    @Test
    fun examplesAreAllInferrable() {
        StoryInfer.EXAMPLES.forEach { example ->
            val inference = StoryInfer.infer(example)
            assertTrue(example, inference.worldName.isNotBlank())
            assertTrue(example, inference.ambienceEn.isNotBlank())
            assertTrue(example, inference.characterCard.wordCount() > 0)
        }
        assertEquals(
            Genre.ACTION,
            StoryInfer.infer(StoryInfer.EXAMPLES[1]).genre
        )
        assertEquals(
            Genre.HOROR,
            StoryInfer.infer(StoryInfer.EXAMPLES[2]).genre
        )
    }
}
