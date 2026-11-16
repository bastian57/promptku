package com.daftech.promptku.engine

// V2Pipeline — orkestrasi panggilan LLM v2.0 (Kotlin murni).
// Rantai fallback: coba provider pertama, kalau gagal (429/5xx/timeout)
// lanjut ke berikutnya; 401/404 (kunci salah/model pensiun) langsung
// berhenti dengan pesan yang jelas. Kuota dicatat lewat [onCall] oleh
// data layer (SharedPreferences) — pipeline sendiri tidak menyimpan apa pun.

class V2Pipeline(
    private val transport: HttpTransport,
    private val geminiKey: String,
    private val groqKey: String,
    // v2.0 fix3 — model Director dari tab Kunci (default = konstanta).
    // GEMINI_FLASH_LITE tetap konstanta (model hemat, bukan editan user).
    private val geminiModel: String = LlmClients.MODEL_DIRECTOR_GEMINI,
    private val groqModel: String = LlmClients.MODEL_DIRECTOR_GROQ,
    private val onCall: (V2Provider) -> Unit = {}
) {

    /** Coba rantai provider sampai satu mengembalikan teks. */
    private fun callChain(
        chain: List<V2Provider>,
        buildPrompt: (V2Provider) -> String,
        images: List<String> = emptyList()
    ): V2Call {
        var lastFail: V2Call.Fail? = null
        for (provider in chain) {
            val result = LlmClients.callProvider(
                transport, provider, geminiKey, groqKey,
                buildPrompt(provider), images, geminiModel, groqModel
            )
            when (result) {
                is V2Call.Ok -> {
                    onCall(result.provider)
                    return result
                }
                is V2Call.Fail -> {
                    lastFail = result
                    if (!result.retryable) return result
                }
            }
        }
        return lastFail ?: V2Call.Fail("Semua provider gagal.", retryable = true)
    }

    /** Vision: satu panggilan per gambar (maks 3). */
    fun describeImages(images: List<Pair<Int, String>>, labels: List<String>): Pair<List<V2VisionDesc>, V2Call.Fail?> {
        val out = mutableListOf<V2VisionDesc>()
        for ((index, base64) in images) {
            val label = labels.getOrNull(index) ?: "Gambar ${index + 1}"
            val prompt = DirectorPrompts.visionUser(label)
            val result = callChain(
                chain = V2Provider.visionChain(),
                buildPrompt = { prompt },
                images = listOf(base64)
            )
            when (result) {
                is V2Call.Ok -> {
                    val obj = JsonLite.extractFirstObject(result.text)
                    val desc = obj?.let { V2Models.visionFromJson(it, index) }
                    if (desc != null) out.add(desc)
                }
                is V2Call.Fail -> return out to result
            }
        }
        return out to null
    }

    /** Director: ide -> bible JSON. */
    fun developStory(
        idea: String,
        durationMinutes: Int,
        vision: List<V2VisionDesc>,
        isCourierHint: Boolean
    ): Pair<StoryBible?, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = {
                DirectorPrompts.directorUser(idea, durationMinutes, vision, isCourierHint)
            }
        )
        return when (result) {
            is V2Call.Ok -> {
                val obj = JsonLite.extractFirstObject(result.text)
                val bible = obj?.let { V2Models.bibleFromJson(it) }
                if (bible != null) bible to null
                else null to V2Call.Fail(
                    "Sutradara mengembalikan cerita yang tidak bisa dibaca — coba lagi.",
                    retryable = true
                )
            }
            is V2Call.Fail -> null to result
        }
    }

    /** Revisi bible 1 putaran (instruksi user). */
    fun reviseStory(bible: StoryBible, instruction: String): Pair<StoryBible?, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = { DirectorPrompts.revisionUser(bible, instruction) }
        )
        return when (result) {
            is V2Call.Ok -> {
                val obj = JsonLite.extractFirstObject(result.text)
                val revised = obj?.let { V2Models.bibleFromJson(it) }
                if (revised != null) revised to null
                else null to V2Call.Fail(
                    "Revisi sutradara tidak bisa dibaca — coba lagi.",
                    retryable = true
                )
            }
            is V2Call.Fail -> null to result
        }
    }

    /** Prompter: satu batch scene saja (dipakai retry gerbang 6). */
    fun buildPromptBatch(
        bible: StoryBible,
        sceneNumbers: List<Int>,
        previousExit: String
    ): Pair<List<V2ScenePlan>, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = {
                DirectorPrompts.prompterUser(bible, sceneNumbers, previousExit)
            }
        )
        return when (result) {
            is V2Call.Ok -> {
                val arr = JsonLite.extractFirstArray(result.text)
                val plans = arr?.let { V2Models.scenePlansFromJson(it) }.orEmpty()
                if (plans.isEmpty()) {
                    emptyList<V2ScenePlan>() to V2Call.Fail(
                        "Prompter mengembalikan scene kosong untuk scene ${sceneNumbers.joinToString(", ")} — coba lagi.",
                        retryable = true
                    )
                } else plans to null
            }
            is V2Call.Fail -> emptyList<V2ScenePlan>() to result
        }
    }

    /** Prompter: batch 3–4 scene per panggilan, semua scene bible. */
    fun buildPrompts(
        bible: StoryBible,
        onBatch: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<List<V2ScenePlan>, V2Call.Fail?> {
        val all = mutableListOf<V2ScenePlan>()
        val total = bible.totalScenes
        val batches = (1..total).chunked(4)
        var previousExit = "Opening: ${bible.beats.firstOrNull()?.summaryEn.orEmpty()}"
        for ((index, batch) in batches.withIndex()) {
            val (plans, fail) = buildPromptBatch(bible, batch, previousExit)
            if (fail != null) return all to fail
            all.addAll(plans)
            onBatch(index + 1, batches.size)
            previousExit = plans.lastOrNull()?.let {
                "Scene ${it.sceneNumber} ends: ${it.actionEn}. ${it.transitionEn}"
            } ?: previousExit
        }
        return all to null
    }

    companion object {
        /** Tebakan kurir dari ide (token-based — dipakai sebagai chip). */
        fun isCourierHint(idea: String): Boolean =
            TokenBoundary.containsAnyToken(
                idea,
                listOf("kurir", "paket", "pengiriman", "delivery", "ojek", "ojol", "kiriman")
            )
    }
}
