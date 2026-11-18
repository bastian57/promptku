package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v2.2-fix2 — batch prompter kosong diulang 1x + pesan gagal membawa
// diagnosa (provider + cuplikan balasan mentah).
// Latar: film Gotres 18 Nov malam — batch scene 1 kosong, tap ulang ->
// scene 1 jadi, batch scene 2 kosong lagi, film terpotong 1/6. "Scene
// kosong" = provider mengembalikan HTTP 200 + teks, tapi teks itu tidak
// mengandung array plan yang bisa di-parse (prosa / JSON terpotong /
// item tanpa scene_number/action_en). Dua cacat: (1) buildPrompts tidak
// pernah mengulang batch yang gagal; (2) teks mentah LLM dibuang.
class V2Fix2Test {

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

    private fun batchCount(b: StoryBible): Int {
        val beatMap = BeatMap.build(b.totalScenes, b.beats)
        return beatMap.flatMap { beat ->
            (beat.firstScene..beat.lastScene).toList().chunked(5)
        }.size
    }

    private fun sceneNumbersFromBody(body: String): List<Int> =
        Regex("scene numbers ([0-9, ]+)").find(body)
            ?.groupValues?.get(1)?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?: listOf(1)

    @Test
    fun emptyBatchIsRetriedOnceAndSucceeds() {
        val b = bible()
        val bodies = mutableListOf<String>()
        var calls = 0
        val transport = HttpTransport { _, _, body ->
            calls++
            bodies.add(body)
            if (calls == 1) {
                // Percobaan pertama: LLM mengembalikan prosa, tanpa array.
                HttpResult(200, geminiEnvelope("Maaf, saya tidak bisa menulis JSON."))
            } else {
                HttpResult(200, geminiEnvelope(plansJson(sceneNumbersFromBody(body))))
            }
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.buildPrompts(b)
        assertNull(fail)
        assertEquals(6, plans.size)
        assertEquals((1..6).toList(), plans.map { it.sceneNumber }.sorted())
        // Satu batch gagal -> 1 panggilan ekstra (retry), bukan film terpotong.
        assertEquals(batchCount(b) + 1, calls)
        // Retry membawa nudge perbaikan "hanya array JSON".
        assertTrue(bodies[1].contains("Return ONLY the JSON array"))
    }

    @Test
    fun emptyBatchFailsAfterRetryWithDiagnostic() {
        val b = bible()
        var calls = 0
        val transport = HttpTransport { _, _, _ ->
            calls++
            HttpResult(200, geminiEnvelope("Maaf, saya tidak bisa menulis JSON."))
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.buildPrompts(b)
        assertTrue(plans.isEmpty())
        assertTrue(fail is V2Call.Fail)
        val msg = (fail as V2Call.Fail).message
        assertTrue(msg.contains("scene kosong"))
        // Diagnosa: provider + cuplikan balasan mentah ikut di pesan.
        assertTrue(msg.contains("Gemini 3.8 Flash"))
        assertTrue(msg.contains("Maaf, saya tidak bisa"))
        // Tepat 2 panggilan: percobaan + 1 retry, lalu berhenti.
        assertEquals(2, calls)
    }

    @Test
    fun httpFailureIsNotRetried() {
        // Gagal HTTP (429) sudah melewati rantai 3 provider — mengulang
        // batch yang sama hanya mengulang 429 yang sama. Retry fix2
        // HANYA untuk kasus "scene kosong" (Ok tapi tak bisa di-parse).
        var calls = 0
        val transport = HttpTransport { _, _, _ ->
            calls++
            HttpResult(429, """{"error":{"message":"quota"}}""")
        }
        val pipeline = V2Pipeline(transport, "AIza_x", "gsk_x")
        val (plans, fail) = pipeline.buildPrompts(bible())
        assertTrue(plans.isEmpty())
        assertTrue(fail is V2Call.Fail)
        assertTrue(!(fail as V2Call.Fail).message.contains("scene kosong"))
        // Tepat 3 panggilan = satu rantai provider, tanpa retry tambahan.
        assertEquals(3, calls)
    }
}
