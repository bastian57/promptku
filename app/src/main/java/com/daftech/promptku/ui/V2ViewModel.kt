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
import com.daftech.promptku.data.V2DnaItem
import com.daftech.promptku.data.V2SceneItem
import com.daftech.promptku.engine.EffectLibrary
import com.daftech.promptku.engine.GateResult
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.JsonLite
import com.daftech.promptku.engine.ModeRender
import com.daftech.promptku.engine.QuotaTracker
import com.daftech.promptku.engine.ScriptEngine
import com.daftech.promptku.engine.ScriptPools
import com.daftech.promptku.engine.StoryBible
import com.daftech.promptku.engine.StoryInfer
import com.daftech.promptku.engine.TargetEngine
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
enum class V2Step {
    IDEA, STORY, PROMPTS, KEYS,
    // v2.1 — Mode Manual (mesin v1) tinggal sebagai langkah di dalam tab
    // Cerita, bukan tab sendiri: Quick -> Wizard -> Template -> Naskah.
    MANUAL, MANUAL_DETAIL, MANUAL_TEMPLATES, MANUAL_SCRIPT
}

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
    // v2.1 — ringkasan per bible untuk kartu tab Proyek (jumlah scene,
    // berapa yang ditandai, durasi, nomor episode).
    data class V2BibleSummary(
        val sceneCount: Int,
        val flaggedCount: Int,
        val durationMinutes: Int,
        val episodeNumber: Int
    )
    var bibleSummaries by mutableStateOf<Map<Long, V2BibleSummary>>(emptyMap())
        private set
    // v2.1 — pilihan film di Layar Ide (aspek/bahasa/genre).
    var aspectRatio by mutableStateOf("9:16")
        private set
    var dialogueLang by mutableStateOf("ID")
        private set
    var genreHint by mutableStateOf<Genre?>(null)
        private set
    // v2.1 — Bank Tokoh DNA (Room v2_dna) + pilihan Hero/Antagonis.
    var dnaCharacters by mutableStateOf<List<V2DnaItem>>(emptyList())
        private set
    var selectedDnaIds by mutableStateOf<List<Long>>(emptyList())
        private set
    // v2.1 — dialog "Simpan ke DNA" (index gambar + nama tokoh).
    var dnaDialogIndex by mutableStateOf<Int?>(null)
        private set
    var dnaDialogName by mutableStateOf("")
        private set
    // v2.1 — Mode Series: bible episode sebelumnya (dihapus setelah develop).
    private var seriesContinuation: StoryBible? = null

    init {
        refreshKeyState()
        refreshQuota()
        loadSavedBibles()
        loadDna()
    }

    fun updateIdea(value: String) { idea = value }
    fun updateDuration(value: Int) { durationMinutes = value.coerceIn(1, 10) }
    fun updateStep(value: V2Step) { step = value }
    fun clearStatusMessage() { statusMessage = "" }
    fun updateAspectRatio(value: String) { aspectRatio = if (value == "16:9") "16:9" else "9:16" }
    fun updateDialogueLang(value: String) { dialogueLang = if (value == "EN") "EN" else "ID" }
    fun updateGenreHint(value: Genre?) { genreHint = value }

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
        // v2.1 — gambar yang sudah dideskripsikan (termasuk tokoh DNA yang
        // vision-nya sudah terisi) JANGAN dideskripsikan ulang: hemat kuota
        // dan deskripsi DNA tidak tertimpa hasil baru.
        val pending = images.mapIndexedNotNull { i, img ->
            if (img.vision == null) i to img.base64 else null
        }
        if (pending.isEmpty()) {
            statusMessage = "Semua gambar sudah dideskripsikan."
            return
        }
        viewModelScope.launch {
            busy = true
            progress = "Mendeskripsikan gambar (Groq Vision)..."
            try {
                val (descs, fail) = withContext(Dispatchers.IO) {
                    pipeline().describeImages(pending, images.map { it.label })
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

    // ---------- Bank Tokoh DNA (v2.1) ----------

    fun loadDna() {
        viewModelScope.launch {
            dnaCharacters = withContext(Dispatchers.IO) { db.v2DnaDao().all() }
        }
    }

    /** Pilih/batalkan tokoh DNA. Tap ke-1 = Hero, tap ke-2 = Antagonis.
     *  Tokoh terpilih ikut masuk daftar gambar referensi (vision sudah
     *  terisi dari DNA) supaya Mode B + director memakainya langsung. */
    fun toggleDnaSelection(id: Long) {
        val char = dnaCharacters.firstOrNull { it.id == id } ?: return
        if (id in selectedDnaIds) {
            selectedDnaIds = selectedDnaIds - id
            images = images.filterNot { it.base64 == char.imageBase64 }
            return
        }
        if (selectedDnaIds.size >= 2) {
            statusMessage = "Maksimal 2 tokoh DNA (Hero + Antagonis)."
            return
        }
        if (images.size >= 3) {
            statusMessage = "Maksimal 3 gambar referensi — hapus satu dulu."
            return
        }
        selectedDnaIds = selectedDnaIds + id
        val vision = V2VisionDesc(
            imageIndex = images.size,
            label = char.name,
            subjectName = char.name,
            anchorsEn = char.anchorsEn.split(";").map { it.trim() }.filter { it.isNotBlank() },
            settingEn = "",
            paletteEn = "",
            notesEn = char.descriptionEn
        )
        images = images + V2Image(Uri.EMPTY, char.imageBase64, char.name, vision)
    }

    fun openDnaDialog(index: Int) {
        val img = images.getOrNull(index) ?: return
        if (img.vision == null) {
            statusMessage = "Deskripsikan dulu gambarnya (tombol Deskripsikan Gambar)."
            return
        }
        dnaDialogIndex = index
        dnaDialogName = img.vision.subjectName.ifBlank { img.label }
    }

    fun updateDnaDialogName(value: String) { dnaDialogName = value }

    fun dismissDnaDialog() {
        dnaDialogIndex = null
        dnaDialogName = ""
    }

    fun saveImageToDna() {
        val index = dnaDialogIndex ?: return
        val img = images.getOrNull(index) ?: return
        val vision = img.vision ?: return
        val name = dnaDialogName.trim()
        if (name.isBlank()) {
            statusMessage = "Isi dulu nama tokohnya."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.v2DnaDao().insert(
                V2DnaItem(
                    name = name,
                    descriptionEn = vision.notesEn.ifBlank { vision.anchorsEn.joinToString("; ") },
                    anchorsEn = vision.anchorsEn.joinToString(";"),
                    imageBase64 = img.base64,
                    createdAt = System.currentTimeMillis()
                )
            )
            loadDna()
            withContext(Dispatchers.Main) {
                dnaDialogIndex = null
                dnaDialogName = ""
                statusMessage = "\"$name\" tersimpan di DNA — bisa dipakai di film berikutnya."
            }
        }
    }

    fun deleteDnaCharacter(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.v2DnaDao().delete(id)
            loadDna()
            withContext(Dispatchers.Main) {
                selectedDnaIds = selectedDnaIds - id
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
                val continuation = seriesContinuation
                val (result, fail) = withContext(Dispatchers.IO) {
                    pipeline().developStory(
                        trimmed, durationMinutes, vision, hint,
                        aspectRatio, dialogueLang, genreHint, continuation
                    )
                }
                if (fail != null) {
                    statusMessage = fail.message + " (Draft offline tetap bisa dipakai — tombol di bawah.)"
                    return@launch
                }
                if (result != null) {
                    bible = result
                    isOfflineDraft = false
                    seriesContinuation = null
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
                    genre = genreHint ?: inference.genre,
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
                    sourceIdea = trimmedIdea,
                    aspectRatio = aspectRatio,
                    dialogueLang = dialogueLang
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

    /** Mode Series (v2.1): siapkan episode berikutnya dari bible yang sudah
     *  ada — dunia, tokoh, cahaya, dan ambience dibawa verbatim oleh Director
     *  (lihat DirectorPrompts.directorUser, blok continuation). User tinggal
     *  tap Kembangkan Cerita di Layar Ide. */
    fun continueSeries(bibleId: Long) {
        viewModelScope.launch {
            busy = true
            try {
                val item = withContext(Dispatchers.IO) { db.v2BibleDao().byId(bibleId) }
                if (item == null) {
                    statusMessage = "Cerita tidak ditemukan di cache."
                    return@launch
                }
                val parsed = runCatching {
                    JsonLite.parse(item.bibleJson) as? com.daftech.promptku.engine.Json.Obj
                }.getOrNull()?.let { com.daftech.promptku.engine.V2Models.bibleFromJson(it) }
                if (parsed == null) {
                    statusMessage = "Cerita tidak bisa dibaca dari cache."
                    return@launch
                }
                seriesContinuation = parsed
                idea = "Lanjutan ${parsed.title} — episode berikutnya"
                durationMinutes = parsed.durationMinutes
                images = emptyList()
                selectedDnaIds = emptyList()
                bible = null
                scenes = emptyList()
                gates = emptyList()
                step = V2Step.IDEA
                statusMessage = "Lanjutan Episode ${parsed.episodeNumber + 1}: dunia, tokoh, dan cahaya \"${parsed.title}\" dibawa verbatim. Tap Kembangkan Cerita."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal menyiapkan episode lanjutan: ${e.message}"
            } finally {
                busy = false
            }
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
                var rendered = ModeRender.renderAll(current, plans, mode, isFight, images.size)
                var gateResults = ValidationGates.runGates(current, rendered, images.isNotEmpty())
                // Gerbang 6: coba ulang 1x scene yang gagal (kecuali G4 — itu
                // soal render, bukan soal LLM), lalu tandai sisanya.
                val failedNums = gateSceneNumbers(gateResults, excludeG4 = true)
                if (failedNums.isNotEmpty()) {
                    progress = "Mengulang ${failedNums.size} scene yang gagal gerbang..."
                    // v2.1 fix6 — alasan gerbang yang SEBENARNYA ikut ke prompt
                    // retry (bukan "fix the flagged issues" yang generik).
                    val retryReasons = gateResults
                        .filter { it.id != "G4" && it.id != "G6" }
                        .flatMap { it.failures }
                        .mapNotNull { failure ->
                            val m = Regex("Scene (\\d+): (.*)").find(failure)
                                ?: return@mapNotNull null
                            val num = m.groupValues[1].toIntOrNull()
                                ?: return@mapNotNull null
                            num to m.groupValues[2]
                        }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { it.value.joinToString("; ") }
                    val prevExit = "Retry for scenes ${failedNums.joinToString(", ")}: keep continuity, fix the flagged issues."
                    val (retryPlans, _) = withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000L) {
                                pipeline().buildPromptBatch(current, failedNums.sorted(), prevExit, retryReasons)
                            }
                        } catch (e: TimeoutCancellationException) {
                            // Retry nyangkut: pakai hasil pertama, jangan hang.
                            emptyList<V2ScenePlan>() to null
                        }
                    }
                    if (retryPlans.isNotEmpty()) {
                        val retryRendered = ModeRender.renderAll(current, retryPlans, mode, isFight, images.size)
                        val byNum = retryRendered.associateBy { it.sceneNumber }
                        rendered = rendered.map { scene -> byNum[scene.sceneNumber] ?: scene }
                        gateResults = ValidationGates.runGates(current, rendered, images.isNotEmpty())
                    }
                }
                val stillFailing = gateSceneNumbers(gateResults, excludeG4 = false)
                if (stillFailing.isNotEmpty()) {
                    // v2.1 fix6 — G6 dikecualikan: "Scene N: gagal gerbang tapi
                    // tidak ditandai" adalah keluhan diri sendiri yang basi
                    // (ditangkap SEBELUM penandaan) — cuma noise di export.
                    val reasons = gateResults.filter { it.id != "G6" && !it.passed }
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

    /** Regenerasi 1 scene (v2.1 inovasi #1): ganti SATU scene yang jelek tanpa
     *  mengulang 18 scene (10+ menit). Alasan gerbang scene itu ikut ke prompt
     *  supaya perbaikan tepat sasaran; efek baru diambil dari kosakata internal
     *  yang BELUM dipakai scene lain (assignEffects dengan daftar penuh). */
    fun regenerateScene(sceneNumber: Int) {
        val current = bible ?: return
        if (scenes.none { it.sceneNumber == sceneNumber }) return
        buildJob = viewModelScope.launch {
            busy = true
            progress = "Mengulang scene $sceneNumber..."
            try {
                val mode = if (images.isNotEmpty()) V2Mode.REFERENCE else V2Mode.TEXT_ONLY
                val isFight = current.genre == Genre.ACTION ||
                    EffectLibrary.detect(current.sourceIdea).fight
                val prevPlan = scenes.filter { it.sceneNumber < sceneNumber }
                    .maxByOrNull { it.sceneNumber }?.plan
                val previousExit = prevPlan?.let {
                    "Scene ${it.sceneNumber} ends: ${it.actionEn}. ${it.transitionEn}"
                } ?: "Opening: ${current.beats.firstOrNull()?.summaryEn.orEmpty()}"
                val reasons = gates.filter { it.id != "G6" }.flatMap { it.failures }
                    .filter { it.contains("Scene $sceneNumber:") }
                val retryReasons = if (reasons.isEmpty()) emptyMap()
                else mapOf(sceneNumber to reasons.joinToString("; "))
                val (newPlans, fail) = withContext(Dispatchers.IO) {
                    try {
                        withTimeout(300_000L) {
                            pipeline().buildPromptBatch(current, listOf(sceneNumber), previousExit, retryReasons)
                        }
                    } catch (e: TimeoutCancellationException) {
                        emptyList<V2ScenePlan>() to V2Call.Fail(
                            "Regenerasi scene $sceneNumber timeout 5 menit — coba lagi.",
                            retryable = true
                        )
                    }
                }
                if (fail != null || newPlans.isEmpty()) {
                    statusMessage = fail?.message ?: "Regenerasi scene $sceneNumber gagal — coba lagi."
                    return@launch
                }
                val fresh = newPlans.first()
                val allPlans = scenes.map { it.plan }
                    .map { if (it.sceneNumber == sceneNumber) fresh.copy(effectId = "") else it }
                val assigned = ModeRender.assignEffects(
                    allPlans, isFight, current.beats.size.coerceAtLeast(1)
                )
                val newPlan = assigned.firstOrNull { it.sceneNumber == sceneNumber } ?: fresh
                val rendered = ModeRender.renderScene(current, newPlan, mode, images.size)
                    .copy(approved = false)
                scenes = scenes.map { if (it.sceneNumber == sceneNumber) rendered else it }
                gates = ValidationGates.runGates(current, scenes, images.isNotEmpty())
                withContext(Dispatchers.IO) { saveScenes(current, scenes) }
                statusMessage = "Scene $sceneNumber dibuat ulang."
                refreshQuota()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusMessage = "Gagal mengulang scene $sceneNumber: ${e.message}"
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    /** Tandai/batalkan persetujuan 1 scene (tombol Setuju di layar Prompt). */
    fun toggleSceneApproval(sceneNumber: Int) {
        scenes = scenes.map {
            if (it.sceneNumber == sceneNumber) it.copy(approved = !it.approved) else it
        }
        val approved = scenes.firstOrNull { it.sceneNumber == sceneNumber }?.approved ?: return
        val id = savedBibleId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.v2SceneDao().setApproved(id, sceneNumber, approved)
        }
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
                    reviewReason = scene.reviewReason,
                    approved = scene.approved
                )
            )
        }
    }

    fun loadSavedBibles() {
        viewModelScope.launch {
            val bibles = withContext(Dispatchers.IO) { db.v2BibleDao().all() }
            savedBibles = bibles
            bibleSummaries = withContext(Dispatchers.IO) {
                bibles.associate { item ->
                    val scenes = db.v2SceneDao().ofBible(item.id)
                    val parsed = runCatching {
                        JsonLite.parse(item.bibleJson) as? com.daftech.promptku.engine.Json.Obj
                    }.getOrNull()?.let { com.daftech.promptku.engine.V2Models.bibleFromJson(it) }
                    item.id to V2BibleSummary(
                        sceneCount = scenes.size,
                        flaggedCount = scenes.count { it.needsReview },
                        durationMinutes = parsed?.durationMinutes ?: 3,
                        episodeNumber = parsed?.episodeNumber ?: 1
                    )
                }
            }
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
                            reviewReason = item.reviewReason,
                            approved = item.approved
                        )
                    }
                    gates = bible?.let { ValidationGates.runGates(it, scenes, images.isNotEmpty()) }.orEmpty()
                    // v2.1 — bible tanpa scene (baru dikembangkan, belum
                    // di-approve) dibuka di Layar Cerita, bukan Prompt kosong.
                    step = if (scenes.isNotEmpty()) V2Step.PROMPTS else V2Step.STORY
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

    /** Export .txt ke Download.
     *  v2.1 inovasi #2 — default HANYA engine yang aktif di layar Prompt
     *  (keluhan 21:02: file 72 blok padahal cuma Flow yang dipakai);
     *  [allEngines] = true baru mengeluarkan keempat engine. Narasi SuaraKu
     *  ikut diexport (per scene + blok lengkap di akhir). */
    fun exportPrompts(context: Context, engine: TargetEngine, allEngines: Boolean): Boolean {
        val current = bible ?: return false
        val mode = if (images.isNotEmpty()) "Mode B (referensi)" else "Mode A (teks-saja)"
        val engineLabel = { e: TargetEngine ->
            when (e) {
                TargetEngine.FLOW -> "Flow"
                TargetEngine.KLING -> "Kling"
                TargetEngine.SEEDANCE -> "Seedance"
                TargetEngine.WAN -> "WanGP"
            }
        }
        val sb = StringBuilder()
        sb.appendLine("PromptKu v2.1 — ${current.title}")
        sb.appendLine("Ide: ${current.sourceIdea.ifBlank { idea }}")
        sb.appendLine(
            "Mode: $mode | Genre: ${current.genre} | Durasi: ${current.durationMinutes} menit" +
                " | Rasio: ${current.aspectRatio} | Dialog: ${current.dialogueLang}" +
                if (current.episodeNumber > 1) " | Episode ${current.episodeNumber}" else ""
        )
        sb.appendLine("Dunia: ${current.worldName} — ${current.worldDescEn}")
        sb.appendLine("Cahaya: ${current.lightEn}")
        sb.appendLine("Ambience: ${current.ambienceEn}")
        if (isOfflineDraft) sb.appendLine("PERINGATAN: DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final.")
        sb.appendLine("Total scene: ${scenes.size} | Disetujui: ${scenes.count { it.approved }}/${scenes.size}")
        sb.appendLine()
        val engines = if (allEngines) {
            listOf(TargetEngine.FLOW, TargetEngine.KLING, TargetEngine.SEEDANCE, TargetEngine.WAN)
        } else {
            listOf(engine)
        }
        for (scene in scenes) {
            for (eng in engines) {
                val text = when (eng) {
                    TargetEngine.FLOW -> scene.flowText
                    TargetEngine.KLING -> scene.klingText
                    TargetEngine.SEEDANCE -> scene.seedanceText
                    TargetEngine.WAN -> scene.wanText
                }
                sb.appendLine("Scene ${scene.sceneNumber} — ${engineLabel(eng)}:")
                sb.appendLine(text)
                if (eng == engines.first()) {
                    if (scene.plan.dialogueId.isNotBlank()) {
                        sb.appendLine("Dialog ID: \"${scene.plan.dialogueId}\"")
                    }
                    if (scene.plan.dialogueEn.isNotBlank()) {
                        sb.appendLine("Dialog EN: \"${scene.plan.dialogueEn}\"")
                    }
                    if (scene.plan.narrationId.isNotBlank()) {
                        sb.appendLine("Narasi SuaraKu: \"${scene.plan.narrationId}\"")
                    }
                    if (scene.approved) {
                        sb.appendLine("Disetujui.")
                    }
                    if (scene.needsReview) {
                        sb.appendLine("PERLU CEK MANUAL: ${scene.reviewReason}")
                    }
                }
                sb.appendLine()
            }
        }
        val narrations = scenes.filter { it.plan.narrationId.isNotBlank() }
        if (narrations.isNotEmpty()) {
            sb.appendLine("NARASI LENGKAP (untuk SuaraKu — satu baris per scene):")
            narrations.forEach { scene ->
                sb.appendLine("Scene ${scene.sceneNumber}: ${scene.plan.narrationId}")
            }
            sb.appendLine()
        }
        val fileName = "PromptKu-${current.title.take(40).replace(Regex("[^A-Za-z0-9]+"), "-")}-v2.1.txt"
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
