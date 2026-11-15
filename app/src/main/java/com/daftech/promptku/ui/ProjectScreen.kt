package com.daftech.promptku.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.daftech.promptku.data.ExportHelper
import com.daftech.promptku.data.toGlobalLock
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.ExportText
import com.daftech.promptku.engine.GeneratedPrompt
import com.daftech.promptku.engine.GlobalLock
import com.daftech.promptku.engine.LightKind
import com.daftech.promptku.engine.TargetEngine
import com.daftech.promptku.engine.VisualStyle
import com.daftech.promptku.engine.WorldLock

@Composable
fun ProjectScreen(viewModel: PromptKuViewModel, onGoToWizard: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var newTitle by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var lockEditId by remember { mutableStateOf<Long?>(null) }
    var lockDraft by remember { mutableStateOf(GlobalLock()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Proyek Film", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Satu proyek berisi banyak scene. Global Lock ditempel ke semua scene. Ekspor sekaligus jadi file .txt ke folder Download.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Judul proyek baru") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            viewModel.createProject(newTitle) { newTitle = "" }
        }) {
            Text("Buat Proyek")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.projects) { project ->
                val lock = project.toGlobalLock()
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = project.title, style = MaterialTheme.typography.titleMedium)
                        val scenes = viewModel.projectScenes[project.id] ?: emptyList()
                        Text(text = "${scenes.size} scene", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Global Lock: ${lock.summaryId()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = {
                                expandedId = if (expandedId == project.id) null else project.id
                            }) {
                                Text(if (expandedId == project.id) "Tutup" else "Lihat Scene")
                            }
                            TextButton(onClick = {
                                val pairs = scenes.map { scene ->
                                    val engine = TargetEngine.values().firstOrNull { it.labelId == scene.engine }
                                        ?: TargetEngine.FLOW
                                    scene.title to GeneratedPrompt(
                                        engine = engine,
                                        text = scene.promptText,
                                        negativePrompt = scene.negativeText.ifBlank { null },
                                        wordCount = scene.promptText.split(Regex("\\s+")).size,
                                        warnings = emptyList()
                                    )
                                }
                                val content = ExportText.buildProjectText(project.title, pairs, lock)
                                val fileName = "PromptKu-${project.title.replace(Regex("[^A-Za-z0-9_-]"), "-")}.txt"
                                val uri = ExportHelper.exportTextToDownloads(context, fileName, content)
                                if (uri != null) {
                                    Toast.makeText(context, "Diekspor ke Download: $fileName", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Ekspor gagal.", Toast.LENGTH_LONG).show()
                                }
                            }) {
                                Text("Ekspor .txt")
                            }
                            TextButton(onClick = { viewModel.deleteProject(project) }) {
                                Text("Hapus")
                            }
                        }
                        Row {
                            OutlinedButton(onClick = {
                                viewModel.startProjectScene(project.id) { onGoToWizard() }
                            }) {
                                Text("+ Scene baru")
                            }
                            TextButton(onClick = {
                                lockDraft = lock
                                lockEditId = if (lockEditId == project.id) null else project.id
                            }) {
                                Text(if (lockEditId == project.id) "Tutup Lock" else "Atur Global Lock")
                            }
                        }
                        if (lockEditId == project.id) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Global Lock — berlaku untuk semua scene",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Field yang dikunci ditempel verbatim ke setiap scene baru. Pilih (Kosong) untuk tidak mengunci.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val worldOptions = listOf("(Kosong)") + viewModel.dnaWorlds.map { it.name }
                                    ChipRow(
                                        label = "Dunia (World Lock)",
                                        options = worldOptions,
                                        selected = lockDraft.world?.name ?: "(Kosong)",
                                        onSelect = { index ->
                                            if (index == 0) {
                                                lockDraft = lockDraft.copy(world = null)
                                            } else {
                                                val item = viewModel.dnaWorlds[index - 1]
                                                lockDraft = lockDraft.copy(
                                                    world = WorldLock(item.name, item.descriptionEn)
                                                )
                                            }
                                        }
                                    )
                                    val lightOptions = listOf("(Kosong)") + LightKind.values().map { it.labelId }
                                    ChipRow(
                                        label = "Cahaya",
                                        options = lightOptions,
                                        selected = lockDraft.light?.labelId ?: "(Kosong)",
                                        onSelect = { index ->
                                            lockDraft = lockDraft.copy(
                                                light = if (index == 0) null else LightKind.values()[index - 1]
                                            )
                                        }
                                    )
                                    val styleOptions = listOf("(Kosong)") + VisualStyle.values().map { it.labelId }
                                    ChipRow(
                                        label = "Gaya visual (membawa grade warna)",
                                        options = styleOptions,
                                        selected = lockDraft.style?.labelId ?: "(Kosong)",
                                        onSelect = { index ->
                                            lockDraft = lockDraft.copy(
                                                style = if (index == 0) null else VisualStyle.values()[index - 1]
                                            )
                                        }
                                    )
                                    val lensOptions = listOf("(Kosong)", "24mm", "35mm", "50mm", "85mm")
                                    ChipRow(
                                        label = "Lensa",
                                        options = lensOptions,
                                        selected = lockDraft.lensMm?.let { "${it}mm" } ?: "(Kosong)",
                                        onSelect = { index ->
                                            lockDraft = lockDraft.copy(
                                                lensMm = if (index == 0) null else listOf(24, 35, 50, 85)[index - 1]
                                            )
                                        }
                                    )
                                    val charOptions = listOf("(Kosong)") + viewModel.dnaCharacters.map { it.name }
                                    ChipRow(
                                        label = "Tokoh utama",
                                        options = charOptions,
                                        selected = lockDraft.character?.name ?: "(Kosong)",
                                        onSelect = { index ->
                                            if (index == 0) {
                                                lockDraft = lockDraft.copy(character = null)
                                            } else {
                                                val item = viewModel.dnaCharacters[index - 1]
                                                lockDraft = lockDraft.copy(
                                                    character = CharacterDna(item.name, item.descriptionEn)
                                                )
                                            }
                                        }
                                    )
                                    ChipRow(
                                        label = "Tokoh kedua",
                                        options = charOptions,
                                        selected = lockDraft.secondCharacter?.name ?: "(Kosong)",
                                        onSelect = { index ->
                                            if (index == 0) {
                                                lockDraft = lockDraft.copy(secondCharacter = null)
                                            } else {
                                                val item = viewModel.dnaCharacters[index - 1]
                                                lockDraft = lockDraft.copy(
                                                    secondCharacter = CharacterDna(item.name, item.descriptionEn)
                                                )
                                            }
                                        }
                                    )
                                    OutlinedTextField(
                                        value = lockDraft.ambience ?: "",
                                        onValueChange = { value ->
                                            lockDraft = lockDraft.copy(ambience = value.ifBlank { null })
                                        },
                                        label = { Text("Ambience Lock (misal: jangkrik + angin malam)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Ambience yang sama di semua klip = perekat kontinuitas antar-klip.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row {
                                        Button(onClick = {
                                            viewModel.updateProjectLock(project.id, lockDraft)
                                            lockEditId = null
                                        }) {
                                            Text("Simpan Lock")
                                        }
                                        TextButton(onClick = {
                                            viewModel.updateProjectLock(project.id, GlobalLock())
                                            lockEditId = null
                                        }) {
                                            Text("Hapus Lock")
                                        }
                                        TextButton(onClick = { lockEditId = null }) {
                                            Text("Batal")
                                        }
                                    }
                                }
                            }
                        }
                        if (expandedId == project.id) {
                            scenes.forEach { scene ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = scene.title, style = MaterialTheme.typography.titleSmall)
                                        Text(text = scene.engine, style = MaterialTheme.typography.bodySmall)
                                        if (scene.poseStart.isNotBlank() || scene.poseEnd.isNotBlank()) {
                                            Text(
                                                text = "Pose: ${scene.poseStart.ifBlank { "?" }} -> ${scene.poseEnd.ifBlank { "?" }}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(text = scene.promptText, style = MaterialTheme.typography.bodySmall)
                                        Row {
                                            TextButton(onClick = {
                                                clipboard.setText(AnnotatedString(scene.promptText))
                                                Toast.makeText(context, "Disalin.", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Salin")
                                            }
                                            TextButton(onClick = { viewModel.deleteScene(scene) }) {
                                                Text("Hapus Scene")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (viewModel.projects.isEmpty()) {
            Text(text = "Belum ada proyek. Buat dari wizard (tombol Tambah ke Proyek) atau dari sini.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
