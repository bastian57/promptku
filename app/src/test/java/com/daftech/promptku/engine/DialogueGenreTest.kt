package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes Dialog per Genre v1.4.3 — tiap genre punya suara sendiri di tiap
// peran dramatis; peran dihitung dari totalBeats (bukan nomor beat
// mentah) supaya FIVE beat 3 = Klimaks, beat 4 = Penutup.
class DialogueGenreTest {

    @Test
    fun everyGenreHasThreeShortUniqueLinesPerRole() {
        for (genre in Genre.values()) {
            for (role in ScriptPools.DialogueRole.values()) {
                val lines = ScriptPools.dialoguesFor(genre, roleBeat(role, 8), 8)
                assertEquals("Genre $genre peran $role harus 3 baris", 3, lines.size)
                assertEquals("Genre $genre peran $role ada baris kembar", 3, lines.toSet().size)
                for (line in lines) {
                    assertTrue("Baris kosong: $genre $role", line.isNotBlank())
                    val words = line.split(Regex("\\s+")).size
                    assertTrue("Baris terlalu panjang ($words kata): $line", words <= 8)
                }
            }
        }
    }

    // Beat wakil tiap peran di struktur EIGHT (beat 0 tidak ada dialog).
    private fun roleBeat(role: ScriptPools.DialogueRole, totalBeats: Int): Int = when (role) {
        ScriptPools.DialogueRole.DISCOVERY -> 1
        ScriptPools.DialogueRole.TENSION -> 2
        ScriptPools.DialogueRole.CLIMAX -> totalBeats - 2
        ScriptPools.DialogueRole.CLOSING -> totalBeats - 1
    }

    @Test
    fun beatZeroHasNoDialogue() {
        for (genre in Genre.values()) {
            assertTrue(ScriptPools.dialoguesFor(genre, 0, 8).isEmpty())
            assertTrue(ScriptPools.dialoguesFor(genre, 0, 5).isEmpty())
        }
    }

    @Test
    fun roleMappingDiffersBetweenFiveAndEight() {
        assertEquals(ScriptPools.DialogueRole.DISCOVERY, ScriptPools.dialogueRoleFor(1, 8))
        assertEquals(ScriptPools.DialogueRole.TENSION, ScriptPools.dialogueRoleFor(2, 8))
        assertEquals(ScriptPools.DialogueRole.TENSION, ScriptPools.dialogueRoleFor(4, 8))
        assertEquals(ScriptPools.DialogueRole.CLIMAX, ScriptPools.dialogueRoleFor(6, 8))
        assertEquals(ScriptPools.DialogueRole.CLOSING, ScriptPools.dialogueRoleFor(7, 8))
        assertEquals(ScriptPools.DialogueRole.TENSION, ScriptPools.dialogueRoleFor(2, 5))
        assertEquals(ScriptPools.DialogueRole.CLIMAX, ScriptPools.dialogueRoleFor(3, 5))
        assertEquals(ScriptPools.DialogueRole.CLOSING, ScriptPools.dialogueRoleFor(4, 5))
    }

    @Test
    fun bejoBugIsGoneClosingSceneIsNotBetrayal() {
        // Cacat lapangan 16 Nov 13:25 WIB: film Bejo 1 menit (FIVE)
        // dapat "You sold me out." di scene Penutup — kalimat itu
        // ditulis untuk EIGHT beat 4 (Harga Kemenangan). Sekarang
        // FIVE beat 4 = Penutup, dan ACTION Penutup = kalimat penutup.
        val fiveClosing = ScriptPools.dialoguesFor(Genre.ACTION, 4, 5)
        assertEquals(listOf("It's done.", "Let's go home.", "It's over."), fiveClosing)
        val eightBeat4 = ScriptPools.dialoguesFor(Genre.ACTION, 4, 8)
        assertEquals(
            listOf("Come on. Is that all?", "You picked the wrong fight.", "Stay down."),
            eightBeat4
        )
        // Klimaks FIVE (beat 3) = kalimat klimaks, bukan tengah.
        assertEquals(
            listOf("Not while I'm still standing.", "This ends now.", "Get up. We finish this."),
            ScriptPools.dialoguesFor(Genre.ACTION, 3, 5)
        )
    }

    @Test
    fun actionNeverGetsTheFourMisplacedLines() {
        val banned = listOf("sold me out", "need your help", "Did you see that?", "Please. Not like this.")
        for (totalBeats in listOf(5, 8)) {
            for (beat in 1 until totalBeats) {
                val joined = ScriptPools.dialoguesFor(Genre.ACTION, beat, totalBeats).joinToString(" ")
                for (bad in banned) {
                    assertFalse("ACTION beat $beat (total $totalBeats) masih mengandung: $bad", joined.contains(bad))
                }
            }
        }
    }

    @Test
    fun misplacedLinesAreRelocatedNotDeleted() {
        val thriller = (1..7).flatMap { ScriptPools.dialoguesFor(Genre.THRILLER, it, 8) }
        assertTrue(thriller.contains("You sold me out."))
        val drama = (1..7).flatMap { ScriptPools.dialoguesFor(Genre.DRAMA, it, 8) }
        assertTrue(drama.contains("I need your help."))
        assertTrue(drama.contains("Please. Not like this."))
        val doc = (1..7).flatMap { ScriptPools.dialoguesFor(Genre.DOKUMENTER, it, 8) }
        assertTrue(doc.contains("Did you see that?"))
    }

    @Test
    fun bejoOneMinuteFilmDialogueFitsItsBeats() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 1, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        val dialogues = draft.scenes.mapNotNull { it.dialogue?.textEn }
        assertEquals(4, dialogues.size)
        // Klimaks (scene 5) = kalimat klimaks Action; Penutup = penutup.
        val climax = ScriptPools.dialoguesFor(Genre.ACTION, 3, 5)
        assertTrue("Dialog klimaks bukan kalimat klimaks: ${draft.scenes[4].dialogue?.textEn}", climax.contains(draft.scenes[4].dialogue?.textEn))
        val closing = ScriptPools.dialoguesFor(Genre.ACTION, 4, 5)
        assertTrue(closing.contains(draft.scenes[5].dialogue?.textEn))
        for (line in dialogues) {
            assertFalse("Dialog Bejo masih mengandung kalimat salah tempat: $line", line.contains("sold me out"))
            assertFalse("Dialog Bejo masih mengandung kalimat salah tempat: $line", line.contains("need your help"))
            assertFalse("Dialog Bejo masih mengandung kalimat salah tempat: $line", line.contains("Did you see that?"))
        }
    }

    @Test
    fun tenMinuteActionFilmDialogueComesFromRoleTables() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 10, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        assertEquals(60, draft.scenes.size)
        for (scene in draft.scenes) {
            val line = scene.dialogue?.textEn ?: continue
            val roleLines = ScriptPools.dialoguesFor(Genre.ACTION, scene.sequenceIndex, 8)
            assertTrue("Dialog scene ${scene.orderIndex + 1} bukan dari tabel peran beat ${scene.sequenceIndex}: $line", roleLines.contains(line))
        }
    }
}
