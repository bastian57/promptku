package com.daftech.promptku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.TargetEngine

// Layar 2 v2.0 — Cerita hasil Director + chip konfirmasi + revisi.
@Composable
fun V2StoryScreen(v2: V2ViewModel) {
    BackHandler { v2.updateStep(V2Step.IDEA) }
    val bible = v2.bible
    var revision by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bible == null) {
            item { Text("Belum ada cerita — kembangkan dari Layar Ide dulu.") }
            item {
                OutlinedButton(onClick = { v2.updateStep(V2Step.IDEA) }) { Text("Ke Layar Ide") }
            }
            return@LazyColumn
        }
        item {
            Text(bible.title)
            Text("Logline: ${bible.logline}")
            Text("Genre: ${bible.genre} • Durasi: ${bible.durationMinutes} menit • ${bible.totalScenes} scene")
            Text("Rasio: ${bible.aspectRatio} • Dialog: ${if (bible.dialogueLang == "EN") "English" else "Indonesia"} • Engine: ${bible.targetEngine.name}${if (bible.episodeNumber > 1) " • Episode ${bible.episodeNumber}" else ""}")
            if (v2.isOfflineDraft) {
                Text("DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final.")
            }
        }
        item {
            Text("Tokoh:")
            Column {
                bible.characters.forEach { c ->
                    Text("• ${c.name}: ${c.anchorsEn.joinToString("; ")}")
                }
            }
            if (bible.antagonistName.isNotBlank()) {
                Text("Antagonis: ${bible.antagonistName} — ${bible.antagonistDescEn}")
            }
        }
        item {
            Text("Dunia: ${bible.worldName}")
            Text(bible.worldDescEn)
            Text("Cahaya: ${bible.lightEn}")
            Text("Ambience: ${bible.ambienceEn}")
        }
        item {
            Text("Busur cerita:")
            Column {
                bible.beats.forEach { beat ->
                    Text("${beat.index + 1}. ${beat.name} — ${beat.summaryEn}")
                }
            }
        }
        if (bible.episodes.size > 1) {
            item {
                Text("Episode:")
                Column {
                    bible.episodes.forEach { ep ->
                        Text("Ep ${ep.number}. ${ep.title} — ${ep.logline} (${ep.sceneCount} scene)")
                    }
                }
            }
        }
        item {
            Text("Chip konfirmasi — koreksi tebakan sutradara sebelum setuju:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { v2.updateCourierChip(!bible.isCourier) },
                    enabled = !v2.busy
                ) {
                    Text("Kisah kurir? ${if (bible.isCourier) "YA" else "TIDAK"}")
                }
            }
            // v2.2 — ganti engine tujuan (prompter berikutnya pakai engine
            // baru; render ulang ke engine lain tetap gratis).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetEngine.values().forEach { engine ->
                    OutlinedButton(
                        onClick = { v2.updateTargetEngineChip(engine) },
                        enabled = !v2.busy && bible.targetEngine != engine
                    ) {
                        Text(engine.name)
                    }
                }
            }
            OutlinedTextField(
                value = bible.worldName,
                onValueChange = { v2.updateWorldName(it) },
                label = { Text("Nama dunia") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bible.antagonistName,
                onValueChange = { v2.updateAntagonistName(it) },
                label = { Text("Nama antagonis") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = revision,
                onValueChange = { revision = it },
                label = { Text("Minta ubah (mis. buat klimaksnya lebih gelap)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { v2.requestRevision(revision) },
                enabled = !v2.busy && revision.isNotBlank()
            ) { Text("Minta Ubah (1 putaran)") }
        }
        item {
            if (v2.busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text(v2.progress)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { v2.cancelBuild() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Batalkan") }
            }
            if (v2.statusMessage.isNotBlank()) {
                Text(v2.statusMessage)
            }
            Button(
                onClick = { v2.approveAndBuildPrompts() },
                enabled = !v2.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Setuju, Buat Prompt") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { v2.updateStep(V2Step.IDEA) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kembali ke Ide") }
        }
    }
}
