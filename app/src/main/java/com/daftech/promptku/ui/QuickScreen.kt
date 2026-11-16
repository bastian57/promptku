package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.StoryInfer
import com.daftech.promptku.engine.VisualStyle

// Mode Cepat v1.4 — "Satu Kalimat Jadi Film".
// Layar pertama app: 3 field saja (1 kalimat, nama tokoh, durasi).
// StoryInfer menebak genre/dunia/gaya/ambience dari kata kunci (offline);
// tebakan tampil sebagai chip yang bisa diubah SEBELUM tekan Buat Film.
// Wizard 5 langkah tetap ada sebagai Mode Detail.
@Composable
fun QuickScreen(
    viewModel: PromptKuViewModel,
    onGoToDetail: () -> Unit,
    onGoToScript: () -> Unit
) {
    val sentence = viewModel.quickSentence
    val heroName = viewModel.quickHeroName
    val minutes = viewModel.quickMinutes
    val inference = viewModel.quickInference
    val wordCount = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }.size

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Mode Cepat — Satu Kalimat Jadi Film", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Tulis 1 kalimat, film jadi. Tanpa 5 langkah, tanpa 20 field. " +
                "App menebak genre dan dunia dari kalimatmu — tebakan bisa diubah.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onGoToDetail,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mode Detail (wizard 5 langkah)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = sentence,
            onValueChange = { viewModel.updateQuickSentence(it) },
            label = { Text("Ceritamu dalam 1 kalimat (minimal 10 kata)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Text(
            text = "$wordCount kata (minimal 10)",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Coba contoh (ketuk untuk isi):", style = MaterialTheme.typography.titleSmall)
        StoryInfer.EXAMPLES.forEach { example ->
            TextButton(onClick = { viewModel.useQuickExample(example) }) {
                Text(text = example, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = heroName,
            onValueChange = { viewModel.updateQuickHeroName(it) },
            label = { Text("Nama tokoh (kosong = dari kalimat)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        val durationOptions = listOf("1 menit (6 scene)", "3 menit (18 scene)", "5 menit (30 scene)", "10 menit (60 scene)")
        val durationValues = listOf(1, 3, 5, 10)
        ChipRow(
            label = "Durasi (1 scene = 1 klip 10 detik)",
            options = durationOptions,
            selected = durationOptions[durationValues.indexOf(minutes)],
            onSelect = { index -> viewModel.updateQuickMinutes(durationValues[index]) }
        )
        Button(
            onClick = { viewModel.inferQuick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tebak Genre & Dunia")
        }
        if (inference != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tebakan app — ketuk untuk ubah:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChipRow(
                        label = "Genre",
                        options = Genre.values().map { it.labelId },
                        selected = inference.genre.labelId,
                        onSelect = { index ->
                            viewModel.updateQuickGenre(Genre.values()[index])
                        }
                    )
                    ChipRow(
                        label = "Gaya visual",
                        options = VisualStyle.values().map { it.labelId },
                        selected = inference.style.labelId,
                        onSelect = { index ->
                            viewModel.updateQuickStyle(VisualStyle.values()[index])
                        }
                    )
                    OutlinedTextField(
                        value = viewModel.quickWorldName,
                        onValueChange = { value ->
                            viewModel.updateQuickWorld(value, viewModel.quickWorldDescEn)
                        },
                        label = { Text("Dunia (nama)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = viewModel.quickWorldDescEn,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.quickAmbience,
                        onValueChange = { viewModel.updateQuickAmbience(it) },
                        label = { Text("Ambience (suara latar semua klip)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Kartu tokoh: ${inference.characterCard.toDescription()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (wordCount >= 10) {
                    viewModel.buildQuickFilm()
                    onGoToScript()
                } else {
                    viewModel.buildQuickFilm()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buat Film")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "100% offline — tebakan dari kata kunci, bukan AI online. " +
                "Kalau tebakan salah, ubah chip-nya sebelum tekan Buat Film. " +
                "Hasilnya bisa diedit per scene di tab Naskah.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
