package com.daftech.promptku.engine

// Naskah Score v1.2 — 10 poin checklist (pola Prompt Score v2).
// Dihitung dari DRAFT naskah (bukan dari teks hasil), supaya user tahu
// bagian mana yang kurang SEBELUM generate prompt massal.
// Riset 15 Nov 2026: 9 dari 12 kesalahan naskah amatir bisa dicegah
// mesin rule-based; subtext dan orisinalitas tetap butuh manusia.
object ScriptScore {

    // Kata kerja internal (tidak bisa difilmkan) — dicek di aksi + dialog.
    val INTERNAL_VERBS: List<String> = listOf(
        "thinks", "realizes", "realises", "remembers", "feels", "knows",
        "wonders", "decides", "understands", "believes", "hopes", "fears"
    )

    // Frasa ekspositoris — dialog yang menjelaskan plot ke penonton.
    val EXPOSITORY_PHRASES: List<String> = listOf(
        "as you know", "like i told you", "remember when", "as we all know",
        "as you remember", "like we discussed"
    )

    fun score(draft: ScriptDraft): ScoreResult {
        val scenes = draft.scenes
        val shots = draft.shots
        val items = listOf(
            ScoreItem(
                id = "want_obstacle",
                labelId = "Tiap scene punya want + obstacle",
                passed = scenes.isNotEmpty() && scenes.all {
                    it.want.isNotBlank() && it.obstacle.isNotBlank()
                },
                missingHint = "Setiap scene dramatis butuh want (tujuan tokoh) + obstacle (halangan)."
            ),
            ScoreItem(
                id = "one_action",
                labelId = "1 aksi per shot (<=12 kata)",
                passed = shots.isNotEmpty() && shots.all {
                    it.actionEn.isNotBlank() &&
                        it.actionEn.split(Regex("\\s+")).size <= 12
                },
                missingHint = "Satu shot = satu aksi. Pecah aksi ganda jadi dua shot."
            ),
            ScoreItem(
                id = "scene_action_short",
                labelId = "Aksi scene <=40 kata",
                passed = scenes.all {
                    it.actionEn.split(Regex("\\s+")).size <= 40
                },
                missingHint = "Aksi satu scene jangan lebih dari 40 kata — potong jadi shot."
            ),
            ScoreItem(
                id = "no_internal_verbs",
                labelId = "Tanpa kata kerja internal",
                passed = scenes.none { scene ->
                    val text = (scene.actionEn + " " + scene.dialogueLine).lowercase()
                    INTERNAL_VERBS.any { verb -> text.contains(verb) }
                } && shots.none { shot ->
                    val text = shot.actionEn.lowercase()
                    INTERNAL_VERBS.any { verb -> text.contains(verb) }
                },
                missingHint = "Kata kerja internal (thinks, realizes, remembers) tidak bisa difilmkan — ganti dengan aksi terlihat."
            ),
            ScoreItem(
                id = "no_expository",
                labelId = "Tanpa dialog ekspositoris",
                passed = scenes.none { scene ->
                    val text = scene.dialogueLine.lowercase()
                    EXPOSITORY_PHRASES.any { phrase -> text.contains(phrase) }
                },
                missingHint = "Hapus frasa seperti 'as you know' — penonton harus melihat, bukan diceritakan."
            ),
            ScoreItem(
                id = "dialogue_short",
                labelId = "Dialog <=12 kata per scene",
                passed = scenes.all {
                    it.dialogueLine.isBlank() ||
                        it.dialogueLine.split(Regex("\\s+")).size <= 12
                },
                missingHint = "Dialog satu scene maksimal 12 kata — lip-sync AI lemah untuk dialog panjang."
            ),
            ScoreItem(
                id = "few_speakers",
                labelId = "Tokoh bicara <=3",
                passed = scenes.mapNotNull {
                    it.dialogueName.ifBlank { null }
                }.distinct().size <= 3,
                missingHint = "Batasi tokoh yang bicara maksimal 3 — lebih dari itu penonton bingung."
            ),
            ScoreItem(
                id = "few_locations",
                labelId = "Lokasi <=3",
                passed = scenes.map { scene ->
                    // Lokasi = bagian slugline antara INT./EXT. dan " - WAKTU".
                    val body = scene.slugline.substringAfter(". ").substringBefore(" - ")
                    body
                }.distinct().size <= 3,
                missingHint = "Batasi lokasi maksimal 3 — tiap lokasi baru butuh establishing shot."
            ),
            ScoreItem(
                id = "clip_count_matches",
                labelId = "Total klip = durasi target",
                passed = shots.size == ScriptEngine.clipCount(draft.input.durationMinutes),
                missingHint = "Jumlah klip harus sama dengan durasi ÷ 10 detik."
            ),
            ScoreItem(
                id = "entry_exit_state",
                labelId = "ENTRY/EXIT state semua shot",
                passed = shots.isNotEmpty() && shots.all {
                    it.poseStart.isNotBlank() && it.poseEnd.isNotBlank()
                },
                missingHint = "Setiap shot butuh pose awal dan pose akhir (rantai kontinuitas)."
            )
        )
        return ScoreResult(items, items.count { it.passed })
    }
}
