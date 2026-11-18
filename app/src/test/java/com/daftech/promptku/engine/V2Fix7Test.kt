package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix7 — 10 item paparan 18 Nov 2026 (audit lapangan Aang fix9 +
// Olympus): siklus jurus berhenti di Klimaks, segmen timeline + frasa
// kontinuitas dilindungi trim, trim batas frasa + dedupe sambungan,
// join inti→fragmen ", then ", narasi ≠ kalimat dunia, cahaya Mode A
// verbatim dari bible, R4b tetap per-move-once + deteksi lowercase,
// big-attack proximity (bukan bag-of-words), beat-block IMPACT/HERO_HURT
// anti-duplikasi fragmen, G4 kecualikan nama bare + aturan Mode B.
class V2Fix7Test {

    // ---------- fixture ----------

    private fun bima18Bible(): StoryBible = StoryBible(
        title = "Storm Titans: Clash of Legends",
        logline = "Bima battles Rudra.",
        genre = Genre.ACTION,
        characters = listOf(V2Character("Bima", listOf("golden eye tattoos"))),
        antagonistName = "Rudra",
        antagonistDescEn = "A ruthless immortal titan.",
        worldName = "Bayangan Langit",
        worldDescEn = "Floating volcanic islands drift through purple thunderclouds.",
        lightEn = "Blinding violet lightning.",
        ambienceEn = "Roaring thunder.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Storm Titans", "Bima vs Rudra.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun bimaPlan(num: Int): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = "Bima trades blows with Rudra number $num",
        cameraMove = "tracking shot", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = "thunder, wind", effectId = "",
        wantEn = "win", obstacleEn = "Rudra", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Bima"
    )

    private fun aangBible(): StoryBible = StoryBible(
        title = "Final battle Aang vs Fire Emperor",
        logline = "Aang battles the Fire Emperor.",
        genre = Genre.ACTION,
        characters = listOf(V2Character("Aang", listOf("glowing tattoos", "airbending staff"))),
        antagonistName = "Fire Emperor",
        antagonistDescEn = "A ruthless fire tyrant in golden armor.",
        worldName = "Volcanic Wasteland",
        worldDescEn = "Jagged obsidian rocks jut upward while rivers of molten lava carve glowing paths.",
        lightEn = "High-contrast harsh lighting cast by blazing fireballs.",
        ambienceEn = "Roaring flames, crackling stone.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow")
        ),
        episodes = listOf(V2Episode(1, "Final battle", "Aang vs Fire Emperor.", 6)),
        durationMinutes = 1,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun aangPlan(
        num: Int,
        action: String = "Aang trades blows with Fire Emperor number $num",
        dialogueEn: String = "",
        effectId: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = dialogueEn, audioEn = "wind, fire", effectId = effectId,
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Aang"
    )

    // Bible LAPANGAN Aang fix9 (export 10:00 WIB 18 Nov 2026): dunia,
    // cahaya, ambience + anchor verbatim dari bible baru — dipakai untuk
    // replay kasus "the the" S3 dan tes G4.
    private fun fieldBible(): StoryBible = StoryBible(
        title = "Final Battle Aang vs Fire Emperor",
        logline = "Aang battles the Fire Emperor.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Aang",
                listOf("Aang", "bald head with a blue arrow tattoo", "mustard-yellow tunic")
            )
        ),
        antagonistName = "Fire Emperor",
        antagonistDescEn = "A ruthless fire tyrant in golden armor.",
        worldName = "Volcanic Wasteland",
        worldDescEn = "Towering black rock pillars jut from the molten earth while rivers of liquid fire carve glowing pathways through the jagged canyons.",
        lightEn = "Fiery orange and crimson glow casting dramatic shadows across the battlefield.",
        ambienceEn = "Roaring flames, crackling embers, and the fierce howling wind.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow")
        ),
        episodes = listOf(V2Episode(1, "Final Battle", "Aang vs Fire Emperor.", 6)),
        durationMinutes = 1,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun dramaBible(): StoryBible = StoryBible(
        title = "Lembah Sunyi",
        logline = "Mira walks through the valley.",
        genre = Genre.DRAMA,
        characters = listOf(V2Character("Mira", listOf("red scarf"))),
        antagonistName = "",
        antagonistDescEn = "",
        worldName = "Lembah Sunyi",
        worldDescEn = "A quiet valley where wind moves slowly through the grass.",
        lightEn = "Soft morning light.",
        ambienceEn = "Wind in the grass.",
        beats = listOf(
            V2Beat(0, "Pembuka", "morning in the valley"),
            V2Beat(1, "Pemicu", "a letter arrives"),
            V2Beat(2, "Eskalasi", "the walk grows long"),
            V2Beat(3, "Klimaks", "the decision"),
            V2Beat(4, "Penutup", "evening")
        ),
        episodes = listOf(V2Episode(1, "Lembah Sunyi", "Mira walks.", 6)),
        durationMinutes = 1,
        dialogueLang = "ID",
        aspectRatio = "9:16"
    )

    private fun dramaPlan(
        num: Int,
        action: String,
        narrationId: String = "Narasi per scene."
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "slow dolly in", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = "wind", effectId = "",
        wantEn = "rest", obstacleEn = "", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = ""
    )

    private fun flowPrompt(plan: V2ScenePlan, flowText: String = ""): V2ScenePrompt =
        V2ScenePrompt(plan.sceneNumber, plan, flowText, "", "", "")

    private fun fightIds(): Set<String> =
        EffectLibrary.FIGHT_MOVES_ALL.map { it.id }.toSet()

    // ---------- fix7-1: siklus berhenti di Klimaks ----------

    @Test
    fun cycleStopsAtClimaxInRenderAll() {
        // Film 18 scene (Klimaks S13–15): Penutup S16–S18 TIDAK BOLEH
        // dapat jurus fight dari siklus (audit Olympus: S16 "cross" +
        // S17 "uppercut" di aftermath). S15 (Klimaks) tetap dapat jurus.
        val bible = bima18Bible()
        val plans = (1..18).map { bimaPlan(it) }
        val prompts = ModeRender.renderAll(bible, plans, V2Mode.TEXT_ONLY, isFight = true)
        val byNum = prompts.associateBy { it.sceneNumber }
        val fight = fightIds()
        assertEquals("finisher", byNum.getValue(13).plan.effectId)
        assertTrue(byNum.getValue(15).plan.effectId, byNum.getValue(15).plan.effectId in fight)
        assertTrue(byNum.getValue(14).plan.effectId, byNum.getValue(14).plan.effectId in fight)
        // S16 jatuh ke cabang kamera (16 % 4 == 0), bukan jurus.
        val s16 = byNum.getValue(16).plan.effectId
        assertFalse("S16 dapat jurus fight: $s16", s16 in fight)
        assertEquals(
            EffectCategory.CAMERA,
            EffectLibrary.ALL.first { it.id == s16 }.category
        )
        assertEquals("", byNum.getValue(17).plan.effectId)
        assertEquals("", byNum.getValue(18).plan.effectId)
        // Item 6 (plumbing): cahaya Mode A = teks bible, bukan frasa enum.
        assertTrue(byNum.getValue(1).flowText.contains("Lighting: Blinding violet lightning."))
        assertFalse(byNum.getValue(1).flowText.contains("soft overcast daylight"))
    }

    @Test
    fun cycleWithoutClimaxParamStillRunsToEnd() {
        // Kompatibel mundur: pemanggil lama tanpa climaxLastScene (default
        // 0) tetap menjalankan siklus sampai akhir film.
        val bible = bima18Bible()
        val beatMap = BeatMap.build(bible.totalScenes, bible.beats)
        val roles = BeatMap.fightRoles(beatMap, bible.beats, true)
        val plans = (1..18).map { bimaPlan(it) }
        val assigned = ModeRender.assignEffects(
            plans, isFight = true, totalBeats = 5,
            fightRoles = roles, filmTotalScenes = 18
        )
        val s16 = assigned.first { it.sceneNumber == 16 }.effectId
        assertTrue("tanpa climaxLastScene S16 harus dapat jurus: $s16", s16 in fightIds())
    }

    @Test
    fun blankActionFailsG5() {
        val bible = dramaBible()
        val scenes = listOf(flowPrompt(dramaPlan(1, "")))
        val g5 = ValidationGates.runGates(bible, scenes, hasImages = false)
            .first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.toString(), g5.failures.any { it.contains("aksi kosong") })
    }

    @Test
    fun worldOnlyActionFailsG5AsBlank() {
        // Mekanisme scene hantu Olympus S17: aksi LLM = kalimat dunia,
        // sanitizeAction mengosongkannya saat render — gerbang harus
        // membaca yang sama dengan yang dirender.
        val bible = dramaBible()
        val scenes = listOf(flowPrompt(dramaPlan(1, bible.worldDescEn)))
        val g5 = ValidationGates.runGates(bible, scenes, hasImages = false)
            .first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.toString(), g5.failures.any { it.contains("aksi kosong") })
    }

    // ---------- fix7-2: trim lindungi segmen + kontinuitas ----------

    private fun trimFixtureText(): String = listOf(
        "Aang stands on cracked ice under a storm sky, wind tearing at his tunic.",
        "[00:00-00:04] Aang lunges forward with blazing fists, fire swirling around his arms.",
        "The crowd gasps as embers scatter across the frozen ground.",
        "Sparks fly while the emperor raises his burning staff high.",
        "[00:04-00:08] the motion settles, ending as standing tall.",
        "Style A, Style B.",
        "Audio: wind, fire, crackling stone.",
        "Starts from standing still, ends as standing tall."
    ).joinToString(" ")

    @Test
    fun trimToWordsKeepsTimelineSegment() {
        // 71 kata -> cap 40: yang dibuang = kalimat pengisi (s2/s3/s4/
        // "Style A, Style B."); yang hidup = seg2 [00:04-00:08], Audio,
        // dan frasa kontinuitas "Starts from...".
        val out = MythFilter.trimToWords(trimFixtureText(), 40)
        assertTrue(out.contains("[00:04-00:08]"))
        assertTrue(out.contains("Starts from standing still"))
        assertTrue(out.contains("Audio:"))
        assertFalse(out.contains("The crowd gasps"))
        assertFalse(out.contains("[00:00-00:04]"))
    }

    @Test
    fun trimToCharsKeepsTimelineSegment() {
        val out = MythFilter.trimToChars(trimFixtureText(), 230)
        assertTrue(out.contains("[00:04-00:08]"))
        assertTrue(out.contains("Starts from standing still"))
        assertTrue(out.contains("Audio:"))
        assertFalse(out.contains("The crowd gasps"))
    }

    // ---------- fix7-3: trim batas frasa (Mode B) ----------

    // Aksi inti S3 lapangan VERBATIM (export Aang fix9): trim lama
    // memotong "...as the" lalu fragmen melengkapi jadi "as the the blow
    // lands". 33 kata; render lapangan tepat 180 kata (trim jalan).
    private val aangS3Core =
        "Aang drives his elbow forward into the Fire Emperor with lips parted in intense exertion while his mustard-yellow tunic snaps in the scorching wind, transitioning instantly into slow motion as the blow lands"

    private fun fieldS3Plan(action: String): V2ScenePlan = V2ScenePlan(
        sceneNumber = 3, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "close-up", angle = "eye",
        dialogueId = "", dialogueEn = "",
        audioEn = "All sound drops to a low heartbeat just before the heavy blow, then a loud impact crack echoes through the roaring flames.",
        effectId = "fight_impact",
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Aang"
    )

    @Test
    fun modeBTrimBacksOffToPhraseBoundary() {
        // Replay kasus lapangan: trim mundur ke koma terakhir ("wind,")
        // lalu join ", then " menutupi bekas potongan — bukan "the the".
        // v2.2 — titik potong pindah dengan cap Flow 300 (keep ~150 kata,
        // bukan ~30): batas "wind," digeser ke dekat titik potong baru
        // dengan filler 122 kata DI DEPAN inti, supaya guard 8 kata tetap
        // menguji back-off yang sama.
        val bible = fieldBible()
        val fillerA = "Aang presses forward through the storm, fists blazing in the wind, driving the Emperor back. "
        val fillerB = "Aang presses forward, fists blazing in the wind, driving the Emperor back across the frozen ground. "
        val plan = fieldS3Plan(fillerA.repeat(6) + fillerB.repeat(2) + aangS3Core)
        val shot = ModeRender.effectiveShot(plan)
        val rendered = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(rendered.contains("scorching wind, then the blow lands"))
        assertFalse(rendered.contains("the the"))
        assertFalse(rendered.contains("as the, then"))
        // v2.2 — cap Flow 180 -> 300 (terukur 296 kata).
        assertTrue(MythFilter.countWords(rendered) <= 300)
    }

    @Test
    fun modeBTrimNeverLeavesDanglingFunctionWord() {
        // Inti panjang (pengisi berkoma tiap 5-8 kata + ekor S3): trim
        // PASTI mundur ke batas frasa; kata sebelum ", then " tidak
        // boleh kata sambung yang menggantung; fragmen tetap utuh.
        val filler = "Aang lunges forward, fists blazing in the wind, ".repeat(6)
        val bible = fieldBible()
        val plan = fieldS3Plan(filler + aangS3Core)
        val shot = ModeRender.effectiveShot(plan)
        val rendered = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertFalse(rendered.contains("the the"))
        assertTrue(rendered.contains("dust bursting from the ground"))
        // v2.2 — cap Flow 180 -> 300.
        assertTrue(MythFilter.countWords(rendered) <= 300)
        val beforeThen = rendered.substringBefore(", then").trim()
            .split(Regex("\\s+")).last()
        assertFalse(
            "kata sebelum ', then ' menggantung: '$beforeThen'",
            beforeThen.trimEnd(',', '.', ';', ':').lowercase() in
                setOf("the", "as", "to", "into", "a", "an", "and", "while", "with")
        )
    }

    // ---------- fix7-4: join ", then " ----------

    @Test
    fun joinActionWithFragmentUsesThen() {
        assertEquals(
            "Aang stands tall, then the cross snaps the head back",
            ModeRender.joinActionWithFragment("Aang stands tall", "the cross snaps the head back")
        )
    }

    @Test
    fun joinActionWithFragmentDedupesRepeatedWord() {
        // Kasus S3: inti "...as the" + fragmen "the blow lands" — kata
        // inti yang dibuang, fragmen utuh (prinsip fix9).
        assertEquals(
            "Aang strikes as, then the blow lands hard",
            ModeRender.joinActionWithFragment("Aang strikes as the", "the blow lands hard")
        )
        assertEquals("the blow lands hard", ModeRender.joinActionWithFragment("", "the blow lands hard"))
        assertEquals("Aang stands tall", ModeRender.joinActionWithFragment("Aang stands tall", ""))
    }

    @Test
    fun modeAFlowContainsThenJoin() {
        // Mode A: shotInputFor menggabung inti + fragmen dengan ", then ".
        val bible = aangBible()
        val plan = aangPlan(
            3,
            action = "Aang drives his elbow forward into the Fire Emperor",
            effectId = "fight_impact"
        )
        val prompts = ModeRender.renderAll(
            bible, listOf(plan), V2Mode.TEXT_ONLY, isFight = true
        )
        val flow = prompts.first().flowText
        assertTrue(flow.contains("Aang drives his elbow forward into the Fire Emperor, then the blow lands"))
        assertFalse(flow.contains("the the"))
    }

    // ---------- fix7-5: narasi ≠ kalimat dunia ----------

    @Test
    fun narrationEqualToWorldFailsG5() {
        val bible = dramaBible()
        val plan = dramaPlan(1, "Mira walks slowly through the valley grass.", narrationId = bible.worldDescEn)
        val g5 = ValidationGates.runGates(bible, listOf(flowPrompt(plan)), hasImages = false)
            .first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.toString(), g5.failures.any { it.contains("narasi sama dengan kalimat dunia") })
    }

    @Test
    fun narrationIsWorldCopyChecksPrefix() {
        val world = fieldBible().worldDescEn
        assertTrue(ValidationGates.narrationIsWorldCopy(world, world))
        assertTrue(
            ValidationGates.narrationIsWorldCopy(
                world.take(60) + " lalu Aang melangkah maju.", world
            )
        )
        assertFalse(
            ValidationGates.narrationIsWorldCopy(
                "Angin bertiup pelan di lembah yang sunyi.", world
            )
        )
    }

    @Test
    fun scenePlansFromJsonStripsNarrationPrefix() {
        // Prefix "Narator:" / "Suara narator:" tidak konsisten di
        // lapangan (audit Olympus) — parser menyeragamkan di sini.
        val arr = Json.arrOf(
            Json.objOf(
                "scene_number" to Json.numOf(1),
                "action_en" to Json.strOf("Aang stands tall."),
                "narration_id" to Json.strOf("Narator: Angin bertiup pelan di lembah.")
            ),
            Json.objOf(
                "scene_number" to Json.numOf(2),
                "action_en" to Json.strOf("Aang steps forward."),
                "narration_id" to Json.strOf("Suara narator: Api menyala di kejauhan.")
            ),
            Json.objOf(
                "scene_number" to Json.numOf(3),
                "action_en" to Json.strOf("Aang rests."),
                "narration_id" to Json.strOf("Angin bertiup pelan.")
            )
        )
        val plans = V2Models.scenePlansFromJson(arr)
        assertEquals("Angin bertiup pelan di lembah.", plans[0].narrationId)
        assertEquals("Api menyala di kejauhan.", plans[1].narrationId)
        assertEquals("Angin bertiup pelan.", plans[2].narrationId)
    }

    // ---------- fix7-6: cahaya Mode A verbatim ----------

    @Test
    fun modeALightUsesBibleTextVerbatim() {
        // LightKind OVERCAST tanpa lightText = "soft overcast daylight";
        // dengan lightText, teks bible menang (audit Olympus 18/18 salah).
        val input = ShotInput(
            title = "Test Scene",
            genre = Genre.ACTION,
            style = VisualStyle.REALISTIC,
            engine = TargetEngine.FLOW,
            character = Templates.gokuDna,
            secondCharacter = Templates.cellDna,
            world = Templates.gokuVsCellWorld,
            shotSize = ShotSize.WIDE,
            angle = CameraAngle.LOW,
            lensMm = 35,
            cameraMove = CameraMove.CRANE_UP,
            light = LightKind.OVERCAST,
            lightText = "Harsh, directional sunlight slicing through clouds, casting long shadows on the ruins.",
            actionEn = "lands hard in a burst of dust, stands up slowly and faces Cell across the ring",
            poseStart = "standing at the far edge of the arena",
            poseEnd = "standing tall, facing Cell",
            materialEn = "dust explodes outward, ground cracks propagate from his boots",
            audioEn = "desert wind, distant crowd murmur fading, low orchestral drone",
            focusMode = FocusMode.FULL_STABLE,
            hideTactic = HideTactic.DISTANCE
        )
        val prompt = PromptEngine.build(input)
        assertTrue(
            prompt.text.contains(
                "Lighting: Harsh, directional sunlight slicing through clouds, casting long shadows on the ruins."
            )
        )
        assertFalse(prompt.text.contains("soft overcast daylight"))
    }

    // ---------- fix7-7: R4b lowercase ----------

    @Test
    fun lowercaseAvatarStateIsNamedMove() {
        // Kasus lapangan Aang fix9 S5: "the ultimate avatar state"
        // lowercase — extractNamedMove lama (Title-Case saja) buta.
        val bible = aangBible()
        assertEquals(
            "avatar state",
            ArcLexicon.extractNamedMove(
                "Aang channels the ultimate avatar state with glowing eyes.", bible
            )
        )
        assertEquals(
            "beast mode",
            ArcLexicon.extractNamedMove("Aang enters beast mode, roaring.", bible)
        )
    }

    @Test
    fun lowercaseTransformWithoutTriggerIsNotNamedMove() {
        // Scan SEMPIT (transformasi saja): tanpa kata pemicu, frasa
        // "state" biasa bukan jurus — "energy beam" / "plasma disk" /
        // "orange vortex" tetap urusan R4 (isBigAttack), bukan R4b.
        val bible = aangBible()
        assertNull(
            ArcLexicon.extractNamedMove(
                "Aang stands in a calm mental state, breathing slowly.", bible
            )
        )
        assertNull(
            ArcLexicon.extractNamedMove("Aang surveys the state of the world.", bible)
        )
    }

    @Test
    fun lowercaseMoveRepeatFailsR4b() {
        // Dua scene pakai "avatar state" lowercase: yang kedua gagal R4b
        // (per-move-once — keputusan gas fix7, bukan 1 jurus total).
        val bible = aangBible()
        val scenes = listOf(
            flowPrompt(aangPlan(1, "Aang stands facing the Fire Emperor.")),
            flowPrompt(aangPlan(2, "Aang circles the Fire Emperor slowly, testing his guard.")),
            flowPrompt(
                aangPlan(
                    3,
                    "Aang channels the ultimate avatar state, eyes glowing white as energy gathers around his fists."
                )
            ),
            flowPrompt(aangPlan(4, "Aang steadies his breathing, watching the Fire Emperor.")),
            flowPrompt(
                aangPlan(
                    5,
                    "Aang channels the ultimate avatar state again, light bursting from his body as the Fire Emperor staggers."
                )
            ),
            flowPrompt(aangPlan(6, "Aang walks away from the ruined battlefield, smoke clearing behind him."))
        )
        val result = ValidationGates.runArcGate(bible, scenes)
        assertFalse(result.passed)
        assertTrue(
            result.failures.toString(),
            result.failures.any {
                it.contains("avatar state") && it.contains("sudah dipakai di scene 3")
            }
        )
    }

    // ---------- fix7-8: big-attack proximity ----------

    @Test
    fun bigAttackNeedsNearbyNoun() {
        // Jendela 6 token (diukur dari fixture verbatim: serangan asli
        // terjauh 6 = Kaito S7; noise terdekat 8 = Olympus S11).
        assertFalse(
            ArcLexicon.isBigAttack(
                "Bima shouts, mouth wide open, eyes blazing with rage, swinging his silver gauntlet in a sweeping arc while his hair whips in the wind, and Rudra counters, thrusting his spear upward, sparks flying between them."
            )
        )
        assertTrue(
            ArcLexicon.isBigAttack(
                "Bima hurls a glowing plasma disk across the chasm, weight shifting heavily onto his lead foot as his scarred chest heaves for breath."
            )
        )
        assertTrue(
            ArcLexicon.isBigAttack(
                "Kaito unleashes a storm-surge strike, channeling lightning through his fists while his hair whips wildly, his expression fierce with defiance."
            )
        )
        assertFalse(
            ArcLexicon.isBigAttack(
                "Kaito channels a blinding Kamehameha, his aura blazing orange, his breath huffing heavily, and releases the beam toward Frieza's violet energy field."
            )
        )
        // "blast" sekaligus kata kerja dan nomina di token YANG SAMA —
        // tidak boleh berpasangan dengan dirinya sendiri.
        assertFalse(
            ArcLexicon.isBigAttack(
                "General Kallistos charges forward, spear-like blade slashing, hair flickering in the blast, mouth wide open shouting rage ducks under the swinging arm by inches."
            )
        )
    }

    // ---------- fix7-9: beat-block anti-duplikasi ----------

    @Test
    fun impactBeatBlockAvoidsSlowMotionWords() {
        // Blok IMPACT lama nyuruh "lands the decisive blow" (R1 menghukum
        // "decisive") + "time ramps into slow motion" (fragmen IMPACT juga
        // slow-mo — duplikasi S3/S5 lapangan). Blok baru: "time dilates"
        // + larangan eksplisit menulis "slow motion" di action_en.
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(10), "")
        assertTrue(prompt.contains("time dilates"))
        assertTrue(prompt.contains("lands a heavy blow"))
        assertTrue(prompt.contains("Do NOT write the words \"slow motion\""))
        assertFalse(prompt.contains("Write the words \"slow motion\" in action_en HERE"))
        assertFalse(prompt.contains("time ramps into slow motion"))
        assertFalse(prompt.contains("the camera pushing in close on"))
    }

    @Test
    fun heroHurtBeatBlockUsesDifferentWordsThanFragment() {
        // Blok HERO_HURT lama = parafrasa fragmen ("staggers back, blood
        // on his lip, knees buckling") — inti + fragmen duplikat 2x di
        // render (S4 lapangan). Blok fix7 ganti "blood on his lip" tapi
        // "one knee buckling" masih menggema "knees buckling" fragmen
        // (lapangan Everest S4: citra sama 2x, singular vs plural).
        // Blok fix10 hindari SEMUA frasa fragmen: "clutching his side"
        // (bukan "knees buckling"), tetap lolos R9b via "stumbles"/"reeling".
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(12), "")
        assertTrue(prompt.contains("reeling"))
        assertTrue(prompt.contains("clutching his side"))
        assertFalse(prompt.contains("blood on his lip"))
        assertFalse(prompt.contains("staggers back"))
        assertFalse(prompt.contains("knees buckling"))
        assertFalse(prompt.contains("one knee buckling"))
    }

    @Test
    fun newRoleWordingsPassR9() {
        // Aksi inti gaya blok BARU: IMPACT tanpa frasa slow-mo (R9a),
        // HERO_HURT dengan penanda yang dikenali isHeroHurt (R9b).
        val bible = aangBible()
        val scenes = listOf(
            flowPrompt(aangPlan(1, "Aang stands facing the Fire Emperor across the cracked ground.")),
            flowPrompt(aangPlan(2, "Aang circles the Fire Emperor slowly, testing his guard.")),
            flowPrompt(
                aangPlan(
                    3,
                    "Aang lands a heavy blow on Fire Emperor — time dilates into a held heartbeat as the blow connects, Fire Emperor's face twisting in shock."
                )
            ),
            flowPrompt(
                aangPlan(
                    4,
                    "Fire Emperor lands a clean hit — Aang stumbles backward, reeling from the hit, clutching his side, then his jaw sets and he rises."
                )
            ),
            flowPrompt(
                aangPlan(
                    5,
                    "Aang drives his fist into the Fire Emperor's chest, the final blow landing with a thunderous crack."
                )
            ),
            flowPrompt(aangPlan(6, "Aang walks away from the ruined battlefield, smoke clearing behind him."))
        )
        val result = ValidationGates.runArcGate(bible, scenes)
        assertTrue(
            result.failures.toString(),
            result.failures.none { it.contains("slow-mo") || it.contains("HERO HURT") }
        )
    }

    // ---------- fix7-10: G4 kecualikan nama + aturan Mode B ----------

    @Test
    fun g4IgnoresBareHeroNameAnchor() {
        // Anchor nama bare ("Aang") = noise: fix4 WAJIB menamai aktor di
        // tiap aksi, jadi nama pasti muncul. G4 cuma cek anchor penampilan.
        val bible = fieldBible()
        val plan = aangPlan(
            1,
            action = "Aang steps forward into the volcanic wasteland.",
            dialogueEn = "Stand down."
        )
        val flowText = "Using the reference image of Aang, Aang steps forward into the volcanic wasteland. " +
            "Setting: Towering black rock pillars. Aang says: \"Stand down.\" Audio: wind."
        val g4 = ValidationGates.runGates(bible, listOf(flowPrompt(plan, flowText)), hasImages = true)
            .first { it.id == "G4" }
        assertTrue(g4.failures.toString(), g4.passed)
    }

    @Test
    fun g4StillCatchesAppearanceAnchor() {
        // Anchor penampilan TETAP dicek — nama dikecualikan, bukan
        // deskripsinya.
        val bible = fieldBible()
        val plan = aangPlan(
            1,
            action = "Aang steps forward, his bald head with a blue arrow tattoo catching the firelight.",
            dialogueEn = "Stand down."
        )
        val flowText = "Using the reference image of Aang, Aang steps forward, his bald head with a blue arrow tattoo catching the firelight. " +
            "Setting: Towering black rock pillars. Aang says: \"Stand down.\" Audio: wind."
        val g4 = ValidationGates.runGates(bible, listOf(flowPrompt(plan, flowText)), hasImages = true)
            .first { it.id == "G4" }
        assertFalse(g4.passed)
        assertTrue(
            g4.failures.toString(),
            g4.failures.any { it.contains("bald head with a blue arrow tattoo") }
        )
    }

    @Test
    fun prompterUserModeBHasAppearanceRule() {
        val bible = bima18Bible()
        val ref = DirectorPrompts.prompterUser(bible, listOf(1), "", mode = V2Mode.REFERENCE)
        assertTrue(ref.contains("REFERENCE IMAGES:"))
        assertTrue(ref.contains("NEVER describe their appearance"))
        val textOnly = DirectorPrompts.prompterUser(bible, listOf(1), "", mode = V2Mode.TEXT_ONLY)
        assertFalse(textOnly.contains("REFERENCE IMAGES:"))
    }
}
