package com.daftech.promptku.engine

// Prompt Score v2 — 10 poin checklist dari riset 15 Nov 2026.
// Skor dihitung dari INPUT wizard (bukan dari teks hasil), supaya user tahu
// baris mana yang kurang SEBELUM generate — bukan sesudah videonya gagal.
object PromptScore {

    fun score(input: ShotInput): ScoreResult {
        val items = listOf(
            ScoreItem(
                id = "shot_lens",
                labelId = "Shot + lensa disebut",
                passed = input.shotSize != null && input.lensMm != null,
                missingHint = "Pilih ukuran shot dan lensa (misal Medium, 35mm)."
            ),
            ScoreItem(
                id = "one_action_camera",
                labelId = "1 aksi + 1 gerakan kamera",
                passed = input.actionEn.isNotBlank() && input.cameraMove != null,
                missingHint = "Tulis SATU aksi dan pilih SATU gerakan kamera."
            ),
            ScoreItem(
                id = "pose_start_end",
                labelId = "Pose awal ke pose akhir",
                passed = input.poseStart.isNotBlank() && input.poseEnd.isNotBlank(),
                missingHint = "Isi pose awal dan pose akhir (misal: berdiri diam -> berdiri tegak)."
            ),
            ScoreItem(
                id = "named_light",
                labelId = "Cahaya bernama (sumber + posisi)",
                passed = input.light != null,
                missingHint = "Pilih cahaya (misal Golden Hour, bukan sekadar terang)."
            ),
            ScoreItem(
                id = "grade_lock",
                labelId = "Grade lock konsisten",
                passed = input.style != null,
                missingHint = "Pilih gaya visual — grade lock diambil dari gaya ini."
            ),
            ScoreItem(
                id = "face_or_hands",
                labelId = "Wajah ATAU tangan (bukan keduanya)",
                passed = input.focusMode != null,
                missingHint = "Pilih fokus: wajah saja, tangan saja, atau keduanya stabil."
            ),
            ScoreItem(
                id = "negative_lock",
                labelId = "Negative lock sesuai engine",
                passed = input.engine != null,
                missingHint = "Pilih engine tujuan — negative lock beda tiap engine."
            ),
            ScoreItem(
                id = "hide_weakness",
                labelId = "Taktik hide-the-weakness",
                passed = input.hideTactic != HideTactic.NONE,
                missingHint = "Pilih 1 taktik (back shot, siluet, asap, jarak jauh) untuk shot berisiko."
            ),
            ScoreItem(
                id = "dna_verbatim",
                labelId = "DNA karakter verbatim",
                passed = input.character != null && input.character.descriptionEn.isNotBlank(),
                missingHint = "Pilih atau tulis DNA karakter (deskripsi Inggris lengkap)."
            ),
            ScoreItem(
                id = "audio",
                labelId = "Audio SFX / ambient",
                passed = input.audioEn.isNotBlank(),
                missingHint = "Tulis SFX + ambient (misal: wind, distant crowd murmur)."
            )
        )
        return ScoreResult(items, items.count { it.passed })
    }

    // Prompt Score v3 (v1.2) — 8 aturan multi-scene, dipakai Mode Naskah
    // untuk menilai tiap shot DENGAN konteks shot sebelumnya (prev).
    // Tanpa prev, aturan perbandingan lolos (tidak ada yang dibandingkan).
    // sameScene = true bila prev shot masih satu scene dengan input:
    // shot-shot dalam satu scene MEMANG coverage dari SATU aksi yang sama,
    // jadi aturan "aksi tidak mengulang" tidak berlaku di situ — aturan
    // itu hanya untuk aksi yang sama di scene BERBEDA.
    fun scoreV3(input: ShotInput, prev: ShotInput? = null, sameScene: Boolean = false): ScoreResult {
        val dialogueWords = input.dialogueLine.split(Regex("\\s+"))
            .filter { it.isNotBlank() }.size
        val fastActionWords = listOf(
            "runs", "sprints", "charges", "leaps", "dodges", "crashes",
            "flees", "races", "dashes"
        )
        val fastCamera = setOf(
            CameraMove.TRACKING, CameraMove.HANDHELD,
            CameraMove.ORBIT, CameraMove.AERIAL
        )
        val actionLower = input.actionEn.lowercase()
        val wanEstimate = if (input.engine == TargetEngine.WAN) {
            MythFilter.countWords(
                listOf(
                    input.actionEn, input.materialEn, input.audioEn,
                    input.dialogueLine,
                    input.character?.descriptionEn ?: "",
                    input.world?.descriptionEn ?: ""
                ).joinToString(" ")
            )
        } else {
            0
        }
        val items = listOf(
            ScoreItem(
                id = "dialogue_speed",
                labelId = "Dialog <=2,5 kata/detik klip",
                passed = dialogueWords <= 25,
                missingHint = "Dialog maksimal 25 kata per klip 10 detik — lebih dari itu lip-sync dan pacing pecah."
            ),
            ScoreItem(
                id = "single_action",
                labelId = "Satu aksi, bukan dua",
                passed = !actionLower.contains(" and then ") &&
                    !actionLower.contains(", then ") &&
                    !actionLower.contains(" while "),
                missingHint = "Aksi mengandung dua langkah ('and then' / 'while') — pecah jadi dua shot."
            ),
            ScoreItem(
                id = "fast_action_camera",
                labelId = "Aksi cepat + kamera cepat tidak bersamaan",
                passed = !(fastActionWords.any { actionLower.contains(it) } &&
                    input.cameraMove in fastCamera),
                missingHint = "Aksi cepat (lari/lompat) + kamera cepat (tracking/handheld) = motion blur dan gagal render."
            ),
            ScoreItem(
                id = "shot_size_varies",
                labelId = "Ukuran shot beda dari shot sebelumnya",
                passed = prev == null || prev.shotSize != input.shotSize,
                missingHint = "Dua shot berurutan ukuran sama melanggar aturan 30 derajat — variasikan (wide -> close -> insert)."
            ),
            ScoreItem(
                id = "screen_direction",
                labelId = "Arah layar konsisten dengan shot sebelumnya",
                passed = prev == null ||
                    prev.screenDirection.isBlank() ||
                    input.screenDirection.isBlank() ||
                    prev.screenDirection == input.screenDirection,
                missingHint = "Arah layar berubah tanpa alasan — tokoh akan terlihat berlari ke arah berlawanan."
            ),
            ScoreItem(
                id = "wan_word_guard",
                labelId = "Wan: estimasi kata aman (<=100)",
                passed = input.engine != TargetEngine.WAN || wanEstimate <= 100,
                missingHint = "Prompt Wan mendekati batas 80-120 kata (encoder 512 token) — pendekkan deskripsi dunia/DNA."
            ),
            ScoreItem(
                id = "light_source",
                labelId = "Sumber cahaya eksplisit",
                passed = input.light != null,
                missingHint = "Tiap shot butuh satu sumber cahaya bernama — tanpa itu lighting berubah-ubah antar klip."
            ),
            ScoreItem(
                id = "action_not_repeated",
                labelId = "Aksi tidak mengulang scene sebelumnya",
                passed = prev == null || sameScene || prev.actionEn.trim() != input.actionEn.trim(),
                missingHint = "Aksi sama persis dengan scene sebelumnya — variasikan gerakannya."
            )
        )
        return ScoreResult(items, items.count { it.passed })
    }

    // Prompt Score v4 (v1.3) — 5 aturan kesehatan PROMPT (bukan input):
    // dinilai dari input + prompt hasil build (bila sudah ada) + konteks
    // klip sebelumnya. Aturan 6-8 (show-don't-tell, rantai kontinuitas,
    // negative per engine) = v1.3.1.
    // clipSeconds = durasi klip (default 10 dtk, standar Flow).
    fun scoreV4(
        input: ShotInput,
        generated: GeneratedPrompt? = null,
        prev: ShotInput? = null,
        clipSeconds: Float = 10f
    ): ScoreResult {
        val engine = input.engine ?: TargetEngine.FLOW
        val meter = PromptCore.meterFor(input, engine, clipSeconds)
        val estimateTotal = meter.sumOf { it.words }
        val total = generated?.wordCount ?: estimateTotal
        val dominant = meter.maxByOrNull { it.words }
        val dominantShare = if (estimateTotal > 0) {
            (dominant?.words ?: 0).toDouble() / estimateTotal
        } else {
            0.0
        }
        val dialogueWords = input.dialogue?.wordCount()
            ?: MythFilter.countWords(input.dialogueLine)
        val dialogueLimit = (clipSeconds * 2.5f).toInt().coerceAtLeast(5)
        // Jumlah pembicara: 1 per DialogueLine; baris lama dihitung dari
        // kemunculan "says" (adegan 2 pembicara seperti Scene 13 Riko).
        val renderedDialogue = PromptEngine.renderDialogue(input, engine)
        val speakers = if (renderedDialogue.isBlank()) {
            0
        } else {
            val saysCount = Regex("(?i)\\bsays\\b").findAll(renderedDialogue).count()
            val parenCount = Regex("\\([^)]+\\):").findAll(renderedDialogue).count()
            maxOf(1, saysCount, parenCount)
        }
        val lang = input.dialogue?.language ?: DialogueLang.EN
        val matrix = DialogueMatrix.check(engine, lang)
        val items = listOf(
            ScoreItem(
                id = "slot_budget",
                labelId = "Anggaran kata per slot",
                passed = total <= engine.maxWords && dominantShare <= 0.40,
                missingHint = if (total > engine.maxWords) {
                    "Prompt $total kata, lewat batas keras ${engine.maxWords} — pendekkan ${dominant?.slot ?: "slot terbesar"}."
                } else if (dominantShare > 0.40) {
                    "Slot ${dominant?.slot} makan ${(dominantShare * 100).toInt()}% anggaran — pendekkan atau pakai kartu 3 jangkar (DNA panjang tidak boleh ditempel)."
                } else {
                    ""
                }
            ),
            ScoreItem(
                id = "trim_report",
                labelId = "Tidak ada trim diam-diam",
                passed = generated == null ||
                    generated.warnings.none { it.contains("dipangkas") },
                missingHint = generated?.warnings
                    ?.firstOrNull { it.contains("dipangkas") }
                    ?: "Prompt dipangkas saat build — periksa peringatan trim."
            ),
            ScoreItem(
                id = "dialogue_timing",
                labelId = "Timing dialog (<=2,5 kata/detik)",
                passed = dialogueWords <= dialogueLimit,
                missingHint = "Dialog $dialogueWords kata untuk klip ${clipSeconds.toInt()} dtk (maks $dialogueLimit) — potong atau bagi ke 2 klip."
            ),
            ScoreItem(
                id = "speaker_rule",
                labelId = "1 pembicara (<=8 dtk) / 2 pembicara (>=10 dtk)",
                passed = speakers <= 1 || clipSeconds >= 10f,
                missingHint = "$speakers pembicara dalam klip ${clipSeconds.toInt()} dtk — aturan: 1 pembicara untuk klip <=8 dtk; 2 pembicara butuh >=10 dtk dan keduanya terlihat."
            ),
            ScoreItem(
                id = "dialogue_language",
                labelId = "Bahasa dialog cocok dengan engine",
                passed = matrix.check != DialogueCheck.BLOCKED &&
                    matrix.check != DialogueCheck.SILENT,
                missingHint = matrix.message
            )
        )
        return ScoreResult(items, items.count { it.passed })
    }
}
