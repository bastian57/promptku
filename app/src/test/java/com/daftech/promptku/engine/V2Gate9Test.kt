package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix8 v2 — gerbang R9 (disiplin slow-mo + hero terluka), bagian
// dari G7. Aturan: kata slow-mo di action_en HANYA di scene IMPACT +
// finisher; scene HERO_HURT wajib menunjukkan hero terluka. Fragmen
// render TIDAK digate (sudah dijamin kode) — yang digate aksi LLM.
class V2Gate9Test {

    private fun bible(genre: Genre = Genre.ACTION): StoryBible = StoryBible(
        title = "Fall of Olympus",
        logline = "Ariadne fights Kallistos in the Aegean ruins.",
        genre = genre,
        characters = listOf(V2Character("Ariadne", listOf("scarred shield", "stormy eyes"))),
        antagonistName = "Kallistos",
        antagonistDescEn = "a ruthless commander",
        worldName = "Aegean Ruins",
        worldDescEn = "The shattered marble streets of a once-glorious Greek citadel.",
        lightEn = "Harsh sunlight",
        ambienceEn = "Echoing rumble",
        beats = listOf(
            V2Beat(0, "Pembuka", "the threat arrives"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the decisive clash"),
            V2Beat(4, "Penutup", "silence returns")
        ),
        episodes = listOf(V2Episode(1, "Fall of Olympus", "Ariadne vs Kallistos.", 18)),
        durationMinutes = 3
    )

    private fun plan(num: Int, action: String): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action, cameraMove = "tracking shot",
        lensMm = 35, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "",
        audioEn = "wind, steel", effectId = "",
        wantEn = "win", obstacleEn = "Kallistos", transitionEn = "",
        narrationId = "", dialogueSpeaker = ""
    )

    private fun scenePrompt(p: V2ScenePlan): V2ScenePrompt =
        V2ScenePrompt(p.sceneNumber, p, "x", "x", "x", "x")

    /** Film baseline yang MEMENUHI R9: IMPACT S10 slow-mo, HERO_HURT S12
     *  hero terluka, finisher S13, sisanya rentetan cepat tanpa slow-mo. */
    private fun baselineFilm(): List<V2ScenePrompt> {
        val actions = mutableMapOf<Int, String>()
        for (n in 1..18) {
            actions[n] = "Ariadne trades a rapid flurry with Kallistos — jab, cross, spinning kick, no pause."
        }
        actions[2] = "Ariadne circles slowly, eyes locked on Kallistos, dust settling between them."
        actions[5] = "Ariadne holds her ground, chest heaving, eyes never leaving Kallistos."
        actions[9] = "Ariadne circles slowly, eyes locked on Kallistos, both breathing hard."
        actions[10] = "Ariadne lands a heavy blow on Kallistos — time ramps into slow motion for a heartbeat, the camera pushing in close on his face, eyes wide with pain."
        actions[12] = "Kallistos lands a clean hit — Ariadne staggers back, blood on his lip, knees buckling, then his eyes harden."
        actions[13] = "Ariadne rises into the air, fist glowing — the final blow lands, Kallistos collapses."
        actions[16] = "Ariadne stands tall, surveying the quiet ruins."
        actions[17] = "Ariadne walks through the settling dust."
        actions[18] = "Ariadne stands tall as the wind calms over the ruins."
        return (1..18).map { scenePrompt(plan(it, actions[it]!!)) }
    }

    private fun r9Failures(scenes: List<V2ScenePrompt>, genre: Genre = Genre.ACTION): List<String> =
        ValidationGates.runArcGate(bible(genre), scenes).failures
            .filter { it.contains("slow-mo") || it.contains("HERO HURT") }

    @Test
    fun baselineFilmPassesR9() {
        val failures = r9Failures(baselineFilm())
        assertTrue(failures.toString(), failures.isEmpty())
    }

    @Test
    fun slowMotionInExchangeSceneFails() {
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 7) scenePrompt(plan(7, "Ariadne punches Kallistos — the impact freezes for a heartbeat, sweat sprays in slow motion."))
            else it
        }
        val failures = r9Failures(scenes)
        assertTrue(failures.toString(), failures.any { it.contains("Scene 7") && it.contains("slow-mo") })
    }

    @Test
    fun slowMotionInImpactAndFinisherIsAllowed() {
        // Baseline SUDAH punya slow-mo di S10 (IMPACT) — tambah slow-mo
        // eksplisit di finisher S13, tetap lolos R9.
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 13) scenePrompt(plan(13, "Ariadne rises into the air — the final blow lands in slow motion, Kallistos collapses."))
            else it
        }
        assertTrue(r9Failures(scenes).toString(), r9Failures(scenes).isEmpty())
    }

    @Test
    fun heroHurtMissingFails() {
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 12) scenePrompt(plan(12, "Ariadne trades a rapid flurry with Kallistos — jab, cross, spinning kick, no pause."))
            else it
        }
        val failures = r9Failures(scenes)
        assertTrue(failures.toString(), failures.any { it.contains("Scene 12") && it.contains("HERO HURT") })
    }

    @Test
    fun villainStaggeringDoesNotCountAsHeroHurt() {
        // "Kallistos staggers" = villain yang kena pukul (IMPACT), bukan
        // hero terluka — gerbang harus tetap gagal di S12.
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 12) scenePrompt(plan(12, "Ariadne lands a heavy blow — Kallistos staggers back, blood on his lip, knees buckling."))
            else it
        }
        val failures = r9Failures(scenes)
        assertTrue(failures.toString(), failures.any { it.contains("Scene 12") && it.contains("HERO HURT") })
    }

    @Test
    fun heroHurtRequiresBibleHeroName() {
        // Bible tes ini hero-nya "Ariadne" (bukan "Ariadne Stormblade") —
        // "Stormblade" saja tidak cocok, jadi tetap gagal. Ini mengunci
        // bahwa detektor memakai nama bible, bukan tebakan.
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 12) scenePrompt(plan(12, "Kallistos lands a clean hit — Stormblade staggers back, blood on his lip."))
            else it
        }
        val failures = r9Failures(scenes)
        assertTrue(failures.toString(), failures.any { it.contains("Scene 12") && it.contains("HERO HURT") })
    }

    @Test
    fun freezingInFearIsNotSlowMotion() {
        val scenes = baselineFilm().map {
            if (it.sceneNumber == 7) scenePrompt(plan(7, "Ariadne freezes in fear, eyes wide, breath held as Kallistos approaches."))
            else it
        }
        assertTrue(r9Failures(scenes).toString(), r9Failures(scenes).isEmpty())
    }

    @Test
    fun nonActionFilmIsExempt() {
        // Genre DRAMA + ide non-fight: slow-mo di S7 dan S12 tanpa hero
        // terluka tetap lolos R9 (tidak ada peran fight).
        val scenes = baselineFilm().map {
            when (it.sceneNumber) {
                7 -> scenePrompt(plan(7, "Ariadne punches Kallistos — sweat sprays in slow motion."))
                12 -> scenePrompt(plan(12, "Ariadne trades a rapid flurry with Kallistos."))
                else -> it
            }
        }
        assertTrue(r9Failures(scenes, Genre.DRAMA).toString(), r9Failures(scenes, Genre.DRAMA).isEmpty())
    }

    @Test
    fun slowMoDetector() {
        assertTrue(ArcLexicon.isSlowMoMoment("the impact freezes for a heartbeat"))
        assertTrue(ArcLexicon.isSlowMoMoment("sweat sprays in slow motion"))
        assertTrue(ArcLexicon.isSlowMoMoment("time slows as the blow lands"))
        assertTrue(ArcLexicon.isSlowMoMoment("everything slows to a crawl"))
        assertFalse(ArcLexicon.isSlowMoMoment("he freezes in fear"))
        assertFalse(ArcLexicon.isSlowMoMoment("she circles slowly, breathing hard"))
        assertFalse(ArcLexicon.isSlowMoMoment("the world holds still around him"))
    }

    @Test
    fun heroHurtDetector() {
        assertTrue(ArcLexicon.isHeroHurt("Ariadne staggers back, blood on his lip.", "Ariadne"))
        assertTrue(ArcLexicon.isHeroHurt("Kallistos lands a clean hit — Ariadne drops to one knee, clutching his ribs.", "Ariadne"))
        assertFalse(ArcLexicon.isHeroHurt("Kallistos staggers back, blood on his lip.", "Ariadne"))
        assertFalse(ArcLexicon.isHeroHurt("Ariadne trades a rapid flurry with Kallistos.", "Ariadne"))
        // "the hero" sebagai subjek juga dihitung (aksi tanpa nama).
        assertTrue(ArcLexicon.isHeroHurt("The hero staggers back, blood on his lip.", ""))
    }
}
