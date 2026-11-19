package com.daftech.promptku.engine

// Perakit teks ekspor proyek — pure Kotlin, tanpa dependensi Android.
// File .txt ditulis ke MediaStore.Downloads oleh ExportHelper (data layer).
object ExportText {

    fun buildProjectText(
        projectTitle: String,
        scenes: List<Pair<String, GeneratedPrompt>>,
        lock: GlobalLock = GlobalLock()
    ): String {
        val out = StringBuilder()
        out.append("PROMPTKU - $projectTitle\n")
        out.append("Dibuat dengan PromptKu v1.4.4 (offline, struktur v3 — 12 slot)\n")
        out.append("Total scene: ${scenes.size}\n")
        if (!lock.isEmpty()) {
            out.append("GLOBAL LOCK (berlaku untuk semua scene):\n")
            lock.world?.let { out.append("- Dunia: ${it.name} — ${it.descriptionEn}\n") }
            lock.light?.let { out.append("- Cahaya: ${it.labelId}\n") }
            lock.style?.let { out.append("- Gaya: ${it.labelId}\n") }
            lock.lensMm?.let { out.append("- Lensa: ${it}mm\n") }
            lock.character?.let { out.append("- Tokoh: ${it.name} — ${it.descriptionEn}\n") }
            lock.secondCharacter?.let { out.append("- Tokoh 2: ${it.name} — ${it.descriptionEn}\n") }
        }
        out.append("========================================\n\n")
        scenes.forEachIndexed { index, pair ->
            val title = pair.first
            val prompt = pair.second
            out.append("SCENE ${index + 1} - $title\n")
            out.append("Engine: ${prompt.engine.labelId}\n")
            out.append("----------------------------------------\n")
            out.append(prompt.text)
            out.append("\n\n")
            if (prompt.negativePrompt != null) {
                out.append("Negative prompt (${prompt.engine.labelId}):\n")
                out.append(prompt.negativePrompt)
                out.append("\n\n")
            }
            if (prompt.warnings.isNotEmpty()) {
                out.append("Catatan:\n")
                prompt.warnings.forEach { warning -> out.append("- $warning\n") }
                out.append("\n")
            }
            out.append("========================================\n\n")
        }
        out.append("Tips: tempel satu prompt per generate di engine tujuan.\n")
        out.append("Flow (Omni Flash 1.2): 10 detik per klip, Ingredients max 3 gambar. Veo 3.1 Lite (fallback): maks 8 detik, 720p.\n")
        out.append("WanGP TI2V-5B: 121 frame, 1280x704, first-frame mengunci subjek.\n")
        return out.toString()
    }
}
