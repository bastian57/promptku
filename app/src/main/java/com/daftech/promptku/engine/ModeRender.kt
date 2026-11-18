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
                // v2.1 fix8 v2 — gerakan cepat (nama + frasa dicek nama dulu
                // di atas; ini fallback untuk teks bebas LLM seperti
                // "whip pan left to right" / "handheld shake").
                t.contains("whip") -> CameraMove.WHIP_PAN
                t.contains("push-in") || t.contains("push in") -> CameraMove.PUSH_IN
                t.contains("projectile") -> CameraMove.PROJECTILE_POV
                t.contains("shake") -> CameraMove.HANDHELD_SHAKE
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
     *  finisher 1x di scene pertama klimaks, Penutup = aftermath.
     *  v2.1 fix8 v2 — [fightRoles]: peran per scene dari BeatMap (sama
     *  dengan yang diterima prompter + gerbang R9). Peran MENANG atas
     *  siklus generik: scene IMPACT/HERO_HURT/TENSION mendapat efek
     *  perannya, bukan jurus acak — kalau tidak, momen impact bisa jatuh
     *  di scene mana pun dan slow-mo massal kembali. Kosong = perilaku
     *  lama (penelepon lama / film non-fight tetap jalan). */
    fun assignEffects(
        plans: List<V2ScenePlan>,
        isFight: Boolean,
        totalBeats: Int,
        fightRoles: Map<Int, FightRole> = emptyMap(),
        // v2.1 fix9 — scene terakhir FILM (bukan maxOf daftar yang dilempar):
        // retry/regenerasi render subset dulu menghitung lastScene dari subset
        // (scene retry jadi "last scene" sendiri -> efeknya dikosongin, audit
        // Aang: S3 kehilangan fight_impact, lensa 50 bukan 85). 0 = perilaku
        // lama (maxOf) untuk pemanggil lama/tes lama.
        filmTotalScenes: Int = 0,
        // v2.1 fix7 — scene terakhir KLIMAKS (dari BeatMap, dihitung
        // renderAll): siklus jurus berhenti di sini. 0 = perilaku lama
        // (siklus jalan sampai akhir film) untuk pemanggil lama/tes lama.
        climaxLastScene: Int = 0
    ): List<V2ScenePlan> {
        if (plans.isEmpty()) return plans
        val totalScenes = plans.size
        val scenesPerBeat = (totalScenes + totalBeats - 1) / totalBeats
        val climaxBeat = if (totalBeats <= 5) 3 else 5
        val climaxFirstScene = climaxBeat * scenesPerBeat + 1
        val lastScene = if (filmTotalScenes > 0) filmTotalScenes else plans.maxOf { it.sceneNumber }
        val fightMoves = EffectLibrary.FIGHT_MOVES_ALL.filter { it.id != "finisher" }
        val cameraMoves = EffectLibrary.ALL.filter { it.category == EffectCategory.CAMERA }
        // v2.1 — benih `used` dari efek yang SUDAH terpasang (regenerasi 1 scene:
        // tanpa ini, jurus baru bisa duplikat jurus scene lain yang sudah ada).
        val used = plans.mapNotNull { it.effectId.ifBlank { null } }.toMutableSet()
        var fightIdx = 0
        var camIdx = 0
        var tensionIdx = 0
        return plans.map { plan ->
            var effectId = plan.effectId
            if (effectId.isBlank()) {
                when {
                    // v2.1 fix9 — cabang PERAN DULU, baru lastScene (audit Aang):
                    // film pendek yang berakhir di Klimaks (5 scene, S5 =
                    // finisher + last scene) kehilangan efek finisher karena
                    // lastScene dicek duluan ("slow dolly in" rencana, bukan
                    // CRANE_UP). Scene ber-peran tidak pernah kena lastScene;
                    // lastScene hanya mengosongkan scene TANPA peran (Penutup).
                    fightRoles[plan.sceneNumber] == FightRole.IMPACT -> {
                        effectId = "fight_impact"
                        used.add(effectId)
                    }
                    fightRoles[plan.sceneNumber] == FightRole.HERO_HURT -> {
                        effectId = "fight_hero_hurt"
                        used.add(effectId)
                    }
                    fightRoles[plan.sceneNumber] == FightRole.TENSION -> {
                        val pool = EffectLibrary.TENSION_MOVES
                        if (pool.isNotEmpty()) {
                            effectId = pool[tensionIdx % pool.size].id
                            tensionIdx++
                            used.add(effectId)
                        }
                    }
                    fightRoles[plan.sceneNumber] == FightRole.FINISHER -> {
                        effectId = "finisher"
                        used.add("finisher")
                    }
                    // v2.1 fix9 — cabang lastScene HANYA untuk scene TANPA
                    // peran (Penutup/aftermath): scene ber-peran (termasuk
                    // EXCHANGE — perannya siklus, bukan cabang di atas)
                    // tidak pernah dikosongkan. Film pendek yang berakhir
                    // di Klimaks: scene terakhirnya EXCHANGE (S6 di peta
                    // [1,1,2,2]) — kalau dikosongkan, klimaks kehilangan
                    // jurus di scene penutupnya.
                    plan.sceneNumber == lastScene &&
                        (fightRoles[plan.sceneNumber] == null ||
                            fightRoles[plan.sceneNumber] == FightRole.NONE) -> effectId = ""
                    isFight && plan.sceneNumber == climaxFirstScene -> {
                        effectId = "finisher"
                        used.add("finisher")
                    }
                    // v2.1 fix7 — siklus jurus BERHENTI di Klimaks (audit
                    // Olympus: S16 Penutup dapat "cross" dari siklus —
                    // fragmen fight di aftermath). Scene sesudah Klimaks
                    // jatuh ke cabang kamera / tanpa efek, bukan jurus.
                    isFight && (climaxLastScene <= 0 || plan.sceneNumber <= climaxLastScene) -> {
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
        // v2.1 fix9 — nama diri WAJIB kapital (drive-by fix7 item 4: audit
        // Olympus menemukan "general kallistos" lowercase 5x karena
        // .lowercase() di sini; nama tokoh di tengah kalimat tetap kapital).
        return effect.fragmentEn
            .replace("the giant", target)
            .replace("The giant", target)
    }

    /** v2.1 fix7 — gabung aksi inti + fragmen efek dengan ", then "
     *  (bukan spasi telanjang: "expression the cross snaps" tidak
     *  gramatikal — audit Aang fix9 S1/S2). Plus dedupe kata ganda di
     *  sambungan: kalau kata terakhir inti == kata pertama fragmen
     *  ("...as the" + "the blow lands"), kata inti yang dibuang — fragmen
     *  tetap utuh (prinsip fix9), dan ", then " menutupi bekas
     *  potongannya. Inti kosong = fragmen saja (scene hantu — gerbang
     *  G5 yang menangkapnya, bukan join ini). */
    fun joinActionWithFragment(core: String, frag: String): String {
        if (frag.isBlank()) return core
        val c = core.trim()
        if (c.isBlank()) return frag.trim()
        val coreWords = c.split(Regex("\\s+"))
        val fragFirst = frag.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            .trimEnd(',', '.', ';', ':').lowercase()
        val coreLast = coreWords.lastOrNull().orEmpty()
            .trimEnd(',', '.', ';', ':').lowercase()
        val deduped = if (coreLast.isNotEmpty() && coreLast == fragFirst) {
            coreWords.dropLast(1).joinToString(" ")
        } else c
        // v2.1 fix7 — koma ujung inti WAJIB dibuang sebelum ", then ":
        // trim batas frasa memotong di koma ("...scorching wind,") dan
        // koma itu menempel di kata ("wind,") — tanpa strip ini join jadi
        // koma ganda ("wind,, then"). Ditemukan tes fix7 (replay S3 Aang).
        return "${deduped.trimEnd(',').trim()}, then ${frag.trim()}"
    }

    fun effectiveShot(plan: V2ScenePlan, cameraOverride: CameraMove? = null): EffectiveShot {
        val effect = EffectLibrary.ALL.firstOrNull { it.id == plan.effectId }
        return EffectiveShot(
            // v2.1 fix8 v2 — cameraOverride (rantai kamera) menang atas
            // kamera efek maupun kamera rencana LLM.
            cameraMove = cameraOverride ?: effect?.cameraMove ?: cameraMoveFromString(plan.cameraMove),
            lensMm = effect?.lensMm ?: plan.lensMm.coerceIn(14, 200),
            angle = effect?.angle ?: angleFromString(plan.angle),
            shotSize = shotSizeFromString(plan.shotSize)
        )
    }

    private fun shotInputFor(
        bible: StoryBible,
        plan: V2ScenePlan,
        shot: EffectiveShot,
        cameraContinues: Boolean = false
    ): ShotInput {
        val hero = bible.hero
        val card = if (hero == null) null else CharacterCard(
            trait = hero.anchorsEn.getOrNull(0).orEmpty(),
            gesture = "",
            objectProp = hero.anchorsEn.getOrNull(1).orEmpty(),
            faceAnchor = hero.anchorsEn.getOrNull(2).orEmpty()
        )
        val effect = EffectLibrary.ALL.firstOrNull { it.id == plan.effectId }
        // v2.1 fix7 — join ", then " (bukan spasi telanjang) + dedupe
        // sambungan; lihat joinActionWithFragment.
        val frag = effect?.let { effectFragment(it, bible) }.orEmpty()
        val action = joinActionWithFragment(cleanField(plan.actionEn), frag)
        // v2.1 fix6 — antagonis == hero (film satu penjahat): jangan pasang
        // tokoh kedua yang isinya nama yang sama.
        val secondCharacter = if (bible.antagonistName.isBlank() ||
            bible.antagonistName.equals(hero?.name, ignoreCase = true)
        ) null else CharacterDna(bible.antagonistName, bible.antagonistDescEn)
        // v2.1 fix6 (riset) — kata/karakter klausa aspek ("Vertical 9:16
        // framing.") masuk anggaran PromptEngine (cap efektif = cap engine
        // dikurangi ini), supaya total export tidak lewat cap 3 kata.
        val aspect = aspectClause(bible.aspectRatio)
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
            cameraContinues = cameraContinues,
            light = lightKindFrom(bible.lightEn),
            // v2.1 fix7 — cahaya verbatim (item 6): Mode A pakai teks
            // bible, bukan frasa enum lightKindFrom.
            lightText = cleanField(bible.lightEn),
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
            ambienceEn = cleanField(bible.ambienceEn),
            aspectWords = MythFilter.countWords(aspect),
            aspectChars = aspect.length
        )
    }

    /** Batas back-off trim Mode B (v2.1 fix12): posisi TERAKHIR di [text]
     *  tempat trim boleh memotong — potong SEBELUM batas, bukan di
     *  tengah frasa. Selain ", " dan " and " (fix7), tambah batas klausa
     *  bawahan " while "/" as " dan marker beat "[00:" — audit Peak of
     *  Destiny S3: inti kepotong jadi "...while the tyrant's face"
     *  karena batas ", " terakhir 13 kata ke belakang, lewat guard 8
     *  kata. "while"/"as" cuma dihitung kalau diikuti artikel /
     *  pronomen / nama diri (awal klausa bawahan: "while the tyrant's
     *  face", "as the wind howls") — BUKAN komparasi ("as fast as
     *  lightning": potong di situ malah ninggalin "as fast" yang
     *  menggantung) atau frasa durasi ("wait a while longer").
     *  Kalau batas terakhir yang bagus ternyata komparasi/durasi,
     *  fungsi ini tidak mencari kemunculan sebelumnya — false negative
     *  (perilaku lama) lebih aman daripada false positive. */
    fun clauseBoundaryAt(text: String): Int {
        var at = maxOf(text.lastIndexOf(", "), text.lastIndexOf(" and "))
        for (word in listOf(" while ", " as ")) {
            val idx = text.lastIndexOf(word)
            if (idx > at) {
                val after = text.substring(idx + word.length).trimStart()
                    .substringBefore(" ").trimEnd(',', '.', ';', ':')
                val startsClause = after.lowercase() in setOf(
                    "the", "his", "her", "its", "a", "an", "he", "she",
                    "it", "they", "this", "that"
                ) || after.firstOrNull()?.isUpperCase() == true
                if (startsClause) at = idx
            }
        }
        val beatAt = text.lastIndexOf("[00:")
        if (beatAt > at) at = beatAt
        return at
    }

    /** Mode B: teks penunjuk gambar per engine (template lokal).
     *  v2.1 fix6 — daftar referensi dibangun sebagai SATU list ("for A, B, C"),
     *  bukan tempelan ", X" + " and, X" (itu sumber "Frieza, Frieza" 54x dan
     *  "and, Frieza" 36x); antagonis == hero di-drop; klaim "Image 2" hanya
     *  kalau gambar ke-2 memang ada ([imageCount] >= 2). */
    fun renderReference(
        bible: StoryBible,
        plan: V2ScenePlan,
        shot: EffectiveShot,
        engine: TargetEngine,
        imageCount: Int = 1,
        cameraContinues: Boolean = false
    ): String {
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
        // v2.1 fix8 v2 — rantai kamera: gerakan yang sama berlanjut dari
        // shot sebelumnya (bukan potongan baru).
        val cameraPhrase = if (cameraContinues) {
            "continuing ${shot.cameraMove.phraseEn} from the previous shot"
        } else {
            shot.cameraMove.phraseEn
        }
        val anglePhrase = shot.angle.phraseEn
        // v2.1 fix9 — Mode B ikut menempel fragmen efek (audit Aang: fragmen
        // peran IMPACT/HERO_HURT/FINISHER tidak pernah sampai ke Flow di Mode
        // B karena renderReference pakai plan.actionEn mentah; tempelan
        // fragmen cuma ada di shotInputFor Mode A). Fragmen DILINDUNGI dari
        // trim di bawah: yang dipotong cuma ekor aksi INTI (LLM), fragmen
        // utuh — kalau ekor fragmen yang dipotong, momen perannya (mis.
        // slow-mo finisher) justru hilang.
        val effect = EffectLibrary.ALL.firstOrNull { it.id == plan.effectId }
        val frag = effect?.let { effectFragment(it, bible) }.orEmpty()
        val coreAction = cleanField(sanitizeAction(plan.actionEn, bible))
        // v2.1 fix7 — join ", then " (bukan spasi telanjang) + dedupe
        // sambungan; lihat joinActionWithFragment.
        val action = joinActionWithFragment(coreAction, frag)
        val world = cleanField(cleanWorldStart(bible.worldDescEn))
        val light = cleanField(bible.lightEn)
        // v2.1 fix4 — dialog ditulis beserta pembicaranya bila prompter
        // mengisinya; kosong = hero (sama seperti shotInputFor Mode A).
        // v2.1 fix6 (riset) — format dialog resmi 2026 TANPA kutip:
        // `Nama says: teks` (bukan `Dialogue (Nama): "teks"`). Teks dialog
        // dibersihkan titik ujungnya ("...last.". -> tidak ada "..").
        val speaker = plan.dialogueSpeaker.trim()
        val dialogueText = when {
            bible.dialogueLang == "EN" && plan.dialogueEn.isNotBlank() -> plan.dialogueEn
            plan.dialogueId.isNotBlank() -> plan.dialogueId
            plan.dialogueEn.isNotBlank() -> plan.dialogueEn
            else -> ""
        }
        val speakerName = speaker.ifBlank { heroName }
        // v2.1 fix9 — jangan append "." kalau dialog sudah diakhiri "!" / "?"
        // (audit Aang: "world!." di 4/4 scene dialog). Tanda serunya
        // DIPERTAHANKAN (teriakan tetap teriakan) — bukan dibuang cleanField.
        val dialogue = if (dialogueText.isBlank()) "" else {
            val clean = cleanField(dialogueText)
            val end = if (clean.endsWith("!") || clean.endsWith("?")) "" else "."
            " $speakerName says: $clean$end"
        }
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
        // v2.1 fix6 (riset) — Mode B akhirnya punya trim (dulu TIDAK ADA —
        // S17 Kaito 1.453 karakter / 211 kata lolos ke export, lewat batas
        // Flow ~1.400 karakter). Yang dipotong HANYA ekor aksi (detail
        // sekunder); dunia (G1), dialog, audio, dan aspek TIDAK PERNAH
        // disentuh — teks yang terpotong tetap koheren karena aksi adalah
        // satu kalimat panjang, bukan daftar klausa wajib.
        fun buildText(a: String): String {
            val lead = if (startsWithName(a, bible)) a else "$heroName $a"
            val wanA = stripLeadingName(a, bible)
            return when (engine) {
                TargetEngine.FLOW -> "Using the provided images for $refList, " +
                    "create ${aOrAn(shotPhrase)} $shotPhrase of $pointer, $a. " +
                    "Setting: $world. $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase. Lighting: $light.$dialogue $audio$aspect"
                TargetEngine.KLING -> "$lead. $shotPhrase, $cameraPhrase, ${shot.lensMm}mm lens, $anglePhrase. " +
                    "Using the provided images for $refList. " +
                    "Setting: $world.$dialogue $audio$aspect"
                TargetEngine.SEEDANCE -> "Using the provided images for $refList, " +
                    "create ${aOrAn(shotPhrase)} $shotPhrase of $pointer, $a. $cameraPhrase, ${shot.lensMm}mm lens. " +
                    referenceLine +
                    "Setting: $world.$dialogue $audio$aspect"
                TargetEngine.WAN -> "Using the reference image of $heroName, $wanA. " +
                    "$cameraPhrase, ${shot.lensMm}mm lens, $shotPhrase.$dialogue ${cleanField(plan.audioEn).ifBlank { "quiet ambience" }}.$aspect"
            }
        }
        var text = buildText(action)
        val overWords = MythFilter.countWords(text) - engine.maxWords
        val overChars = if (engine == TargetEngine.FLOW) {
            text.length - PromptEngine.FLOW_CHAR_CAP
        } else {
            0
        }
        if (overWords > 0 || overChars > 0) {
            // v2.1 fix9 — trim HANYA ekor aksi inti (fragmen utuh, lihat di
            // atas): potong kata inti sebanyak kelebihan, fragmen ditempel lagi
            // utuh. Tanpa fragmen, coreWords == actionWords (perilaku lama).
            val coreWords = coreAction.split(Regex("\\s+"))
            val keepByWords = if (overWords > 0) coreWords.size - overWords else coreWords.size
            // ~6 karakter/kata rata-rata: potong 1 kata per 6 karakter lebih.
            val keepByChars = if (overChars > 0) coreWords.size - (overChars / 6 + 1) else coreWords.size
            val keep = maxOf(4, minOf(keepByWords, keepByChars))
            // v2.1 fix7 — potong di BATAS FRASA, bukan tengah frasa (audit
            // Aang fix9 S3: inti kepotong jadi "...as the", fragmen mulai
            // "the blow lands" -> "the the"). Mundur ke koma/"and"
            // terakhir di dalam teks yang dipertahankan, tapi jangan mundur
            // lebih dari 8 kata (kalau batasnya jauh, potongan kata lebih
            // berharga daripada kerapian). Dedupe sambungan ditangani
            // joinActionWithFragment di bawah.
            // v2.1 fix12 — batasnya ditambah klausa bawahan " while "/" as "
            // dan marker beat "[00:" (lihat clauseBoundaryAt): Peak of
            // Destiny S3 menggantung "...while the tyrant's face" karena
            // batas ", " terakhir 13 kata ke belakang.
            val keptText = coreWords.take(keep).joinToString(" ")
            val boundaryAt = clauseBoundaryAt(keptText)
            val boundaryKeep = if (boundaryAt > 0) MythFilter.countWords(keptText.substring(0, boundaryAt)) else keep
            val finalKeep = if (boundaryKeep >= maxOf(4, keep - 8)) boundaryKeep else keep
            val trimmedCore = cleanField(coreWords.take(finalKeep).joinToString(" "))
            text = buildText(joinActionWithFragment(trimmedCore, frag))
        }
        return text
    }

    fun renderScene(
        bible: StoryBible,
        plan: V2ScenePlan,
        mode: V2Mode,
        imageCount: Int = 1,
        // v2.1 fix8 v2 — cameraOverride (rantai kamera dari renderAll):
        // bukan null = kamera scene ini melanjutkan scene sebelumnya.
        cameraOverride: CameraMove? = null
    ): V2ScenePrompt {
        // v2.1 fix6 — bersihkan aksi SEBELUM render (berlaku dua mode +
        // rencana lama dari cache ikut bersih saat dibuka ulang).
        val cleanPlan = plan.copy(actionEn = cleanField(sanitizeAction(plan.actionEn, bible)))
        val shot = effectiveShot(cleanPlan, cameraOverride)
        val continues = cameraOverride != null
        val aspect = aspectClause(bible.aspectRatio)
        return if (mode == V2Mode.TEXT_ONLY) {
            val input = shotInputFor(bible, cleanPlan, shot, continues)
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
                flowText = renderReference(bible, cleanPlan, shot, TargetEngine.FLOW, imageCount, continues),
                klingText = renderReference(bible, cleanPlan, shot, TargetEngine.KLING, imageCount, continues),
                seedanceText = renderReference(bible, cleanPlan, shot, TargetEngine.SEEDANCE, imageCount, continues),
                wanText = renderReference(bible, cleanPlan, shot, TargetEngine.WAN, imageCount, continues)
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
        // v2.1 fix8 v2 — peran fight dari BeatMap (satu sumber kebenaran
        // dengan prompter + gerbang R9). Peran dihitung dari TOTAL scene
        // film (bible.totalScenes), BUKAN plans.size — retry hanya me-render
        // subset scene gagal, dan peran subset harus sama dengan peran film
        // penuh (scene 10 tetap IMPACT walau retry cuma 3 scene).
        val beatMap = BeatMap.build(bible.totalScenes, bible.beats)
        val roles = BeatMap.fightRoles(beatMap, bible.beats, isFight)
        // v2.1 fix7 — batas akhir Klimaks untuk siklus jurus (item 1):
        // scene Penutup tidak boleh dapat jurus fight dari siklus.
        val climaxLastScene = beatMap.getOrNull(BeatMap.climaxIndex(bible.beats))?.lastScene
            ?: bible.totalScenes
        // v2.1 fix9 — teruskan scene terakhir FILM ke assignEffects (bukan
        // maxOf daftar plans): render subset (retry/regenerasi) tidak boleh
        // mengubah siapa "scene terakhir" — audit Aang.
        val assigned = assignEffects(plans, isFight, totalBeats, roles, bible.totalScenes, climaxLastScene)
        // v2.1 fix6 (riset) — Mode A cloud tidak punya pose (prompter JSON
        // tak ada field pose) -> "Ends as standing firm." 18x (Bima). Isi
        // dari siklus di sini, SETELAH assignEffects, SEBELUM renderScene:
        // poseStart[N] = poseEnd[N-1] (rantai), poseEnd dari POSE_CYCLE
        // dengan seed = hash judul (deterministik per film). Rencana yang
        // sudah punya pose (Draft Offline) TIDAK disentuh.
        val posed = fillPosesFromCycle(bible, assigned)
        val chained = chainCameras(posed, roles)
        return posed.map { plan ->
            val (override, _) = chained[plan.sceneNumber] ?: (null to false)
            renderScene(bible, plan, mode, imageCount, override)
        }
    }

    /** Rantai kamera fix8 v2 — jawaban "5 scene jadi 1 scene": dua scene
     *  EXCHANGE berurutan memakai gerakan kamera YANG SAMA, dan prompt
     *  menulis "continuing <gerakan> from the previous shot" — potongan
     *  antar-klip jadi tidak kerasa. Rantai PUTUS di scene bukan-EXCHANGE
     *  (TENSION/IMPACT/HERO_HURT/FINISHER memulai gerakan baru) dan di
     *  film non-fight (roles kosong = tidak ada override).
     *  Mengembalikan peta sceneNumber -> (cameraOverride, continues).
     *  Deterministik per sceneNumber (seperti fillPosesFromCycle) supaya
     *  render ulang konsisten. */
    fun chainCameras(
        plans: List<V2ScenePlan>,
        fightRoles: Map<Int, FightRole>
    ): Map<Int, Pair<CameraMove?, Boolean>> {
        if (fightRoles.isEmpty()) return emptyMap()
        val result = mutableMapOf<Int, Pair<CameraMove?, Boolean>>()
        var prevRole: FightRole? = null
        var prevCamera: CameraMove? = null
        for (plan in plans.sortedBy { it.sceneNumber }) {
            val role = fightRoles[plan.sceneNumber]
            val own = effectiveShot(plan).cameraMove
            if (role == FightRole.EXCHANGE && prevRole == FightRole.EXCHANGE && prevCamera != null) {
                result[plan.sceneNumber] = prevCamera to true
            } else {
                result[plan.sceneNumber] = null to false
                prevCamera = own
            }
            prevRole = role
        }
        return result
    }

    // v2.1 fix6 (riset) — isi pose kosong dari siklus pose ScriptEngine.
    // Deterministik per sceneNumber (bukan per posisi di list) supaya
    // render ulang sebagian (retry/regenerasi 1 scene) tetap konsisten
    // dengan render penuh: poseEnd[N] = POSE_CYCLE[(seed + N - 1) % 8],
    // poseStart[N] = poseEnd[N-1] (scene 1: "standing still").
    fun fillPosesFromCycle(bible: StoryBible, plans: List<V2ScenePlan>): List<V2ScenePlan> {
        val cycle = ScriptPools.POSE_CYCLE
        val seed = bible.title.hashCode()
        return plans.map { plan ->
            val n = plan.sceneNumber
            val end = if (plan.poseEnd.isBlank()) {
                cycle[Math.floorMod(seed + n - 1, cycle.size)]
            } else {
                plan.poseEnd
            }
            val start = if (plan.poseStart.isBlank()) {
                if (n <= 1) "standing still" else cycle[Math.floorMod(seed + n - 2, cycle.size)]
            } else {
                plan.poseStart
            }
            plan.copy(poseStart = start, poseEnd = end)
        }
    }
}
