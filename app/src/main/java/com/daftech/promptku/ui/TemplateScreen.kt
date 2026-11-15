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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.Templates

@Composable
fun TemplateScreen(
    onUseTemplate: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(text = "Galeri Template", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${Templates.all.size} template siap pakai. Tap \"Pakai\" untuk mengisi wizard, lalu edit seperlunya.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(Templates.all) { template ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = template.titleId, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${template.input.genre?.labelId} - ${template.input.style?.labelId} - ${template.input.engine?.labelId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = template.noteId, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onUseTemplate(template.id) }) {
                        Text("Pakai Template")
                    }
                }
            }
        }
    }
}
