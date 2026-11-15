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
import com.daftech.promptku.engine.ExportText
import com.daftech.promptku.engine.GeneratedPrompt
import com.daftech.promptku.engine.TargetEngine

@Composable
fun ProjectScreen(viewModel: PromptKuViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var newTitle by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Proyek Film", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Satu proyek berisi banyak scene. Ekspor sekaligus jadi file .txt ke folder Download.",
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
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = project.title, style = MaterialTheme.typography.titleMedium)
                        val scenes = viewModel.projectScenes[project.id] ?: emptyList()
                        Text(text = "${scenes.size} scene", style = MaterialTheme.typography.bodySmall)
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
                                val content = ExportText.buildProjectText(project.title, pairs)
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
                        if (expandedId == project.id) {
                            scenes.forEach { scene ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = scene.title, style = MaterialTheme.typography.titleSmall)
                                        Text(text = scene.engine, style = MaterialTheme.typography.bodySmall)
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
