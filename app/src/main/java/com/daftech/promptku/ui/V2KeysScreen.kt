package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.QuotaTracker
import com.daftech.promptku.engine.V2Provider

// Layar Kunci v2.0 — BYOK (Gemini + Groq) + kuota + peringatan privasi.
// Pola UI mengikuti SettingsScreen UnduhKu (kunci diisi user di app,
// TIDAK PERNAH hardcode).
@Composable
fun V2KeysScreen(v2: V2ViewModel) {
    var gemini by remember { mutableStateOf("") }
    var groq by remember { mutableStateOf("") }
    // v2.0 fix3 — isi awal = model tersimpan (sebelumnya selalu default,
    // jadi editan lama tidak pernah kelihatan lagi saat layar dibuka).
    var geminiModel by remember { mutableStateOf(v2.storedGeminiModel) }
    var groqModel by remember { mutableStateOf(v2.storedGroqModel) }
    Column(
        // v2.0 fix3 — layar HARUS bisa di-scroll: sebelumnya Column polos,
        // konten lebih tinggi dari layar, pesan "Kunci disimpan" jatuh di
        // bawah lipatan dan tidak pernah terlihat (laporan lapangan 16 Nov).
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kunci API (BYOK)")
        Text(
            "PromptKu memakai KUNCI MILIKMU SENDIRI — tidak ada kunci bersama, " +
                "tidak ada proxy. Kunci disimpan terenkripsi di HP ini " +
                "(${if (v2.keysEncrypted) "terenkripsi" else "PERINGATAN: tidak terenkripsi, KeyStore bermasalah"})."
        )
        Text(
            "PRIVASI: foto yang dikirim ke Gemini free tier bisa dipakai Google " +
                "untuk improve produk dan dibaca human reviewer — jangan kirim foto " +
                "sensitif. Satu-satunya jalur fully private = on-device."
        )
        OutlinedTextField(
            value = gemini,
            onValueChange = { gemini = it },
            label = { Text("Kunci Gemini (AI Studio, gratis)") },
            placeholder = { Text(if (v2.hasGeminiKey) "•••• terisi" else "AIza...") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = groq,
            onValueChange = { groq = it },
            label = { Text("Kunci Groq (gratis, console.groq.com)") },
            placeholder = { Text(if (v2.hasGroqKey) "•••• terisi" else "gsk_...") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = geminiModel,
            onValueChange = { geminiModel = it },
            label = { Text("Model Gemini Director") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = groqModel,
            onValueChange = { groqModel = it },
            label = { Text("Model Groq Director") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                v2.updateKeys(gemini, groq)
                // v2.0 fix3 — model ikut disimpan (sebelumnya field model
                // tidak pernah ditulis ke mana pun).
                v2.updateModels(geminiModel, groqModel)
                // Kosongkan field kunci: placeholder "•••• terisi" muncul =
                // bukti visual kunci tersimpan.
                gemini = ""
                groq = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan Kunci") }
        // v2.0 fix3 — pesan status TEPAT di bawah tombol (sebelumnya di
        // bawah hint, di luar layar): tap Simpan langsung kelihatan.
        if (v2.statusMessage.isNotBlank()) {
            Text(v2.statusMessage)
        }
        Text("Kosongkan field = biarkan kunci lama (nilai kunci tidak pernah ditampilkan).")
        Spacer(Modifier.height(8.dp))
        Text("Kuota hari ini (sisa):")
        V2Provider.values().forEach { provider ->
            Text("${provider.label}: ${QuotaTracker.remaining(provider, v2.quotaCounts)}/${provider.dailyLimit}")
        }
        Text("Perkiraan film penuh hari ini: ${v2.filmsEstimate} (7 panggilan/film).")
        OutlinedButton(onClick = { v2.refreshQuota() }) { Text("Perbarui Kuota") }
        OutlinedButton(onClick = { v2.updateStep(V2Step.IDEA) }) { Text("Kembali ke Ide") }
    }
}
