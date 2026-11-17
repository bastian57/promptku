package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes PromptCore v3: 12 slot, anggaran kata per slot, meter, matriks bahasa,
// kartu 3 jangkar, Ambience Lock.
class PromptCoreTest {

    // DNA panjang Riko (129 kata) — kasus yang bikin bug trim v1.2.0.
    private fun longDna(): String = (
        "A 32-year-old Indonesian man, Riko, with a calm confident face, " +
            "weathered skin with oil stains on his forehead, short black hair slicked back, " +
            "a thick mustache, deep brown eyes, a grease-smudged cheek, broad shoulders, " +
            "muscular arms covered in tattoos of gears and chains, wearing a torn denim jacket " +
            "over a white undershirt, oil-stained work pants, heavy boots, an old leather apron, " +
            "a silver ring on his right hand, and a worn leather watch on his left wrist, " +
            "standing in a small roadside tire repair shop with worn tires stacked against " +
            "the wall, tools scattered on the ground, an old motorcycle in the corner, " +
            "flickering fluorescent light, dust in the air, and a glass tower visible across " +
            "the street through the open door"
        )

    private fun rikoInput(withCard: Boolean): ShotInput = ShotInput(
        title = "Riko — Scene 1",
        engine = TargetEngine.FLOW,
        genre = Genre.DRAMA,
        character = CharacterDna("Riko", longDna()),
        characterCard = if (withCard) {
            CharacterCard(
                trait = "calm confident look",
                gesture = "hand to hair",
                objectProp = "torn denim jacket",
                faceAnchor = "grease-smudged face"
            )
        } else {
            null
        },
        world = WorldLock("Bengkel", "a small roadside tire repair shop at dusk, worn tires stacked against the wall, tools scattered on the ground"),
        actionEn = "kneels and begins fixing the tire",
        light = LightKind.GOLDEN_HOUR,
        cameraMove = CameraMove.DOLLY_IN,
        lensMm = 35,
        style = VisualStyle.REALISTIC
    )

    @Test
    fun cardReplacesLongDnaInFlow() {
        val prompt = PromptEngine.build(rikoInput(withCard = true))
        // Tidak ada pemangkasan, dunia + aksi + audio utuh.
        assertFalse(prompt.warnings.any { it.contains("dipangkas") })
        assertTrue(prompt.text.contains("torn denim jacket"))
        assertTrue(prompt.text.contains("tire repair shop"))
        assertTrue(prompt.text.contains("kneels and begins fixing the tire"))
    }

    @Test
    fun longDnaWithoutCardStillTrimsWithReport() {
        val prompt = PromptEngine.build(rikoInput(withCard = false))
        assertTrue(prompt.warnings.any { it.contains("dipangkas") })
        assertTrue(prompt.text.contains("tire repair shop"))
    }

    @Test
    fun meterLineShowsSlotsAndTotal() {
        val line = PromptCore.meterLine(rikoInput(withCard = true), TargetEngine.FLOW)
        assertTrue(line.contains("SHOT"))
        assertTrue(line.contains("SUBJECT"))
        assertTrue(line.contains("WORLD"))
        assertTrue(line.contains("TOTAL"))
    }

    @Test
    fun longDnaDominatesMeterWithoutCard() {
        val meter = PromptCore.meterFor(rikoInput(withCard = false), TargetEngine.FLOW)
        val subject = meter.first { it.slot == "SUBJECT" }
        assertEquals(SlotStatus.RED, subject.status)
        assertTrue(subject.words > 100)
        assertEquals("SUBJECT", PromptCore.hungriestSlot(rikoInput(withCard = false), TargetEngine.FLOW)?.slot)
    }

    @Test
    fun cardSubjectStaysGreen() {
        val meter = PromptCore.meterFor(rikoInput(withCard = true), TargetEngine.FLOW)
        val subject = meter.first { it.slot == "SUBJECT" }
        assertEquals(SlotStatus.GREEN, subject.status)
        assertTrue(subject.words <= 25)
    }

    @Test
    fun dialogueMatrixSeedanceIdAllowedWanSilent() {
        assertEquals(DialogueCheck.ALLOWED, DialogueMatrix.check(TargetEngine.SEEDANCE, DialogueLang.ID).check)
        assertEquals(DialogueCheck.ALLOWED, DialogueMatrix.check(TargetEngine.SEEDANCE, DialogueLang.EN).check)
        assertEquals(DialogueCheck.SILENT, DialogueMatrix.check(TargetEngine.WAN, DialogueLang.ID).check)
        assertEquals(DialogueCheck.SILENT, DialogueMatrix.check(TargetEngine.WAN, DialogueLang.EN).check)
        assertEquals(DialogueCheck.WARN, DialogueMatrix.check(TargetEngine.FLOW, DialogueLang.ID).check)
        assertEquals(DialogueCheck.ALLOWED, DialogueMatrix.check(TargetEngine.FLOW, DialogueLang.EN).check)
        assertEquals(DialogueCheck.WARN, DialogueMatrix.check(TargetEngine.KLING, DialogueLang.ID).check)
        assertEquals(DialogueCheck.ALLOWED, DialogueMatrix.check(TargetEngine.KLING, DialogueLang.EN).check)
    }

    @Test
    fun slotsFollowCanonicalOrder() {
        val slots = PromptCore.slotsFor(rikoInput(withCard = true), TargetEngine.FLOW)
        assertTrue(slots.shot.isNotBlank())
        assertTrue(slots.subject.isNotBlank())
        assertTrue(slots.world.isNotBlank())
        assertTrue(slots.action.contains("[00:00-00:04]"))
    }

    @Test
    fun dialogueBudgetScalesWithClipSeconds() {
        val fiveSec = PromptCore.slotBudgets(TargetEngine.FLOW, 5f)["DIALOGUE"]
        val tenSec = PromptCore.slotBudgets(TargetEngine.FLOW, 10f)["DIALOGUE"]
        assertEquals(12, fiveSec)
        assertEquals(25, tenSec)
        assertEquals(0, PromptCore.slotBudgets(TargetEngine.WAN, 10f)["DIALOGUE"])
    }

    @Test
    fun ambienceLockPrependsAudio() {
        val lock = GlobalLock(ambience = "crickets and night wind")
        val result = lock.applyTo(ShotInput(audioEn = "metal tools clinking"))
        assertTrue(result.audioEn.startsWith("crickets and night wind"))
        assertTrue(result.audioEn.contains("metal tools clinking"))
        assertTrue(lock.summaryId().contains("Ambience"))
    }

    @Test
    fun ambienceNotDuplicatedWhenAlreadyPresent() {
        val lock = GlobalLock(ambience = "crickets and night wind")
        val result = lock.applyTo(ShotInput(audioEn = "crickets and night wind, metal tools clinking"))
        assertEquals(1, result.audioEn.split("crickets and night wind").size - 1)
    }

    @Test
    fun emptyLockLeavesInputUnchanged() {
        val input = ShotInput(audioEn = "wind")
        assertEquals(input, GlobalLock().applyTo(input))
        assertEquals("Belum dikunci", GlobalLock().summaryId())
    }

    @Test
    fun characterCardWordCount() {
        val card = CharacterCard(
            trait = "calm confident look",
            gesture = "hand to hair",
            objectProp = "torn denim jacket",
            faceAnchor = "grease-smudged face"
        )
        assertEquals(11, card.wordCount())
        assertEquals(
            "calm confident look, hand to hair, torn denim jacket, grease-smudged face",
            card.toDescription()
        )
        assertTrue(CharacterCard().isEmpty())
        assertFalse(card.isEmpty())
    }
}
