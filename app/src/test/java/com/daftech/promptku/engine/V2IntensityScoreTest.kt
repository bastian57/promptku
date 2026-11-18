package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 (riset) — skor intensitas per scene (ArcLexicon).
// Nilai dihitung tangan dari teks VERBATIM export Storm Titans; tiga
// koreksi riset terkunci di sini: Bima S3 = 12 (bukan 6 — "terakhir"
// menghitung ganda: kata "terakhir" + kata "akhir" via stemmer + frasa
// "serangan terakhir" — plus "triumph" 2), Kaito S17 = 11 (bukan 9 —
// "Kemenangan" ganda: "menang" + "kemenangan" — dan TANPA named move
// setelah posesif "Golden Frieza's" dikecualikan), Klimaks maks = 3
// (S15 Bima / S14 Kaito — S16 Kaito skor 3 itu PENUTUP, bukan Klimaks;
// v2.1 fix7: big 3 S16 hilang — "blazing" + "beam" berjarak 9 token).
class V2IntensityScoreTest {

    private fun bimaBible(): StoryBible = StoryBible(
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
            V2Beat(0, "Pembuka", ""),
            V2Beat(1, "Pemicu", ""),
            V2Beat(2, "Eskalasi", ""),
            V2Beat(3, "Klimaks", ""),
            V2Beat(4, "Penutup", "")
        ),
        episodes = listOf(V2Episode(1, "Storm Titans", "Bima vs Rudra.", 18)),
        durationMinutes = 3,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun kaitoBible(): StoryBible = bimaBible().copy(
        characters = listOf(
            V2Character(
                "Kaito",
                listOf("orange gi with blue sash", "spiky black hair", "glowing orange aura")
            )
        ),
        antagonistName = "Golden Frieza",
        antagonistDescEn = "a golden-armored tyrant with a violet energy field",
        worldName = "Stormbound Wasteland",
        worldDescEn = "Lightning splits the bruised sky over jagged rocks as thunder rolls.",
        episodes = listOf(V2Episode(1, "Storm Titans", "Kaito vs Golden Frieza.", 18))
    )

    @Test
    fun bimaScene3IsTwelve() {
        // big 3 + named "Tempest Breaker" 1 + finalitas 6 ("terakhir" cocok
        // 2 kata + frasa "serangan terakhir") + resolusi "triumph" 2.
        val bible = bimaBible()
        val score = ArcLexicon.intensityScore(
            "Bima unleashes the Tempest Breaker, channeling blinding violet lightning through his gauntlet, mouth wide open shouting in triumph as his golden eye tattoos flare, while Rudra staggers, his obsidian armor cracking under the surge.",
            "Dengan kekuatan petir tertinggi, Bima menyalurkan energi ke serangan terakhir, memecah perisai obsidian Rudra.",
            bible
        )
        assertEquals(12, score)
    }

    @Test
    fun bimaScene17IsFive() {
        // big 3 + "terakhirnya" 2.
        val bible = bimaBible()
        val score = ArcLexicon.intensityScore(
            "Bima hurls an explosive ball of concentrated plasma from his palms with his mouth wide open shouting in fierce defiance, while his scarred bronze chest muscles strain against the recoil.",
            "Di ambang batas kehancuran, Bima melepaskan seluruh energi kosmik terakhirnya untuk meruntuhkan kekuasaan kegelapan.",
            bible
        )
        assertEquals(5, score)
    }

    @Test
    fun kaitoScene3IsSix() {
        // big 3 + named "Kamehameha" 1 + "Puncak" 2.
        val bible = kaitoBible()
        val score = ArcLexicon.intensityScore(
            "Kaito channels a blinding blue Kamehameha with open mouth shouting in intense focus, colliding violently with Golden Frieza's massive violet energy sphere in a cataclysmic shockwave.",
            "Puncak pertarungan meledak dalam benturan energi murni yang menentukan nasib dunia.",
            bible
        )
        assertEquals(6, score)
    }

    @Test
    fun kaitoScene16IsThree() {
        // v2.1 fix7 — big 3 HILANG: "blazing" + "beam" berjarak 9 token
        // (noise proximity). Skor = named "Kamehameha" 1 + "akhir" 2.
        // S16 adalah PENUTUP — skor ini TIDAK masuk Klimaks (R6),
        // melainkan Penutup (R6b).
        val bible = kaitoBible()
        val score = ArcLexicon.intensityScore(
            "Kaito channels a blinding Kamehameha, his aura blazing orange, his breath huffing heavily, and releases the beam toward Frieza's violet energy field.",
            "Saat energi biru dan oranye bertabrakan, ledakan cahaya menentukan akhir pertarungan antara dua legenda.",
            bible
        )
        assertEquals(3, score)
    }

    @Test
    fun kaitoScene17IsEleven() {
        // big 3 + finalitas "akhir" 2 + resolusi 6 ("tenang" 2 +
        // "Kemenangan" ganda 4). TANPA named move: "Golden Frieza's"
        // posesif dikecualikan dari jurus bernama (bukan jurus).
        val bible = kaitoBible()
        val score = ArcLexicon.intensityScore(
            "Kaito unleashes a spiraling orange vortex, his gi flapping violently as the beam collides with Golden Frieza's violet aura, shattering monoliths.",
            "Setelah ledakan menggemparkan, Kaito berdiri di antara puing-puing, menatap langit yang kini mulai tenang. Kemenangan ini menandai akhir era tirani alien di tanah yang terpuruk.",
            bible
        )
        assertEquals(11, score)
    }

    @Test
    fun bimaScene13IsOne() {
        // resolusi "kedamaian" 2 + tenang "rests" -1.
        val bible = bimaBible()
        val score = ArcLexicon.intensityScore(
            "Bima rests his scarred bronze chest against a broken pillar, breathing heavily as wind ripples his hair.",
            "Di antara reruntuhan yang sunyi, kedamaian perlahan merayap menggantikan amukan badai purba.",
            bible
        )
        assertEquals(1, score)
    }

    @Test
    fun calmSceneIsZero() {
        val bible = bimaBible()
        val score = ArcLexicon.intensityScore(
            "Bima leaps across a fractured chasm, arms wide to gather the morning light while his scarred bronze chest heaves with heavy breaths.",
            "Sang penjaga langit melangkah maju membawa fajar baru.",
            bible
        )
        assertEquals(0, score)
    }
}
