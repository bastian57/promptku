package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.1 fix6 (riset) — gerbang G7 "Satu Puncak". Fixture utama = teks
// VERBATIM dua film lapangan "Storm Titans" (Bima vs Rudra, Mode A;
// Kaito vs Golden Frieza, Mode B, 17 Nov 2026) — aksi dari segmen
// [00:00-00:05] / teks Flow, narasi dari blok NARASI LENGKAP. Harapan
// dihitung ulang dari KODE (BeatMap + runArcGate), bukan dari ringkasan
// riset — kalibrasi sempat salah hitung 3x sebelum tes ini hijau:
// (1) Bima S3 JUGA melanggar R3 ("shouting in triumph" di aksi = klaim
// resolusi), skornya 12 bukan 6 ("terakhir" ganda + frasa + "triumph");
// (2) R6 Kaito GAGAL (Klimaks maks 3 dari S13–15 — S16 adalah PENUTUP,
// bukan Klimaks); pra-Klimaks Kaito 7 (S9), Penutup 11 (S17);
// (3) v2.1 fix7 — Bima S2 BUKAN big attack lagi ("blazing" + "spear"
// berjarak 23 token — proximity 6 membunuh noise ini; R4 Pembuka jadi
// "1x", bukan "2x"). Kalibrasi fix6 sempat membaliknya ("tidak harus
// sebelahan") — fix7 membalik lagi dengan bukti ukur (serangan asli
// terjauh 6, noise terdekat 8).
class V2Gate7Test {

    private fun bimaBible(): StoryBible = StoryBible(
        title = "Storm Titans: Clash of Legends",
        logline = "Bima battles Rudra in a floating sky world.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character(
                "Bima",
                listOf("glowing golden eye tattoos", "scarred bronze chest", "ancient silver gauntlets")
            )
        ),
        antagonistName = "Rudra",
        antagonistDescEn = "A ruthless immortal titan clad in black obsidian armor who commands " +
            "the dark tempest clouds and seeks to plunge the world into eternal night.",
        worldName = "Bayangan Langit",
        worldDescEn = "Floating volcanic islands drift through churning purple thunderclouds " +
            "while ancient stone temples crumble under relentless gale-force winds.",
        lightEn = "Blinding violet lightning flashes piercing through heavy, bruised-indigo storm clouds.",
        ambienceEn = "Roaring deafening thunder, howling hurricane winds.",
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

    private fun plan(
        num: Int,
        action: String,
        narrationId: String = "",
        dialogueEn: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action, cameraMove = "slow dolly in",
        lensMm = 35, shotSize = "wide", angle = "low",
        dialogueId = "", dialogueEn = dialogueEn,
        audioEn = "thunder, wind", effectId = "",
        wantEn = "win", obstacleEn = "the tyrant", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = ""
    )

    private fun scenePrompt(p: V2ScenePlan): V2ScenePrompt =
        V2ScenePrompt(
            sceneNumber = p.sceneNumber, plan = p,
            flowText = "x", klingText = "x", seedanceText = "x", wanText = "x"
        )

    private fun bimaFilm(): List<V2ScenePrompt> {
        val rows = listOf(
            1 to ("Rudra crashes from the dark clouds, slamming his obsidian spear into the crumbling stone, his cape rippling in the gale as thunder roars around him." to
                "Saat Rudra menurunkan tombaknya, awan gelap bergemuruh menandakan permulaan pertarungan abadi."),
            2 to ("Bima shouts, mouth wide open, eyes blazing with rage, swinging his silver gauntlet in a sweeping arc while his hair whips in the wind, and Rudra counters, thrusting his spear upward, sparks flying between them." to
                "Benturan pertama menggetarkan pulau mengapung, menandai pertarungan sengit antara cahaya dan kegelapan."),
            3 to ("Bima unleashes the Tempest Breaker, channeling blinding violet lightning through his gauntlet, mouth wide open shouting in triumph as his golden eye tattoos flare, while Rudra staggers, his obsidian armor cracking under the surge." to
                "Dengan kekuatan petir tertinggi, Bima menyalurkan energi ke serangan terakhir, memecah perisai obsidian Rudra."),
            4 to ("Bima stands atop the silent ruins, golden eye tattoos glowing softly, breathing calmly as the violet lightning fades and a serene golden dawn spreads across the floating islands." to
                "Saat fajar keemasan muncul, Bima berdiri sebagai penjaga, dunia mengambang kembali damai."),
            5 to ("Bima hurls a glowing plasma disk across the chasm, weight shifting heavily onto his lead foot as his scarred chest heaves for breath." to
                "Di tengah reruntuhan, pertempuran mencapai puncak ketegangan saat takdir dunia digantungkan pada kekuatan para dewa petir."),
            6 to ("Bima leaps across a fractured chasm, arms wide to gather the morning light while his scarred bronze chest heaves with heavy breaths." to
                "Sang penjaga langit melangkah maju membawa fajar baru."),
            7 to ("Bima rests his ancient silver gauntlets upon a sacred altar, lips parted as he breathes out a long steady sigh of relief." to
                "Kekuatan petir kini menyatu dengan kedamaian abadi."),
            8 to ("Bima turns toward the horizon as the golden sunlight envelops his scarred bronze chest and glowing golden eye tattoos fade into a gentle amber glow." to
                "Di bawah langit yang cerah, legenda para titan bersemi kembali dalam keabadian."),
            9 to ("Rudra descends from the swirling dark clouds, his obsidian spear striking the ancient stone temple, dust swirling as his cape billows in the gale." to
                "Rudra muncul dari awan kelam, memulai pertarungan yang akan mengguncang pulau-pulau mengapung."),
            10 to ("Bima meets Rudra, swinging his silver gauntlet in a sweeping arc, hair and dust rippling as the two titans clash, sending shockwaves through the crumbling pillars." to
                "Benturan pertama antara dua raksasa menggetarkan tanah berapi yang melayang."),
            11 to ("Bima shouts, mouth wide open roaring with rage, as he channels blinding violet lightning into his gauntlet, unleashing a massive bolt that strikes Rudra, shattering the obsidian armor." to
                "Dengan kekuatan petir yang terpusat, Bima menyalakan serangan terakhir yang menghancurkan pertahanan gelap Rudra."),
            12 to ("Bima stands victorious atop the quiet floating ruins, golden sunrise bathing the shattered islands, his breath steady as the wind calms." to
                "Saat fajar menyinari puing-puing mengapung, kedamaian kembali ke dunia yang pernah terancam."),
            13 to ("Bima rests his scarred bronze chest against a broken pillar, breathing heavily as wind ripples his hair." to
                "Di antara reruntuhan yang sunyi, kedamaian perlahan merayap menggantikan amukan badai purba."),
            14 to ("Rudra's broken obsidian armor crumbles into dark dust on the stone floor, his heavy armor plating shifting with a final metallic clatter." to
                "Kekuatan kegelapan telah runtuh, meninggalkan sisa-sisa kehancuran di atas pulau-pulau melayang."),
            15 to ("Bima raises his ancient silver gauntlets toward the sky, his lips parted as he shouts a quiet sigh of relief with a triumphant expression." to
                "Sang penjaga langit berdiri sebagai saksi kebangkitan dunia baru yang tenang."),
            16 to ("Bima stands motionless on the golden horizon, his glowing golden eye tattoos pulsing softly as fabric responds to the gentle morning breeze." to
                "Badai telah usai, dan takdir dunia melayang kini aman dalam pelukan keabadian."),
            17 to ("Bima hurls an explosive ball of concentrated plasma from his palms with his mouth wide open shouting in fierce defiance, while his scarred bronze chest muscles strain against the recoil." to
                "Di ambang batas kehancuran, Bima melepaskan seluruh energi kosmik terakhirnya untuk meruntuhkan kekuasaan kegelapan."),
            18 to ("Bima plants his silver gauntlets into the bedrock, pivoting his torso with lips parted as he shouts a fierce challenge at the falling dark titan." to
                "Ketegangan semakin memuncak di atas langit yang terkoyak badai.")
        )
        return rows.map { (num, pair) -> scenePrompt(plan(num, pair.first, pair.second)) }
    }

    private fun kaitoFilm(): List<V2ScenePrompt> {
        val rows = listOf(
            1 to ("Golden Frieza descends rapidly from the dark clouds, his golden armor glinting with a blinding sheen, eyes narrowed in arrogance as he points a finger forward to declare his total domination." to
                "Kehadiran sang tiran emas membelah angkasa, membawa ancaman kehancuran mutlak di medan pertempuran."),
            2 to ("Kaito and Golden Frieza engage in a high-speed aerial melee, trading rapid airborne kicks and punches while massive stone guardians surge forward from the crumbling earth around them." to
                "Bentrokan kekuatan maha dahsyat membangunkan para raksasa purba dari tidur panjang mereka."),
            3 to ("Kaito channels a blinding blue Kamehameha with open mouth shouting in intense focus, colliding violently with Golden Frieza's massive violet energy sphere in a cataclysmic shockwave." to
                "Puncak pertarungan meledak dalam benturan energi murni yang menentukan nasib dunia."),
            4 to ("Kaito stands victorious amidst the settling dust and quieted wasteland, his chest heaving as the orange fabric of his tattered gi flutters softly in the fading breeze." to
                "Saat badai mereda dan para raksasa kembali tertidur, sang legenda berdiri teguh di atas reruntuhan."),
            5 to ("Kaito surveys the storm-scarred wasteland, his breath visible and his orange gi rippling in the lingering wind." to
                "Kaito berdiri di antara puing-puing, menatap langit yang perlahan kembali tenang setelah badai mengamuk."),
            6 to ("The Golden Frieza releases a crackling violet pulse, his cape flaring as the energy distorts the air around him." to
                "Meskipun terkalahkan, kekuatan Frieza masih bergetar, mengancam untuk memicu kembali konflik yang belum selesai."),
            7 to ("Kaito unleashes a storm-surge strike, channeling lightning through his fists while his hair whips wildly, his expression fierce with defiance." to
                "Dengan tekad yang tak tergoyahkan, Kaito menyalurkan badai ke dalam serangan, menutup celah energi jahat yang tersisa."),
            8 to ("Kaito watches the crumbling stone guardians, his silhouette outlined as the clouds part and the sky brightens, dust swirling around his feet." to
                "Saat batu-batu kuno runtuh, dunia mulai pulih; Kaito menjadi saksi kelahiran kembali tanah yang pernah terperangkap dalam badai."),
            9 to ("Kaito stomps the cracked ground with furious weight, lifting both palms to unleash a searing blue energy crescent." to
                "Ketegangan mencapai puncaknya saat energi dahsyat mulai dilepaskan."),
            10 to ("Golden Frieza opens his mouth wide to shout in furious defiance, thrusting forward a colossal sphere of violet energy with trembling hands." to
                "Sang tiran membalas dengan kekuatan penghancur yang mengerikan."),
            11 to ("Kaito and Golden Frieza push their palms outward simultaneously as massive beams collide, their muscles straining and hair whipping violently in the shockwave." to
                "Benturan dua kekuatan legendaris mengguncang seluruh semesta."),
            12 to ("Kaito sweeps his leg in a low spin kick, grazing the ground while dust billows around his blue sash." to
                "Di tengah kancah pertempuran yang membara, Kaito melancarkan serangan bawah untuk memecah pertahanan musuh."),
            13 to ("Kaito steadies his stance, his orange gi fluttering in the wind, his eyes narrowed in defiant focus." to
                "Di tengah badai yang mengamuk, Kaito menyiapkan diri untuk pertarungan yang menentukan nasib dunia."),
            14 to ("Golden Frieza crashes down, his golden armor shimmering, and hurls a searing plasma spear while his cape ripples in the gale." to
                "Dengan kilau emas yang memancar, sang tiran mengumumkan dominasi atas tanah yang hancur."),
            15 to ("Kaito spins, delivering a thunderous palm strike that shatters a stone guardian, his hair whipping in the gale." to
                "Benteng batu kuno bangkit, namun serangan Kaito menembus pertahanan mereka satu per satu."),
            16 to ("Kaito channels a blinding Kamehameha, his aura blazing orange, his breath huffing heavily, and releases the beam toward Frieza's violet energy field." to
                "Saat energi biru dan oranye bertabrakan, ledakan cahaya menentukan akhir pertarungan antara dua legenda."),
            17 to ("Kaito unleashes a spiraling orange vortex, his gi flapping violently as the beam collides with Golden Frieza's violet aura, shattering monoliths." to
                "Setelah ledakan menggemparkan, Kaito berdiri di antara puing-puing, menatap langit yang kini mulai tenang. Kemenangan ini menandai akhir era tirani alien di tanah yang terpuruk."),
            18 to ("Kaito raises his fists in triumph, his hair whipping in the gentle breeze as the storm recedes." to
                "Dengan badai yang mereda, Kaito mengamankan tanah yang dulu dipenuhi kekacauan. Kedamaian kini kembali mengisi ruang yang pernah dipenuhi amarah.")
        )
        return rows.map { (num, pair) -> scenePrompt(plan(num, pair.first, pair.second)) }
    }

    private fun mentionedScenes(failures: List<String>): Set<Int> =
        failures.mapNotNull { Regex("Scene (\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }.toSet()

    // --- Film lapangan verbatim ---

    @Test
    fun bimaFilmFailsTwelveScenes() {
        val g7 = ValidationGates.runArcGate(bimaBible(), bimaFilm())
        assertFalse(g7.failures.toString(), g7.passed)
        val scenes = mentionedScenes(g7.failures)
        assertEquals(
            setOf(3, 4, 5, 7, 9, 10, 11, 12, 13, 15, 17, 18), scenes
        )
        // S1, S2, S6, S8, S14, S16 lolos — S14 "final" di Klimaks BOLEH,
        // S16 resolusi di Penutup BOLEH.
        assertFalse(scenes.contains(1))
        assertFalse(scenes.contains(14))
        assertFalse(scenes.contains(16))
        // Kurva: pra-Klimaks 12 (S3) > Klimaks 3 (S15); Penutup 5 (S17).
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("puncak pra-Klimaks (12) mengalahkan Klimaks (3)") })
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Penutup (5) setinggi/melebihi Klimaks (3)") })
    }

    @Test
    fun bimaScene3FailsFourRules() {
        val g7 = ValidationGates.runArcGate(bimaBible(), bimaFilm())
        val s3 = g7.failures.filter { it.contains("Scene 3") }
        assertTrue(s3.toString(), s3.any { it.contains("klaim finalitas") })
        // Koreksi riset: "shouting in triumph" = klaim resolusi di Pembuka.
        assertTrue(s3.toString(), s3.any { it.contains("klaim resolusi") })
        // v2.1 fix7 — "serangan besar 1x": HANYA S3. S2 BUKAN big attack
        // lagi: "blazing" + "spear" berjarak 23 token (klausa lain) —
        // proximity 6 membunuh noise ini. Kalibrasi fix6 sempat bilang
        // S2 big attack ("trigger + nomina tidak harus sebelahan") —
        // fix7 membalik dengan bukti ukur (serangan asli terjauh 6,
        // noise terdekat 8).
        assertTrue(s3.toString(), s3.any { it.contains("serangan besar 1x (maks 0 per beat)") })
        assertTrue(s3.toString(), s3.any { it.contains("jurus bernama") })
    }

    @Test
    fun bimaScene16And14Pass() {
        val g7 = ValidationGates.runArcGate(bimaBible(), bimaFilm())
        assertFalse(g7.failures.any { it.contains("Scene 16") })
        assertFalse(g7.failures.any { it.contains("Scene 14") })
    }

    @Test
    fun kaitoFilmFailsNineScenes() {
        val g7 = ValidationGates.runArcGate(kaitoBible(), kaitoFilm())
        assertFalse(g7.failures.toString(), g7.passed)
        assertEquals(
            setOf(3, 4, 5, 6, 9, 11, 12, 16, 17), mentionedScenes(g7.failures)
        )
        // v2.1 fix8 v2 — S12 BARU dari R9b: film Kaito lama TIDAK punya
        // beat HERO HURT (S12-nya cuma "low spin kick") — temuan asli,
        // bukan artefak tes.
        // Koreksi riset: R6 Kaito GAGAL — Klimaks maks 3 (S14), BUKAN 6.
        // S16 (skor 6) adalah Penutup, bukan Klimaks. Pra-Klimaks maks 7
        // (S9: "mencapai puncaknya" = kata + frasa); Penutup maks 11 (S17).
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("puncak pra-Klimaks (7) mengalahkan Klimaks (3)") })
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Penutup (11) setinggi/melebihi Klimaks (3)") })
    }

    @Test
    fun kaitoScene17FailsClosingFight() {
        val g7 = ValidationGates.runArcGate(kaitoBible(), kaitoFilm())
        assertTrue(
            g7.failures.toString(),
            g7.failures.any { it.contains("Scene 17 (Penutup): masih ada aksi berantem (\"unleashes\")") }
        )
    }

    @Test
    fun kaitoScene18Passes() {
        val g7 = ValidationGates.runArcGate(kaitoBible(), kaitoFilm())
        assertFalse(g7.failures.any { it.contains("Scene 18") })
    }

    // --- Aturan satu-satu (film 18 scene netral + 1 pelanggaran) ---

    private fun neutralFilm(vararg overrides: Pair<Int, V2ScenePlan>): List<V2ScenePrompt> {
        val byNum = overrides.toMap()
        return (1..18).map { n ->
            scenePrompt(
                byNum[n] ?: plan(
                    n,
                    // v2.1 fix8 v2 — S12 WAJIB beat HERO HURT: R9 menuntut
                    // scene HERO_HURT menunjukkan hero terluka; film
                    // "netral" yang lolos semua gerbang harus punya beat
                    // itu. Nama hero di bible ini "Bima" (aksi netral lain
                    // masih menyebut "Kaito" — noise fixture lama, gerbang
                    // R1–R7 tidak membaca nama hero).
                    if (n == 12) "Rudra lands a clean hit — Bima staggers back, blood on his lip, knees buckling, then his eyes harden."
                    else "Kaito walks through the quiet valley, looking at the distant mountains.",
                    "Angin bertiup pelan di lembah yang sunyi."
                )
            )
        }
    }

    @Test
    fun r1FinalityOutsideKlimaksFails() {
        val scenes = neutralFilm(
            1 to plan(1, "Kaito unleashes the final strike against the tyrant.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertFalse(g7.passed)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 1 (Pembuka): klaim finalitas \"final\" di luar Klimaks") })
    }

    @Test
    fun r1FinalityInsideKlimaksPasses() {
        val scenes = neutralFilm(
            14 to plan(14, "Kaito delivers the final blow.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.passed)
    }

    @Test
    fun r2FirstnessAfterOpeningFails() {
        val scenes = neutralFilm(
            9 to plan(9, "Kaito walks forward.", "Benturan pertama mengguncang tanah.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 9 (Eskalasi): klaim awal \"benturan pertama\" setelah Pembuka/Pemicu") })
    }

    @Test
    fun r2FirstLightOfDawnIsNotFirstness() {
        val scenes = neutralFilm(
            9 to plan(9, "Kaito walks forward.", "The first light of dawn breaks over the hills.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.passed)
    }

    @Test
    fun r3ResolutionOutsideClosingFails() {
        val scenes = neutralFilm(
            2 to plan(2, "Kaito stands victorious over the fallen tyrant.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 2 (Pembuka): klaim resolusi \"victorious\" di luar Penutup") })
    }

    @Test
    fun r3ResolutionInsideClosingPasses() {
        val scenes = neutralFilm(
            17 to plan(17, "Kaito raises his fists in triumph.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.passed)
    }

    @Test
    fun r4BigAttackInOpeningFails() {
        val scenes = neutralFilm(
            2 to plan(2, "Kaito hurls a blazing plasma wave at the tyrant.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 2 (Pembuka): serangan besar 1x (maks 0 per beat)") })
    }

    @Test
    fun r4bNamedMoveTwiceFails() {
        val scenes = neutralFilm(
            9 to plan(9, "Kaito unleashes the Tempest Breaker, channeling lightning."),
            14 to plan(14, "Kaito channels the Tempest Breaker once more.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 14: jurus bernama \"tempest breaker\" sudah dipakai di scene 9 (maks 1x per film)") })
    }

    @Test
    fun r4bNamedMoveOutsideClimaxFails() {
        val scenes = neutralFilm(
            2 to plan(2, "Kaito unleashes the Tempest Breaker.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 2 (Pembuka): jurus bernama \"tempest breaker\" di luar Eskalasi/Klimaks") })
    }

    @Test
    fun r5SecondEntranceFails() {
        val scenes = neutralFilm(
            1 to plan(1, "Rudra descends from the dark clouds."),
            5 to plan(5, "Rudra emerges from the shadows onto the battlefield.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 5 (Pemicu): entrance Rudra kedua (sudah masuk di scene 1)") })
    }

    @Test
    fun r5EntranceInLateBeatFails() {
        val scenes = neutralFilm(
            10 to plan(10, "Rudra descends from the dark clouds onto the battlefield.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 10 (Eskalasi): entrance Rudra di luar Pembuka/Pemicu") })
    }

    @Test
    fun r7NarrationClaimsClimaxActionDoesNotFails() {
        val scenes = neutralFilm(
            6 to plan(6, "Kaito walks through the quiet valley.", "Bima menyalurkan energi ke serangan terakhir.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        // Pesan memakai match PERTAMA findFinality ("terakhir" — kata,
        // sebelum frasa "serangan terakhir"; stemmer juga cocok "akhir").
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 6: narasi mengklaim klimaks (\"terakhir\") yang tidak ditunjukkan aksinya") })
    }

    @Test
    fun r7ClosingMustBeClean() {
        val scenes = neutralFilm(
            17 to plan(17, "Bima shouts a fierce challenge at the falling dark titan.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.failures.any { it.contains("Scene 17 (Penutup): masih ada aksi berantem (\"challenge\")") })
    }

    @Test
    fun r6SkippedForNonActionFilm() {
        // Film tanpa big attack / jurus bernama: R6 dilewati (tidak ada
        // kurva yang bisa diukur), tapi R3 tetap berlaku.
        val scenes = neutralFilm(
            2 to plan(2, "Kaito stands victorious.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertFalse(g7.passed)
        assertFalse(g7.failures.toString(), g7.failures.any { it.contains("Kurva intensitas") })
        assertTrue(g7.failures.any { it.contains("klaim resolusi") })
    }

    @Test
    fun dialogueIsNotGated() {
        // Tokoh boleh sesumbar/bohong di dialog — "finally free" di dialog
        // (bukan aksi/narasi) tidak boleh menggagalkan G7.
        val scenes = neutralFilm(
            3 to plan(3, "Kaito walks through the quiet valley.", dialogueEn = "This world is finally free.")
        )
        val g7 = ValidationGates.runArcGate(bimaBible(), scenes)
        assertTrue(g7.failures.toString(), g7.passed)
    }
}
