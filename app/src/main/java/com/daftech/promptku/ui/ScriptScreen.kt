package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.ScriptEnding
import com.daftech.promptku.engine.ScriptTemplates
import com.daftech.promptku.engine.TargetEngine
import com.daftech.promptku.engine.VisualStyle

// Mode Naskah v1.2 — wizard 5 langkah: sinopsis -> naskah -> prompt.
// 8 field inti, 1 wajib (sinopsis). Draft rule-based, bukan tulisan AI.
@Composable
fun ScriptScreen(viewModel: PromptKuViewModel, onGoToProjects: () -> Unit) {
    val input = viewModel.scriptInput
    val step = viewModel.scriptStep
    val draft = viewModel.scriptDraft
    val score = viewModel.scriptScore
    var expandedSceneId by remember { mutableStateOf<Long?>(null) }
    var sceneEdits by remember { mutableStateOf<Map<Long, Pair<String, String>>>(emptyMap()) }
    var shotEdits by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    val stepTitles = listOf(
        "1. Cerita",
        "2. Tokoh & Dunia",
        "3. Genre & Gaya",
        "4. Konflik & Akhir",
        "5. Naskah"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Mode Naskah", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Sinopsis jadi naskah, naskah jadi prompt. Draft terstruktur (rule-based) — poles dialognya sebelum generate.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.scripts.isNotEmpty()) {
            Text(text = "Naskah tersimpan", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            viewModel.scripts.forEach { script ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = script.title.ifBlank { "Tanpa Judul" }, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${script.genre} • ${script.durationMinutes} menit • ${script.status}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row {
                            TextButton(onClick = { viewModel.loadScript(script.id) }) {
                                Text("Buka")
                            }
                            TextButton(onClick = { viewModel.deleteScript(script) }) {
                                Text("Hapus")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LinearProgressIndicator(
            progress = { (step + 1) / 5f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stepTitles[step], style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        when (step) {
            0 -> {
                Text(text = "Mulai dari template, atau tulis ceritamu sendiri:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val templates = listOf(
                    "Film Action 10 Menit" to ScriptTemplates.action10Min(),
                    "Drama Pendek 3 Menit" to ScriptTemplates.drama3Min(),
                    "Horor 1 Menit" to ScriptTemplates.horror1Min()
                )
                templates.forEach { (label, template) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = label, style = MaterialTheme.typography.titleSmall)
                            Text(text = template.synopsis, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { viewModel.applyScriptTemplate(template) }) {
                                Text("Pakai Template Ini")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = input.title,
                    onValueChange = { viewModel.updateScriptInput(input.copy(title = it)) },
                    label = { Text("Judul film") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.synopsis,
                    onValueChange = { viewModel.updateScriptInput(input.copy(synopsis = it)) },
                    label = { Text("Sinopsis singkat (ceritakan filmnya)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                val wordCount = input.synopsis.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                Text(
                    text = "$wordCount kata (minimal 10 — satu-satunya field wajib)",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.buildScript() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pakai Default & Buat Naskah")
                }
            }
            1 -> {
                val charNames = viewModel.dnaCharacters.map { it.name }
                ChipRow(
                    label = "Pilih tokoh tersimpan",
                    options = charNames,
                    selected = null,
                    onSelect = { index ->
                        val item = viewModel.dnaCharacters[index]
                        viewModel.updateScriptInput(
                            input.copy(protagonistName = item.name, protagonistDescEn = item.descriptionEn)
                        )
                    }
                )
                OutlinedTextField(
                    value = input.protagonistName,
                    onValueChange = { viewModel.updateScriptInput(input.copy(protagonistName = it)) },
                    label = { Text("Nama tokoh utama") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.protagonistDescEn,
                    onValueChange = { viewModel.updateScriptInput(input.copy(protagonistDescEn = it)) },
                    label = { Text("Deskripsi tokoh (Inggris, 1 kalimat)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.antagonist,
                    onValueChange = { viewModel.updateScriptInput(input.copy(antagonist = it)) },
                    label = { Text("Antagonis (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                val worldNames = viewModel.dnaWorlds.map { it.name }
                ChipRow(
                    label = "Pilih dunia tersimpan",
                    options = worldNames,
                    selected = null,
                    onSelect = { index ->
                        val item = viewModel.dnaWorlds[index]
                        viewModel.updateScriptInput(
                            input.copy(worldName = item.name, worldDescEn = item.descriptionEn)
                        )
                    }
                )
                OutlinedTextField(
                    value = input.worldName,
                    onValueChange = { viewModel.updateScriptInput(input.copy(worldName = it)) },
                    label = { Text("Nama dunia / lokasi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.worldDescEn,
                    onValueChange = { viewModel.updateScriptInput(input.copy(worldDescEn = it)) },
                    label = { Text("Deskripsi dunia (Inggris, 1 kalimat)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            2 -> {
                ChipRow(
                    label = "Genre",
                    options = Genre.values().map { it.labelId },
                    selected = input.genre.labelId,
                    onSelect = { index -> viewModel.updateScriptInput(input.copy(genre = Genre.values()[index])) }
                )
                ChipRow(
                    label = "Gaya visual",
                    options = VisualStyle.values().map { it.labelId },
                    selected = input.style.labelId,
                    onSelect = { index -> viewModel.updateScriptInput(input.copy(style = VisualStyle.values()[index])) }
                )
                val durations = listOf(1, 3, 5, 10)
                ChipRow(
                    label = "Durasi film",
                    options = durations.map { "$it menit" },
                    selected = "${input.durationMinutes} menit",
                    onSelect = { index -> viewModel.updateScriptInput(input.copy(durationMinutes = durations[index])) }
                )
                Text(
                    text = "5 menit = preset default (pemenang festival AI menang di 5–8 menit). 10 menit = proyek besar: 60 klip.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipRow(
                    label = "Engine tujuan",
                    options = TargetEngine.values().map { it.labelId },
                    selected = input.engine.labelId,
                    onSelect = { index -> viewModel.updateScriptInput(input.copy(engine = TargetEngine.values()[index])) }
                )
            }
            3 -> {
                OutlinedTextField(
                    value = input.conflict,
                    onValueChange = { viewModel.updateScriptInput(input.copy(conflict = it)) },
                    label = { Text("Konflik inti (1 kalimat)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipRow(
                    label = "Akhir yang diinginkan",
                    options = ScriptEnding.values().map { it.labelId },
                    selected = input.ending.labelId,
                    onSelect = { index -> viewModel.updateScriptInput(input.copy(ending = ScriptEnding.values()[index])) }
                )
                OutlinedTextField(
                    value = input.emotion,
                    onValueChange = { viewModel.updateScriptInput(input.copy(emotion = it)) },
                    label = { Text("Emosi inti (1 kata, misal: harapan)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.buildScript() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembangkan Naskah")
                }
            }
            4 -> {
                if (draft == null) {
                    Text(
                        text = "Belum ada naskah. Isi form (langkah 1–4) lalu tap Kembangkan Naskah, atau buka naskah tersimpan di atas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    if (score != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Naskah Score: ${score.score}/10",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (score.missing.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Yang kurang:", style = MaterialTheme.typography.titleSmall)
                                    score.missing.forEach { item ->
                                        Text(
                                            text = "- ${item.labelId}: ${item.missingHint}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = "${draft.totalClips} klip • ${draft.estimatedCostId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = draft.estimatedHoursId,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mode Teks: konsistensi wajah mengandalkan teks DNA — drift bisa terjadi setelah 5–6 klip. Re-anchor ke character sheet tiap 4–5 klip (ditandai [RE-ANCHOR]).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val activeId = viewModel.activeScriptId
                    val scenes = if (activeId != null) {
                        viewModel.scriptScenes[activeId] ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val shots = if (activeId != null) {
                        viewModel.scriptShots[activeId] ?: emptyList()
                    } else {
                        emptyList()
                    }
                    scenes.sortedBy { it.orderIndex }.forEach { scene ->
                        val sceneShots = shots.filter { it.sceneOrder == scene.orderIndex }
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Sequence ${scene.orderIndex + 1} — ${scene.beatName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(text = scene.slugline, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "${sceneShots.size} klip • ${if (scene.approved) "Disetujui" else "Belum disetujui"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row {
                                    TextButton(onClick = {
                                        expandedSceneId = if (expandedSceneId == scene.id) null else scene.id
                                    }) {
                                        Text(if (expandedSceneId == scene.id) "Tutup" else "Lihat / Edit")
                                    }
                                    if (!scene.approved && activeId != null) {
                                        TextButton(onClick = { viewModel.approveScriptSequence(activeId, scene.orderIndex) }) {
                                            Text("Setujui")
                                        }
                                    }
                                    if (activeId != null) {
                                        TextButton(onClick = {
                                            viewModel.regenerateScriptSequence(
                                                scene.orderIndex,
                                                (System.currentTimeMillis() % 1000).toInt()
                                            )
                                        }) {
                                            Text("Ulang")
                                        }
                                    }
                                }
                                if (expandedSceneId == scene.id) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Want: ${scene.want}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Obstacle: ${scene.obstacle}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val edit = sceneEdits[scene.id] ?: (scene.actionEn to scene.dialogueLine)
                                    OutlinedTextField(
                                        value = edit.first,
                                        onValueChange = { value ->
                                            sceneEdits = sceneEdits + (scene.id to (value to edit.second))
                                        },
                                        label = { Text("Aksi scene (Inggris)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = edit.second,
                                        onValueChange = { value ->
                                            sceneEdits = sceneEdits + (scene.id to (edit.first to value))
                                        },
                                        label = { Text("Dialog (Inggris, opsional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pose: ${scene.poseStart} -> ${scene.poseEnd}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Arah layar: ${scene.screenDirection} | Sisi axis: ${scene.axisSide}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row {
                                        Button(onClick = {
                                            viewModel.updateScriptScene(
                                                scene.copy(actionEn = edit.first, dialogueLine = edit.second)
                                            )
                                            sceneEdits = sceneEdits - scene.id
                                        }) {
                                            Text("Simpan Scene")
                                        }
                                        TextButton(onClick = { viewModel.deleteScriptScene(scene) }) {
                                            Text("Hapus Scene")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    sceneShots.sortedBy { it.orderIndex }.forEach { shot ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Shot ${shot.orderIndex + 1} [${shot.priority}, ${shot.shotFunction}]" +
                                                        if (shot.reAnchor) " [RE-ANCHOR]" else "",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "${shot.shotSize}, ${shot.lensMm}mm, ${shot.cameraMove}, ${shot.light}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                val shotAction = shotEdits[shot.id] ?: shot.actionEn
                                                OutlinedTextField(
                                                    value = shotAction,
                                                    onValueChange = { value ->
                                                        shotEdits = shotEdits + (shot.id to value)
                                                    },
                                                    label = { Text("Aksi shot (Inggris)") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row {
                                                    TextButton(onClick = {
                                                        viewModel.updateScriptShot(shot.copy(actionEn = shotAction))
                                                        shotEdits = shotEdits - shot.id
                                                    }) {
                                                        Text("Simpan")
                                                    }
                                                    TextButton(onClick = { viewModel.deleteScriptShot(shot) }) {
                                                        Text("Hapus Shot")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (activeId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.approveScript(activeId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Setujui Semua")
                            }
                            Button(
                                onClick = { viewModel.generateScriptPrompts(activeId) { onGoToProjects() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Generate Prompt")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.exportScriptFountain(activeId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ekspor .fountain")
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportScriptText(activeId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ekspor .txt")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { viewModel.updateScriptStep(step - 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kembali")
                }
            }
            if (step < 3) {
                Button(
                    onClick = { viewModel.updateScriptStep(step + 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lanjut")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetScriptForm() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }
    }
}
