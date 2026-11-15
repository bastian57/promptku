package com.daftech.promptku.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daftech.promptku.data.DnaItem
import com.daftech.promptku.data.HistoryItem
import com.daftech.promptku.data.ProjectItem
import com.daftech.promptku.data.PromptKuDatabase
import com.daftech.promptku.data.SceneItem
import com.daftech.promptku.data.toGlobalLock
import com.daftech.promptku.data.withLock
import com.daftech.promptku.engine.CameraAngle
import com.daftech.promptku.engine.CameraMove
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.Dice
import com.daftech.promptku.engine.FocusMode
import com.daftech.promptku.engine.GalleryTemplate
import com.daftech.promptku.engine.GeneratedPrompt
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.GlobalLock
import com.daftech.promptku.engine.HideTactic
import com.daftech.promptku.engine.LightKind
import com.daftech.promptku.engine.PromptEngine
import com.daftech.promptku.engine.PromptScore
import com.daftech.promptku.engine.ScoreResult
import com.daftech.promptku.engine.ShotInput
import com.daftech.promptku.engine.ShotSize
import com.daftech.promptku.engine.TargetEngine
import com.daftech.promptku.engine.Templates
import com.daftech.promptku.engine.VisualStyle
import com.daftech.promptku.engine.WorldLock
import com.daftech.promptku.engine.continuityPoseStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel PromptKu v1.0.0.
// Aturan state (pelajaran ClipKu/UnduhKu): tulis Compose state HANYA dari
// Main. Kerja database jalan di Dispatchers.IO dan hasilnya ditugaskan
// kembali di Main setelah withContext selesai. Fungsi update dinamai
// updateXxx — JANGAN setXxx, supaya tidak clash dengan setter JVM
// property var yang bernama sama.
class PromptKuViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PromptKuDatabase.get(application)

    var input by mutableStateOf(ShotInput())
        private set

    var wizardStep by mutableStateOf(0)
        private set

    var generatedAll by mutableStateOf<List<GeneratedPrompt>>(emptyList())
        private set

    var resultEngineIndex by mutableStateOf(0)
        private set

    var score by mutableStateOf<ScoreResult?>(null)
        private set

    var dnaCharacters by mutableStateOf<List<DnaItem>>(emptyList())
        private set

    var dnaWorlds by mutableStateOf<List<DnaItem>>(emptyList())
        private set

    var history by mutableStateOf<List<HistoryItem>>(emptyList())
        private set

    var projects by mutableStateOf<List<ProjectItem>>(emptyList())
        private set

    var projectScenes by mutableStateOf<Map<Long, List<SceneItem>>>(emptyMap())
        private set

    // Mode proyek v1.1: kalau tidak null, wizard sedang merakit scene
    // untuk proyek ini dan Global Lock-nya ditegakkan saat generate.
    var activeProjectId by mutableStateOf<Long?>(null)
        private set

    var activeLock by mutableStateOf<GlobalLock?>(null)
        private set

    var statusMessage by mutableStateOf("")
        private set

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val chars = withContext(Dispatchers.IO) { db.dnaDao().byKind("CHARACTER") }
            val worlds = withContext(Dispatchers.IO) { db.dnaDao().byKind("WORLD") }
            if (chars.isEmpty() && worlds.isEmpty()) {
                withContext(Dispatchers.IO) { seedDefaultDna() }
                val chars2 = withContext(Dispatchers.IO) { db.dnaDao().byKind("CHARACTER") }
                val worlds2 = withContext(Dispatchers.IO) { db.dnaDao().byKind("WORLD") }
                dnaCharacters = chars2
                dnaWorlds = worlds2
            } else {
                dnaCharacters = chars
                dnaWorlds = worlds
            }
            val hist = withContext(Dispatchers.IO) { db.historyDao().all() }
            history = hist
            val projs = withContext(Dispatchers.IO) { db.projectDao().allProjects() }
            projects = projs
            val scenes = mutableMapOf<Long, List<SceneItem>>()
            for (project in projs) {
                val list = withContext(Dispatchers.IO) { db.projectDao().scenesOf(project.id) }
                scenes[project.id] = list
            }
            projectScenes = scenes
        }
    }

    private suspend fun seedDefaultDna() {
        val now = System.currentTimeMillis()
        db.dnaDao().insert(
            DnaItem(
                kind = "CHARACTER",
                name = Templates.gokuDna.name,
                descriptionEn = Templates.gokuDna.descriptionEn,
                createdAt = now
            )
        )
        db.dnaDao().insert(
            DnaItem(
                kind = "CHARACTER",
                name = Templates.cellDna.name,
                descriptionEn = Templates.cellDna.descriptionEn,
                createdAt = now + 1
            )
        )
        db.dnaDao().insert(
            DnaItem(
                kind = "WORLD",
                name = Templates.gokuVsCellWorld.name,
                descriptionEn = Templates.gokuVsCellWorld.descriptionEn,
                createdAt = now + 2
            )
        )
    }

    fun updateWizardStep(step: Int) {
        wizardStep = step.coerceIn(0, 4)
    }

    fun updateTitle(value: String) {
        input = input.copy(title = value)
    }

    fun updateGenre(genre: Genre) {
        input = input.copy(genre = genre)
    }

    fun updateStyle(style: VisualStyle) {
        input = input.copy(style = style)
    }

    fun updateEngine(engine: TargetEngine) {
        input = input.copy(engine = engine)
        if (generatedAll.isNotEmpty()) {
            generate()
        }
    }

    fun updateCharacter(dna: CharacterDna?) {
        input = input.copy(character = dna)
    }

    fun updateSecondCharacter(dna: CharacterDna?) {
        input = input.copy(secondCharacter = dna)
    }

    fun updateWorld(world: WorldLock?) {
        input = input.copy(world = world)
    }

    fun updateShotSize(size: ShotSize) {
        input = input.copy(shotSize = size)
    }

    fun updateAngle(angle: CameraAngle) {
        input = input.copy(angle = angle)
    }

    fun updateLens(lensMm: Int) {
        input = input.copy(lensMm = lensMm)
    }

    fun updateCameraMove(move: CameraMove) {
        input = input.copy(cameraMove = move)
    }

    fun updateLight(light: LightKind) {
        input = input.copy(light = light)
    }

    fun updateAction(value: String) {
        input = input.copy(actionEn = value)
    }

    fun updatePoses(start: String, end: String) {
        input = input.copy(poseStart = start, poseEnd = end)
    }

    fun updateMaterial(value: String) {
        input = input.copy(materialEn = value)
    }

    fun updateAudio(value: String) {
        input = input.copy(audioEn = value)
    }

    fun updateDialogue(name: String, line: String) {
        input = input.copy(dialogueName = name, dialogueLine = line)
    }

    fun updateFocus(focus: FocusMode) {
        input = input.copy(focusMode = focus)
    }

    fun updateHide(tactic: HideTactic) {
        input = input.copy(hideTactic = tactic)
    }

    fun applyTemplate(template: GalleryTemplate) {
        input = template.input
        wizardStep = 4
        generate()
        statusMessage = "Template dipakai: ${template.titleId}"
    }

    fun rollDice() {
        input = Dice.roll()
        wizardStep = 4
        generate()
        statusMessage = "Dadu dilempar — edit seperlunya, lalu generate ulang."
    }

    fun resetWizard() {
        input = ShotInput()
        wizardStep = 0
        generatedAll = emptyList()
        resultEngineIndex = 0
        score = null
        activeProjectId = null
        activeLock = null
    }

    // Masuk mode proyek: wizard dimulai dari Global Lock proyek + pose awal
    // mengikuti pose akhir scene terakhir. Dipanggil dari layar Proyek.
    fun startProjectScene(projectId: Long, onReady: () -> Unit) {
        viewModelScope.launch {
            val project = withContext(Dispatchers.IO) {
                db.projectDao().allProjects().firstOrNull { it.id == projectId }
            } ?: return@launch
            val scenes = withContext(Dispatchers.IO) { db.projectDao().scenesOf(projectId) }
            val lock = project.toGlobalLock()
            val lastPoseEnd = scenes.maxByOrNull { it.orderIndex }?.poseEnd ?: ""
            activeProjectId = projectId
            activeLock = if (lock.isEmpty()) null else lock
            input = lock.applyTo(ShotInput()).copy(
                poseStart = continuityPoseStart(lastPoseEnd)
            )
            wizardStep = 0
            generatedAll = emptyList()
            resultEngineIndex = 0
            score = null
            statusMessage = "Mode proyek: ${project.title} — Global Lock aktif."
            onReady()
        }
    }

    fun exitProjectMode() {
        activeProjectId = null
        activeLock = null
        statusMessage = "Keluar dari mode proyek."
    }

    fun updateProjectLock(projectId: Long, lock: GlobalLock) {
        viewModelScope.launch {
            val project = withContext(Dispatchers.IO) {
                db.projectDao().allProjects().firstOrNull { it.id == projectId }
            } ?: return@launch
            withContext(Dispatchers.IO) {
                db.projectDao().updateProject(project.withLock(lock))
            }
            if (activeProjectId == projectId) {
                activeLock = if (lock.isEmpty()) null else lock
                input = lock.applyTo(input)
            }
            refreshAll()
            statusMessage = "Global Lock disimpan."
        }
    }

    fun generate() {
        if (activeLock != null) {
            input = activeLock!!.applyTo(input)
        }
        val current = input
        generatedAll = PromptEngine.buildAll(current)
        resultEngineIndex = when (current.engine) {
            TargetEngine.FLOW -> 0
            TargetEngine.WAN -> 1
            TargetEngine.KLING -> 2
            TargetEngine.SEEDANCE -> 3
            null -> 0
        }
        score = PromptScore.score(current)
    }

    fun updateResultEngine(index: Int) {
        resultEngineIndex = index.coerceIn(0, 3)
    }

    fun saveCurrentToHistory() {
        val prompt = generatedAll.getOrNull(resultEngineIndex) ?: return
        val currentScore = score?.score ?: 0
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.historyDao().insert(
                    HistoryItem(
                        title = input.title.ifBlank { "Prompt tanpa judul" },
                        engine = prompt.engine.labelId,
                        promptText = prompt.text,
                        negativeText = prompt.negativePrompt ?: "",
                        score = currentScore,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            val hist = withContext(Dispatchers.IO) { db.historyDao().all() }
            history = hist
            statusMessage = "Disimpan ke Riwayat."
        }
    }

    fun searchHistory(query: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (query.isBlank()) db.historyDao().all() else db.historyDao().search(query)
            }
            history = result
        }
    }

    fun duplicateHistory(item: HistoryItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.historyDao().insert(item.copy(id = 0, createdAt = System.currentTimeMillis()))
            }
            val hist = withContext(Dispatchers.IO) { db.historyDao().all() }
            history = hist
            statusMessage = "Prompt diduplikasi."
        }
    }

    fun deleteHistory(item: HistoryItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.historyDao().delete(item) }
            val hist = withContext(Dispatchers.IO) { db.historyDao().all() }
            history = hist
        }
    }

    fun addDna(kind: String, name: String, descriptionEn: String) {
        if (name.isBlank() || descriptionEn.isBlank()) {
            statusMessage = "Nama dan deskripsi DNA wajib diisi."
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.dnaDao().insert(
                    DnaItem(kind = kind, name = name, descriptionEn = descriptionEn, createdAt = System.currentTimeMillis())
                )
            }
            refreshAll()
            statusMessage = "DNA disimpan: $name"
        }
    }

    fun deleteDna(item: DnaItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.dnaDao().delete(item) }
            refreshAll()
        }
    }

    fun createProject(title: String, onCreated: (Long) -> Unit) {
        if (title.isBlank()) {
            statusMessage = "Judul proyek wajib diisi."
            return
        }
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                db.projectDao().insertProject(ProjectItem(title = title, createdAt = System.currentTimeMillis()))
            }
            refreshAll()
            statusMessage = "Proyek dibuat: $title"
            onCreated(id)
        }
    }

    fun addCurrentSceneToProject(projectId: Long, sceneTitle: String) {
        val prompt = generatedAll.getOrNull(resultEngineIndex) ?: return
        viewModelScope.launch {
            val order = withContext(Dispatchers.IO) {
                db.projectDao().scenesOf(projectId).size
            }
            withContext(Dispatchers.IO) {
                db.projectDao().insertScene(
                    SceneItem(
                        projectId = projectId,
                        orderIndex = order,
                        title = sceneTitle.ifBlank { "Scene ${order + 1}" },
                        engine = prompt.engine.labelId,
                        promptText = prompt.text,
                        negativeText = prompt.negativePrompt ?: "",
                        createdAt = System.currentTimeMillis(),
                        poseStart = input.poseStart,
                        poseEnd = input.poseEnd
                    )
                )
            }
            refreshAll()
            statusMessage = "Scene ditambahkan ke proyek."
        }
    }

    fun deleteProject(project: ProjectItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.projectDao().deleteProject(project) }
            refreshAll()
        }
    }

    fun deleteScene(scene: SceneItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.projectDao().deleteScene(scene) }
            refreshAll()
        }
    }

    fun clearStatusMessage() {
        statusMessage = ""
    }
}
