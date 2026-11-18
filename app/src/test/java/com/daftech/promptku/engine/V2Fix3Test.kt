package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.2 fix3 — reduplikasi "puncak-puncak" lolos dari filter kata
// tempat fix11 (temuan lapangan Gotres 19 Nov 00:18, narasi S1 verbatim:
// "Di antara puncak-puncak gunung ..." dituduh klaim klimaks R1 +
// R7-narasi). Akar: tokensOf membelah tanda hubung, jadi token sesudah
// "puncak" pertama adalah "puncak" kedua, bukan kata tempat.
class V2Fix3Test {

    // ---------- fixture (disalin dari V2Fix11Test) ----------

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

    // ---------- fix3: "puncak-puncak" + kata tempat ----------

    @Test
    fun reduplicatedPeakBeforePlaceWordIsNotFinality() {
        // VERBATIM narasi S1 film Gotres 19 Nov 00:18: "puncak-puncak
        // gunung" = puncak-puncak GUNUNG (lokasi jamak), bukan puncak
        // pertarungan.
        val found = ArcLexicon.findFinality(
            "Di antara puncak-puncak gunung yang tertutup salju abadi, angin kencang berhembus membawa ketegangan."
        )
        assertFalse(found.toString(), found.contains("puncak"))
        assertTrue(found.toString(), found.isEmpty())
    }

    @Test
    fun reduplicatedPeakBeforeNonPlaceWordStillCounts() {
        // "puncak-puncak pertempuran" = klimaks jamak — tetap kena.
        val found = ArcLexicon.findFinality(
            "Inilah puncak-puncak pertempuran yang menentukan segalanya."
        )
        assertTrue(found.toString(), found.contains("puncak"))
    }

    @Test
    fun reduplicatedPeakWithSuffixStillCounts() {
        // "puncak-puncaknya" + "saat" (bukan kata tempat) — tetap kena.
        // Menguji lompatan reduplikasi juga bekerja untuk bentuk bersufiks.
        val found = ArcLexicon.findFinality(
            "Ketegangan mencapai puncak-puncaknya saat energi dahsyat mulai dilepaskan."
        )
        assertTrue(found.toString(), found.contains("puncak"))
    }

    @Test
    fun singlePeakBeforePlaceWordStillExempt() {
        // Perilaku fix11 yang sudah benar — jangan rusak.
        val found = ArcLexicon.findFinality(
            "Di puncak gunung yang tertutup salju, sang Avatar berdiri tegak."
        )
        assertFalse(found.toString(), found.contains("puncak"))
    }

    @Test
    fun reduplicatedPeakPassesGate() {
        // Ujung-ke-ujung: narasi S1 Gotres verbatim di Pembuka — R1 dan
        // R7-narasi TIDAK BOLEH gagal. Film lolos G7 penuh.
        val scenes = everestFilm("Di antara puncak-puncak gunung yang tertutup salju abadi, angin kencang berhembus membawa ketegangan.")
        val result = ValidationGates.runArcGate(everestBible(), scenes)
        assertTrue(result.failures.toString(), result.passed)
    }
}
