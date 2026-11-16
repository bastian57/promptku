package com.daftech.promptku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema PromptKu v2.1 — Dark Cinematic + Neon Purple (dari mockup yang
// disetujui Putra 16 Nov). Selalu gelap, bukan ikut sistem: latar nyaris
// hitam ungu, kartu ungu gelap, aksen ungu neon di tombol + chip aktif.
private val PromptKuDark = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7C3AED),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF1A0B2E),
    tertiary = Color(0xFFE9D5FF),
    background = Color(0xFF0D0B14),
    surface = Color(0xFF1B1826),
    onBackground = Color(0xFFF2F0FA),
    onSurface = Color(0xFFF2F0FA),
    surfaceVariant = Color(0xFF241F33),
    onSurfaceVariant = Color(0xFF9A94B0),
    outline = Color(0xFF2C2740)
)

@Composable
fun PromptKuTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PromptKuDark,
        content = content
    )
}
