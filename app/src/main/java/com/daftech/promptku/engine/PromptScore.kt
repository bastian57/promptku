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
}
