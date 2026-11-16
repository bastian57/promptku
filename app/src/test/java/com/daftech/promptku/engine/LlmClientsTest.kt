package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientsTest {

    private fun transportFor(code: Int, body: String): HttpTransport =
        HttpTransport { _, _, _ -> HttpResult(code, body) }

    private val groqOkBody = """{"choices":[{"message":{"content":"{\"title\":\"T\"}"}}]}"""
    private val geminiOkBody =
        """{"candidates":[{"content":{"parts":[{"text":"{\"title\":\"T\"}"}]}}]}"""

    @Test
    fun groqSuccessReturnsContent() {
        val result = LlmClients.chatGroq(
            transportFor(200, groqOkBody), "gsk_test", "openai/gpt-oss-120b",
            listOf(ChatMsg("user", "hi"))
        )
        assertTrue(result is V2Call.Ok)
        assertEquals("{\"title\":\"T\"}", (result as V2Call.Ok).text)
    }

    @Test
    fun groqMissingKeyFailsWithoutNetwork() {
        var called = false
        val transport = HttpTransport { _, _, _ ->
            called = true
            HttpResult(200, groqOkBody)
        }
        val result = LlmClients.chatGroq(
            transport, "", "openai/gpt-oss-120b", listOf(ChatMsg("user", "hi"))
        )
        assertTrue(result is V2Call.Fail)
        assertFalse(called)
    }

    @Test
    fun groq429IsRetryable() {
        val result = LlmClients.chatGroq(
            transportFor(429, """{"error":{"message":"rate limit"}}"""),
            "gsk_test", "openai/gpt-oss-120b", listOf(ChatMsg("user", "hi"))
        )
        assertTrue(result is V2Call.Fail)
        assertTrue((result as V2Call.Fail).retryable)
        assertTrue(result.message.contains("429"))
    }

    @Test
    fun groq401IsNotRetryable() {
        val result = LlmClients.chatGroq(
            transportFor(401, """{"error":{"message":"invalid key"}}"""),
            "gsk_bad", "openai/gpt-oss-120b", listOf(ChatMsg("user", "hi"))
        )
        assertTrue(result is V2Call.Fail)
        assertTrue(!(result as V2Call.Fail).retryable)
    }

    @Test
    fun geminiSuccessReturnsJoinedText() {
        val result = LlmClients.generateGemini(
            transportFor(200, geminiOkBody), "AIza_test", "gemini-3.8-flash", "hi"
        )
        assertTrue(result is V2Call.Ok)
        assertEquals("{\"title\":\"T\"}", (result as V2Call.Ok).text)
        assertEquals(V2Provider.GEMINI_FLASH, result.provider)
    }

    @Test
    fun gemini404IsNotRetryable() {
        val result = LlmClients.generateGemini(
            transportFor(404, """{"error":{"message":"model not found"}}"""),
            "AIza_test", "gemini-2.5-flash", "hi"
        )
        assertTrue(result is V2Call.Fail)
        assertTrue(!(result as V2Call.Fail).retryable)
    }

    @Test
    fun stripCodeFencesRemovesMarkdown() {
        assertEquals(
            "{\"a\":1}",
            LlmClients.stripCodeFences("```json\n{\"a\":1}\n```")
        )
        assertEquals("{\"a\":1}", LlmClients.stripCodeFences("{\"a\":1}"))
    }

    @Test
    fun visionModelMapsToVisionProvider() {
        val result = LlmClients.chatGroq(
            transportFor(200, groqOkBody), "gsk_test", "qwen/qwen3.8-27b",
            listOf(ChatMsg("user", "describe", listOf("aGVsbG8=")))
        )
        assertTrue(result is V2Call.Ok)
        assertEquals(V2Provider.GROQ_VISION, (result as V2Call.Ok).provider)
    }

    // v2.0 fix3 — model Director dari tab Kunci WAJIB dipakai, bukan
    // konstanta hardcode (sebelumnya field model di layar Kunci mati).
    @Test
    fun callProviderUsesCustomGeminiModel() {
        var seenUrl = ""
        val transport = HttpTransport { url, _, _ ->
            seenUrl = url
            HttpResult(200, geminiOkBody)
        }
        val result = LlmClients.callProvider(
            transport, V2Provider.GEMINI_FLASH, "AIza_test", "gsk_test", "hi",
            geminiModel = "gemini-2.0-flash"
        )
        assertTrue(result is V2Call.Ok)
        assertTrue(seenUrl.contains("gemini-2.0-flash"))
        assertEquals(V2Provider.GEMINI_FLASH, (result as V2Call.Ok).provider)
    }

    @Test
    fun callProviderUsesCustomGroqModel() {
        var seenBody = ""
        val transport = HttpTransport { _, _, body ->
            seenBody = body
            HttpResult(200, groqOkBody)
        }
        val result = LlmClients.callProvider(
            transport, V2Provider.GROQ_GPT_OSS, "AIza_test", "gsk_test", "hi",
            groqModel = "llama-3.3-70b-versatile"
        )
        assertTrue(result is V2Call.Ok)
        assertTrue(seenBody.contains("llama-3.3-70b-versatile"))
    }
}
