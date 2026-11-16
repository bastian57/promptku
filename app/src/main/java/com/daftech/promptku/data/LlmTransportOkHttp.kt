package com.daftech.promptku.data

import com.daftech.promptku.engine.HttpResult
import com.daftech.promptku.engine.HttpTransport
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Transport HTTP v2.0 — OkHttp (versi sama dengan UnduhKu, terbukti).
// User-Agent browser WAJIB: request tanpa UA browser pernah kena
// Cloudflare 1010 di Groq (pelajaran Nalar/ClipKu).
// Kunci API TIDAK PERNAH di-log di sini.
class LlmTransportOkHttp : HttpTransport {

    companion object {
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 13; Redmi Note 10S) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResult {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_UA)
            .post(body.toRequestBody(JSON))
        for ((key, value) in headers) builder.header(key, value)
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: 0
            return HttpResult(response.code, raw, retryAfter)
        }
    }
}
