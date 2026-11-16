package com.daftech.promptku.ui

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.TargetEngine

// Layar 3 v2.0 — Semua prompt scene + hasil 6 gerbang + export.
@Composable
fun V2PromptScreen(v2: V2ViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var engineIndex by remember { mutableIntStateOf(0) }
    val engines = TargetEngine.values()
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
                Text("Total scene: ${v2.scenes.size} • Mode: ${if (v2.images.isNotEmpty()) "B (referensi)" else "A (teks-saja)"}")
                if (v2.isOfflineDraft) {
                    Text("DRAFT OFFLINE (rule-based, tanpa AI) — ulangi di cloud untuk prompt final.")
                }
            }
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
            Button(
                onClick = { v2.exportPrompts(context) },
                enabled = v2.scenes.isNotEmpty() && !v2.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export .txt ke Download") }
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
                Text("Scene ${scene.sceneNumber}${if (scene.plan.effectId.isNotBlank()) " • efek: ${scene.plan.effectId}" else ""}")
                Text(text)
                if (scene.plan.dialogueId.isNotBlank()) {
                    Text("Dialog ID: \"${scene.plan.dialogueId}\"")
                }
                if (scene.needsReview) {
                    Text("PERLU CEK MANUAL: ${scene.reviewReason}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(text))
                    }) { Text("Salin") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
