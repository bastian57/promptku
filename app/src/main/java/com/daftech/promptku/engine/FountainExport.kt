package com.daftech.promptku.engine

// Ekspor naskah format Fountain (John August & Stu Maschwitz, 2012) —
// plain-text, sintaks 1:1 dari data naskah (slugline caps, NAMA caps +
// dialog, transisi "> CUT TO:"). Dibuka Highland / Beat / Slugline /
// Fade In / WriterDuet. Biaya implementasi hampir nol: murni formatting.
// buildScriptText = versi .txt PromptKu (naskah + shot list + prompt +
// Global Lock), mengikuti pola ExportText v1.0/v1.1.
object FountainExport {

    fun buildFountain(draft: ScriptDraft): String {
        val input = draft.input
        val out = StringBuilder()
        out.append("Title: ${input.title.ifBlank { "Tanpa Judul" }}\n")
        out.append("Credit: Written by\n")
        out.append("Author: PromptKu\n")
        out.append("Draft date: 2026-11-15\n")
        out.append("Contact: Mode Naskah PromptKu v1.2 (rule-based draft)\n")
        out.append("\n")
        out.append("FADE IN:\n\n")
        draft.scenes.forEach { scene ->
            out.append("${scene.slugline}\n\n")
            out.append("${scene.actionEn}\n\n")
            if (scene.dialogueLine.isNotBlank()) {
                out.append("${scene.dialogueName.uppercase()}\n")
                out.append("${scene.dialogueLine}\n\n")
            }
            val nextScene = draft.scenes.getOrNull(scene.orderIndex + 1)
            if (nextScene != null) {
                out.append("> CUT TO:\n\n")
            }
        }
        out.append("FADE OUT.\n")
        return out.toString()
    }

    fun buildScriptText(
        draft: ScriptDraft,
        promptsByShot: List<Pair<DraftShot, GeneratedPrompt>>,
        lock: GlobalLock = GlobalLock()
    ): String {
        val input = draft.input
        val out = StringBuilder()
        out.append("PROMPTKU - ${input.title.ifBlank { "Tanpa Judul" }}\n")
        out.append("Dibuat dengan PromptKu v1.2.0 (Mode Naskah, offline, rule-based draft)\n")
        out.append("Genre: ${input.genre.labelId} | Durasi: ${input.durationMinutes} menit | ")
        out.append("Total klip: ${draft.totalClips} | ${draft.estimatedCostId}\n")
        if (input.synopsis.isNotBlank()) {
            out.append("Sinopsis: ${input.synopsis}\n")
        }
        if (!lock.isEmpty()) {
            out.append("GLOBAL LOCK (berlaku untuk semua shot):\n")
            lock.world?.let { out.append("- Dunia: ${it.name} — ${it.descriptionEn}\n") }
            lock.light?.let { out.append("- Cahaya: ${it.labelId}\n") }
            lock.style?.let { out.append("- Gaya: ${it.labelId}\n") }
            lock.lensMm?.let { out.append("- Lensa: ${it}mm\n") }
            lock.character?.let { out.append("- Tokoh: ${it.name} — ${it.descriptionEn}\n") }
            lock.secondCharacter?.let { out.append("- Tokoh 2: ${it.name} — ${it.descriptionEn}\n") }
        }
        out.append("========================================\n\n")
        draft.scenes.forEach { scene ->
            out.append("SCENE ${scene.orderIndex + 1} - ${scene.beatName}\n")
            out.append("${scene.slugline}\n")
            out.append("Want: ${scene.want}\n")
            out.append("Obstacle: ${scene.obstacle}\n")
            out.append("Aksi: ${scene.actionEn}\n")
            if (scene.dialogueLine.isNotBlank()) {
                out.append("Dialog (${scene.dialogueName}): ${scene.dialogueLine}\n")
            }
            out.append("Pose: ${scene.poseStart} -> ${scene.poseEnd}\n")
            out.append("Arah layar: ${scene.screenDirection} | Sisi axis: ${scene.axisSide}\n")
            out.append("----------------------------------------\n")
            val sceneShots = promptsByShot.filter { it.first.sceneOrder == scene.orderIndex }
            sceneShots.forEach { (shot, prompt) ->
                out.append("SHOT ${shot.orderIndex + 1} [${shot.priority.labelId}, ${shot.shotFunction.labelId}]")
                if (shot.reAnchor) out.append(" [RE-ANCHOR ke character sheet]")
                out.append("\n")
                out.append("Engine: ${prompt.engine.labelId} (${prompt.wordCount} kata)\n")
                out.append(prompt.text)
                out.append("\n\n")
                if (prompt.negativePrompt != null) {
                    out.append("Negative prompt (${prompt.engine.labelId}):\n")
                    out.append(prompt.negativePrompt)
                    out.append("\n\n")
                }
            }
            out.append("========================================\n\n")
        }
        out.append("Catatan: naskah ini draft terstruktur (rule-based), bukan tulisan AI — ")
        out.append("poles dialognya sebelum generate. Tempel satu prompt per generate di engine tujuan.\n")
        out.append("Flow/Veo: 10 detik per klip, Ingredients max 3 gambar.\n")
        out.append("WanGP TI2V-5B: 121 frame, 1280x704, first-frame mengunci subjek.\n")
        return out.toString()
    }
}
