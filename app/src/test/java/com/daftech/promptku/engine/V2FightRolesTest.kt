package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix8 v2 — FightRole: peran fight per scene dari BeatMap.
// Tabel kanonik dihitung tangan (sama seperti V2BeatMapTest): 18/5 ->
// IMPACT S10 (tengah Eskalasi), HERO_HURT S12 (akhir Eskalasi),
// FINISHER S13 (awal Klimaks), TENSION S2/S5/S9, EXCHANGE sisanya,
// Penutup tanpa peran. Peran ini yang diprompt ke LLM DAN dipakai gerbang
// R9 — satu peta yang sama di kedua ujung.
class V2FightRolesTest {

    private fun beats5(): List<V2Beat> = listOf(
        V2Beat(0, "Pembuka", "kenalan dunia dan ancaman"),
        V2Beat(1, "Pemicu", "konfrontasi pertama"),
        V2Beat(2, "Eskalasi", "pertarungan membesar"),
        V2Beat(3, "Klimaks", "benturan penentu"),
        V2Beat(4, "Penutup", "dunia kembali tenang")
    )

    private fun beats8(): List<V2Beat> = listOf(
        V2Beat(0, "Pembuka", ""),
        V2Beat(1, "Pemicu", ""),
        V2Beat(2, "Konflik", ""),
        V2Beat(3, "Eskalasi", ""),
        V2Beat(4, "Titik Balik", ""),
        V2Beat(5, "Klimaks", ""),
        V2Beat(6, "Penutup", ""),
        V2Beat(7, "Epilog", "")
    )

    private fun bible(genre: Genre = Genre.ACTION, idea: String = ""): StoryBible = StoryBible(
        title = "Ariadne vs Kallistos",
        logline = "Ariadne fights Kallistos in the Aegean ruins.",
        genre = genre,
        characters = listOf(V2Character("Ariadne", listOf("scarred shield", "stormy eyes"))),
        antagonistName = "Kallistos",
        antagonistDescEn = "a ruthless commander",
        worldName = "Aegean Ruins",
        worldDescEn = "The shattered marble streets of a once-glorious Greek citadel.",
        lightEn = "Harsh sunlight",
        ambienceEn = "Echoing rumble",
        beats = beats5(),
        episodes = listOf(V2Episode(1, "Fall of Olympus", "Ariadne vs Kallistos.", 18)),
        durationMinutes = 3,
        sourceIdea = idea
    )

    private fun roles(total: Int, beats: List<V2Beat>): Map<Int, FightRole> {
        val map = BeatMap.build(total, beats)
        return BeatMap.fightRoles(map, beats, true)
    }

    @Test
    fun canonicalEighteenScenesFiveBeats() {
        val r = roles(18, beats5())
        assertEquals(FightRole.IMPACT, r[10])
        assertEquals(FightRole.HERO_HURT, r[12])
        assertEquals(FightRole.FINISHER, r[13])
        assertEquals(setOf(2, 5, 9), r.filterValues { it == FightRole.TENSION }.keys)
        assertEquals(setOf(1, 3, 4, 6, 7, 8, 11, 14, 15), r.filterValues { it == FightRole.EXCHANGE }.keys)
        // Penutup (16-18) tanpa peran; total 15 scene berperan.
        assertTrue(r.keys.none { it in 16..18 })
        assertEquals(15, r.size)
    }

    @Test
    fun thirtyScenesFiveBeats() {
        val r = roles(30, beats5())
        assertEquals(FightRole.IMPACT, r[16])
        assertEquals(FightRole.HERO_HURT, r[20])
        assertEquals(FightRole.FINISHER, r[21])
        assertEquals(setOf(2, 7, 13), r.filterValues { it == FightRole.TENSION }.keys)
        assertTrue(r.keys.none { it in 26..30 })
    }

    @Test
    fun sixScenesFiveBeats() {
        val r = roles(6, beats5())
        assertEquals(FightRole.IMPACT, r[3])
        assertEquals(FightRole.HERO_HURT, r[4])
        assertEquals(FightRole.FINISHER, r[5])
        assertTrue(r.values.none { it == FightRole.TENSION })
        assertEquals(setOf(1, 2), r.filterValues { it == FightRole.EXCHANGE }.keys)
    }

    @Test
    fun twelveScenesFiveBeats() {
        val r = roles(12, beats5())
        assertEquals(FightRole.IMPACT, r[6])
        assertEquals(FightRole.HERO_HURT, r[8])
        assertEquals(FightRole.FINISHER, r[9])
        assertEquals(setOf(2, 4), r.filterValues { it == FightRole.TENSION }.keys)
        assertEquals(setOf(1, 3, 5, 7, 10), r.filterValues { it == FightRole.EXCHANGE }.keys)
    }

    @Test
    fun fourBeatBible() {
        val four = listOf(
            V2Beat(0, "Pembuka", ""),
            V2Beat(1, "Eskalasi", ""),
            V2Beat(2, "Klimaks", ""),
            V2Beat(3, "Penutup", "")
        )
        val r = roles(18, four)
        assertEquals(FightRole.IMPACT, r[6])
        assertEquals(FightRole.HERO_HURT, r[9])
        assertEquals(FightRole.FINISHER, r[10])
        assertEquals(setOf(2, 5), r.filterValues { it == FightRole.TENSION }.keys)
    }

    @Test
    fun thirtyScenesEightBeats() {
        val r = roles(30, beats8())
        assertEquals(FightRole.IMPACT, r[19])
        assertEquals(FightRole.HERO_HURT, r[21])
        assertEquals(FightRole.FINISHER, r[22])
        assertEquals(setOf(2, 5, 10, 15), r.filterValues { it == FightRole.TENSION }.keys)
        // Epilog (28-30) tanpa peran — beat setelah Klimaks tidak dapat peran.
        assertTrue(r.keys.none { it in 28..30 })
    }

    @Test
    fun nonFightFilmHasNoRoles() {
        val map = BeatMap.build(18, beats5())
        assertTrue(BeatMap.fightRoles(map, beats5(), false).isEmpty())
    }

    @Test
    fun isFightFilmFromGenreOrIdea() {
        assertTrue(BeatMap.isFightFilm(bible(Genre.ACTION)))
        // FANTASI tapi ide berantem -> tetap film fight (kasus Bima).
        assertTrue(BeatMap.isFightFilm(bible(Genre.FANTASI, "Bima melawan Rudra di langit")))
        assertTrue(BeatMap.isFightFilm(bible(Genre.DRAMA, "duel pedang terakhir")))
        assertTrue(!BeatMap.isFightFilm(bible(Genre.DRAMA, "keluarga makan malam bersama")))
        assertTrue(!BeatMap.isFightFilm(bible(Genre.ROMANSA, "")))
    }
}
