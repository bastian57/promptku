package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

@Composable
fun DnaScreen(viewModel: PromptKuViewModel) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("CHARACTER") }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(text = "DNA Lock Library", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "DNA tersimpan di library. Prompt v1.3 memakai kartu 3 jangkar (~12-15 kata), bukan DNA panjang — DNA panjang tidak ditempel supaya dunia/aksi/audio tidak terpotong (bug trim v1.2.0).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama (misal: Goku)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Inggris (verbatim)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            val descWords = description.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            Text(
                text = "$descWords kata" +
                    if (descWords > 40) " — DNA panjang, simpan di sini saja; buat kartu 3 jangkar di wizard/naskah untuk prompt." else "",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { kind = if (kind == "CHARACTER") "WORLD" else "CHARACTER" }) {
                Text("Jenis: ${if (kind == "CHARACTER") "Karakter" else "Dunia"} (tap untuk ganti)")
            }
            Button(onClick = {
                viewModel.addDna(kind, name, description)
                name = ""
                description = ""
            }) {
                Text("Simpan DNA")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Karakter tersimpan", style = MaterialTheme.typography.titleMedium)
        }
        items(viewModel.dnaCharacters) { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                    Text(text = item.descriptionEn, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.deleteDna(item) }) {
                        Text("Hapus")
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Dunia tersimpan", style = MaterialTheme.typography.titleMedium)
        }
        items(viewModel.dnaWorlds) { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                    Text(text = item.descriptionEn, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.deleteDna(item) }) {
                        Text("Hapus")
                    }
                }
            }
        }
    }
}
