package com.daftech.promptku.engine

// Mode Cepat v1.4 — "Satu Kalimat Jadi Film".
// Tebakan rule-based OFFLINE dari 1 kalimat Bahasa Indonesia: genre,
// dunia, emosi, gaya, ambience, kartu 3 jangkar. Tanpa AI online.
// Tebakan tampil sebagai chip yang bisa diubah user SEBELUM build.
// Urutan cek genre = paling spesifik dulu (Horor/Sci-Fi/Fantasi sebelum
// Action; lokasi di kalimat menimpa dunia default genre).
object StoryInfer {

    data class Inference(
        val genre: Genre,
        val worldName: String,
        val worldDescEn: String,
        val emotion: String,
        val style: VisualStyle,
        val ambienceEn: String,
        val characterCard: CharacterCard
    )

    // Contoh 1 ketukan — ketuk untuk mengisi kalimat.
    val EXAMPLES: List<String> = listOf(
        "Riko si tukang tambal ban ternyata CEO perusahaan menara itu",
        "Bejo melawan raksasa 2,8 meter di langit kota",
        "Kuntilanak di kebun pisang menakuti anak yang pulang malam"
    )

    // Lokasi di kalimat -> dunia (menimpa dunia default genre).
    private val LOCATIONS: List<Triple<List<String>, String, String>> = listOf(
        Triple(
            listOf("bengkel", "tambal ban"),
            "Bengkel",
            "a small roadside tire repair shop at dusk, worn tires stacked against the wall, tools scattered on the ground"
        ),
        Triple(
            listOf("pantai", "laut"),
            "Pantai",
            "a quiet beach at sunset, golden light on calm water, empty benches"
        ),
        Triple(
            listOf("pasar"),
            "Pasar",
            "a busy traditional market in the morning, colorful stalls, steam from food carts"
        ),
        Triple(
            listOf("kampung", "desa"),
            "Kampung",
            "a quiet village road at dawn, wooden houses, rice fields, mist"
        ),
        Triple(
            listOf("kota", "jakarta", "gedung"),
            "Kota",
            "a metropolitan city street at night, glass towers, neon signs, wet asphalt"
        ),
        Triple(
            listOf("hutan", "kebun"),
            "Hutan",
            "a dense forest at dawn, mist between tall trees, mossy stones"
        ),
        Triple(
            listOf("sekolah"),
            "Sekolah",
            "an old schoolyard in the afternoon, empty benches, a flagpole"
        ),
        Triple(
            listOf("kantor"),
            "Kantor",
            "a quiet office at night, one desk lamp, city lights through the window"
        ),
        Triple(
            listOf("pelabuhan", "kapal", "nelayan"),
            "Pelabuhan",
            "a fishing harbor at dawn, wooden boats, mist over calm water"
        ),
        Triple(
            listOf("gunung"),
            "Gunung",
            "a mountain peak above the clouds at sunrise, stone steps, prayer flags"
        ),
        Triple(
            listOf("sawah", "panen", "petani"),
            "Sawah",
            "a golden rice field at noon, workers in a line, mountains far away"
        ),
        Triple(
            listOf("stasiun", "kereta"),
            "Stasiun",
            "a train station at midnight, empty platforms, fluorescent light, fog outside"
        ),
        Triple(
            listOf("candi", "istana"),
            "Candi",
            "an ancient stone temple courtyard at sunrise, carved pillars, incense smoke"
        )
    )

    // genre -> (kata kunci, dunia default, emosi, gaya, ambience, kartu).
    private data class GenreRule(
        val keywords: List<String>,
        val worldName: String,
        val worldDescEn: String,
        val emotion: String,
        val style: VisualStyle,
        val ambienceEn: String,
        val card: CharacterCard
    )

    private val RULES: List<Pair<Genre, GenreRule>> = listOf(
        Genre.HOROR to GenreRule(
            keywords = listOf(
                "hantu", "kuntilanak", "pocong", "santet", "setan", "kuburan",
                "angker", "arwah", "genderuwo", "tuyul", "sundel", "menakuti"
            ),
            worldName = "Kampung Angker",
            worldDescEn = "a quiet village at midnight, wooden houses, thick mist, one flickering lamp",
            emotion = "takut",
            style = VisualStyle.NOIR,
            ambienceEn = "crickets, distant dogs barking, cold night wind",
            card = CharacterCard("pale frightened face", "trembling hands", "flashlight", "")
        ),
        Genre.SCI_FI to GenreRule(
            keywords = listOf(
                "robot", "luar angkasa", "alien", "astronot", "masa depan",
                "android", "pesawat luar angkasa"
            ),
            worldName = "Stasiun Luar Angkasa",
            worldDescEn = "a space station corridor, cold metal walls, blinking lights, Earth visible through a window",
            emotion = "takjub",
            style = VisualStyle.CYBERPUNK,
            ambienceEn = "low station hum, soft beeps, air circulation",
            card = CharacterCard("calm focused eyes", "pressing a glowing panel", "white flight suit", "")
        ),
        Genre.FANTASI to GenreRule(
            keywords = listOf(
                "naga", "sihir", "penyihir", "dewa", "peri", "wizard",
                "pedang ajaib", "kerajaan sihir"
            ),
            worldName = "Hutan Kuno",
            worldDescEn = "an ancient forest at dawn, mist between tall trees, mossy stones, fireflies",
            emotion = "takjub",
            style = VisualStyle.ANIME,
            ambienceEn = "birdsong, soft wind through leaves, faint chime",
            card = CharacterCard("brave curious eyes", "raising a glowing hand", "deep blue robe", "")
        ),
        Genre.THRILLER to GenreRule(
            keywords = listOf(
                "mata-mata", "culik", "buron", "misteri", "detektif", "bunuh",
                "rahasia", "penculikan", "spionase"
            ),
            worldName = "Gang Hujan",
            worldDescEn = "a narrow rainy alley at night, flickering street lamp, wet asphalt, brick walls",
            emotion = "curiga",
            style = VisualStyle.NOIR,
            ambienceEn = "rain, footsteps on wet stone, distant siren",
            card = CharacterCard("sharp watchful eyes", "glancing over the shoulder", "grey trench coat", "")
        ),
        Genre.ACTION to GenreRule(
            keywords = listOf(
                "berantem", "bertarung", "pukul", "tendang", "super", "raksasa",
                "kejar", "tembak", "silat", "jagoan", "duel", "berkelahi",
                "melawan"
            ),
            worldName = "Kota Malam",
            worldDescEn = "a metropolitan city at night, glass towers, neon signs, wet asphalt, steam vents",
            emotion = "berani",
            style = VisualStyle.REALISTIC,
            ambienceEn = "distant traffic, wind, low city hum",
            card = CharacterCard("calm confident look", "fist raised", "torn jacket", "")
        ),
        Genre.KOMEDI to GenreRule(
            keywords = listOf(
                "lucu", "ngakak", "kocak", "konyol", "ketawa", "sial", "apes",
                "mertua", "nyasar"
            ),
            worldName = "Warung Kopi",
            worldDescEn = "a busy small coffee shop in the morning, warm light, wooden tables, steaming cups",
            emotion = "senang",
            style = VisualStyle.REALISTIC,
            ambienceEn = "laughter, market chatter, clinking cups",
            card = CharacterCard("wide grin", "scratching his head", "batik shirt", "")
        ),
        Genre.ROMANSA to GenreRule(
            keywords = listOf(
                "cinta", "sayang", "kangen", "rindu", "putus", "nikah", "pacar",
                "jodoh", "lamaran"
            ),
            worldName = "Pantai Senja",
            worldDescEn = "a seaside promenade at sunset, golden light on calm water, empty benches",
            emotion = "rindu",
            style = VisualStyle.VINTAGE,
            ambienceEn = "soft waves, gentle guitar, distant gulls",
            card = CharacterCard("soft hopeful eyes", "reaching hand", "white summer dress", "")
        ),
        Genre.DOKUMENTER to GenreRule(
            keywords = listOf(
                "nelayan", "petani", "pasar", "panen", "pelabuhan",
                "pedagang"
            ),
            worldName = "Pelabuhan Subuh",
            worldDescEn = "a fishing harbor at dawn, wooden boats, mist over calm water, gulls circling",
            emotion = "tenang",
            style = VisualStyle.REALISTIC,
            ambienceEn = "water lapping, gulls, wooden boat creaks",
            card = CharacterCard("sun-darkened skin", "strong working hands", "worn cap", "")
        ),
        Genre.SEJARAH to GenreRule(
            keywords = listOf(
                "majapahit", "diponegoro", "pahlawan", "kemerdekaan",
                "perang puputan", "kerajaan"
            ),
            worldName = "Candi Kuno",
            worldDescEn = "an ancient stone temple courtyard at sunrise, carved pillars, incense smoke",
            emotion = "bangga",
            style = VisualStyle.VINTAGE,
            ambienceEn = "gamelan far away, birdsong, temple bell",
            card = CharacterCard("calm proud face", "hands folded", "white and gold royal cloth", "")
        )
    )

    private val DRAMA_RULE = GenreRule(
        keywords = emptyList(),
        worldName = "Jalanan Jakarta",
        worldDescEn = "a quiet Jakarta street at dusk, rain on asphalt, closed shop fronts, one street lamp",
        emotion = "sedih",
        style = VisualStyle.REALISTIC,
        ambienceEn = "rain on asphalt, distant traffic, a soft piano note",
        card = CharacterCard("calm confident look", "hand to hair", "worn jacket", "")
    )

    fun styleForGenre(genre: Genre): VisualStyle {
        return RULES.firstOrNull { it.first == genre }?.second?.style ?: DRAMA_RULE.style
    }

    fun ambienceForGenre(genre: Genre): String {
        return RULES.firstOrNull { it.first == genre }?.second?.ambienceEn ?: DRAMA_RULE.ambienceEn
    }

    // Tebak genre + dunia dari 1 kalimat. Lokasi menimpa dunia default.
    fun infer(sentence: String): Inference {
        val lower = " " + sentence.lowercase() + " "
        val matched = RULES.firstOrNull { (_, rule) ->
            rule.keywords.any { keyword -> lower.contains(keyword) }
        }
        val genre = matched?.first ?: Genre.DRAMA
        val rule = matched?.second ?: DRAMA_RULE
        val location = LOCATIONS.firstOrNull { (keywords, _, _) ->
            keywords.any { keyword -> lower.contains(keyword) }
        }
        return Inference(
            genre = genre,
            worldName = location?.second ?: rule.worldName,
            worldDescEn = location?.third ?: rule.worldDescEn,
            emotion = rule.emotion,
            style = rule.style,
            ambienceEn = rule.ambienceEn,
            characterCard = rule.card
        )
    }

    // 1 kalimat + nama tokoh + durasi -> ScriptInput siap build (flat v1.3).
    fun toScriptInput(
        sentence: String,
        heroName: String,
        minutes: Int,
        inference: Inference
    ): ScriptInput {
        val hero = heroName.trim().ifBlank { "Raka" }
        return ScriptInput(
            title = sentence.trim().take(80),
            synopsis = sentence.trim(),
            genre = inference.genre,
            durationMinutes = minutes.coerceIn(1, 10),
            emotion = inference.emotion,
            protagonistName = hero,
            protagonistDescEn = "a young Indonesian man in his thirties, worn jacket, determined eyes",
            worldName = inference.worldName,
            worldDescEn = inference.worldDescEn,
            conflict = sentence.trim(),
            style = inference.style,
            engine = TargetEngine.FLOW,
            characterCard = inference.characterCard
        )
    }
}
