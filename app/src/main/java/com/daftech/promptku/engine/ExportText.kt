package com.daftech.promptku.engine

// Perakit teks ekspor proyek — pure Kotlin, tanpa dependensi Android.
// File .txt ditulis ke MediaStore.Downloads oleh ExportHelper (data layer).
object ExportText {

    fun buildProjectText(
        projectTitle: String,
        scenes: List<Pair<String, GeneratedPrompt>>
    ): String {
        val out = StringBuilder()
        out.append("PROMPTKU - $projectTitle\n")
        out.append("Dibuat dengan PromptKu v1.0.0 (offline, struktur 9 lapis v2)\n")
        out.append("Total scene: ${scenes.size}\n")
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
        out.append("Flow/Veo: 10 detik per klip, Ingredients max 3 gambar.\n")
        out.append("WanGP TI2V-5B: 121 frame, 1280x704, first-frame mengunci subjek.\n")
        return out.toString()
    }
}
