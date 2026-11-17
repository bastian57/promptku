package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 + inovasi — cacat render dari audit lapangan Zenith /
// Golden Demon (16 Nov) + perilaku baru (narasi, retry ber-alasan, DNA
// tidak di sini — itu Room/ViewModel, bukan engine murni).
class V2Fix6Test {

    private fun friezaBible(): StoryBible = StoryBible(
        title = "Invasi Zenith",
        logline = "Alien datang ke bumi.",
        genre = Genre.SCI_FI,
        characters = listOf(
            V2Character("Frieza", listOf("large golden feathered wings", "pale white skin", "glowing violet eyes"))
        ),
        // Film satu penjahat: antagonis == hero (kasus lapangan Zenith).
        antagonistName = "Frieza",
        antagonistDescEn = "a cold alien emperor",
        worldName = "Jakarta Metropolis",
        worldDescEn = "A sprawling ruined metropolis, burning towers, ash falling.",
        lightEn = "cold moonlight.",
        ambienceEn = "distant sirens, wind through broken glass.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the alien arrives"),
            V2Beat(1, "Klimaks", "the city burns"),
            V2Beat(2, "Penutup", "silence returns")
        ),
        episodes = listOf(V2Episode(1, "Invasi Zenith", "Alien datang.", 6)),
        durationMinutes = 1
    )

    private fun plan(num: Int, action: String, audio: String = "stone cracking"): V2ScenePlan =
        V2ScenePlan(
            sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
            lensMm = 50, shotSize = "medium", angle = "eye",
            dialogueId = "", dialogueEn = "",
            audioEn = audio, effectId = "",
            wantEn = "survive", obstacleEn = "the alien", transitionEn = ""
        )

    private fun shot(p: V2ScenePlan): ModeRender.EffectiveShot =
        ModeRender.effectiveShot(p)

    @Test
    fun referenceDedupesHeroEqualsAntagonist() {
        val bible = friezaBible()
        val rendered = ModeRender.renderScene(bible, plan(1, "descends slowly"), V2Mode.REFERENCE)
        assertFalse(rendered.flowText, rendered.flowText.contains("Frieza, Frieza"))
        assertFalse(rendered.flowText, rendered.flowText.contains("and, Frieza"))
        assertTrue(rendered.flowText.contains("Frieza in the reference images"))
        assertTrue(rendered.seedanceText.contains("Reference: Image 1 = Frieza."))
    }

    @Test
    fun referenceStripsTrailingPeriodsAndLowercasesWorldArticle() {
        val bible = friezaBible()
        val rendered = ModeRender.renderScene(bible, plan(1, "descends slowly"), V2Mode.REFERENCE)
        assertFalse(rendered.flowText, rendered.flowText.contains(".."))
        // v2.1 fix5 — template "In $world," + "lit with $light" DICABUT:
        // dunia/cahaya kini kalimat, jadi "Setting: ..."/"Lighting: ...".
        assertTrue(rendered.flowText.contains("Setting: a sprawling ruined metropolis"))
        assertTrue(rendered.flowText.contains("Lighting: cold moonlight."))
        assertFalse(rendered.flowText, rendered.flowText.contains("In a sprawling"))
        assertFalse(rendered.flowText, rendered.flowText.contains("lit with cold"))
    }

    @Test
    fun sanitizeActionStripsWorldLeakButKeepsActor() {
        // v2.1 fix4 — PEMBALIKAN kontrak fix6: nama di awal aksi DIPERTAHANKAN.
        // Bukti lapangan (export Broly 18 scene): strip fix6 bikin 13/18 aksi
        // tanpa pelaku ("spreads its wings..." — siapa?). Bug "Frieza, Frieza"
        // yang asli ada di DAFTAR referensi (sudah diperbaiki terpisah), bukan
        // di aksi. Yang masih dibuang: bocor deskripsi dunia + "and" menggantung.
        val bible = friezaBible()
        val leaked = "Frieza channels the storm. A sprawling ruined metropolis, burning towers, ash falling."
        val clean = ModeRender.sanitizeAction(leaked, bible)
        assertFalse(clean, clean.contains("sprawling"))
        assertTrue(clean, clean.startsWith("Frieza channels the storm"))
        val andLeft = ModeRender.sanitizeAction("and the Golden Demon slam together", bible)
        assertFalse(andLeft, andLeft.lowercase().startsWith("and "))
        assertTrue(andLeft, andLeft.contains("Golden Demon"))
    }

    @Test
    fun audioClauseDedupesIdenticalAmbience() {
        val same = ModeRender.audioClause("distant sirens, wind", "distant sirens, wind")
        assertFalse(same, same.contains("Ambient noise"))
        assertTrue(same.startsWith("SFX:"))
        val different = ModeRender.audioClause("stone cracking", "distant sirens, wind")
        assertTrue(different.contains("Ambient noise: distant sirens, wind"))
    }

    @Test
    fun seedanceImageTwoOnlyWhenSecondImageExists() {
        val bible = friezaBible()
        val p = plan(1, "descends slowly")
        val one = ModeRender.renderReference(bible, p, shot(p), TargetEngine.SEEDANCE, imageCount = 1)
        assertFalse(one, one.contains("Image 2"))
        val two = ModeRender.renderReference(bible, p, shot(p), TargetEngine.SEEDANCE, imageCount = 2)
        assertTrue(two.contains("Image 2 = Jakarta Metropolis"))
    }

    @Test
    fun aspectClauseAppendedToAllEngines() {
        val bible = friezaBible()
        val rendered = ModeRender.renderScene(bible, plan(1, "descends slowly"), V2Mode.REFERENCE)
        assertTrue(rendered.flowText.contains("Vertical 9:16 framing."))
        assertTrue(rendered.wanText.contains("Vertical 9:16 framing."))
        val wide = ModeRender.renderScene(
            bible.copy(aspectRatio = "16:9"), plan(1, "descends slowly"), V2Mode.REFERENCE
        )
        assertTrue(wide.flowText.contains("Widescreen 16:9 framing."))
    }

    @Test
    fun g2FlagsNearDuplicateActions() {
        val bible = friezaBible()
        val scenes = listOf(
            V2ScenePrompt(8, plan(8, "hovers above the chaotic street, laughing while the skyline burns beneath him"), "x", "x", "x", "x"),
            V2ScenePrompt(9, plan(9, "hovers above the chaotic street, laughing while the skyline burns beneath him, hovering above the chaotic street"), "x", "x", "x", "x")
        )
        val g2 = ValidationGates.runGates(bible, scenes, false).first { it.id == "G2" }
        assertFalse(g2.passed)
        assertTrue(g2.failures.any { it.contains("nyaris sama") && it.contains("scene 8") })
    }

    @Test
    fun g4FlagsParaphrasedAnchorInAction() {
        val bible = friezaBible()
        val good = V2ScenePrompt(
            1, plan(1, "descends slowly"),
            "Using the provided images for Frieza, create a wide shot of Frieza in the reference images descending slowly. Setting: Jakarta Metropolis.",
            "x", "x", "x"
        )
        val g4good = ValidationGates.runGates(bible, listOf(good), true).first { it.id == "G4" }
        assertTrue(g4good.failures.toString(), g4good.passed)
        val bad = V2ScenePrompt(
            2, plan(2, "spreads his massive feathered wings over the city"),
            "Using the provided images for Frieza, create a wide shot of Frieza in the reference images spreading his massive feathered wings. Setting: Jakarta Metropolis.",
            "x", "x", "x"
        )
        val g4bad = ValidationGates.runGates(bible, listOf(bad), true).first { it.id == "G4" }
        assertFalse(g4bad.passed)
        assertTrue(g4bad.failures.any { it.contains("parafrasa") })
    }

    private fun geminiEnvelope(content: String): String {
        val escaped = JsonLite.escape(content)
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private fun bibleJson(isCourier: Boolean = true): String = """{
        "title": "Invasi Zenith", "logline": "Alien datang.", "genre": "SCI_FI",
        "characters": [{"name": "Frieza", "anchors_en": ["pale white skin"], "look_en": "", "outfit_en": ""}],
        "antagonist_name": "Frieza", "antagonist_desc_en": "a cold alien emperor",
        "world_name": "Jakarta Metropolis", "world_desc_en": "A sprawling ruined metropolis.",
        "light_en": "cold moonlight", "ambience_en": "wind",
        "beats": [{"index": 0, "name": "Pembuka", "summary_en": "a"}],
        "episodes": [{"number": 1, "title": "E1", "logline": "l", "scene_count": 6}],
        "duration_minutes": 1, "is_courier": $isCourier, "source_idea": "Datangnya alien ke bumi"
    }"""

    @Test
    fun pipelineForcesCourierHintFromKeyword() {
        val transport = HttpTransport { _, _, _ ->
            HttpResult(200, geminiEnvelope(bibleJson(isCourier = true)))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        // JSON LLM bilang is_courier=true, hint kata kunci bilang false.
        val (result, fail) = pipeline.developStory("Datangnya alien ke bumi", 1, emptyList(), false)
        assertNull(fail)
        assertEquals(false, result?.isCourier)
    }

    @Test
    fun pipelineCopiesUserSettingsOntoBible() {
        val transport = HttpTransport { _, _, _ ->
            HttpResult(200, geminiEnvelope(bibleJson(isCourier = false)))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (result, fail) = pipeline.developStory(
            "Datangnya alien ke bumi", 1, emptyList(), false,
            aspectRatio = "16:9", dialogueLang = "EN", genreHint = Genre.KOMEDI
        )
        assertNull(fail)
        assertEquals("16:9", result?.aspectRatio)
        assertEquals("EN", result?.dialogueLang)
        assertEquals(Genre.KOMEDI, result?.genre)
        assertEquals(1, result?.episodeNumber)
    }

    @Test
    fun buildPromptBatchSendsRetryReasonsAndSanitizes() {
        val bodies = mutableListOf<String>()
        val transport = HttpTransport { _, _, body ->
            bodies.add(body)
            val out = """{"scene_number": 5, "action_en": "descends over the city. a ruined city at night, broken towers", "camera_move": "static", "lens_mm": 35, "shot_size": "medium", "angle": "eye", "dialogue_id": "", "dialogue_en": "", "audio_en": "", "effect_id": "", "want_en": "", "obstacle_en": "", "transition_en": "", "narration_id": "Kota itu diam, menunggu."}"""
            HttpResult(200, geminiEnvelope("[$out]"))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val bible = friezaBible().copy(worldDescEn = "a ruined city at night, broken towers")
        val (plans, fail) = pipeline.buildPromptBatch(
            bible, listOf(5), "Opening: a",
            retryReasons = mapOf(5 to "aksi duplikat scene 4")
        )
        assertNull(fail)
        assertTrue(bodies.joinToString(" "), bodies.any { it.contains("Scene 5: aksi duplikat scene 4") })
        assertEquals(1, plans.size)
        // Deskripsi dunia yang bocor ke aksi sudah dibersihkan di pipeline.
        assertFalse(plans[0].actionEn, plans[0].actionEn.contains("ruined city"))
        assertEquals("Kota itu diam, menunggu.", plans[0].narrationId)
    }

    @Test
    fun bibleRoundTripsUserSettingsAndPlanParsesNarration() {
        val bible = friezaBible().copy(aspectRatio = "16:9", dialogueLang = "EN", episodeNumber = 2)
        val obj = V2Models.bibleToJson(bible)
        val back = V2Models.bibleFromJson(obj)!!
        assertEquals("16:9", back.aspectRatio)
        assertEquals("EN", back.dialogueLang)
        assertEquals(2, back.episodeNumber)
        val arr = JsonLite.parse(
            """[{"scene_number": 1, "action_en": "descends", "narration_id": "Langit retak."}]"""
        ) as Json.Arr
        val plans = V2Models.scenePlansFromJson(arr)
        assertEquals("Langit retak.", plans[0].narrationId)
    }

    @Test
    fun assignEffectsSeedsUsedFromExistingPlans() {
        // Scene 4 = terakhir (aftermath, efek kosong — benar); scene 3 blank
        // harus dapat jurus yang BELUM dipakai scene 1-2.
        val existing = listOf(
            plan(1, "punches the giant").copy(effectId = "punch"),
            plan(2, "kicks the giant").copy(effectId = "kick"),
            plan(3, "does something new"),
            plan(4, "stands in the silence")
        )
        val assigned = ModeRender.assignEffects(existing, isFight = true, totalBeats = 5)
        assertEquals("punch", assigned[0].effectId)
        assertEquals("kick", assigned[1].effectId)
        assertTrue(assigned[2].effectId.isNotBlank())
        assertFalse(assigned[2].effectId == "punch" || assigned[2].effectId == "kick")
        assertEquals("", assigned[3].effectId)
    }
}
