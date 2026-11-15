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
    fun styleLook(style: VisualStyle): Triple<String, String, String> {
        return when (style) {
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

    private fun shortDna(input: ShotInput): String {
        val dna = input.character?.descriptionEn ?: ""
        val words = dna.split(Regex("\\s+")).take(12).joinToString(" ")
        return "${input.character?.name ?: "a person"}, $words"
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

    private fun dialogueFlow(input: ShotInput): String {
        if (input.dialogueLine.isBlank()) return ""
        val name = input.dialogueName.ifBlank { "The character" }
        return " $name says, \"${input.dialogueLine.trim()}\""
    }

    private fun dialogueKling(input: ShotInput): String {
        if (input.dialogueLine.isBlank()) return ""
        val name = input.dialogueName.ifBlank { "Character" }
        return " $name (determined): \"${input.dialogueLine.trim()}\""
    }

    // protectedTail: kalimat kunci (lock / constraints) yang TIDAK BOLEH
    // terpotong trim. Kalau teks kepanjangan, yang dipangkas hanya bagian
    // kepala — tail selalu dipertahankan utuh di akhir.
    private fun finish(
        engine: TargetEngine,
        raw: String,
        negative: String?,
        input: ShotInput,
        protectedTail: String? = null
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
        val count = MythFilter.countWords(trimmed)
        if (count < engine.minWords) {
            warnings.add(
                "Prompt hanya $count kata (target ${engine.minWords}-${engine.maxWords}). " +
                "Tambah detail aksi, dunia, atau cahaya supaya model tidak menebak-nebak."
            )
        }
        if (engine == TargetEngine.WAN && input.dialogueLine.isNotBlank()) {
            warnings.add("Wan 2.2 tanpa audio: dialog tidak dimasukkan ke prompt.")
        }
        return GeneratedPrompt(engine, trimmed, negative, count, warnings)
    }

    // Flow / Veo — kamera ditulis PERTAMA, dua beat timestamp, audio di baris sendiri.
    // Aksi TIDAK diulang mentah di beat kedua (cukup "The action completes")
    // supaya anggaran 200 kata cukup untuk DNA verbatim dua karakter.
    private fun buildFlow(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style)
        val shot = input.shotSize?.phraseEn ?: "medium shot"
        val angle = input.angle?.phraseEn ?: "eye level"
        val lens = "${input.lensMm ?: 35}mm"
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light?.phraseEn ?: LightKind.GOLDEN_HOUR.phraseEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { "dust drifting slowly through the air" }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val hide = if (input.hideTactic != HideTactic.NONE) {
            " ${input.hideTactic.phraseEn}."
        } else {
            ""
        }
        val sentences = listOf(
            Sentence(
                "$shot, $angle, $lens, ${look.first}. ${subjectLine(input)} in ${worldText(input)}, $camera, lit with $light.$hide",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "[00:00-00:05] $poseStart, $action begins.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "$material.",
                mandatory = false,
                dropPriority = 3
            ),
            Sentence(
                "[00:05-00:10] The action completes as $poseEnd. ${look.second}, ${look.third}.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "Audio: $audio.${dialogueFlow(input)}",
                mandatory = true,
                dropPriority = 0
            )
        )
        val raw = assembleOrdered(sentences, FLOW_POSITIVE_LOCK, TargetEngine.FLOW.maxWords)
        return finish(TargetEngine.FLOW, raw, null, input, FLOW_POSITIVE_LOCK)
    }

    // WanGP TI2V-5B — gambar first-frame sudah mengunci subjek dan scene,
    // jadi teks hanya membawa GERAKAN + KAMERA. Target 80-120 kata.
    // Batas keras encoder 512 token: info penting ditulis di awal.
    private fun buildWan(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style)
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light?.phraseEn ?: LightKind.GOLDEN_HOUR.phraseEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val raw = StringBuilder()
        raw.append("${shortDna(input)} in ${worldText(input)}. ")
        raw.append("$poseStart, then $action, ending $poseEnd. ")
        raw.append("$camera. Lit with $light. ${look.first}. ${look.second}.")
        var text = MythFilter.filterMyths(raw.toString())
        if (MythFilter.countWords(text) < 80) {
            val extra = " ${input.materialEn.ifBlank { "Dust drifts slowly, debris settles, the ground stays cracked underfoot." }} " +
                "The same character design stays consistent in every frame."
            text = MythFilter.filterMyths(text + extra)
        }
        return finish(TargetEngine.WAN, text, WAN_NEGATIVE_MANDARIN, input)
    }

    // Kling — Subject + Action + Setting + Camera + Lighting + Mood.
    // Batas 100 kata: DNA dipendekkan (nama + ciri kunci) karena identitas
    // di Kling dibawa Element Reference, bukan deskripsi panjang.
    private fun buildKling(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style)
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light ?: LightKind.GOLDEN_HOUR
        val mood = (input.genre ?: Genre.DRAMA).moodEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { "dust drifts slowly through the air" }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val secondName = input.secondCharacter?.let { ", facing ${it.name}" } ?: ""
        val sentences = listOf(
            Sentence(
                "${shortDna(input)}$secondName, in ${shortWorld(input)}.",
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
        )
        val raw = assembleOrdered(sentences, POSITIVE_LOCK_SHORT, TargetEngine.KLING.maxWords)
        return finish(TargetEngine.KLING, raw, null, input, POSITIVE_LOCK_SHORT)
    }

    // Seedance — 8 elemen berurutan + timeline 3 segmen, tanpa negative prompt.
    // Eksklusi ditulis sebagai constraints (3-5), bukan field negative.
    // Constraints DILINDUNGI dari trim (tail), sama seperti lock Flow.
    private fun buildSeedance(input: ShotInput): GeneratedPrompt {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = styleLook(style)
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light ?: LightKind.GOLDEN_HOUR
        val mood = (input.genre ?: Genre.DRAMA).moodEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val material = input.materialEn.ifBlank { "dust drifts slowly through the air" }
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val secondName = input.secondCharacter?.let { ", facing ${it.name}" } ?: ""
        val tail = "Constraints: one action only, one camera move only, no text on screen, " +
            "consistent character design, no extra limbs."
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
                "[2-4s] $action.${dialogueFlow(input)}",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence(
                "[4-6s] The action completes as $poseEnd. Audio: $audio.",
                mandatory = true,
                dropPriority = 0
            ),
            Sentence("$material.", mandatory = false, dropPriority = 3)
        )
        val raw = assembleOrdered(sentences, tail, TargetEngine.SEEDANCE.maxWords)
        return finish(TargetEngine.SEEDANCE, raw, null, input, tail)
    }
}
