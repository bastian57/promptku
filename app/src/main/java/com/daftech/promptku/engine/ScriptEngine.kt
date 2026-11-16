package com.daftech.promptku.engine

// Mesin naskah PromptKu v1.3 — rule-based offline, tanpa AI.
// Sinopsis -> naskah FLAT -> prompt multi-scene.
// MODEL FLAT (keputusan Putra 19:08 WIB, mengikat — ganti model 2 tingkat
// v1.2.0 yang ditolak karena "bertele-tele"):
// 1 scene = 1 klip 10 detik. Jumlah scene = durasi / 10 detik.
// 5 menit = 30 scene; 10 menit = 60 scene. Tidak ada Sequence/Shot
// bersarang, tidak ada coverage A/B/C — tiap scene maju 1 beat cerita.
// Struktur 8 Sequence TETAP dipakai sebagai KERANGKA CERITA (busur
// naik-turun: dunia biasa -> pemicu -> rencana -> tawa -> harga -> hancur
// -> bangkit -> akhir) tapi HANYA sebagai label bab di daftar flat,
// BUKAN wadah/tingkat UI. Approve per scene, bukan per sequence.
// 1 klip = 10 detik (standar Flow). Dialog menempel di TEPAT SATU scene
// (bug v1.2: dialog menempel 2x di shot OVER_SHOULDER + CLOSEUP).

enum class ScriptEnding(val labelId: String) {
    HAPPY("Bahagia"),
    TRAGIC("Tragis"),
    OPEN("Terbuka")
}

// Satu beat/sequence: label bab di daftar flat. tension 1-8 menggerakkan
// pilihan sutradara (shot makin ketat + lensa makin panjang saat tegang).
data class BeatDef(
    val index: Int,
    val nameId: String,
    val questionId: String,
    val tension: Int,
    val timeOfDay: String,
    val stcBeats: String
)

object ScriptBeats {
    // 8 Sequence (Frank Daniel / Gulino 2004) + checkpoint Save the Cat.
    val EIGHT: List<BeatDef> = listOf(
        BeatDef(0, "Dunia Biasa", "Seperti apa hidup tokoh sebelum semuanya berubah?", 1, "DAY", "Opening Image, Theme Stated, Set-Up"),
        BeatDef(1, "Pemicu", "Peristiwa apa yang memaksa tokoh bergerak?", 2, "DAY", "Catalyst (10%)"),
        BeatDef(2, "Rencana & Taruhan", "Rencana apa yang dibuat tokoh, dan apa taruhannya?", 3, "DUSK", "Debate, Break into Two (25%)"),
        BeatDef(3, "Fun & Games", "Apa yang tokoh coba — dan apa yang berhasil?", 5, "GOLDEN HOUR", "Fun and Games, Midpoint (50%)"),
        BeatDef(4, "Harga Kemenangan", "Apa harga yang harus dibayar atas kemenangan itu?", 7, "NIGHT", "Bad Guys Close In"),
        BeatDef(5, "Kehancuran", "Apa titik terendah tokoh — saat semuanya hilang?", 8, "NIGHT", "All Is Lost (75%), Dark Night of the Soul"),
        BeatDef(6, "Kebangkitan", "Apa yang membuat tokoh bangkit dan melawan balik?", 4, "DAWN", "Break into Three (80%), Finale"),
        BeatDef(7, "Dunia Baru", "Seperti apa dunia setelah semuanya selesai?", 2, "DAY", "Final Image")
    )

    // Film pendek (<=3 menit): 5 beat, tanpa B Story.
    val FIVE: List<BeatDef> = listOf(
        BeatDef(0, "Pembuka", "Siapa tokoh ini, di dunia seperti apa?", 1, "DAY", "Opening Image, Set-Up"),
        BeatDef(1, "Pemicu", "Apa yang mengganggu dunia tokoh?", 2, "DUSK", "Catalyst"),
        BeatDef(2, "Eskalasi", "Bagaimana masalah membesar?", 5, "NIGHT", "Fun and Games, Midpoint"),
        BeatDef(3, "Klimaks", "Apa konfrontasi terbesarnya?", 8, "DAWN", "All Is Lost, Finale"),
        BeatDef(4, "Penutup", "Apa yang berubah setelahnya?", 2, "DAY", "Final Image")
    )

    fun forDuration(durationMinutes: Int): List<BeatDef> {
        return if (durationMinutes <= 3) FIVE else EIGHT
    }

    fun lightForTime(timeOfDay: String): LightKind {
        return when (timeOfDay) {
            "GOLDEN HOUR" -> LightKind.GOLDEN_HOUR
            "NIGHT" -> LightKind.MOONLIGHT
            "DAWN" -> LightKind.VOLUMETRIC
            "DUSK" -> LightKind.GOLDEN_HOUR
            else -> LightKind.OVERCAST
        }
    }

    // v1.4.1 — baca waktu dari deskripsi dunia ("at night" -> NIGHT).
    // Null kalau dunia tidak menyebut waktu (pakai waktu beat seperti biasa).
    // Dunia yang bilang "malam" berarti SELURUH film malam — beat tidak
    // boleh menimpa dengan DAY/DUSK/GOLDEN HOUR (cacat lapangan 16 Nov:
    // 18/30 scene Bejo bercahaya siang di dunia malam).
    fun timeOfDayFromWorld(worldDescEn: String): String? {
        val lower = worldDescEn.lowercase()
        return when {
            lower.contains("night") || lower.contains("midnight") -> "NIGHT"
            lower.contains("dawn") || lower.contains("sunrise") -> "DAWN"
            lower.contains("dusk") || lower.contains("sunset") || lower.contains("golden hour") -> "DUSK"
            lower.contains("morning") -> "MORNING"
            lower.contains("noon") || lower.contains("midday") -> "DAY"
            else -> null
        }
    }

    // v1.4.1 — dunia yang memang dilihat dari atas (BIRD angle boleh).
    // Jalanan kota/gang/pasar/bengkel = EYE/LOW (cacat lapangan 16 Nov:
    // bird's-eye di jalanan kota diabaikan model karena first-frame
    // eye-level mengunci framing).
    fun worldSeenFromAbove(worldDescEn: String): Boolean {
        val lower = worldDescEn.lowercase()
        return lower.contains("high above") || lower.contains("above the clouds") ||
            lower.contains("mountain") || lower.contains("rice field") ||
            lower.contains("beach") || lower.contains("aerial") ||
            lower.contains("from above")
    }

    // v1.4.1 — kata waktu di dalam satu aksi pool ("morning market").
    // Null kalau aksi tidak menyebut waktu.
    fun timeWordOf(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("morning") -> "MORNING"
            lower.contains("dawn") || lower.contains("sunrise") -> "DAWN"
            lower.contains("noon") || lower.contains("midday") -> "DAY"
            lower.contains("dusk") || lower.contains("sunset") || lower.contains("golden") -> "DUSK"
            lower.contains("night") || lower.contains("midnight") -> "NIGHT"
            else -> null
        }
    }

    // v1.4.1 — aksi bertabrakan dengan waktu dunia/beat (misal aksi
    // "morning" di dunia malam). Aksi tanpa kata waktu tidak pernah
    // bertabrakan.
    fun timeClashes(action: String, worldTime: String): Boolean {
        val actionTime = timeWordOf(action) ?: return false
        return actionTime != worldTime
    }

    // Lensa makin panjang saat ketegangan naik (preseden 12 Angry Men:
    // 28mm -> 50mm -> 100mm sepanjang film).
    fun lensForTension(tension: Int): Int {
        return when {
            tension <= 2 -> 24
            tension <= 4 -> 35
            tension <= 6 -> 50
            else -> 85
        }
    }

    fun moveForTension(tension: Int, shotIndex: Int): CameraMove {
        return when {
            tension <= 2 -> if (shotIndex % 2 == 0) CameraMove.STATIC else CameraMove.DOLLY_OUT
            tension <= 4 -> if (shotIndex % 2 == 0) CameraMove.TRACKING else CameraMove.PAN_RIGHT
            tension <= 6 -> CameraMove.DOLLY_IN
            else -> if (shotIndex % 2 == 0) CameraMove.DOLLY_IN else CameraMove.HANDHELD
        }
    }

    // Nada dialog default mengikuti ketegangan beat (dipakai bila user
    // belum menulis nada sendiri). Frasa lengkap, bukan 1 kata — prompt
    // merender "says, in a low, steady voice: ...".
    fun toneForTension(tension: Int): String {
        return when {
            tension <= 2 -> "a calm voice"
            tension <= 4 -> "a low, steady voice"
            tension <= 6 -> "a firm voice"
            else -> "an urgent voice"
        }
    }
}

// Input form naskah — 8 field inti, 1 wajib (sinopsis).
data class ScriptInput(
    val title: String = "",
    val synopsis: String = "",
    val genre: Genre = Genre.ACTION,
    val durationMinutes: Int = 5,
    val emotion: String = "",
    val protagonistName: String = "",
    val protagonistDescEn: String = "",
    val antagonist: String = "",
    val antagonistDescEn: String = "",
    val worldName: String = "",
    val worldDescEn: String = "",
    val conflict: String = "",
    val ending: ScriptEnding = ScriptEnding.HAPPY,
    val style: VisualStyle = VisualStyle.REALISTIC,
    val engine: TargetEngine = TargetEngine.FLOW,
    // v1.3 — bahasa dialog default naskah (ID/EN) + kartu 3 jangkar tokoh.
    val dialogueLang: DialogueLang = DialogueLang.EN,
    val characterCard: CharacterCard? = null,
    // v1.4 — Ambience Lock dari Mode Cepat (in-memory; tidak disimpan ke
    // ScriptItem — saat naskah dimuat ulang dari DB, ambience kembali
    // kosong dan user bisa isi lewat Ambience Lock proyek).
    val ambienceEn: String = "",
    // v1.4.2 — token efek terdeteksi dari kalimat ("FIGHT,EXPLOSION,
    // emotion_sad"), dihitung StoryInfer.toScriptInput dan dihitung ulang
    // dari sinopsis saat naskah dimuat dari DB (ScriptItem.toScriptInput).
    // In-memory saja; yang PERSISTEN per scene adalah DraftScene.effectId.
    val effectIds: String = ""
) {
    fun isReady(): Boolean = synopsis.trim().split(Regex("\\s+")).size >= 10
}

// Satu scene FLAT = 1 klip 10 detik = 1 ShotInput = 1 prompt.
// sceneNumber 1-based (Scene 01..N), orderIndex 0-based.
// sequenceIndex + beatName = label bab (kerangka 8 Sequence), BUKAN wadah.
// dialogue menempel di TEPAT SATU scene per beat (null di scene lain).
// directorNote = "Kenapa shot ini" (tension/genre/durasi, 1 kalimat).
// reAnchor = klip ke-5, 10, 15, ... — tandai re-anchor ke character sheet.
data class DraftScene(
    val sceneNumber: Int,
    val orderIndex: Int,
    val sequenceIndex: Int,
    val beatName: String,
    val slugline: String,
    val want: String,
    val obstacle: String,
    val actionEn: String,
    val dialogue: DialogueLine?,
    val shotSize: ShotSize,
    val angle: CameraAngle,
    val lensMm: Int,
    val cameraMove: CameraMove,
    val light: LightKind,
    val poseStart: String,
    val poseEnd: String,
    val screenDirection: String,
    val axisSide: String,
    val transition: String,
    val directorNote: String,
    val reAnchor: Boolean,
    // v1.4.2 — id efek Perpustakaan Efek scene ini ("" = tanpa efek).
    // Diisi otomatis oleh build() dari trigger kalimat, atau manual lewat
    // chip efek per scene (cycleSceneEffect). Persist di ScriptSceneItem.
    val effectId: String = ""
) {
    fun timecode(): String = ScriptEngine.timecodeFor(orderIndex)
}

data class ScriptDraft(
    val input: ScriptInput,
    val scenes: List<DraftScene>,
    val totalClips: Int,
    val estimatedGenerations: Int,
    val estimatedCostId: String,
    val estimatedHoursId: String
)

// Pool aksi per beat — 8 aksi per beat (maks 8 klip per beat di film
// 10 menit), supaya TIDAK ADA aksi yang berulang dalam satu beat.
// Semua aksi: present tense, 1 aksi, <=12 kata. {P} = tokoh, {A} = antagonis.
object ScriptPools {
    fun actionsFor(genre: Genre, beatIndex: Int): List<String> {
        val action = listOf(
            listOf(
                "walks through the city street, alert and watchful",
                "trains alone in the empty gym as rain falls outside",
                "stands on the rooftop overlooking the sleeping city",
                "sharpens his focus in the quiet room, breathing steadily",
                "patrols the block, checking every shadow",
                "buys a coffee and watches the street from the window",
                "stretches his shoulders and cracks his knuckles",
                "rides his motorcycle through the empty streets"
            ),
            listOf(
                "hears a crash and turns toward the sound",
                "sees a shadow fall across the street and stops",
                "notices the city lights flicker and die",
                "finds a warning carved into the wall",
                "watches a stranger run past, terrified",
                "hears a low roar from above the rooftops",
                "receives a call and his face goes pale",
                "spots a massive footprint in the cracked asphalt"
            ),
            listOf(
                "studies the surroundings and makes a plan",
                "warns the crowd to run and take cover",
                "gathers his courage and steps into the open",
                "calls an old friend and asks for help",
                "memorizes the streets and picks his ground",
                "ties his shoes tighter and rolls his shoulders",
                "checks his fists and takes a fighting stance",
                "draws a line in the dust and waits"
            ),
            listOf(
                "trades blows with the enemy, testing its guard",
                "dodges a swinging arm and counters fast",
                "leaps onto a parked car and surveys the fight",
                "slides under a falling beam and rolls to his feet",
                "sprints across the rooftops as debris rains down",
                "swings from a loose cable across the gap",
                "hides behind a food cart as the enemy passes",
                "runs along the train tracks as headlights approach"
            ),
            listOf(
                "takes a heavy blow and staggers back",
                "watches his friend fall and grits his teeth",
                "loses his footing on the wet ground",
                "sees the city burn behind him and keeps moving",
                "clutches his injured arm and breathes hard",
                "finds his phone smashed on the wet pavement",
                "watches his motorcycle burn across the street",
                "reads a threatening message and deletes it"
            ),
            listOf(
                "is cornered in a dead-end alley by armed men",
                "drops to his knees as the enemy closes in",
                "lies on the wet asphalt as boots surround him",
                "reaches out but a boot pins his hand",
                "crawls toward the gutter as the men laugh",
                "presses his forehead to the cold ground",
                "watches the last light fade from the street",
                "lies still as the footsteps fade away"
            ),
            listOf(
                "rises slowly, fists clenched, eyes burning",
                "charges forward and throws a punch",
                "stands tall in the smoke as the enemy hesitates",
                "wrenches a metal pipe free and swings it",
                "shoves the leader aside and breaks through the door",
                "faces the enemy alone, breathing steadily",
                "wipes the blood from his lip and smiles",
                "roars and rushes back into the fight"
            ),
            listOf(
                "walks on as the city wakes behind him",
                "looks back once at the ruined city, then walks on",
                "helps a stranger to their feet and nods",
                "stands on the rooftop as the sky clears",
                "smiles quietly and walks home",
                "waves goodbye to the city that tested him",
                "sits on the curb and finally breathes out",
                "walks home through the quiet street"
            )
        )
        val generic = listOf(
            listOf(
                "walks slowly through the quiet street",
                "looks around the empty room",
                "opens the window and breathes in the morning air",
                "waters the plants on the windowsill",
                "sits on the porch and watches the street wake",
                "folds a letter and slips it into a drawer",
                "stirs a cup of tea and stares out the window",
                "ties his shoes and steps outside"
            ),
            listOf(
                "finds something unexpected and stops",
                "reads the letter twice, hands shaking",
                "picks up the phone and listens in silence",
                "opens the old box and lifts the lid slowly",
                "hears a knock and turns toward the door",
                "finds a photograph face-down on the floor",
                "reads a name he has not seen in years",
                "closes the letter and holds it to his chest"
            ),
            listOf(
                "makes a plan and gathers what he needs",
                "asks for help from a stranger",
                "writes a list and crosses out the first line",
                "packs a small bag by the door",
                "studies an old map under the lamp light",
                "calls a number he memorized long ago",
                "hides a key beneath the loose floorboard",
                "practices the words he will say tomorrow"
            ),
            listOf(
                "tries the plan and it almost works",
                "laughs for the first time in days",
                "runs through the market as stalls blur past",
                "dances alone in the empty kitchen",
                "climbs the hill and sees the whole town",
                "sings quietly while washing the dishes",
                "chases a dog down the narrow lane",
                "jumps the fence and lands laughing"
            ),
            listOf(
                "learns the price of the plan",
                "loses something he cannot replace",
                "watches the rain wash the chalk away",
                "finds the door locked from the inside",
                "counts the coins and shakes his head",
                "reads the bad news twice in silence",
                "watches a friend walk away without looking back",
                "holds a broken cup and sighs"
            ),
            listOf(
                "faces the worst moment alone",
                "falls silent as everything breaks",
                "sits on the floor as the light fades",
                "stares at the wall as the clock ticks",
                "drops the letter and covers his face",
                "kneels in the rain and closes his eyes",
                "watches the last bus leave without him",
                "lies awake as the house creaks around him"
            ),
            listOf(
                "finds the strength to stand again",
                "chooses to fight instead of running",
                "wipes his eyes and ties his shoes",
                "picks up the phone and dials the number",
                "opens the door and steps into the light",
                "lifts the fallen chair and sets it straight",
                "takes a deep breath and knocks twice",
                "runs toward the sound instead of away"
            ),
            listOf(
                "walks toward a new morning",
                "smiles quietly as the light returns",
                "plants a seed in the old garden",
                "writes a new letter and seals it",
                "opens the shutters to the sunrise",
                "hangs the photograph back on the wall",
                "waves at the neighbor and walks on",
                "sits on the porch as the evening settles"
            )
        )
        val idx = beatIndex % 8
        return if (genre == Genre.ACTION) action[idx] else generic[idx]
    }

    // v1.4.2 — cerita kurir + chip (pool Action lama v1.0–v1.4.1) TIDAK
    // LAGI jadi default. Ia hidup sebagai TEMPLATE KISAH EKSPLISIT:
    // hanya dipakai kalau kalimatnya memang tentang kurir/paket/chip
    // (isCourierStory), atau lewat template "Kurir Terakhir" di wizard.
    // Default Action baru = pool netral di actionsFor (tanpa kurir/chip).
    fun isCourierStory(synopsis: String): Boolean {
        val lower = " " + synopsis.lowercase() + " "
        return listOf(
            "kurir", "paket", "antar", "pengiriman", "delivery", "chip",
            "ojek", "ojol", "kiriman"
        ).any { lower.contains(it) }
    }

    fun courierActionsFor(beatIndex: Int): List<String> {
        val courier = listOf(
            listOf(
                "walks through the morning market carrying a delivery bag",
                "weaves his motorcycle through slow traffic",
                "delivers a parcel and waves goodbye",
                "checks his phone while waiting at a red light",
                "parks his motorcycle and stretches his shoulders",
                "buys a coffee and sips it slowly",
                "greets a shopkeeper and laughs warmly",
                "rides past the old harbor as gulls circle"
            ),
            listOf(
                "opens the package and finds a glowing chip inside",
                "stares at the chip as it pulses with light",
                "hides the chip in his jacket pocket",
                "looks over his shoulder as footsteps approach",
                "closes the package and tucks it under his arm",
                "reads the delivery note twice, hands shaking",
                "locks the door and pulls the curtain shut",
                "whispers a warning to himself and nods"
            ),
            listOf(
                "studies a city map and marks three escape routes",
                "meets a stranger who warns him to run",
                "packs a small bag and locks the door",
                "calls an old friend and asks for help",
                "counts his money and hides it in his boot",
                "memorizes the address and burns the paper",
                "checks the chip once more before dawn",
                "draws a route on the fogged window"
            ),
            listOf(
                "sprints across rooftops as drones scan below",
                "slides under a closing shutter and rolls to his feet",
                "leaps between balconies above the crowded street",
                "dodges through the night market as stalls crash behind him",
                "swings across a gap on a loose cable",
                "slides down a drainpipe and lands in an alley",
                "hides behind a food cart as a drone passes",
                "runs along the train tracks as headlights approach"
            ),
            listOf(
                "discovers his friend betrayed him for money",
                "watches the chip's light fade as the signal dies",
                "runs through the rain as sirens close in",
                "hides in a stairwell, breathing hard",
                "finds his phone smashed on the wet pavement",
                "watches his motorcycle burn across the street",
                "clutches the chip as the rain soaks him",
                "reads a threatening message and deletes it"
            ),
            listOf(
                "is cornered in a dead-end alley by armed men",
                "drops to his knees as the chip is torn away",
                "lies on the wet asphalt as boots surround him",
                "reaches for the chip but a boot pins his hand",
                "crawls toward the gutter as the men laugh",
                "presses his forehead to the cold ground",
                "watches the chip disappear into a black van",
                "lies still as the footsteps fade away"
            ),
            listOf(
                "rises slowly, fists clenched, eyes burning",
                "charges forward and throws a punch",
                "snatches the chip back and sprints for the exit",
                "stands tall in the smoke as the men hesitate",
                "wrenches a metal pipe free and swings it",
                "shoves the leader aside and breaks through the door",
                "grabs the chip from the table and runs",
                "faces the men alone, breathing steadily"
            ),
            listOf(
                "walks into the sunrise, chip secured in his pocket",
                "hands the chip to the woman who waited for him",
                "looks back once at the ruined city, then walks on",
                "rides away as the city wakes behind him",
                "watches the sun rise over the harbor",
                "smiles quietly and pockets the chip for good",
                "waves goodbye to the city that chased him",
                "walks home as the morning light returns"
            )
        )
        return courier[beatIndex % 8]
    }

    // Pool cerita untuk satu beat: kurir kalau kalimatnya memang kisah
    // kurir, kalau tidak pool netral actionsFor (default Action baru).
    fun actionsForStory(input: ScriptInput, beatIndex: Int): List<String> {
        return if (input.genre == Genre.ACTION && isCourierStory(input.synopsis)) {
            courierActionsFor(beatIndex)
        } else {
            actionsFor(input.genre, beatIndex)
        }
    }

    // Dialog per genre v1.4.3 — tiap genre punya suaranya sendiri di
    // tiap peran dramatis (Discovery / Tension / Climax / Closing).
    // Peran dihitung dari totalBeats, BUKAN dari nomor beat mentah:
    // di struktur FIVE, beat 3 = Klimaks dan beat 4 = Penutup,
    // sementara di EIGHT beat 3 = Fun & Games dan beat 4 = Harga
    // Kemenangan. Tabel lama (per nomor beat) menaruh kalimat
    // pengkhianatan "You sold me out." — yang ditulis untuk EIGHT
    // beat 4 — di scene Penutup film Bejo 1 menit (audit lapangan
    // 16 Nov 13:25 WIB). Tiap peran punya 3 baris, dipilih
    // (seed + beatIdx) % 3; semua baris <= 8 kata (dialog <= 2
    // kata/detik di jendela 5-10 detik).
    // 4 kalimat generik lama DIRELOKASI, bukan dihapus: "I need your
    // help." -> DRAMA Tension; "Did you see that?" -> DOKUMENTER
    // Discovery; "You sold me out." -> THRILLER Tension;
    // "Please. Not like this." -> DRAMA Climax.
    enum class DialogueRole { DISCOVERY, TENSION, CLIMAX, CLOSING }

    fun dialogueRoleFor(beatIndex: Int, totalBeats: Int): DialogueRole = when {
        beatIndex == 1 -> DialogueRole.DISCOVERY
        beatIndex == totalBeats - 2 -> DialogueRole.CLIMAX
        beatIndex == totalBeats - 1 -> DialogueRole.CLOSING
        else -> DialogueRole.TENSION
    }

    private val DIALOGUES_BY_GENRE: Map<Genre, Map<DialogueRole, List<String>>> = mapOf(
        Genre.ACTION to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "What is this thing?", "Something is wrong here.", "This isn't right."
            ),
            DialogueRole.TENSION to listOf(
                "Come on. Is that all?", "You picked the wrong fight.", "Stay down."
            ),
            DialogueRole.CLIMAX to listOf(
                "Not while I'm still standing.", "This ends now.", "Get up. We finish this."
            ),
            DialogueRole.CLOSING to listOf(
                "It's done.", "Let's go home.", "It's over."
            )
        ),
        Genre.DRAMA to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Are you sure?", "I don't understand.", "What happened to us?"
            ),
            DialogueRole.TENSION to listOf(
                "I need your help.", "Wait for me.", "I can't do this."
            ),
            DialogueRole.CLIMAX to listOf(
                "Forgive me.", "Please. Not like this.", "I remember everything now."
            ),
            DialogueRole.CLOSING to listOf(
                "Goodbye.", "We made it.", "Let's start over."
            )
        ),
        Genre.ROMANSA to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Is that really you?", "I was looking for you.", "You came."
            ),
            DialogueRole.TENSION to listOf(
                "Stay with me.", "Don't go.", "I was afraid of this."
            ),
            DialogueRole.CLIMAX to listOf(
                "I love you.", "Choose me.", "I can't lose you."
            ),
            DialogueRole.CLOSING to listOf(
                "Stay.", "Forever, then.", "I'm home."
            )
        ),
        Genre.HOROR to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Did you hear that?", "Who's there?", "Something is here."
            ),
            DialogueRole.TENSION to listOf(
                "Don't look back.", "Stay close to me.", "It's behind us."
            ),
            DialogueRole.CLIMAX to listOf(
                "Run!", "It's not human.", "Don't let it take me."
            ),
            DialogueRole.CLOSING to listOf(
                "Is it over?", "We survived.", "Never again."
            )
        ),
        Genre.SCI_FI to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "What is that?", "The readings are off the scale.", "It's alive."
            ),
            DialogueRole.TENSION to listOf(
                "The ship is failing.", "We have minutes left.", "Cut the power."
            ),
            DialogueRole.CLIMAX to listOf(
                "Initiate the sequence.", "Hold it together.", "Now!"
            ),
            DialogueRole.CLOSING to listOf(
                "We made it out.", "Set course for home.", "Begin again."
            )
        ),
        Genre.FANTASI to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "The old magic stirs.", "Who calls me?", "The prophecy was true."
            ),
            DialogueRole.TENSION to listOf(
                "The realm is falling.", "Hold the line.", "Trust the light."
            ),
            DialogueRole.CLIMAX to listOf(
                "By blade and by oath.", "The darkness ends here.", "For the kingdom!"
            ),
            DialogueRole.CLOSING to listOf(
                "Peace at last.", "The realm is healed.", "A new age begins."
            )
        ),
        Genre.THRILLER to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Someone is watching us.", "This doesn't add up.", "Check the locks."
            ),
            DialogueRole.TENSION to listOf(
                "You sold me out.", "It was you all along.", "Trust no one."
            ),
            DialogueRole.CLIMAX to listOf(
                "The truth comes out now.", "You can't run.", "It's over."
            ),
            DialogueRole.CLOSING to listOf(
                "It's done.", "Burn it all.", "We disappear tonight."
            )
        ),
        Genre.KOMEDI to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Wait, what?", "That can't be right.", "Oh no. Not again."
            ),
            DialogueRole.TENSION to listOf(
                "This is fine. Totally fine.", "Why is it always me?", "Run! I mean, walk fast!"
            ),
            DialogueRole.CLIMAX to listOf(
                "Okay, that actually worked.", "Did anyone see that?", "I meant to do that."
            ),
            DialogueRole.CLOSING to listOf(
                "We did it!", "Pizza's on me.", "Let's never speak of this."
            )
        ),
        Genre.DOKUMENTER to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "Did you see that?", "Look at this.", "No one believed it."
            ),
            DialogueRole.TENSION to listOf(
                "We keep filming.", "The truth is here.", "Stay with the story."
            ),
            DialogueRole.CLIMAX to listOf(
                "This changes everything.", "The evidence is clear.", "There it is."
            ),
            DialogueRole.CLOSING to listOf(
                "And so it remains.", "The story continues.", "We were there."
            )
        ),
        Genre.SEJARAH to mapOf(
            DialogueRole.DISCOVERY to listOf(
                "History remembers this day.", "Who are these people?", "The old world is gone."
            ),
            DialogueRole.TENSION to listOf(
                "Hold the line.", "For those who came before.", "We cannot retreat."
            ),
            DialogueRole.CLIMAX to listOf(
                "For freedom!", "This is our moment.", "History is watching."
            ),
            DialogueRole.CLOSING to listOf(
                "It is done.", "A new era begins.", "We endure."
            )
        )
    )

    fun dialoguesFor(genre: Genre, beatIndex: Int, totalBeats: Int = 8): List<String> {
        if (beatIndex <= 0) return emptyList()
        val role = dialogueRoleFor(beatIndex, totalBeats)
        return DIALOGUES_BY_GENRE[genre]?.get(role) ?: emptyList()
    }

    fun wantsFor(beatIndex: Int): String {
        return listOf(
            "{P} wants an ordinary day",
            "{P} wants to know what is happening",
            "{P} wants a plan to stay alive",
            "{P} wants to outrun {A}",
            "{P} wants to trust someone",
            "{P} wants to survive",
            "{P} wants to take back what is his",
            "{P} wants a new beginning"
        )[beatIndex % 8]
    }

    fun obstaclesFor(beatIndex: Int): String {
        return listOf(
            "the city never sleeps",
            "{A} is already watching",
            "every exit is watched",
            "the city turns into a trap",
            "the betrayal costs everything",
            "{A} takes everything",
            "{A} stands in the way",
            "the past still follows"
        )[beatIndex % 8]
    }

    // Siklus pose akhir per klip — urutan TETAP supaya rantai pose masuk
    // akal (berdiri -> jalan -> berdiri tegak -> berlutut -> bangkit ->
    // berdiri -> jongkok -> berdiri tegak). Pelajaran audit v1.2: pose
    // acak ("berdiri -> berlutut -> telentang -> jongkok") kelihatan aneh.
    // Pose awal klip = pose akhir klip sebelumnya (rantai utuh).
    val POSE_CYCLE: List<String> = listOf(
        "standing still",
        "walking forward",
        "standing tall",
        "kneeling on the ground",
        "rising slowly",
        "standing still",
        "crouching low",
        "standing tall"
    )

    // Fragmen pose lama (v1.2) — dipertahankan untuk kompatibilitas,
    // build() v1.3 memakai POSE_CYCLE.
    val POSE_FRAGMENTS: List<String> = listOf(
        "standing still",
        "walking forward",
        "running hard",
        "crouching low",
        "kneeling on the ground",
        "lying flat",
        "rising slowly",
        "fist extended",
        "hand on the door",
        "looking over the shoulder",
        "breathing hard",
        "standing tall"
    )
}

object ScriptEngine {

    // 1 klip = 10 detik -> 6 klip per menit.
    fun clipCount(durationMinutes: Int): Int = durationMinutes * 6

    // Timecode klip ke-n (0-based): "00:00-00:10", "00:10-00:20", ...
    fun timecodeFor(orderIndex: Int): String {
        val start = orderIndex * 10
        val end = start + 10
        fun fmt(seconds: Int): String {
            val minutes = seconds / 60
            val secs = seconds % 60
            return "%02d:%02d".format(minutes, secs)
        }
        return "${fmt(start)}-${fmt(end)}"
    }

    // Alokasi klip per beat dari persentase durasi (murni aritmetika).
    // 8 sequence: jatah sama 12,5%, sisa ke sequence paling tegang dulu.
    // 5 beat (film <=3 menit): alokasi tetap per tabel.
    fun allocateClips(durationMinutes: Int): List<Int> {
        val total = clipCount(durationMinutes)
        if (durationMinutes <= 3) {
            return when (total) {
                6 -> listOf(1, 1, 2, 1, 1)
                else -> listOf(3, 3, 6, 4, 2)
            }
        }
        val beats = ScriptBeats.EIGHT.size
        val base = total / beats
        var remainder = total % beats
        val result = MutableList(beats) { base }
        // Sisa ke sequence paling tegang dulu: 6, 5, 4, 7, 3, 8, 2, 1.
        val tensionOrder = listOf(5, 4, 3, 6, 2, 7, 1, 0)
        for (idx in tensionOrder) {
            if (remainder <= 0) break
            result[idx] += 1
            remainder -= 1
        }
        return result
    }

    // Pola ukuran shot per klip dalam satu beat — bervariasi supaya tidak
    // ada dua klip berurutan yang ukurannya sama (aturan 30 derajat).
    private val SHOT_PATTERN: List<ShotSize> = listOf(
        ShotSize.WIDE,
        ShotSize.MEDIUM,
        ShotSize.CLOSEUP,
        ShotSize.MEDIUM_CLOSEUP,
        ShotSize.TWO_SHOT,
        ShotSize.EXTREME_CLOSEUP,
        ShotSize.WIDE,
        ShotSize.MEDIUM
    )

    // v1.4.1 — aksi netral per beat (tanpa kata waktu, tanpa lokasi
    // spesifik): fallback kalau SEMUA aksi pool satu beat bertabrakan
    // dengan waktu dunia (jarang; pool 8 aksi, yang berkala waktu cuma
    // 1-2). Dipakai sebagai pengganti pool, bukan tambahan.
    private val NEUTRAL_ACTIONS: List<String> = listOf(
        "walks down the street, looking around",
        "stops and looks at something in the distance",
        "studies the surroundings and makes a plan",
        "moves quickly through the street, testing his limits",
        "pauses, realizing the cost of what he did",
        "sinks down, defeated, as the street empties around him",
        "rises slowly, fists clenched, eyes burning",
        "walks on as the city wakes behind him"
    )

    // "Kenapa shot ini" — 1 kalimat penjelasan dari tension/genre/durasi.
    fun directorNoteFor(beat: BeatDef, shotSize: ShotSize, lensMm: Int, move: CameraMove): String {
        val reason = when {
            beat.tension <= 2 -> "dunia biasa butuh ketenangan — kamera diam, shot lebar."
            beat.tension <= 4 -> "ketegangan naik — kamera mulai bergerak, shot menengah."
            beat.tension <= 6 -> "ketegangan tinggi — shot makin ketat, lensa makin panjang."
            else -> "titik terendah/klimaks — shot paling ketat, kamera paling dekat."
        }
        return "Beat ${beat.nameId} — ketegangan ${beat.tension}/8. " +
            "${shotSize.labelId}, ${lensMm}mm, ${move.labelId}: $reason"
    }

    // v1.4.1 — aksi yang kata waktunya bertabrakan dengan waktu dunia
    // di-skip, lalu pool diisi kembali sampai ukuran semula dengan aksi
    // netral (supaya aturan "tidak ada aksi berulang dalam satu beat"
    // tetap utuh walau 1-2 aksi terfilter). Dipakai build() + clearEffect().
    fun usableActionsFor(actions: List<String>, beatIndex: Int, effectiveTimeOfDay: String): List<String> {
        val filtered = actions.filter { action ->
            !ScriptBeats.timeClashes(action, effectiveTimeOfDay)
        }
        val filled = (filtered + NEUTRAL_ACTIONS.filter { it !in filtered })
            .take(actions.size)
        return if (filled.isEmpty()) {
            listOf(NEUTRAL_ACTIONS[beatIndex % NEUTRAL_ACTIONS.size])
        } else {
            filled
        }
    }

    // v1.4.2 — tempel SATU efek ke scene: fragmen efek menggantikan aksi
    // (kecuali efek CAMERA yang fragmentEn-nya kosong — aksinya tetap),
    // override kamera/lensa/cahaya/sudut dari efek dipakai kalau ada.
    fun applyEffect(scene: DraftScene, effectId: String): DraftScene {
        val effect = EffectLibrary.byId(effectId) ?: return scene
        return scene.copy(
            actionEn = if (effect.fragmentEn.isNotBlank()) effect.fragmentEn else scene.actionEn,
            cameraMove = effect.cameraMove ?: scene.cameraMove,
            lensMm = effect.lensMm ?: scene.lensMm,
            light = effect.lightKind ?: scene.light,
            angle = effect.angle ?: scene.angle,
            effectId = effect.id
        )
    }

    // v1.4.2 — lepas efek dari scene (chip "Tanpa efek"): aksi kembali
    // ke pool cerita beat itu + kamera/lensa/cahaya kembali ke default
    // ketegangan beat. Dipakai cycleSceneEffect.
    fun clearEffect(scene: DraftScene, input: ScriptInput): DraftScene {
        val beats = ScriptBeats.forDuration(input.durationMinutes)
        val beat = beats.firstOrNull { it.index == scene.sequenceIndex }
            ?: return scene.copy(
                effectId = "",
                actionEn = NEUTRAL_ACTIONS[scene.sequenceIndex % NEUTRAL_ACTIONS.size]
            )
        val allocation = allocateClips(input.durationMinutes)
        val beatStart = allocation.take(beats.indexOf(beat)).sum()
        val clipIdx = (scene.orderIndex - beatStart).coerceIn(0, 7)
        val pool = ScriptPools.actionsForStory(input, beat.index)
        val worldTime = ScriptBeats.timeOfDayFromWorld(input.worldDescEn)
        val effectiveTime = worldTime ?: beat.timeOfDay
        val usable = usableActionsFor(pool, beat.index, effectiveTime)
        return scene.copy(
            effectId = "",
            actionEn = usable.getOrElse(clipIdx % usable.size) {
                NEUTRAL_ACTIONS[beat.index % NEUTRAL_ACTIONS.size]
            },
            cameraMove = if (clipIdx == 0) CameraMove.STATIC else ScriptBeats.moveForTension(beat.tension, clipIdx),
            lensMm = ScriptBeats.lensForTension(beat.tension),
            light = ScriptBeats.lightForTime(effectiveTime)
        )
    }

    fun build(input: ScriptInput, seed: Int = 0): ScriptDraft {
        val beats = ScriptBeats.forDuration(input.durationMinutes)
        val allocation = allocateClips(input.durationMinutes)
        val protagonist = input.protagonistName.ifBlank { "Raka" }
        val antagonist = input.antagonist.ifBlank { "a shadowy figure" }
        val worldShort = input.worldName.ifBlank { "Kota Tua" }
            .split(Regex("\\s+")).take(3).joinToString(" ").uppercase()

        val scenes = mutableListOf<DraftScene>()
        var globalClipIndex = 0
        var lastPoseEnd = "standing still"
        // v1.4.2 — ukuran shot scene sebelumnya (fix two-shot berurutan:
        // dua klip dialog berurutan tidak boleh sama-sama TWO_SHOT).
        var prevShotSize: ShotSize? = null

        beats.forEachIndexed { beatIdx, beat ->
            val clipCount = allocation.getOrElse(beatIdx) { 0 }
            if (clipCount <= 0) return@forEachIndexed
            val actions = ScriptPools.actionsForStory(input, beat.index)
            // v1.4.3 — dialog per genre + per peran dramatis; totalBeats
            // ikut supaya FIVE beat 3 = Klimaks, beat 4 = Penutup
            // (bukan nomor beat mentah EIGHT).
            val dialogues = ScriptPools.dialoguesFor(input.genre, beat.index, beats.size)
            val dialogueText = if (dialogues.isNotEmpty()) {
                dialogues[(seed + beatIdx) % dialogues.size]
            } else {
                ""
            }
            // Dialog menempel di TEPAT SATU klip per beat: klip ke-2 bila
            // beat punya >=2 klip, kalau tidak klip pertama. Tidak pernah 2x.
            val dialogueClipIdx = if (clipCount >= 2) 1 else 0
            val want = ScriptPools.wantsFor(beat.index)
                .replace("{P}", protagonist).replace("{A}", antagonist)
            val obstacle = ScriptPools.obstaclesFor(beat.index)
                .replace("{P}", protagonist).replace("{A}", antagonist)
            val screenDirection = if (beatIdx % 2 == 0) "left to right" else "right to left"
            val axisSide = if (beatIdx % 2 == 0) "left" else "right"
            // v1.4.1 — waktu dunia mengalahkan waktu beat (cahaya ikut
            // dunia), dan aksi yang kata waktunya bertabrakan dengan
            // waktu dunia di-skip. Kalau pool habis, aksi netral beat.
            val worldTime = ScriptBeats.timeOfDayFromWorld(input.worldDescEn)
            val effectiveTimeOfDay = worldTime ?: beat.timeOfDay
            val light = ScriptBeats.lightForTime(effectiveTimeOfDay)
            val lens = ScriptBeats.lensForTension(beat.tension)
            val usableActions = usableActionsFor(actions, beat.index, effectiveTimeOfDay)

            for (clipIdx in 0 until clipCount) {
                // Aksi BERBEDA tiap klip dalam satu beat (pool 8 aksi,
                // maks 8 klip per beat) — tidak ada aksi yang mengulang.
                val action = usableActions[(seed + clipIdx) % usableActions.size]
                // Ukuran shot: klip dialog dengan antagonis = TWO_SHOT
                // (kecuali pola sudah TWO_SHOT — pakai CLOSEUP supaya
                // tidak ada dua klip berurutan berukuran sama);
                // tanpa antagonis, TWO_SHOT di pola diganti MEDIUM.
                // v1.4.2 — klip sebelumnya sudah TWO_SHOT -> CLOSEUP
                // (cacat lapangan 16 Nov: Scene 4 + 5 export Bejo 6 scene
                // dua-duanya two-shot berurutan).
                val rawSize = SHOT_PATTERN[(clipIdx + beatIdx) % SHOT_PATTERN.size]
                val baseShotSize = when {
                    clipIdx == dialogueClipIdx && dialogueText.isNotBlank() &&
                        input.antagonist.isNotBlank() -> {
                        if (rawSize == ShotSize.TWO_SHOT) ShotSize.CLOSEUP else ShotSize.TWO_SHOT
                    }
                    rawSize == ShotSize.TWO_SHOT && input.antagonist.isBlank() -> ShotSize.MEDIUM
                    else -> rawSize
                }
                val shotSize = if (baseShotSize == ShotSize.TWO_SHOT && prevShotSize == ShotSize.TWO_SHOT) {
                    ShotSize.CLOSEUP
                } else {
                    baseShotSize
                }
                // v1.4.1 — BIRD hanya untuk dunia yang dilihat dari atas;
                // dunia jalanan (kota/gang/pasar/bengkel) selalu EYE.
                // Scene 01 tidak pernah BIRD (BIRD+STATIC+WIDE dilarang
                // di pembuka: first-frame eye-level mengunci framing).
                val angle = if (
                    (shotSize == ShotSize.WIDE || shotSize == ShotSize.EXTREME_WIDE) &&
                    clipIdx % 2 == 0 &&
                    globalClipIndex > 0 &&
                    ScriptBeats.worldSeenFromAbove(input.worldDescEn)
                ) {
                    CameraAngle.BIRD
                } else {
                    CameraAngle.EYE
                }
                val move = if (clipIdx == 0) {
                    CameraMove.STATIC
                } else {
                    ScriptBeats.moveForTension(beat.tension, clipIdx)
                }
                val poseStart = lastPoseEnd
                val poseEnd = ScriptPools.POSE_CYCLE[(seed + globalClipIndex) % ScriptPools.POSE_CYCLE.size]
                val dialogue = if (clipIdx == dialogueClipIdx && dialogueText.isNotBlank()) {
                    DialogueLine(
                        speaker = protagonist,
                        tactic = "",
                        tone = ScriptBeats.toneForTension(beat.tension),
                        textId = "",
                        textEn = dialogueText,
                        language = input.dialogueLang,
                        startSec = 5f,
                        endSec = 10f
                    )
                } else {
                    null
                }
                val slugline = if (dialogue != null) {
                    "INT. $worldShort - $effectiveTimeOfDay"
                } else {
                    "EXT. $worldShort - $effectiveTimeOfDay"
                }
                val transition = when {
                    globalClipIndex == 0 -> ""
                    clipIdx == 0 -> "MATCH CUT TO:"
                    else -> "CUT TO:"
                }
                // v1.4.2 — slot efek: fragmen efek MENGGANTIKAN aksi pool
                // di scene ini (satu klip = satu efek), plus override
                // kamera/lensa/cahaya/sudut dari efek. Efek CAMERA punya
                // fragmentEn kosong -> aksinya tetap dari pool, hanya
                // cara shot diambil yang berubah.
                val effectId = EffectLibrary.effectIdForScene(
                    beatIndex = beat.index,
                    clipIdx = clipIdx,
                    globalClipIndex = globalClipIndex,
                    totalBeats = beats.size,
                    detected = EffectLibrary.detectFromTokens(input.effectIds)
                )
                var scene = DraftScene(
                    sceneNumber = globalClipIndex + 1,
                    orderIndex = globalClipIndex,
                    sequenceIndex = beat.index,
                    beatName = beat.nameId,
                    slugline = slugline,
                    want = want,
                    obstacle = obstacle,
                    actionEn = action,
                    dialogue = dialogue,
                    shotSize = shotSize,
                    angle = angle,
                    lensMm = lens,
                    cameraMove = move,
                    light = light,
                    poseStart = poseStart,
                    poseEnd = poseEnd,
                    screenDirection = screenDirection,
                    axisSide = axisSide,
                    transition = transition,
                    directorNote = directorNoteFor(beat, shotSize, lens, move),
                    reAnchor = globalClipIndex > 0 && (globalClipIndex + 1) % 5 == 0,
                    effectId = effectId
                )
                if (effectId.isNotBlank()) {
                    scene = applyEffect(scene, effectId)
                }
                scenes.add(scene)
                prevShotSize = shotSize
                lastPoseEnd = poseEnd
                globalClipIndex += 1
            }
        }

        val total = scenes.size
        val minutes = total / 6
        return ScriptDraft(
            input = input,
            scenes = scenes,
            totalClips = total,
            estimatedGenerations = total,
            estimatedCostId = "±$total generasi • estimasi \$${minutes * 30}–\$${minutes * 50}",
            estimatedHoursId = "±${minutes * 2}–${minutes * 4} jam generate + review"
        )
    }

    // Kembangkan ulang SATU scene saja — scene lain tidak berubah.
    // Pose awal/akhir scene TIDAK diubah (rantai kontinuitas tetap utuh);
    // yang baru: aksi + ukuran shot + gerakan kamera dari pool (seed baru).
    fun regenerateScene(draft: ScriptDraft, sceneOrderIndex: Int, seed: Int): ScriptDraft {
        val rebuilt = build(draft.input, seed)
        val newScene = rebuilt.scenes.firstOrNull { it.orderIndex == sceneOrderIndex }
            ?: return draft
        val oldScene = draft.scenes.firstOrNull { it.orderIndex == sceneOrderIndex }
            ?: return draft
        val fixed = newScene.copy(
            poseStart = oldScene.poseStart,
            poseEnd = oldScene.poseEnd,
            dialogue = oldScene.dialogue,
            transition = oldScene.transition,
            // v1.4.2 — efek scene lama dipertahankan; kalau scene lama
            // "Tanpa efek" (""), pakai efek otomatis hasil rebuild.
            effectId = if (oldScene.effectId.isNotBlank()) oldScene.effectId else newScene.effectId
        )
        // v1.4.2 — override efek manual scene lama dipertahankan: kalau
        // scene lama punya efek (manual atau otomatis), tempel ulang di
        // atas scene hasil rebuild (aksi baru dari seed baru tetap kalah
        // oleh efek yang sudah dipilih user).
        val fixedWithEffect = if (oldScene.effectId.isNotBlank()) {
            applyEffect(fixed, oldScene.effectId)
        } else {
            fixed
        }
        val scenes = draft.scenes.map { scene ->
            if (scene.orderIndex == sceneOrderIndex) fixedWithEffect else scene
        }
        return draft.copy(scenes = scenes)
    }

    // Satu scene flat -> ShotInput wizard (dipakai generate massal + ekspor).
    // Global Lock proyek diterapkan di atasnya (lock menang).
    // Dialog menempel TEPAT SEKALI (di scene ini saja) — bukan 2x seperti v1.2.
    fun toShotInput(draft: ScriptDraft, scene: DraftScene, lock: GlobalLock? = null): ShotInput {
        val input = draft.input
        val base = ShotInput(
            title = "Scene ${scene.sceneNumber} — ${scene.beatName}",
            genre = input.genre,
            style = input.style,
            engine = input.engine,
            character = CharacterDna(
                input.protagonistName.ifBlank { "Raka" },
                input.protagonistDescEn.ifBlank { "a young man in his twenties, lean build, worn jacket" }
            ),
            secondCharacter = if (input.antagonist.isNotBlank()) {
                CharacterDna(
                    input.antagonist,
                    input.antagonistDescEn.ifBlank { "a shadowy figure, cold eyes, dark coat" }
                )
            } else {
                null
            },
            world = WorldLock(
                input.worldName.ifBlank { "Kota Tua" },
                input.worldDescEn.ifBlank { "an old port city at dusk, crumbling warehouses, neon signs" }
            ),
            shotSize = scene.shotSize,
            angle = scene.angle,
            lensMm = scene.lensMm,
            cameraMove = scene.cameraMove,
            light = scene.light,
            actionEn = scene.actionEn,
            poseStart = scene.poseStart,
            poseEnd = scene.poseEnd,
            materialEn = "",
            audioEn = "",
            dialogueName = scene.dialogue?.speaker ?: "",
            dialogueLine = scene.dialogue?.activeText() ?: "",
            dialogue = scene.dialogue,
            characterCard = input.characterCard,
            focusMode = FocusMode.FULL_STABLE,
            hideTactic = HideTactic.NONE,
            screenDirection = scene.screenDirection,
            axisSide = scene.axisSide,
            transition = scene.transition
        )
        // v1.4 — ambience dari Mode Cepat (ScriptInput) ditempel dulu,
        // lalu Global Lock proyek menimpa/menambah di atasnya.
        val withAmbience = if (input.ambienceEn.isNotBlank()) {
            GlobalLock(ambience = input.ambienceEn).applyTo(base)
        } else {
            base
        }
        return lock?.applyTo(withAmbience) ?: withAmbience
    }
}

// 3 kartu template di atas langkah 1 wizard naskah — sekali tap, form terisi.
object ScriptTemplates {
    fun action10Min(): ScriptInput = ScriptInput(
        title = "Kurir Terakhir",
        synopsis = "Raka, kurir muda, menerima paket misterius berisi chip yang bisa mematikan seluruh jaringan listrik kota. Pemburu bayaran mengejarnya semalaman; ia harus mengantar paket itu sebelum fajar.",
        genre = Genre.ACTION,
        durationMinutes = 10,
        emotion = "harapan",
        protagonistName = "Raka",
        protagonistDescEn = "a young courier in his twenties, lean build, worn jacket, tired eyes",
        antagonist = "Kolektor",
        antagonistDescEn = "a wealthy collector in a dark suit, cold eyes, calm voice",
        worldName = "Kota Tua",
        worldDescEn = "an old port city at dusk, crumbling warehouses, neon signs, rain-slicked streets",
        conflict = "paket berisi chip yang mematikan listrik kota",
        ending = ScriptEnding.HAPPY,
        style = VisualStyle.REALISTIC,
        engine = TargetEngine.FLOW
    )

    fun drama3Min(): ScriptInput = ScriptInput(
        title = "Surat Terakhir",
        synopsis = "Seorang ayah menemukan surat yang ditulis anaknya sepuluh tahun lalu, dan memutuskan menepati janji yang tertunda.",
        genre = Genre.DRAMA,
        durationMinutes = 3,
        emotion = "rindu",
        protagonistName = "Bapak Darma",
        protagonistDescEn = "an old man in his sixties, grey hair, worn batik shirt, gentle eyes",
        antagonist = "",
        worldName = "Rumah Tua",
        worldDescEn = "an old wooden house at dusk, peeling paint, warm lamp light, rain on the roof",
        conflict = "janji yang tertunda sepuluh tahun",
        ending = ScriptEnding.OPEN,
        style = VisualStyle.REALISTIC,
        engine = TargetEngine.FLOW
    )

    fun horror1Min(): ScriptInput = ScriptInput(
        title = "Ketukan Ketiga",
        synopsis = "Di rumah kosong, seseorang mendengar tiga ketukan dari dalam lemari — dan ketukan ketiga menjawab namanya.",
        genre = Genre.HOROR,
        durationMinutes = 1,
        emotion = "takut",
        protagonistName = "Mira",
        protagonistDescEn = "a young woman in her twenties, pale face, dark hair, trembling hands",
        antagonist = "Suara dari Lemari",
        antagonistDescEn = "an unseen presence in the dark wardrobe, whispering voice",
        worldName = "Rumah Kosong",
        worldDescEn = "an empty house at night, peeling walls, dust in the air, a dark wardrobe",
        conflict = "ketukan dari dalam lemari",
        ending = ScriptEnding.TRAGIC,
        style = VisualStyle.NOIR,
        engine = TargetEngine.FLOW
    )
}
