package com.daftech.promptku.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Layar 1 v2.0 — Ide + gambar referensi opsional (maks 3).
@Composable
fun V2IdeaScreen(v2: V2ViewModel) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) v2.addImage(uri)
    }
    var durationText by remember(v2.durationMinutes) {
        mutableStateOf(v2.durationMinutes.toString())
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cerita Baru — v2.0")
            Spacer(Modifier.height(4.dp))
            Text("Tulis ide singkat, tambah gambar referensi kalau ada (opsional, maks 3), lalu Kembangkan Cerita.")
        }
        item {
            OutlinedTextField(
                value = v2.idea,
                onValueChange = { v2.updateIdea(it) },
                label = { Text("Ide film (1–3 kalimat)") },
                placeholder = { Text("Kurir terakhir di kota yang sudah runtuh…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        item {
            OutlinedTextField(
                value = durationText,
                onValueChange = { text ->
                    durationText = text
                    text.toIntOrNull()?.let { v2.updateDuration(it) }
                },
                label = { Text("Durasi (menit, 1–10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text("1 menit = 6 scene @10 detik. Film 3 menit = 18 scene.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    enabled = v2.images.size < 3 && !v2.busy
                ) { Text("Tambah Gambar (${v2.images.size}/3)") }
                OutlinedButton(
                    onClick = { v2.describeImages() },
                    enabled = v2.images.isNotEmpty() && !v2.busy
                ) { Text("Deskripsikan Gambar") }
            }
            Text("Gambar dikirim ke Vision (Groq) untuk dideskripsikan — bukan disimpan mentah di prompt.")
        }
        itemsIndexed(v2.images) { index, img ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bitmap = remember(img.base64) {
                    val bytes = Base64.decode(img.base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = img.label,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = img.label,
                        onValueChange = { v2.updateImageLabel(index, it) },
                        label = { Text("Label gambar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (img.vision != null) {
                        Text("Vision: ${img.vision.subjectName} — ${img.vision.anchorsEn.joinToString("; ")}")
                    }
                }
                TextButton(onClick = { v2.removeImage(index) }) { Text("Hapus") }
            }
        }
        item {
            if (v2.busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(v2.progress)
                }
            }
            if (v2.statusMessage.isNotBlank()) {
                Text(v2.statusMessage)
            }
        }
        item {
            Button(
                onClick = { v2.developStory() },
                enabled = !v2.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kembangkan Cerita") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { v2.offlineDraft() },
                enabled = !v2.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Draft Offline (tanpa AI, mesin lama)") }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { v2.updateStep(V2Step.KEYS) }) { Text("Kunci API") }
                OutlinedButton(onClick = { v2.refreshQuota() }) { Text("Cek Kuota") }
            }
            Text("Kuota hari ini: Gemini Flash ${quotaRemaining(v2, "GEMINI_FLASH")}/20 • Flash-Lite ${quotaRemaining(v2, "GEMINI_FLASH_LITE")}/500 • Groq ${quotaRemaining(v2, "GROQ_VISION")}/1000. Perkiraan film: ${v2.filmsEstimate}.")
            Text("Kunci: Gemini ${if (v2.hasGeminiKey) "terisi" else "KOSONG"} • Groq ${if (v2.hasGroqKey) "terisi" else "KOSONG"}${if (!v2.keysEncrypted) " • PERINGATAN: kunci tidak terenkripsi" else ""}.")
        }
        if (v2.savedBibles.isNotEmpty()) {
            item { Text("Cerita tersimpan (cache, 0 panggilan API):") }
            itemsIndexed(v2.savedBibles) { _, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.title, modifier = Modifier.weight(1f))
                    TextButton(onClick = { v2.loadBible(item.id) }) { Text("Buka") }
                }
            }
        }
    }
}

private fun quotaRemaining(v2: V2ViewModel, providerName: String): Int {
    val provider = com.daftech.promptku.engine.V2Provider.values()
        .firstOrNull { it.name == providerName } ?: return 0
    return com.daftech.promptku.engine.QuotaTracker.remaining(provider, v2.quotaCounts)
}
