package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.EffectCategory
import com.daftech.promptku.engine.EffectLibrary

// Tab Perpustakaan Efek v1.4.2 — browse semua resep efek sutradara.
// Read-only: efek dipilih otomatis dari trigger kalimat Mode Cepat, atau
// diganti lewat chip efek di tiap scene (tab Naskah).
// Nomor beat mengikuti kerangka 8 Sequence (0 Pembuka ... 7 Dunia Baru).
@Composable
fun EffectsScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Perpustakaan Efek", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Resep efek sutradara: tiap efek punya struktur pemicu → hantaman → akibat. Satu klip = satu efek. Efek terdeteksi otomatis dari kalimat Mode Cepat, atau diganti lewat chip efek di tiap scene (tab Naskah).",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        EffectCategory.values().forEach { category ->
            Text(text = category.labelId, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            EffectLibrary.ALL.filter { it.category == category }.forEach { effect ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = effect.nameId, style = MaterialTheme.typography.titleSmall)
                        if (effect.fragmentEn.isNotBlank()) {
                            Text(text = effect.fragmentEn, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                text = "Hanya mengganti cara shot diambil (kamera/lensa/sudut) — aksinya tetap dari naskah.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        val extras = buildList {
                            effect.cameraMove?.let { add("Kamera: ${it.labelId}") }
                            effect.lensMm?.let { add("Lensa: ${it}mm") }
                            effect.lightKind?.let { add("Cahaya: ${it.labelId}") }
                            effect.angle?.let { add("Sudut: ${it.labelId}") }
                            add("Beat ${effect.beats.first}–${effect.beats.last}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = extras.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
