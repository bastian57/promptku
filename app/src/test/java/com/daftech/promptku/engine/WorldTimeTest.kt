package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v1.4.1 — 7 cacat lapangan Mode Cepat (16 Nov 2026):
// 1. cahaya dari dunia (bukan beat), 2. aksi difilter lawan waktu dunia,
// 3. timeline tanpa lem pose ("begins"/"completes as"),
// 4. BIRD hanya untuk dunia dari atas,
// 7. material default dari dunia + gestur kartu pisah dari subject line.
class WorldTimeTest {

    private val nightCityWorld =
        "a metropolitan city street at night, glass towers, neon signs, wet asphalt"

    private fun nightCityInput(minutes: Int = 5): ScriptInput {
        return ScriptTemplates.action10Min().copy(
            durationMinutes = minutes,
            worldName = "Kota",
            worldDescEn = nightCityWorld,
            antagonist = "",
            antagonistDescEn = ""
        )
    }

    @Test
    fun nightWorldForcesMoonlightAllScenes() {
        val draft = ScriptEngine.build(nightCityInput())
        assertEquals(30, draft.scenes.size)
        assertTrue(draft.scenes.all { it.light == LightKind.MOONLIGHT })
        // Slugline ikut waktu dunia, bukan waktu beat.
        assertTrue(draft.scenes.all { it.slugline.contains("NIGHT") })
    }

    @Test
    fun duskWorldForcesGoldenHourAllScenes() {
        val input = ScriptTemplates.drama3Min().copy(
            durationMinutes = 3,
            worldDescEn = "an old wooden house at dusk, peeling paint, warm lamp light, rain on the roof"
        )
        val draft = ScriptEngine.build(input)
        assertTrue(draft.scenes.all { it.light == LightKind.GOLDEN_HOUR })
    }

    @Test
    fun worldWithoutTimeKeepsBeatArc() {
        val input = ScriptTemplates.action10Min().copy(
            durationMinutes = 5,
            worldDescEn = "a quiet village, green fields, wooden houses, a slow river"
        )
        val draft = ScriptEngine.build(input)
        // Beat 0 (DAY) -> OVERCAST, beat 4 (NIGHT) -> MOONLIGHT: busur tetap.
        assertEquals(LightKind.OVERCAST, draft.scenes.first { it.sequenceIndex == 0 }.light)
        assertEquals(LightKind.MOONLIGHT, draft.scenes.first { it.sequenceIndex == 4 }.light)
    }

    @Test
    fun morningActionFilteredInNightWorld() {
        val draft = ScriptEngine.build(nightCityInput())
        assertFalse(
            draft.scenes.any { it.actionEn.lowercase().contains("morning") }
        )
        // Aksi tetap terisi (tidak ada scene tanpa aksi).
        assertTrue(draft.scenes.all { it.actionEn.isNotBlank() })
    }

    @Test
    fun flowTimelineHasNoPoseGlue() {
        val input = ShotInput(
            title = "Test",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            character = Templates.gokuDna,
            world = WorldLock("Kota", nightCityWorld),
            actionEn = "walks through the market carrying a delivery bag",
            poseStart = "standing still",
            poseEnd = "standing tall",
            materialEn = "",
            audioEn = ""
        )
        val prompt = PromptEngine.build(input)
        assertTrue(prompt.text.contains("[00:00-00:04] walks through the market carrying a delivery bag."))
        assertFalse(prompt.text.contains("begins"))
        assertFalse(prompt.text.contains("completes as"))
        // Rantai pose pindah ke klausa kontinuitas.
        assertTrue(prompt.text.contains("Starts from standing still, ends as standing tall"))
    }

    @Test
    fun seedanceSecondBeatHasNoCompletesAs() {
        val input = ShotInput(
            title = "Test",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.SEEDANCE,
            character = Templates.gokuDna,
            world = WorldLock("Kota", nightCityWorld),
            actionEn = "walks through the market carrying a delivery bag",
            poseStart = "standing still",
            poseEnd = "standing tall",
            materialEn = "",
            audioEn = ""
        )
        val prompt = PromptEngine.build(input)
        assertFalse(prompt.text.contains("completes as"))
        assertTrue(prompt.text.contains("[4-6s] the motion settles, ending as standing tall."))
    }

    @Test
    fun birdAngleOnlyForAboveWorlds() {
        val streetDraft = ScriptEngine.build(nightCityInput())
        assertFalse(streetDraft.scenes.any { it.angle == CameraAngle.BIRD })
        val fieldDraft = ScriptEngine.build(
            ScriptTemplates.action10Min().copy(
                durationMinutes = 5,
                worldName = "Sawah",
                worldDescEn = "a rice field seen from above, green terraces, water channels"
            )
        )
        assertTrue(fieldDraft.scenes.any { it.angle == CameraAngle.BIRD })
    }

    @Test
    fun materialDefaultFollowsWorld() {
        assertTrue(
            PromptEngine.materialDefaultFor(nightCityWorld).contains("mist")
        )
        assertFalse(
            PromptEngine.materialDefaultFor(nightCityWorld).contains("dust")
        )
        assertTrue(
            PromptEngine.materialDefaultFor("a dry desert road, hot sun").contains("sand")
        )
        // Prompt Flow dengan materialEn kosong di dunia malam: hujan/kabut,
        // bukan debu.
        val input = ShotInput(
            title = "Test",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            character = Templates.gokuDna,
            world = WorldLock("Kota", nightCityWorld),
            actionEn = "walks down the street",
            poseStart = "standing still",
            poseEnd = "standing tall",
            materialEn = "",
            audioEn = ""
        )
        val prompt = PromptEngine.build(input)
        assertFalse(prompt.text.contains("dust drifting slowly"))
        assertTrue(prompt.text.contains("mist drifts through the neon light"))
    }

    @Test
    fun cardGestureSeparatedFromSubject() {
        val input = ShotInput(
            title = "Test",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            character = CharacterDna("bejo", "a young man, worn jacket"),
            characterCard = CharacterCard(
                trait = "calm confident look",
                gesture = "fist raised",
                objectProp = "torn jacket",
                faceAnchor = ""
            ),
            world = WorldLock("Kota", nightCityWorld),
            actionEn = "lies on the wet asphalt as boots surround him",
            poseStart = "kneeling on the ground",
            poseEnd = "lying on the wet asphalt",
            materialEn = "",
            audioEn = ""
        )
        val prompt = PromptEngine.build(input)
        // Gestur hidup sebagai klausa identitas terpisah...
        assertTrue(prompt.text.contains("Signature gesture: fist raised."))
        // ...bukan menempel di subject line di samping pose berbaring.
        assertFalse(prompt.text.contains("bejo, calm confident look, fist raised"))
        // Rantai pose tetap ada di klausa kontinuitas.
        assertTrue(
            prompt.text.contains("Starts from kneeling on the ground, ends as lying on the wet asphalt")
        )
    }
}
