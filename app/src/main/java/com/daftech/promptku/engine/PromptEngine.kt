package com.daftech.promptku.engine

// Mesin prompt PromptKu v1.0.0 — struktur 9 lapis DANCUK Cinematic v2.
// Satu input wizard -> empat varian output, masing-masing mengikuti
// FORMULA RESMI engine tujuannya (jangan disalin lintas engine):
// Flow/Veo: Cinematography + Subject + Action + Context + Style & Ambiance
// WanGP: Subject + Scene + Motion (I2V: gerakan + kamera saja)
// Kling: Subject + Action + Setting + Camera + Lighting + Mood
// Seedance: 8 elemen berurutan + timeline 3 segmen, tanpa negative prompt.
object PromptEngine {

    // Negative prompt resmi Wan 2.2 TI2V-5B — Mandarin verbatim, JANGAN diterjemahkan.
    // Text encoder Wan multilingual; versi terjemahan belum terbukti sama hasilnya.
    const val WAN_NEGATIVE_MANDARIN: String =
        "色调艳丽，过曝，静态，细节模糊不清，字幕，风格，作品，画作，画面，静止，整体发灰，最差质量，低质量，JPEG压缩残留，丑陋的，残缺的，多余的手指，画得不好的手部，画得不好的脸部，畸形的，毁容的，形态畸形的肢体，手指融合，静止不动的画面，杂乱的背景，三条腿，背景人很多，倒着走"

    // Kunci positif Flow/Veo — negative ditulis sebagai keadaan yang diinginkan,
    // bukan "no ..." (panduan resmi Google). Sengaja pendek (23 kata) supaya
    // tidak memakan anggaran kata prompt; kalimat ini DILINDUNGI dari trim.
    const val FLOW_POSITIVE_LOCK: String =
        "Same face and proportions throughout. Hands relaxed and natural. " +
        "Lighting and background unchanged. Clean frame, no text, no logos, no subtitles, no watermark."

    // Kunci positif Kling / Seedance — kedua engine ini tidak memakai field negative.
    const val POSITIVE_LOCK_SHORT: String =
        "Locked camera. The same character design stays consistent. Clean frame, no text, no logos."

    // Gaya visual -> (tampilan, grade warna, lensa). Satu grade lock per gaya,
    // dicopas identik di semua scene supaya film terasa satu.
    // Draft Offline v2 — frasa gaya yang menyebut tempat yang TIDAK ADA di
    // dunia cerita dibuang ("neon-lit streets" di stasiun luar angkasa,
    // cacat lapangan 16 Nov 2026). Hanya frasa tempat yang disaring;
    // tampilan/grade/lensa lain utuh. Dunia kosong = tanpa filter.
    private val STYLE_PLACE_TOKENS = listOf(
        "street", "alley", "rooftop", "harbor", "temple", "forest",
        "coffee shop", "promenade", "seaside", "city", "village",
        "market", "house"
    )

    fun styleLook(style: VisualStyle, worldText: String = ""): Triple<String, String, String> {
        val base = when (style) {
            VisualStyle.REALISTIC -> Triple(
                "live-action realistic, natural skin texture, realistic dirt, sweat and torn fabric",
                "teal-and-orange grade, subtle film grain",
                "shallow depth of field"
            )
            VisualStyle.ANIME -> Triple(
                "2D anime film style, cel shading, clean line art, expressive eyes, vibrant flat colors",
                "vivid saturated grade, clean cel colors",
                "35mm, sharp focus, anime film framing"
            )
            VisualStyle.PIXAR_3D -> Triple(
                "3D animated film style, soft rounded shapes, subsurface skin, expressive large eyes",
                "bright warm grade, soft global illumination",
                "35mm, deep focus"
            )
            VisualStyle.CYBERPUNK -> Triple(
                "cyberpunk film style, neon-lit streets, rain-slicked surfaces, holographic signs",
                "magenta-and-cyan grade, high contrast neon",
                "35mm anamorphic, anamorphic flare"
            )
            VisualStyle.NOIR -> Triple(
                "film noir style, high-contrast shadows, venetian blind light, rain on glass",
                "desaturated grade leaning monochrome, deep blacks",
                "50mm, deep focus"
            )
            VisualStyle.VINTAGE -> Triple(
                "vintage film style, worn 1970s color film, soft grain, faded colors",
                "faded warm grade, visible film grain, slight vignette",
                "50mm, soft focus"
            )
        }
        val world = worldText.lowercase()
        if (world.isBlank()) return base
        val look = base.first.split(", ").filter { phrase ->
            val lower = phrase.lowercase()
            STYLE_PLACE_TOKENS.none { token -> lower.contains(token) && !world.contains(token) }
        }.joinToString(", ")
        return Triple(look.ifBlank { base.first }, base.second, base.third)
    }

    // Dunia default per genre, dipakai kalau user belum menulis World Lock sendiri.
    fun defaultWorld(genre: Genre): String {
        return when (genre) {
            Genre.ACTION -> "a ruined city street at dusk, debris and smoke, collapsed buildings"
            Genre.DRAMA -> "a small quiet room at dusk, rain on the window, warm lamp light"
            Genre.ROMANSA -> "a seaside town at sunset, golden light on the water, empty promenade"
            Genre.HOROR -> "an old abandoned house at night, peeling walls, dust in the air"
            Genre.SCI_FI -> "a space station corridor, cold metal walls, blinking lights"
            Genre.FANTASI -> "an ancient forest at dawn, mist between tall trees, mossy stones"
            Genre.THRILLER -> "a narrow rainy alley at night, flickering street lamp, wet asphalt"
            Genre.KOMEDI -> "a busy small coffee shop in the morning, warm light, laughing customers"
            Genre.DOKUMENTER -> "a fishing harbor at dawn, wooden boats, mist over calm water"
            Genre.SEJARAH -> "an ancient stone temple courtyard at sunrise, carved pillars, incense smoke"
        }
    }

    fun build(input: ShotInput): GeneratedPrompt {
        val engine = input.engine ?: TargetEngine.FLOW
        return when (engine) {
            TargetEngine.FLOW -> buildFlow(input)
            TargetEngine.WAN -> buildWan(input)
            TargetEngine.KLING -> buildKling(input)
            TargetEngine.SEEDANCE -> buildSeedance(input)
        }
    }

    fun buildAll(input: ShotInput): List<GeneratedPrompt> {
        return TargetEngine.values().map { engine -> build(input.copy(engine = engine)) }
    }

    private fun warningsFor(input: ShotInput): List<String> {
        val found = mutableListOf<String>()
        found.addAll(MythFilter.detectLicensed(input.character?.name ?: ""))
        found.addAll(MythFilter.detectLicensed(input.secondCharacter?.name ?: ""))
        found.addAll(MythFilter.detectLicensed(input.actionEn))
        val distinct = found.distinct()
        if (distinct.isEmpty()) return emptyList()
        val suggestions = distinct.joinToString("; ") { name ->
            "$name -> ${MythFilter.genericSuggestion(name)}"
        }
        return listOf(
            "Peringatan filter IP: nama berlisensi bisa ditolak Flow/Veo. Saran generik: $suggestions. " +
            "Keputusan generate tetap di tanganmu — app tidak memblokir."
        )
    }

    private fun subjectLine(input: ShotInput): String {
        val main = input.character?.let { "${it.name}, ${it.descriptionEn}" } ?: "a person"
        val second = input.secondCharacter?.let { ", facing ${it.name}, ${it.descriptionEn}" } ?: ""
        return main + second
    }

    // Pangkas DNA di batas kalimat ke anggaran yang tersisa (v1.2.1).
    // DNA verbatim panjang (kasus Riko: 129 kata) duduk di tengah kalimat 1
    // dan membuat trim memotong dunia/kamera/cahaya/aksi/audio dari belakang.
    // Kembalikan DNA yang terpotong rapi + peringatan, atau DNA utuh + null.
    private fun capDnaAtSentence(
        dna: String,
        allowed: Int,
        name: String,
        maxWords: Int
    ): Pair<String, String?> {
        val fullWords = MythFilter.countWords(dna)
        if (fullWords <= allowed) return dna to null
        val sentences = dna.split(Regex("(?<=[.!])\\s+"))
        val kept = mutableListOf<String>()
        var count = 0
        for (sentence in sentences) {
            val w = MythFilter.countWords(sentence)
            if (count + w <= allowed) {
                kept.add(sentence)
                count += w
            } else {
                break
            }
        }
        var capped = kept.joinToString(" ")
        if (capped.isBlank()) {
            capped = dna.split(Regex("\\s+")).take(allowed).joinToString(" ")
            count = MythFilter.countWords(capped)
        }
        val warning = "DNA $name dipangkas dari $fullWords ke $count kata " +
            "supaya dunia, aksi, dan cahaya muat dalam $maxWords kata."
        return capped to warning
    }

    // Subject line dengan DNA dianggarkan (v1.2.1): DNA utama dulu, sisa
    // anggaran untuk karakter kedua. Kalau tidak ada sisa, karakter kedua
    // tetap disebut namanya tanpa deskripsi.
    private fun budgetedSubjectLine(
        input: ShotInput,
        dnaAllowed: Int,
        maxWords: Int
    ): Pair<String, String?> {
        val warnings = mutableListOf<String>()
        val main = input.character
        var remaining = dnaAllowed
        val mainText = if (main == null) {
            "a person"
        } else {
            val (capped, warning) = capDnaAtSentence(
                main.descriptionEn.trim(), remaining, main.name, maxWords
            )
            if (warning != null) warnings.add(warning)
            remaining -= MythFilter.countWords(capped)
            "${main.name}, $capped"
        }
        val secondText = input.secondCharacter?.let { second ->
            val (capped, warning) = capDnaAtSentence(
                second.descriptionEn.trim(), remaining.coerceAtLeast(0), second.name, maxWords
            )
            if (warning != null) warnings.add(warning)
            if (capped.isBlank()) ", facing ${second.name}" else ", facing ${second.name}, $capped"
        } ?: ""
        val warning = if (warnings.isEmpty()) null else warnings.joinToString(" ")
        return (mainText + secondText) to warning
    }

    // Subject line v1.3 — kartu 3 jangkar menggantikan DNA verbatim bila
    // terisi: kartu (~15 kata) tidak pernah kena cap DNA, jadi tidak ada
    // lagi pemangkasan diam-diam di subject. Karakter kedua tetap memakai
    // sisa anggaran DNA seperti biasa.
    // v1.4.1 — kartu di subject line TANPA gestur (gestur pindah ke
    // klausa terpisah, lihat gestureSentence).
    private fun subjectLineFor(
        input: ShotInput,
        dnaAllowed: Int,
        maxWords: Int
    ): Pair<String, String?> {
        val card = input.characterCard
        if (card != null && !card.isEmpty() && input.character != null) {
            val cardInput = input.copy(
                character = input.character.copy(descriptionEn = card.subjectDescription())
            )
            return budgetedSubjectLine(cardInput, dnaAllowed, maxWords)
        }
        return budgetedSubjectLine(input, dnaAllowed, maxWords)
    }

    private fun shortDna(input: ShotInput): String {
        val card = input.characterCard
        if (card != null && !card.isEmpty()) {
            return "${input.character?.name ?: "a person"}, ${card.subjectDescription()}"
        }
        val dna = input.character?.descriptionEn ?: ""
        val words = dna.split(Regex("\\s+")).take(12).joinToString(" ")
        return "${input.character?.name ?: "a person"}, $words"
    }

    // v1.4.1 — default material mengikuti dunia, bukan selalu debu
    // (cacat lapangan 16 Nov: "dust drifting" di 30/30 scene kota
    // neon hujan). Draft Offline v2 menambah stasiun/hutan/kuil
    // (cacat lapangan 16 Nov 23:35: "dust drifts" di stasiun luar angkasa).
    fun materialDefaultFor(worldDescEn: String): String {
        val lower = worldDescEn.lowercase()
        return when {
            lower.contains("space station") || lower.contains("spacecraft") ||
                lower.contains("spaceship") || lower.contains("orbit") ->
                "soft machine hum fills the recycled air, console lights blink steadily"
            lower.contains("forest") || lower.contains("trees") ->
                "mist drifts between the tall trees, leaves rustle softly"
            lower.contains("temple") || lower.contains("incense") ->
                "incense smoke curls slowly through the still air"
            lower.contains("rain") -> "rain streaks the air, mist drifts low over the wet ground"
            lower.contains("snow") -> "snowflakes drift slowly through the cold air"
            lower.contains("night") && lower.contains("city") -> "mist drifts through the neon light, rain on the wet ground"
            lower.contains("sea") || lower.contains("beach") -> "sea spray drifts in the salty wind"
            lower.contains("desert") -> "sand drifts slowly through the hot air"
            else -> "dust drifts slowly through the air"
        }
    }

    // v1.4.1 — gestur kartu sebagai klausa karakter terpisah (identitas,
    // bukan pose saat ini). Null kalau kartu tidak punya gestur.
    private fun gestureSentence(input: ShotInput): String? {
        val gesture = input.characterCard?.gesture?.trim().orEmpty()
        return if (gesture.isBlank()) null else "Signature gesture: $gesture."
    }

    private fun worldText(input: ShotInput): String {
        return input.world?.descriptionEn ?: defaultWorld(input.genre ?: Genre.DRAMA)
    }

    // Versi pendek dunia (12 kata pertama) untuk engine berbatas 100 kata.
    private fun shortWorld(input: ShotInput): String {
        return worldText(input).split(Regex("\\s+")).take(12).joinToString(" ")
    }

    // Versi pendek cahaya untuk engine berbatas 100 kata.
    private fun shortLight(light: LightKind): String {
        return when (light) {
            LightKind.GOLDEN_HOUR -> "low golden-hour sun, warm backlight"
            LightKind.VOLUMETRIC -> "volumetric god rays through dust"
            LightKind.REMBRANDT -> "hard Rembrandt side light"
            LightKind.CHIAROSCURO -> "single hard side light"
            LightKind.RIM -> "strong rim light"
            LightKind.MOONLIGHT -> "cold blue moonlight"
            LightKind.NEON -> "magenta and cyan neon"
            LightKind.OVERCAST -> "soft overcast daylight"
            LightKind.CANDLE -> "warm candlelight"
            LightKind.STUDIO_SOFT -> "soft studio light"
        }
    }

    // Satu kalimat dalam rakitan beranggaran: kalimat wajib tidak pernah
    // dibuang; kalimat opsional dibuang dari prioritas tertinggi (dropPriority
    // terbesar dibuang duluan) sampai total kata muat. Urutan kalimat di
    // output tetap urutan narasi, bukan urutan prioritas.
    private data class Sentence(
        val text: String,
        val mandatory: Boolean,
        val dropPriority: Int
    )

    private fun assembleOrdered(
        sentences: List<Sentence>,
        tail: String,
        maxWords: Int
    ): String {
        val kept = sentences.toMutableList()
        var count = MythFilter.countWords((kept.map { it.text } + tail).joinToString(" "))
        while (count > maxWords) {
            val victim = kept.filter { !it.mandatory }.maxByOrNull { it.dropPriority } ?: break
            kept.remove(victim)
            count = MythFilter.countWords((kept.map { it.text } + tail).joinToString(" "))
        }
        return (kept.map { it.text } + tail).joinToString(" ")
    }

    // Render dialog v1.3 — DialogueLine first-class (ID/EN + nada).
    // Tanpa DialogueLine, jatuh ke field lama dialogueName/dialogueLine
    // (template + tes lama + wizard v1) dengan perilaku lama yang sama.
    // Flow: `Nama says, in [tone]: "teks"` (nada dulu, lalu kalimat);
    // Kling: `Nama (tone): "teks"` (tone default "determined");
    // Seedance ID: `Language: Indonesian. Nama says, in [tone]: teks`;
    // WanGP: "" (bisu — dialog ke Lembar Suara, bukan prompt).
    fun renderDialogue(input: ShotInput, engine: TargetEngine): String {
        val line = input.dialogue
        if (line != null && !line.isEmpty()) {
            val name = line.speaker.ifBlank { input.dialogueName.ifBlank { "The character" } }
            val text = line.activeText().trim()
            if (text.isBlank()) return ""
            val tone = line.tone.trim()
            return when (engine) {
                TargetEngine.WAN -> ""
                TargetEngine.KLING -> {
                    val toneText = tone.ifBlank { "determined" }
                    "$name ($toneText): \"$text\""
                }
                TargetEngine.SEEDANCE -> {
                    val toneText = tone.ifBlank { "a calm voice" }
                    if (line.language == DialogueLang.ID) {
                        "Language: Indonesian. $name says, in $toneText: $text"
                    } else {
                        "$name says, in $toneText: \"$text\""
                    }
                }
                TargetEngine.FLOW -> {
                    if (tone.isBlank()) {
                        "$name says, \"$text\""
                    } else {
                        "$name says, in $tone: \"$text\""
                    }
                }
            }
        }
        if (input.dialogueLine.isBlank()) return ""
        val name = input.dialogueName.ifBlank { "The character" }
        val text = input.dialogueLine.trim()
        return when (engine) {
            TargetEngine.WAN -> ""
            TargetEngine.KLING -> "$name (determined): \"$text\""
            TargetEngine.SEEDANCE -> "$name says, \"$text\""
            TargetEngine.FLOW -> "$name says, \"$text\""
        }
    }

    private fun dialogueFlow(input: ShotInput): String {
        val rendered = renderDialogue(input, TargetEngine.FLOW)
        return if (rendered.isBlank()) "" else " $rendered"
    }

    private fun dialogueKling(input: ShotInput): String {
        val rendered = renderDialogue(input, TargetEngine.KLING)
        return if (rendered.isBlank()) "" else " $rendered"
    }

    private fun dialogueSeedance(input: ShotInput): String {
        val rendered = renderDialogue(input, TargetEngine.SEEDANCE)
        return if (rendered.isBlank()) "" else " $rendered"
    }

    // Frasa kontinuitas v1.2 — hanya muncul bila field terisi (Mode Naskah).
    // Arah layar + sisi axis menjaga tokoh tidak berlari ke arah berlawanan
    // antar klip (aturan 180 derajat); transisi menandai sambungan antar shot.
    private fun continuityPhrase(input: ShotInput): String {
        val parts = mutableListOf<String>()
        // v1.4.1 — rantai pose tinggal di klausa kontinuitas (bukan di
        // timeline aksi): "Starts from X, ends as Y". Kalau pose awal =
        // pose akhir, dilewati (pose statis, aksi yang bicara).
        val start = input.poseStart.trim()
        val end = input.poseEnd.trim()
        if (start.isNotBlank() && end.isNotBlank() && start != end) {
            parts.add("Starts from $start, ends as $end")
        }
        if (input.screenDirection.isNotBlank()) {
            parts.add("Screen direction: ${input.screenDirection.trim()}")
        }
        if (input.axisSide.isNotBlank()) {
            parts.add("camera stays on the ${input.axisSide.trim()} side of the axis")
        }
        if (input.transition.isNotBlank()) {
            parts.add("Transition: ${input.transition.trim()}")
        }
        return parts.joinToString(", ")
    }

    // protectedTail: kalimat kunci (lock / constraints) yang TIDAK BOLEH
    // terpotong trim. Kalau teks kepanjangan, yang dipangkas hanya bagian
    // kepala — tail selalu dipertahankan utuh di akhir.
    private fun finish(
        engine: TargetEngine,
        raw: String,
        negative: String?,
        input: ShotInput,
        protectedTail: String? = null,
        extraWarnings: List<String> = emptyList(),
        meter: String = ""
    ): GeneratedPrompt {
        val filtered = MythFilter.filterMyths(raw)
        val trimmed = if (protectedTail != null && filtered.endsWith(protectedTail)) {
            val head = filtered.substring(0, filtered.length - protectedTail.length).trim()
            val headBudget = engine.maxWords - MythFilter.countWords(protectedTail)
            val trimmedHead = MythFilter.trimToWords(head, headBudget)
            "$trimmedHead $protectedTail"
        } else {
            MythFilter.trimToWords(filtered, engine.maxWords)
        }
        val warnings = warningsFor(input).toMutableList()
        warnings.addAll(extraWarnings)
        // Laporan trim v1.2.1 — tidak ada lagi pemotongan diam-diam (bug Riko):
        // kalau trim membuang kalimat, user diberitahu kalimat apa yang hilang.
        val beforeCount = MythFilter.countWords(filtered)
        val afterCount = MythFilter.countWords(trimmed)
        if (afterCount < beforeCount) {
            val dropped = filtered.split(Regex("(?<=[.!])\\s+"))
                .filter { sentence -> !trimmed.contains(sentence) }
                .map { it.split(Regex("\\s+")).take(6).joinToString(" ") }
            val detail = if (dropped.isEmpty()) "" else " Yang hilang: ${dropped.joinToString("; ")}…"
            warnings.add(
                "Prompt dipangkas dari $beforeCount ke $afterCount kata supaya muat dalam ${engine.maxWords} kata.$detail " +
                "Periksa dunia, aksi, dan audio — pendekkan DNA atau dunia kalau sering terjadi."
            )
        }
        val count = afterCount
        if (count < engine.minWords) {
            warnings.add(
                "Prompt hanya $count kata (target ${engine.minWords}-${engine.maxWords}). " +
                "Tambah detail aksi, dunia, atau cahaya supaya model tidak menebak-nebak."
            )
        }
        if (engine == TargetEngine.WAN &&
            (input.dialogueLine.isNotBlank() ||
                (input.dialogue != null && !input.dialogue.isEmpty()))
        ) {
            warnings.add("Wan 2.2 tanpa audio: dialog tidak dimasukkan ke prompt.")
        }
        return GeneratedPrompt(engine, trimmed, negative, count, warnings, meter)
    }

    // Flow / Veo — kamera ditulis PERTAMA, dua beat timestamp, audio di baris sendiri.
    // v1.4.1 — timeline HANYA aksi ("[00:00-00:05] $action."), pose
    // pindah ke klausa kontinuitas ("Starts from X, ends as Y"); aksi
    // TIDAK diulang mentah di beat kedua ("Ends as $poseEnd.").
    private fun buildFlow(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style, worldText(input))
        val shot = input.shotSize?.phraseEn ?: "medium shot"
        val angle = input.angle?.phraseEn ?: "eye level"
        val lens = "${input.lensMm ?: 35}mm"
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light?.phraseEn ?: LightKind.GOLDEN_HOUR.phraseEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { materialDefaultFor(worldText(input)) }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val hide = if (input.hideTactic != HideTactic.NONE) {
            " ${input.hideTactic.phraseEn}."
        } else {
            ""
        }
        val continuity = continuityPhrase(input)
        val gesture = gestureSentence(input)
        // Anggaran DNA v1.2.1 (bug Riko): hitung kata di luar DNA dulu, sisa
        // anggaran untuk DNA. Lantai 25 kata menjaga 1-2 kalimat pertama DNA
        // (ciri wajah paling penting) tetap ada; kalau dunia/aksi juga
        // kepanjangan, laporan trim di finish() yang memberi tahu user.
        // v2.1 fix5 — "in ${worldText}" berasumsi dunia adalah frasa; sejak
        // fix4 dunia adalah KALIMAT ("Lightning splits...") — "in Lightning
        // splits..." tidak gramatikal. Dunia jadi kalimat sendiri
        // ("Setting: ..."), cahaya kalimat sendiri ("Lighting: ...").
        // WAJIB identik dengan Sentence di bawah (anggaran kata DNA).
        val s1WithoutDna =
            "$shot, $angle, $lens, ${look.first}. ${input.character?.name ?: "a person"}. Setting: ${worldText(input)}. $camera. Lighting: $light.$hide"
        val otherWords = MythFilter.countWords(
            listOf(
                s1WithoutDna,
                "[00:00-00:05] $action.",
                "$material.",
                "[00:05-00:10] Ends as $poseEnd. ${look.second}, ${look.third}.",
                "Audio: $audio.${dialogueFlow(input)}",
                continuity,
                gesture ?: "",
                FLOW_POSITIVE_LOCK
            ).joinToString(" ")
        )
        val dnaAllowed = (TargetEngine.FLOW.maxWords - otherWords).coerceAtLeast(25)
        val subject = subjectLineFor(input, dnaAllowed, TargetEngine.FLOW.maxWords)
        val sentences = listOf(
            Sentence(
                "$shot, $angle, $lens, ${look.first}. ${subject.first}. Setting: ${worldText(input)}. $camera. Lighting: $light.$hide",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "[00:00-00:05] $action.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "$material.",
                mandatory = false,
                dropPriority = 3
            ),
            Sentence(
                "[00:05-00:10] Ends as $poseEnd. ${look.second}, ${look.third}.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "Audio: $audio.${dialogueFlow(input)}",
                mandatory = true,
                dropPriority = 0
            )
        ) + if (continuity.isNotBlank()) {
            listOf(Sentence("$continuity.", mandatory = false, dropPriority = 4))
        } else {
            emptyList()
        } + if (gesture != null) {
            listOf(Sentence(gesture, mandatory = false, dropPriority = 5))
        } else {
            emptyList()
        }
        val raw = assembleOrdered(sentences, FLOW_POSITIVE_LOCK, TargetEngine.FLOW.maxWords)
        return finish(
            TargetEngine.FLOW, raw, null, input, FLOW_POSITIVE_LOCK,
            extraWarnings = listOfNotNull(subject.second),
            meter = PromptCore.meterLine(input, TargetEngine.FLOW)
        )
    }

    // WanGP TI2V-5B — gambar first-frame sudah mengunci subjek dan scene,
    // jadi teks hanya membawa GERAKAN + KAMERA. Target 80-120 kata.
    // Batas keras encoder 512 token: info penting ditulis di awal.
    private fun buildWan(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style, worldText(input))
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light?.phraseEn ?: LightKind.GOLDEN_HOUR.phraseEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val raw = StringBuilder()
        raw.append("${shortDna(input)}. Setting: ${worldText(input)}. ")
        // v1.4.1 — gestur kartu sebagai klausa terpisah (bukan di subject).
        gestureSentence(input)?.let { raw.append("$it ") }
        raw.append("$poseStart, then $action, ending $poseEnd. ")
        raw.append("$camera. Lit with $light. ${look.first}. ${look.second}.")
        val continuity = continuityPhrase(input)
        if (continuity.isNotBlank()) {
            raw.append(" $continuity.")
        }
        var text = MythFilter.filterMyths(raw.toString())
        if (MythFilter.countWords(text) < 80) {
            // v1.4.1 — material mengikuti dunia (bukan selalu debu).
            val mat = input.materialEn.ifBlank {
                materialDefaultFor(worldText(input)).replaceFirstChar { it.titlecase() }
            }
            val extra = " $mat. The same character design stays consistent in every frame."
            text = MythFilter.filterMyths(text + extra)
        }
        return finish(
            TargetEngine.WAN, text, WAN_NEGATIVE_MANDARIN, input,
            meter = PromptCore.meterLine(input, TargetEngine.WAN)
        )
    }

    // Kling — Subject + Action + Setting + Camera + Lighting + Mood.
    // Batas 100 kata: DNA dipendekkan (nama + ciri kunci) karena identitas
    // di Kling dibawa Element Reference, bukan deskripsi panjang.
    private fun buildKling(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style, worldText(input))
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light ?: LightKind.GOLDEN_HOUR
        val mood = (input.genre ?: Genre.DRAMA).moodEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { materialDefaultFor(worldText(input)) }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val secondName = input.secondCharacter?.let { ", facing ${it.name}" } ?: ""
        val continuity = continuityPhrase(input)
        val gesture = gestureSentence(input)
        val sentences = listOf(
            Sentence(
                "${shortDna(input)}$secondName. Setting: ${shortWorld(input)}.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "$poseStart, then $action, ending $poseEnd.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence("$material.", mandatory = false, dropPriority = 3),
            Sentence("$camera.", mandatory = true, dropPriority = 0),
            Sentence(
                "Lit with ${shortLight(light)}. ${look.first}, ${look.second}.",
                mandatory = false,
                dropPriority = 2
            ),
            Sentence("Mood: $mood.", mandatory = true, dropPriority = 0),
            Sentence(
                "Audio: $audio.${dialogueKling(input)}",
                mandatory = true,
                dropPriority = 0
            )
        ) + if (continuity.isNotBlank()) {
            listOf(Sentence("$continuity.", mandatory = false, dropPriority = 4))
        } else {
            emptyList()
        } + if (gesture != null) {
            listOf(Sentence(gesture, mandatory = false, dropPriority = 5))
        } else {
            emptyList()
        }
        val raw = assembleOrdered(sentences, POSITIVE_LOCK_SHORT, TargetEngine.KLING.maxWords)
        return finish(
            TargetEngine.KLING, raw, null, input, POSITIVE_LOCK_SHORT,
            meter = PromptCore.meterLine(input, TargetEngine.KLING)
        )
    }

    // Seedance — 8 elemen berurutan + timeline 3 segmen, tanpa negative prompt.
    // Eksklusi ditulis sebagai constraints (3-5), bukan field negative.
    // Constraints DILINDUNGI dari trim (tail), sama seperti lock Flow.
    private fun buildSeedance(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style, worldText(input))
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light ?: LightKind.GOLDEN_HOUR
        val mood = (input.genre ?: Genre.DRAMA).moodEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { materialDefaultFor(worldText(input)) }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val secondName = input.secondCharacter?.let { ", facing ${it.name}" } ?: ""
        val tail = "Constraints: one action only, one camera move only, no text on screen, " +
            "consistent character design, no extra limbs."
        val gesture = gestureSentence(input)
        val sentences = listOf(
            Sentence(
                "${shortDna(input)}$secondName. ${look.first}.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "Setting: ${shortWorld(input)}. Light: ${shortLight(light)}. Mood: $mood.",
                mandatory = false,
                dropPriority = 2
            ),
            Sentence("[0-2s] $poseStart, $camera.", mandatory = true, dropPriority = 0),
            Sentence(
                "[2-4s] $action.${dialogueSeedance(input)}",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "[4-6s] Ends as $poseEnd. Audio: $audio.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence("$material.", mandatory = false, dropPriority = 3)
        ) + if (continuityPhrase(input).isNotBlank()) {
            listOf(Sentence("${continuityPhrase(input)}.", mandatory = false, dropPriority = 4))
        } else {
            emptyList()
        } + if (gesture != null) {
            listOf(Sentence(gesture, mandatory = false, dropPriority = 5))
        } else {
            emptyList()
        }
        val raw = assembleOrdered(sentences, tail, TargetEngine.SEEDANCE.maxWords)
        return finish(
            TargetEngine.SEEDANCE, raw, null, input, tail,
            meter = PromptCore.meterLine(input, TargetEngine.SEEDANCE)
        )
    }
}
