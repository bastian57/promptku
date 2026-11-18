package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix11 — 3 item paparan 18 Nov 2026 (temuan lapangan fix10,
// film Borobudur): "puncak" kata tempat bukan finalitas, isHeroHurt
// tahan morfologi ("stumbling" vs "stumbles"), aturan penampilan Mode B
// berlaku juga di Penutup.
class V2Fix11Test {

    // ---------- fixture (disalin dari V2Fix10Test) ----------

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

    private fun everestFilm(openingNarration: String): List<V2ScenePrompt> {
        return listOf(
            flowPrompt(
                everestPlan(1, "Aang stands facing the Fire Emperor across the cracked ice.", openingNarration)
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
                everestPlan(6, "Aang lifts his head, mouth wide open, proclaiming, 'Peace returns to the summit,' as the storm eases and snow gently settles.", "Gunung kembali terbungkus keheningan, menandai akhir pertarungan.")
            )
        )
    }

    // ---------- fix11-1: "puncak" kata tempat ----------

    @Test
    fun peakAsPlaceWordIsNotFinality() {
        // VERBATIM narasi S1 Borobudur: "puncak" = puncak CANDI
        // (lokasi), bukan puncak pertarungan.
        val found = ArcLexicon.findFinality(
            "Di puncak candi suci, sang Avatar berdiri tegak menghadapi ancaman besar."
        )
        assertFalse(found.toString(), found.contains("puncak"))
        assertTrue(found.toString(), found.isEmpty())
    }

    @Test
    fun peakAsPlaceWordGunungIsNotFinality() {
        val found = ArcLexicon.findFinality(
            "Di puncak gunung yang tertutup salju, sang Avatar berdiri tegak."
        )
        assertFalse(found.toString(), found.contains("puncak"))
    }

    @Test
    fun peakAsClimaxStillCounts() {
        // "puncak pertempuran" = klimaks, bukan tempat — tetap kena.
        val found = ArcLexicon.findFinality(
            "Inilah puncak pertempuran terakhir, saat sang Avatar menghadapi ancaman besar."
        )
        assertTrue(found.toString(), found.contains("puncak"))
        assertTrue(found.toString(), found.contains("terakhir"))
    }

    @Test
    fun peakReachingStillCounts() {
        // Kunci fixture Bima/Kaito: "mencapai puncaknya" (diikuti
        // "saat", bukan kata tempat) tetap dihitung finalitas.
        val found = ArcLexicon.findFinality(
            "Ketegangan mencapai puncaknya saat energi dahsyat mulai dilepaskan."
        )
        assertTrue(found.toString(), found.contains("puncak"))
    }

    @Test
    fun mixedPeakPlaceAndClimaxKeepsPeak() {
        // Satu kemunculan klimaks saja cukup: "puncak" tetap dihitung.
        val found = ArcLexicon.findFinality(
            "Di puncak candi suci, puncak pertempuran semakin dekat."
        )
        assertTrue(found.toString(), found.contains("puncak"))
    }

    @Test
    fun peakPlaceWordPassesGate() {
        // Ujung-ke-ujung: narasi S1 Borobudur verbatim di Pembuka —
        // R1 dan R7-narasi TIDAK BOLEH gagal. Film lolos G7 penuh.
        val scenes = everestFilm("Di puncak candi suci, sang Avatar berdiri tegak menghadapi ancaman besar.")
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertTrue(result.failures.toString(), result.passed)
    }

    @Test
    fun peakClimaxWordStillFailsGate() {
        // KONTROL: "puncak pertempuran" di narasi Pembuka tetap gagal R1.
        val scenes = everestFilm("Inilah puncak pertempuran terakhir, saat sang Avatar menghadapi ancaman besar.")
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertFalse(result.passed)
        assertTrue(result.failures.toString(), result.failures.any { it.contains("klaim finalitas") })
    }

    // ---------- fix11-2: isHeroHurt tahan morfologi ----------

    @Test
    fun heroHurtStumblingCounts() {
        // VERBATIM inti S4 Borobudur: LLM menulis "stumbling" (bukan
        // "stumbles" yang ada di leksikon) — tetap harus ketangkep.
        assertTrue(
            ArcLexicon.isHeroHurt(
                "Fire Emperor unleashes a ferocious palm strike that lands squarely against Aang's ribs, " +
                    "sending the hero stumbling backward with mouth agape in sudden pain before his jaw " +
                    "sets hard and he drives forward again",
                "Aang"
            )
        )
    }

    @Test
    fun heroHurtReelsAndClutchingCount() {
        assertTrue(ArcLexicon.isHeroHurt("Aang reels backward, clutching his side, then his jaw sets.", "Aang"))
        assertTrue(ArcLexicon.isHeroHurt("Aang staggers back, his knee buckling under the blow.", "Aang"))
        // Bentuk lama tetap jalan.
        assertTrue(ArcLexicon.isHeroHurt("Aang stumbles backward, reeling from the hit.", "Aang"))
    }

    @Test
    fun villainStaggeringStillDoesNotCount() {
        // Penjaga klausa tetap jalan dengan kata baru: villain yang
        // sempoyongan BUKAN hero terluka, walau "staggering" kini
        // ada di leksikon.
        assertFalse(
            ArcLexicon.isHeroHurt("Ariadne lands a heavy blow — Kallistos staggering back, blood on his lip.", "Ariadne")
        )
        assertFalse(ArcLexicon.isHeroHurt("Kallistos reels backward, clutching his side.", "Ariadne"))
    }

    @Test
    fun heroHurtMorphologyFixesR9b() {
        // Ujung-ke-ujung: inti S4 Borobudur verbatim sebagai HERO_HURT
        // — R9b TIDAK BOLEH gagal lagi.
        val scenes = everestFilm("Di puncak candi suci, sang Avatar berdiri tegak menghadapi ancaman besar.")
            .map { s ->
                if (s.sceneNumber == 4) {
                    flowPrompt(
                        everestPlan(
                            4,
                            "Fire Emperor unleashes a ferocious palm strike that lands squarely against Aang's ribs, " +
                                "sending the hero stumbling backward with mouth agape in sudden pain before his jaw " +
                                "sets hard and he drives forward again",
                            "Serangan membara menurunkan Aang."
                        )
                    )
                } else s
            }
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertTrue(result.failures.toString(), result.passed)
    }

    // ---------- fix11-3: aturan penampilan Mode B di Penutup ----------

    @Test
    fun referenceRuleCoversClosingScene() {
        val ref = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "", mode = V2Mode.REFERENCE)
        assertTrue(ref.contains("REFERENCE IMAGES:"))
        assertTrue(ref.contains("NEVER describe their appearance"))
        assertTrue(ref.contains("including the closing scene"))
        assertTrue(ref.contains("a tunic rippling in the breeze"))
        val textOnly = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "", mode = V2Mode.TEXT_ONLY)
        assertFalse(textOnly.contains("REFERENCE IMAGES:"))
    }
}
