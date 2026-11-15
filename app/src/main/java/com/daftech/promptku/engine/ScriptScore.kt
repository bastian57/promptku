package com.daftech.promptku.engine

// Naskah Score v1.3 — 10 poin checklist (pola Prompt Score v2).
// Dihitung dari DRAFT naskah FLAT (bukan dari teks hasil), supaya user tahu
// bagian mana yang kurang SEBELUM generate prompt massal.
// Riset 15 Nov 2026: 9 dari 12 kesalahan naskah amatir bisa dicegah
// mesin rule-based; subtext dan orisinalitas tetap butuh manusia.
// v1.3: scene = klip (tidak ada lagi shot) — semua aturan dihitung per scene.
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

    private fun dialogueText(scene: DraftScene): String {
        val line = scene.dialogue ?: return ""
        return "${line.textEn} ${line.textId}".trim()
    }

    fun score(draft: ScriptDraft): ScoreResult {
        val scenes = draft.scenes
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
                labelId = "1 aksi per scene (<=12 kata)",
                passed = scenes.isNotEmpty() && scenes.all {
                    it.actionEn.isNotBlank() &&
                        it.actionEn.split(Regex("\\s+")).size <= 12
                },
                missingHint = "Satu scene = satu aksi. Pecah aksi ganda jadi dua scene."
            ),
            ScoreItem(
                id = "dialogue_once_per_beat",
                labelId = "Dialog menempel 1x per beat",
                passed = scenes.groupBy { it.sequenceIndex }.values.all { beatScenes ->
                    beatScenes.count { it.dialogue != null && !it.dialogue.isEmpty() } <= 1
                },
                missingHint = "Dialog satu beat hanya boleh di 1 scene — dialog yang sama di 2 scene = bertele-tele (bug v1.2)."
            ),
            ScoreItem(
                id = "no_internal_verbs",
                labelId = "Tanpa kata kerja internal",
                passed = scenes.none { scene ->
                    val text = (scene.actionEn + " " + dialogueText(scene)).lowercase()
                    INTERNAL_VERBS.any { verb -> text.contains(verb) }
                },
                missingHint = "Kata kerja internal (thinks, realizes, remembers) tidak bisa difilmkan — ganti dengan aksi terlihat."
            ),
            ScoreItem(
                id = "no_expository",
                labelId = "Tanpa dialog ekspositoris",
                passed = scenes.none { scene ->
                    val text = dialogueText(scene).lowercase()
                    EXPOSITORY_PHRASES.any { phrase -> text.contains(phrase) }
                },
                missingHint = "Hapus frasa seperti 'as you know' — penonton harus melihat, bukan diceritakan."
            ),
            ScoreItem(
                id = "dialogue_short",
                labelId = "Dialog <=12 kata per scene",
                passed = scenes.all { scene ->
                    val line = scene.dialogue ?: return@all true
                    maxOf(
                        line.textEn.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                        line.textId.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    ) <= 12
                },
                missingHint = "Dialog satu scene maksimal 12 kata — lip-sync AI lemah untuk dialog panjang."
            ),
            ScoreItem(
                id = "few_speakers",
                labelId = "Tokoh bicara <=3",
                passed = scenes.mapNotNull {
                    it.dialogue?.speaker?.ifBlank { null }
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
                passed = scenes.size == ScriptEngine.clipCount(draft.input.durationMinutes),
                missingHint = "Jumlah scene harus sama dengan durasi ÷ 10 detik."
            ),
            ScoreItem(
                id = "pose_chain",
                labelId = "Rantai pose utuh (ENTRY = EXIT sebelumnya)",
                passed = scenes.isNotEmpty() &&
                    scenes.all { it.poseStart.isNotBlank() && it.poseEnd.isNotBlank() } &&
                    (1 until scenes.size).all { i ->
                        scenes[i].poseStart == scenes[i - 1].poseEnd
                    },
                missingHint = "Setiap scene butuh pose awal = pose akhir scene sebelumnya (rantai kontinuitas)."
            )
        )
        return ScoreResult(items, items.count { it.passed })
    }
}
