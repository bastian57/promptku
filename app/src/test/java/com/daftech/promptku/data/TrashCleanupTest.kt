package com.daftech.promptku.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes Bersihkan Sampah v1.4.4 — logika murni (tanpa Room).
class TrashCleanupTest {

    private fun scene(id: Long, projectId: Long, title: String) = SceneItem(
        id = id,
        projectId = projectId,
        orderIndex = 0,
        title = title,
        engine = "Flow / Omni 1.2",
        promptText = "prompt $title",
        negativeText = "",
        createdAt = id * 1000L
    )

    private fun script(id: Long, title: String, status: String) = ScriptItem(
        id = id,
        title = title,
        synopsis = "sinopsis $title yang cukup panjang untuk lolos isReady sepuluh kata",
        genre = "ACTION",
        durationMinutes = 1,
        emotion = "Brave",
        protagonistName = "Bejo",
        protagonistDescEn = "a brave courier",
        antagonist = "Raksasa",
        worldName = "Kota",
        worldDescEn = "a metropolitan city street at night",
        conflict = "melawan raksasa",
        ending = "HAPPY",
        style = "REALISTIC",
        engine = "FLOW",
        status = status,
        createdAt = id * 1000L
    )

    @Test
    fun duplicateSceneIdsKeepOnlyNewestPerTitle() {
        // Kasus lapangan 16 Nov 14:24 WIB: proyek Bejo 12 scene =
        // 6 lama (id 1-6, build v1.4.2) + 6 baru (id 7-12, v1.4.3).
        val scenes = (1L..6L).map { scene(it, 1L, "Scene $it") } +
            (7L..12L).map { scene(it, 1L, "Scene ${it - 6}") }
        val dupes = TrashCleanup.duplicateSceneIds(scenes)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L).sorted(), dupes.sorted())
    }

    @Test
    fun sameTitleInDifferentProjectIsNotADuplicate() {
        val scenes = listOf(
            scene(1L, 1L, "Scene 1 — Pembuka"),
            scene(2L, 2L, "Scene 1 — Pembuka")
        )
        assertTrue(TrashCleanup.duplicateSceneIds(scenes).isEmpty())
    }

    @Test
    fun uniqueTitlesAndEmptyListAreUntouched() {
        val scenes = listOf(
            scene(1L, 1L, "Adegan Pasar"),
            scene(2L, 1L, "Adegan Menara")
        )
        assertTrue(TrashCleanup.duplicateSceneIds(scenes).isEmpty())
        assertTrue(TrashCleanup.duplicateSceneIds(emptyList()).isEmpty())
    }

    @Test
    fun supersededScriptsKeepOnlyNewestPerTitle() {
        val scripts = listOf(
            script(1L, "Bejo melawan raksasa", "DRAFT"),
            script(2L, "Bejo melawan raksasa", "GENERATED"),
            script(3L, "Bejo melawan raksasa", "GENERATED"),
            script(4L, "Riko si CEO", "DRAFT")
        )
        val stale = TrashCleanup.supersededScripts(scripts)
        assertEquals(listOf(1L, 2L).sorted(), stale.map { it.id }.sorted())
        // Naskah bertitel unik (Riko) tidak tersentuh.
        assertTrue(stale.none { it.id == 4L })
    }

    @Test
    fun wizardShotsWithoutScriptIdSurviveDedupByUniqueTitle() {
        // Shot Wizard berjudul unik tidak pernah kembar — dedup
        // tidak menghapus apa pun meski scriptId-nya null.
        val scenes = listOf(
            scene(1L, 1L, "Toko di Pasar Pagi"),
            scene(2L, 1L, "Menara Saat Hujan")
        )
        assertTrue(TrashCleanup.duplicateSceneIds(scenes).isEmpty())
    }
}
