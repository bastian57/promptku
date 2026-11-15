package com.daftech.promptku.engine

// Mesin naskah PromptKu v1.2 — rule-based offline, tanpa AI.
// Sinopsis -> naskah terstruktur -> shot list -> prompt multi-scene.
// Struktur hibrida (riset 15 Nov 2026): 8 Sequence sebagai wadah kerja,
// Save the Cat 15 beat sebagai checkpoint persentase, 3 babak sebagai header.
// Durasi <=3 menit: diringkas ke 5 beat film pendek, B Story dihapus.
// 1 klip = 10 detik (standar Flow). 1 scene dramatis = 1 beat -> 3-7 shot
// (coverage). 1 shot = 1 ShotInput = 1 prompt.

enum class ScriptEnding(val labelId: String) {
    HAPPY("Bahagia"),
    TRAGIC("Tragis"),
    OPEN("Terbuka")
}

enum class ShotPriority(val labelId: String) {
    A("A - Wajib"),
    B("B - Pendukung"),
    C("C - Nice-to-have")
}

enum class ShotFunction(val labelId: String) {
    ESTABLISHING("Establishing"),
    DETAIL("Detail"),
    RELATION("Relasi"),
    ACTION("Aksi"),
    SCALE("Skala")
}

// Satu beat/sequence: wadah kerja naskah. tension 1-8 menggerakkan
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
    val worldName: String = "",
    val worldDescEn: String = "",
    val conflict: String = "",
    val ending: ScriptEnding = ScriptEnding.HAPPY,
    val style: VisualStyle = VisualStyle.REALISTIC,
    val engine: TargetEngine = TargetEngine.FLOW
) {
    fun isReady(): Boolean = synopsis.trim().split(Regex("\\s+")).size >= 10
}

// Satu scene dramatis: 1 slugline, 1 want, 1 obstacle, 0-1 dialog.
data class DraftScene(
    val sequenceIndex: Int,
    val orderIndex: Int,
    val beatName: String,
    val slugline: String,
    val want: String,
    val obstacle: String,
    val actionEn: String,
    val dialogueName: String,
    val dialogueLine: String,
    val poseStart: String,
    val poseEnd: String,
    val screenDirection: String,
    val axisSide: String
)

// Satu shot = 1 klip 10 detik = 1 ShotInput = 1 prompt.
data class DraftShot(
    val sceneOrder: Int,
    val orderIndex: Int,
    val shotSize: ShotSize,
    val angle: CameraAngle,
    val lensMm: Int,
    val cameraMove: CameraMove,
    val light: LightKind,
    val actionEn: String,
    val poseStart: String,
    val poseEnd: String,
    val screenDirection: String,
    val axisSide: String,
    val priority: ShotPriority,
    val shotFunction: ShotFunction,
    val transition: String,
    val reAnchor: Boolean
)

data class ScriptDraft(
    val input: ScriptInput,
    val scenes: List<DraftScene>,
    val shots: List<DraftShot>,
    val totalClips: Int,
    val estimatedGenerations: Int,
    val estimatedCostId: String,
    val estimatedHoursId: String
)

// Pool aksi per beat — Action paling lengkap; genre lain memakai pool generik.
// Semua aksi: present tense, 1 aksi, <=12 kata. {P} = tokoh, {A} = antagonis.
object ScriptPools {
    fun actionsFor(genre: Genre, beatIndex: Int): List<String> {
        val action = listOf(
            listOf(
                "walks through the morning market carrying a delivery bag",
                "weaves his motorcycle through slow traffic",
                "delivers a parcel and waves goodbye",
                "checks his phone while waiting at a red light"
            ),
            listOf(
                "opens the package and finds a glowing chip inside",
                "stares at the chip as it pulses with light",
                "hides the chip in his jacket pocket",
                "looks over his shoulder as footsteps approach"
            ),
            listOf(
                "studies a city map and marks three escape routes",
                "meets a stranger who warns him to run",
                "packs a small bag and locks the door",
                "calls an old friend and asks for help"
            ),
            listOf(
                "sprints across rooftops as drones scan below",
                "slides under a closing shutter and rolls to his feet",
                "leaps between balconies above the crowded street",
                "dodges through the night market as stalls crash behind him"
            ),
            listOf(
                "discovers his friend betrayed him for money",
                "watches the chip's light fade as the signal dies",
                "runs through the rain as sirens close in",
                "hides in a stairwell, breathing hard"
            ),
            listOf(
                "is cornered in a dead-end alley by armed men",
                "drops to his knees as the chip is torn away",
                "lies on the wet asphalt as boots surround him",
                "reaches for the chip but a boot pins his hand"
            ),
            listOf(
                "rises slowly, fists clenched, eyes burning",
                "charges forward and throws a punch",
                "snatches the chip back and sprints for the exit",
                "stands tall in the smoke as the men hesitate"
            ),
            listOf(
                "walks into the sunrise, chip secured in his pocket",
                "hands the chip to the woman who waited for him",
                "looks back once at the ruined city, then walks on",
                "rides away as the city wakes behind him"
            )
        )
        val generic = listOf(
            listOf(
                "walks slowly through the quiet street",
                "looks around the empty room"
            ),
            listOf(
                "finds something unexpected and stops",
                "reads the letter twice, hands shaking"
            ),
            listOf(
                "makes a plan and gathers what he needs",
                "asks for help from a stranger"
            ),
            listOf(
                "tries the plan and it almost works",
                "laughs for the first time in days"
            ),
            listOf(
                "learns the price of the plan",
                "loses something he cannot replace"
            ),
            listOf(
                "faces the worst moment alone",
                "falls silent as everything breaks"
            ),
            listOf(
                "finds the strength to stand again",
                "chooses to fight instead of running"
            ),
            listOf(
                "walks toward a new morning",
                "smiles quietly as the light returns"
            )
        )
        val idx = beatIndex % 8
        return if (genre == Genre.ACTION) action[idx] else generic[idx]
    }

    fun dialoguesFor(genre: Genre, beatIndex: Int): List<String> {
        val action = mapOf(
            1 to listOf("What is this thing?", "This was not the delivery."),
            2 to listOf("I need your help.", "There is no going back now."),
            3 to listOf("Follow me. Stay low.", "Did you see that?"),
            4 to listOf("You sold me out.", "It was never about the money."),
            5 to listOf("It's over.", "Please. Not like this."),
            6 to listOf("Not while I'm still standing.", "Get up. We finish this."),
            7 to listOf("It's done.", "Let's go home.")
        )
        val generic = mapOf(
            1 to listOf("Are you sure?", "I don't understand."),
            2 to listOf("Wait for me.", "Thank you."),
            3 to listOf("Did you hear that?", "Stay close to me."),
            4 to listOf("I remember now.", "It was you all along."),
            5 to listOf("Forgive me.", "I can't do this."),
            6 to listOf("One more time.", "I am not afraid."),
            7 to listOf("Goodbye.", "We made it.")
        )
        return if (genre == Genre.ACTION) {
            action[beatIndex] ?: emptyList()
        } else {
            generic[beatIndex] ?: emptyList()
        }
    }

    fun wantsFor(beatIndex: Int): String {
        return listOf(
            "{P} wants an ordinary day",
            "{P} wants to know what the package hides",
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

    // Pose akhir per aksi — dipakai sebagai ENTRY STATE shot berikut.
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

    fun build(input: ScriptInput, seed: Int = 0): ScriptDraft {
        val beats = ScriptBeats.forDuration(input.durationMinutes)
        val allocation = allocateClips(input.durationMinutes)
        val protagonist = input.protagonistName.ifBlank { "Raka" }
        val antagonist = input.antagonist.ifBlank { "a shadowy figure" }
        val worldShort = input.worldName.ifBlank { "Kota Tua" }
            .split(Regex("\\s+")).take(3).joinToString(" ").uppercase()

        val scenes = mutableListOf<DraftScene>()
        val shots = mutableListOf<DraftShot>()
        var globalShotIndex = 0
        var lastPoseEnd = "standing still"

        beats.forEachIndexed { beatIdx, beat ->
            val shotCount = allocation.getOrElse(beatIdx) { 0 }
            if (shotCount <= 0) return@forEachIndexed
            val actions = ScriptPools.actionsFor(input.genre, beat.index)
            val action = actions[(seed + beatIdx) % actions.size]
            val dialogues = ScriptPools.dialoguesFor(input.genre, beat.index)
            val dialogueLine = if (dialogues.isNotEmpty()) {
                dialogues[(seed + beatIdx) % dialogues.size]
            } else {
                ""
            }
            val isDialogScene = dialogueLine.isNotBlank()
            val slugline = if (isDialogScene) {
                "INT. $worldShort - ${beat.timeOfDay}"
            } else {
                "EXT. $worldShort - ${beat.timeOfDay}"
            }
            val want = ScriptPools.wantsFor(beat.index)
                .replace("{P}", protagonist).replace("{A}", antagonist)
            val obstacle = ScriptPools.obstaclesFor(beat.index)
                .replace("{P}", protagonist).replace("{A}", antagonist)
            val screenDirection = if (beatIdx % 2 == 0) "left to right" else "right to left"
            val axisSide = if (beatIdx % 2 == 0) "left" else "right"
            val poseStartScene = lastPoseEnd
            val poseEndScene = ScriptPools.POSE_FRAGMENTS[(seed + beatIdx * 3) % ScriptPools.POSE_FRAGMENTS.size]

            val scene = DraftScene(
                sequenceIndex = beat.index,
                orderIndex = beatIdx,
                beatName = beat.nameId,
                slugline = slugline,
                want = want,
                obstacle = obstacle,
                actionEn = action,
                dialogueName = if (isDialogScene) protagonist else "",
                dialogueLine = dialogueLine,
                poseStart = poseStartScene,
                poseEnd = poseEndScene,
                screenDirection = screenDirection,
                axisSide = axisSide
            )
            scenes.add(scene)

            // Pecah scene -> shot dengan pola coverage.
            val pattern = coveragePattern(isDialogScene)
            val functions = coverageFunctions(isDialogScene)
            val light = ScriptBeats.lightForTime(beat.timeOfDay)
            val lens = ScriptBeats.lensForTension(beat.tension)
            var shotPoseStart = poseStartScene
            for (shotIdx in 0 until shotCount) {
                val patternIdx = shotIdx % pattern.size
                val shotSize = pattern[patternIdx]
                val angle = if (shotSize == ShotSize.WIDE || shotSize == ShotSize.EXTREME_WIDE) {
                    if (shotIdx % 2 == 0) CameraAngle.BIRD else CameraAngle.EYE
                } else {
                    CameraAngle.EYE
                }
                val move = if (shotIdx == 0) {
                    CameraMove.STATIC
                } else {
                    ScriptBeats.moveForTension(beat.tension, shotIdx)
                }
                val shotPoseEnd = if (shotIdx == shotCount - 1) {
                    poseEndScene
                } else {
                    ScriptPools.POSE_FRAGMENTS[(seed + globalShotIndex + 1) % ScriptPools.POSE_FRAGMENTS.size]
                }
                val priority = when {
                    shotCount == 1 -> ShotPriority.A
                    shotIdx == 0 || shotIdx == shotCount - 1 -> ShotPriority.A
                    shotIdx % 2 == 0 -> ShotPriority.B
                    else -> ShotPriority.C
                }
                val transition = when {
                    globalShotIndex == 0 -> ""
                    shotIdx == 0 -> "MATCH CUT TO:"
                    else -> "CUT TO:"
                }
                shots.add(
                    DraftShot(
                        sceneOrder = beatIdx,
                        orderIndex = shotIdx,
                        shotSize = shotSize,
                        angle = angle,
                        lensMm = lens,
                        cameraMove = move,
                        light = light,
                        actionEn = action,
                        poseStart = shotPoseStart,
                        poseEnd = shotPoseEnd,
                        screenDirection = screenDirection,
                        axisSide = axisSide,
                        priority = priority,
                        shotFunction = functions[patternIdx % functions.size],
                        transition = transition,
                        reAnchor = globalShotIndex > 0 && globalShotIndex % 5 == 4
                    )
                )
                shotPoseStart = shotPoseEnd
                globalShotIndex += 1
            }
            lastPoseEnd = poseEndScene
        }

        val total = shots.size
        val generations = total * 3
        val minutes = total / 6
        return ScriptDraft(
            input = input,
            scenes = scenes,
            shots = shots,
            totalClips = total,
            estimatedGenerations = generations,
            estimatedCostId = "±$generations generasi • estimasi \$${minutes * 30}–\$${minutes * 50}",
            estimatedHoursId = "±${minutes * 2}–${minutes * 4} jam generate + review"
        )
    }

    // Coverage: dialog 2 orang = master + OTS + CU + insert + cutaway;
    // aksi = wide penetap + medium + CU puncak + insert + exit wide.
    fun coveragePattern(isDialogScene: Boolean): List<ShotSize> {
        return if (isDialogScene) {
            listOf(
                ShotSize.TWO_SHOT,
                ShotSize.OVER_SHOULDER,
                ShotSize.CLOSEUP,
                ShotSize.MEDIUM_CLOSEUP,
                ShotSize.CLOSEUP,
                ShotSize.EXTREME_CLOSEUP
            )
        } else {
            listOf(
                ShotSize.WIDE,
                ShotSize.MEDIUM,
                ShotSize.CLOSEUP,
                ShotSize.EXTREME_CLOSEUP,
                ShotSize.WIDE
            )
        }
    }

    fun coverageFunctions(isDialogScene: Boolean): List<ShotFunction> {
        return if (isDialogScene) {
            listOf(
                ShotFunction.RELATION,
                ShotFunction.RELATION,
                ShotFunction.ACTION,
                ShotFunction.DETAIL,
                ShotFunction.ACTION,
                ShotFunction.DETAIL
            )
        } else {
            listOf(
                ShotFunction.ESTABLISHING,
                ShotFunction.ACTION,
                ShotFunction.ACTION,
                ShotFunction.DETAIL,
                ShotFunction.SCALE
            )
        }
    }

    // Kembangkan ulang SATU sequence saja — sequence lain tidak berubah.
    fun regenerateSequence(draft: ScriptDraft, sequenceOrder: Int, seed: Int): ScriptDraft {
        val rebuilt = build(draft.input, seed)
        val keptScenes = draft.scenes.filter { it.orderIndex != sequenceOrder }
        val keptShots = draft.shots.filter { it.sceneOrder != sequenceOrder }
        val newScenes = rebuilt.scenes.filter { it.orderIndex == sequenceOrder }
        val newShots = rebuilt.shots.filter { it.sceneOrder == sequenceOrder }
        // Rantai pose: pose awal sequence baru mengikuti pose akhir scene sebelumnya.
        val prevPoseEnd = draft.scenes
            .filter { it.orderIndex < sequenceOrder }
            .maxByOrNull { it.orderIndex }?.poseEnd ?: "standing still"
        val fixedScenes = newScenes.map { it.copy(poseStart = prevPoseEnd) }
        val fixedShots = newShots.mapIndexed { idx, shot ->
            if (idx == 0) shot.copy(poseStart = prevPoseEnd) else shot
        }
        // Rantai pose di batas AKHIR sequence baru: scene berikut memakai
        // pose akhir sequence baru sebagai pose awalnya.
        val newLastPoseEnd = fixedScenes.lastOrNull()?.poseEnd ?: prevPoseEnd
        val allScenes = (keptScenes + fixedScenes).sortedBy { it.orderIndex }
            .map { scene ->
                if (scene.orderIndex == sequenceOrder + 1) {
                    scene.copy(poseStart = newLastPoseEnd)
                } else {
                    scene
                }
            }
        val allShots = (keptShots + fixedShots).sortedWith(
            compareBy({ it.sceneOrder }, { it.orderIndex })
        ).map { shot ->
            if (shot.sceneOrder == sequenceOrder + 1 && shot.orderIndex == 0) {
                shot.copy(poseStart = newLastPoseEnd)
            } else {
                shot
            }
        }
        return draft.copy(scenes = allScenes, shots = allShots)
    }

    // Satu shot -> ShotInput wizard (dipakai generate massal + ekspor).
    // Global Lock proyek diterapkan di atasnya (lock menang).
    fun toShotInput(draft: ScriptDraft, shot: DraftShot, lock: GlobalLock? = null): ShotInput {
        val input = draft.input
        val scene = draft.scenes.firstOrNull { it.orderIndex == shot.sceneOrder }
        val base = ShotInput(
            title = "S${shot.sceneOrder + 1} - Shot ${shot.orderIndex + 1}",
            genre = input.genre,
            style = input.style,
            engine = input.engine,
            character = CharacterDna(
                input.protagonistName.ifBlank { "Raka" },
                input.protagonistDescEn.ifBlank { "a young man in his twenties, lean build, worn jacket" }
            ),
            secondCharacter = if (input.antagonist.isNotBlank()) {
                CharacterDna(input.antagonist, "a shadowy figure, cold eyes, dark coat")
            } else {
                null
            },
            world = WorldLock(
                input.worldName.ifBlank { "Kota Tua" },
                input.worldDescEn.ifBlank { "an old port city at dusk, crumbling warehouses, neon signs" }
            ),
            shotSize = shot.shotSize,
            angle = shot.angle,
            lensMm = shot.lensMm,
            cameraMove = shot.cameraMove,
            light = shot.light,
            actionEn = shot.actionEn,
            poseStart = shot.poseStart,
            poseEnd = shot.poseEnd,
            materialEn = "",
            audioEn = "",
            dialogueName = scene?.dialogueName ?: "",
            dialogueLine = if (shot.shotSize == ShotSize.OVER_SHOULDER || shot.shotSize == ShotSize.CLOSEUP) {
                scene?.dialogueLine ?: ""
            } else {
                ""
            },
            focusMode = FocusMode.FULL_STABLE,
            hideTactic = HideTactic.NONE,
            screenDirection = shot.screenDirection,
            axisSide = shot.axisSide,
            transition = shot.transition
        )
        return lock?.applyTo(base) ?: base
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
        worldName = "Rumah Kosong",
        worldDescEn = "an empty house at night, peeling walls, dust in the air, a dark wardrobe",
        conflict = "ketukan dari dalam lemari",
        ending = ScriptEnding.TRAGIC,
        style = VisualStyle.NOIR,
        engine = TargetEngine.FLOW
    )
}
