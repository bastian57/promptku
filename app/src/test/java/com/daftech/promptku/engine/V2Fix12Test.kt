package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix12 — 2 item paparan 18 Nov 2026 (audit lapangan Peak of
// Destiny + master prompt Wushu yang Putra pakai di Flow):
// 1. trim Mode B kenal batas klausa "while"/"as" + marker beat "[00:"
//    (inti S3 Peak of Destiny menggantung "...while the tyrant's face").
// 2. gramatika Wushu: micro-beat bertimecode (Mode B), never-stop-moving,
//    defense-as-movement, aerial continuity, kamera projectile POV.
class V2Fix12Test {

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

    private fun fieldS3Plan(action: String, audioEn: String): V2ScenePlan = V2ScenePlan(
        sceneNumber = 3, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "close-up", angle = "eye",
        dialogueId = "", dialogueEn = "",
        audioEn = audioEn,
        effectId = "fight_impact",
        wantEn = "win", obstacleEn = "Fire Emperor", transitionEn = "",
        narrationId = "Narasi per scene.", dialogueSpeaker = "Aang"
    )

    // Inti S3 Peak of Destiny VERBATIM (25 kata) + lanjutan yang ditulis
    // LLM (diduga — export cuma menyimpan hasil trim): total 42 kata.
    // Dengan audio 5 kata lebih panjang dari fixture fix7, anggaran kata
    // memaksa keep = 25 — persis titik potong lapangan yang menggantung.
    private val peakS3LongCore =
        "Aang drives a heavy palm strike straight into the Fire Emperor's chest, shattering molten golden armor shards into the freezing air while the tyrant's face twists in agony, his golden crown cracking under the force, molten shards scattering across the frozen ground"

    private val fieldS3Audio =
        "All sound drops to a low heartbeat just before the heavy blow, then a loud impact crack echoes through the roaring flames through the frozen mountain air."

    // ---------- fix12-1: batas klausa trim ----------

    @Test
    fun trimBacksOffBeforeWhileClause() {
        // Kasus lapangan verbatim: batas ", " terakhir ("...chest,")
        // 13 kata ke belakang — " while " (diikuti "the") lebih dekat.
        val text =
            "Aang drives a heavy palm strike straight into the Fire Emperor's chest, shattering molten golden armor shards into the freezing air while the tyrant's face"
        val at = ModeRender.clauseBoundaryAt(text)
        assertEquals(text.lastIndexOf(" while "), at)
        assertTrue(text.substring(0, at).endsWith("air"))
    }

    @Test
    fun trimIgnoresComparisonAs() {
        // "as fast as lightning": dua " as " — keduanya diikuti kata
        // yang BUKAN awal klausa ("fast", "lightning") — bukan batas.
        val text = "Aang moves as fast as lightning, striking the Emperor's guard"
        val at = ModeRender.clauseBoundaryAt(text)
        assertEquals(text.lastIndexOf(", "), at)
        // Frasa komparasi utuh SEBELUM batas — trim yang potong di
        // batas ini menyimpan "as fast as lightning" lengkap, bukan
        // ninggalin "as fast" yang menggantung.
        assertTrue(text.substring(0, at).contains("as fast as lightning"))
    }

    @Test
    fun trimIgnoresDurationWhile() {
        // "wait a while longer": " while " diikuti "longer" — frasa
        // durasi, bukan klausa bawahan — bukan batas.
        val text = "Aang waits a while longer, gathering his breath, then strikes"
        val at = ModeRender.clauseBoundaryAt(text)
        assertEquals(text.lastIndexOf(", "), at)
    }

    @Test
    fun trimBacksOffBeforeBeatMarker() {
        // Marker beat "[00:" = batas: potong SEBELUM marker, jangan
        // ninggalin "[00:06-00:10]" tanpa isi.
        val text =
            "[00:00-00:03] Aang launches a spinning kick, cracking the ice [00:03-00:06] he drives a palm thrust, sending snow [00:06-00:10] the Emperor answers"
        val at = ModeRender.clauseBoundaryAt(text)
        assertEquals(text.lastIndexOf("[00:06"), at)
        assertTrue(text.substring(0, at).trimEnd().endsWith("snow"))
    }

    @Test
    fun modeBTrimCutsBeforeWhileNotMidPhrase() {
        // Replay Peak of Destiny S3: trim lama (fix7) memotong keras di
        // "face" (batas ", " 13 kata ke belakang, lewat guard 8 kata);
        // trim fix12 mundur ke " while " — inti berakhir kalimat utuh.
        // v2.2 — titik potong pindah dengan cap Flow 300 (keep ~145 kata,
        // bukan ~25): batas " while " digeser ke dekat titik potong baru
        // dengan filler 120 kata DI DEPAN inti verbatim, supaya guard 8
        // kata tetap menguji back-off yang sama (bukan potongan ekor).
        val bible = fieldBible()
        val fillerA = "Aang presses forward through the storm, fists blazing in the wind, driving the Emperor back. "
        val plan = fieldS3Plan(fillerA.repeat(8) + peakS3LongCore, fieldS3Audio)
        val shot = ModeRender.effectiveShot(plan)
        val rendered = ModeRender.renderReference(bible, plan, shot, TargetEngine.FLOW, 3)
        assertTrue(rendered.contains("into the freezing air, then the blow lands"))
        assertFalse(rendered.contains("while the tyrant's face"))
        assertFalse(rendered.contains("the the"))
        // v2.2 — cap Flow 180 -> 300 (terukur 296 kata).
        assertTrue(MythFilter.countWords(rendered) <= 300)
    }

    // ---------- fix12-2: gramatika Wushu ----------

    @Test
    fun exchangeBlockModeBHasMicroBeats() {
        // Mode B (REFERENCE): EXCHANGE ditulis sebagai micro-beat
        // bertimecode sepanjang 10 detik + never-stop-moving + aerial.
        val prompt = DirectorPrompts.prompterUser(
            bima18Bible(), listOf(1), "", mode = V2Mode.REFERENCE
        )
        assertTrue(prompt.contains("timestamped micro-beats"))
        assertTrue(prompt.contains("[00:00-00:03]"))
        assertTrue(prompt.contains("[00:06-00:10]"))
        assertTrue(prompt.contains("NEVER stop moving"))
        assertTrue(prompt.contains("every defense is a movement"))
        assertTrue(prompt.contains("stays airborne until the beat ends"))
    }

    @Test
    fun exchangeBlockModeAHasNoMicroBeats() {
        // Mode A (TEXT_ONLY): TANPA timecode di actionEn — renderer
        // sudah menimestamp segmennya sendiri ([00:00-00:04] /
        // [00:04-00:08]); timecode di action bakal dobel. Aturan
        // never-stop-moving + aerial tetap berlaku di dua mode.
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertFalse(prompt.contains("timestamped micro-beats"))
        assertFalse(prompt.contains("[00:00-00:03]"))
        assertTrue(prompt.contains("NEVER stop moving"))
        assertTrue(prompt.contains("stays airborne until the beat ends"))
    }

    @Test
    fun fightPacingRuleHasNeverStopMoving() {
        // Aturan global fight: larangan bertahan statis + rantai
        // defense-as-movement + aerial continuity (kondisional).
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("NEVER stop moving"))
        assertTrue(prompt.contains("no standing block, no static cast"))
        assertTrue(prompt.contains("every defense is a movement"))
        assertTrue(prompt.contains("aerial movement, parry again"))
        assertTrue(prompt.contains("stays airborne until the beat ends"))
        assertTrue(prompt.contains("he recovers in the air"))
    }

    @Test
    fun oneActionRuleHasDefenseAsMovement() {
        // Aturan ONE-action: "not three separate beats" dihapus karena
        // micro-beat Mode B MEMANG 2-3 beat — yang dilarang itu tiga
        // SCENE terpisah. Plus defense-as-movement.
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("every defense a movement"))
        assertTrue(prompt.contains("one flurry, not three separate scenes"))
        assertFalse(prompt.contains("not three separate beats"))
    }

    @Test
    fun projectilePovCameraParses() {
        // Opsi kamera baru: "projectile pov" dikenali dari nama opsi,
        // dari teks bebas ("following the fireball"), dan dari kapital.
        assertEquals(
            CameraMove.PROJECTILE_POV,
            ModeRender.cameraMoveFromString("projectile pov")
        )
        assertEquals(
            CameraMove.PROJECTILE_POV,
            ModeRender.cameraMoveFromString("projectile POV, following the fireball")
        )
        assertEquals(
            CameraMove.PROJECTILE_POV,
            ModeRender.cameraMoveFromString("Projectile POV")
        )
        assertEquals(
            "projectile POV, following the projectile",
            CameraMove.PROJECTILE_POV.phraseEn
        )
    }

    @Test
    fun cameraMoveOptionsIncludeProjectilePov() {
        // Opsi camera_move di JSON prompter ikut menawarkan projectile pov.
        val prompt = DirectorPrompts.prompterUser(bima18Bible(), listOf(1), "")
        assertTrue(prompt.contains("projectile pov"))
    }
}
