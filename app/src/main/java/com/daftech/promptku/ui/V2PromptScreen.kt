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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.TargetEngine

// Layar 3 v2.1 — Semua prompt scene + hasil 6 gerbang + export.
// v2.1: regenerasi 1 scene, persetujuan per scene, salin narasi SuaraKu,
// export cuma engine yang aktif (default) atau semua engine (toggle).
@Composable
fun V2PromptScreen(v2: V2ViewModel) {
    // Tombol back sistem: kembali ke Layar Ide (bukan keluar aplikasi).
    // Tanpa ini, layar prompt adalah jalan buntu — satu-satunya jalan
    // keluar adalah menutup paksa aplikasi.
    BackHandler { v2.updateStep(V2Step.IDEA) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var engineIndex by remember { mutableIntStateOf(0) }
    var exportAll by remember { mutableStateOf(false) }
    // Urutan chip mengikuti export lama (Flow/Kling/Seedance/WanGP).
    val engines = listOf(
        TargetEngine.FLOW, TargetEngine.KLING, TargetEngine.SEEDANCE, TargetEngine.WAN
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val bible = v2.bible
            if (bible == null) {
                Text("Belum ada prompt — kembangkan cerita dulu.")
            } else {
                Text(bible.title)
                Text("Total scene: ${v2.scenes.size} • Mode: ${if (v2.images.isNotEmpty()) "B (referensi)" else "A (teks-saja)"} • Rasio: ${bible.aspectRatio} • Disetujui: ${v2.scenes.count { it.approved }}/${v2.scenes.size}")
                if (v2.isOfflineDraft) {
                    Text("DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final.")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { v2.updateStep(V2Step.IDEA) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kembali ke Ide") }
        }
        if (v2.gates.isNotEmpty()) {
            item {
                Text("Gerbang validasi:")
                Column {
                    v2.gates.forEach { gate ->
                        Text("${if (gate.passed) "LULUS" else "GAGAL"} ${gate.id} — ${gate.nameId}")
                        gate.failures.forEach { failure -> Text("  • $failure") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                engines.forEachIndexed { index, engine ->
                    OutlinedButton(
                        onClick = { engineIndex = index },
                        enabled = engineIndex != index
                    ) { Text(engine.name) }
                }
            }
        }
        item {
            if (v2.busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text(v2.progress)
                }
            }
            if (v2.statusMessage.isNotBlank()) {
                Text(v2.statusMessage)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export semua engine (${v2.scenes.size * 4} blok)")
                Switch(checked = exportAll, onCheckedChange = { exportAll = it })
            }
            Button(
                onClick = { v2.exportPrompts(context, engines[engineIndex], exportAll) },
                enabled = v2.scenes.isNotEmpty() && !v2.busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (exportAll) "Export .txt — semua engine"
                    else "Export .txt — ${engines[engineIndex].name} saja"
                )
            }
            if (!exportAll) {
                Text("Export cuma ngeluarin engine yang aktif — tidak lagi 72 blok kalau lo cuma pakai Flow.")
            }
        }
        items(v2.scenes) { scene ->
            Column(modifier = Modifier.fillMaxWidth()) {
                val engine = engines[engineIndex]
                val text = when (engine) {
                    TargetEngine.FLOW -> scene.flowText
                    TargetEngine.KLING -> scene.klingText
                    TargetEngine.SEEDANCE -> scene.seedanceText
                    TargetEngine.WAN -> scene.wanText
                }
                Text("Scene ${scene.sceneNumber}${if (scene.plan.effectId.isNotBlank()) " • efek: ${scene.plan.effectId}" else ""}${if (scene.approved) " • ✓ Disetujui" else ""}")
                Text(text)
                if (scene.plan.dialogueId.isNotBlank()) {
                    Text("Dialog ID: \"${scene.plan.dialogueId}\"")
                }
                if (scene.needsReview) {
                    Text("PERLU CEK MANUAL: ${scene.reviewReason}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { v2.regenerateScene(scene.sceneNumber) },
                        enabled = !v2.busy
                    ) { Text("Ulangi scene ini") }
                    OutlinedButton(
                        onClick = { v2.toggleSceneApproval(scene.sceneNumber) },
                        enabled = !v2.busy
                    ) { Text(if (scene.approved) "✓ Disetujui" else "Setuju") }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(text))
                    }) { Text("Salin") }
                }
                if (scene.plan.narrationId.isNotBlank()) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(scene.plan.narrationId))
                    }) { Text("Salin narasi SuaraKu") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
