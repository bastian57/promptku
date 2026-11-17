package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes Perpustakaan Efek v1.4.2 — 28 efek, trigger kalimat, nama tokoh
// dari kalimat, slot efek di mesin naskah, dan invarian no-repeat.
class EffectLibraryTest {

    @Test
    fun clashDetectsFight() {
        // v2.1 fix6 (riset) — "clash" + "bentrok" masuk FIGHT_TRIGGERS
        // (paralel dengan ACTION di StoryInfer).
        assertTrue(EffectLibrary.detect("Dua titan clash di tengah badai").fight)
        assertTrue(EffectLibrary.detect("Dua geng bentrok di jalanan").fight)
    }

    @Test
    fun libraryHas33EffectsAcrossFiveCategories() {
        // v2.1 fix8 v2 — 33 efek: 28 generik + 5 roleOnly (fight_impact,
        // fight_hero_hurt, fight_tension_a/b/c) yang TIDAK masuk siklus
        // generik. Kategori FIGHT 8 -> 13.
        assertEquals(33, EffectLibrary.ALL.size)
        assertEquals(5, EffectCategory.values().size)
        assertEquals(33, EffectLibrary.ALL.map { it.id }.toSet().size)
        assertEquals(13, EffectLibrary.ALL.count { it.category == EffectCategory.FIGHT })
        assertEquals(5, EffectLibrary.ALL.count { it.roleOnly })
        assertEquals(4, EffectLibrary.ALL.count { it.category == EffectCategory.EXPLOSION })
        assertEquals(6, EffectLibrary.ALL.count { it.category == EffectCategory.EMOTION })
        assertEquals(6, EffectLibrary.ALL.count { it.category == EffectCategory.CAMERA })
        assertEquals(4, EffectLibrary.ALL.count { it.category == EffectCategory.TIME })
    }

    @Test
    fun noFragmentContainsTimeWords() {
        // Fragmen efek tidak boleh menyebut waktu (morning/dawn/dll) —
        // supaya tidak bertabrakan dengan waktu dunia (pelajaran v1.4.1).
        val timeWords = listOf("morning", "dawn", "sunrise", "noon", "midday", "dusk", "sunset", "golden", "night", "midnight")
        for (effect in EffectLibrary.ALL) {
            val lower = effect.fragmentEn.lowercase()
            for (word in timeWords) {
                assertFalse("Efek ${effect.id} mengandung kata waktu: $word", lower.contains(word))
            }
        }
    }

    @Test
    fun cameraEffectsOnlyChangeHowTheShotIsTaken() {
        for (effect in EffectLibrary.ALL.filter { it.category == EffectCategory.CAMERA }) {
            assertTrue("Efek kamera ${effect.id} harus punya fragmentEn kosong", effect.fragmentEn.isBlank())
            assertTrue("Efek kamera ${effect.id} harus punya override kamera", effect.cameraMove != null)
        }
    }

    @Test
    fun assimilatedVerbFormsStillTrigger() {
        // Bentuk me- berasimilasi ("meninju", "memukul", "menendang") TIDAK
        // mengandung akar katanya — trigger wajib mencakup bentuk itu.
        assertTrue(EffectLibrary.detect("Bejo meninju raksasa itu sampai tersungkur").fight)
        assertTrue(EffectLibrary.detect("Dia memukul preman di pasar malam itu").fight)
        assertTrue(EffectLibrary.detect("Dia menendang pintu sampai jebol dan masuk").fight)
        assertEquals(
            Genre.ACTION,
            StoryInfer.infer("Bejo meninju raksasa di pasar malam itu").genre
        )
    }

    @Test
    fun bejoSentenceDetectsFightNotExplosion() {
        val detected = EffectLibrary.detect("Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh")
        assertTrue(detected.fight)
        assertFalse(detected.explosion)
        assertNull(detected.emotionId)
        assertEquals(listOf("FIGHT"), detected.toTokens())
        assertEquals(detected, EffectLibrary.detectFromTokens("FIGHT"))
    }

    @Test
    fun explosionAndEmotionTriggers() {
        val boom = EffectLibrary.detect("Ledakan besar menghancurkan gedung dan membakar jalanan kota itu")
        assertTrue(boom.explosion)
        val sad = EffectLibrary.detect("Riko sedih menangis di tengah hujan setelah kehilangan tokonya")
        assertEquals("emotion_sad", sad.emotionId)
        val angry = EffectLibrary.detect("Dia marah dan meninju tembok sampai tangannya berdarah")
        assertEquals("emotion_angry", angry.emotionId)
        assertTrue(angry.fight)
    }

    @Test
    fun heroNameComesFromSentence() {
        assertEquals("Bejo", StoryInfer.heroNameFromSentence("Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"))
        assertEquals("Bejo", StoryInfer.heroNameFromSentence("Di langit kota yang telah runtuh, Bejo melawan raksasa besar itu"))
        assertEquals("Riko", StoryInfer.heroNameFromSentence("Riko si tukang tambal ban ternyata CEO perusahaan menara itu"))
        assertNull(StoryInfer.heroNameFromSentence("Di kota yang hancur itu terjadi pertempuran besar semalaman"))
    }

    @Test
    fun toScriptInputDerivesHeroAndEffectTokens() {
        val inference = StoryInfer.infer("Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh")
        val input = StoryInfer.toScriptInput(
            "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh", "", 1, inference
        )
        assertEquals("Bejo", input.protagonistName)
        assertEquals("FIGHT", input.effectIds)
        assertEquals("Raksasa", input.antagonist)
    }

    @Test
    fun bejoOneMinuteFilmFightsTheGiant() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 1, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        assertEquals(6, draft.scenes.size)
        // Klimaks (scene ke-5, orderIndex 4) = finisher.
        assertEquals("finisher", draft.scenes[4].effectId)
        assertTrue(draft.scenes[4].actionEn.contains("final blow"))
        // Beat Eskalasi (scene 3-4) = jurus berantem.
        assertTrue(draft.scenes[2].effectId.isNotBlank())
        assertTrue(draft.scenes[3].effectId.isNotBlank())
        // TIDAK ADA lagi cerita kurir + chip di film berantem.
        val allActions = draft.scenes.joinToString(" ") { it.actionEn }
        for (word in listOf("chip", "package", "delivery", "parcel", "courier")) {
            assertFalse("Aksi masih mengandung cerita kurir: $word", allActions.lowercase().contains(word))
        }
        // Nama tokoh di prompt = Bejo, bukan Raka.
        val shot = ScriptEngine.toShotInput(draft, draft.scenes[0])
        assertEquals("Bejo", shot.character?.name)
    }

    @Test
    fun tenMinuteFightFilmHasNoRepeatedActionWithinABeat() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 10, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        assertEquals(60, draft.scenes.size)
        draft.scenes.groupBy { it.sequenceIndex }.forEach { (beat, scenes) ->
            val actions = scenes.map { it.actionEn }
            assertEquals(
                "Beat $beat punya aksi berulang",
                actions.size,
                actions.toSet().size
            )
        }
    }

    @Test
    fun noTwoConsecutiveTwoShotsInBejoFilm() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 1, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        val sizes = draft.scenes.map { it.shotSize }
        for (i in 1 until sizes.size) {
            assertFalse(
                "Scene ${i} dan ${i + 1} sama-sama TWO_SHOT",
                sizes[i] == ShotSize.TWO_SHOT && sizes[i - 1] == ShotSize.TWO_SHOT
            )
        }
    }

    @Test
    fun courierStoryStillWorksWhenSentenceIsAboutCourier() {
        val input = ScriptTemplates.action10Min()
        assertTrue(ScriptPools.isCourierStory(input.synopsis))
        val draft = ScriptEngine.build(input)
        val allActions = draft.scenes.joinToString(" ") { it.actionEn }
        assertTrue(allActions.contains("chip"))
        // Pool kurir tetap punya 8 aksi per beat, tanpa pengulangan.
        val pool = ScriptPools.courierActionsFor(0)
        assertEquals(8, pool.size)
        assertEquals(8, pool.toSet().size)
    }

    @Test
    fun neutralActionPoolHasNoCourierWords() {
        for (beat in 0..7) {
            val pool = ScriptPools.actionsFor(Genre.ACTION, beat)
            assertEquals(8, pool.size)
            val joined = pool.joinToString(" ").lowercase()
            for (word in listOf("chip", "package", "delivery", "parcel", "courier")) {
                assertFalse("Pool Action beat $beat masih mengandung: $word", joined.contains(word))
            }
        }
        // Dialog + want beat 1 sudah netral (bukan kurir).
        assertFalse(ScriptPools.dialoguesFor(Genre.ACTION, 1).joinToString().contains("delivery"))
        assertFalse(ScriptPools.wantsFor(1).contains("package"))
    }

    @Test
    fun applyAndClearEffectRoundTrip() {
        val sentence = "Bejo melawan raksasa 2,8 meter di langit kota yang telah runtuh"
        val input = StoryInfer.toScriptInput(sentence, "", 1, StoryInfer.infer(sentence))
        val draft = ScriptEngine.build(input)
        val scene = draft.scenes[4]
        assertEquals("finisher", scene.effectId)
        val cleared = ScriptEngine.clearEffect(scene, input)
        assertEquals("", cleared.effectId)
        assertFalse(cleared.actionEn.contains("final blow"))
        assertNotEquals(scene.actionEn, cleared.actionEn)
        val reapplied = ScriptEngine.applyEffect(cleared, "finisher")
        assertEquals("finisher", reapplied.effectId)
        assertEquals(scene.actionEn, reapplied.actionEn)
    }

    @Test
    fun candidatesForBeatStartWithNoEffect() {
        val candidates = EffectLibrary.candidatesForBeat(3)
        assertEquals("", candidates.first())
        assertTrue(candidates.contains("finisher"))
        assertTrue(candidates.contains("explosion_fire"))
        for (id in candidates.filter { it.isNotBlank() }) {
            val effect = EffectLibrary.byId(id)
            assertTrue("Kandidat $id tidak dikenal", effect != null)
            assertTrue("Kandidat $id tidak boleh di beat 3", 3 in effect!!.beats)
        }
    }

    @Test
    fun kisahKurirTemplateScoresTen() {
        val template = Templates.kisahKurir()
        val result = PromptScore.score(template.input)
        assertEquals(10, result.score)
        assertTrue(Templates.all.any { it.id == "kisah-kurir-01" })
    }
}
