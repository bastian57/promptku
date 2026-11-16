package com.daftech.promptku.engine

// Ekspor naskah format Fountain (John August & Stu Maschwitz, 2012) —
// plain-text, sintaks 1:1 dari data naskah (slugline caps, NAMA caps +
// dialog, transisi "> CUT TO:"). Dibuka Highland / Beat / Slugline /
// Fade In / WriterDuet. Biaya implementasi hampir nol: murni formatting.
// buildScriptText = versi .txt PromptKu (naskah flat + prompt +
// Global Lock), mengikuti pola ExportText v1.0/v1.1.
// v1.3: naskah FLAT (1 scene = 1 klip) — tidak ada lagi shot list.
object FountainExport {

    fun buildFountain(draft: ScriptDraft): String {
        val input = draft.input
        val out = StringBuilder()
        out.append("Title: ${input.title.ifBlank { "Tanpa Judul" }}\n")
        out.append("Credit: Written by\n")
        out.append("Author: PromptKu\n")
        out.append("Draft date: 2026-11-15\n")
        out.append("Contact: Mode Naskah PromptKu v1.4 (rule-based draft)\n")
        out.append("\n")
        out.append("FADE IN:\n\n")
        draft.scenes.forEach { scene ->
            out.append("${scene.slugline}\n\n")
            out.append("${scene.actionEn}\n\n")
            val line = scene.dialogue
            if (line != null && !line.isEmpty()) {
                val text = line.textFor(input.dialogueLang)
                if (text.isNotBlank()) {
                    out.append("${line.speaker.uppercase()}\n")
                    out.append("$text\n\n")
                }
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
        promptsByScene: List<Pair<DraftScene, GeneratedPrompt>>,
        lock: GlobalLock = GlobalLock()
    ): String {
        val input = draft.input
        val out = StringBuilder()
        out.append("PROMPTKU - ${input.title.ifBlank { "Tanpa Judul" }}\n")
        out.append("Dibuat dengan PromptKu v1.4.2 (Mode Naskah, offline, rule-based draft)\n")
        out.append("Genre: ${input.genre.labelId} | Durasi: ${input.durationMinutes} menit | ")
        out.append("Total klip: ${draft.totalClips} | ${draft.estimatedCostId}\n")
        if (input.synopsis.isNotBlank()) {
            out.append("Sinopsis: ${input.synopsis}\n")
        }
        if (!lock.isEmpty()) {
            out.append("GLOBAL LOCK (berlaku untuk semua scene):\n")
            lock.world?.let { out.append("- Dunia: ${it.name} — ${it.descriptionEn}\n") }
            lock.light?.let { out.append("- Cahaya: ${it.labelId}\n") }
            lock.style?.let { out.append("- Gaya: ${it.labelId}\n") }
            lock.lensMm?.let { out.append("- Lensa: ${it}mm\n") }
            lock.character?.let { out.append("- Tokoh: ${it.name} — ${it.descriptionEn}\n") }
            lock.secondCharacter?.let { out.append("- Tokoh 2: ${it.name} — ${it.descriptionEn}\n") }
            lock.ambience?.let { out.append("- Ambience: $it\n") }
        }
        out.append("========================================\n\n")
        draft.scenes.forEach { scene ->
            out.append("SCENE ${scene.sceneNumber} - ${scene.beatName} [${scene.timecode()}]\n")
            out.append("${scene.slugline}\n")
            out.append("Want: ${scene.want}\n")
            out.append("Obstacle: ${scene.obstacle}\n")
            out.append("Aksi: ${scene.actionEn}\n")
            val line = scene.dialogue
            if (line != null && !line.isEmpty()) {
                out.append("Dialog (${line.speaker})")
                if (line.textEn.isNotBlank()) out.append(" EN: \"${line.textEn}\"")
                if (line.textId.isNotBlank()) out.append(" ID: \"${line.textId}\"")
                out.append("\n")
                if (line.tone.isNotBlank()) out.append("Nada: ${line.tone}\n")
                if (line.tactic.isNotBlank()) out.append("Taktik: ${line.tactic}\n")
            }
            out.append("Pose: ${scene.poseStart} -> ${scene.poseEnd}\n")
            out.append("Arah layar: ${scene.screenDirection} | Sisi axis: ${scene.axisSide}\n")
            out.append("Kenapa shot ini: ${scene.directorNote}\n")
            if (scene.reAnchor) out.append("[RE-ANCHOR ke character sheet]\n")
            out.append("----------------------------------------\n")
            val prompt = promptsByScene.firstOrNull { it.first.orderIndex == scene.orderIndex }?.second
            if (prompt != null) {
                out.append("Engine: ${prompt.engine.labelId} (${prompt.wordCount} kata)\n")
                if (prompt.meter.isNotBlank()) out.append("Meter: ${prompt.meter}\n")
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
                // Lembar Suara WanGP (v1.3): WanGP bisu — dialog, ambience,
                // dan SFX scene ini untuk TTS/dubbing di CapCut, bukan prompt.
                if (prompt.engine == TargetEngine.WAN) {
                    out.append("LEMBAR SUARA (WanGP bisu — untuk TTS/dubbing di CapCut):\n")
                    if (line != null && !line.isEmpty()) {
                        out.append("- Dialog ${line.speaker}: \"${line.textFor(input.dialogueLang)}\"\n")
                    }
                    out.append("- Ambience: ${lock.ambience ?: "wind, quiet ambience"}\n")
                    out.append("- SFX: metal tools clinking, distant traffic\n\n")
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
