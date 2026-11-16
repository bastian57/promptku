package com.daftech.promptku.engine

// Model data v2.0 (alur baru: Ide -> Kembangkan Cerita -> Buat Prompt).
// Semua model di file ini Kotlin murni + JsonLite — bisa diuji lokal.
// Aturan: bible adalah SATU-SATUNYA sumber kebenaran cerita. Prompt
// scene dirender DARI bible (verbatim untuk dunia/cahaya/ambience),
// bukan dari tebakan ulang per scene.

enum class V2Provider(val label: String, val dailyLimit: Int) {
    GEMINI_FLASH("Gemini 3.8 Flash", 20),
    GEMINI_FLASH_LITE("Gemini 3.5 Flash-Lite", 500),
    GROQ_GPT_OSS("Groq gpt-oss-120b", 1000),
    GROQ_VISION("Groq Qwen 3.8 27B (Vision)", 1000);

    companion object {
        fun directorChain(): List<V2Provider> =
            listOf(GEMINI_FLASH, GEMINI_FLASH_LITE, GROQ_GPT_OSS)

        fun visionChain(): List<V2Provider> =
            listOf(GROQ_VISION, GEMINI_FLASH)
    }
}

/** Hasil satu panggilan LLM: teks mentah + provider yang dipakai. */
sealed class V2Call {
    data class Ok(val text: String, val provider: V2Provider) : V2Call()
    data class Fail(val message: String, val retryable: Boolean) : V2Call()
}

data class V2Character(
    val name: String,
    /** Tiga jangkar visual (12–15 kata total), verbatim di tiap prompt. */
    val anchorsEn: List<String>,
    val lookEn: String = "",
    val outfitEn: String = ""
)

data class V2Beat(
    val index: Int,
    val name: String,
    val summaryEn: String
)

data class V2Episode(
    val number: Int,
    val title: String,
    val logline: String,
    val sceneCount: Int
)

/** Bible cerita — hasil Director, disetujui user sebelum Prompter jalan. */
data class StoryBible(
    val title: String,
    val logline: String,
    val genre: Genre,
    val characters: List<V2Character>,
    val antagonistName: String,
    val antagonistDescEn: String,
    val worldName: String,
    val worldDescEn: String,
    val lightEn: String,
    val ambienceEn: String,
    val beats: List<V2Beat>,
    val episodes: List<V2Episode>,
    val durationMinutes: Int,
    /** Chip konfirmasi (bisa diubah user): tebakan yang dipakai Director. */
    val isCourier: Boolean = false,
    val sourceIdea: String = "",
    /** v2.1 — pilihan user di Layar Ide (bukan tebakan LLM). */
    val aspectRatio: String = "9:16",
    val dialogueLang: String = "ID",
    val episodeNumber: Int = 1
) {
    val hero: V2Character? get() = characters.firstOrNull()
    /** Jumlah scene flat (aturan Putra: 1 menit = 6 scene @10 detik). */
    val totalScenes: Int get() = (durationMinutes.coerceIn(1, 10) * 6)
}

data class V2VisionDesc(
    val imageIndex: Int,
    val label: String,
    val subjectName: String,
    val anchorsEn: List<String>,
    val settingEn: String,
    val paletteEn: String,
    val notesEn: String = ""
)

/** Satu scene dari Prompter (JSON mentah, sebelum render lokal). */
data class V2ScenePlan(
    val sceneNumber: Int,
    val actionEn: String,
    val cameraMove: String,
    val lensMm: Int,
    val shotSize: String,
    val angle: String,
    val dialogueId: String,
    val dialogueEn: String,
    val audioEn: String,
    val effectId: String,
    val wantEn: String,
    val obstacleEn: String,
    val transitionEn: String,
    /** v2.1 — narasi voiceover per scene (Indonesia, gaya Nalar) untuk SuaraKu. */
    val narrationId: String = ""
)

/** Satu scene SETELAH render lokal + gerbang validasi. */
data class V2ScenePrompt(
    val sceneNumber: Int,
    val plan: V2ScenePlan,
    val flowText: String,
    val klingText: String,
    val seedanceText: String,
    val wanText: String,
    val needsReview: Boolean = false,
    val reviewReason: String = "",
    /** v2.1 — user menandai scene ini sudah disetujui (tombol Setuju). */
    val approved: Boolean = false
)

object V2Models {

    private fun Json.Obj.strList(key: String): List<String> =
        arr(key)?.mapNotNull { (it as? Json.Str)?.value }.orEmpty()

    fun bibleFromJson(obj: Json.Obj): StoryBible? {
        val title = obj.str("title")?.trim().orEmpty()
        val world = obj.str("world_desc_en")?.trim().orEmpty()
        if (title.isBlank() || world.isBlank()) return null
        val characters = obj.arr("characters").orEmpty().mapNotNull { item ->
            val c = item as? Json.Obj ?: return@mapNotNull null
            val name = c.str("name")?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            V2Character(
                name = name,
                anchorsEn = c.strList("anchors_en").take(3),
                lookEn = c.str("look_en").orEmpty(),
                outfitEn = c.str("outfit_en").orEmpty()
            )
        }
        val beats = obj.arr("beats").orEmpty().mapNotNull { item ->
            val b = item as? Json.Obj ?: return@mapNotNull null
            val idx = b.num("index")?.toInt() ?: return@mapNotNull null
            V2Beat(
                index = idx,
                name = b.str("name").orEmpty(),
                summaryEn = b.str("summary_en").orEmpty()
            )
        }.sortedBy { it.index }
        val episodes = obj.arr("episodes").orEmpty().mapNotNull { item ->
            val e = item as? Json.Obj ?: return@mapNotNull null
            V2Episode(
                number = e.num("number")?.toInt() ?: return@mapNotNull null,
                title = e.str("title").orEmpty(),
                logline = e.str("logline").orEmpty(),
                sceneCount = e.num("scene_count")?.toInt() ?: 0
            )
        }.sortedBy { it.number }
        val genre = runCatching {
            Genre.valueOf(obj.str("genre")?.uppercase().orEmpty())
        }.getOrDefault(Genre.DRAMA)
        return StoryBible(
            title = title,
            logline = obj.str("logline").orEmpty(),
            genre = genre,
            characters = characters,
            antagonistName = obj.str("antagonist_name").orEmpty(),
            antagonistDescEn = obj.str("antagonist_desc_en").orEmpty(),
            worldName = obj.str("world_name").orEmpty(),
            worldDescEn = world,
            lightEn = obj.str("light_en").orEmpty(),
            ambienceEn = obj.str("ambience_en").orEmpty(),
            beats = beats,
            episodes = episodes,
            durationMinutes = (obj.num("duration_minutes")?.toInt() ?: 3).coerceIn(1, 10),
            isCourier = obj.bool("is_courier") ?: false,
            sourceIdea = obj.str("source_idea").orEmpty(),
            // v2.1 — default aman untuk bible lama (cache fix7 ke bawah).
            aspectRatio = obj.str("aspect_ratio")?.takeIf { it == "16:9" || it == "9:16" } ?: "9:16",
            dialogueLang = obj.str("dialogue_lang")?.takeIf { it == "EN" || it == "ID" } ?: "ID",
            episodeNumber = (obj.num("episode_number")?.toInt() ?: 1).coerceAtLeast(1)
        )
    }

    fun bibleToJson(bible: StoryBible): Json.Obj = Json.objOf(
        "title" to Json.strOf(bible.title),
        "logline" to Json.strOf(bible.logline),
        "genre" to Json.strOf(bible.genre.name),
        "characters" to Json.arrOf(*bible.characters.map { c ->
            Json.objOf(
                "name" to Json.strOf(c.name),
                "anchors_en" to Json.arrOf(*c.anchorsEn.map { Json.strOf(it) }.toTypedArray()),
                "look_en" to Json.strOf(c.lookEn),
                "outfit_en" to Json.strOf(c.outfitEn)
            )
        }.toTypedArray()),
        "antagonist_name" to Json.strOf(bible.antagonistName),
        "antagonist_desc_en" to Json.strOf(bible.antagonistDescEn),
        "world_name" to Json.strOf(bible.worldName),
        "world_desc_en" to Json.strOf(bible.worldDescEn),
        "light_en" to Json.strOf(bible.lightEn),
        "ambience_en" to Json.strOf(bible.ambienceEn),
        "beats" to Json.arrOf(*bible.beats.map { b ->
            Json.objOf(
                "index" to Json.numOf(b.index),
                "name" to Json.strOf(b.name),
                "summary_en" to Json.strOf(b.summaryEn)
            )
        }.toTypedArray()),
        "episodes" to Json.arrOf(*bible.episodes.map { e ->
            Json.objOf(
                "number" to Json.numOf(e.number),
                "title" to Json.strOf(e.title),
                "logline" to Json.strOf(e.logline),
                "scene_count" to Json.numOf(e.sceneCount)
            )
        }.toTypedArray()),
        "duration_minutes" to Json.numOf(bible.durationMinutes),
        "is_courier" to Json.boolOf(bible.isCourier),
        "source_idea" to Json.strOf(bible.sourceIdea),
        "aspect_ratio" to Json.strOf(bible.aspectRatio),
        "dialogue_lang" to Json.strOf(bible.dialogueLang),
        "episode_number" to Json.numOf(bible.episodeNumber)
    )

    fun visionFromJson(obj: Json.Obj, imageIndex: Int): V2VisionDesc? {
        val label = obj.str("label")?.trim().orEmpty()
        val subject = obj.str("subject_name")?.trim().orEmpty()
        if (label.isBlank() && subject.isBlank()) return null
        return V2VisionDesc(
            imageIndex = imageIndex,
            label = label.ifBlank { "Gambar ${imageIndex + 1}" },
            subjectName = subject,
            anchorsEn = obj.strList("anchors_en").take(3),
            settingEn = obj.str("setting_en").orEmpty(),
            paletteEn = obj.str("palette_en").orEmpty(),
            notesEn = obj.str("notes_en").orEmpty()
        )
    }

    fun scenePlansFromJson(arr: Json.Arr): List<V2ScenePlan> =
        arr.items.mapNotNull { item ->
            val o = item as? Json.Obj ?: return@mapNotNull null
            val num = o.num("scene_number")?.toInt() ?: return@mapNotNull null
            val action = o.str("action_en")?.trim().orEmpty()
            if (action.isBlank()) return@mapNotNull null
            V2ScenePlan(
                sceneNumber = num,
                actionEn = action,
                cameraMove = o.str("camera_move").orEmpty(),
                lensMm = o.num("lens_mm")?.toInt() ?: 35,
                shotSize = o.str("shot_size").orEmpty(),
                angle = o.str("angle").orEmpty(),
                dialogueId = o.str("dialogue_id").orEmpty(),
                dialogueEn = o.str("dialogue_en").orEmpty(),
                audioEn = o.str("audio_en").orEmpty(),
                effectId = o.str("effect_id").orEmpty(),
                wantEn = o.str("want_en").orEmpty(),
                obstacleEn = o.str("obstacle_en").orEmpty(),
                transitionEn = o.str("transition_en").orEmpty(),
                narrationId = o.str("narration_id").orEmpty()
            )
        }.sortedBy { it.sceneNumber }

    fun scenePlanToJson(plan: V2ScenePlan): Json.Obj = Json.objOf(
        "scene_number" to Json.numOf(plan.sceneNumber),
        "action_en" to Json.strOf(plan.actionEn),
        "camera_move" to Json.strOf(plan.cameraMove),
        "lens_mm" to Json.numOf(plan.lensMm),
        "shot_size" to Json.strOf(plan.shotSize),
        "angle" to Json.strOf(plan.angle),
        "dialogue_id" to Json.strOf(plan.dialogueId),
        "dialogue_en" to Json.strOf(plan.dialogueEn),
        "audio_en" to Json.strOf(plan.audioEn),
        "effect_id" to Json.strOf(plan.effectId),
        "want_en" to Json.strOf(plan.wantEn),
        "obstacle_en" to Json.strOf(plan.obstacleEn),
        "transition_en" to Json.strOf(plan.transitionEn),
        "narration_id" to Json.strOf(plan.narrationId)
    )
}
