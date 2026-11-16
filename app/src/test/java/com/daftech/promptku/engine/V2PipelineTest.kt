package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Pipeline diuji dengan transport palsu — tanpa network, tanpa kunci asli.
class V2PipelineTest {

    private fun bibleJson(): String = """{
        "title": "Bejo vs Raksasa",
        "logline": "Bejo melawan raksasa batu.",
        "genre": "ACTION",
        "characters": [{"name": "Bejo", "anchors_en": ["torn denim jacket", "determined eyes", "black hair"], "look_en": "", "outfit_en": ""}],
        "antagonist_name": "Raksasa",
        "antagonist_desc_en": "a 2.8-meter-tall giant",
        "world_name": "Kota Runtuh",
        "world_desc_en": "a ruined city at night",
        "light_en": "cold moonlight",
        "ambience_en": "wind",
        "beats": [{"index": 0, "name": "Pembuka", "summary_en": "a"}, {"index": 1, "name": "Klimaks", "summary_en": "b"}],
        "episodes": [{"number": 1, "title": "E1", "logline": "l", "scene_count": 6}],
        "duration_minutes": 1,
        "is_courier": false,
        "source_idea": "Bejo melawan raksasa"
    }"""

    private fun groqEnvelope(content: String): String {
        val escaped = JsonLite.escape(content)
        return """{"choices":[{"message":{"content":"$escaped"}}]}"""
    }

    private fun geminiEnvelope(content: String): String {
        val escaped = JsonLite.escape(content)
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private fun bible(): StoryBible {
        val obj = JsonLite.extractFirstObject(bibleJson())!!
        return V2Models.bibleFromJson(obj)!!
    }

    @Test
    fun directorFallsBackFromFlash429ToLite() {
        val calls = mutableListOf<String>()
        val transport = HttpTransport { url, _, _ ->
            calls.add(url)
            if (url.contains("gemini-3.8-flash")) {
                HttpResult(429, """{"error":{"message":"quota"}}""")
            } else {
                HttpResult(200, geminiEnvelope(bibleJson()))
            }
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (result, fail) = pipeline.developStory("Bejo melawan raksasa", 1, emptyList(), false)
        assertNull(fail)
        assertEquals("Bejo vs Raksasa", result?.title)
        assertEquals(2, calls.size)
        assertTrue(calls[0].contains("gemini-3.8-flash"))
        assertTrue(calls[1].contains("gemini-3.5-flash-lite"))
    }

    @Test
    fun directorStopsImmediatelyOn401() {
        var calls = 0
        val transport = HttpTransport { _, _, _ ->
            calls++
            HttpResult(401, """{"error":{"message":"bad key"}}""")
        }
        val pipeline = V2Pipeline(transport, "AIza_bad", "gsk_x")
        val (result, fail) = pipeline.developStory("Bejo melawan raksasa", 1, emptyList(), false)
        assertNull(result)
        assertTrue(fail is V2Call.Fail)
        assertTrue(!(fail as V2Call.Fail).retryable)
        assertEquals(1, calls)
    }

    @Test
    fun buildPromptsBatchesEighteenScenesIntoFiveCalls() {
        val bodies = mutableListOf<String>()
        val transport = HttpTransport { url, _, body ->
            bodies.add(body)
            // Nomor scene yang diminta ada di teks prompt: "scene numbers 1, 2, 3, 4".
            val wanted = Regex("scene numbers ([0-9, ]+)").find(body)?.groupValues?.get(1)
                ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: listOf(1)
            val out = wanted.map { n ->
                """{"scene_number": $n, "action_en": "action $n", "camera_move": "static", "lens_mm": 35, "shotSize": "medium", "angle": "eye", "dialogue_id": "", "dialogue_en": "", "audio_en": "", "effect_id": "", "want_en": "", "obstacle_en": "", "transition_en": ""}"""
            }
            HttpResult(200, geminiEnvelope("[" + out.joinToString(",") + "]"))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val testBible = bible().copy(durationMinutes = 3)
        val (plans, fail) = pipeline.buildPrompts(testBible)
        assertNull(fail)
        assertEquals(18, plans.size)
        assertEquals(5, bodies.size) // 4+4+4+4+2
        assertEquals((1..18).toList(), plans.map { it.sceneNumber })
    }

    @Test
    fun malformedDirectorJsonFailsCleanly() {
        val transport = HttpTransport { _, _, _ ->
            HttpResult(200, geminiEnvelope("bukan json sama sekali"))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (result, fail) = pipeline.developStory("Bejo melawan raksasa", 1, emptyList(), false)
        assertNull(result)
        assertTrue(fail is V2Call.Fail)
    }
}
