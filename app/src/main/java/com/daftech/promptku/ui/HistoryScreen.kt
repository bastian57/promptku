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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun HistoryScreen(viewModel: PromptKuViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Riwayat", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Semua prompt tersimpan offline di HP. Cari, salin ulang, atau duplikasi.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchHistory(it)
            },
            label = { Text("Cari riwayat") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.history) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${item.engine} - Skor ${item.score}/10",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(text = item.promptText, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(item.promptText))
                                Toast.makeText(context, "Disalin.", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Salin")
                            }
                            TextButton(onClick = { viewModel.duplicateHistory(item) }) {
                                Text("Duplikat")
                            }
                            TextButton(onClick = { viewModel.deleteHistory(item) }) {
                                Text("Hapus")
                            }
                        }
                    }
                }
            }
        }
        if (viewModel.history.isEmpty()) {
            Text(text = "Riwayat masih kosong. Generate prompt di tab Buat, lalu tap Salin + Simpan.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
