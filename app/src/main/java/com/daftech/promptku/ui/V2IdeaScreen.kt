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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.TargetEngine

// Layar 1 v2.1 — Ide + bank tokoh DNA + gambar referensi + pengaturan film.
// Bottom bar cuma Cerita + Proyek: tab lama yang dihapus fungsinya pindah ke sini.
private val QUICK_IDEAS = listOf(
    "Kurir terakhir di kota yang sudah runtuh",
    "Invasi alien ke bumi",
    "Pendekar terakhir membela desanya",
    "Hantu laut di tengah badai",
    "Robot kecil mencari tuannya",
    "Kota terapung di atas awan"
)

private val GENRE_CHOICES = listOf(
    Genre.ACTION, Genre.HOROR, Genre.SCI_FI, Genre.DRAMA, Genre.KOMEDI, Genre.FANTASI
)

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
fun V2IdeaScreen(v2: V2ViewModel) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) v2.addImage(uri)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cerita Baru — v2.1")
            Spacer(Modifier.height(4.dp))
            Text("Tulis ide singkat, pilih tokoh dari DNA, tambah gambar referensi kalau ada (opsional, maks 3), lalu Kembangkan Cerita.")
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
            Text("Ide cepat — tap untuk isi:")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QUICK_IDEAS) { quick ->
                    ChoiceChip(quick, selected = false, onClick = { v2.updateIdea(quick) })
                }
            }
        }
        item {
            Text("Tokoh dari DNA — tap 1 = Hero, tap ke-2 = Antagonis:")
        }
        if (v2.dnaCharacters.isEmpty()) {
            item {
                Text("Belum ada tokoh — deskripsikan gambar referensi di bawah lalu Simpan ke DNA.")
            }
        }
        items(v2.dnaCharacters) { char ->
            val selectedIndex = v2.selectedDnaIds.indexOf(char.id)
            val role = when (selectedIndex) {
                0 -> " — Hero"
                1 -> " — Antagonis"
                else -> ""
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${char.name}$role")
                    Text(char.descriptionEn)
                }
                TextButton(onClick = { v2.toggleDnaSelection(char.id) }) {
                    Text(if (selectedIndex >= 0) "Batal" else "Pakai")
                }
                TextButton(onClick = { v2.deleteDnaCharacter(char.id) }) { Text("Hapus") }
            }
        }
        item {
            Text("Gambar referensi (dipakai tokoh DNA + upload manual, maks 3):")
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
                val bitmap = androidx.compose.runtime.remember(img.base64) {
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
                        TextButton(onClick = { v2.openDnaDialog(index) }) { Text("Simpan ke DNA") }
                    }
                }
                TextButton(onClick = { v2.removeImage(index) }) { Text("Hapus") }
            }
        }
        item {
            Text("Durasi:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10).forEach { minutes ->
                    ChoiceChip(
                        "$minutes mnt",
                        selected = v2.durationMinutes == minutes,
                        onClick = { v2.updateDuration(minutes) }
                    )
                }
            }
            Text("1 menit = 6 scene @10 detik. Film 3 menit = 18 scene.")
        }
        item {
            Text("Genre (otomatis = sutradara yang memilih):")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ChoiceChip("Otomatis", selected = v2.genreHint == null, onClick = { v2.updateGenreHint(null) })
                }
                items(GENRE_CHOICES) { genre ->
                    ChoiceChip(genre.labelId, selected = v2.genreHint == genre, onClick = { v2.updateGenreHint(genre) })
                }
            }
        }
        item {
            Text("Rasio aspek:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("9:16 — Shorts / TikTok", selected = v2.aspectRatio == "9:16", onClick = { v2.updateAspectRatio("9:16") })
                ChoiceChip("16:9 — YouTube", selected = v2.aspectRatio == "16:9", onClick = { v2.updateAspectRatio("16:9") })
            }
        }
        item {
            Text("Bahasa dialog:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("Indonesia", selected = v2.dialogueLang == "ID", onClick = { v2.updateDialogueLang("ID") })
                ChoiceChip("English", selected = v2.dialogueLang == "EN", onClick = { v2.updateDialogueLang("EN") })
            }
        }
        // v2.2 — engine tujuan: prompter menulis khusus untuk engine ini
        // (satu profil panjang + aturan audio), bukan generik 4 engine.
        item {
            Text("Engine tujuan (prompter menulis khusus untuk engine ini):")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetEngine.values().forEach { engine ->
                    ChoiceChip(engine.name, selected = v2.targetEngine == engine, onClick = { v2.updateTargetEngine(engine) })
                }
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
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { v2.updateStep(V2Step.MANUAL) }) { Text("Mode Manual (mesin v1) ›") }
        }
    }
    if (v2.dnaDialogIndex != null) {
        AlertDialog(
            onDismissRequest = { v2.dismissDnaDialog() },
            title = { Text("Simpan Tokoh ke DNA") },
            text = {
                Column {
                    Text("Gambar ini sudah dideskripsikan Vision. Simpan sekali — pakai di semua film berikutnya.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = v2.dnaDialogName,
                        onValueChange = { v2.updateDnaDialogName(it) },
                        label = { Text("Nama tokoh") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { v2.saveImageToDna() }) { Text("Simpan ke DNA") }
            },
            dismissButton = {
                TextButton(onClick = { v2.dismissDnaDialog() }) { Text("Lewati") }
            }
        )
    }
}

private fun quotaRemaining(v2: V2ViewModel, providerName: String): Int {
    val provider = com.daftech.promptku.engine.V2Provider.values()
        .firstOrNull { it.name == providerName } ?: return 0
    return com.daftech.promptku.engine.QuotaTracker.remaining(provider, v2.quotaCounts)
}
