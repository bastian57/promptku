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
}
