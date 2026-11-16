package com.daftech.promptku.engine

// Klien LLM v2.0 — Kotlin murni, transport disuntik (lihat
// data/LlmTransportOkHttp.kt untuk implementasi Android).
// Kunci API TIDAK PERNAH di-log, TIDAK PERNAH masuk pesan error.
//
// Pola yang dipakai (terbukti di UnduhKu v1.1.0):
//   Groq   = POST https://api.groq.com/openai/v1/chat/completions,
//            header Authorization: Bearer <key>, JSON mode.
//   Gemini = POST https://generativelanguage.googleapis.com/v1beta/
//            models/<model>:generateContent, header x-goog-api-key.
// Keduanya pakai User-Agent browser di transport (pelajaran Nalar:
// request tanpa UA browser kena Cloudflare 1010 di Groq).

data class HttpResult(val code: Int, val body: String, val retryAfterSeconds: Int = 0)

fun interface HttpTransport {
    fun post(url: String, headers: Map<String, String>, body: String): HttpResult
}

data class ChatMsg(val role: String, val text: String, val imagesBase64: List<String> = emptyList())

object LlmClients {

    private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    const val MODEL_VISION_GROQ = "qwen/qwen3.8-27b"
    const val MODEL_DIRECTOR_GEMINI = "gemini-3.8-flash"
    const val MODEL_DIRECTOR_GEMINI_LITE = "gemini-3.5-flash-lite"
    const val MODEL_DIRECTOR_GROQ = "openai/gpt-oss-120b"

    /** Hapus ```json fence kalau LLM membungkus JSON dengan markdown. */
    fun stripCodeFences(text: String): String {
        val t = text.trim()
        if (!t.startsWith("```")) return t
        val firstNl = t.indexOf('\n')
        val body = if (firstNl >= 0) t.substring(firstNl + 1) else t
        return body.trim().removeSuffix("```").trim()
    }

    private fun mapHttpError(code: Int, raw: String, provider: String): V2Call.Fail {
        val detail = runCatching {
            (JsonLite.parse(raw) as? Json.Obj)?.let { obj ->
                obj.obj("error")?.str("message")
                    ?: obj.str("message")
                    ?: obj.arr("candidates")?.let { null }
            }
        }.getOrNull()
        val short = detail?.take(160) ?: ""
        return when (code) {
            401, 403 -> V2Call.Fail(
                "Kunci $provider ditolak (401/403) — cek kunci di tab Kunci.${
                    if (short.isNotBlank()) " $short" else ""
                }", retryable = false
            )
            404 -> V2Call.Fail(
                "Model tidak ditemukan di $provider (404) — model mungkin sudah pensiun, ganti model di tab Kunci.${
                    if (short.isNotBlank()) " $short" else ""
                }", retryable = false
            )
            429 -> V2Call.Fail(
                "Kuota $provider habis (429) — coba provider lain atau besok.${
                    if (short.isNotBlank()) " $short" else ""
                }", retryable = true
            )
            in 500..599 -> V2Call.Fail(
                "Server $provider error ($code) — coba lagi.${
                    if (short.isNotBlank()) " $short" else ""
                }", retryable = true
            )
            else -> V2Call.Fail(
                "$provider mengembalikan HTTP $code.${
                    if (short.isNotBlank()) " $short" else ""
                }", retryable = false
            )
        }
    }

    private fun groqBody(model: String, messages: List<ChatMsg>, jsonMode: Boolean, maxTokens: Int): String {
        val msgs = Json.arrOf(*messages.map { m ->
            if (m.imagesBase64.isEmpty()) {
                Json.objOf(
                    "role" to Json.strOf(m.role),
                    "content" to Json.strOf(m.text)
                )
            } else {
                val parts = mutableListOf<Json>(
                    Json.objOf("type" to Json.strOf("text"), "text" to Json.strOf(m.text))
                )
                for (img in m.imagesBase64.take(3)) {
                    parts.add(
                        Json.objOf(
                            "type" to Json.strOf("image_url"),
                            "image_url" to Json.objOf(
                                "url" to Json.strOf("data:image/jpeg;base64,$img")
                            )
                        )
                    )
                }
                Json.objOf(
                    "role" to Json.strOf(m.role),
                    "content" to Json.arrOf(*parts.toTypedArray())
                )
            }
        }.toTypedArray())
        val body = Json.objOf(
            "model" to Json.strOf(model),
            "messages" to msgs,
            "temperature" to Json.numOf(0.2),
            "max_tokens" to Json.numOf(maxTokens)
        )
        return if (jsonMode) {
            val withFormat = LinkedHashMap(body.map)
            withFormat["response_format"] = Json.objOf("type" to Json.strOf("json_object"))
            JsonLite.stringify(Json.Obj(withFormat))
        } else {
            JsonLite.stringify(body)
        }
    }

    /** Chat completions Groq (teks + vision). */
    fun chatGroq(
        transport: HttpTransport,
        apiKey: String,
        model: String,
        messages: List<ChatMsg>,
        jsonMode: Boolean = true,
        maxTokens: Int = 4000
    ): V2Call {
        if (apiKey.isBlank()) return V2Call.Fail("Kunci Groq belum diisi — buka tab Kunci.", retryable = false)
        val result = try {
            transport.post(
                GROQ_URL,
                mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
                groqBody(model, messages, jsonMode, maxTokens)
            )
        } catch (e: Exception) {
            return V2Call.Fail("Tidak bisa menghubungi Groq: ${e.message}", retryable = true)
        }
        if (result.code !in 200..299) return mapHttpError(result.code, result.body, "Groq")
        val content = runCatching {
            val root = JsonLite.parse(result.body) as? Json.Obj ?: return V2Call.Fail(
                "Jawaban Groq bukan JSON.", retryable = false
            )
            val choices = root.arr("choices") ?: return V2Call.Fail(
                "Jawaban Groq tanpa choices.", retryable = false
            )
            val first = choices.firstOrNull() as? Json.Obj
            first?.obj("message")?.str("content")
        }.getOrNull()
        if (content.isNullOrBlank()) {
            return V2Call.Fail("Groq mengembalikan jawaban kosong.", retryable = true)
        }
        return V2Call.Ok(stripCodeFences(content), V2Provider.GROQ_VISION.takeIf { model == MODEL_VISION_GROQ }
            ?: V2Provider.GROQ_GPT_OSS)
    }

    private fun geminiBody(prompt: String, imagesBase64: List<String>, jsonMode: Boolean): String {
        val parts = mutableListOf<Json>(Json.objOf("text" to Json.strOf(prompt)))
        for (img in imagesBase64.take(3)) {
            parts.add(
                Json.objOf(
                    "inline_data" to Json.objOf(
                        "mime_type" to Json.strOf("image/jpeg"),
                        "data" to Json.strOf(img)
                    )
                )
            )
        }
        val config = LinkedHashMap<String, Json>()
        config["temperature"] = Json.numOf(0.2)
        config["maxOutputTokens"] = Json.numOf(4096)
        if (jsonMode) config["responseMimeType"] = Json.strOf("application/json")
        return JsonLite.stringify(
            Json.objOf(
                "contents" to Json.arrOf(
                    Json.objOf("parts" to Json.arrOf(*parts.toTypedArray()))
                ),
                "generationConfig" to Json.Obj(config)
            )
        )
    }

    /** generateContent Gemini (teks + vision). */
    fun generateGemini(
        transport: HttpTransport,
        apiKey: String,
        model: String,
        prompt: String,
        imagesBase64: List<String> = emptyList(),
        jsonMode: Boolean = true
    ): V2Call {
        if (apiKey.isBlank()) return V2Call.Fail("Kunci Gemini belum diisi — buka tab Kunci.", retryable = false)
        val provider = when (model) {
            MODEL_DIRECTOR_GEMINI -> V2Provider.GEMINI_FLASH
            MODEL_DIRECTOR_GEMINI_LITE -> V2Provider.GEMINI_FLASH_LITE
            else -> V2Provider.GEMINI_FLASH
        }
        val result = try {
            transport.post(
                "$GEMINI_BASE$model:generateContent",
                mapOf("x-goog-api-key" to apiKey, "Content-Type" to "application/json"),
                geminiBody(prompt, imagesBase64, jsonMode)
            )
        } catch (e: Exception) {
            return V2Call.Fail("Tidak bisa menghubungi Gemini: ${e.message}", retryable = true)
        }
        if (result.code !in 200..299) return mapHttpError(result.code, result.body, "Gemini")
        val text = runCatching {
            val root = JsonLite.parse(result.body) as? Json.Obj
            val candidates = root?.arr("candidates") ?: return V2Call.Fail(
                "Jawaban Gemini tanpa candidates.", retryable = false
            )
            val first = candidates.firstOrNull() as? Json.Obj
            val parts = first?.obj("content")?.arr("parts").orEmpty()
            parts.mapNotNull { (it as? Json.Obj)?.str("text") }.joinToString("")
        }.getOrNull()
        if (text.isNullOrBlank()) {
            return V2Call.Fail("Gemini mengembalikan jawaban kosong.", retryable = true)
        }
        return V2Call.Ok(stripCodeFences(text), provider)
    }

    /** Satu panggilan ke provider v2.0 mana pun (dipakai rantai fallback).
     *  Model Director bisa di-override dari tab Kunci (fix3); default =
     *  konstanta. Model custom Gemini tetap dihitung kuotanya sebagai
     *  GEMINI_FLASH (lihat generateGemini). */
    fun callProvider(
        transport: HttpTransport,
        provider: V2Provider,
        geminiKey: String,
        groqKey: String,
        prompt: String,
        imagesBase64: List<String> = emptyList(),
        geminiModel: String = MODEL_DIRECTOR_GEMINI,
        groqModel: String = MODEL_DIRECTOR_GROQ
    ): V2Call {
        return when (provider) {
            V2Provider.GEMINI_FLASH -> generateGemini(transport, geminiKey, geminiModel, prompt, imagesBase64)
            V2Provider.GEMINI_FLASH_LITE -> generateGemini(transport, geminiKey, MODEL_DIRECTOR_GEMINI_LITE, prompt, imagesBase64)
            V2Provider.GROQ_GPT_OSS -> chatGroq(
                transport, groqKey, groqModel,
                listOf(ChatMsg("user", prompt)), jsonMode = true, maxTokens = 6000
            )
            V2Provider.GROQ_VISION -> chatGroq(
                transport, groqKey, MODEL_VISION_GROQ,
                listOf(ChatMsg("user", prompt, imagesBase64)), jsonMode = true, maxTokens = 1500
            )
        }
    }
}
