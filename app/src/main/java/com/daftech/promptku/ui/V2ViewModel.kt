package com.daftech.promptku.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daftech.promptku.data.ApiKeyStoreV2
import com.daftech.promptku.data.ExportHelper
import com.daftech.promptku.data.LlmTransportOkHttp
import com.daftech.promptku.data.PromptKuDatabase
import com.daftech.promptku.data.V2BibleItem
import com.daftech.promptku.data.V2SceneItem
import com.daftech.promptku.engine.EffectLibrary
import com.daftech.promptku.engine.GateResult
import com.daftech.promptku.engine.JsonLite
import com.daftech.promptku.engine.ModeRender
import com.daftech.promptku.engine.QuotaTracker
import com.daftech.promptku.engine.ScriptEngine
import com.daftech.promptku.engine.ScriptPools
import com.daftech.promptku.engine.StoryBible
import com.daftech.promptku.engine.StoryInfer
import com.daftech.promptku.engine.V2Beat
import com.daftech.promptku.engine.V2Character
import com.daftech.promptku.engine.V2Episode
import com.daftech.promptku.engine.V2Call
import com.daftech.promptku.engine.V2Mode
import com.daftech.promptku.engine.V2Pipeline
import com.daftech.promptku.engine.V2ScenePlan
import com.daftech.promptku.engine.V2ScenePrompt
import com.daftech.promptku.engine.V2VisionDesc
import com.daftech.promptku.engine.ValidationGates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ViewModel v2.0 — alur baru (Ide -> Kembangkan Cerita -> Buat Prompt).
// Aturan state (sama seperti ViewModel lama): tulis Compose state HANYA
// dari Main. Kerja network/DB jalan di Dispatchers.IO, hasilnya
// ditugaskan kembali di Main. Fungsi update dinamai updateXxx.
enum class V2Step { IDEA, STORY, PROMPTS, KEYS }

data class V2Image(
    val uri: Uri,
    val base64: String,
    val label: String,
    val vision: V2VisionDesc? = null
)

class V2ViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = PromptKuDatabase.get(application)
    private val keyStore = ApiKeyStoreV2(application)

    var step by mutableStateOf(V2Step.IDEA)
        private set
    var idea by mutableStateOf("")
        private set
    var durationMinutes by mutableStateOf(3)
        private set
    var images by mutableStateOf<List<V2Image>>(emptyList())
        private set
    var bible by mutableStateOf<StoryBible?>(null)
        private set
    var isOfflineDraft by mutableStateOf(false)
        private set
    var scenes by mutableStateOf<List<V2ScenePrompt>>(emptyList())
        private set
    var gates by mutableStateOf<List<GateResult>>(emptyList())
        private set
    var progress by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("")
        private set
    // Job prompter yang sedang jalan (untuk tombol Batalkan) + id baris
    // bible yang tersimpan (untuk menyimpan koreksi chip ke cache).
    private var buildJob: Job? = null
    private var savedBibleId: Long? = null
    var quotaCounts by mutableStateOf<Map<com.daftech.promptku.engine.V2Provider, Int>>(emptyMap())
        private set
    var filmsEstimate by mutableStateOf(0)
        private set
    var hasGeminiKey by mutableStateOf(false)
        private set
    var hasGroqKey by mutableStateOf(false)
        private set
    var keysEncrypted by mutableStateOf(false)
        private set
    var savedBibles by mutableStateOf<List<V2BibleItem>>(emptyList())
        private set

    init {
        refreshKeyState()
        refreshQuota()
        loadSavedBibles()
    }

    fun updateIdea(value: String) { idea = value }
    fun updateDuration(value: Int) { durationMinutes = value.coerceIn(1, 10) }
    fun updateStep(value: V2Step) { step = value }
    fun clearStatusMessage() { statusMessage = "" }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun refreshKeyState() {
        hasGeminiKey = keyStore.hasGeminiKey()
        hasGroqKey = keyStore.hasGroqKey()
        keysEncrypted = keyStore.isEncrypted
    }

    fun refreshQuota() {
        val counts = keyStore.getCounts(today())
        quotaCounts = counts
        filmsEstimate = QuotaTracker.filmsEstimate(counts)
    }

    fun updateKeys(gemini: String, groq: String) {
        // Kosong = biarkan kunci lama (field kunci di UI selalu mulai
        // kosong demi keamanan — nilai kunci tidak pernah ditampilkan).
        if (gemini.isNotBlank()) keyStore.geminiApiKey = gemini
        if (groq.isNotBlank()) keyStore.groqApiKey = groq
        refreshKeyState()
        statusMessage = "Kunci disimpan (terenkripsi: ${if (keyStore.isEncrypted) "ya" else "TIDAK — peringatan, KeyStore bermasalah"})."
    }

    // Model Director tersimpan (dipakai pipeline; default = konstanta
    // LlmClients). v2.0 fix3 — sebelumnya field model di layar Kunci
    // tidak pernah ditulis ke mana pun (tombol Simpan cuma memanggil
    // updateKeys) dan pipeline memakai konstanta hardcode.
    val storedGeminiModel: String get() = keyStore.geminiModelId
    val storedGroqModel: String get() = keyStore.groqModelId

    fun updateModels(geminiModel: String, groqModel: String) {
        keyStore.geminiModelId = geminiModel
        keyStore.groqModelId = groqModel
    }

    private fun pipeline(): V2Pipeline = V2Pipeline(
        transport = LlmTransportOkHttp(),
        geminiKey = keyStore.geminiApiKey,
        groqKey = keyStore.groqApiKey,
        geminiModel = keyStore.geminiModelId,
        groqModel = keyStore.groqModelId,
        onCall = { provider -> keyStore.recordCall(provider, today()) }
    )

    // ---------- Gambar referensi ----------

    fun addImage(uri: Uri) {
        if (images.size >= 3) {
            statusMessage = "Maksimal 3 gambar referensi (batas resmi Flow Ingredients)."
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Menyiapkan gambar..."
            val base64 = withContext(Dispatchers.IO) { downscaleToBase64(app, uri) }
            if (base64 == null) {
                statusMessage = "Gambar tidak bisa dibaca — coba gambar lain."
            } else {
                val index = images.size
                images = images + V2Image(uri, base64, "Gambar ${index + 1}")
            }
            busy = false
            progress = ""
        }
    }

    fun removeImage(index: Int) {
        images = images.filterIndexed { i, _ -> i != index }
            .mapIndexed { i, img -> img.copy(label = "Gambar ${i + 1}") }
    }

    fun updateImageLabel(index: Int, label: String) {
        images = images.mapIndexed { i, img ->
            if (i == index) img.copy(label = label) else img
        }
    }

    fun describeImages() {
        if (images.isEmpty()) {
            statusMessage = "Belum ada gambar — tambah dulu, atau langsung Kembangkan Cerita tanpa gambar."
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Mendeskripsikan gambar (Groq Vision)..."
            try {
                val (descs, fail) = withContext(Dispatchers.IO) {
                    pipeline().describeImages(
                        images.mapIndexed { i, img -> i to img.base64 },
                        images.map { it.label }
                    )
                }
                if (fail != null && descs.isEmpty()) {
                    statusMessage = fail.message
                } else {
                    images = images.map { img ->
                        val desc = descs.firstOrNull { it.imageIndex == images.indexOf(img) }
                        if (desc != null) img.copy(vision = desc) else img
                    }
                    statusMessage = if (fail != null) {
                        "Sebagian gambar gagal dideskripsikan: ${fail.message}"
                    } else {
                        "${descs.size} gambar dideskripsikan. Periksa labelnya, koreksi kalau salah."
                    }
                }
                refreshQuota()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal mendeskripsikan gambar: ${e.message} — coba lagi."
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    // ---------- Director ----------

    fun developStory() {
        val trimmed = idea.trim()
        if (trimmed.isBlank()) {
            statusMessage = "Tulis dulu ide singkat filmnya (1–3 kalimat)."
            return
        }
        if (!keyStore.hasAnyKey()) {
            // Tanpa kunci: draft offline rule-based (Mode Manual), dilabel jelas.
            offlineDraft(trimmed)
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Sutradara senior sedang mengembangkan cerita..."
            try {
                val vision = images.mapNotNull { it.vision }
                val hint = V2Pipeline.isCourierHint(trimmed)
                val (result, fail) = withContext(Dispatchers.IO) {
                    pipeline().developStory(trimmed, durationMinutes, vision, hint)
                }
                if (fail != null) {
                    statusMessage = fail.message + " (Draft offline tetap bisa dipakai — tombol di bawah.)"
                    return@launch
                }
                if (result != null) {
                    bible = result
                    isOfflineDraft = false
                    withContext(Dispatchers.IO) { saveBible(result, trimmed) }
                    loadSavedBibles()
                    step = V2Step.STORY
                    statusMessage = "Cerita jadi. Baca, koreksi chip-nya, lalu setujui."
                }
                refreshQuota()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal mengembangkan cerita: ${e.message} — coba lagi."
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    /** Draft offline rule-based (Mode Manual) — tanpa LLM, dilabel jelas. */
    fun offlineDraft(trimmedIdea: String = idea.trim()) {
        if (trimmedIdea.isBlank()) {
            // Tanpa ide, StoryInfer jatuh ke default template v1 (Raka /
            // DRAMA / Jalanan Jakarta) — itu yang tampil sebagai "prompt lama".
            statusMessage = "Tulis dulu ide singkat filmnya (1–3 kalimat)."
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Membuat draft offline (mesin lama, tanpa AI)..."
            val result = withContext(Dispatchers.Default) {
                val inference = StoryInfer.infer(trimmedIdea)
                val heroName = StoryInfer.heroNameFromSentence(trimmedIdea) ?: "Raka"
                val input = StoryInfer.toScriptInput(trimmedIdea, heroName, durationMinutes, inference)
                val draft = ScriptEngine.build(input)
                val card = inference.characterCard
                val anchors = listOf(card.trait, card.objectProp, card.faceAnchor)
                    .map { it.trim() }.filter { it.isNotBlank() }
                val beats = draft.scenes.map { it.beatName }.distinct().mapIndexed { i, name ->
                    V2Beat(i, name, draft.scenes.firstOrNull { it.beatName == name }?.actionEn.orEmpty())
                }
                val builtBible = StoryBible(
                    title = input.title,
                    logline = trimmedIdea,
                    genre = inference.genre,
                    characters = listOf(
                        V2Character(
                            name = heroName,
                            anchorsEn = anchors.ifEmpty { listOf("a young Indonesian man", "determined eyes") },
                            lookEn = card.trait,
                            outfitEn = card.objectProp
                        )
                    ),
                    antagonistName = inference.antagonist,
                    antagonistDescEn = inference.antagonistDescEn,
                    worldName = inference.worldName,
                    worldDescEn = inference.worldDescEn,
                    lightEn = draft.scenes.firstOrNull()?.light?.phraseEn.orEmpty(),
                    ambienceEn = inference.ambienceEn,
                    beats = beats,
                    episodes = listOf(
                        V2Episode(1, input.title, trimmedIdea, draft.scenes.size)
                    ),
                    durationMinutes = durationMinutes,
                    isCourier = ScriptPools.isCourierStory(trimmedIdea),
                    sourceIdea = trimmedIdea
                )
                val plans = draft.scenes.map { scene ->
                    V2ScenePlan(
                        sceneNumber = scene.sceneNumber,
                        actionEn = scene.actionEn,
                        cameraMove = scene.cameraMove.phraseEn,
                        lensMm = scene.lensMm,
                        shotSize = scene.shotSize.name,
                        angle = scene.angle.name,
                        dialogueId = scene.dialogue?.textId.orEmpty(),
                        dialogueEn = scene.dialogue?.textEn.orEmpty(),
                        audioEn = "",
                        effectId = scene.effectId,
                        wantEn = scene.want,
                        obstacleEn = scene.obstacle,
                        transitionEn = scene.transition
                    )
                }
                val rendered = ModeRender.renderAll(
                    builtBible, plans, V2Mode.TEXT_ONLY,
                    EffectLibrary.detect(trimmedIdea).fight
                )
                builtBible to rendered
            }
            bible = result.first
            scenes = result.second
            gates = ValidationGates.runGates(result.first, result.second, hasImages = false)
            isOfflineDraft = true
            step = V2Step.PROMPTS
            statusMessage = "DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final."
            busy = false
            progress = ""
        }
    }

    // ---------- Chip konfirmasi + revisi ----------

    fun updateCourierChip(value: Boolean) {
        bible = bible?.copy(isCourier = value)
        persistBibleEdits()
    }

    fun updateWorldName(value: String) {
        bible = bible?.copy(worldName = value)
        persistBibleEdits()
    }

    fun updateAntagonistName(value: String) {
        bible = bible?.copy(antagonistName = value)
        persistBibleEdits()
    }

    /**
     * Simpan koreksi chip/dunia/antagonis ke baris cache yang sama — kalau
     * tidak, cerita yang dibuka ulang dari tab Cerita kembali ke nilai lama
     * (chip cuma hidup di memori).
     */
    private fun persistBibleEdits() {
        val current = bible ?: return
        val id = savedBibleId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val item = db.v2BibleDao().byId(id) ?: return@launch
            db.v2BibleDao().update(
                item.copy(
                    bibleJson = JsonLite.stringify(
                        com.daftech.promptku.engine.V2Models.bibleToJson(current)
                    )
                )
            )
        }
    }

    fun requestRevision(instruction: String) {
        val current = bible ?: return
        if (instruction.isBlank()) {
            statusMessage = "Tulis dulu yang mau diubah (mis. \"buat klimaksnya lebih gelap\")."
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Sutradara merevisi cerita..."
            try {
                val (revised, fail) = withContext(Dispatchers.IO) {
                    pipeline().reviseStory(current, instruction.trim())
                }
                if (fail != null) {
                    statusMessage = fail.message
                } else if (revised != null) {
                    bible = revised
                    statusMessage = "Revisi selesai. Baca lagi sebelum setuju."
                }
                refreshQuota()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal merevisi cerita: ${e.message} — coba lagi."
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    // ---------- Prompter ----------

    fun approveAndBuildPrompts() {
        val current = bible ?: return
        buildJob = viewModelScope.launch {
            busy = true
            progress = "Prompter senior membuat ${current.totalScenes} prompt scene..."
            try {
                val mode = if (images.isNotEmpty()) V2Mode.REFERENCE else V2Mode.TEXT_ONLY
                val isFight = current.genre == com.daftech.promptku.engine.Genre.ACTION ||
                    EffectLibrary.detect(current.sourceIdea).fight
                val (plans, fail) = withContext(Dispatchers.IO) {
                    try {
                        // Batas keras 15 menit: satu batch yang nyangkut (DNS
                        // WiFi, server diam) tidak boleh bikin spinner abadi.
                        // 15 menit, bukan 10 — run free-tier yang lambat tapi
                        // jalan (5 batch + retry) wajar sampai ~11 menit.
                        withTimeout(900_000L) {
                            pipeline().buildPrompts(current) { done, total ->
                                // Callback dipanggil dari thread IO — lempar
                                // ke Main (tulis state Compose dari IO crash).
                                viewModelScope.launch(Dispatchers.Main) {
                                    progress = "Prompter senior: batch $done/$total (${current.totalScenes} scene)..."
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        emptyList<V2ScenePlan>() to V2Call.Fail(
                            "Prompter timeout 15 menit — ada batch yang nyangkut " +
                                "(cek koneksi / pindah ke data seluler). Cerita tersimpan, coba lagi.",
                            retryable = true
                        )
                    }
                }
                if (fail != null && plans.isEmpty()) {
                    statusMessage = fail.message
                    return@launch
                }
                var rendered = ModeRender.renderAll(current, plans, mode, isFight)
                var gateResults = ValidationGates.runGates(current, rendered, images.isNotEmpty())
                // Gerbang 6: coba ulang 1x scene yang gagal (kecuali G4 — itu
                // soal render, bukan soal LLM), lalu tandai sisanya.
                val failedNums = gateSceneNumbers(gateResults, excludeG4 = true)
                if (failedNums.isNotEmpty()) {
                    progress = "Mengulang ${failedNums.size} scene yang gagal gerbang..."
                    val prevExit = "Retry for scenes ${failedNums.joinToString(", ")}: keep continuity, fix the flagged issues."
                    val (retryPlans, _) = withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000L) {
                                pipeline().buildPromptBatch(current, failedNums.sorted(), prevExit)
                            }
                        } catch (e: TimeoutCancellationException) {
                            // Retry nyangkut: pakai hasil pertama, jangan hang.
                            emptyList<V2ScenePlan>() to null
                        }
                    }
                    if (retryPlans.isNotEmpty()) {
                        val retryRendered = ModeRender.renderAll(current, retryPlans, mode, isFight)
                        val byNum = retryRendered.associateBy { it.sceneNumber }
                        rendered = rendered.map { scene -> byNum[scene.sceneNumber] ?: scene }
                        gateResults = ValidationGates.runGates(current, rendered, images.isNotEmpty())
                    }
                }
                val stillFailing = gateSceneNumbers(gateResults, excludeG4 = false)
                if (stillFailing.isNotEmpty()) {
                    val reasons = gateResults.filter { !it.passed }
                        .flatMap { it.failures }.joinToString("; ")
                    rendered = rendered.map { scene ->
                        if (scene.sceneNumber in stillFailing) {
                            scene.copy(needsReview = true, reviewReason = reasons.take(300))
                        } else scene
                    }
                    gateResults = ValidationGates.runGates(current, rendered, images.isNotEmpty())
                }
                scenes = rendered
                gates = gateResults
                withContext(Dispatchers.IO) { saveScenes(current, rendered) }
                step = V2Step.PROMPTS
                val flagged = rendered.count { it.needsReview }
                statusMessage = if (flagged == 0) {
                    "Semua ${rendered.size} prompt lolos 6 gerbang validasi."
                } else {
                    "$flagged dari ${rendered.size} scene ditandai perlu cek manual — baca alasannya di tiap scene."
                }
                refreshQuota()
            } catch (e: CancellationException) {
                // Dibatalkan tombol Batalkan (atau ViewModel dibersihkan):
                // JANGAN timpa pesan "Dibatalkan" dengan pesan error.
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal membuat prompt: ${e.message} — cerita tersimpan, coba lagi."
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    /** Batalkan prompter yang sedang jalan — cerita tetap tersimpan. */
    fun cancelBuild() {
        buildJob?.cancel()
        buildJob = null
        busy = false
        progress = ""
        statusMessage = "Dibatalkan — cerita tersimpan di tab Cerita, bisa diulang kapan saja."
    }

    private fun gateSceneNumbers(results: List<GateResult>, excludeG4: Boolean): List<Int> {
        val nums = mutableSetOf<Int>()
        for (result in results) {
            if (excludeG4 && result.id == "G4") continue
            if (result.id == "G6") continue
            for (failure in result.failures) {
                Regex("Scene (\\d+)").find(failure)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull()?.let { nums.add(it) }
            }
        }
        return nums.toList()
    }

    // ---------- Cache + export ----------

    private fun saveBible(result: StoryBible, ideaText: String) {
        val json = JsonLite.stringify(com.daftech.promptku.engine.V2Models.bibleToJson(result))
        savedBibleId = db.v2BibleDao().insert(
            V2BibleItem(title = result.title, idea = ideaText, bibleJson = json, createdAt = System.currentTimeMillis())
        )
    }

    private fun saveScenes(result: StoryBible, rendered: List<V2ScenePrompt>) {
        val bibles = db.v2BibleDao().all()
        val bibleId = bibles.firstOrNull { it.title == result.title }?.id ?: return
        db.v2SceneDao().deleteOfBible(bibleId)
        for (scene in rendered) {
            db.v2SceneDao().insert(
                V2SceneItem(
                    bibleId = bibleId,
                    sceneNumber = scene.sceneNumber,
                    planJson = JsonLite.stringify(
                        com.daftech.promptku.engine.V2Models.scenePlanToJson(scene.plan)
                    ),
                    flowText = scene.flowText,
                    klingText = scene.klingText,
                    seedanceText = scene.seedanceText,
                    wanText = scene.wanText,
                    needsReview = scene.needsReview,
                    reviewReason = scene.reviewReason
                )
            )
        }
    }

    fun loadSavedBibles() {
        viewModelScope.launch {
            savedBibles = withContext(Dispatchers.IO) { db.v2BibleDao().all() }
        }
    }

    fun loadBible(id: Long) {
        savedBibleId = id
        viewModelScope.launch {
            busy = true
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val item = db.v2BibleDao().byId(id) ?: return@withContext null
                    val parsed = runCatching {
                        JsonLite.parse(item.bibleJson) as? com.daftech.promptku.engine.Json.Obj
                    }.getOrNull()?.let { com.daftech.promptku.engine.V2Models.bibleFromJson(it) }
                    val sceneItems = db.v2SceneDao().ofBible(id)
                    parsed to sceneItems
                }
                if (loaded?.first != null) {
                    bible = loaded.first
                    isOfflineDraft = false
                    scenes = loaded.second.map { item ->
                        val plan = runCatching {
                            val obj = JsonLite.parse(item.planJson) as? com.daftech.promptku.engine.Json.Obj
                            obj?.let { o ->
                                com.daftech.promptku.engine.V2Models.scenePlansFromJson(
                                    com.daftech.promptku.engine.Json.arrOf(o)
                                ).firstOrNull()
                            }
                        }.getOrNull()
                        V2ScenePrompt(
                            sceneNumber = item.sceneNumber,
                            plan = plan ?: V2ScenePlan(item.sceneNumber, "", "", 35, "", "", "", "", "", "", "", "", ""),
                            flowText = item.flowText,
                            klingText = item.klingText,
                            seedanceText = item.seedanceText,
                            wanText = item.wanText,
                            needsReview = item.needsReview,
                            reviewReason = item.reviewReason
                        )
                    }
                    gates = bible?.let { ValidationGates.runGates(it, scenes, images.isNotEmpty()) }.orEmpty()
                    step = V2Step.PROMPTS
                    statusMessage = "Cerita \"${loaded.first?.title}\" dimuat dari cache (0 panggilan API)."
                } else {
                    statusMessage = "Cerita tidak bisa dimuat dari cache."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal memuat cerita dari cache: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    fun exportPrompts(context: Context): Boolean {
        val current = bible ?: return false
        val mode = if (images.isNotEmpty()) "Mode B (referensi)" else "Mode A (teks-saja)"
        val sb = StringBuilder()
        sb.appendLine("PromptKu v2.0 — ${current.title}")
        sb.appendLine("Ide: ${current.sourceIdea.ifBlank { idea }}")
        sb.appendLine("Mode: $mode | Genre: ${current.genre} | Durasi: ${current.durationMinutes} menit")
        sb.appendLine("Dunia: ${current.worldName} — ${current.worldDescEn}")
        sb.appendLine("Cahaya: ${current.lightEn}")
        sb.appendLine("Ambience: ${current.ambienceEn}")
        if (isOfflineDraft) sb.appendLine("PERINGATAN: DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final.")
        sb.appendLine("Total scene: ${scenes.size}")
        sb.appendLine()
        for (scene in scenes) {
            sb.appendLine("Scene ${scene.sceneNumber} — Flow:")
            sb.appendLine(scene.flowText)
            if (scene.plan.dialogueId.isNotBlank()) {
                sb.appendLine("Dialog ID: \"${scene.plan.dialogueId}\"")
            }
            if (scene.plan.dialogueEn.isNotBlank()) {
                sb.appendLine("Dialog EN: \"${scene.plan.dialogueEn}\"")
            }
            if (scene.needsReview) {
                sb.appendLine("PERLU CEK MANUAL: ${scene.reviewReason}")
            }
            sb.appendLine()
            sb.appendLine("Scene ${scene.sceneNumber} — Kling:")
            sb.appendLine(scene.klingText)
            sb.appendLine()
            sb.appendLine("Scene ${scene.sceneNumber} — Seedance:")
            sb.appendLine(scene.seedanceText)
            sb.appendLine()
            sb.appendLine("Scene ${scene.sceneNumber} — WanGP:")
            sb.appendLine(scene.wanText)
            sb.appendLine()
        }
        val fileName = "PromptKu-${current.title.take(40).replace(Regex("[^A-Za-z0-9]+"), "-")}-v2.txt"
        val uri = ExportHelper.exportTextToDownloads(context, fileName, sb.toString())
        statusMessage = if (uri != null) "Diexport ke Download: $fileName" else "Export gagal — coba lagi."
        return uri != null
    }

    companion object {
        /** Downscale gambar ke JPEG max 1024px + Base64 (hemat kuota Vision). */
        fun downscaleToBase64(context: Context, uri: Uri): String? {
            return try {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while ((bounds.outWidth / sample) > 1024 || (bounds.outHeight / sample) > 1024) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return null
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }
    }
}
