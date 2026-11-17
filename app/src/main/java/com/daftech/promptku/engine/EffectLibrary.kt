package com.daftech.promptku.engine

// Perpustakaan Efek v1.4.2 — template efek sutradara (bukan teks bebas).
// Setiap efek = 1 resep: fragmen prompt Inggris (struktur pemicu ->
// hantaman -> akibat) + override kamera/lensa/cahaya/sudut opsional +
// beat yang boleh memakainya. SATU scene = SATU efek (tiga hukum §5.1
// audit v1.4.1: aksi tanpa konsekuensi fisik = tidak dramatis; emosi =
// kamera + cahaya + warna + suara, bukan kata sifat; 1 klip = 1 efek).
// Efek dipilih dari trigger kalimat (StoryInfer) atau chip per scene
// di tab Naskah (cycleSceneEffect) — dan bisa di-browse di tab Efek.
// Fragmen TIDAK boleh mengandung kata waktu (morning/dawn/noon/dusk/
// night/golden) — supaya tidak bertabrakan dengan waktu dunia (v1.4.1).
// Efek CAMERA punya fragmentEn kosong: ia hanya mengganti cara shot
// diambil (kamera/lensa/sudut), aksinya tetap dari pool.

enum class EffectCategory(val labelId: String) {
    FIGHT("Gerakan Berantem"),
    EXPLOSION("Ledakan"),
    EMOTION("Emosi"),
    CAMERA("Kamera Dramatis"),
    TIME("Waktu")
}

data class Effect(
    val id: String,
    val category: EffectCategory,
    val nameId: String,
    val triggers: List<String>,
    val fragmentEn: String,
    val cameraMove: CameraMove? = null,
    val lensMm: Int? = null,
    val lightKind: LightKind? = null,
    val angle: CameraAngle? = null,
    // sequenceIndex yang boleh memakai efek ini (0..7 EIGHT, 0..4 FIVE).
    val beats: IntRange,
    // v2.1 fix8 v2 — efek PERAN (IMPACT/HERO_HURT/TENSION): hanya boleh
    // dipasang assignEffects dari FightRole, TIDAK pernah masuk siklus
    // jurus generik (FIGHT_MOVES) atau dipilih trigger acak.
    val roleOnly: Boolean = false
)

// Hasil deteksi trigger dari 1 kalimat (StoryInfer).
data class DetectedEffects(
    val fight: Boolean = false,
    val explosion: Boolean = false,
    val emotionId: String? = null
) {
    // Token ringkas untuk ScriptInput.effectIds (dipakai ulang saat
    // naskah dimuat dari DB — deteksi deterministik dari sinopsis).
    fun toTokens(): List<String> = listOfNotNull(
        if (fight) "FIGHT" else null,
        if (explosion) "EXPLOSION" else null,
        emotionId
    )
}

object EffectLibrary {

    val ALL: List<Effect> = listOf(
        // ---- GERAKAN BERANTEM (8 jurus) ----
        // v2.1 fix8 v2 — fragmen fight ditulis ulang CEPAT: bahasa
        // freeze/slow-mo ("the impact freezes for a heartbeat", "sweat
        // sprays in slow motion") DICABUT dari semua jurus biasa — kata
        // itu yang bikin 5 scene berantem Olympus berasa 5x slow motion
        // (audit lapangan 17 Nov). Slow-mo kini HANYA di fight_impact
        // (1x/film) + finisher. Kamera fight juga diganti cepat (WHIP_PAN /
        // TRACKING / HANDHELD_SHAKE); DOLLY_IN slow cuma tersisa di
        // tension (di sanalah kamera pelan memang benar).
        Effect(
            id = "punch_straight",
            category = EffectCategory.FIGHT,
            nameId = "Pukulan Lurus",
            triggers = listOf("pukul", "tinju", "melawan", "berantem"),
            fragmentEn = "the cross snaps the head back, sweat spraying, the guard splitting open",
            cameraMove = CameraMove.WHIP_PAN,
            lensMm = 50,
            beats = 2..6
        ),
        Effect(
            id = "punch_uppercut",
            category = EffectCategory.FIGHT,
            nameId = "Uppercut",
            triggers = listOf("uppercut", "pukul", "tinju"),
            fragmentEn = "the uppercut lifts the jaw, the body rocking back, feet skidding across the ground",
            cameraMove = CameraMove.TRACKING,
            lensMm = 24,
            beats = 2..6
        ),
        Effect(
            id = "kick_roundhouse",
            category = EffectCategory.FIGHT,
            nameId = "Tendangan Putar",
            triggers = listOf("tendang", "tendangan"),
            fragmentEn = "the roundhouse kick whips around, the leg a blur, the torso twisting with the impact, dust bursting from the ground",
            cameraMove = CameraMove.WHIP_PAN,
            lensMm = 35,
            beats = 2..6
        ),
        Effect(
            id = "dodge",
            category = EffectCategory.FIGHT,
            nameId = "Hindaran",
            triggers = listOf("hindar", "menghindar", "ducks"),
            fragmentEn = "ducks under the swinging arm by inches — the fist whistles past the camera, hair and cloth whip in the wind",
            cameraMove = CameraMove.TRACKING,
            lensMm = 35,
            beats = 2..6
        ),
        Effect(
            id = "block",
            category = EffectCategory.FIGHT,
            nameId = "Blok",
            triggers = listOf("blok", "tahan", "bertahan"),
            fragmentEn = "raises both forearms — the blow lands with a dull thud, the ground cracks under his boots, he does not move an inch",
            cameraMove = CameraMove.HANDHELD_SHAKE,
            lensMm = 50,
            beats = 2..6
        ),
        Effect(
            id = "slam",
            category = EffectCategory.FIGHT,
            nameId = "Bantingan",
            triggers = listOf("banting", "slam", "lempar"),
            // "asphalt" -> "ground" (drive-by dari audit Olympus: aspal di
            // reruntuhan Yunani kuno; fragmen ini ditulis ulang di fix8).
            fragmentEn = "lifts the giant over his shoulder — the body slams down, the ground shatters in a radial crack, dust blooms outward",
            cameraMove = CameraMove.TRACKING,
            lensMm = 24,
            beats = 3..6
        ),
        Effect(
            id = "grapple",
            category = EffectCategory.FIGHT,
            nameId = "Cengkeraman",
            triggers = listOf("cengkeram", "pegang", "cekik"),
            fragmentEn = "grabs the giant's wrist — veins bulge, the arm trembles, the fingers prying open under the strain",
            cameraMove = CameraMove.RACK_FOCUS,
            lensMm = 85,
            beats = 2..6
        ),
        Effect(
            id = "finisher",
            category = EffectCategory.FIGHT,
            nameId = "Finisher",
            triggers = listOf("finisher", "pamungkas", "terakhir"),
            // Finisher BOLEH slow-mo (satu-satunya jurus biasa yang boleh,
            // bersama fight_impact): ini momen puncak, bukan tukaran cepat.
            fragmentEn = "rises into the air, fist glowing — the final blow lands, a ring of light expands, the giant collapses in slow motion",
            cameraMove = CameraMove.CRANE_UP,
            lensMm = 24,
            beats = 3..6
        ),
        // ---- PERAN FIGHT fix8 v2 (roleOnly — hanya dari FightRole) ----
        // IMPACT = satu momen slow-mo per film (grammar referensi: pukulan
        // mendarat -> waktu melambat -> push-in close-up wajah korban
        // kesakitan -> ngebut lagi). HERO_HURT = villain mendarat pukulan
        // bersih di Eskalasi. TENSION = jeda tanpa kontak (napas di antara
        // rentetan). Ketiganya TIDAK masuk siklus FIGHT_MOVES.
        Effect(
            id = "fight_impact",
            category = EffectCategory.FIGHT,
            nameId = "Pukulan Dampak",
            triggers = emptyList(),
            fragmentEn = "the blow lands — the motion ramps into slow motion for a heartbeat, the camera pushing in close on his face, eyes wide with pain, jaw slack, then snaps back to full speed as he crashes backward, dust bursting from the ground",
            cameraMove = CameraMove.PUSH_IN,
            lensMm = 85,
            beats = 2..6,
            roleOnly = true
        ),
        Effect(
            id = "fight_hero_hurt",
            category = EffectCategory.FIGHT,
            nameId = "Hero Terluka",
            triggers = emptyList(),
            fragmentEn = "staggers back, blood on his lip, knees buckling, the world tilting — then his eyes harden and he plants his feet again",
            cameraMove = CameraMove.HANDHELD_SHAKE,
            lensMm = 50,
            beats = 2..6,
            roleOnly = true
        ),
        Effect(
            id = "fight_tension_a",
            category = EffectCategory.FIGHT,
            nameId = "Ketegangan: Mengelilingi",
            triggers = emptyList(),
            fragmentEn = "circles slowly, eyes locked, dust settling between them, both breathing hard, neither blinking",
            cameraMove = CameraMove.DOLLY_IN,
            lensMm = 85,
            beats = 2..6,
            roleOnly = true
        ),
        Effect(
            id = "fight_tension_b",
            category = EffectCategory.FIGHT,
            nameId = "Ketegangan: Menahan Napas",
            triggers = emptyList(),
            fragmentEn = "holds his ground, chest heaving, eyes never leaving his opponent, dust drifting through the still air",
            cameraMove = CameraMove.STATIC,
            lensMm = 50,
            beats = 2..6,
            roleOnly = true
        ),
        Effect(
            id = "fight_tension_c",
            category = EffectCategory.FIGHT,
            nameId = "Ketegangan: Menimbang Lawan",
            triggers = emptyList(),
            fragmentEn = "weighs his opponent, shifting his weight from foot to foot, the silence stretching between them, a bead of sweat tracing his temple",
            cameraMove = CameraMove.ORBIT,
            lensMm = 35,
            beats = 2..6,
            roleOnly = true
        ),
        // ---- LEDAKAN (4 efek) ----
        Effect(
            id = "explosion_fire",
            category = EffectCategory.EXPLOSION,
            nameId = "Ledakan Api",
            triggers = listOf("ledak", "meledak", "bom"),
            fragmentEn = "a fireball blooms outward, debris sprays in every direction, a column of black smoke rises, embers drift in slow motion",
            cameraMove = CameraMove.HANDHELD,
            lensMm = 24,
            beats = 3..5
        ),
        Effect(
            id = "explosion_dust",
            category = EffectCategory.EXPLOSION,
            nameId = "Ledakan Debu",
            triggers = listOf("ledak", "debu"),
            fragmentEn = "the ground erupts in a wall of dust, visibility drops to zero, silhouettes emerge through the haze",
            cameraMove = CameraMove.STATIC,
            lensMm = 35,
            beats = 3..5
        ),
        Effect(
            id = "explosion_crash",
            category = EffectCategory.EXPLOSION,
            nameId = "Tabrakan",
            triggers = listOf("tabrak", "tabrakan", "kecelakaan"),
            fragmentEn = "metal screams, glass shatters in slow motion, the car tumbles end over end, sparks trail behind it",
            cameraMove = CameraMove.TRACKING,
            lensMm = 35,
            beats = 3..5
        ),
        Effect(
            id = "explosion_collapse",
            category = EffectCategory.EXPLOSION,
            nameId = "Runtuhan Gedung",
            triggers = listOf("runtuh", "roboh", "ambruk"),
            fragmentEn = "the tower folds in on itself, floors pancake downward, a wave of dust rolls down the street",
            cameraMove = CameraMove.CRANE_UP,
            lensMm = 24,
            beats = 3..5
        ),
        // ---- EMOSI (6 efek) — paket lengkap: fragmen + cahaya + kamera.
        Effect(
            id = "emotion_sad",
            category = EffectCategory.EMOTION,
            nameId = "Sedih",
            triggers = listOf("sedih", "menangis", "duka"),
            fragmentEn = "tears well up and one tear rolls down his cheek — he lowers his head, rain drips from his hair, the world goes quiet around him",
            cameraMove = CameraMove.DOLLY_IN,
            lensMm = 85,
            lightKind = LightKind.CHIAROSCURO,
            beats = 3..7
        ),
        Effect(
            id = "emotion_happy",
            category = EffectCategory.EMOTION,
            nameId = "Bahagia",
            triggers = listOf("bahagia", "senang", "gembira", "ketawa"),
            fragmentEn = "bursts into laughter, throws his arms wide — he spins once, grinning, the whole street seems to lift with him",
            cameraMove = CameraMove.TRACKING,
            lensMm = 35,
            lightKind = LightKind.GOLDEN_HOUR,
            beats = 3..7
        ),
        Effect(
            id = "emotion_angry",
            category = EffectCategory.EMOTION,
            nameId = "Marah",
            triggers = listOf("marah", "murka", "kesal"),
            fragmentEn = "his jaw clenches, his fists shake — he steps forward, breathing hard, the air itself seems to tighten",
            cameraMove = CameraMove.HANDHELD,
            lensMm = 50,
            lightKind = LightKind.RIM,
            beats = 3..7
        ),
        Effect(
            id = "emotion_afraid",
            category = EffectCategory.EMOTION,
            nameId = "Takut",
            triggers = listOf("takut", "ketakutan", "ngeri"),
            fragmentEn = "his eyes widen, he freezes mid-step — he glances over his shoulder, breath held, every sound suddenly loud",
            cameraMove = CameraMove.STATIC,
            lensMm = 85,
            lightKind = LightKind.CHIAROSCURO,
            beats = 3..7
        ),
        Effect(
            id = "emotion_proud",
            category = EffectCategory.EMOTION,
            nameId = "Bangga",
            triggers = listOf("bangga", "pahlawan"),
            fragmentEn = "lifts his chin, chest out — wind catches his jacket, he stands tall as the crowd looks up at him",
            cameraMove = CameraMove.CRANE_UP,
            lensMm = 24,
            lightKind = LightKind.VOLUMETRIC,
            beats = 3..7
        ),
        Effect(
            id = "emotion_despair",
            category = EffectCategory.EMOTION,
            nameId = "Putus Asa",
            triggers = listOf("putus asa", "menyerah", "pasrah"),
            fragmentEn = "his shoulders drop, he sinks to his knees — his hands go slack, the fight leaves his eyes",
            cameraMove = CameraMove.DOLLY_OUT,
            lensMm = 50,
            lightKind = LightKind.OVERCAST,
            beats = 3..7
        ),
        // ---- KAMERA DRAMATIS (6 gerakan) — fragmentEn kosong: hanya
        // mengganti cara shot diambil, aksinya tetap dari pool.
        Effect(
            id = "camera_whip",
            category = EffectCategory.CAMERA,
            nameId = "Whip-Pan",
            triggers = emptyList(),
            fragmentEn = "",
            // v2.1 fix8 v2 — dulu PAN_LEFT ("slow pan left") padahal
            // namanya Whip-Pan: whip pan yang lambat itu bukan whip pan.
            cameraMove = CameraMove.WHIP_PAN,
            lensMm = 35,
            beats = 0..7
        ),
        Effect(
            id = "camera_orbit",
            category = EffectCategory.CAMERA,
            nameId = "Orbit 180°",
            triggers = emptyList(),
            fragmentEn = "",
            cameraMove = CameraMove.ORBIT,
            lensMm = 35,
            beats = 0..7
        ),
        Effect(
            id = "camera_crashzoom",
            category = EffectCategory.CAMERA,
            nameId = "Crash Zoom",
            triggers = emptyList(),
            fragmentEn = "",
            cameraMove = CameraMove.ZOOM_IN,
            lensMm = 85,
            beats = 0..7
        ),
        Effect(
            id = "camera_dolly_rack",
            category = EffectCategory.CAMERA,
            nameId = "Dolly + Rack Focus",
            triggers = emptyList(),
            fragmentEn = "",
            cameraMove = CameraMove.RACK_FOCUS,
            lensMm = 50,
            beats = 0..7
        ),
        Effect(
            id = "camera_slowpush",
            category = EffectCategory.CAMERA,
            nameId = "Slow-Mo Push",
            triggers = emptyList(),
            fragmentEn = "",
            cameraMove = CameraMove.DOLLY_IN,
            lensMm = 85,
            beats = 0..7
        ),
        Effect(
            id = "camera_dutch",
            category = EffectCategory.CAMERA,
            nameId = "Dutch Angle",
            triggers = emptyList(),
            fragmentEn = "",
            cameraMove = CameraMove.HANDHELD,
            lensMm = 24,
            angle = CameraAngle.DUTCH,
            beats = 0..7
        ),
        // ---- WAKTU (4 efek) ----
        Effect(
            id = "time_hitstop",
            category = EffectCategory.TIME,
            nameId = "Hit-Stop",
            triggers = emptyList(),
            fragmentEn = "lands the blow — time freezes for two frames, the impact hangs in the air, then everything snaps back into motion",
            cameraMove = CameraMove.STATIC,
            lensMm = 50,
            beats = 0..7
        ),
        Effect(
            id = "time_ramping",
            category = EffectCategory.TIME,
            nameId = "Speed Ramping",
            triggers = emptyList(),
            fragmentEn = "charges forward — the motion ramps from full speed into slow motion as he closes in, every detail suddenly sharp",
            cameraMove = CameraMove.TRACKING,
            lensMm = 35,
            beats = 0..7
        ),
        Effect(
            id = "time_freeze",
            category = EffectCategory.TIME,
            nameId = "Freeze-Frame",
            triggers = emptyList(),
            fragmentEn = "freezes mid-motion, one fist raised — the world holds still around him for a heartbeat",
            cameraMove = CameraMove.STATIC,
            lensMm = 85,
            beats = 0..7
        ),
        Effect(
            id = "time_matchcut",
            category = EffectCategory.TIME,
            nameId = "Match Cut",
            triggers = emptyList(),
            fragmentEn = "turns toward the door — the motion carries straight through the cut, the same gesture continuing in a new place",
            cameraMove = CameraMove.DOLLY_IN,
            lensMm = 35,
            beats = 0..7
        )
    )

    fun byId(id: String): Effect? = ALL.firstOrNull { it.id == id }

    // Jurus berantem tanpa finisher (siklus beat biasa) vs semua jurus
    // (siklus beat non-klimaks) — lihat effectIdForScene soal invarian
    // "tidak ada aksi berulang dalam satu beat".
    // v2.1 fix8 v2 — efek PERAN (roleOnly) TIDAK masuk siklus: IMPACT /
    // HERO_HURT / TENSION hanya dipasang assignEffects dari FightRole.
    // Tanpa filter ini, fight_impact (yang mengandung "slow motion")
    // bisa tersiklus ke scene EXCHANGE mana pun — slow-mo massal lagi.
    val FIGHT_MOVES: List<Effect> = ALL.filter {
        it.category == EffectCategory.FIGHT && it.id != "finisher" && !it.roleOnly
    }
    val FIGHT_MOVES_ALL: List<Effect> = ALL.filter {
        it.category == EffectCategory.FIGHT && !it.roleOnly
    }

    /** Efek ketegangan (TENSION): disiklus assignEffects antar scene
     *  TENSION supaya tidak ada satu fragmen yang berulang 3x se-film. */
    val TENSION_MOVES: List<Effect> = ALL.filter {
        it.id == "fight_tension_a" || it.id == "fight_tension_b" || it.id == "fight_tension_c"
    }

    // Trigger kalimat -> kategori efek. "runtuh" SENGAJA tidak memicu
    // ledakan: ia sudah memicu modifier dunia "ruined" (v1.4.1) — kota
    // runtuh bukan berarti ada ledakan di tiap scene.
    // Kata kerja me- berasimilasi: "memukul" TIDAK mengandung "pukul",
    // "menendang" TIDAK mengandung "tendang", "meninju" TIDAK mengandung
    // "tinju" — bentuk berasimilasi ("mukul/ninju/nendang/nembak") WAJIB
    // ikut jadi trigger (pelajaran v1.4.2: tes meninju gagal).
    private val FIGHT_TRIGGERS = listOf(
        "berantem", "bertarung", "melawan", "lawan", "pukul", "mukul",
        "memukul", "dipukul", "tinju", "ninju", "meninju", "tendang",
        "nendang", "menendang", "tembak", "nembak", "menembak",
        "silat", "duel", "berkelahi", "jagoan", "serang",
        "menyerang", "diserang", "hajar", "tarung", "tumbuk",
        // v2.1 fix4 — sama seperti ACTION di StoryInfer, plus "perang"
        // (bare): di sini aman karena detect() hanya menentukan isFight,
        // bukan genre (tes Diponegoro-SEJARAH tidak tersentuh).
        // v2.1 fix6 (riset) — "clash" + "bentrok" (lihat StoryInfer).
        "pertarungan", "pertempuran", "peperangan", "perang",
        "battle", "fight", "war", "clash", "bentrok"
    )
    private val EXPLOSION_TRIGGERS = listOf(
        "ledak", "ledakan", "meledak", "bom", "granat", "terbakar", "bakar",
        "membakar", "dibakar", "kebakaran"
    )
    private val EMOTION_TRIGGERS: List<Pair<List<String>, String>> = listOf(
        listOf("sedih", "menangis", "duka") to "emotion_sad",
        listOf("bahagia", "senang", "gembira", "ketawa") to "emotion_happy",
        listOf("marah", "murka", "kesal") to "emotion_angry",
        listOf("takut", "ketakutan", "ngeri") to "emotion_afraid",
        listOf("bangga", "pahlawan") to "emotion_proud",
        listOf("putus asa", "menyerah", "pasrah") to "emotion_despair"
    )

    fun detect(sentence: String): DetectedEffects {
        // v2.0 — pencocokan TOKEN, bukan substring (pelajaran Antartika:
        // substring menelan kata lain). Bentuk berasimilasi/pasif me-
        // /di- ditulis eksplisit di daftar trigger di atas.
        val fight = TokenBoundary.containsAny(sentence, FIGHT_TRIGGERS)
        val explosion = TokenBoundary.containsAny(sentence, EXPLOSION_TRIGGERS)
        val emotion = EMOTION_TRIGGERS.firstOrNull { (keys, _) ->
            TokenBoundary.containsAny(sentence, keys)
        }?.second
        return DetectedEffects(fight = fight, explosion = explosion, emotionId = emotion)
    }

    // Token "FIGHT,EXPLOSION,emotion_sad" (dari ScriptInput.effectIds)
    // -> DetectedEffects. Token tak dikenal diabaikan, bukan crash.
    fun detectFromTokens(tokens: String): DetectedEffects {
        val parts = tokens.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val emotion = parts.firstOrNull { it.startsWith("emotion_") }
        return DetectedEffects(
            fight = "FIGHT" in parts,
            explosion = "EXPLOSION" in parts,
            emotionId = emotion
        )
    }

    // Efek OTOMATIS untuk satu scene. Aturan prioritas per klip:
    // - Klimaks (beat 5 EIGHT / 3 FIVE), klip pertama: ledakan > finisher
    //   (berantem) > emosi. Satu ledakan/finisher per film, di puncak.
    // - Beat berantem (2..6): jurus bersiklus dari globalClipIndex.
    //   Beat klimaks memakai 7 jurus TANPA finisher (finisher sudah
    //   dipakai klip 0); beat lain memakai 8 jurus — supaya TIDAK ADA
    //   efek berulang dalam satu beat (maks 8 klip/beat, 8 jurus).
    // - Beat akhir, klip pertama: emosi (kalau ada trigger emosi).
    fun effectIdForScene(
        beatIndex: Int,
        clipIdx: Int,
        globalClipIndex: Int,
        totalBeats: Int,
        detected: DetectedEffects
    ): String {
        val climaxBeat = if (totalBeats <= 5) 3 else 5
        val finalBeat = if (totalBeats <= 5) 4 else 7
        return when {
            beatIndex == climaxBeat && clipIdx == 0 -> when {
                detected.explosion -> "explosion_fire"
                detected.fight -> "finisher"
                detected.emotionId != null -> detected.emotionId
                else -> ""
            }
            detected.fight && beatIndex in 2..6 -> {
                if (beatIndex == climaxBeat) {
                    FIGHT_MOVES[globalClipIndex % FIGHT_MOVES.size].id
                } else {
                    FIGHT_MOVES_ALL[globalClipIndex % FIGHT_MOVES_ALL.size].id
                }
            }
            detected.emotionId != null && beatIndex == finalBeat && clipIdx == 0 -> {
                detected.emotionId
            }
            else -> ""
        }
    }

    // Kandidat chip per scene: semua efek yang boleh di beat itu,
    // diurutkan per kategori; "" (Tanpa efek) selalu pertama.
    // Dipakai cycleSceneEffect (ketuk chip = efek berikutnya).
    fun candidatesForBeat(beatIndex: Int): List<String> {
        val ids = ALL.filter { beatIndex in it.beats }.map { it.id }
        return listOf("") + ids
    }
}
