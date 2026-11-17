package com.daftech.promptku.engine

// ModeRender v2.0 — render prompt scene dari bible + rencana scene.
// DUA renderer, dipilih otomatis dari ada/tidaknya gambar referensi:
//   Mode A (teks-saja): prompt bawa semuanya — kartu 3 jangkar verbatim,
//     dunia/cahaya/ambience verbatim, via PromptEngine.buildAll.
//   Mode B (referensi): teks HANYA menunjuk gambar ("Using the provided
//     images for ..."), JANGAN deskripsikan ulang subjek — sesuai
//     panduan resmi Flow Help ("complement, not contradict").
// Efek (EffectLibrary) dipakai di sini sebagai kosakata internal
// prompter: fragmen + override kamera/lensa ditempel saat render,
// TIDAK dipilih user di UI.

enum class V2Mode { TEXT_ONLY, REFERENCE }

object ModeRender {

    // ---------- v2.1 fix6 — pembersih teks render ----------

    /** Buang titik di ujung kalimat sebelum dirangkai (pembunuh ".."). */
    fun cleanField(text: String): String = text.trim().trimEnd('.').trim()

    /** "In A sprawling..." -> "In a sprawling...": artikel "A" di awal deskripsi
     *  dunia jadi huruf kecil. Hanya "A " di awal; kata benda nama diri tidak disentuh. */
    fun cleanWorldStart(desc: String): String =
        if (desc.startsWith("A ")) "a" + desc.substring(1) else desc

    private fun tokensOf(text: String): Set<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()

    /** "a extreme close-up" -> "an extreme close-up": artikel a/an
     *  mengikuti huruf awal frasa (vokal = "an"). Dipakai template Mode B
     *  ("create a/an ...") — audit Broly menemukan "a extreme" 2x. */
    fun aOrAn(phrase: String): String {
        val first = phrase.trim().firstOrNull()?.lowercaseChar()
        return if (first != null && first in "aeiou") "an" else "a"
    }

    /** Bersihkan actionEn dari 2 cacat lapangan:
     *  (a) daftar deskripsi dunia yang bocor verbatim ke aksi;
     *  (b) konjungsi "and" menggantung di awal aksi ("and the Golden Demon
     *      slam together" — sisa sambungan LLM).
     *  v2.1 fix4 — PENCABUTAN sadar: strip nama hero/antagonis di awal aksi
     *  (fix6) DICABUT. Bukti lapangan (export Broly 18 scene): strip itu
     *  yang bikin 13/18 aksi tanpa pelaku ("spreads its wings..." — siapa?).
     *  Bug "Frieza, Frieza" yang asli ada di DAFTAR referensi (sudah
     *  diperbaiki terpisah di fix6), bukan di aksi; aksi yang menyebut
     *  pelakunya ("The Golden Demon spreads its wings...") adalah kalimat
     *  yang benar. Prompter fix4 kini WAJIB menulis aktor di awal aksi.
     *  Pengecualian: template KLING/WAN yang menempel nama hero sendiri
     *  tetap menangani nama ganda di renderReference (bukan di sini). */
    fun sanitizeAction(action: String, bible: StoryBible): String {
        var out = action.trim()
        val world = bible.worldDescEn.trim()
        if (world.length >= 20) {
            val idx = out.lowercase().indexOf(world.lowercase())
            if (idx >= 0) {
                out = (out.substring(0, idx) + out.substring(idx + world.length)).trim()
                    .trimStart(',', ' ', ';').trim()
            } else {
                // Varian: hanya 40 karakter pertama dunia yang bocor.
                val head = world.take(40)
                val idx2 = out.lowercase().indexOf(head.lowercase())
                if (idx2 >= 0) {
                    out = (out.substring(0, idx2) + out.substring(idx2 + head.length)).trim()
                        .trimStart(',', ' ', ';').trim()
                }
            }
        }
        // "and ..." di awal aksi = sisa konjungsi LLM; buang, aktornya tetap.
        if (out.startsWith("and ", ignoreCase = true)) {
            out = out.substring(4).trim()
        }
        return out
    }

    /** Buang nama HERO di AWAL aksi — HANYA untuk template WAN yang sudah
     *  menempel nama itu sendiri ("Using the reference image of X, ...").
     *  Nama antagonis TIDAK dibuang (tidak ganda dengan "of X"). Posesif
     *  ("Broly's fist...") TIDAK dibuang — "'s" butuh pemiliknya; dibiarkan
     *  utuh ("of Broly, Broly's fist..." tetap gramatikal).
     *  FLOW/SEEDANCE/KLING tidak memakai ini (aktor dipertahankan, fix4). */
    private fun stripLeadingName(action: String, bible: StoryBible): String {
        val heroName = bible.hero?.name?.trim().orEmpty()
        if (heroName.isBlank()) return action.trim()
        val out = action.trim()
        val stripped = when {
            out.startsWith("The $heroName", ignoreCase = true) -> out.substring(4 + heroName.length)
            out.startsWith(heroName, ignoreCase = true) -> out.substring(heroName.length)
            else -> return out
        }
        // Hanya buang kalau nama diikuti batas kata (spasi/koma/dll) —
        // posesif "'s" bukan batas kata: "Broly's" harus tetap utuh.
        return if (stripped.isEmpty() || stripped.first() in " ,;:") {
            stripped.trimStart(',', ' ', ';', ':').trim()
        } else {
            out
        }
    }

    /** Apakah aksi sudah diawali nama hero/antagonis ("Broly hurls..." —
     *  template KLING tidak perlu menempel nama lagi). */
    private fun startsWithName(action: String, bible: StoryBible): Boolean {
        val out = action.trim()
        return listOfNotNull(bible.hero?.name, bible.antagonistName.ifBlank { null })
            .any { n ->
                n.isNotBlank() &&
                    (out.startsWith(n, ignoreCase = true) ||
                        out.startsWith("The $n", ignoreCase = true))
            }
    }

    /** SFX vs ambience: kalau isinya nyaris sama (Jaccard >= 0.8), jangan cetak
     *  paragraf yang sama dua kali. */
    fun audioClause(audioEn: String, ambienceEn: String): String {
        val audio = cleanField(audioEn).ifBlank { "quiet ambience" }
        val amb = cleanField(ambienceEn)
        if (amb.isBlank()) return "SFX: $audio."
        val a = tokensOf(audio)
        val b = tokensOf(amb)
        val union = a union b
        val jaccard = if (union.isEmpty()) 0.0 else (a intersect b).size.toDouble() / union.size
        return if (jaccard >= 0.8) "SFX: $audio."
        else "SFX: $audio. Ambient noise: $amb."
    }

    /** Klausa rasio aspek, ditempel di ujung tiap teks prompt. */
    fun aspectClause(aspectRatio: String): String =
        if (aspectRatio == "16:9") " Widescreen 16:9 framing." else " Vertical 9:16 framing."

    /** Kamera/lensa/sudut efek menimpa rencana Prompter saat render. */
    data class EffectiveShot(
        val cameraMove: CameraMove,
        val lensMm: Int,
        val angle: CameraAngle,
        val shotSize: ShotSize
    )

    fun cameraMoveFromString(raw: String): CameraMove {
        val t = raw.lowercase().trim()
        if (t.isBlank()) return CameraMove.STATIC
        CameraMove.values().firstOrNull { it.name.lowercase() == t.replace(" ", "_").replace("-", "_") }
            ?.let { return it }
        return CameraMove.values().firstOrNull { it.phraseEn.lowercase().startsWith(t.split(" ").first()) }
            ?: when {
                t.contains("dolly") && t.contains("out") -> CameraMove.DOLLY_OUT
                t.contains("dolly") -> CameraMove.DOLLY_IN
                t.contains("pan left") -> CameraMove.PAN_LEFT
                t.contains("pan right") -> CameraMove.PAN_RIGHT
                t.contains("tracking") -> CameraMove.TRACKING
                t.contains("crane up") -> CameraMove.CRANE_UP
                t.contains("crane down") -> CameraMove.CRANE_DOWN
                t.contains("orbit") -> CameraMove.ORBIT
                t.contains("aerial") || t.contains("drone") -> CameraMove.AERIAL
                t.contains("rack") -> CameraMove.RACK_FOCUS
                t.contains("handheld") -> CameraMove.HANDHELD
                t.contains("tilt up") -> CameraMove.TILT_UP
                t.contains("zoom") -> CameraMove.ZOOM_IN
                else -> CameraMove.STATIC
            }
    }

    fun angleFromString(raw: String): CameraAngle = when (raw.lowercase().trim()) {
        "low" -> CameraAngle.LOW
        "high" -> CameraAngle.HIGH
        "bird", "bird's-eye", "birds-eye" -> CameraAngle.BIRD
        "dutch" -> CameraAngle.DUTCH
        else -> CameraAngle.EYE
    }

    fun shotSizeFromString(raw: String): ShotSize = when (raw.lowercase().trim()) {
        "wide" -> ShotSize.WIDE
        "close-up", "closeup" -> ShotSize.CLOSEUP
        "extreme close-up", "extreme closeup" -> ShotSize.EXTREME_CLOSEUP
        "extreme wide" -> ShotSize.EXTREME_WIDE
        "medium close-up" -> ShotSize.MEDIUM_CLOSEUP
        "two-shot", "two shot" -> ShotSize.TWO_SHOT
        "over-the-shoulder", "over shoulder" -> ShotSize.OVER_SHOULDER
        else -> ShotSize.MEDIUM
    }

    fun lightKindFrom(text: String): LightKind {
        val t = text.lowercase()
        return when {
            t.contains("moon") -> LightKind.MOONLIGHT
            t.contains("neon") -> LightKind.NEON
            t.contains("golden") -> LightKind.GOLDEN_HOUR
            t.contains("candle") -> LightKind.CANDLE
            t.contains("studio") -> LightKind.STUDIO_SOFT
            t.contains("volumetric") || t.contains("god ray") -> LightKind.VOLUMETRIC
            t.contains("rim") -> LightKind.RIM
            t.contains("chiaroscuro") -> LightKind.CHIAROSCURO
            t.contains("rembrandt") -> LightKind.REMBRANDT
            else -> LightKind.OVERCAST
        }
    }

    /** Penugasan efek level FILM: jurus tidak pernah berulang se-film,
     *  finisher 1x di scene pertama klimaks, Penutup = aftermath. */
    fun assignEffects(
        plans: List<V2ScenePlan>,
        isFight: Boolean,
        totalBeats: Int
    ): List<V2ScenePlan> {
        if (plans.isEmpty()) return plans
        val totalScenes = plans.size
        val scenesPerBeat = (totalScenes + totalBeats - 1) / totalBeats
        val climaxBeat = if (totalBeats <= 5) 3 else 5
        val climaxFirstScene = climaxBeat * scenesPerBeat + 1
        val lastScene = plans.maxOf { it.sceneNumber }
        val fightMoves = EffectLibrary.FIGHT_MOVES_ALL.filter { it.id != "finisher" }
        val cameraMoves = EffectLibrary.ALL.filter { it.category == EffectCategory.CAMERA }
        // v2.1 — benih `used` dari efek yang SUDAH terpasang (regenerasi 1 scene:
        // tanpa ini, jurus baru bisa duplikat jurus scene lain yang sudah ada).
        val used = plans.mapNotNull { it.effectId.ifBlank { null } }.toMutableSet()
        var fightIdx = 0
        var camIdx = 0
        return plans.map { plan ->
            var effectId = plan.effectId
            if (effectId.isBlank()) {
                when {
                    plan.sceneNumber == lastScene -> effectId = ""
                    isFight && plan.sceneNumber == climaxFirstScene -> {
                        effectId = "finisher"
                        used.add("finisher")
                    }
                    isFight -> {
                        // Jurus berikutnya yang BELUM dipakai se-film.
                        var picked = ""
                        var tries = 0
                        while (tries < fightMoves.size) {
                            val cand = fightMoves[(fightIdx + tries) % fightMoves.size]
                            if (cand.id !in used) {
                                picked = cand.id
                                break
                            }
                            tries++
                        }
                        if (picked.isEmpty() && fightMoves.isNotEmpty()) {
                            picked = fightMoves[fightIdx % fightMoves.size].id
                        }
                        fightIdx++
                        effectId = picked
                        if (picked.isNotBlank()) used.add(picked)
                    }
                    plan.sceneNumber % 4 == 0 && cameraMoves.isNotEmpty() -> {
                        val cam = cameraMoves[camIdx % cameraMoves.size]
                        camIdx++
                        effectId = cam.id
                    }
                }
            }
            plan.copy(effectId = effectId)
        }
    }

    /** Fragmen efek dengan nama antagonis (bukan "the giant" hardcode). */
    fun effectFragment(effect: Effect, bible: StoryBible): String {
        if (effect.fragmentEn.isBlank()) return ""
        val target = bible.antagonistName.ifBlank { bible.hero?.name ?: "the opponent" }
        return effect.fragmentEn
            .replace("the giant", target.lowercase())
            .replace("The giant", target)
    }

    fun effectiveShot(plan: V2ScenePlan): EffectiveShot {
        val effect = EffectLibrary.ALL.firstOrNull { it.id == plan.effectId }
        return EffectiveShot(
            cameraMove = effect?.cameraMove ?: cameraMoveFromString(plan.cameraMove),
            lensMm = effect?.lensMm ?: plan.lensMm.coerceIn(14, 200),
            angle = effect?.angle ?: angleFromString(plan.angle),
            shotSize = shotSizeFromString(plan.shotSize)
        )
    }

    private fun shotInputFor(bible: StoryBible, plan: V2ScenePlan, shot: EffectiveShot): ShotInput {
        val hero = bible.hero
        val card = if (hero == null) null else CharacterCard(
            trait = hero.anchorsEn.getOrNull(0).orEmpty(),
            gesture = "",
            objectProp = hero.anchorsEn.getOrNull(1).orEmpty(),
            faceAnchor = hero.anchorsEn.getOrNull(2).orEmpty()
        )
        val effect = EffectLibrary.ALL.firstOrNull { it.id == plan.effectId }
        val action = buildString {
            append(cleanField(plan.actionEn))
            val frag = effect?.let { effectFragment(it, bible) }.orEmpty()
            if (frag.isNotBlank()) append(" ").append(frag)
        }
        // v2.1 fix6 — antagonis == hero (film satu penjahat): jangan pasang
        // tokoh kedua yang isinya nama yang sama.
        val secondCharacter = if (bible.antagonistName.isBlank() ||
            bible.antagonistName.equals(hero?.name, ignoreCase = true)
        ) null else CharacterDna(bible.antagonistName, bible.antagonistDescEn)
        return ShotInput(
            title = bible.title,
            genre = bible.genre,
            style = StoryInfer.styleForGenre(bible.genre),
            character = hero?.let { CharacterDna(it.name, it.anchorsEn.joinToString(", ")) },
            secondCharacter = secondCharacter,
            world = WorldLock(bible.worldName, cleanField(cleanWorldStart(bible.worldDescEn))),
            shotSize = shot.shotSize,
            angle = shot.angle,
            lensMm = shot.lensMm,
            cameraMove = shot.cameraMove,
            light = lightKindFrom(bible.lightEn),
            actionEn = action,
            // Draft Offline v2 — pose dari rencana (rantai v1); kosong =
            // default render ("standing still" / "standing firm").
            poseStart = plan.poseStart,
            poseEnd = plan.poseEnd,
            audioEn = cleanField(plan.audioEn),
            dialogue = DialogueLine(
                // v2.1 fix4 — pembicara dari rencana LLM (dialogue_speaker);
                // kosong = hero seperti sebelumnya.
                speaker = plan.dialogueSpeaker.ifBlank { hero?.name.orEmpty() },
                textId = plan.dialogueId,
                textEn = plan.dialogueEn,
                // v2.1 — bahasa dialog pilihan user menang; jatuh ke versi yang ada.
                language = when {
                    bible.dialogueLang == "EN" && plan.dialogueEn.isNotBlank() -> DialogueLang.EN
                    plan.dialogueId.isNotBlank() -> DialogueLang.ID
                    else -> DialogueLang.EN
                }
            ),
            characterCard = card,
            ambienceEn = cleanField(bible.ambienceEn)
        )
    }

    /** Mode B: teks penunjuk gambar per engine (template lokal).
     *  v2.1 fix6 — daftar referensi dibangun sebagai SATU list ("for A, B, C"),
     *  bukan tempelan ", X" + " and, X" (itu sumber "Frieza, Frieza" 54x dan
     *  "and, Frieza" 36x); antagonis == hero di-drop; klaim "Image 2" hanya
     *  kalau gambar ke-2 memang ada ([imageCount] >= 2). */
    fun renderReference(bible: StoryBible, plan: V2ScenePlan, shot: EffectiveShot, engine: TargetEngine, imageCount: Int = 1): String {
        val hero = bible.hero
        val heroName = hero?.name ?: "the hero"
        val sameAntagonist = bible.antagonistName.isBlank() ||
            bible.antagonistName.equals(heroName, ignoreCase = true)
        val antagonistAnd = if (sameAntagonist) "" else " and ${bible.antagonistName}"
        // v2.1 fix4 — Reference Assignment (ChatGPT 15 layer): tiap gambar
        // diberi tugas eksplisit di daftar referensi, supaya model tidak
        // menukar peran gambar ("Broly (hero), Golden Demon (antagonist),
        // Antarctic Abyss (environment)").
        val refNames = listOf("$heroName (hero)") +
            (if (sameAntagonist) emptyList() else listOf("${bible.antagonistName} (antagonist)")) +
            listOf("${bible.worldName} (environment)")
        val refList = refNames.joinToString(", ")
        val pointer = "$heroName$antagonistAnd in the reference images"
        val shotPhrase = shot.shotSize.phraseEn
        val cameraPhrase = shot.cameraMove.phraseEn
        val anglePhrase = shot.angle.phraseEn
        val action = cleanField(sanitizeAction(plan.actionEn, bible))
        val world = cleanField(cleanWorldStart(bible.worldDescEn))
        val light = cleanField(bible.lightEn)
        // v2.1 fix4 — dialog ditulis beserta pembicaranya ("Dialogue (Broly):
        // ...") bila prompter mengisinya; kosong = format lama tanpa nama.
        val speaker = plan.dialogueSpeaker.trim()
        val dialogueText = when {
            bible.dialogueLang == "EN" && plan.dialogueEn.isNotBlank() -> plan.dialogueEn
            plan.dialogueId.isNotBlank() -> plan.dialogueId
            plan.dialogueEn.isNotBlank() -> plan.dialogueEn
            else -> ""
        }
        val dialogue = if (dialogueText.isBlank()) "" else
            if (speaker.isNotBlank()) " Dialogue ($speaker): \"$dialogueText\"."
            else " Dialogue: \"$dialogueText\"."
        val audio = audioClause(plan.audioEn, bible.ambienceEn)
        val aspect = aspectClause(bible.aspectRatio)
        val referenceLine = if (imageCount >= 2) {
            "Reference: Image 1 = $heroName, Image 2 = ${bible.worldName}. "
        } else {
            "Reference: Image 1 = $heroName. "
        }
        // v2.1 fix4 — aksi kini WAJIB menyebut aktornya (prompter): pointer
        // dipisah koma ("...images, the Golden Demon spreads...") supaya
        // klausa aksi terbaca sebagai kalimat sendiri. KLING menempel nama
        // hero sendiri di depan aksi — kalau aksi sudah diawali nama, jangan
        // ditempel lagi ("Broly Broly hurls"). WAN juga menempel nama hero
        // ("Using the reference image of X, ...") — nama hero di awal aksi
        // dibuang di sana saja (stripLeadingName).
        // v2.1 fix5 — "In $world," + "lit with $light" berasumsi dunia/cahaya
        // adalah FRASA ("In ice floes..."). Sejak fix4 keduanya KALIMAT
        // ("Lightning splits the bruised sky...") — "In Lightning splits..."
        // 18x tidak gramatikal (audit Storm Titans). KLING/SEEDANCE sudah
        // selamat ("Setting: $world.") — FLOW ikut pola itu; cahaya jadi
        // kalimat sendiri ("Lighting: ..."). Berlaku juga untuk dunia frasa
        // lama ("Setting: ice floes..." tetap terbaca sebagai deskripsi).
        val klingLead = if (startsWithName(action, bible)) action else "$heroName $action"
        val wanAction = stripLeadingName(action, bible)
        return when (engine) {
            TargetEngine.FLOW -> "Using the provided images for $refList, " +
                "create ${aOrAn(shotPhrase)} $shotPhrase of $pointer, $action. " +
                "Setting: $world. $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase. Lighting: $light.$dialogue $audio$aspect"
            TargetEngine.KLING -> "$klingLead. $shotPhrase, $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase. " +
                "Using the provided images for $refList. " +
                "Setting: $world.$dialogue $audio$aspect"
            TargetEngine.SEEDANCE -> "Using the provided images for $refList, " +
                "create ${aOrAn(shotPhrase)} $shotPhrase of $pointer, $action. $cameraPhrase, ${shot.lensMm}mm lens. " +
                referenceLine +
                "Setting: $world.$dialogue $audio$aspect"
            TargetEngine.WAN -> "Using the reference image of $heroName, $wanAction. " +
                "$cameraPhrase, ${shot.lensMm}mm lens, $shotPhrase.$dialogue ${cleanField(plan.audioEn).ifBlank { "quiet ambience" }}.$aspect"
        }
    }

    fun renderScene(
        bible: StoryBible,
        plan: V2ScenePlan,
        mode: V2Mode,
        imageCount: Int = 1
    ): V2ScenePrompt {
        // v2.1 fix6 — bersihkan aksi SEBELUM render (berlaku dua mode +
        // rencana lama dari cache ikut bersih saat dibuka ulang).
        val cleanPlan = plan.copy(actionEn = cleanField(sanitizeAction(plan.actionEn, bible)))
        val shot = effectiveShot(cleanPlan)
        val aspect = aspectClause(bible.aspectRatio)
        return if (mode == V2Mode.TEXT_ONLY) {
            val input = shotInputFor(bible, cleanPlan, shot)
            val byEngine = PromptEngine.buildAll(input).associateBy { it.engine }
            V2ScenePrompt(
                sceneNumber = cleanPlan.sceneNumber,
                plan = cleanPlan,
                flowText = byEngine[TargetEngine.FLOW]?.text.orEmpty() + aspect,
                klingText = byEngine[TargetEngine.KLING]?.text.orEmpty() + aspect,
                seedanceText = byEngine[TargetEngine.SEEDANCE]?.text.orEmpty() + aspect,
                wanText = byEngine[TargetEngine.WAN]?.text.orEmpty() + aspect
            )
        } else {
            V2ScenePrompt(
                sceneNumber = cleanPlan.sceneNumber,
                plan = cleanPlan,
                flowText = renderReference(bible, cleanPlan, shot, TargetEngine.FLOW, imageCount),
                klingText = renderReference(bible, cleanPlan, shot, TargetEngine.KLING, imageCount),
                seedanceText = renderReference(bible, cleanPlan, shot, TargetEngine.SEEDANCE, imageCount),
                wanText = renderReference(bible, cleanPlan, shot, TargetEngine.WAN, imageCount)
            )
        }
    }

    fun renderAll(
        bible: StoryBible,
        plans: List<V2ScenePlan>,
        mode: V2Mode,
        isFight: Boolean,
        imageCount: Int = 1
    ): List<V2ScenePrompt> {
        val totalBeats = bible.beats.size.coerceAtLeast(1)
        val assigned = assignEffects(plans, isFight, totalBeats)
        return assigned.map { renderScene(bible, it, mode, imageCount) }
    }
}
