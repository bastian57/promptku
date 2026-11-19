package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Resume — lanjutkan film terpotong dari scene yang hilang (desain
// ~/workspace/promptku/resume-design.md, gas 20 Nov 2026).
// Latar: pipeline 5 batch tanpa ingatan — satu batch gagal memotong film,
// tap ulang mengulang dari scene 1 (kuota batch yang sudah jadi terbuang);
// kalau semua provider gagal, user cuma lihat error provider TERAKHIR
// (Groq 429 menutupi alasan Gemini). Dua perbaikan: (A) resumePrompts
// baru — hanya scene yang hilang yang dipanggil ulang; (B) callChain
// mencatat SEMUA percobaan provider di V2Call.Fail.attempts.
class V2ResumeTest {

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

    private fun geminiEnvelope(content: String): String {
        val escaped = JsonLite.escape(content)
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private fun bible(): StoryBible {
        val obj = JsonLite.extractFirstObject(bibleJson())!!
        return V2Models.bibleFromJson(obj)!!
    }

    private fun plansJson(nums: List<Int>): String =
        "[" + nums.joinToString(",") { n ->
            """{"scene_number":$n,"action_en":"Bejo drives a heavy punch into the giant's chest, armor shards flying."}"""
        } + "]"

    private fun plansFor(nums: List<Int>): List<V2ScenePlan> {
        val arr = JsonLite.extractFirstArray(plansJson(nums))!!
        return V2Models.scenePlansFromJson(arr)
    }

    private fun sceneNumbersFromBody(body: String): List<Int> =
        Regex("scene numbers ([0-9, ]+)").find(body)
            ?.groupValues?.get(1)?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?: listOf(1)

    @Test
    fun buildPromptsReturnsPartialPlansWithAllProviderAttempts() {
        val b = bible()
        var calls = 0
        val transport = HttpTransport { _, _, body ->
            calls++
            val nums = sceneNumbersFromBody(body)
            if (nums == listOf(1, 2, 3)) {
                HttpResult(200, geminiEnvelope(plansJson(nums)))
            } else {
                HttpResult(429, """{"error":{"message":"quota"}}""")
            }
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.buildPrompts(b)
        // Batch 1 jadi (3 scene), batch 2 gagal — film terpotong, bukan nol.
        assertEquals(listOf(1, 2, 3), plans.map { it.sceneNumber }.sorted())
        assertTrue(fail is V2Call.Fail)
        val f = fail as V2Call.Fail
        // Rantai 3 provider tercatat semua, urut — bukan cuma Groq terakhir.
        assertEquals(
            listOf("Gemini 3.8 Flash", "Gemini 3.5 Flash-Lite", "Groq gpt-oss-120b"),
            f.attempts.map { it.first }
        )
        assertTrue(f.attempts.all { it.second.isNotBlank() })
        assertTrue(f.fullMessage().contains("Semua provider gagal"))
        assertTrue(f.fullMessage().contains("Gemini 3.8 Flash"))
        assertTrue(f.fullMessage().contains("Groq gpt-oss-120b"))
        // 1 panggilan batch 1 + 3 panggilan rantai batch 2.
        assertEquals(4, calls)
    }

    @Test
    fun resumePromptsOnlyCallsMissingScenes() {
        val b = bible()
        var calls = 0
        val transport = HttpTransport { _, _, body ->
            calls++
            val nums = sceneNumbersFromBody(body)
            if (nums == listOf(1, 2, 3)) {
                HttpResult(200, geminiEnvelope(plansJson(nums)))
            } else {
                HttpResult(429, """{"error":{"message":"quota"}}""")
            }
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (kept, fail) = pipeline.buildPrompts(b)
        assertEquals(3, kept.size)
        assertTrue(fail is V2Call.Fail)

        // Fase resume: provider sehat — hanya scene hilang yang dipanggil.
        val resumeBodies = mutableListOf<String>()
        var resumeCalls = 0
        val resumeTransport = HttpTransport { _, _, body ->
            resumeCalls++
            resumeBodies.add(body)
            HttpResult(200, geminiEnvelope(plansJson(sceneNumbersFromBody(body))))
        }
        val resumePipeline = V2Pipeline(resumeTransport, "AIza_x", "gsk_x")
        val (merged, resumeFail) = resumePipeline.resumePrompts(b, kept)
        assertNull(resumeFail)
        assertEquals((1..6).toList(), merged.map { it.sceneNumber }.sorted())
        assertEquals(1, resumeCalls)
        assertEquals(listOf(4, 5, 6), sceneNumbersFromBody(resumeBodies[0]))
        // previousExit diambil dari scene terakhir yang tersimpan (scene 3).
        assertTrue(resumeBodies[0].contains("Scene 3 ends"))
    }

    @Test
    fun resumePromptsHealsShortBatch() {
        // Batch 1 diam-diam pendek (LLM kembalikan 2 dari 3 plan) — tanpa
        // kegagalan, jadi buildPrompts tidak bersuara soal scene 3.
        val b = bible()
        val transport = HttpTransport { _, _, body ->
            val nums = sceneNumbersFromBody(body)
            if (nums == listOf(1, 2, 3)) {
                HttpResult(200, geminiEnvelope(plansJson(listOf(1, 2))))
            } else {
                HttpResult(200, geminiEnvelope(plansJson(nums)))
            }
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.buildPrompts(b)
        assertNull(fail)
        assertEquals(listOf(1, 2, 4, 5, 6), plans.map { it.sceneNumber }.sorted())

        val resumeBodies = mutableListOf<String>()
        val resumeTransport = HttpTransport { _, _, body ->
            resumeBodies.add(body)
            HttpResult(200, geminiEnvelope(plansJson(sceneNumbersFromBody(body))))
        }
        val resumePipeline = V2Pipeline(resumeTransport, "AIza_x", "gsk_x")
        val (merged, resumeFail) = resumePipeline.resumePrompts(b, plans)
        assertNull(resumeFail)
        assertEquals((1..6).toList(), merged.map { it.sceneNumber }.sorted())
        assertEquals(1, resumeBodies.size)
        assertEquals(listOf(3), sceneNumbersFromBody(resumeBodies[0]))
    }

    @Test
    fun resumePromptsKeepsProgressWhenItFailsAgain() {
        val b = bible()
        val kept = plansFor(listOf(1, 2, 3))
        var calls = 0
        val transport = HttpTransport { _, _, _ ->
            calls++
            HttpResult(429, """{"error":{"message":"quota"}}""")
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.resumePrompts(b, kept)
        // Scene yang sudah jadi tetap ada; kegagalan tidak menghapusnya.
        assertEquals(listOf(1, 2, 3), plans.map { it.sceneNumber }.sorted())
        assertTrue(fail is V2Call.Fail)
        assertEquals(3, (fail as V2Call.Fail).attempts.size)
        // Tepat 3 panggilan = satu rantai provider, tanpa retry tambahan.
        assertEquals(3, calls)
    }

    @Test
    fun resumePromptsReturnsKeptWhenNothingMissing() {
        val b = bible()
        val kept = plansFor((1..6).toList())
        var calls = 0
        val transport = HttpTransport { _, _, _ ->
            calls++
            HttpResult(429, """{"error":{"message":"quota"}}""")
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.resumePrompts(b, kept)
        assertNull(fail)
        assertEquals((1..6).toList(), plans.map { it.sceneNumber }.sorted())
        // Film sudah lengkap — tidak ada panggilan sama sekali.
        assertEquals(0, calls)
    }

    @Test
    fun failFullMessageFallsBackToMessage() {
        assertEquals("x", V2Call.Fail("x", true).fullMessage())
        val f = V2Call.Fail(
            "terakhir", true,
            listOf("Gemini 3.8 Flash" to "kuota habis", "Groq gpt-oss-120b" to "429")
        )
        assertTrue(f.fullMessage().contains("Semua provider gagal"))
        assertTrue(f.fullMessage().contains("- Gemini 3.8 Flash: kuota habis"))
        assertTrue(f.fullMessage().contains("- Groq gpt-oss-120b: 429"))
    }
}
