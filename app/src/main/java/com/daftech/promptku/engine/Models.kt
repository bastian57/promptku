package com.daftech.promptku.engine

// Model data PromptKu v1.0.0 — pure Kotlin, tanpa dependensi Android,
// supaya bisa diuji di JVM biasa (kotlinc + JUnit) tanpa emulator.
// Bahasa UI: Indonesia. Bahasa prompt output: Inggris.

// Sepuluh genre v1 — persis seperti scope yang digas. Jangan tambah.
enum class Genre(val labelId: String, val moodEn: String) {
    ACTION("Action", "tense, high-energy, urgent"),
    DRAMA("Drama", "quiet, emotional, intimate"),
    ROMANSA("Romansa", "tender, warm, longing"),
    HOROR("Horor", "dreadful, suffocating, uneasy"),
    SCI_FI("Sci-Fi", "awe, cold wonder, vast"),
    FANTASI("Fantasi", "wondrous, mythic, magical"),
    THRILLER("Thriller", "paranoid, tight, suspenseful"),
    KOMEDI("Komedi", "playful, light, warm-hearted"),
    DOKUMENTER("Dokumenter", "observational, honest, grounded"),
    SEJARAH("Sejarah", "solemn, grand, timeless")
}

// Enam gaya visual v1 — persis seperti scope yang digas. Jangan tambah.
enum class VisualStyle(val labelId: String) {
    REALISTIC("Realistic Hollywood"),
    ANIME("Anime"),
    PIXAR_3D("3D Pixar"),
    CYBERPUNK("Cyberpunk"),
    NOIR("Noir"),
    VINTAGE("Vintage Film")
}

// Empat engine tujuan v1 — output TEKS saja, app tidak generate video.
enum class TargetEngine(val labelId: String, val maxWords: Int, val minWords: Int) {
    // v2.2 — cap riset detail-vs-panjang (18 Nov 2026, sumber resmi):
    // FLOW 180 -> 300 (contoh resmi DeepMind ~470 kata/8 dtk; kasus Wushu
    // 900 kata jalan di Flow; tidak ada batas resmi Flow yang ditemukan).
    // SEEDANCE 100 -> 250 (anjuran resmi <=1.000 kata; yang dibatasi adalah
    // jumlah beat, bukan kata). WAN 120 -> 200 (profil WanGP LOKAL — cap 800
    // karakter API hosted tidak berlaku lokal; rewriter resmi Wan 60-200
    // kata). KLING TETAP 100 (blog resmi Kling: >150 kata cenderung
    // memburuk; target resmi 50-100 kata).
    // v1.3.1 — Veo 3.1 Fast/Quality PENSIUN 5 Des 2026 (Flow): profil FLOW
    // retarget ke Omni Flash 1.2 (default Flow, sampai 15 dtk, Ingredients);
    // Veo 3.1 Lite jadi fallback (maks 8 dtk, 720p) — cuma di catatan
    // export, bukan pilihan engine ke-5. Cap 300 + min 40 TIDAK berubah
    // (tidak ada batas resmi Flow yang ditemukan; riset 18 Nov).
    FLOW("Flow / Omni 1.2", 300, 40),
    WAN("WanGP", 200, 40),
    KLING("Kling", 100, 60),
    SEEDANCE("Seedance", 250, 60)
}

/**
 * v2.2 — profil prompt per engine (DATA, bukan cabang if di prompter).
 * Angka actionEn DITURUNKAN dari overhead renderer (diukur dari film Clash
 * 18 Nov 2026, Mode B): total render = inti + sisanya (fragmen + dunia +
 * cahaya + kamera + audio + aspek + framing). Overhead tengah: Flow ~117,
 * Kling ~97, Seedance ~114, Wan ~53 kata. Target inti = cap - overhead,
 * dibulatkan ke bawah; "standar" = scene biasa, "maks" = langit-langit
 * adegan aksi kompleks (Mode B micro-beat, maks 3 beat — sudah perilaku
 * fix12). Kling: overhead ~97 dari cap 100 -> inti hampir tidak punya
 * ruang; instruksinya memang "sependek mungkin, satu gerakan".
 */
data class EnginePromptProfile(
    val engine: TargetEngine,
    /** Kata target action_en di scene biasa. */
    val actionWordsStandard: Int,
    /** Langit-langit action_en untuk adegan aksi kompleks. */
    val actionWordsMax: Int,
    /** Aturan audio_en khusus engine ini ("" = tidak ada aturan khusus). */
    val audioRule: String
) {
    /**
     * Kalimat instruksi untuk prompter (satu blok, ditempel ke Rules).
     * Menyebut angka TOTAL scene (cap engine) DAN angka action_en — LLM
     * hanya mengontrol action_en; renderer menempel sisanya di sekitarnya.
     * Wording dijaga bebas dari frasa yang dites di V2Fix10Test /
     * V2Fix12Test ("timestamped micro-beats", "slow motion", frasa fragmen
     * fight_hero_hurt) supaya aturan ini tidak memecahkan tes lama.
     */
    fun ruleText(): String = when (engine) {
        // v1.3.1 — "Flow / Veo" -> "Flow / Omni Flash 1.2" (Veo 3.1
        // Fast/Quality pensiun 5 Des 2026). Angka TIDAK berubah: total
        // 300 kata, action_en 50/150 (re-lock v1.3.1).
        TargetEngine.FLOW -> "TARGET ENGINE: Flow / Omni Flash 1.2. Total scene budget 300 words. Write " +
            "action_en full and detailed: about 50 words for a normal scene, up to 150 words " +
            "for a complex fight scene (several fast moves in one continuous flow). Every word " +
            "must change what the camera sees — no filler adjectives. "
        TargetEngine.KLING -> "TARGET ENGINE: Kling. Total scene budget 100 words — the tightest " +
            "of all engines. Write action_en as SHORT as possible: one clear move, about 20 " +
            "words. Anything longer will be trimmed by the app, cutting the action itself. "
        TargetEngine.SEEDANCE -> "TARGET ENGINE: Seedance. Total scene budget 250 words. Write " +
            "action_en about 60 words for a normal scene, up to 120 words for a complex scene, " +
            "with at most 3 timecoded beats per 10-second scene — never more beats than that. "
        TargetEngine.WAN -> "TARGET ENGINE: WanGP (local). Total scene budget 200 words. Write " +
            "action_en about 60 words for a normal scene, up to 120 words for a complex scene. " +
            "Keep audio_en very short or empty — WanGP generates silent video, so audio words " +
            "only eat the budget. "
    }

    companion object {
        fun forEngine(engine: TargetEngine): EnginePromptProfile = when (engine) {
            TargetEngine.FLOW -> EnginePromptProfile(engine, 50, 150, "")
            TargetEngine.KLING -> EnginePromptProfile(engine, 20, 25, "")
            TargetEngine.SEEDANCE -> EnginePromptProfile(engine, 60, 120, "")
            TargetEngine.WAN -> EnginePromptProfile(
                engine, 60, 120,
                "Keep audio_en very short or empty — WanGP generates silent video."
            )
        }
    }
}

enum class ShotSize(val labelId: String, val phraseEn: String) {
    EXTREME_WIDE("Sangat Lebar", "extreme wide shot"),
    WIDE("Lebar", "wide shot"),
    MEDIUM("Medium", "medium shot"),
    MEDIUM_CLOSEUP("Medium Close-Up", "medium close-up"),
    CLOSEUP("Close-Up", "close-up"),
    EXTREME_CLOSEUP("Extreme Close-Up", "extreme close-up"),
    TWO_SHOT("Two-Shot", "two-shot"),
    OVER_SHOULDER("Over-the-Shoulder", "over-the-shoulder shot")
}

enum class CameraAngle(val labelId: String, val phraseEn: String) {
    LOW("Low Angle", "low angle"),
    EYE("Eye Level", "eye level"),
    HIGH("High Angle", "high angle"),
    BIRD("Bird's Eye", "bird's-eye view"),
    DUTCH("Dutch Angle", "dutch angle, slightly tilted")
}

enum class CameraMove(val labelId: String, val phraseEn: String) {
    STATIC("Statis", "static locked camera"),
    DOLLY_IN("Dolly In (maju pelan)", "slow dolly in"),
    DOLLY_OUT("Dolly Out (mundur pelan)", "slow dolly out"),
    TRACKING("Tracking (mengikuti)", "tracking shot, following the subject"),
    CRANE_UP("Crane Up (naik)", "slow crane up"),
    CRANE_DOWN("Crane Down (turun)", "slow crane down"),
    ORBIT("Orbit (memutar pelan)", "slow orbit, less than 45 degrees"),
    AERIAL("Aerial / Drone", "slow aerial drone shot"),
    RACK_FOCUS("Rack Focus", "rack focus from foreground to subject"),
    HANDHELD("Handheld (goyang halus)", "handheld camera, subtle sway only"),
    PAN_LEFT("Pan Kiri", "slow pan left"),
    PAN_RIGHT("Pan Kanan", "slow pan right"),
    TILT_UP("Tilt Up", "slow tilt up"),
    ZOOM_IN("Zoom In (pelan)", "slow zoom in"),
    // v2.1 fix8 v2 — gerakan CEPAT untuk beat berantem (grammar referensi:
    // cepat = default, slow-mo cuma di impact). Sebelum fix8, pool kamera
    // didominasi "slow ..." dan efek fight mengunci kamera slow (right
    // cross -> slow dolly in) — itu yang bikin 5 scene berantem Olympus
    // berasa 5x slow motion. WHIP_PAN = sapuan cepat (bukan "slow pan");
    // PUSH_IN = zoom/dorong cepat ke close-up wajah (khusus IMPACT);
    // HANDHELD_SHAKE = goyang mengejar aksi (bukan "subtle sway").
    WHIP_PAN("Whip Pan (cepat)", "whip pan, fast motion blur, snapping across the action"),
    PUSH_IN("Push-In Cepat", "fast push-in to close-up on the face"),
    HANDHELD_SHAKE("Handheld Goyang", "handheld camera, urgent shake, chasing the action"),
    // v2.1 fix12 — PROJECTILE POV (master prompt Wushu): kamera sempat
    // ngikutin proyektilnya (fireball / bola es) — opsi camera_move baru
    // buat scene yang proyektilnya jadi bintang; parser di
    // ModeRender.cameraMoveFromString kenal "projectile" / "pov".
    PROJECTILE_POV("Projectile POV", "projectile POV, following the projectile")
}

enum class LightKind(val labelId: String, val phraseEn: String) {
    GOLDEN_HOUR("Golden Hour", "low golden-hour sun, warm amber backlight, long shadows"),
    VOLUMETRIC("Volumetrik / God Rays", "volumetric god rays through dust and smoke, soft shafts of light"),
    REMBRANDT("Rembrandt", "hard key light from one side, small triangle of light on the shadowed cheek"),
    CHIAROSCURO("Chiaroscuro", "single hard side light, background falling into deep black"),
    RIM("Rim Light", "strong rim light tracing the silhouette, face mostly in shadow"),
    MOONLIGHT("Cahaya Bulan", "cold blue moonlight, deep shadows, faint silver rim"),
    NEON("Neon", "neon signs in magenta and cyan, wet reflections, dark shadows"),
    OVERCAST("Mendung Lembut", "soft overcast daylight, even shadows, muted sky"),
    CANDLE("Lilin", "warm flickering candlelight, deep amber shadows"),
    STUDIO_SOFT("Studio Lembut", "soft diffused studio light, gentle shadows")
}

// Fokus: wajah ATAU tangan yang bergerak — jangan keduanya sekaligus.
enum class FocusMode(val labelId: String) {
    FACE_ONLY("Wajah saja (tangan diam)"),
    HANDS_ONLY("Tangan saja (wajah stabil)"),
    FULL_STABLE("Keduanya stabil")
}

enum class HideTactic(val labelId: String, val phraseEn: String) {
    NONE("Tidak perlu", ""),
    BACK_SHOT("Back shot (dari belakang)", "shot from behind, face unseen"),
    SILHOUETTE("Siluet lawan cahaya", "silhouette against the bright light"),
    RUBBLE_BURY("Badan ketimbun / tertutup", "half-buried in rubble, lower body unseen"),
    SMOKE_FOREGROUND("Asap di depan lensa", "thick smoke drifting in the foreground"),
    DISTANCE("Jarak jauh", "shot from a distance, faces small"),
    SLOW_MOTION("Gerak lambat", "slow movement, dust hanging in the air")
}

// DNA karakter: deskripsi Inggris verbatim, ditempel apa adanya di semua scene.
data class CharacterDna(
    val name: String,
    val descriptionEn: String
)

// Bahasa dialog (v1.3): app menyimpan TEKS ID + EN, tidak menerjemahkan
// otomatis (rule-based merusak subteks/idiom/ritme). Terjemahan AI = v1.4+ BYOK.
enum class DialogueLang(val labelId: String) {
    ID("Indonesia"),
    EN("Inggris")
}

// Dialog first-class v1.3 — satu baris yang diucapkan satu tokoh di satu klip.
// tactic = tujuan taktis baris ini (misal: "memohon", "mengancam");
// tone = nada sebelum kalimat (misal: "calmly", "firmly");
// startSec/endSec = timing dalam klip (dialog duduk di paruh kedua, 5-10 dtk).
data class DialogueLine(
    val speaker: String = "",
    val tactic: String = "",
    val tone: String = "",
    val textId: String = "",
    val textEn: String = "",
    val language: DialogueLang = DialogueLang.EN,
    val startSec: Float = 5f,
    val endSec: Float = 10f
) {
    // Teks aktif mengikuti bahasa yang dipilih; kalau versi itu kosong,
    // jatuh ke versi satunya (jangan pernah render string kosong).
    fun textFor(lang: DialogueLang): String = when (lang) {
        DialogueLang.ID -> textId.ifBlank { textEn }
        DialogueLang.EN -> textEn.ifBlank { textId }
    }

    fun activeText(): String = textFor(language)

    fun wordCount(lang: DialogueLang = language): Int = MythFilter.countWords(textFor(lang))

    fun isEmpty(): Boolean = textId.isBlank() && textEn.isBlank()
}

// Kartu karakter 3 jangkar v1.3 — SATU sifat + SATU gestur + SATU benda
// (+ jangkar wajah opsional), ~12-15 kata. Inilah yang ditempel ke prompt,
// BUKAN deskripsi DNA panjang (DNA panjang tetap hidup di kartu karakter
// dan TIDAK ditempel — obat struktural bug trim v1.2.0).
// Contoh Riko: "calm confident look" + "hand to hair" +
// "torn denim jacket" + "grease-smudged face".
data class CharacterCard(
    val trait: String = "",
    val gesture: String = "",
    val objectProp: String = "",
    val faceAnchor: String = ""
) {
    fun toDescription(): String = listOf(trait, gesture, objectProp, faceAnchor)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")

    // v1.4.1 — deskripsi untuk subject line: sifat + benda + wajah,
    // TANPA gestur pose. Gestur ("fist raised") pindah ke klausa
    // karakter terpisah supaya tidak bertabrakan dengan poseEnd
    // (berbaring/berlutut) — cacat lapangan 16 Nov.
    fun subjectDescription(): String = listOf(trait, objectProp, faceAnchor)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")

    fun wordCount(): Int = MythFilter.countWords(toDescription())

    fun isEmpty(): Boolean = toDescription().isBlank()
}

// Kunci dunia: deskripsi Inggris verbatim, satu dunia untuk satu film.
data class WorldLock(
    val name: String,
    val descriptionEn: String
)

// Global Lock proyek (v1.1): field yang dikunci di tingkat proyek,
// ditempel verbatim ke SEMUA scene. Field null = tidak dikunci.
// World + Light + Style (membawa grade warna) + Lens = Global Lock v2.
// Ambience (v1.3): suara latar yang sama di semua klip ("jangkrik + angin
// malam") = perekat kontinuitas antar-klip, sejajar Global Lock visual.
data class GlobalLock(
    val world: WorldLock? = null,
    val light: LightKind? = null,
    val style: VisualStyle? = null,
    val lensMm: Int? = null,
    val character: CharacterDna? = null,
    val secondCharacter: CharacterDna? = null,
    val ambience: String? = null
) {
    fun isEmpty(): Boolean =
        world == null && light == null && style == null && lensMm == null &&
            character == null && secondCharacter == null && ambience == null

    // Lock menang atas input scene; field yang tidak dikunci dibiarkan apa adanya.
    // Ambience yang dikunci DITARUH DI DEPAN audio scene (lapisan latar),
    // bukan mengganti SFX scene ("metal tools clinking" tetap ada).
    fun applyTo(input: ShotInput): ShotInput {
        val audio = if (ambience != null) {
            val base = input.audioEn.trim()
            if (base.isBlank()) {
                ambience
            } else if (base.startsWith(ambience)) {
                base
            } else {
                "$ambience, $base"
            }
        } else {
            input.audioEn
        }
        return input.copy(
            world = world ?: input.world,
            light = light ?: input.light,
            style = style ?: input.style,
            lensMm = lensMm ?: input.lensMm,
            character = character ?: input.character,
            secondCharacter = secondCharacter ?: input.secondCharacter,
            audioEn = audio,
            ambienceEn = ambience ?: input.ambienceEn
        )
    }

    fun summaryId(): String {
        val parts = mutableListOf<String>()
        world?.let { parts.add("Dunia: ${it.name}") }
        light?.let { parts.add("Cahaya: ${it.labelId}") }
        style?.let { parts.add("Gaya: ${it.labelId}") }
        lensMm?.let { parts.add("Lensa: ${it}mm") }
        character?.let { parts.add("Tokoh: ${it.name}") }
        secondCharacter?.let { parts.add("Tokoh 2: ${it.name}") }
        ambience?.let { parts.add("Ambience: $it") }
        return if (parts.isEmpty()) "Belum dikunci" else parts.joinToString(" | ")
    }
}

// Kontinuitas pose: pose akhir scene sebelumnya jadi pose awal scene berikut.
fun continuityPoseStart(lastPoseEnd: String): String = lastPoseEnd.trim()

// Satu kartu shot — input wizard 5 langkah.
data class ShotInput(
    val title: String = "",
    val genre: Genre? = null,
    val style: VisualStyle? = null,
    val engine: TargetEngine? = null,
    val character: CharacterDna? = null,
    val secondCharacter: CharacterDna? = null,
    val world: WorldLock? = null,
    val shotSize: ShotSize? = null,
    val angle: CameraAngle? = null,
    val lensMm: Int? = null,
    val cameraMove: CameraMove? = null,
    val light: LightKind? = null,
    // v2.1 fix7 — teks cahaya bible VERBATIM (bukan frasa enum):
    // "Harsh, directional sunlight..." tidak boleh diganti "soft overcast
    // daylight" (audit Olympus 18/18 salah). Mode B sudah verbatim sejak
    // dulu; Mode A menyusul. Kosong = perilaku lama (frasa enum).
    val lightText: String = "",
    val actionEn: String = "",
    val poseStart: String = "",
    val poseEnd: String = "",
    val materialEn: String = "",
    val audioEn: String = "",
    val dialogueName: String = "",
    val dialogueLine: String = "",
    val focusMode: FocusMode? = null,
    val hideTactic: HideTactic = HideTactic.NONE,
    // Kontinuitas multi-scene v1.2 (ENTRY/EXIT STATE): arah gerak tokoh di
    // layar + sisi kamera terhadap garis aksi (aturan 180 derajat).
    // Kosong = tidak disebut di prompt (wizard v1.0/v1.1 tidak berubah).
    val screenDirection: String = "",
    val axisSide: String = "",
    val transition: String = "",
    // v1.3 — DialogueLine (ID/EN + taktik + nada + timing) menggantikan
    // pasangan polos dialogueName/dialogueLine; field lama tetap ada
    // sebagai fallback (template + tes lama + wizard v1).
    // characterCard menggantikan DNA verbatim di prompt bila terisi.
    // ambienceEn = suara latar kunci proyek (Ambience Lock), dipisah
    // dari audioEn (SFX per scene).
    val dialogue: DialogueLine? = null,
    val characterCard: CharacterCard? = null,
    val ambienceEn: String = "",
    // v2.1 fix6 (riset) — kata/karakter klausa aspek ("Vertical 9:16
    // framing." = 3 kata, 22 karakter) yang ditempel ModeRender SETELAH
    // PromptEngine selesai. Anggaran kata/karakter efektif = cap engine
    // DIKURANGI field ini (0 = jalur v1 wizard, tanpa klausa aspek).
    val aspectWords: Int = 0,
    val aspectChars: Int = 0,
    // v2.1 fix8 v2 — rantai kamera: true = gerakan kamera scene ini
    // MELANJUTKAN gerakan kamera scene sebelumnya (satu aliran, bukan
    // potongan baru). PromptEngine menulis "continuing <gerakan> from the
    // previous shot" — inilah yang bikin 5 scene berantem berasa 1 take.
    val cameraContinues: Boolean = false
)

data class GeneratedPrompt(
    val engine: TargetEngine,
    val text: String,
    val negativePrompt: String?,
    val wordCount: Int,
    val warnings: List<String>,
    // Meter anggaran v1.3 — satu baris ("SHOT 8/15 • SUBJECT 18/25 • ..."),
    // dihitung PromptCore saat build; kosong bila meter tidak dihitung.
    val meter: String = ""
)

data class ScoreItem(
    val id: String,
    val labelId: String,
    val passed: Boolean,
    val missingHint: String
)

data class ScoreResult(
    val items: List<ScoreItem>,
    val score: Int
) {
    val missing: List<ScoreItem> get() = items.filter { !it.passed }
}
