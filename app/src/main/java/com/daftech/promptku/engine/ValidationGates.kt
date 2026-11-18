package com.daftech.promptku.engine

// 7 Gerbang Validasi v2.0 — dicek otomatis SEBELUM export, dari bible +
// scene yang sudah dirender. Gerbang 6 prosedural (coba ulang 1x lalu
// tandai) — di sini diwakili sebagai "semua kegagalan sudah ditandai".
// v2.1 fix6 (riset): G7 "Satu Puncak" — gerbang alur (BeatMap + ArcLexicon).
// R8 lunak (klimaks tanpa kejatuhan antagonis terlihat) SENGAJA tidak
// masuk: tanpa kanal "warning" di GateResult, ia akan memicu retry yang
// salah — ditunda sampai ada kanal itu.

data class GateResult(
    val id: String,
    val nameId: String,
    val passed: Boolean,
    val failures: List<String>
)

object ValidationGates {

    // v2.1 fix9 — kata sambung yang kalau jadi kata TERAKHIR aksi berarti
    // kalimatnya tidak selesai ("...follows the subject closely as.",
    // "...snaps back to full speed while." — audit Aang S2/S3). Kata
    // sambung di TENGAH kalimat sah ("...as the sun sets") — yang dicek
    // cuma kata terakhir setelah tanda baca ujung dibuang.
    private val DANGLING_ENDINGS = setOf("while", "as", "and", "but", "with")

    /** Kata sambung yang menggantung di ujung [actionEn], atau null kalau
     *  aksi selesai dengan baik / kosong (kosong urusan gerbang lain). */
    fun danglingWord(actionEn: String): String? {
        val trimmed = actionEn.trim().trimEnd('.', '!', '?', ',', ';', ':').trim()
        if (trimmed.isBlank()) return null
        val last = trimmed.substringAfterLast(' ').lowercase()
        return if (last in DANGLING_ENDINGS) last else null
    }

    /** v2.1 fix7 — narasi = salinan kalimat dunia (Bima, Olympus, Aang
     *  fix9 S1: narration_id == worldDescEn verbatim). Normalisasi
     *  (case + titik ujung + spasi ganda) lalu bandingkan utuh ATAU
     *  awalan 40 karakter pertama dunia (kepala yang sama dipakai
     *  sanitizeAction untuk mendeteksi bocoran dunia). Narasi Indonesia
     *  yang sah tidak pernah diawali teks dunia Inggris — false
     *  positive hampir mustahil. */
    fun narrationIsWorldCopy(narration: String, world: String): Boolean {
        fun norm(s: String): String = s.trim().trimEnd('.').trim().lowercase()
            .replace(Regex("\\s+"), " ")
        val n = norm(narration)
        val w = norm(world)
        if (n.isEmpty() || w.isEmpty()) return false
        return n == w || n.startsWith(w.take(40))
    }

    private val FIGHT_VERBS = listOf(
        "punch", "punches", "kick", "kicks", "grapple", "grapples",
        "slam", "slams", "uppercut", "strike", "strikes", "hit", "hits",
        "fight", "fights", "battle", "battles", "smash", "smashes"
    )

    // v2.1 fix5 — kata fungsi Inggris yang TIDAK deskriptif: token-token ini
    // lolos filter "panjang >= 4" G4 dan bikin false positive ("orange, with"
    // di Storm Titans S17 — "with" dari anchor "orange gi with blue sash"
    // dihitung sebagai token deskriptif). Hanya kata fungsi murni; kata
    // deskriptif ("long", "dark", "golden") TIDAK masuk daftar ini.
    private val STOPWORDS = setOf(
        "with", "from", "that", "this", "then", "when", "over", "into",
        "onto", "your", "their", "them", "than", "about", "after", "before",
        "between", "through", "during", "under", "above", "below", "across",
        "behind", "beside", "while", "which", "where", "there", "here",
        "have", "been", "being", "will", "would", "could", "should", "very",
        "much", "more", "most", "some", "such", "only", "also", "even",
        "still", "each", "both", "same", "other", "another", "upon",
        "among", "around", "toward", "towards", "against", "within",
        "without", "along"
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

    /** G7 "Satu Puncak" — gerbang alur sadar-beat (fix6 riset).
     *  Aturan (semua membaca action_en + narration_id, kecuali disebut lain):
     *  R1 FINALITAS (final/terakhir/puncak/akhir, "serangan terakhir",
     *    "semakin memuncak") HANYA di Klimaks.
     *  R2 AWAL ("first clash", "benturan pertama", "memulai pertarungan")
     *    HANYA di Pembuka/Pemicu — cerita tidak boleh "dimulai" dua kali.
     *  R3 RESOLUSI (victorious/kemenangan/bebas/damai/tenang/terkalahkan/
     *    usai/mereda) HANYA di Penutup.
     *  R4 SERANGAN BESAR: maks per beat Pembuka 0, Pemicu 1, Eskalasi 2,
     *    Klimaks 3, Penutup 0 (maks 1 per scene — pola ArcLexicon).
     *  R4b JURUS BERNAMA: maks 1x per film DAN hanya di Eskalasi/Klimaks.
     *  R5 ENTRANCE VILLAIN: maks 1x per film DAN hanya di Pembuka/Pemicu.
     *  R6 KURVA: skor maks pra-Klimaks tidak boleh mengalahkan Klimaks;
     *    skor maks Penutup tidak boleh setinggi/melebihi Klimaks. Dilewati
     *    kalau film tidak punya busur aksi sama sekali (tanpa big attack /
     *    jurus bernama — drama/romansa tidak dipaksa berkurva).
     *  R7 PENUTUP BERSIH: aksi Penutup tanpa kata berantem/tantangan; DAN
     *    narasi yang mengklaim klimaks harus ditunjukkan aksinya di scene
     *    yang sama (bukan cuma diceritakan).
     *  R9 DISIPLIN SLOW-MO (fix8 v2): kata slow-mo di action_en ("slow
     *    motion", "time freezes", "time slows", ...) HANYA di scene IMPACT
     *    + finisher; scene HERO_HURT wajib menunjukkan hero terluka
     *    (penanda + nama hero di kalimat yang sama). Fragmen render TIDAK
     *    digate — sudah dijamin kode (fragmen FAST tak punya kata slow-mo;
     *    IMPACT/finisher satu-satunya yang punya). Hanya film ACTION.
     *  Kalibrasi lapangan (teks verbatim): Bima 12/18 scene melanggar
     *  (S3, S4, S5, S7, S9, S10, S11, S12, S13, S15, S17, S18); Kaito 8/18
     *  (S3, S4, S5, S6, S9, S11, S16, S17); Kaito S18 LOLOS ("raises his
     *  fists in triumph" di Penutup = resolusi di tempatnya). */
    fun runArcGate(bible: StoryBible, scenes: List<V2ScenePrompt>): GateResult {
        val failures = mutableListOf<String>()
        if (scenes.isEmpty()) {
            return GateResult("G7", "Satu puncak: alur naik ke satu klimaks", true, emptyList())
        }
        val map = BeatMap.build(bible.totalScenes, bible.beats)
        val climaxIdx = BeatMap.climaxIndex(bible.beats)
        val closingIdx = BeatMap.closingIndex(bible.beats)

        for (scene in scenes) {
            val num = scene.sceneNumber
            val beat = BeatMap.beatForScene(num, map)
            val role = BeatMap.roleOf(beat.beatIndex, bible.beats)
            val action = scene.plan.actionEn
            val narration = scene.plan.narrationId
            // R1 — finalitas hanya di Klimaks.
            val finality = ArcLexicon.findFinality(action) + ArcLexicon.findFinality(narration)
            if (finality.isNotEmpty() && role != BeatRole.CLIMAX) {
                failures.add(
                    "Scene $num (${beat.name}): klaim finalitas \"${finality.first()}\" di luar Klimaks"
                )
            }
            // R2 — klaim awal hanya di Pembuka/Pemicu.
            val firstness = ArcLexicon.findFirstness(action) + ArcLexicon.findFirstness(narration)
            if (firstness.isNotEmpty() && role != BeatRole.OPENING && role != BeatRole.EARLY) {
                failures.add(
                    "Scene $num (${beat.name}): klaim awal \"${firstness.first()}\" setelah Pembuka/Pemicu"
                )
            }
            // R3 — resolusi hanya di Penutup.
            val resolution = ArcLexicon.findResolution(action) + ArcLexicon.findResolution(narration)
            if (resolution.isNotEmpty() && role != BeatRole.CLOSING) {
                failures.add(
                    "Scene $num (${beat.name}): klaim resolusi \"${resolution.first()}\" di luar Penutup"
                )
            }
            // R7 — narasi mengklaim klimaks yang tidak ditunjukkan aksinya.
            val narrFinality = ArcLexicon.findFinality(narration)
            if (narrFinality.isNotEmpty() && !ArcLexicon.isBigAttack(action)) {
                failures.add(
                    "Scene $num: narasi mengklaim klimaks (\"${narrFinality.first()}\") yang tidak ditunjukkan aksinya"
                )
            }
            // R7 — Penutup bersih dari kata berantem/tantangan.
            if (role == BeatRole.CLOSING) {
                val fight = ArcLexicon.findFightChallenge(action)
                if (fight.isNotEmpty()) {
                    failures.add(
                        "Scene $num (Penutup): masih ada aksi berantem (\"${fight.first()}\") setelah klimaks"
                    )
                }
            }
        }

        // R4 — serangan besar per beat (batas: Pembuka 0, Pemicu 1,
        // Eskalasi 2, Klimaks 3, Penutup 0). Klimaks TIDAK punya batas
        // minimum — drama tanpa big attack tidak dipaksa berantem.
        val bigByBeat = mutableMapOf<Int, MutableList<Int>>()
        for (scene in scenes) {
            if (ArcLexicon.isBigAttack(scene.plan.actionEn)) {
                val beat = BeatMap.beatForScene(scene.sceneNumber, map)
                bigByBeat.getOrPut(beat.beatIndex) { mutableListOf() }.add(scene.sceneNumber)
            }
        }
        for ((beatIdx, nums) in bigByBeat) {
            val max = BeatMap.maxBigAttacks(beatIdx, bible.beats)
            if (nums.size > max) {
                val beat = map.first { it.beatIndex == beatIdx }
                failures.add(
                    "Scene ${nums.last()} (${beat.name}): serangan besar ${nums.size}x (maks $max per beat)"
                )
            }
        }

        // R4b — jurus bernama: maks 1x per film, hanya Eskalasi/Klimaks.
        val moves = mutableMapOf<String, MutableList<Int>>()
        val moveBeat = mutableMapOf<String, Int>()
        for (scene in scenes) {
            val move = ArcLexicon.extractNamedMove(scene.plan.actionEn, bible) ?: continue
            val key = move.lowercase()
            moves.getOrPut(key) { mutableListOf() }.add(scene.sceneNumber)
            moveBeat.putIfAbsent(key, BeatMap.beatForScene(scene.sceneNumber, map).beatIndex)
        }
        for ((move, nums) in moves) {
            if (nums.size > 1) {
                failures.add(
                    "Scene ${nums.last()}: jurus bernama \"$move\" sudah dipakai di scene ${nums.first()} (maks 1x per film)"
                )
            }
            val role = BeatMap.roleOf(moveBeat[move] ?: 0, bible.beats)
            if (role != BeatRole.LATE && role != BeatRole.CLIMAX) {
                val beat = map.first { it.beatIndex == moveBeat[move] }
                failures.add(
                    "Scene ${nums.first()} (${beat.name}): jurus bernama \"$move\" di luar Eskalasi/Klimaks"
                )
            }
        }

        // R5 — entrance villain: maks 1x per film, hanya Pembuka/Pemicu.
        val entrances = scenes.filter {
            ArcLexicon.isVillainEntrance(it.plan.actionEn, bible.antagonistName)
        }.map { it.sceneNumber }
        for ((idx, num) in entrances.withIndex()) {
            val beat = BeatMap.beatForScene(num, map)
            val role = BeatMap.roleOf(beat.beatIndex, bible.beats)
            if (idx > 0) {
                failures.add(
                    "Scene $num (${beat.name}): entrance ${bible.antagonistName} kedua (sudah masuk di scene ${entrances.first()})"
                )
            }
            if (role != BeatRole.OPENING && role != BeatRole.EARLY) {
                failures.add(
                    "Scene $num (${beat.name}): entrance ${bible.antagonistName} di luar Pembuka/Pemicu"
                )
            }
        }

        // R9 — disiplin slow-mo + hero terluka (fix8 v2). Hanya film
        // ACTION (isFightFilm — rumus yang sama dengan prompter): film
        // lain tidak punya peran fight, jadi tidak diperiksa.
        // R9a: kata slow-mo di action_en hanya di scene IMPACT + finisher
        // (peran dari BeatMap — SAMA dengan yang diterima prompter; kalau
        // gerbang memakai peta lain, LLM dihukum atas peran yang tidak
        // pernah diterimanya).
        // R9b: scene HERO_HURT wajib menunjukkan hero terluka (penanda +
        // nama hero di kalimat yang sama — "villain staggers" tidak dihitung).
        if (BeatMap.isFightFilm(bible)) {
            val roles = BeatMap.fightRoles(map, bible.beats, true)
            val impactScene = roles.entries.firstOrNull { it.value == FightRole.IMPACT }?.key
            val finisherScene = roles.entries.firstOrNull { it.value == FightRole.FINISHER }?.key
            val hurtScene = roles.entries.firstOrNull { it.value == FightRole.HERO_HURT }?.key
            for (scene in scenes) {
                val num = scene.sceneNumber
                val slowMo = ArcLexicon.findSlowMo(scene.plan.actionEn)
                if (slowMo.isNotEmpty() && num != impactScene && num != finisherScene) {
                    val beat = BeatMap.beatForScene(num, map)
                    failures.add(
                        "Scene $num (${beat.name}): kata slow-mo \"${slowMo.first()}\" di luar scene IMPACT/finisher"
                    )
                }
            }
            if (hurtScene != null) {
                val hurtPlan = scenes.firstOrNull { it.sceneNumber == hurtScene }
                if (hurtPlan != null &&
                    !ArcLexicon.isHeroHurt(hurtPlan.plan.actionEn, bible.hero?.name.orEmpty())
                ) {
                    failures.add(
                        "Scene $hurtScene: beat HERO HURT — aksi tidak menunjukkan hero terluka"
                    )
                }
            }
        }

        // R6 — kurva intensitas (dilewati kalau film tanpa busur aksi).
        val hasActionArc = scenes.any {
            ArcLexicon.isBigAttack(it.plan.actionEn) ||
                ArcLexicon.extractNamedMove(it.plan.actionEn, bible) != null
        }
        if (hasActionArc) {
            val scores = scenes.associate {
                it.sceneNumber to ArcLexicon.intensityScore(it.plan.actionEn, it.plan.narrationId, bible)
            }
            val climaxRange = map.first { it.beatIndex == climaxIdx }
            val closingRange = map.first { it.beatIndex == closingIdx }
            val climaxMax = scores.filterKeys { it in climaxRange.firstScene..climaxRange.lastScene }
                .values.maxOrNull() ?: 0
            val preMax = scores.filterKeys { it < climaxRange.firstScene }
                .values.maxOrNull() ?: 0
            val closeMax = scores.filterKeys { it in closingRange.firstScene..closingRange.lastScene }
                .values.maxOrNull() ?: 0
            if (preMax > climaxMax) {
                failures.add(
                    "Kurva intensitas: puncak pra-Klimaks ($preMax) mengalahkan Klimaks ($climaxMax)"
                )
            }
            if (climaxMax > 0 && closeMax >= climaxMax) {
                failures.add(
                    "Kurva intensitas: Penutup ($closeMax) setinggi/melebihi Klimaks ($climaxMax)"
                )
            }
        }
        return GateResult(
            "G7", "Satu puncak: finalitas/resolusi di tempatnya, jurus & entrance 1x",
            failures.isEmpty(), failures
        )
    }

    fun runGates(
        bible: StoryBible,
        scenes: List<V2ScenePrompt>,
        hasImages: Boolean,
        // v2.1 fix4 — narasi wajib hanya untuk jalur CLOUD (prompter LLM
        // wajib mengisi narration_id). Draft Offline (rule-based, tanpa AI)
        // tidak punya sumber narasi — dibebaskan supaya G5-nya tidak merah
        // 18x untuk sesuatu yang memang tidak ada di jalur itu.
        requireNarration: Boolean = true
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
                    // v2.1 fix7 — anchor NAMA BARE ("Aang") dikecualikan:
                    // fix4 WAJIB menamai aktor di tiap aksi, jadi nama pasti
                    // muncul dan pasti ke-flag — noise, bukan sinyal (audit
                    // Aang fix9: 66× "bertabrakan", sebagian besar dari
                    // anchor nama). Yang dicek cuma anchor PENAMPILAN
                    // ("bald head with a blue arrow tattoo"). Anchor yang
                    // MENGANDUNG nama ("Aang's scarred shield") tetap dicek.
                    if (anchor.trim().equals(hero?.name, ignoreCase = true) ||
                        anchor.trim().equals(bible.antagonistName, ignoreCase = true)
                    ) return@forEach
                    if (anchor.length >= 4 && text.contains(anchor.lowercase())) {
                        g4Failures.add("Scene ${scene.sceneNumber}: deskripsi subjek penuh (\"$anchor\") bertabrakan dengan gambar")
                    }
                    // v2.1 fix6 — parafrasa ("massive feathered wings" vs anchor
                    // "large golden feathered wings") lolos dari cocok frasa
                    // persis: cek tumpang tindih token deskriptif di actionEn
                    // SAJA — "golden" di cahaya akan false positive kalau
                    // dicek di seluruh teks.
                    val anchorTokens = anchor.lowercase().split(Regex("[^a-z0-9]+"))
                        .filter { it.length >= 4 && it !in STOPWORDS }.toSet()
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
        // v2.1 fix4 — narasi voiceover WAJIB terisi tiap scene (propmter
        // dilarang mengosongkan narration_id; S18 Broly hilang karena "or
        // empty"). Scene tanpa narasi gagal di sini supaya masuk retryReasons
        // dan prompter mengisi ulang — bukan lolos diam-diam.
        // v2.1 fix9 — aksi menggantung (audit Aang: S2 "closely as.", S3
        // "full speed while."): aksi yang berakhir dengan kata sambung =
        // kalimat tidak selesai (LLM kepotong di tengah pikiran). Bukan
        // trim — kata-kata di bawah cap — jadi ini cacat LLM yang retry
        // bisa perbaiki: masuk G5 (bukan G4 yang dikecualikan dari retry).
        val g5Failures = scenes.mapNotNull { scene ->
            val idWords = wordCount(scene.plan.dialogueId)
            val enWords = wordCount(scene.plan.dialogueEn)
            // v2.1 fix5 — dialog WAJIB bahasa pilihan user (bible.dialogueLang).
            // Akar keluhan lapangan: prompter tidak pernah diberi tahu pilihan
            // ini, jadi field bahasa pilihan bisa kosong sementara terjemahannya
            // terisi — render lalu jatuh ke fallback diam-diam. Sekarang itu
            // gagal gerbang dan masuk retryReasons, bukan lolos diam-diam.
            val hasDialogue = scene.plan.dialogueId.isNotBlank() || scene.plan.dialogueEn.isNotBlank()
            val chosenBlank = if (bible.dialogueLang == "EN") scene.plan.dialogueEn.isBlank()
                else scene.plan.dialogueId.isBlank()
            val dangling = danglingWord(scene.plan.actionEn)
            // v2.1 fix7 — aksi kosong (audit Olympus S17 "scene hantu":
            // aksi inti kosong, render cuma fragmen efek). Cek aksi
            // SETELAH sanitizeAction (aksi yang isinya cuma kalimat dunia
            // dikosongkan sanitizeAction saat render — gerbang harus
            // membaca yang sama dengan yang dirender). Cabang pertama:
            // aksi kosong paling parah, laporkan itu dulu.
            val cleanAction = ModeRender.sanitizeAction(scene.plan.actionEn, bible)
            when {
                cleanAction.isBlank() -> "Scene ${scene.sceneNumber}: aksi kosong (tidak ada aksi di klip ini)"
                idWords > 8 -> "Scene ${scene.sceneNumber}: dialog ID $idWords kata (maks 8)"
                enWords > 8 -> "Scene ${scene.sceneNumber}: dialog EN $enWords kata (maks 8)"
                hasDialogue && chosenBlank -> "Scene ${scene.sceneNumber}: dialog ada tapi bukan bahasa pilihan (${bible.dialogueLang})"
                scene.plan.narrationId.isBlank() && requireNarration -> "Scene ${scene.sceneNumber}: narasi kosong (wajib terisi)"
                // v2.1 fix7 — narasi = salinan kalimat dunia (Bima, Olympus,
                // Aang fix9 S1). Gagal gerbang (retryable) supaya prompter
                // menulis narasi sendiri. requireNarration yang sama dengan
                // cek kosong: draft offline dibebaskan.
                scene.plan.narrationId.isNotBlank() && requireNarration &&
                    narrationIsWorldCopy(scene.plan.narrationId, bible.worldDescEn) -> "Scene ${scene.sceneNumber}: narasi sama dengan kalimat dunia (tulis narasi sendiri, bukan deskripsi dunia)"
                dangling != null -> "Scene ${scene.sceneNumber}: aksi menggantung di kata '$dangling' (kalimat tidak selesai)"
                else -> null
            }
        }
        results.add(GateResult("G5", "Dialog ≤8 kata, 1 pembicara per klip; narasi terisi & bukan salinan dunia; aksi terisi & tidak menggantung", g5Failures.isEmpty(), g5Failures))

        // G7 — Satu Puncak (fix6 riset): alur naik ke SATU klimaks.
        // Membaca action_en DAN narration_id (narasi membawa 9/12 klaim
        // sakit di film Bima — gerbang yang tidak membaca narasi buta 75%
        // penyakitnya). Dialog TIDAK digate (tokoh boleh sesumbar/bohong).
        // Berjalan juga untuk Draft Offline (narasi kosong = tidak ada
        // temuan R1–R3; aksi tetap diperiksa R4–R7).
        results.add(runArcGate(bible, scenes))

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
