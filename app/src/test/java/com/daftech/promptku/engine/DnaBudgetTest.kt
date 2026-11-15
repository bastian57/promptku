package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes anggaran DNA v1.2.1 (bug trim kasus Riko): DNA verbatim panjang
// (129 kata) tidak boleh lagi mengamputasi dunia/aksi/audio dari prompt Flow.
class DnaBudgetTest {

    companion object {
        // DNA Riko, 129 kata — persis dari tes film pertama Putra
        // ("Tukang Tambal Ban itu Ternyata CEO", 15 Nov 2026).
        // Dipakai juga DialogueLineTest (scoreV4 trim).
        const val RIKO_DNA_LONG = "His skin is smooth, clear, and fair with a natural, healthy complexion. " +
            "His eyes are medium-sized, almond-shaped, and dark brown, with a calm, deep, and slightly intense gaze. " +
            "His eyebrows are naturally thick, dark, straight, and gently defined, giving his face a confident and masculine expression. " +
            "He has a straight, well-proportioned nose with a refined bridge and a naturally shaped tip. " +
            "His lips are medium-full, symmetrical, and naturally pink, with a clearly defined upper lip. " +
            "His cheekbones are subtly sculpted, creating a harmonious facial structure without looking overly sharp. " +
            "His hair is thick, naturally black, and voluminous, styled in a sophisticated middle-part hairstyle with soft, slightly wavy layers framing his forehead and temples. " +
            "His overall expression is composed, confident, charismatic, and subtly serious, with an understated sense of elegance."
    }

    private val rikoDna = RIKO_DNA_LONG

    private fun rikoInput(): ShotInput {
        return ShotInput(
            title = "S1 - Shot 1",
            genre = Genre.DRAMA,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            character = CharacterDna("Riko", rikoDna),
            world = WorldLock(
                "Bengkel Tambal Ban",
                "a small roadside tire repair shop at dusk, worn tires stacked against the wall, tools scattered on the ground"
            ),
            shotSize = ShotSize.WIDE,
            angle = CameraAngle.BIRD,
            lensMm = 24,
            cameraMove = CameraMove.STATIC,
            light = LightKind.GOLDEN_HOUR,
            actionEn = "kneels beside a flat tire and loosens the lug nuts",
            poseStart = "standing still",
            poseEnd = "kneeling by the tire",
            materialEn = "dust drifting slowly through the air",
            audioEn = "metal tools clinking, distant traffic",
            focusMode = FocusMode.FULL_STABLE
        )
    }

    @Test
    fun flowKeepsWorldBeatsAndAudioWithLongDna() {
        val prompt = PromptEngine.build(rikoInput())
        assertTrue(prompt.text.contains("tire repair shop"))
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.text.contains("[00:05-00:10]"))
        assertTrue(prompt.text.contains("Audio:"))
        assertTrue(prompt.text.contains("golden-hour"))
        assertTrue(prompt.wordCount <= 200)
        assertTrue(prompt.warnings.any { it.contains("dipangkas") })
        assertTrue(prompt.warnings.any { it.contains("129") })
    }

    @Test
    fun shortDnaIsUntouched() {
        val input = rikoInput().copy(
            character = CharacterDna("Riko", "A young man with a calm confident look.")
        )
        val prompt = PromptEngine.build(input)
        assertFalse(prompt.warnings.any { it.contains("dipangkas") })
        assertTrue(prompt.text.contains("tire repair shop"))
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.text.contains("Audio:"))
    }

    @Test
    fun secondCharacterLongDnaIsAlsoCapped() {
        val input = rikoInput().copy(secondCharacter = CharacterDna("Cell", rikoDna))
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("tire repair shop"))
        assertTrue(prompt.text.contains("[00:00-00:05]"))
        assertTrue(prompt.text.contains("Audio:"))
        assertTrue(prompt.text.contains("Cell"))
        assertTrue(prompt.wordCount <= 200)
        assertTrue(prompt.warnings.any { it.contains("dipangkas") })
    }

    @Test
    fun pathologicalLongWorldStillReportsTrim() {
        val longWorld = (1..25).joinToString(" ") { "very old rusty broken tire" }
        val input = rikoInput().copy(
            world = WorldLock("Bengkel Panjang", longWorld)
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.wordCount <= 200)
        assertTrue(prompt.warnings.any { it.contains("dipangkas") })
    }

    @Test
    fun otherEnginesUnaffectedByLongDna() {
        val input = rikoInput()
        for (engine in listOf(TargetEngine.WAN, TargetEngine.KLING, TargetEngine.SEEDANCE)) {
            val prompt = PromptEngine.build(input.copy(engine = engine))
            assertTrue(prompt.wordCount <= engine.maxWords)
            assertFalse(prompt.warnings.any { it.contains("dipangkas") })
        }
    }
}
