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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Layar Proyek v2.1 — semua film v2 tersimpan otomatis (Room v2_bibles).
// Riwayat export + naskah hidup di dalam tiap kartu (dulu tab terpisah).
// Mode Series: tombol "Lanjut Episode 2" di tiap kartu.
@Composable
fun V2ProjectsScreen(
    v2: V2ViewModel,
    onOpenBible: (Long) -> Unit,
    onContinueSeries: (Long) -> Unit,
    onNewFilm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Proyek")
                Button(onClick = onNewFilm) { Text("+ Film Baru") }
            }
            Spacer(Modifier.height(4.dp))
            Text("Semua film tersimpan otomatis — riwayat & naskah ada di dalam tiap kartu.")
        }
        if (v2.savedBibles.isEmpty()) {
            item {
                Text("Belum ada film — buat dari tab Cerita.")
            }
        }
        items(v2.savedBibles) { item ->
            val summary = v2.bibleSummaries[item.id]
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(item.title)
                Text(
                    if (summary == null) {
                        "Ide: ${item.idea.take(80)}"
                    } else if (summary.sceneCount == 0) {
                        "Draft • ${summary.durationMinutes} mnt" +
                            if (summary.episodeNumber > 1) " • Episode ${summary.episodeNumber}" else "" +
                            " • ${formatDate(item.createdAt)}"
                    } else {
                        val ok = summary.sceneCount - summary.flaggedCount
                        "${summary.sceneCount} scene • ${summary.durationMinutes} mnt" +
                            if (summary.episodeNumber > 1) " • Episode ${summary.episodeNumber}" else "" +
                            " • $ok/${summary.sceneCount} langsung pakai • ${formatDate(item.createdAt)}"
                    }
                )
                Text("Ide: ${item.idea.take(80)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenBible(item.id) }) { Text("Buka") }
                    OutlinedButton(onClick = { onContinueSeries(item.id) }) {
                        Text("Lanjut Episode ${(summary?.episodeNumber ?: 1) + 1}")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(millis))
