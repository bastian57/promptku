package com.daftech.promptku.engine

// BeatMap v2.1 fix6 (riset) — peta beat→scene sebagai KONTRAK alur.
// Diagnosis (riset alur, 17 Nov 2026): dua baris prompt eskalasi gagal
// bukan karena kalimatnya kurang tegas, melainkan karena pipeline tidak
// memberi LLM tiga hal: POSISI (V2Beat tanpa rentang scene — prompter tak
// tahu batch-nya di beat mana), STATE (antar-batch cuma previousExit satu
// baris — batch 3 tak tahu batch 1 sudah memakai "Tempest Breaker" di S3),
// VERIFIKASI (tak ada gerbang yang membaca isi narration_id).
// BeatMap adalah satu sumber kebenaran untuk ketiganya: prompter (blok
// beat per batch), gerbang G7, dan ledger (apa yang sudah terpakai).
// Literatur: DOC (detailed outline + controller, ACL 2023), Re3 (recursive
// reprompting, EMNLP 2022), PlotMachines (label posisi struktural, 2020),
// Dramatron (prompt chaining, CHI 2023), Save the Cat (midpoint/Finale).
// File ini Kotlin murni, tanpa dependensi Android — bisa diuji lokal.

/** Satu beat: rentang scene kontigu + intensitas (1..5). */
data class BeatRange(
    val beatIndex: Int,
    val name: String,
    val firstScene: Int,
    val lastScene: Int,
    val intensity: Int
) {
    val sceneCount: Int get() = lastScene - firstScene + 1
    fun contains(scene: Int) = scene in firstScene..lastScene
}

/** Peran dramatik sebuah beat — yang menentukan aturan leksikalnya. */
enum class BeatRole {
    OPENING, EARLY, LATE, CLIMAX, FALLING, CLOSING
}

/** State alur yang sudah terpakai — dihitung kode dari plan batch
 *  sebelumnya, ditempel ke prompt batch berikutnya (ledger). */
data class ArcLedger(
    val bigAttacks: List<Pair<Int, String>> = emptyList(),
    val villainEntranceScene: Int? = null,
    val victoriesClaimed: List<Int> = emptyList()
) {
    companion object {
        val EMPTY = ArcLedger()

        /** Hitung ledger dari plan yang sudah ada (batch sebelumnya / scene
         *  lain saat retry & regenerasi). Detektor yang sama dengan G7 —
         *  satu sumber kebenaran leksikon. */
        fun fromPlans(plans: List<V2ScenePlan>, bible: StoryBible): ArcLedger {
            val big = plans.filter { ArcLexicon.isBigAttack(it.actionEn) }.map { plan ->
                val named = ArcLexicon.extractNamedMove(plan.actionEn, bible)
                val label = named ?: plan.actionEn.split(Regex("\\s+")).take(6).joinToString(" ")
                plan.sceneNumber to label
            }
            val entrance = plans.firstOrNull {
                ArcLexicon.isVillainEntrance(it.actionEn, bible.antagonistName)
            }?.sceneNumber
            val victories = plans.filter { plan ->
                ArcLexicon.findResolution(plan.actionEn).isNotEmpty() ||
                    ArcLexicon.findResolution(plan.narrationId).isNotEmpty()
            }.map { it.sceneNumber }
            return ArcLedger(bigAttacks = big, villainEntranceScene = entrance, victoriesClaimed = victories)
        }
    }

    /** Teks ledger untuk prompt batch ("STATE SO FAR ..."). */
    fun describe(): String {
        val big = if (bigAttacks.isEmpty()) "none"
        else bigAttacks.joinToString("; ") { (num, label) -> "\"$label\" (scene $num)" }
        val entrance = villainEntranceScene?.let { "used (scene $it)" } ?: "not yet used"
        val victories = if (victoriesClaimed.isEmpty()) "none"
        else victoriesClaimed.joinToString(", ") { "scene $it" }
        return "big attacks: $big. Villain entrance: $entrance. Victories claimed: $victories."
    }
}

/** Leksikon + detektor alur — dipakai G7, ledger, dan blok beat prompt.
 *  Dua tingkat (riset §4.3): kata ambigu ("first", "free", "calm", "last",
 *  "tenang") HANYA lewat frasa multi-kata — kalau tidak, false positive.
 *  Dialog TIDAK pernah diperiksa (tokoh boleh sesumbar/bohong); yang
 *  diperiksa action_en + narration_id. */
object ArcLexicon {

    // --- Tingkat KATA (cocok token utuh, tidak ambigu) ---

    val FINALITY_WORDS_EN = setOf("final", "ultimate", "climax", "decisive")
    // "akhir" ditambah dari kalibrasi lapangan Kaito S16 ("menentukan akhir
    // pertarungan" = klaim finalitas; stemmer menangkap "akhirnya" juga).
    val FINALITY_WORDS_ID = setOf("terakhir", "pamungkas", "puncak", "memuncak", "akhir")

    // "triumphant" ditambah dari kalibrasi Bima S15 ("triumphant expression"
    // = klaim kemenangan; token "triumphant" tidak sama dengan "triumph").
    val RESOLUTION_WORDS_EN = setOf("victorious", "victory", "triumph", "triumphant", "defeated", "peace", "freedom")
    val RESOLUTION_WORDS_ID = setOf("menang", "kemenangan", "bebas", "damai", "terkalahkan", "usai", "mereda", "tenang", "kalah")

    // --- Tingkat FRASA (cocok frasa multi-kata, presisi tinggi) ---

    val FINALITY_PHRASES_EN = listOf(
        "reaches its peak", "rising tension", "at last", "once and for all", "final blow"
    )
    val FINALITY_PHRASES_ID = listOf(
        "serangan terakhir", "mencapai puncaknya", "semakin memuncak"
    )

    val FIRSTNESS_PHRASES_EN = listOf(
        "first clash", "first time", "first strike", "begins the fight", "first battle"
    )
    val FIRSTNESS_PHRASES_ID = listOf(
        "benturan pertama", "pertama kali", "memulai pertarungan", "pertarungan dimulai"
    )

    val RESOLUTION_PHRASES_EN = listOf(
        "stands victorious", "finally free", "battle is over", "fight is over", "world is free"
    )
    val RESOLUTION_PHRASES_ID = listOf(
        "dunia kembali damai", "telah usai", "akhirnya bebas", "badai reda", "dunia akhirnya bebas"
    )

    // --- Serangan besar: (modifier ATAU verba energi) + nomina serang ---

    val BIG_MODIFIERS = setOf(
        "massive", "blinding", "explosive", "concentrated", "cataclysmic",
        "devastating", "colossal", "searing", "blazing", "spiraling", "tremendous"
    )
    val BIG_ENERGY_VERBS = setOf(
        "unleash", "unleashes", "detonate", "detonates", "hurl", "hurls",
        "blast", "blasts", "channel", "channels", "summon", "summons",
        "throw", "throws", "launch", "launches", "fire", "fires"
    )
    // "surge" DICABUT dari nomina serang (kalibrasi Kaito S2): "massive
    // stone guardians surge forward" — "surge" di situ verba gerak (para
    // penjaga maju), bukan nomina serangan. Kaito S7 tetap kena big attack
    // lewat "channeling lightning". "down" DICABUT dari preposisi entrance
    // (kalibrasi Kaito S14): "crashes down" = mendarat menyerang, bukan
    // masuk arena — entrance butuh asal ("from"/"onto"/"into").
    val BIG_ATTACK_NOUNS = setOf(
        "bolt", "beam", "wave", "ball", "blast", "vortex", "spear", "disk",
        "shockwave", "crescent", "sphere", "lightning", "plasma"
    )

    // --- Entrance villain ---

    val ENTRANCE_VERBS = setOf(
        "descend", "descends", "crash", "crashes", "arrive", "arrives",
        "appear", "appears", "emerge", "emerges", "rise", "rises",
        "burst", "bursts", "dive", "dives", "plunge", "plunges",
        "swoop", "swoops"
    )
    val ENTRANCE_PREPOSITIONS = setOf(
        "from", "onto", "into", "toward", "towards", "above", "over"
    )

    // --- Leksikon berantem/tantangan (Penutup harus bersih) ---

    val FIGHT_CHALLENGE_WORDS_EN = setOf(
        "punch", "punches", "kick", "kicks", "slam", "slams", "strike", "strikes",
        "hurl", "hurls", "unleash", "unleashes", "blast", "blasts", "clash",
        "clashes", "challenge", "challenges", "threaten", "threatens",
        "attack", "attacks", "duel", "duels", "defiance"
    )
    val FIGHT_CHALLENGE_WORDS_ID = setOf(
        "tantangan", "melawan", "menyerang", "serang", "berantem", "bertarung"
    )

    // --- Penanda tenang (penyeimbang skor intensitas) ---

    val CALM_MARKERS_EN = listOf(
        "rests", "stands motionless", "sigh of relief", "breathes calmly",
        "breathing calmly", "surveys", "watches", "turns toward", "breath steady"
    )

    // --- Verba berantem untuk skor (di luar verba energi big-attack) ---

    val FIGHT_VERBS_SCORER = setOf(
        "punch", "punches", "kick", "kicks", "slam", "slams", "strike", "strikes",
        "clash", "clashes", "attack", "attacks", "duel", "duels", "grapple",
        "grapples", "smash", "smashes", "hit", "hits", "fight", "fights",
        "battle", "battles"
    )

    // --- Kejatuhan antagonis (R8 lunak — belum dipakai gerbang) ---

    val FALL_WORDS_EN = setOf("falls", "collapses", "crumbles", "shatters", "defeated", "falling")
    val FALL_WORDS_ID = setOf("runtuh", "hancur", "jatuh", "roboh")

    // Kata larangan per beat untuk BLOK PROMPT (lebih ketat dari gerbang —
    // di prompt, false positive tidak masalah; yang penting LLM tidak
    // menulis kata itu sama sekali di beat yang salah).
    val PROMPT_FORBIDDEN_FINALITY_EN = "final, last, ultimate, peak, climax, decisive"
    const val PROMPT_FORBIDDEN_FINALITY_ID = "terakhir, final, pamungkas, puncak, menang, kemenangan, bebas, damai, tenang, kalah, terkalahkan, usai"
    const val PROMPT_FORBIDDEN_RESOLUTION_EN = "victory, victorious, free, freedom, peace, calm, defeat, defeated, triumph"
    const val PROMPT_FORBIDDEN_RESOLUTION_ID = "kemenangan, bebas, damai, tenang, telah usai, mereda, terkalahkan"

    private fun tokensOf(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

    private fun tokenSetOf(text: String): Set<String> = tokensOf(text).toSet()

    /** Stemmer Indonesia minimal untuk gerbang: buang SATU prefiks
     *  (ke/pe/me/ber/ter/di/se) + SATU sufiks (nya/an/kan/i), lalu cocokkan.
     *  "kedamaian" -> "damai" (kena), "menangis" -> "nangis" (TIDAK kena
     *  "menang"), "terakhirnya" -> "terakhir", "kemenangan" -> "menang".
     *  Bahasa Inggris: cocok persis + jamak -s. */
    private fun stemMatches(token: String, word: String): Boolean {
        if (token == word) return true
        // Inggris: jamak -s ("victories" TIDAK dicocokkan — bentuknya beda).
        if (token.length == word.length + 1 && token.endsWith("s") && token.dropLast(1) == word) return true
        // Indonesia: satu prefiks + satu sufiks.
        var stem = token
        val prefixes = listOf("ke", "pe", "me", "ber", "ter", "di", "se")
        for (p in prefixes) {
            if (stem.startsWith(p) && stem.length > p.length + 2) {
                stem = stem.substring(p.length)
                break
            }
        }
        val suffixes = listOf("nya", "kan", "an", "i")
        for (s in suffixes) {
            if (stem.endsWith(s) && stem.length > s.length + 2) {
                stem = stem.substring(0, stem.length - s.length)
                break
            }
        }
        return stem == word
    }

    private fun findWords(text: String, words: Set<String>): List<String> {
        val tokens = tokensOf(text)
        return words.filter { w -> tokens.any { t -> stemMatches(t, w) } }
    }

    private fun findPhrases(text: String, phrases: List<String>): List<String> {
        val lower = " " + text.lowercase().replace(Regex("[^a-z0-9]+"), " ") + " "
        return phrases.filter { phrase -> lower.contains(" $phrase ") }
    }

    /** Semua klaim finalitas di teks (kata + frasa, EN + ID). */
    fun findFinality(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return findWords(text, FINALITY_WORDS_EN + FINALITY_WORDS_ID) +
            findPhrases(text, FINALITY_PHRASES_EN + FINALITY_PHRASES_ID)
    }

    /** Semua klaim "awal/pertama" di teks (frasa saja — kata "first"
     *  ambigu: "first light of dawn" bukan klaim awal cerita). */
    fun findFirstness(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return findPhrases(text, FIRSTNESS_PHRASES_EN + FIRSTNESS_PHRASES_ID)
    }

    /** Semua klaim resolusi di teks (kata + frasa, EN + ID). */
    fun findResolution(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return findWords(text, RESOLUTION_WORDS_EN + RESOLUTION_WORDS_ID) +
            findPhrases(text, RESOLUTION_PHRASES_EN + RESOLUTION_PHRASES_ID)
    }

    /** Kata berantem/tantangan di teks aksi. */
    fun findFightChallenge(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return findWords(text, FIGHT_CHALLENGE_WORDS_EN + FIGHT_CHALLENGE_WORDS_ID)
    }

    /** Pola serangan besar: (modifier ATAU verba energi) + nomina serang,
     *  di mana pun dalam aksi. Nomina dicocokkan dengan jamak -s/-es
     *  ("massive beams collide" kena lewat "beams" -> "beam"). "slamming
     *  his obsidian spear" TIDAK kena (slam bukan verba energi — itu
     *  entrance, bukan serangan besar); "hurls a blazing plasma wave" kena
     *  (hurls + wave). */
    fun isBigAttack(actionEn: String): Boolean {
        val tokens = tokenSetOf(actionEn)
        val hasTrigger = tokens.any { it in BIG_MODIFIERS || it in BIG_ENERGY_VERBS }
        if (!hasTrigger) return false
        return tokens.any { token ->
            BIG_ATTACK_NOUNS.any { noun ->
                token == noun || token == noun + "s" || token == noun + "es"
            }
        }
    }

    /** Jurus bernama: frasa Kapital di tengah kalimat yang bukan nama
     *  hero/antagonis/dunia dan bukan bagian dari deskripsi yang sudah
     *  ada di bible (anchor hero, deskripsi dunia/cahaya). "Tempest
     *  Breaker" dan "Kamehameha" kena; "Dragon Ball" TIDAK (bagian dari
     *  anchor hero); "Golden Demon" TIDAK (nama antagonis); "Broly" di
     *  awal kalimat TIDAK (posisi kalimat). */
    fun extractNamedMove(actionEn: String, bible: StoryBible): String? {
        if (actionEn.isBlank()) return null
        val exclusions = (
            listOf(bible.hero?.name, bible.antagonistName, bible.worldName) +
                (bible.hero?.anchorsEn.orEmpty()) +
                listOf(bible.antagonistDescEn, bible.worldDescEn, bible.lightEn)
            ).mapNotNull { it?.lowercase()?.trim() }.filter { it.isNotBlank() }
        val sentences = actionEn.split(Regex("(?<=[.!])\\s+"))
        for (sentence in sentences) {
            val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }
            var i = 0
            while (i < words.size) {
                // Bersihkan tanda baca di ujung kata — termasuk "." (nama
                // jurus di ujung kalimat: "Tempest Breaker." -> "Tempest
                // Breaker") dan posesif "'s" ("Golden Frieza's" -> "Golden
                // Frieza" — nama antagonis posesif BUKAN jurus; tanpa ini
                // Kaito S17 salah kena R4b sebagai "golden frieza's").
                val clean = words[i].trim(',', ';', ':', '"', '\'', '.')
                    .removeSuffix("'s").removeSuffix("’s")
                val isCapitalized = clean.length > 1 && clean[0].isUpperCase() && clean[0].isLetter()
                if (i > 0 && isCapitalized) {
                    // Kumpulkan frasa Kapital berurutan ("Tempest Breaker").
                    val phrase = mutableListOf(clean)
                    var j = i + 1
                    while (j < words.size) {
                        val next = words[j].trim(',', ';', ':', '"', '\'', '.')
                            .removeSuffix("'s").removeSuffix("’s")
                        if (next.length > 1 && next[0].isUpperCase() && next[0].isLetter()) {
                            phrase.add(next)
                            j++
                        } else break
                    }
                    val candidate = phrase.joinToString(" ")
                    val lower = candidate.lowercase()
                    val excluded = exclusions.any { ex ->
                        ex == lower || ex.contains(lower) || lower == ex.split(" ").lastOrNull().orEmpty()
                    }
                    if (!excluded) return candidate
                    i = j
                } else {
                    i++
                }
            }
        }
        return null
    }

    /** Entrance villain: [nama antagonis] + verba masuk + preposisi arah.
     *  "Rudra descends from the swirling dark clouds" kena; "Rudra's broken
     *  armor crumbles" TIDAK (tak ada verba masuk). Nama pendek (token
     *  terakhir nama, "Frieza" dari "Golden Frieza") juga dikenali. */
    fun isVillainEntrance(actionEn: String, antagonistName: String): Boolean {
        if (actionEn.isBlank() || antagonistName.isBlank()) return false
        val tokens = tokensOf(actionEn)
        val nameTokens = tokensOf(antagonistName)
        if (nameTokens.isEmpty()) return false
        val variants = listOf(nameTokens, listOf(nameTokens.last()))
        for (variant in variants) {
            var start = 0
            while (start + variant.size <= tokens.size) {
                if (tokens.subList(start, start + variant.size) == variant) {
                    val after = tokens.subList(start + variant.size, tokens.size)
                    val verbIdx = after.indexOfFirst { it in ENTRANCE_VERBS }
                    if (verbIdx >= 0 && verbIdx <= 6) {
                        val window = after.subList(verbIdx + 1, minOf(verbIdx + 4, after.size))
                        if (window.any { it in ENTRANCE_PREPOSITIONS }) return true
                    }
                }
                start++
            }
        }
        return false
    }

    /** Skor intensitas satu scene (aksi + narasi). Big attack maks 1/scene.
     *  Kalibrasi lapangan (dihitung ulang dari teks VERBATIM export, bukan
     *  dari ingatan riset — riset sempat salah di dua titik, dan skor sempat
     *  salah hitung dua kali lagi saat kalibrasi):
     *  - Bima S3 = 12 (big 3 + named 1 + finalitas 6 + "triumph" 2).
     *    "terakhir" menghitung GANDA: kata "terakhir" + kata "akhir"
     *    (stemmer: "terakhir" -> "akhir") + frasa "serangan terakhir".
     *    Ini quirk stemmer yang disengaja dibiarkan (R1/R3 boolean tidak
     *    terpengaruh — hanya magnitudo skor; arahnya konservatif).
     *  - Bima Klimaks maks = 3 (S15); Bima S17 = 5 ("terakhirnya" hanya
     *    cocok "akhir" — sufiks "nya" memutus cocok "terakhir").
     *  - Kaito S3 = 6; Kaito S16 = 6; Kaito S17 = 11 (big 3 + finalitas 2
     *    + resolusi 6 — "Kemenangan" ganda: "menang" + "kemenangan" —
     *    dan TANPA named move: "Golden Frieza's" posesif dikecualikan).
     *  - Kaito Klimaks maks = 3 (S14); pra-Klimaks Kaito maks = 7 (S9 —
     *    "mencapai puncaknya" = kata "puncak" + frasa, +4).
     *  - S16 Kaito adalah PENUTUP, bukan Klimaks (riset sempat
     *    menghitungnya sebagai Klimaks dan menyimpulkan R6 Kaito "lolos
     *    tepat" — SALAH: pra-Klimaks 7 > Klimaks 3, R6a gagal). */
    fun intensityScore(actionEn: String, narrationId: String, bible: StoryBible): Int {
        var score = 0
        if (isBigAttack(actionEn)) score += 3
        if (extractNamedMove(actionEn, bible) != null) score += 1
        val combined = "$actionEn $narrationId"
        score += 2 * findFinality(combined).size
        score += 2 * findResolution(combined).size
        val actionTokens = tokensOf(actionEn)
        score += actionTokens.count { it in FIGHT_VERBS_SCORER }
        val lowerAction = " " + actionEn.lowercase().replace(Regex("[^a-z0-9]+"), " ") + " "
        score -= CALM_MARKERS_EN.count { lowerAction.contains(" $it ") }
        return maxOf(0, score)
    }
}

object BeatMap {

    // Bobot durasi per beat (fraksi film). 5 beat: pembuka pendek, eskalasi
    // terpanjang, klimaks + penutup = ~1/3 akhir. 8 beat (Story Circle):
    // pola yang sama, diskalakan. Jumlah bobot 5 = 18, bobot 8 = 20.
    private val WEIGHTS_5 = listOf(3, 4, 5, 3, 3)
    private val WEIGHTS_8 = listOf(2, 3, 3, 3, 3, 2, 2, 2)

    /** Peran beat dari posisinya. Klimaks dicari dari NAMA dulu ("Klimaks")
     *  — bible tes 4-beat (Pembuka/Eskalasi/Klimaks/Penutup) punya Klimaks
     *  di indeks 2, bukan 3. Kalau nama tidak dikenali, pakai posisi
     *  (assignEffects: indeks 3 untuk ≤5 beat, 5 untuk >5 beat). */
    fun climaxIndex(beats: List<V2Beat>): Int {
        if (beats.isEmpty()) return 3
        beats.indexOfFirst { it.name.contains("klimak", ignoreCase = true) }
            .takeIf { it >= 0 }?.let { return it }
        return if (beats.size <= 5) minOf(3, beats.size - 1) else 5
    }

    fun closingIndex(beats: List<V2Beat>): Int {
        if (beats.isEmpty()) return 4
        beats.indexOfFirst {
            it.name.contains("penutup", ignoreCase = true) ||
                it.name.contains("closing", ignoreCase = true) ||
                it.name.contains("aftermath", ignoreCase = true)
        }.takeIf { it >= 0 }?.let { return it }
        return beats.size - 1
    }

    fun roleOf(beatIndex: Int, beats: List<V2Beat>): BeatRole {
        val climax = climaxIndex(beats)
        val closing = closingIndex(beats)
        return when {
            beatIndex == climax -> BeatRole.CLIMAX
            beatIndex == closing -> BeatRole.CLOSING
            beatIndex == 0 -> BeatRole.OPENING
            beatIndex > climax && beatIndex < closing -> BeatRole.FALLING
            // Paruh pertama pra-klimaks = EARLY (Pemicu), paruh kedua = LATE
            // (Eskalasi): 5 beat (klimax 3) -> beat 1 EARLY, beat 2 LATE.
            beatIndex * 2 <= climax -> BeatRole.EARLY
            else -> BeatRole.LATE
        }
    }

    /** Intensitas 1..5 untuk teks blok beat (tampilan prompt, bukan gerbang).
     *  5 beat kanonik: 1, 2, 3, 5, 1 (Pembuka/Pemicu/Eskalasi/Klimaks/Penutup). */
    fun intensityFor(beatIndex: Int, beats: List<V2Beat>): Int {
        return when (roleOf(beatIndex, beats)) {
            BeatRole.CLIMAX -> 5
            BeatRole.CLOSING -> 1
            BeatRole.OPENING -> 1
            BeatRole.FALLING -> 2
            BeatRole.EARLY -> 2
            BeatRole.LATE -> 3
        }
    }

    /** Maks serangan besar per beat (aturan R4). */
    fun maxBigAttacks(beatIndex: Int, beats: List<V2Beat>): Int {
        return when (roleOf(beatIndex, beats)) {
            BeatRole.OPENING -> 0
            BeatRole.EARLY -> 1
            BeatRole.LATE -> 2
            BeatRole.CLIMAX -> 3
            BeatRole.FALLING -> 0
            BeatRole.CLOSING -> 0
        }
    }

    /** Bangun peta beat→scene. Largest remainder: bobot × total, dibulatkan
     *  ke bawah, sisa dibagikan satu-per-satu ke beat berbobot terbesar
     *  (seri: indeks terkecil). Rentang kontigu; beat terakhir berakhir
     *  tepat di totalScenes. Untuk 18 scene/5 beat: [3,4,5,3,3] → Pembuka
     *  1–3, Pemicu 4–7, Eskalasi 8–12, Klimaks 13–15, Penutup 16–18 —
     *  SAMA dengan climaxFirstScene=13 yang sudah dihitung assignEffects. */
    fun build(totalScenes: Int, beats: List<V2Beat>): List<BeatRange> {
        val n = beats.size
        if (n == 0) {
            val names = listOf("Pembuka", "Pemicu", "Eskalasi", "Klimaks", "Penutup")
            return build(totalScenes, names.mapIndexed { i, name -> V2Beat(i, name, "") })
        }
        val weights: List<Int> = when (n) {
            5 -> WEIGHTS_5
            8 -> WEIGHTS_8
            else -> {
                val climax = climaxIndex(beats)
                List(n) { i ->
                    var w = 1
                    if (i == climax) w += 1
                    if (i == climax - 1) w += 1
                    w
                }
            }
        }
        val weightSum = weights.sum()
        val exact = weights.map { it.toDouble() * totalScenes / weightSum }
        val floors = exact.map { it.toInt() }.toMutableList()
        var remainder = totalScenes - floors.sum()
        val order = weights.indices.sortedWith(
            compareByDescending<Int> { weights[it] }.thenBy { it }
        )
        var oi = 0
        while (remainder > 0 && order.isNotEmpty()) {
            floors[order[oi % order.size]] += 1
            remainder--
            oi++
        }
        val ranges = mutableListOf<BeatRange>()
        var scene = 1
        for (i in 0 until n) {
            val count = floors[i].coerceAtLeast(1)
            val first = scene
            val last = (scene + count - 1).coerceAtMost(totalScenes)
            ranges.add(
                BeatRange(
                    beatIndex = i,
                    name = beats[i].name.ifBlank { "Beat ${i + 1}" },
                    firstScene = first,
                    lastScene = last,
                    intensity = intensityFor(i, beats)
                )
            )
            scene = last + 1
            if (scene > totalScenes) {
                // Sisa beat (kalau ada) menempel di scene terakhir — tidak
                // boleh ada rentang kosong.
                for (j in i + 1 until n) {
                    ranges.add(
                        BeatRange(
                            beatIndex = j,
                            name = beats[j].name.ifBlank { "Beat ${j + 1}" },
                            firstScene = totalScenes,
                            lastScene = totalScenes,
                            intensity = intensityFor(j, beats)
                        )
                    )
                }
                break
            }
        }
        return ranges
    }

    fun beatForScene(scene: Int, map: List<BeatRange>): BeatRange =
        map.firstOrNull { it.contains(scene) } ?: map.last()
}
