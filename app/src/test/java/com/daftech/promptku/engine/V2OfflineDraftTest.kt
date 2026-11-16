package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes Draft Offline v2 — cacat lapangan 16 Nov 2026 23:35 WIB: draft
// offline robot stasiun luar angkasa memakai aksi/gaya/atmosfer v1
// yang buta dunia ("walks slowly through the quiet street",
// "neon-lit streets", "dust drifts", ENTRY/EXIT "standing still...
// ending standing firm" 18x, audio kosong).
class V2OfflineDraftTest {

    private val stationWorld =
        "a space station corridor, cold metal walls, blinking lights, Earth visible through a window"
    private val stationAmbience = "low station hum, soft beeps, air circulation"
    private val cityWorld =
        "an old port city at dusk, crumbling warehouses, neon signs, rain-slicked streets"
    private val forestWorld =
        "an ancient forest at dawn, mist between tall trees, mossy stones"

    private val clashTokens = listOf(
        "street", "windowsill", "porch", "motorcycle", "rooftop",
        "city", "garden", "kitchen", "alley", "market"
    )

    private fun stationBible(): StoryBible = StoryBible(
        title = "Robot Kecil",
        logline = "Robot kecil mencari tuannya.",
        genre = Genre.SCI_FI,
        characters = listOf(
            V2Character("Robot", listOf("calm focused eyes", "white flight suit", "small metal frame"))
        ),
        antagonistName = "",
        antagonistDescEn = "",
        worldName = "Stasiun Luar Angkasa",
        worldDescEn = stationWorld,
        lightEn = "cold station light, soft panels.",
        ambienceEn = stationAmbience,
        beats = listOf(
            V2Beat(0, "Pembuka", "the robot wakes"),
            V2Beat(1, "Klimaks", "the robot finds the trail"),
            V2Beat(2, "Penutup", "the robot finds home")
        ),
        episodes = listOf(V2Episode(1, "Robot Kecil", "Robot mencari tuannya.", 3)),
        durationMinutes = 3
    )

    private fun stationPlan(): V2ScenePlan = V2ScenePlan(
        sceneNumber = 1, actionEn = "checks the oxygen gauge and nods",
        cameraMove = "static", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "",
        audioEn = stationAmbience, effectId = "",
        wantEn = "find its owner", obstacleEn = "the empty station", transitionEn = "",
        poseStart = "walking forward", poseEnd = "standing tall"
    )

    // ---------- Pool aksi sadar dunia ----------

    @Test
    fun stationWorldUsesStationActions() {
        val actions = ScriptPools.worldActionsFor(stationWorld, 0)
        assertNotNull(actions)
        assertEquals(8, actions!!.size)
        assertTrue(actions.all { it.split(Regex("\\s+")).size <= 12 })
        assertTrue(actions.none { a -> clashTokens.any { a.lowercase().contains(it) } })
        assertTrue(actions.any { it.contains("corridor") || it.contains("console") })
    }

    @Test
    fun forestWorldUsesForestActions() {
        val actions = ScriptPools.worldActionsFor(forestWorld, 3)
        assertNotNull(actions)
        assertTrue(actions!!.none { it.lowercase().contains("street") })
        assertTrue(actions.any { it.contains("trees") || it.contains("canopy") || it.contains("vine") })
    }

    @Test
    fun cityWorldHasNoDedicatedPool() {
        assertNull(ScriptPools.worldActionsFor(cityWorld, 0))
        assertNull(ScriptPools.worldActionsFor("", 0))
    }

    @Test
    fun frozenOceanIsNotABeach() {
        // Regresi Antartika (audit 16 Nov): "frozen ocean" mengandung
        // kata "ocean" tapi BUKAN pantai — pool laut tidak boleh dipakai.
        assertNull(
            ScriptPools.worldActionsFor(
                "a frozen Antarctic ocean, drifting ice floes, pale cold light", 0
            )
        )
    }

    @Test
    fun actionsForStoryPrefersWorldOverGenre() {
        val input = ScriptInput(
            genre = Genre.SCI_FI, synopsis = "Robot kecil mencari tuannya",
            worldDescEn = stationWorld, durationMinutes = 3, protagonistName = "Robot"
        )
        val actions = ScriptPools.actionsForStory(input, 0)
        assertEquals(ScriptPools.worldActionsFor(stationWorld, 0), actions)
        assertTrue(actions.any { it.contains("corridor") })
    }

    @Test
    fun courierStoryKeepsCourierPoolEvenInStationWorld() {
        val input = ScriptInput(
            genre = Genre.ACTION,
            synopsis = "Raka, kurir muda, menerima paket misterius berisi chip.",
            worldDescEn = stationWorld, durationMinutes = 3, protagonistName = "Raka"
        )
        val actions = ScriptPools.actionsForStory(input, 0)
        assertTrue(actions.any { it.contains("delivery bag") })
    }

    // ---------- Filter bentrok dunia ----------

    @Test
    fun clashFilterDropsStreetActionsForStationWorld() {
        val generic = ScriptPools.actionsFor(Genre.DRAMA, 0)
        assertTrue(generic.any { it.contains("quiet street") })
        val filtered = ScriptPools.worldClashFreeActions(generic, stationWorld)
        assertEquals(8, filtered.size)
        assertEquals(8, filtered.toSet().size)
        assertTrue(filtered.none { a -> clashTokens.any { a.lowercase().contains(it) } })
        assertTrue(filtered.contains("pauses and looks around carefully"))
    }

    @Test
    fun clashFilterKeepsStreetActionsForCityWorld() {
        val generic = ScriptPools.actionsFor(Genre.DRAMA, 0)
        val filtered = ScriptPools.worldClashFreeActions(generic, cityWorld)
        assertEquals(8, filtered.size)
        assertTrue(filtered.any { it.contains("quiet street") })
    }

    @Test
    fun clashFilterSkipsBlankWorld() {
        val generic = ScriptPools.actionsFor(Genre.DRAMA, 0)
        assertEquals(generic, ScriptPools.worldClashFreeActions(generic, ""))
    }

    @Test
    fun timeFilterTopUpIsWorldAware() {
        val morning = listOf(
            "walks down the street in the morning",
            "sits on the porch in the morning"
        )
        val filled = ScriptEngine.usableActionsFor(morning, 0, "NIGHT", stationWorld)
        assertEquals(2, filled.size)
        assertTrue(filled.none { it.lowercase().contains("street") })
        assertTrue(filled.none { it.lowercase().contains("porch") })
    }

    // ---------- Build offline utuh ----------

    @Test
    fun offlineBuildHasNoPlaceClashAndVariedPoses() {
        val input = ScriptInput(
            title = "Robot Kecil",
            synopsis = "Robot kecil mencari tuannya",
            genre = Genre.SCI_FI, durationMinutes = 3,
            protagonistName = "Robot",
            protagonistDescEn = "a small robot, calm focused eyes, white flight suit",
            worldName = "Stasiun Luar Angkasa",
            worldDescEn = stationWorld
        )
        val draft = ScriptEngine.build(input)
        assertEquals(18, draft.scenes.size)
        assertTrue(draft.scenes.all { it.actionEn.split(Regex("\\s+")).size <= 12 })
        assertTrue(draft.scenes.none { s ->
            clashTokens.any { s.actionEn.lowercase().contains(it) }
        })
        assertTrue(draft.scenes.any { it.actionEn.contains("corridor") || it.actionEn.contains("console") })
        // Rantai pose utuh: ENTRY scene = EXIT scene sebelumnya.
        for (i in 1 until draft.scenes.size) {
            assertEquals(draft.scenes[i - 1].poseEnd, draft.scenes[i].poseStart)
        }
        // Pose bervariasi, bukan "standing firm" 18x.
        assertTrue(draft.scenes.map { it.poseEnd }.toSet().size >= 4)
        assertTrue(draft.scenes.any { it.poseEnd != "standing firm" })
    }

    // ---------- Atmosfer + gaya sadar dunia ----------

    @Test
    fun materialFollowsWorld() {
        assertTrue(PromptEngine.materialDefaultFor(stationWorld).contains("hum"))
        assertFalse(PromptEngine.materialDefaultFor(stationWorld).contains("dust"))
        assertTrue(PromptEngine.materialDefaultFor(forestWorld).contains("trees"))
        assertTrue(
            PromptEngine.materialDefaultFor(
                "an ancient stone temple courtyard at sunrise, carved pillars, incense smoke"
            ).contains("incense")
        )
        assertTrue(PromptEngine.materialDefaultFor("a desert at noon").contains("sand"))
    }

    @Test
    fun styleDropsStreetsForStationKeepsForCity() {
        val station = PromptEngine.styleLook(VisualStyle.CYBERPUNK, stationWorld).first
        assertFalse(station.contains("streets"))
        assertTrue(station.contains("cyberpunk film style"))
        assertTrue(station.contains("holographic signs"))
        val city = PromptEngine.styleLook(VisualStyle.CYBERPUNK, cityWorld).first
        assertTrue(city.contains("neon-lit streets"))
        val unfiltered = PromptEngine.styleLook(VisualStyle.CYBERPUNK).first
        assertTrue(unfiltered.contains("neon-lit streets"))
    }

    // ---------- Render: pose + audio dari ambience ----------

    @Test
    fun renderCarriesPosesAndAmbienceAudio() {
        val bible = stationBible()
        val rendered = ModeRender.renderScene(bible, stationPlan(), V2Mode.TEXT_ONLY)
        assertTrue(
            rendered.wanText.contains(
                "walking forward, then checks the oxygen gauge and nods, ending standing tall."
            )
        )
        assertFalse(rendered.wanText.contains("standing still"))
        assertTrue(rendered.wanText.contains("console lights blink steadily"))
        assertTrue(rendered.flowText.contains("Ends as standing tall."))
        assertTrue(rendered.flowText.contains("Audio: $stationAmbience."))
        assertFalse(rendered.flowText.contains("neon-lit streets"))
        assertFalse(rendered.flowText.contains("dust drifts"))
    }

    @Test
    fun referenceDedupesAudioAgainstAmbience() {
        val bible = stationBible()
        val rendered = ModeRender.renderScene(bible, stationPlan(), V2Mode.REFERENCE)
        assertTrue(rendered.flowText.contains("SFX: $stationAmbience."))
        assertFalse(rendered.flowText.contains("Ambient noise:"))
        assertFalse(rendered.flowText.contains("quiet ambience"))
    }

    @Test
    fun blankPosesStillRenderWithDefaults() {
        val bible = stationBible()
        val plain = stationPlan().copy(poseStart = "", poseEnd = "")
        val rendered = ModeRender.renderScene(bible, plain, V2Mode.TEXT_ONLY)
        assertTrue(rendered.wanText.contains("standing still, then"))
        assertTrue(rendered.wanText.contains("ending standing firm."))
    }

    // ---------- JSON rencana scene ----------

    @Test
    fun planJsonRoundTripsPoses() {
        val plan = stationPlan()
        val json = V2Models.scenePlanToJson(plan)
        val parsed = V2Models.scenePlansFromJson(Json.Arr(listOf(json)))
        assertEquals(1, parsed.size)
        assertEquals("walking forward", parsed[0].poseStart)
        assertEquals("standing tall", parsed[0].poseEnd)
        assertEquals(stationAmbience, parsed[0].audioEn)
    }

    @Test
    fun planJsonWithoutPoseKeysParsesWithBlankPoses() {
        val raw = """{"scene_number":1,"action_en":"walks slowly","camera_move":"","lens_mm":35,"shot_size":"","angle":"","dialogue_id":"","dialogue_en":"","audio_en":"","effect_id":"","want_en":"","obstacle_en":"","transition_en":"","narration_id":""}"""
        val obj = JsonLite.parse(raw) as Json.Obj
        val parsed = V2Models.scenePlansFromJson(Json.Arr(listOf(obj)))
        assertEquals(1, parsed.size)
        assertEquals("", parsed[0].poseStart)
        assertEquals("", parsed[0].poseEnd)
    }
}
