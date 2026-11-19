package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.2 fix13 — 4 item lapangan (pitch 18 Nov 20:46, gas 19 Nov 16:55).
// 1. extractNamedMove pecah kalimat JUGA di marker beat "[00:xx-xx]":
//    kata sesudah marker = awal kalimat (Clash S6: "Gotros" ke-flag).
// 2. G2 cek scene terakhir HANYA kalau film lengkap (Clash S7 "kicks"
//    di film 7/18 = flag palsu).
// 3. cleanField buang JUGA titik di awal (aksi ". Gotres launches...").
// 4. SFX: kalimat dunia dibuang dari audioEn di render (Clash S4/S7,
//    Jubah S5/S6, Ujian S1/S3 — kejadian ke-4 di lapangan).
class V2Fix13Test {

    private fun clashBible(): StoryBible = StoryBible(
        title = "Clash Above the Clouds",
        logline = "Aang vs Gotres above the clouds.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character("Aang", listOf("bald head", "blue arrow tattoo", "orange robes"))
        ),
        antagonistName = "Gotres",
        antagonistDescEn = "The golden tyrant who rules the sky realm.",
        worldName = "Sky Realm",
        worldDescEn = "Endless towering cumulus clouds roll beneath a blazing sunset sky as strong winds whip across floating ancient stone platforms.",
        lightEn = "Golden hour sunlight casting sharp shadows through billowing white clouds.",
        ambienceEn = "Howling high-altitude winds and the thunderous shockwaves of colliding fists.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the duel begins"),
            V2Beat(1, "Pemicu", "the tyrant provokes"),
            V2Beat(2, "Eskalasi", "the exchange grows"),
            V2Beat(3, "Klimaks", "the finisher"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Clash Above the Clouds", "Aang vs Gotres above the clouds.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16",
        sourceIdea = "Aang vs Gotres above the clouds."
    )

    // Aksi inti VERBATIM export Clash 18 Nov (rendered minus fragmen efek).
    // S6 = 3 mikro-beat fix12; S7 = aksi LLM (fragmen finisher ditempel
    // render lewat join ", then " — bukan bagian actionEn).
    private val clashS6Core = "[00:00-00:03] Gotros thrusts a low sweep, tearing a gust of vapor; " +
        "[00:03-00:06] Aang vaults upward, dodging the vapor, driving a spiraling palm that shatters a cloud ribbon; " +
        "[00:06-00:10] Gotros flips, hair streaming, Aang answers with a thunderous knee that sends shockwaves rippling, landing firm, fists raised"
    private val clashS7Core = ". Gotres unleashes a relentless flurry of axe kicks shattering floating gravel, " +
        "and Aang parries each blow with rotating forearm blocks while driving a sweeping elbow strike into Gotres's ribs"
    // SFX mentah VERBATIM Clash S7: kalimat dunia + ".." + SFX asli.
    private val clashS7Audio = "Endless towering cumulus clouds roll beneath a blazing sunset sky as strong winds whip across floating ancient stone platforms.. Heavy bone impacts and howling high-altitude gales."

    // Pengisi tenang untuk scene lain — kosakata sengaja bervariasi supaya
    // tidak ada pasangan yang kena Jaccard >= 0.5 di G2.
    private val fillers = mapOf(
        1 to "Aang stands on the floating platform, watching clouds drift below.",
        2 to "Gotres circles slowly, eyes fixed on the horizon.",
        3 to "Wind whistles across the stone, carrying dust and petals.",
        4 to "Aang breathes deeply, closing his eyes for a moment.",
        5 to "Gotres adjusts his golden armor, glinting in the sunset.",
        6 to "Clouds part above the summit, revealing a pale blue sky.",
        7 to "Gotres pours tea into a clay cup, steam curling upward.",
        8 to "Aang steps forward, raising one open palm in greeting.",
        9 to "Gotres laughs softly, shaking his head at the challenge.",
        10 to "Rain begins to fall gently over the ancient stone platforms.",
        11 to "Aang sits cross-legged, meditating as thunder rolls far away.",
        12 to "Gotres paces along the edge, arms folded behind his back.",
        13 to "A flock of birds crosses the orange sky in silence.",
        14 to "Aang opens a worn scroll, reading the faded ink by torchlight.",
        15 to "Gotres kneels beside a small shrine, placing a single flower.",
        16 to "Mist rises from the valley, softening the mountain outlines.",
        17 to "Aang ties his belt, then looks up toward the highest peak."
    )

    private fun clashPlan(num: Int, action: String, audioEn: String = "wind roar"): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "static", lensMm = 35, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = audioEn, effectId = "",
        wantEn = "win", obstacleEn = "Gotres", transitionEn = "",
        narrationId = "The duel continues.", dialogueSpeaker = "Aang"
    )

    private fun clashPrompt(num: Int, action: String, audioEn: String = "wind roar"): V2ScenePrompt {
        val plan = clashPlan(num, action, audioEn)
        val flow = "in Sky Realm Aang $action."
        return V2ScenePrompt(num, plan, flow, flow, flow, flow)
    }

    @Test
    fun timecodeMarkerShieldsWordAfterIt() {
        // Lapangan Clash S6: "Gotros" (typo LLM untuk "Gotres") sesudah
        // "[00:00-00:03]" dan "[00:06-00:10]" ke-flag sebagai jurus
        // bernama (R4b S6 + R6 palsu 2 vs 0). Sesudah fix13 item 1: null.
        assertNull(ArcLexicon.extractNamedMove(clashS6Core, clashBible()))
    }

    @Test
    fun namedMoveAfterMarkerStillDetectedWhenMidSentence() {
        // Kontrol: fix hanya melindungi kata TEPAT sesudah marker — jurus
        // di tengah klausa mikro-beat tetap kena.
        assertEquals(
            "Tempest Breaker",
            ArcLexicon.extractNamedMove("[00:00-00:03] Aang unleashes the Tempest Breaker", clashBible())
        )
    }

    @Test
    fun namedMoveWithoutMarkerStillDetected() {
        // Kontrol: tanpa marker, typo yang sama tetap terdeteksi (fix
        // bukan penekanan umum atas "Gotros").
        assertEquals(
            "Gotros",
            ArcLexicon.extractNamedMove("Aang dodges as Gotros thrusts a low sweep, tearing a gust of vapor", clashBible())
        )
    }

    @Test
    fun g2SilentWhenFilmTruncated() {
        // Lapangan Clash: film 7/18, S7 "kicks" ke-flag G2 "(Penutup)"
        // padahal scene 7 bukan Penutup. Sesudah fix13 item 2: G2 diam.
        val scenes = (1..7).map { n ->
            if (n == 7) clashPrompt(7, clashS7Core) else clashPrompt(n, fillers.getValue(n))
        }
        val g2 = ValidationGates.runGates(clashBible(), scenes, false).first { it.id == "G2" }
        assertTrue(g2.failures.toString(), g2.passed)
        assertFalse(g2.failures.any { it.contains("masih ada aksi") })
    }

    @Test
    fun g2StillFailsWhenFilmComplete() {
        // Kontrol: aksi yang SAMA sebagai scene 18 film lengkap tetap kena.
        val scenes = (1..18).map { n ->
            if (n == 18) clashPrompt(18, clashS7Core) else clashPrompt(n, fillers.getValue(n))
        }
        val g2 = ValidationGates.runGates(clashBible(), scenes, false).first { it.id == "G2" }
        assertFalse(g2.passed)
        assertTrue(g2.failures.any { it.contains("masih ada aksi \"kicks\"") })
    }

    @Test
    fun cleanFieldTrimsLeadingDot() {
        // Lapangan Clash S4/S7: aksi inti diawali ". ".
        assertEquals(
            "Gotres launches a blinding palm thrust shattering stone debris into dust",
            ModeRender.cleanField(". Gotres launches a blinding palm thrust shattering stone debris into dust")
        )
        assertEquals("Gotres launches", ModeRender.cleanField("... Gotres launches."))
        assertEquals("Normal text", ModeRender.cleanField("Normal text."))
        assertEquals("", ModeRender.cleanField("..."))
    }

    @Test
    fun audioWorldLeakStrippedAtRender() {
        val bible = clashBible()
        // (a) Pola Clash S7: dunia + ".." + SFX asli — dunianya hilang;
        //     sisa ".." dibersihkan cleanField (item 3) di jalur render.
        val sanitized = ModeRender.sanitizeAudio(clashS7Audio, bible)
        assertFalse(sanitized.contains("Endless towering"))
        // cleanField (item 3) buang sisa ".." di depan DAN "." di ujung.
        assertEquals(
            "Heavy bone impacts and howling high-altitude gales",
            ModeRender.cleanField(sanitized)
        )
        // (b) Pola Jubah S5/S6 + Ujian S1/S3: dunia + ambience.
        val jubahAudio = bible.worldDescEn + " " + bible.ambienceEn
        assertEquals(bible.ambienceEn, ModeRender.sanitizeAudio(jubahAudio, bible))
        // (c) SFX bersih tanpa dunia: tidak disentuh.
        assertEquals("Heavy bone impacts.", ModeRender.sanitizeAudio("Heavy bone impacts.", bible))
        // (d) Render Mode B: SFX hasil render bersih dari dunia, sementara
        //     dunia tetap ada di tempatnya (Setting).
        val plan = clashPlan(7, clashS7Core, clashS7Audio)
        val rendered = ModeRender.renderAll(bible, listOf(plan), V2Mode.REFERENCE, isFight = false)
        val flow = rendered.first().flowText
        assertTrue(flow, flow.contains("SFX: Heavy bone impacts and howling high-altitude gales."))
        assertFalse(flow, flow.contains("platforms.. Heavy"))
        assertTrue(flow, flow.contains("Setting: Endless towering cumulus clouds"))
        // (e) Render Mode A (jalur shotInputFor): Audio juga bersih.
        val renderedA = ModeRender.renderAll(bible, listOf(plan), V2Mode.TEXT_ONLY, isFight = false)
        val flowA = renderedA.first().flowText
        assertTrue(flowA, flowA.contains("Audio: Heavy bone impacts and howling high-altitude gales."))
        assertFalse(flowA, flowA.contains("platforms.. Heavy"))
    }
}
