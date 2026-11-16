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
        val used = mutableSetOf<String>()
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
            append(plan.actionEn)
            val frag = effect?.let { effectFragment(it, bible) }.orEmpty()
            if (frag.isNotBlank()) append(" ").append(frag)
        }
        return ShotInput(
            title = bible.title,
            genre = bible.genre,
            style = StoryInfer.styleForGenre(bible.genre),
            character = hero?.let { CharacterDna(it.name, it.anchorsEn.joinToString(", ")) },
            secondCharacter = if (bible.antagonistName.isBlank()) null
            else CharacterDna(bible.antagonistName, bible.antagonistDescEn),
            world = WorldLock(bible.worldName, bible.worldDescEn),
            shotSize = shot.shotSize,
            angle = shot.angle,
            lensMm = shot.lensMm,
            cameraMove = shot.cameraMove,
            light = lightKindFrom(bible.lightEn),
            actionEn = action,
            audioEn = plan.audioEn,
            dialogue = DialogueLine(
                speaker = hero?.name.orEmpty(),
                textId = plan.dialogueId,
                textEn = plan.dialogueEn,
                language = if (plan.dialogueId.isNotBlank()) DialogueLang.ID else DialogueLang.EN
            ),
            characterCard = card,
            ambienceEn = bible.ambienceEn
        )
    }

    /** Mode B: teks penunjuk gambar per engine (template lokal). */
    fun renderReference(bible: StoryBible, plan: V2ScenePlan, shot: EffectiveShot, engine: TargetEngine): String {
        val hero = bible.hero
        val heroName = hero?.name ?: "the hero"
        val antagonistPart = if (bible.antagonistName.isBlank()) "" else ", ${bible.antagonistName}"
        val pointer = "$heroName${if (antagonistPart.isBlank()) "" else " and$antagonistPart"} in the reference images"
        val shotPhrase = shot.shotSize.phraseEn
        val cameraPhrase = shot.cameraMove.phraseEn
        val anglePhrase = shot.angle.phraseEn
        val dialogue = when {
            plan.dialogueId.isNotBlank() -> " Dialogue: \"${plan.dialogueId}\"."
            plan.dialogueEn.isNotBlank() -> " Dialogue: \"${plan.dialogueEn}\"."
            else -> ""
        }
        val audio = "SFX: ${plan.audioEn.ifBlank { "quiet ambience" }}. Ambient noise: ${bible.ambienceEn}."
        return when (engine) {
            TargetEngine.FLOW -> "Using the provided images for $heroName$antagonistPart, and ${bible.worldName}, " +
                "create a $shotPhrase of $pointer ${plan.actionEn}. " +
                "In ${bible.worldDescEn}, $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase, lit with ${bible.lightEn}.$dialogue $audio"
            TargetEngine.KLING -> "$heroName ${plan.actionEn}. $shotPhrase, $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase. " +
                "Using the provided images for $heroName$antagonistPart and ${bible.worldName}. " +
                "Setting: ${bible.worldDescEn}.$dialogue $audio"
            TargetEngine.SEEDANCE -> "Using the provided images for $heroName$antagonistPart, and ${bible.worldName}, " +
                "create a $shotPhrase of $pointer ${plan.actionEn}. $cameraPhrase, ${shot.lensMm}mm lens. " +
                "Reference: Image 1 = $heroName, Image 2 = ${bible.worldName}. " +
                "Setting: ${bible.worldDescEn}.$dialogue $audio"
            TargetEngine.WAN -> "Using the reference image of $heroName, ${plan.actionEn}. " +
                "$cameraPhrase, ${shot.lensMm}mm lens, $shotPhrase.$dialogue ${plan.audioEn}."
        }
    }

    fun renderScene(
        bible: StoryBible,
        plan: V2ScenePlan,
        mode: V2Mode
    ): V2ScenePrompt {
        val shot = effectiveShot(plan)
        return if (mode == V2Mode.TEXT_ONLY) {
            val input = shotInputFor(bible, plan, shot)
            val byEngine = PromptEngine.buildAll(input).associateBy { it.engine }
            V2ScenePrompt(
                sceneNumber = plan.sceneNumber,
                plan = plan,
                flowText = byEngine[TargetEngine.FLOW]?.text.orEmpty(),
                klingText = byEngine[TargetEngine.KLING]?.text.orEmpty(),
                seedanceText = byEngine[TargetEngine.SEEDANCE]?.text.orEmpty(),
                wanText = byEngine[TargetEngine.WAN]?.text.orEmpty()
            )
        } else {
            V2ScenePrompt(
                sceneNumber = plan.sceneNumber,
                plan = plan,
                flowText = renderReference(bible, plan, shot, TargetEngine.FLOW),
                klingText = renderReference(bible, plan, shot, TargetEngine.KLING),
                seedanceText = renderReference(bible, plan, shot, TargetEngine.SEEDANCE),
                wanText = renderReference(bible, plan, shot, TargetEngine.WAN)
            )
        }
    }

    fun renderAll(
        bible: StoryBible,
        plans: List<V2ScenePlan>,
        mode: V2Mode,
        isFight: Boolean
    ): List<V2ScenePrompt> {
        val totalBeats = bible.beats.size.coerceAtLeast(1)
        val assigned = assignEffects(plans, isFight, totalBeats)
        return assigned.map { renderScene(bible, it, mode) }
    }
}
