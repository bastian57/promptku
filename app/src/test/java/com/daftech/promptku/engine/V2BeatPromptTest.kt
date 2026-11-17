package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 (riset) — blok beat posisional di prompt prompter.
// Menggantikan aturan ESCALATION generik fix4/fix5 (gagal 2x di lapangan):
// tiap batch kini tahu beat-nya (nomor, rentang scene, intensitas),
// kata yang DILARANG di beat itu, dan apa yang sudah terpakai (ledger).
class V2BeatPromptTest {

    private fun bible(): StoryBible = StoryBible(
        title = "Storm Titans: Clash of Legends",
        logline = "Bima battles Rudra.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Bima",
                listOf("glowing golden eye tattoos", "scarred bronze chest", "ancient silver gauntlets")
            )
        ),
        antagonistName = "Rudra",
        antagonistDescEn = "A ruthless immortal titan clad in black obsidian armor.",
        worldName = "Bayangan Langit",
        worldDescEn = "Floating volcanic islands drift through churning purple thunderclouds.",
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

    @Test
    fun openingBatchNamesBeatOneAndForbidsFinality() {
        val prompt = DirectorPrompts.prompterUser(bible(), listOf(1), "")
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 1 OF 5"))
        assertTrue(prompt, prompt.contains("(scenes 1-3 of 18)"))
        assertTrue(prompt, prompt.contains("Intensity 1 of 5"))
        assertTrue(prompt, prompt.contains("FORBIDDEN in this beat"))
        assertTrue(prompt, prompt.contains("final, last, ultimate, peak, climax, decisive"))
        // Batch Pembuka tidak boleh membawa blok Klimaks.
        assertFalse(prompt, prompt.contains("YOU ARE WRITING BEAT 4 OF 5"))
    }

    @Test
    fun climaxBatchAllowsFinalityOnlyHere() {
        val prompt = DirectorPrompts.prompterUser(bible(), listOf(13), "")
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 4 OF 5"))
        assertTrue(prompt, prompt.contains("(scenes 13-15 of 18)"))
        assertTrue(prompt, prompt.contains("Intensity 5 of 5"))
        assertTrue(prompt, prompt.contains("ALLOWED in this beat (and ONLY here): final, last, ultimate"))
        assertTrue(prompt, prompt.contains("THE climax"))
    }

    @Test
    fun closingBatchIsAftermathOnly() {
        val prompt = DirectorPrompts.prompterUser(bible(), listOf(16), "")
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 5 OF 5"))
        assertTrue(prompt, prompt.contains("(scenes 16-18 of 18)"))
        assertTrue(prompt, prompt.contains("AFTERMATH ONLY"))
        assertTrue(prompt, prompt.contains("This is the last beat — end the film here."))
    }

    @Test
    fun batchSpanningTwoBeatsCarriesBothBlocks() {
        // Scene 3 (Pembuka) + 4 (Pemicu) = dua blok beat dalam satu batch.
        val prompt = DirectorPrompts.prompterUser(bible(), listOf(3, 4), "")
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 1 OF 5"))
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 2 OF 5"))
    }

    @Test
    fun ledgerStateAppearsAndNamesSpentMove() {
        val ledger = ArcLedger(
            bigAttacks = listOf(3 to "Tempest Breaker"),
            villainEntranceScene = 1,
            victoriesClaimed = emptyList()
        )
        val prompt = DirectorPrompts.prompterUser(bible(), listOf(8), "", ledger = ledger)
        assertTrue(prompt, prompt.contains("STATE SO FAR (already used — do NOT repeat)"))
        assertTrue(prompt, prompt.contains("\"Tempest Breaker\" (scene 3)"))
        assertTrue(prompt, prompt.contains("Villain entrance: used (scene 1)"))
    }

    @Test
    fun retryPromptForKlimaksSceneCarriesKlimaksBlockAndLedger() {
        // Retry (dan regenerasi) memakai ledger yang sama dengan batch
        // biasa — batch Klimaks yang di-retry tetap tahu posisinya.
        val ledger = ArcLedger(
            bigAttacks = listOf(3 to "Tempest Breaker"),
            villainEntranceScene = 1,
            victoriesClaimed = listOf(4)
        )
        val prompt = DirectorPrompts.prompterUser(
            bible(), listOf(13, 14, 15), "",
            retryReasons = mapOf(13 to "Scene 13: klaim resolusi di luar Penutup"),
            ledger = ledger
        )
        assertTrue(prompt, prompt.contains("YOU ARE WRITING BEAT 4 OF 5"))
        assertTrue(prompt, prompt.contains("STATE SO FAR"))
        assertTrue(prompt, prompt.contains("\"Tempest Breaker\" (scene 3)"))
    }
}
