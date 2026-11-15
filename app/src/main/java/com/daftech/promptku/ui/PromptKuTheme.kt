package com.daftech.promptku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema PromptKu — teal (kunci) + oranye (grade hangat), kontras tinggi
// supaya chip dan tombol kebaca tanpa trik. Semua tombol utama terlihat
// langsung di layar, tidak ada yang disembunyikan (pelajaran SuaraKu v1.6.0).
private val PromptKuDark = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF004D47),
    secondary = Color(0xFFFF8A65),
    onSecondary = Color(0xFF3D0C00),
    tertiary = Color(0xFFFFCC80),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

private val PromptKuLight = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFE64A19),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun PromptKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) PromptKuDark else PromptKuLight
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
