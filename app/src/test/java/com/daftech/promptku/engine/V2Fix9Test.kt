package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix9 — temuan audit lapangan "Final battle Aang vs Fire Emperor"
// (Mode B, 5 scene, 00:30 WIB 18 Nov 2026). Tiga bug render + cacat kecil:
//  (1) beat block IMPACT nyuruh "decisive blow" sementara R1 menghukum
//      "decisive" (prompter vs gerbang kontradiksi sendiri);
//  (2) cabang lastScene membayangi peran FINISHER di film pendek;
//  (3) retry render subset (peran + rantai hilang);
//  (4) Mode B tidak pernah mencetak fragmen efek;
//  (5) dialog "!." (cleanField cuma strip '.');
//  (6) kalimat menggantung ("closely as.", "full speed while.");
//  (7) batch gagal = film terpotong diam-diam (V2ViewModel, UI —
//      tidak bisa dites lokal; CI yang jadi gerbangnya).
class V2Fix9Test {

    // ---------- fixture Aang (cerminan film lapangan) ----------

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
        dialogueEn: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = dialogueEn, audioEn = "wind, fire", effectId = "",
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Aang"
    )

    // ---------- fix9-1: IMPACT tanpa kata finalitas ----------

    @Test
    fun impactBeatBlockUsesHeavyBlowNotDecisiveBlow() {
        // Bible 18 scene / 5 beat (gaya V2BeatPromptTest): IMPACT = S10.
        val bible = StoryBible(
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
                V2Beat(0, "Pembuka", "the tyrant descends"),
                V2Beat(1, "Pemicu", "first exchange"),
                V2Beat(2, "Eskalasi", "the fight grows"),
                V2Beat(3, "Klimaks", "the decisive clash"),
                V2Beat(4, "Penutup", "silence returns")
            ),
            episodes = listOf(V2Episode(1, "Storm Titans", "Bima vs Rudra.", 18)),
            durationMinutes = 3,
            dialogueLang = "EN",
            aspectRatio = "9:16"
        )
        val prompt = DirectorPrompts.prompterUser(bible, listOf(10), "")
        // "decisive" BOLEH muncul di daftar FORBIDDEN beat Eskalasi — yang
        // dilarang adalah frasa perintahnya ("lands the decisive blow").
        assertTrue(prompt, prompt.contains("lands a heavy blow"))
        assertFalse(prompt, prompt.contains("lands the decisive blow"))
        assertTrue(prompt, prompt.contains("Scene 10 = IMPACT"))
    }

    // ---------- fix9-2: peran didahulukan atas lastScene ----------

    @Test
    fun finisherKeepsEffectWhenItIsAlsoTheLastScene() {
        // Film Aang: S5 = FINISHER + scene terakhir yang ter-render (S6
        // Penutup hilang di lapangan). Sebelum fix9, cabang lastScene menang
        // -> effectId "" -> kamera "slow dolly in" rencana (bukan CRANE_UP
        // finisher).
        val plans = (1..5).map { aangPlan(it) }
        val roles = mapOf(3 to FightRole.IMPACT, 4 to FightRole.HERO_HURT, 5 to FightRole.FINISHER)
        val assigned = ModeRender.assignEffects(plans, isFight = true, totalBeats = 4, fightRoles = roles)
        assertEquals("finisher", assigned.first { it.sceneNumber == 5 }.effectId)
        assertEquals("fight_impact", assigned.first { it.sceneNumber == 3 }.effectId)
        assertEquals("fight_hero_hurt", assigned.first { it.sceneNumber == 4 }.effectId)
    }

    @Test
    fun roleEffectSurvivesSubsetRenderWhenFilmTotalScenesGiven() {
        // Retry/regenerasi render subset [S3] doang: tanpa filmTotalScenes,
        // S3 jadi "last scene" sendiri -> efek IMPACT hilang. Dengan param
        // filmTotalScenes = 5 (diisi renderAll dari bible.totalScenes),
        // peran tetap menang.
        val subset = listOf(aangPlan(3))
        val roles = mapOf(3 to FightRole.IMPACT, 4 to FightRole.HERO_HURT, 5 to FightRole.FINISHER)
        val assigned = ModeRender.assignEffects(
            subset, isFight = true, totalBeats = 4, fightRoles = roles, filmTotalScenes = 5
        )
        assertEquals("fight_impact", assigned.first().effectId)
        val finisherSubset = ModeRender.assignEffects(
            listOf(aangPlan(5)), isFight = true, totalBeats = 4, fightRoles = roles, filmTotalScenes = 5
        )
        assertEquals("finisher", finisherSubset.first().effectId)
    }

    @Test
    fun lastSceneWithoutRoleStillGetsNoEffect() {
        // Penutup tanpa peran tetap dikosongkan (cabang lastScene masih ada,
        // cuma urutannya di bawah peran). Film non-fight tanpa roles: S6
        // (terakhir) kosong, S4 (%4==0) dapat efek kamera.
        val plans = (1..6).map { aangPlan(it) }
        val assigned = ModeRender.assignEffects(plans, isFight = false, totalBeats = 4)
        assertEquals("", assigned.first { it.sceneNumber == 6 }.effectId)
        assertTrue(assigned.first { it.sceneNumber == 4 }.effectId.isNotBlank())
    }

    // ---------- fix9-3 + fix9-4: retry film penuh + fragmen Mode B ----------

    @Test
    fun modeBRenderPrintsRoleFragmentsAndChain() {
        // Peta beat bible ini (6 scene / 4 beat): [1,1,2,2] — Pembuka S1,
        // Pemicu S2, Eskalasi S3–S4, Klimaks S5–S6. Peran: S1/S2 EXCHANGE,
        // S3 IMPACT, S4 HERO_HURT, S5 FINISHER, S6 EXCHANGE (scene ke-2
        // Klimaks). (Bukan [1,2,2,1] — bobot generik 4 beat = [1,1,2,2].)
        val bible = aangBible()
        val plans = (1..6).map { aangPlan(it) }
        val rendered = ModeRender.renderAll(bible, plans, V2Mode.REFERENCE, isFight = true, imageCount = 3)
        val byNum = rendered.associateBy { it.sceneNumber }
        // Rantai kamera: S2 EXCHANGE melanjutkan whip pan S1.
        assertTrue(byNum.getValue(2).flowText, byNum.getValue(2).flowText.contains(
            "continuing whip pan, fast motion blur, snapping across the action from the previous shot"
        ))
        // Efek siklus S2 tetap punch_uppercut (lensa 24) walau kameranya
        // dirantai ke WHIP_PAN S1.
        assertTrue(byNum.getValue(2).flowText, byNum.getValue(2).flowText.contains("24mm lens"))
        assertTrue(byNum.getValue(2).flowText, byNum.getValue(2).flowText.contains("the uppercut lifts the jaw"))
        // Fragmen peran kini TERCETAK di Mode B (sebelum fix9: tidak ada).
        assertTrue(byNum.getValue(3).flowText, byNum.getValue(3).flowText.contains(
            "the blow lands — the motion ramps into slow motion for a heartbeat"
        ))
        assertTrue(byNum.getValue(3).flowText, byNum.getValue(3).flowText.contains("85mm lens"))
        assertTrue(byNum.getValue(4).flowText, byNum.getValue(4).flowText.contains(
            "staggers back, blood on his lip, knees buckling"
        ))
        assertTrue(byNum.getValue(1).flowText, byNum.getValue(1).flowText.contains("the cross snaps the head back"))
        // Finisher: fragmen + nama antagonis KAPITAL (bukan "fire emperor").
        assertTrue(byNum.getValue(5).flowText, byNum.getValue(5).flowText.contains("the final blow lands"))
        assertTrue(byNum.getValue(5).flowText, byNum.getValue(5).flowText.contains("Fire Emperor collapses in slow motion"))
        assertFalse(byNum.getValue(5).flowText, byNum.getValue(5).flowText.contains("fire emperor collapses"))
        assertTrue(byNum.getValue(5).flowText, byNum.getValue(5).flowText.contains("24mm lens"))
        // S6 EXCHANGE (scene ke-2 Klimaks): jurus siklus berikutnya.
        assertTrue(byNum.getValue(6).flowText, byNum.getValue(6).flowText.contains("the roundhouse kick whips around"))
    }

    @Test
    fun fullListRerenderKeepsRoleEffectAndChainForSubstitutedScene() {
        // Simulasi jalur retry fix9: scene retry diganti plan segar
        // (effectId "") lalu renderAll dipanggil dengan DAFTAR PENUH
        // (bukan subset) — efek peran + rantai harus utuh. Sebelum fix9,
        // renderAll([S3]) bikin S3 kehilangan fight_impact dan S2 kehilangan
        // "continuing".
        val bible = aangBible()
        val plans = (1..6).map { aangPlan(it) }
        val first = ModeRender.renderAll(bible, plans, V2Mode.REFERENCE, isFight = true, imageCount = 3)
        val fresh3 = aangPlan(3, "Aang counters with a spinning kick").copy(effectId = "")
        val allPlans = first.map { it.plan }
            .map { if (it.sceneNumber == 3) fresh3 else it }
        val second = ModeRender.renderAll(bible, allPlans, V2Mode.REFERENCE, isFight = true, imageCount = 3)
        val byNum = second.associateBy { it.sceneNumber }
        assertTrue(byNum.getValue(3).flowText, byNum.getValue(3).flowText.contains(
            "the blow lands — the motion ramps into slow motion for a heartbeat"
        ))
        assertTrue(byNum.getValue(3).flowText, byNum.getValue(3).flowText.contains("85mm lens"))
        assertTrue(byNum.getValue(2).flowText, byNum.getValue(2).flowText.contains(
            "continuing whip pan, fast motion blur, snapping across the action from the previous shot"
        ))
    }

    @Test
    fun longActionTrimKeepsFragmentWhole() {
        // v2.2 — aksi inti 315 kata + fragmen fight_impact: trim Mode B
        // harus memotong ekor INTI saja — fragmen utuh sampai kata
        // terakhirnya ("dust bursting from the ground"), total tetap
        // <= 300 kata (cap Flow baru; dulu 198 kata / cap 180).
        val bible = aangBible()
        val longAction = ("Aang lunges forward with blazing fists and burning rage ".repeat(35)).trim()
        assertTrue(MythFilter.countWords(longAction) > 300)
        val plan = aangPlan(4, longAction).copy(effectId = "fight_impact")
        val shot = ModeRender.effectiveShot(plan)
        val text = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(text, text.contains("dust bursting from the ground"))
        assertTrue(text, text.contains("the blow lands — the motion ramps into slow motion for a heartbeat"))
        assertTrue("kata=${MythFilter.countWords(text)}", MythFilter.countWords(text) <= 300)
    }

    // ---------- fix9-5: dialog "!" / "?" tanpa titik tambahan ----------

    @Test
    fun dialogueEndingWithExclamationKeepsItWithoutExtraPeriod() {
        val bible = aangBible()
        val plan = aangPlan(1, dialogueEn = "Balance will be restored to the world!")
        val shot = ModeRender.effectiveShot(plan)
        val text = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(text, text.contains("Aang says: Balance will be restored to the world!"))
        assertFalse(text, text.contains("world!."))
    }

    @Test
    fun dialogueEndingWithQuestionKeepsItWithoutExtraPeriod() {
        val bible = aangBible()
        val plan = aangPlan(1, dialogueEn = "Are you ready?")
        val shot = ModeRender.effectiveShot(plan)
        val text = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(text, text.contains("Aang says: Are you ready?"))
        assertFalse(text, text.contains("ready?."))
    }

    @Test
    fun dialogueEndingWithPeriodStillGetsSinglePeriod() {
        val bible = aangBible()
        val plan = aangPlan(1, dialogueEn = "We stand together.")
        val shot = ModeRender.effectiveShot(plan)
        val text = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(text, text.contains("Aang says: We stand together."))
        assertFalse(text, text.contains("together.."))
    }

    // ---------- fix9-6: gerbang kalimat menggantung ----------

    @Test
    fun danglingWordDetectsTrailingConjunctions() {
        assertEquals("as", ValidationGates.danglingWord("The Fire Emperor thrusts forward, while tracking shot follows the subject closely as."))
        assertEquals("while", ValidationGates.danglingWord("Aang lands the blow, then snapping back to full speed while."))
        assertEquals("with", ValidationGates.danglingWord("Aang leaves with"))
        assertEquals("and", ValidationGates.danglingWord("Aang strikes, and"))
        assertNull(ValidationGates.danglingWord("Aang fights as the sun sets."))
        assertNull(ValidationGates.danglingWord("Aang stands firm."))
        assertNull(ValidationGates.danglingWord(""))
        assertNull(ValidationGates.danglingWord("Aang walks through the valley while the wind blows."))
    }

    @Test
    fun danglingActionFailsG5AndNothingElse() {
        // Film DRAMA 6 scene yang bersih — satu-satunya cacat: aksi S1
        // menggantung ("...while"). G5 harus gagal dengan pesan menggantung,
        // gerbang lain tetap lolos (fixture bersih, bukan artefak tes).
        val bible = StoryBible(
            title = "Hujan di Jakarta",
            logline = "Riko pulang di tengah hujan.",
            genre = Genre.DRAMA,
            characters = listOf(V2Character("Riko", listOf("jaket hujan kuning"))),
            antagonistName = "",
            antagonistDescEn = "",
            worldName = "Jakarta",
            worldDescEn = "Rainy Jakarta streets glow under neon signs.",
            lightEn = "Soft neon glow through rain.",
            ambienceEn = "Rain tapping on rooftops.",
            beats = listOf(
                V2Beat(0, "Pembuka", "rain begins"),
                V2Beat(1, "Pemicu", "the walk home"),
                V2Beat(2, "Eskalasi", "memories surface"),
                V2Beat(3, "Klimaks", "the phone call"),
                V2Beat(4, "Penutup", "morning after")
            ),
            episodes = listOf(V2Episode(1, "Hujan", "Riko pulang.", 6)),
            durationMinutes = 1,
            dialogueLang = "ID",
            aspectRatio = "9:16"
        )
        val actions = listOf(
            "Riko walks through the rainy market, holding a yellow umbrella.",
            "Riko sits by the window, watching the rain fall on the street.",
            "Riko pours tea in the small kitchen, steam rising slowly.",
            "Riko reads an old letter under the warm lamp light.",
            "Riko calls his mother, smiling as her voice fills the room.",
            "Riko sleeps peacefully as the rain taps the roof above."
        )
        val narrations = listOf(
            "Hujan turun di pasar yang ramai sore itu.",
            "Dari jendela, jalanan basah berkilau diterpa lampu.",
            "Teh hangat menemani malam yang panjang.",
            "Surat lama itu membawa kembali masa kecilnya.",
            "Suara ibunya membuat rumah terasa dekat.",
            "Pagi datang membawa langit yang lebih cerah."
        )
        fun plans(danglingFirst: Boolean): List<V2ScenePlan> = actions.mapIndexed { i, a ->
            V2ScenePlan(
                sceneNumber = i + 1,
                actionEn = if (danglingFirst && i == 0) "Riko walks through the rainy market while" else a,
                cameraMove = "static", lensMm = 35, shotSize = "medium", angle = "eye",
                dialogueId = "", dialogueEn = "", audioEn = "rain", effectId = "",
                wantEn = "pulang", obstacleEn = "hujan", transitionEn = "",
                narrationId = narrations[i], dialogueSpeaker = ""
            )
        }
        // Baseline bersih: semua gerbang lolos.
        val clean = ModeRender.renderAll(bible, plans(false), V2Mode.TEXT_ONLY, isFight = false, imageCount = 1)
        val cleanGates = ValidationGates.runGates(bible, clean, hasImages = false)
        assertTrue(cleanGates.toString(), cleanGates.all { it.passed })
        // Satu aksi menggantung: HANYA G5 yang gagal, dengan pesan yang tepat.
        val dangling = ModeRender.renderAll(bible, plans(true), V2Mode.TEXT_ONLY, isFight = false, imageCount = 1)
        val gates = ValidationGates.runGates(bible, dangling, hasImages = false)
        val g5 = gates.first { it.id == "G5" }
        assertFalse(g5.passed)
        assertTrue(g5.failures.toString(), g5.failures.any { it.contains("Scene 1") && it.contains("menggantung") && it.contains("while") })
        assertTrue(gates.first { it.id == "G1" }.passed)
        assertTrue(gates.first { it.id == "G2" }.passed)
        assertTrue(gates.first { it.id == "G3" }.passed)
        assertTrue(gates.first { it.id == "G4" }.passed)
        assertTrue(gates.first { it.id == "G7" }.passed)
    }
}
