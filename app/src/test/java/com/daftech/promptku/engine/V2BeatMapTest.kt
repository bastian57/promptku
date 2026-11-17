package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 (riset) — BeatMap: peta beat→scene deterministik yang jadi
// satu sumber kebenaran prompter (blok beat per batch), gerbang G7, dan
// ledger. Tabel kanonik dihitung tangan dari algoritma largest remainder
// (bobot 5 = [3,4,5,3,3], bobot 8 = [2,3,3,3,3,2,2,2]).
class V2BeatMapTest {

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

    @Test
    fun canonicalEighteenScenesFiveBeats() {
        val map = BeatMap.build(18, beats5())
        assertEquals(listOf(3, 4, 5, 3, 3), map.map { it.sceneCount })
        assertEquals(1 to 3, map[0].firstScene to map[0].lastScene)
        assertEquals(4 to 7, map[1].firstScene to map[1].lastScene)
        assertEquals(8 to 12, map[2].firstScene to map[2].lastScene)
        assertEquals(13 to 15, map[3].firstScene to map[3].lastScene)
        assertEquals(16 to 18, map[4].firstScene to map[4].lastScene)
        // Intensitas kanonik 1, 2, 3, 5, 1.
        assertEquals(listOf(1, 2, 3, 5, 1), map.map { it.intensity })
    }

    @Test
    fun twelveScenesFiveBeats() {
        val map = BeatMap.build(12, beats5())
        assertEquals(listOf(2, 2, 4, 2, 2), map.map { it.sceneCount })
        assertEquals(12, map.last().lastScene)
    }

    @Test
    fun thirtyScenesFiveBeats() {
        val map = BeatMap.build(30, beats5())
        assertEquals(listOf(5, 6, 9, 5, 5), map.map { it.sceneCount })
        assertEquals(30, map.last().lastScene)
    }

    @Test
    fun sixScenesFiveBeatsStillOneScenePerBeat() {
        val map = BeatMap.build(6, beats5())
        assertEquals(listOf(1, 1, 2, 1, 1), map.map { it.sceneCount })
        assertEquals(6, map.last().lastScene)
    }

    @Test
    fun thirtyScenesEightBeats() {
        val map = BeatMap.build(30, beats8())
        assertEquals(listOf(3, 5, 5, 4, 4, 3, 3, 3), map.map { it.sceneCount })
        assertEquals(5, BeatMap.climaxIndex(beats8()))
        assertEquals(22 to 24, map[5].firstScene to map[5].lastScene)
        assertEquals(25 to 27, map[6].firstScene to map[6].lastScene)
        assertEquals(28 to 30, map[7].firstScene to map[7].lastScene)
        assertEquals(30, map.last().lastScene)
    }

    @Test
    fun eighteenScenesEightBeats() {
        val map = BeatMap.build(18, beats8())
        assertEquals(listOf(2, 3, 3, 3, 3, 2, 1, 1), map.map { it.sceneCount })
        assertEquals(15 to 16, map[5].firstScene to map[5].lastScene)
    }

    @Test
    fun climaxIndexFoundByName() {
        // Bible tes 4-beat (Pembuka/Eskalasi/Klimaks/Penutup): Klimaks di
        // indeks 2, bukan 3 — nama menang atas posisi.
        val four = listOf(
            V2Beat(0, "Pembuka", ""),
            V2Beat(1, "Eskalasi", ""),
            V2Beat(2, "Klimaks", ""),
            V2Beat(3, "Penutup", "")
        )
        assertEquals(2, BeatMap.climaxIndex(four))
        // Bobot generik 4 beat (klimak indeks 2): [1,2,2,1] -> [3,6,6,3].
        val map = BeatMap.build(18, four)
        assertEquals(listOf(3, 6, 6, 3), map.map { it.sceneCount })
        assertEquals(18, map.last().lastScene)
    }

    @Test
    fun rolesAndBigAttackCapsFollowPosition() {
        val beats = beats5()
        assertEquals(BeatRole.OPENING, BeatMap.roleOf(0, beats))
        assertEquals(BeatRole.EARLY, BeatMap.roleOf(1, beats))
        assertEquals(BeatRole.LATE, BeatMap.roleOf(2, beats))
        assertEquals(BeatRole.CLIMAX, BeatMap.roleOf(3, beats))
        assertEquals(BeatRole.CLOSING, BeatMap.roleOf(4, beats))
        assertEquals(0, BeatMap.maxBigAttacks(0, beats))
        assertEquals(1, BeatMap.maxBigAttacks(1, beats))
        assertEquals(2, BeatMap.maxBigAttacks(2, beats))
        assertEquals(3, BeatMap.maxBigAttacks(3, beats))
        assertEquals(0, BeatMap.maxBigAttacks(4, beats))
    }

    @Test
    fun rangesAreContiguousAndEndAtTotalScenes() {
        for (total in listOf(6, 12, 18, 20, 30)) {
            val map = BeatMap.build(total, beats5())
            var expectFirst = 1
            for (range in map) {
                assertEquals(expectFirst, range.firstScene)
                assertTrue(range.lastScene >= range.firstScene)
                expectFirst = range.lastScene + 1
            }
            assertEquals(total, map.last().lastScene)
            assertEquals(13, BeatMap.beatForScene(13, BeatMap.build(18, beats5())).firstScene)
        }
    }
}
