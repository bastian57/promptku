package com.daftech.promptku.engine

// 6 Gerbang Validasi v2.0 — dicek otomatis SEBELUM export, dari bible +
// scene yang sudah dirender. Gerbang 6 prosedural (coba ulang 1x lalu
// tandai) — di sini diwakili sebagai "semua kegagalan sudah ditandai".

data class GateResult(
    val id: String,
    val nameId: String,
    val passed: Boolean,
    val failures: List<String>
)

object ValidationGates {

    private val FIGHT_VERBS = listOf(
        "punch", "punches", "kick", "kicks", "grapple", "grapples",
        "slam", "slams", "uppercut", "strike", "strikes", "hit", "hits",
        "fight", "fights", "battle", "battles", "smash", "smashes"
    )

    /** Tanda tangan dunia: 5 kata pertama deskripsi dunia (lowercase). */
    fun worldSignature(worldDescEn: String): String =
        worldDescEn.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.take(5).joinToString(" ")

    private fun norm(text: String): String =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.joinToString(" ")

    private fun tokensOf(text: String): Set<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        val union = a union b
        if (union.isEmpty()) return 0.0
        return (a intersect b).size.toDouble() / union.size
    }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    fun runGates(
        bible: StoryBible,
        scenes: List<V2ScenePrompt>,
        hasImages: Boolean
    ): List<GateResult> {
        val results = mutableListOf<GateResult>()

        // G1 — Dunia hadir verbatim (Mode A) / konsisten (Mode B).
        val sig = worldSignature(bible.worldDescEn)
        val g1Failures = scenes.filter { scene ->
            val text = scene.flowText.lowercase()
            if (hasImages) {
                val worldFirst = bible.worldName.lowercase().split(Regex("\\s+")).firstOrNull().orEmpty()
                worldFirst.isBlank() || !text.contains(worldFirst)
            } else {
                sig.isBlank() || !text.contains(sig)
            }
        }.map { "Scene ${it.sceneNumber}: dunia tidak verbatim" }
        results.add(GateResult("G1", "Dunia verbatim di tiap scene", g1Failures.isEmpty(), g1Failures))

        // G2 — NOL aksi berulang se-film; finisher maks 1x di klimaks;
        // Penutup = aftermath (tanpa aksi berantem).
        val g2Failures = mutableListOf<String>()
        val seen = mutableMapOf<String, Int>()
        for (scene in scenes) {
            val key = norm(scene.plan.actionEn)
            val prev = seen.put(key, scene.sceneNumber)
            if (prev != null) {
                g2Failures.add("Scene ${scene.sceneNumber}: aksi sama dengan scene $prev")
            }
        }
        // v2.1 fix6 — duplikat NYARIS sama (S9 = aksi S8 + klausa ekor) lolos
        // dari banding string persis: tambah Jaccard token >= 0.5.
        // Pasangan yang SUDAH dilaporkan persis (norm sama) jangan digandakan;
        // tapi sim = 1.0 dengan norm BERBEDA (klausa ekor mengulang kata
        // yang sama) tetap harus ketangkap.
        val normalized = scenes.map { it.sceneNumber to norm(it.plan.actionEn) }
        val actionTokens = scenes.map { it.sceneNumber to tokensOf(it.plan.actionEn) }
        for (i in actionTokens.indices) {
            for (j in i + 1 until actionTokens.size) {
                if (normalized[i].second == normalized[j].second) continue
                val (numI, setI) = actionTokens[i]
                val (numJ, setJ) = actionTokens[j]
                val sim = jaccard(setI, setJ)
                if (sim >= 0.5) {
                    g2Failures.add(
                        "Scene $numJ: aksi nyaris sama dengan scene $numI " +
                            "(kemiripan ${"%.0f".format(sim * 100)}%)"
                    )
                }
            }
        }
        val finishers = scenes.filter {
            it.plan.effectId == "finisher" || norm(it.plan.actionEn).contains("finisher")
        }
        if (finishers.size > 1) {
            g2Failures.add("Finisher muncul ${finishers.size}x (maks 1x di klimaks)")
        }
        val last = scenes.maxByOrNull { it.sceneNumber }
        if (last != null) {
            val lastAction = norm(last.plan.actionEn)
            val verb = FIGHT_VERBS.firstOrNull { lastAction.split(" ").contains(it) }
            if (verb != null) {
                g2Failures.add("Scene ${last.sceneNumber} (Penutup): masih ada aksi \"$verb\" setelah klimaks")
            }
        }
        results.add(GateResult("G2", "Aksi unik se-film, finisher 1x, Penutup aftermath", g2Failures.isEmpty(), g2Failures))

        // G3 — Kamera + lensa efek yang dipilih prompter muncul di teks.
        // (S7 vs S14 Antartika: efek sama tapi kamera/lensa beda.)
        val g3Failures = scenes.mapNotNull { scene ->
            val effect = EffectLibrary.ALL.firstOrNull { it.id == scene.plan.effectId }
                ?: return@mapNotNull null
            val text = scene.flowText.lowercase()
            val missing = mutableListOf<String>()
            effect.lensMm?.let { lens ->
                if (!text.contains("${lens}mm")) missing.add("lensa ${lens}mm")
            }
            effect.cameraMove?.let { move ->
                val firstWord = move.phraseEn.split(" ").firstOrNull().orEmpty()
                if (firstWord.isNotBlank() && !text.contains(firstWord)) missing.add("kamera ${move.phraseEn}")
            }
            if (missing.isEmpty()) null
            else "Scene ${scene.sceneNumber}: efek ${effect.id} tanpa ${missing.joinToString(", ")}"
        }
        results.add(GateResult("G3", "Kamera + lensa efek muncul di prompt", g3Failures.isEmpty(), g3Failures))

        // G4 — Mode B: NOL deskripsi subjek penuh; WAJIB penunjuk gambar.
        if (hasImages) {
            val g4Failures = mutableListOf<String>()
            val hero = bible.hero
            for (scene in scenes) {
                val text = scene.flowText.lowercase()
                if (!text.contains("reference image")) {
                    g4Failures.add("Scene ${scene.sceneNumber}: tidak menunjuk gambar referensi")
                }
                hero?.anchorsEn?.forEach { anchor ->
                    if (anchor.length >= 4 && text.contains(anchor.lowercase())) {
                        g4Failures.add("Scene ${scene.sceneNumber}: deskripsi subjek penuh (\"$anchor\") bertabrakan dengan gambar")
                    }
                    // v2.1 fix6 — parafrasa ("massive feathered wings" vs anchor
                    // "large golden feathered wings") lolos dari cocok frasa
                    // persis: cek tumpang tindih token deskriptif di actionEn
                    // SAJA — "golden" di cahaya akan false positive kalau
                    // dicek di seluruh teks.
                    val anchorTokens = anchor.lowercase().split(Regex("[^a-z0-9]+"))
                        .filter { it.length >= 4 }.toSet()
                    if (anchorTokens.size >= 2) {
                        val actTokens = tokensOf(scene.plan.actionEn)
                        val shared = anchorTokens intersect actTokens
                        if (shared.size >= 2) {
                            g4Failures.add(
                                "Scene ${scene.sceneNumber}: deskripsi subjek parafrasa " +
                                    "(${shared.joinToString(", ")}) bertabrakan dengan gambar"
                            )
                        }
                    }
                }
            }
            results.add(GateResult("G4", "Mode referensi: tunjuk gambar, jangan deskripsikan ulang", g4Failures.isEmpty(), g4Failures))
        } else {
            results.add(GateResult("G4", "Mode referensi: tunjuk gambar, jangan deskripsikan ulang", true, emptyList()))
        }

        // G5 — Dialog: 1 pembicara per klip, ≤8 kata.
        val g5Failures = scenes.mapNotNull { scene ->
            val idWords = wordCount(scene.plan.dialogueId)
            val enWords = wordCount(scene.plan.dialogueEn)
            when {
                idWords > 8 -> "Scene ${scene.sceneNumber}: dialog ID $idWords kata (maks 8)"
                enWords > 8 -> "Scene ${scene.sceneNumber}: dialog EN $enWords kata (maks 8)"
                else -> null
            }
        }
        results.add(GateResult("G5", "Dialog ≤8 kata, 1 pembicara per klip", g5Failures.isEmpty(), g5Failures))

        // G6 — Semua kegagalan sudah ditandai "perlu cek manual".
        val failedScenes = mutableSetOf<Int>()
        for (result in results) {
            for (failure in result.failures) {
                Regex("Scene (\\d+)").find(failure)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull()?.let { failedScenes.add(it) }
            }
        }
        val unflagged = failedScenes.filter { num ->
            scenes.firstOrNull { it.sceneNumber == num }?.needsReview != true
        }
        results.add(
            GateResult(
                "G6",
                "Kegagalan ditandai perlu cek manual (bukan lolos diam-diam)",
                unflagged.isEmpty(),
                unflagged.map { "Scene $it: gagal gerbang tapi tidak ditandai" }
            )
        )
        return results
    }
}
