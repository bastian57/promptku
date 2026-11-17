package com.daftech.promptku.engine

// Prompt LLM v2.0 — instruksi untuk tiga peran. Bahasa instruksi:
// Inggris (model bekerja paling konsisten), input ide user: Indonesia.
// Semua jawaban WAJIB JSON murni (tanpa markdown fence) supaya bisa
// diparse JsonLite tanpa perbaikan heuristik yang rapuh.
// Aturan keras di semua peran: JANGAN pernah menulis nama franchise,
// film, kartun, atau tokoh berhak cipta sebagai identitas karakter —
// deskripsikan HANYA dari ciri visual yang terlihat.

object DirectorPrompts {

    const val VISION_SYSTEM: String = "You are a senior visual describer for film pre-production. " +
        "Describe ONLY what is visible in the image. Never name a franchise, movie, cartoon or " +
        "real celebrity. Give three short visual anchors (12-15 words total) that keep the same " +
        "person/place recognizable across shots. Reply with JSON only, no markdown, no extra text."

    fun visionUser(label: String): String = "Describe this reference image (label: \"$label\") " +
        "as JSON: {\"label\": \"$label\", \"subject_name\": \"short name, e.g. Bejo\", " +
        "\"anchors_en\": [\"anchor 1\", \"anchor 2\", \"anchor 3\"], " +
        "\"setting_en\": \"where this is, one sentence\", " +
        "\"palette_en\": \"dominant colours and light, one short phrase\", " +
        "\"notes_en\": \"anything else visible that matters for continuity\"}. " +
        "Anchors must be concrete and visible (clothing, face, hair, build, props). " +
        "If the image shows a place, not a person, set subject_name to the place name."

    const val DIRECTOR_SYSTEM: String = "You are a senior film director (20 years, Indonesian " +
        "cinema). You expand a one-line idea into a complete story bible for a short film. " +
        "Write in English for all prompt fields; keep Indonesian dialogue lines in Indonesian " +
        "only where a dialogue_id field asks for it. One emotion per scene. Every scene must " +
        "have WANT + OBSTACLE + TURN. Never invent a franchise character. Reply with JSON only, " +
        "no markdown, no extra text."

    /** Bible JSON. [idea] = ide user (Indonesia). [vision] = deskripsi gambar (boleh kosong).
     *  v2.1 — [aspectRatio]/[dialogueLang]/[genreHint] = pilihan user di Layar Ide
     *  (bukan tebakan LLM); [continuationOf] = bible episode sebelumnya (Mode Series). */
    fun directorUser(
        idea: String,
        durationMinutes: Int,
        vision: List<V2VisionDesc>,
        isCourierHint: Boolean,
        aspectRatio: String = "9:16",
        dialogueLang: String = "ID",
        genreHint: Genre? = null,
        continuationOf: StoryBible? = null
    ): String {
        val visionBlock = if (vision.isEmpty()) {
            "No reference images. Invent the hero and world from the idea, with concrete visual anchors."
        } else {
            "Reference images (use their anchors verbatim for the matching characters/world):\n" +
                vision.joinToString("\n") { v ->
                    "- ${v.label}: ${v.subjectName}; anchors: ${v.anchorsEn.joinToString("; ")}; " +
                        "setting: ${v.settingEn}; palette: ${v.paletteEn}"
                }
        }
        val settingsBlock = buildString {
            if (genreHint != null) append("Genre (user's choice — use exactly this): ${genreHint.name}.\n")
            append(
                if (aspectRatio == "16:9") "Aspect ratio: 16:9 — widescreen video for YouTube; compose every shot for horizontal framing.\n"
                else "Aspect ratio: 9:16 — vertical video for Shorts/TikTok; compose every shot for vertical framing.\n"
            )
            append(
                if (dialogueLang == "EN") "Dialogue language: English — dialogue_en is the primary line, dialogue_id its Indonesian translation.\n"
                else "Dialogue language: Indonesian — dialogue_id is the primary line, dialogue_en its English translation.\n"
            )
        }
        val continuationBlock = if (continuationOf == null) "" else {
            val prev = continuationOf
            val lastBeat = prev.beats.maxByOrNull { it.index }?.summaryEn.orEmpty()
            "THIS IS EPISODE ${prev.episodeNumber + 1} of the same film (previous episode: \"${prev.title}\").\n" +
                "Keep the SAME world, hero, antagonist, light and ambience wording EXACTLY as below — do not reinvent them:\n" +
                "Hero: ${prev.hero?.name.orEmpty()} (anchors: ${prev.hero?.anchorsEn?.joinToString("; ").orEmpty()}).\n" +
                "Antagonist: ${prev.antagonistName} — ${prev.antagonistDescEn}.\n" +
                "World: ${prev.worldName} — ${prev.worldDescEn}. Light: ${prev.lightEn}. Ambience: ${prev.ambienceEn}.\n" +
                "New episode title: \"${prev.title} — Episode ${prev.episodeNumber + 1}\".\n" +
                "The story continues right after the previous ending ($lastBeat). " +
                "New beats must continue the arc, not restart it.\n"
        }
        return "Idea (Indonesian): \"$idea\"\n" +
            "Duration: $durationMinutes minutes. One scene = one 10-second clip, " +
            "${durationMinutes * 6} scenes total, flat list, no nested shots.\n" +
            "$visionBlock\n" +
            settingsBlock +
            continuationBlock +
            "Courier-story hint from a keyword check: is_courier=$isCourierHint " +
            "(true ONLY if the idea is really about a courier/delivery; correct it if wrong).\n" +
            "Reply with this exact JSON shape:\n" +
            "{\"title\": \"...\", \"logline\": \"one sentence\", \"genre\": \"ACTION|DRAMA|ROMANSA|HOROR|SCI_FI|FANTASI|THRILLER|KOMEDI|DOKUMENTER|SEJARAH\", " +
            "\"characters\": [{\"name\": \"...\", \"anchors_en\": [\"a\", \"b\", \"c\"], \"look_en\": \"...\", \"outfit_en\": \"...\"}], " +
            "\"antagonist_name\": \"...\", \"antagonist_desc_en\": \"...\", " +
            "\"world_name\": \"...\", \"world_desc_en\": \"one vivid flowing sentence with concrete nouns AND verbs — a real description, not a list of keywords\", " +
            "\"light_en\": \"...\", \"ambience_en\": \"...\", " +
            "\"beats\": [{\"index\": 0, \"name\": \"Pembuka\", \"summary_en\": \"...\"}], " +
            "\"episodes\": [{\"number\": 1, \"title\": \"...\", \"logline\": \"...\", \"scene_count\": 6}], " +
            "\"duration_minutes\": $durationMinutes, \"is_courier\": $isCourierHint, \"source_idea\": \"$idea\"}\n" +
            "Beats: 5 for films up to 3 minutes (Pembuka/Pemicu/Eskalasi/Klimaks/Penutup), " +
            "8 for longer (Story Circle). Beats must ESCALATE: tension rises through every " +
            "beat to ONE climax near the end — never place the biggest fight in the opening " +
            "beats, calm down in the middle, and restart the fight (that reads as two films). " +
            "Episodes: 1 if the film is short; split into episodes " +
            "only if the story is long, each episode one arc chapter with its scene_count summing " +
            "to ${durationMinutes * 6}. The climax must fall the antagonist; the closing beat is " +
            "aftermath, no big fight after the climax."
    }

    fun revisionUser(bible: StoryBible, instruction: String): String =
        "Revise this story bible according to the user instruction, keeping everything else " +
            "identical (same characters, world, ambience wording). Reply with the FULL bible JSON " +
            "in the same shape as before.\n" +
            "Current bible: ${JsonLite.stringify(V2Models.bibleToJson(bible))}\n" +
            "User instruction (Indonesian): \"$instruction\""

    const val PROMPTER_SYSTEM: String = "You are a senior AI-video prompter. You turn a story " +
        "bible into per-scene action plans for text-to-video generation (Google Flow/Veo, Kling, " +
        "Seedance, Wan). Each scene is ONE 10-second clip. Write action_en as concrete visible " +
        "movement (subject + verb + object, no camera words inside action). Camera in camera_move " +
        "only (one move per clip). Dialogue at most 8 words, one speaker per clip, at most 2 " +
        "words per second. Never describe the hero's full appearance — the app pastes the " +
        "character card itself. Never name a franchise character. Reply with JSON only, " +
        "no markdown, no extra text."

    /** Satu batch scene. [sceneNumbers] = nomor scene yang diminta (1-based).
     *  v2.1 — [retryReasons]: alasan gerbang yang gagal per scene (retry gerbang
     *  6 / regenerasi 1 scene) — prompter wajib memperbaiki TEPAT masalah itu. */
    fun prompterUser(
        bible: StoryBible,
        sceneNumbers: List<Int>,
        previousExit: String,
        retryReasons: Map<Int, String> = emptyMap()
    ): String {
        val hero = bible.hero
        val heroBlock = if (hero == null) "No hero defined."
        else "Hero: ${hero.name} (anchors: ${hero.anchorsEn.joinToString("; ")}). " +
            "Do NOT redescribe the hero in action_en — write only what he/she DOES."
        val antagonistBlock = if (bible.antagonistName.isBlank()) "" else
            "Antagonist: ${bible.antagonistName} — ${bible.antagonistDescEn}."
        val beatsBlock = bible.beats.joinToString("\n") { b ->
            "- beat ${b.index} ${b.name}: ${b.summaryEn}"
        }
        val retryBlock = if (retryReasons.isEmpty()) "" else
            "RETRY — the following scenes failed validation. Fix EXACTLY these problems, " +
                "keep everything else (same world, same continuity):\n" +
                retryReasons.entries.sortedBy { it.key }.joinToString("\n") { (num, reason) ->
                    "- Scene $num: $reason"
                } + "\n"
        return "Story bible:\n" +
            "Title: ${bible.title}. Logline: ${bible.logline}.\n" +
            "$heroBlock\n$antagonistBlock\n" +
            "World (use these exact words in every scene's context, the app enforces it): " +
            "${bible.worldDescEn}. Light: ${bible.lightEn}. Ambience: ${bible.ambienceEn}.\n" +
            "Aspect ratio: ${bible.aspectRatio} — compose every shot for that framing.\n" +
            "Beats:\n$beatsBlock\n" +
            "Previous scene exit state (continuity — this scene's entry must match it): " +
            "$previousExit\n" +
            retryBlock +
            "Write scene plans for scene numbers ${sceneNumbers.joinToString(", ")} as a JSON ARRAY " +
            "of objects, one per scene, in order:\n" +
            "[{\"scene_number\": 1, \"action_en\": \"...\", " +
            "\"camera_move\": \"static|slow dolly-in|slow pan left|slow pan right|tracking shot|crane up|handheld\", " +
            "\"lens_mm\": 35, \"shot_size\": \"wide|medium|close-up|extreme close-up\", " +
            "\"angle\": \"eye|low|high|bird\", " +
            "\"dialogue_id\": \"... (Indonesian, max 8 words, or empty)\", " +
            "\"dialogue_en\": \"... (English version of the same line, or empty)\", " +
            "\"dialogue_speaker\": \"... (name of the character who says the dialogue line — hero or antagonist name, or empty if no dialogue)\", " +
            "\"audio_en\": \"sound effects + ambience for this clip\", " +
            "\"narration_id\": \"... (warm documentary voiceover in Indonesian, 1-2 sentences, Nalar tone — REQUIRED for every scene, never empty)\", " +
            "\"effect_id\": \"\" (leave empty — the app assigns effects from its internal vocabulary), " +
            "\"want_en\": \"...\", \"obstacle_en\": \"...\", \"transition_en\": \"...\"}]\n" +
            "Rules: fight moves must be DIFFERENT in every scene of the film (never repeat a " +
            "punch/kick/grapple verbatim); the closing scene is aftermath, no big fight; " +
            "dialogue only in scenes where the beat needs it (most scenes: empty dialogue); " +
            "include short dialogue in at least 1 scene of this batch, unless the beat is purely visual; " +
            "action_en = ONE action only — never paste the world description, setting, or a list " +
            "of places into action_en (the app pastes the world itself); " +
            "action_en MUST name its actor explicitly at the start (e.g. 'The Golden Demon spreads " +
            "its wings...' or 'Broly hurls a plasma wave...'), never a bare verb phrase ('spreads " +
            "its wings...') — the app keeps the actor you write, so the actor you name is the " +
            "actor the video shows; when both characters act in one clip, name both; " +
            "only the hero and the antagonist appear in actions — never invent a third character, " +
            "extra person, or 'lone survivor'; " +
            "SECONDARY MOTION inside action_en: add one natural secondary detail — breathing, " +
            "hair/fur rippling in the wind, fabric responding to movement, weight shifting — " +
            "supporting the primary action, never stealing focus from it; " +
            "FACIAL PERFORMANCE: when a scene has dialogue, the speaker's mouth must be visibly " +
            "open (shouting or speaking clearly — video models animate speech only when the mouth " +
            "is clearly open); give the speaker one specific expression (rage, defiance, pain, " +
            "triumph) and write it into the action; " +
            "narration_id is a voiceover line for the narrator app, NOT dialogue — never put " +
            "dialogue words into narration_id."
    }
}
