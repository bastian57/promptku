package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix10 — 5 item paparan 18 Nov 2026 (temuan lapangan fix7 +
// template video S1 Everest v3): R4b skip kata dalam tanda kutip, G3 cek
// kamera efektif (rantai), blok HERO_HURT hindari SEMUA frasa fragmen,
// aturan flurry v3 jadi baku di engine, R1-narasi longgar untuk kata
// retrospektif di Penutup.
class V2Fix10Test {

    // ---------- fixture ----------

    private fun everestBible(): StoryBible = StoryBible(
        title = "Duel on the Roof of the World",
        logline = "Aang battles the Fire Emperor on Everest.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Aang",
                listOf("Aang", "bald head with a blue arrow tattoo", "mustard-yellow tunic")
            )
        ),
        antagonistName = "Fire Emperor",
        antagonistDescEn = "A ruthless fire tyrant in golden armor.",
        worldName = "Mount Everest Summit",
        worldDescEn = "Razor-sharp icy ridges cut into the freezing stratosphere while volcanic jets of supernatural fire melt century-old glaciers into billowing white vapor.",
        lightEn = "Dazzling high-altitude sunlight splintered by blizzard mists, contrasted against blinding bursts of orange flame and cyan spiritual auras.",
        ambienceEn = "Howling Himalayan gales, concussive thunderclaps of colliding elements, and the hiss of superheated magma flash-freezing on glacial rock.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Duel on the Roof of the World", "Aang vs Fire Emperor.", 6)),
        durationMinutes = 1,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

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

    private fun everestPlan(
        num: Int,
        action: String,
        narrationId: String = "Narasi per scene.",
        effectId: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = "wind, fire", effectId = effectId,
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = "Aang"
    )

    private fun flowPrompt(plan: V2ScenePlan, flowText: String = ""): V2ScenePrompt =
        V2ScenePrompt(plan.sceneNumber, plan, flowText, "", "", "")

    private fun g3Result(bible: StoryBible, scenes: List<V2ScenePrompt>): GateResult =
        ValidationGates.runGates(bible, scenes, hasImages = false)
            .first { it.id == "G3" }

    // ---------- fix10-1: R4b skip kata dalam tanda kutip ----------

    @Test
    fun quotedDialogueWordIsNotNamedMove() {
        // VERBATIM lapangan Everest S6: 'Peace' kapital karena awal
        // dialog dalam kutip tunggal — bukan jurus bernama "peace".
        val move = ArcLexicon.extractNamedMove(
            "Aang lifts his head, mouth wide open, proclaiming, 'Peace returns to the summit,' as the storm eases and snow gently settles.",
            everestBible()
        )
        assertNull(move)
    }

    @Test
    fun quotedDoubleDialogueWordIsNotNamedMove() {
        val move = ArcLexicon.extractNamedMove(
            "Aang lifts his head, proclaiming, \"Peace returns to the summit,\" as the storm eases.",
            everestBible()
        )
        assertNull(move)
    }

    @Test
    fun quotedTransformIsNotNamedMove() {
        // Scan huruf kecil juga buta kutip: "Enter the avatar state"
        // di dalam dialog bukan jurus.
        val move = ArcLexicon.extractNamedMove(
            "Aang lifts his head, proclaiming, 'Enter the avatar state,' as the storm eases.",
            everestBible()
        )
        assertNull(move)
    }

    @Test
    fun realNamedMoveStillDetectedAfterQuote() {
        // Jurus ASLI sebelum kutip tetap kehitung; kutip tidak merusak
        // scan ("Tempest Breaker" + teriak 'Victory is mine!').
        val move = ArcLexicon.extractNamedMove(
            "Aang unleashes the Tempest Breaker, shouting 'Victory is mine!'",
            everestBible()
        )
        assertEquals("Tempest Breaker", move)
    }

    @Test
    fun possessiveStillWorksAfterDequote() {
        // Apostrof ("Emperor's") BUKAN tanda kutip — posesif tetap
        // dibersihkan jadi nama antagonis, bukan jurus.
        val move = ArcLexicon.extractNamedMove(
            "Aang drives his fist into the Fire Emperor's chest plate, ice shards bursting off the armor.",
            everestBible()
        )
        assertNull(move)
    }

    @Test
    fun lowercaseTransformAfterQuoteStillDetected() {
        // Transform ASLI setelah kutip tetap kehitung ("channels the
        // ultimate avatar state" di luar kutip).
        val move = ArcLexicon.extractNamedMove(
            "Aang whispers, 'Hold the line,' then channels the ultimate avatar state.",
            everestBible()
        )
        assertEquals("avatar state", move)
    }

    // ---------- fix10-2: G3 cek kamera efektif (rantai) ----------

    @Test
    fun g3PassesWhenCameraIsChained() {
        // VERBATIM lapangan Everest S2: efek punch_uppercut (kamera
        // "tracking shot", lensa 24mm) tapi teks "continuing whip pan"
        // — rantai override kamera efek, G3 tidak boleh flag.
        val plan = everestPlan(
            2,
            "Aang sweeps a gust of wind across the ridge.",
            effectId = "punch_uppercut"
        )
        val flowText = "Using the reference images, Aang sweeps a gust of wind across the ridge. " +
            "continuing whip pan, fast motion blur, snapping across the action from the previous shot, 24mm lens, eye level."
        val g3 = g3Result(everestBible(), listOf(flowPrompt(plan, flowText)))
        assertTrue(g3.failures.toString(), g3.passed)
    }

    @Test
    fun g3StillFailsWhenCameraMissingWithoutChain() {
        // Kontrol: tanpa "continuing", kamera efek yang hilang TETAP
        // gagal (bukan skip buta).
        val plan = everestPlan(
            2,
            "Aang sweeps a gust of wind across the ridge.",
            effectId = "punch_uppercut"
        )
        val flowText = "Using the reference images, Aang sweeps a gust of wind across the ridge. " +
            "slow dolly in, 50mm lens, eye level."
        val g3 = g3Result(everestBible(), listOf(flowPrompt(plan, flowText)))
        assertFalse(g3.passed)
        // Pesan: "tanpa lensa 24mm, kamera tracking shot, following the
        // subject" — "tanpa kamera" tidak muncul utuh (lensa disebut
        // dulu), jadi yang di-assert frasa kameranya.
        assertTrue(g3.failures.toString(), g3.failures.any { it.contains("kamera tracking shot, following the subject") })
    }

    @Test
    fun g3StillPassesWhenEffectCameraPresent() {
        // Kontrol: kamera efek muncul di teks (tanpa rantai) tetap lolos.
        val plan = everestPlan(
            2,
            "Aang sweeps a gust of wind across the ridge.",
            effectId = "punch_uppercut"
        )
        val flowText = "Using the reference images, Aang sweeps a gust of wind across the ridge. " +
            "tracking shot, following the subject, 24mm lens, eye level."
        val g3 = g3Result(everestBible(), listOf(flowPrompt(plan, flowText)))
        assertTrue(g3.failures.toString(), g3.passed)
    }

    // ---------- fix10-3: blok HERO_HURT hindari SEMUA frasa fragmen ----------

    @Test
    fun heroHurtBlockAvoidsEveryFragmentPhrase() {
        // Fragmen fight_hero_hurt: "staggers back, blood on his lip,
        // knees buckling, the world tilting — then his eyes harden and
        // he plants his feet again". Blok TIDAK BOLEH mengandung satu
        // pun frasa itu (fix7 cuma ganti "blood on his lip" — "one knee
        // buckling" masih menggema "knees buckling").
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(12), "")
        val fragmentPhrases = listOf(
            "staggers back", "blood on his lip", "knees buckling",
            "world tilting", "eyes harden", "plants his feet again"
        )
        for (phrase in fragmentPhrases) {
            assertFalse("blok HERO_HURT mengandung frasa fragmen: $phrase", prompt.contains(phrase))
        }
        // Penanda R9b tetap ada ("stumbles"/"reeling").
        assertTrue(prompt.contains("reeling"))
    }

    // ---------- fix10-4: aturan flurry v3 jadi baku ----------

    @Test
    fun exchangeBlockStartsMidExchange() {
        // Template video S1 Everest v3: mulai mid-exchange, kata kerja
        // pertama = serangan, tiap pukulan ada konsekuensi, larang
        // "snaps back", akhiri di impact dengan hero berdiri.
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("ALREADY mid-exchange"))
        assertTrue(prompt.contains("the first verb is a strike"))
        assertTrue(prompt.contains("each blow with its consequence in the same clause"))
        assertTrue(prompt.contains("NEVER write that the hero \"snaps back\""))
        assertTrue(prompt.contains("End the action on the final impact"))
        assertTrue(prompt.contains("fists raised"))
        assertFalse(prompt.contains("Aang snaps back"))
    }

    @Test
    fun fightPacingRuleHasV3Rules() {
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("ALREADY mid-motion"))
        assertTrue(prompt.contains("Every blow names its consequence in the same clause"))
        assertTrue(prompt.contains("End every fight scene on the final impact"))
        assertTrue(prompt.contains("Camera follows the fastest object"))
        assertTrue(prompt.contains("chase the fists, snap-zoom with each impact"))
    }

    @Test
    fun oneActionRuleHasMidExchangeStart() {
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("starting ALREADY mid-exchange with the first verb a strike"))
        assertTrue(prompt.contains("ending on the final impact with the hero on his feet"))
    }

    // ---------- fix10-5: R1-narasi retrospektif di Penutup ----------

    private fun everestFilm(closingNarration: String): List<V2ScenePrompt> {
        val scenes = listOf(
            flowPrompt(
                everestPlan(1, "Aang stands facing the Fire Emperor across the cracked ice.", "Di ketinggian beku, dua kekuatan berhadapan.")
            ),
            flowPrompt(
                everestPlan(2, "Aang sweeps a gust of wind across the ridge.", "Angin dan api bersaing di ketinggian.")
            ),
            flowPrompt(
                everestPlan(3, "Aang drives a massive wind-fist into the Fire Emperor's chest.", "Detik-detik terasa melambat.")
            ),
            flowPrompt(
                everestPlan(4, "Fire Emperor lands a clean hit — Aang stumbles backward, reeling from the hit, clutching his side, then his jaw sets and he rises.", "Serangan membara menurunkan Aang.")
            ),
            flowPrompt(
                everestPlan(5, "Aang triggers the ultimate Avatar State, his tattoos blazing cyan as he unleashes a decisive elemental sphere.", "Saat penentuan tiba.")
            ),
            flowPrompt(
                everestPlan(6, "Aang lifts his head, mouth wide open, proclaiming, 'Peace returns to the summit,' as the storm eases and snow gently settles.", closingNarration)
            )
        )
        return scenes
    }

    @Test
    fun closingNarrationRetrospectiveAkhirPasses() {
        // Frasa VERBATIM narasi S6 Everest ("menandai akhir
        // pertarungan") di Penutup: retrospektif, bukan klaim klimaks
        // prematur — R1 dan R7-narasi TIDAK BOLEH gagal. Film lolos G7
        // penuh (S5 Klimaks skor 8 vs S6 Penutup skor 2).
        val scenes = everestFilm("Gunung kembali terbungkus keheningan, menandai akhir pertarungan.")
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertTrue(result.failures.toString(), result.passed)
    }

    @Test
    fun closingNarrationPeakWordStillFails() {
        // KONTROL beda kata: "memuncak" di narasi Penutup BUKAN
        // retrospektif — itu klaim klimaks sedang terjadi, tetap gagal
        // R1 (narasi Bima S18 verbatim). Filter ini bedah kata, bukan
        // bebasin Penutup buta.
        val scenes = everestFilm("Ketegangan semakin memuncak di atas langit yang terkoyak badai.")
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertFalse(result.passed)
        assertTrue(result.failures.toString(), result.failures.any { it.contains("klaim finalitas") })
    }

    @Test
    fun closingActionFinalityStillFails() {
        // KONTROL beda sumber: finalitas di AKSI Penutup tetap gagal —
        // pengecualian cuma untuk NARASI retrospektif, bukan aksi.
        val scenes = everestFilm("Gunung kembali terbungkus keheningan, menandai akhir pertarungan.")
            .map { s ->
                if (s.sceneNumber == 6) {
                    flowPrompt(
                        everestPlan(6, "Aang raises his fists as the final blow lands.", "Gunung kembali terbungkus keheningan, menandai akhir pertarungan.")
                    )
                } else s
            }
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertFalse(result.passed)
        assertTrue(result.failures.toString(), result.failures.any { it.contains("klaim finalitas") })
    }
}
