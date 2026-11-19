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
        // Resume — catat tiap percobaan yang gagal supaya pesan gagal bisa
        // menampilkan SEMUA provider + alasannya, bukan cuma yang terakhir
        // (lapangan: Groq 429 menutupi alasan Gemini).
        val attempts = mutableListOf<Pair<String, String>>()
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
                    attempts.add(provider.label to result.message)
                    if (!result.retryable) return result.copy(attempts = attempts.toList())
                }
            }
        }
        return lastFail?.copy(attempts = attempts.toList())
            ?: V2Call.Fail("Semua provider gagal.", retryable = true)
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

    /** Director: ide -> bible JSON.
     *  v2.1 fix6 — isCourier DIPAKSA dari keyword hint, bukan dari JSON LLM
     *  (director pernah menulis is_courier=true untuk cerita alien Frieza
     *  padahal hint=false — chip jadi "Kisah kurir? YA" yang salah; user
     *  tetap bisa membalik chip manual di Layar Cerita). Pilihan user
     *  (aspek/bahasa/genre/episode) juga ditempel di sini, bukan dari LLM. */
    fun developStory(
        idea: String,
        durationMinutes: Int,
        vision: List<V2VisionDesc>,
        isCourierHint: Boolean,
        aspectRatio: String = "9:16",
        dialogueLang: String = "ID",
        genreHint: Genre? = null,
        continuationOf: StoryBible? = null,
        // v2.2 — engine tujuan pilihan user (default Flow).
        targetEngine: TargetEngine = TargetEngine.FLOW
    ): Pair<StoryBible?, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = {
                DirectorPrompts.directorUser(
                    idea, durationMinutes, vision, isCourierHint,
                    aspectRatio, dialogueLang, genreHint, continuationOf, targetEngine
                )
            }
        )
        return when (result) {
            is V2Call.Ok -> {
                val obj = JsonLite.extractFirstObject(result.text)
                val parsed = obj?.let { V2Models.bibleFromJson(it) }
                val bible = parsed?.copy(
                    isCourier = isCourierHint,
                    aspectRatio = aspectRatio,
                    dialogueLang = dialogueLang,
                    targetEngine = targetEngine,
                    genre = genreHint ?: parsed.genre,
                    episodeNumber = (continuationOf?.episodeNumber ?: 0) + 1
                )
                if (bible != null) bible to null
                else null to V2Call.Fail(
                    "Sutradara mengembalikan cerita yang tidak bisa dibaca — coba lagi.",
                    retryable = true
                )
            }
            is V2Call.Fail -> null to result
        }
    }

    /** Revisi bible 1 putaran (instruksi user). Pilihan yang sudah dikunci
     *  user (chip kurir, aspek, bahasa, genre, nomor episode) dipertahankan
     *  — revisi tidak boleh menimpa keputusan user dengan tebakan LLM. */
    fun reviseStory(bible: StoryBible, instruction: String): Pair<StoryBible?, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = { DirectorPrompts.revisionUser(bible, instruction) }
        )
        return when (result) {
            is V2Call.Ok -> {
                val obj = JsonLite.extractFirstObject(result.text)
                val parsed = obj?.let { V2Models.bibleFromJson(it) }
                val revised = parsed?.copy(
                    isCourier = bible.isCourier,
                    aspectRatio = bible.aspectRatio,
                    dialogueLang = bible.dialogueLang,
                    targetEngine = bible.targetEngine,
                    genre = bible.genre,
                    episodeNumber = bible.episodeNumber
                )
                if (revised != null) revised to null
                else null to V2Call.Fail(
                    "Revisi sutradara tidak bisa dibaca — coba lagi.",
                    retryable = true
                )
            }
            is V2Call.Fail -> null to result
        }
    }

    /** Prompter: satu batch scene saja (dipakai retry gerbang 6 + regenerasi
     *  1 scene). [retryReasons] = alasan gerbang per scene — prompter wajib
     *  memperbaiki TEPAT masalah itu (v2.1 fix6: instruksi retry generik
     *  "fix the flagged issues" adalah alasan retry S5 Zenith gagal).
     *  v2.1 fix6 (riset) — [ledger]: state alur yang sudah terpakai di scene
     *  lain (dihitung pemanggil dari plan yang sudah ada), ditempel ke prompt
     *  supaya LLM tidak mengulang jurus/entrance/klaim yang sudah dipakai. */
    fun buildPromptBatch(
        bible: StoryBible,
        sceneNumbers: List<Int>,
        previousExit: String,
        retryReasons: Map<Int, String> = emptyMap(),
        ledger: ArcLedger = ArcLedger.EMPTY,
        // v2.1 fix7 — mode diteruskan ke prompter (aturan penampilan
        // Mode B). Default TEXT_ONLY untuk pemanggil lama/tes lama.
        mode: V2Mode = V2Mode.TEXT_ONLY,
        // v2.2 — engine tujuan diteruskan ke prompter (aturan panjang +
        // audio per engine). Default FLOW untuk pemanggil lama/tes lama.
        engine: TargetEngine = TargetEngine.FLOW,
        // v2.2-fix2 — penjaga rekursi: batch kosong diulang SATU kali
        // dengan nudge perbaikan (lihat bawah). Default false untuk
        // pemanggil lama/tes lama.
        isRetry: Boolean = false
    ): Pair<List<V2ScenePlan>, V2Call.Fail?> {
        val result = callChain(
            chain = V2Provider.directorChain(),
            buildPrompt = {
                DirectorPrompts.prompterUser(bible, sceneNumbers, previousExit, retryReasons, ledger, mode, engine)
            }
        )
        return when (result) {
            is V2Call.Ok -> {
                val arr = JsonLite.extractFirstArray(result.text)
                // v2.1 fix6 — bersihkan aksi bocor (deskripsi dunia verbatim)
                // begitu diterima, sebelum disimpan/render. v2.1 fix4: nama
                // di awal aksi TIDAK lagi dibuang (aktor dipertahankan).
                val plans = arr?.let { V2Models.scenePlansFromJson(it) }.orEmpty()
                    .map { it.copy(actionEn = ModeRender.sanitizeAction(it.actionEn, bible)) }
                if (plans.isEmpty()) {
                    // v2.2-fix2 — film Gotres (18 Nov, malam): batch scene 1
                    // kosong, tap ulang -> scene 1 jadi, batch scene 2 kosong
                    // lagi — film terpotong 1/6. "Scene kosong" BUKAN gagal
                    // HTTP: provider mengembalikan 200 + teks, tapi teks itu
                    // tidak mengandung array plan yang bisa di-parse (prosa /
                    // JSON terpotong / item tanpa scene_number/action_en).
                    // Dua cacat struktural di sini:
                    // (1) buildPrompts TIDAK PERNAH mengulang batch yang
                    // gagal — satu batch buruk memotong SELURUH film, dan
                    // tap ulang mengulang dari scene 1 (kuota batch yang
                    // sudah jadi terbuang). Obat: ulang SATU kali per batch,
                    // dengan nudge eksplisit "hanya array JSON" (alasan
                    // perbaikan digabung dengan retryReasons yang sudah ada,
                    // bukan menimpanya — jalur retry G6 tetap membawa alasan
                    // gerbangnya).
                    // (2) teks mentah LLM DIBUANG — kegagalan berikutnya
                    // tidak bisa didiagnosis. Obat: pesan gagal membawa nama
                    // provider + 160 karakter pertama balasan mentah.
                    if (!isRetry) {
                        val repairKey = sceneNumbers.firstOrNull() ?: 0
                        val repairText = "Return ONLY the JSON array of scene plans, no prose before or after it."
                        val mergedReasons = retryReasons + (repairKey to
                            (retryReasons[repairKey]?.let { "$it $repairText" } ?: repairText))
                        return buildPromptBatch(
                            bible, sceneNumbers, previousExit, mergedReasons,
                            ledger, mode, engine, isRetry = true
                        )
                    }
                    emptyList<V2ScenePlan>() to V2Call.Fail(
                        "Prompter mengembalikan scene kosong untuk scene ${sceneNumbers.joinToString(", ")} (${result.provider.label}) — coba lagi. " +
                            "Balasan mentah: ${result.text.take(160).replace('\n', ' ')}",
                        retryable = true
                    )
                } else plans to null
            }
            is V2Call.Fail -> emptyList<V2ScenePlan>() to result
        }
    }

    /** Prompter: satu batch per BEAT (BeatMap), semua scene bible.
     *  v2.1 fix6 (riset) — batching mekanis chunked(4) DIGANTI batching per
     *  beat: satu batch = satu beat, supaya prompt posisional (nama beat +
     *  nomor scene + intensitas + kata larangan + ledger) bisa bekerja.
     *  Untuk 18 scene/5 beat: [1–3],[4–7],[8–12],[13–15],[16–18] — jumlah
     *  panggilan tetap 5. Sub-batch maks 5 scene (JSON prompter yang terlalu
     *  panjang rawan terpotong). Ledger tumbuh tiap batch dari plan yang
     *  sudah ada. */
    fun buildPrompts(
        bible: StoryBible,
        // v2.1 fix7 — mode diteruskan ke prompter (aturan penampilan
        // Mode B). Default TEXT_ONLY untuk pemanggil lama/tes lama.
        mode: V2Mode = V2Mode.TEXT_ONLY,
        // v2.2 — engine tujuan diteruskan ke prompter. Default FLOW.
        engine: TargetEngine = TargetEngine.FLOW,
        onBatch: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<List<V2ScenePlan>, V2Call.Fail?> {
        val all = mutableListOf<V2ScenePlan>()
        val total = bible.totalScenes
        val beatMap = BeatMap.build(total, bible.beats)
        val batches = beatMap.flatMap { beat ->
            (beat.firstScene..beat.lastScene).toList().chunked(5)
        }
        var previousExit = "Opening: ${bible.beats.firstOrNull()?.summaryEn.orEmpty()}"
        var ledger = ArcLedger.EMPTY
        for ((index, batch) in batches.withIndex()) {
            val (plans, fail) = buildPromptBatch(bible, batch, previousExit, ledger = ledger, mode = mode, engine = engine)
            if (fail != null) return all to fail
            all.addAll(plans)
            ledger = ArcLedger.fromPlans(all, bible)
            onBatch(index + 1, batches.size)
            previousExit = plans.lastOrNull()?.let {
                "Scene ${it.sceneNumber} ends: ${it.actionEn}. ${it.transitionEn}"
            } ?: previousExit
        }
        return all to null
    }

    /** Resume — lanjutkan film terpotong: HANYA scene yang hilang yang
     *  dipanggil ulang (dihitung dari nomor scene, bukan dari batch yang
     *  gagal — ini juga menyembuhkan batch yang diam-diam pendek: LLM
     *  mengembalikan 3 dari 5 plan yang diminta tanpa kegagalan). [keptPlans]
     *  = plan scene yang sudah jadi (dari memori / cache); hasil = gabungan
     *  kept + baru, diurutkan nomor scene. Kalau resume gagal di tengah,
     *  batch baru yang sudah berhasil tetap dikembalikan bersama kept
     *  (kemajuan monoton — tiap percobaan hanya bisa menambah scene).
     *  buildPrompts lama TIDAK disentuh — fungsi ini berdiri sendiri.
     *  previousExit per batch diambil dari scene terakhir (kept/baru) yang
     *  nomornya di bawah batch itu + ledger dari semua plan yang sudah ada
     *  (pola yang sama dengan retry G6 + regenerasi 1 scene). */
    fun resumePrompts(
        bible: StoryBible,
        keptPlans: List<V2ScenePlan>,
        mode: V2Mode = V2Mode.TEXT_ONLY,
        engine: TargetEngine = TargetEngine.FLOW,
        onBatch: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<List<V2ScenePlan>, V2Call.Fail?> {
        val keptNums = keptPlans.map { it.sceneNumber }.toSet()
        val missing = (1..bible.totalScenes).filter { it !in keptNums }
        if (missing.isEmpty()) return keptPlans.sortedBy { it.sceneNumber } to null
        val all = keptPlans.toMutableList()
        val beatMap = BeatMap.build(bible.totalScenes, bible.beats)
        val batches = beatMap.flatMap { beat ->
            (beat.firstScene..beat.lastScene).toList().chunked(5)
        }.filter { batch -> batch.any { it in missing } }
        var ledger = ArcLedger.fromPlans(keptPlans, bible)
        for ((index, batch) in batches.withIndex()) {
            val needed = batch.filter { it in missing }
            val previousExit = all.filter { it.sceneNumber < batch.first() }
                .maxByOrNull { it.sceneNumber }?.let {
                    "Scene ${it.sceneNumber} ends: ${it.actionEn}. ${it.transitionEn}"
                } ?: "Opening: ${bible.beats.firstOrNull()?.summaryEn.orEmpty()}"
            val (plans, fail) = buildPromptBatch(bible, needed, previousExit, ledger = ledger, mode = mode, engine = engine)
            if (fail != null) return all.sortedBy { it.sceneNumber } to fail
            all.addAll(plans)
            ledger = ArcLedger.fromPlans(all, bible)
            onBatch(index + 1, batches.size)
        }
        return all.sortedBy { it.sceneNumber } to null
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
