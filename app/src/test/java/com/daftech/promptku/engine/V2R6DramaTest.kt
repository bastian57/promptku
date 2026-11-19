package com.daftech.promptku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.2 fix5 — kalibrasi R6 buat DRAMA (kajian 19 Nov 13:17, gas 13:23).
// Film 'Dibalik Jubah Debu' (DRAMA, 18 scene, Mode A): klimaks verbal
// (kartu S13 + pemecatan S14) tanpa big attack/fight verb, Penutup menumpuk
// resolusi (S18 = 6 item = 12). Di bawah skor lama (V0, film fight) R6
// gagal 12 vs 4 padahal alur cerita benar; di bawah skor baru (V5, film
// non-fight: finalitas + reveal saja, tanpa resolusi) R6 lolos 4 vs 0.
// Aksi inti + narasi VERBATIM dari export 19 Nov 12:41 (aksi inti =
// isi segmen [00:00-00:04]; kalimat material template "dust drifts..."
// BUKAN actionEn — tidak masuk fixture).
class V2R6DramaTest {

    private fun jubahBible(genre: Genre): StoryBible = StoryBible(
        title = "Dibalik Jubah Debu",
        logline = "Seorang pengemis yang ternyata CEO perusahaan besar.",
        genre = genre,
        characters = listOf(
            V2Character("Pak Joko", listOf("wrinkled face", "white stubble", "gentle sharp eyes"))
        ),
        antagonistName = "Aris",
        antagonistDescEn = "The ruthless corporate director who exploits employees and humiliates the poor outside the headquarters.",
        worldName = "Metropolis Glass Towers",
        worldDescEn = "Glittering skyscrapers tower over dusty concrete sidewalks where luxury sports cars glide past humble street beggars.",
        lightEn = "Harsh afternoon sunlight casting sharp shadows between massive glass skyscrapers",
        ambienceEn = "Loud city traffic honking, engine revs, and distant urban chatter echoing off concrete walls",
        beats = listOf(
            V2Beat(0, "Pembuka", "the beggar waits"),
            V2Beat(1, "Pemicu", "the director humiliates"),
            V2Beat(2, "Eskalasi", "the confrontation grows"),
            V2Beat(3, "Klimaks", "the reveal"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Dibalik Jubah Debu", "Seorang pengemis yang ternyata CEO perusahaan besar.", 18)),
        durationMinutes = 3,
        dialogueLang = "ID",
        aspectRatio = "9:16",
        sourceIdea = "Seorang pengemis yang ternyata CEO perusahaan besar."
    )

    private fun jubahPlan(
        num: Int,
        action: String,
        narrationId: String,
        dialogueId: String = ""
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "static locked camera", lensMm = 35, shotSize = "medium", angle = "eye",
        dialogueId = dialogueId, dialogueEn = "", audioEn = "city traffic", effectId = "",
        wantEn = "reveal", obstacleEn = "Aris", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = "Pak Joko"
    )

    private fun flowPrompt(plan: V2ScenePlan): V2ScenePrompt =
        V2ScenePrompt(plan.sceneNumber, plan, "", "", "", "")

    private fun jubahFilm(): List<V2ScenePrompt> {
        return listOf(
            flowPrompt(
                jubahPlan(
                    1,
                    "Pak Joko sits on the dusty concrete sidewalk, his wrinkled face turned toward the glittering skyscrapers, clutching a worn cardboard bowl; his breath fogs in the harsh afternoon sunlight as a stray wind lifts the edge of his tattered coat",
                    "Di antara gedung kaca yang menjulang, seorang pengemis tua menunggu harapan di trotoar berdebu, tak menyadari bahwa ia berada di depan pintu kantornya sendiri."
                )
            ),
            flowPrompt(
                jubahPlan(
                    2,
                    "Aris emerges from the luxury glass doors, his polished shoes clicking, his crisp suit reflecting the sunlight; his tie flutters as he scans the crowd",
                    "Aris, direktur utama yang dikenal kejam, melangkah keluar, menatap dunia di bawahnya dengan tatapan menilai."
                )
            ),
            flowPrompt(
                jubahPlan(
                    3,
                    "Aris leans forward, lips parting as he snarls, 'Kamu tidak pantas berada di sini,' while Pak Joko lifts his head, eyes steady, his mouth opening in defiant whisper, 'Saya menunggu.'",
                    "Ketegangan muncul ketika sang direktur menatap sang pengemis, menandai awal ujian kesetiaan.",
                    "Kamu tidak pantas berada di sini."
                )
            ),
            flowPrompt(
                jubahPlan(
                    4,
                    "Aris walks forward on the dusty concrete sidewalk, lifting a hand to contemptuously shove Pak Joko's shoulder. Pak Joko stumbles back slightly, his wrinkled face displaying calm patience amidst the harsh afternoon sunlight casting sharp shadows",
                    "Aris mulai melampiaskan arogansinya di hadapan sang pengemis tua.",
                    "Pergi dari sini!"
                )
            ),
            flowPrompt(
                jubahPlan(
                    5,
                    "Aris waves his arm sharply downward, pointing a trembling finger at the concrete floor. His silk tie flutters in the urban wind as employees watch from behind the revolving glass doors with wide eyes",
                    "Tekanan demi tekanan ditebar demi meruntuhkan martabat si tua."
                )
            ),
            flowPrompt(
                jubahPlan(
                    6,
                    "Pak Joko lifts his chin slightly higher, his cotton tunic fabric rippling against the dusty breeze. His hands rest flat on the pavement, steady and unbothered by the escalating hostility surrounding him",
                    "Namun sang pengemis tua tetap berdiri dalam diam yang membingungkan."
                )
            ),
            flowPrompt(
                jubahPlan(
                    7,
                    "Aris points a finger down at Pak Joko, his lips parted as he shouts an insult, while glittering skyscrapers tower overhead in the background and luxury sports cars glide past",
                    "Ketegangan terus meningkat di trotoar di depan gedung pencakar langit.",
                    "Kamu tidak pantas di sini!"
                )
            ),
            flowPrompt(
                jubahPlan(
                    8,
                    "Pak Joko blocks Aris’s aggressive shove, then thrusts his right palm forward, sweeping Aris’s torso and sending the director stumbling backward as his suit jacket flutters; Pak Joko’s steady breathing steadies the moment",
                    "Ketegangan meningkat saat Pak Joko menahan serangan pertama Aris. Penonton merasakan getaran kekuatan tersembunyi di antara gedung-gedung tinggi."
                )
            ),
            flowPrompt(
                jubahPlan(
                    9,
                    "Pak Joko lunges forward, mouth wide open shouting, ‘Kau takkan mengintimidasi saya!’, and delivers two rapid jabs to Aris’s chest, each jab driving the director further back while his breath fogs in the harsh sunlight",
                    "Suara perintah Pak Joko memecah keheningan, menandai pergeseran kekuasaan di antara dua sosok. Langkahnya yang cepat menambah tekanan pada Aris.",
                    "Kau takkan mengintimidasi saya!"
                )
            ),
            flowPrompt(
                jubahPlan(
                    10,
                    "Pak Joko activates his black titanium CEO ID card, unleashing the named special move ‘Titanium Surge’; a blinding arc of metallic energy erupts, slamming into Aris, cracking his polished cufflinks and scattering sparks as his coat ripples violently",
                    "Titanium Surge mengubah arena menjadi medan energi, menyoroti tekad Pak Joko yang tak tergoyahkan. Aris terkejut oleh kekuatan yang tak terduga."
                )
            ),
            flowPrompt(
                jubahPlan(
                    11,
                    "Aris, fury blazing, swings his metal briefcase overhead, but Pak Joko ducks low, his hair rippling, then snaps a swift low kick, striking the briefcase’s base, sending it clattering away while Aris reels from the impact",
                    "Aris mencoba melawan dengan keberanian yang terdesak, namun gerakan Pak Joko tetap lebih terarah. Benturan singkat menandai perubahan arah konflik."
                )
            ),
            flowPrompt(
                jubahPlan(
                    12,
                    "Aris backs toward the security guards, panting, eyes darting for backup, while Pak Joko stands rooted, shoulders squared, his gaze fixed on the director, the city’s harsh afternoon light glinting off his weathered hands",
                    "Dengan keamanan menunggu, Aris mencari dukungan sementara Pak Joko menatapnya tanpa goyah. Ketegangan tetap menggantung, menyiapkan langkah selanjutnya."
                )
            ),
            flowPrompt(
                jubahPlan(
                    13,
                    "Pak Joko stands up slowly on the dusty concrete sidewalk, reaching into his pocket with steady hands to pull out a sleek black titanium CEO ID card that gleams under the harsh afternoon sunlight",
                    "Di titik balik ini, kebenaran mutlak terungkap di hadapan semua orang.",
                    "Kenali kartu ini."
                )
            ),
            flowPrompt(
                jubahPlan(
                    14,
                    "Pak Joko holds the black titanium card high, his mouth wide open speaking with commanding authority, while Aris stares in absolute horror and disbelief as his corporate armor shatters completely",
                    "Serangan pamungkas berupa keputusan pemecatan menjatuhkan sang direktur congkak.",
                    "Kamu saya pecat detik ini juga!"
                )
            ),
            flowPrompt(
                jubahPlan(
                    15,
                    "Aris stumbles backward in absolute shock and ruin on the dusty concrete sidewalk, his face pale and eyes wide, while glittering skyscrapers tower over the scene under harsh afternoon sunlight",
                    "Kejatuhan sang antagonis mencapai puncaknya di depan pintu lobi megah."
                )
            ),
            flowPrompt(
                jubahPlan(
                    16,
                    "Pak Joko strides toward the grand private elevator, his white stubble glinting in the harsh afternoon light, breath steady, his hand resting on the polished metal door as he pauses, lips parting, voice calm, saying, “Kita melangkah ke masa depan.”",
                    "Setelah keputusan tegas, kota kembali bernafas lega, menatap masa depan yang damai.",
                    "Kita melangkah ke masa depan."
                )
            ),
            flowPrompt(
                jubahPlan(
                    17,
                    "Aris lies face-down on the dusty concrete, his torn suit fluttering, eyes closed, breath shallow, while nearby employees lower their heads, some whispering, the ambient city noise softened, a sense of victorious peace settling over the crowd",
                    "Karyawan menyadari keadilan telah ditegakkan, hati mereka tenang."
                )
            ),
            flowPrompt(
                jubahPlan(
                    18,
                    "Pak Joko calmly walks into his grand private elevator as the mirrored doors close smoothly, leaving the dusty concrete sidewalk behind in absolute peace as luxury sports cars glide past",
                    "Kemenangan telah usai diraih, menyisakan ketenangan mutlak bagi sang pemilik sejati."
                )
            )
        )
    }

    // ---------- fix5: R6 buat DRAMA ----------

    @Test
    fun dramaFilmIsNotFightFilm() {
        // Premis gerbang V5: film ini DRAMA dan idenya tidak memicu
        // deteksi fight — jadi R6 memakai skor non-fight.
        assertFalse(BeatMap.isFightFilm(jubahBible(Genre.DRAMA)))
        assertTrue(BeatMap.isFightFilm(jubahBible(Genre.ACTION)))
    }

    @Test
    fun revealLexiconFindsTerungkap() {
        // Sinyal baru V5: "terungkap" di narasi S13. Frasa "kebenaran
        // terungkap" TIDAK kena — di teks verbatim ada kata "mutlak" di
        // tengahnya ("kebenaran mutlak terungkap").
        val found = ArcLexicon.findReveal(
            "Di titik balik ini, kebenaran mutlak terungkap di hadapan semua orang."
        )
        assertEquals(listOf("terungkap"), found)
        assertTrue(ArcLexicon.findReveal("Pak Joko walks slowly into the elevator.").isEmpty())
    }

    @Test
    fun intensityScoresMatchCalibration() {
        // Angka kalibrasi dari simulasi (sim-r6-drama.py, teks verbatim):
        // S13 = 3 (named "ceo id" 1 + reveal "terungkap" 2) di kedua skor;
        // S15 = 4 (finalitas "puncak" + "mencapai puncaknya") di kedua skor;
        // S18 = 12 dengan resolusi, 0 tanpa resolusi (6 item resolusi).
        val bible = jubahBible(Genre.DRAMA)
        val film = jubahFilm()
        val s13 = film[12].plan
        val s15 = film[14].plan
        val s18 = film[17].plan
        assertEquals(3, ArcLexicon.intensityScore(s13.actionEn, s13.narrationId, bible))
        assertEquals(3, ArcLexicon.intensityScore(s13.actionEn, s13.narrationId, bible, countResolution = false))
        assertEquals(4, ArcLexicon.intensityScore(s15.actionEn, s15.narrationId, bible))
        assertEquals(4, ArcLexicon.intensityScore(s15.actionEn, s15.narrationId, bible, countResolution = false))
        assertEquals(12, ArcLexicon.intensityScore(s18.actionEn, s18.narrationId, bible))
        assertEquals(0, ArcLexicon.intensityScore(s18.actionEn, s18.narrationId, bible, countResolution = false))
    }

    @Test
    fun jubahDebuPassesR6UnderDramaScoring() {
        // Kasus lapangan: film DRAMA yang sama persis dengan yang digas
        // di export (R6 12 vs 4) harus LOLOS R6 di bawah skor V5 —
        // Klimaks 4 (S13=3, S14=2, S15=4) vs Penutup 0.
        // Tiga kegagalan lapangan LAINNYA tetap ada (bukan R6): R7-narasi
        // S14 ("pamungkas") + S15 ("puncak") dan R4b S13 ("ceo id").
        val result = ValidationGates.runArcGate(jubahBible(Genre.DRAMA), jubahFilm())
        assertFalse(result.failures.toString(), result.failures.any { it.contains("Kurva intensitas") })
        assertEquals(result.failures.toString(), 3, result.failures.size)
        val joined = result.failures.joinToString(" | ")
        assertTrue(joined, joined.contains("pamungkas"))
        assertTrue(joined, joined.contains("puncak"))
        assertTrue(joined, joined.contains("ceo id"))
    }

    @Test
    fun jubahDebuFailsR6UnderFightScoring() {
        // Regresi: film yang SAMA dengan bible ACTION (skor lama V0)
        // harus tetap gagal R6 dengan angka persis seperti di export
        // lapangan — "Penutup (12) setinggi/melebihi Klimaks (4)".
        // Ini membuktikan fixture-nya mereproduksi kegagalan asli,
        // bukan lolos karena fixture yang dilemahkan.
        val result = ValidationGates.runArcGate(jubahBible(Genre.ACTION), jubahFilm())
        val msg = result.failures.firstOrNull { it.contains("Kurva intensitas") }
        assertTrue(result.failures.toString(), msg != null)
        assertTrue(msg!!, msg.contains("Penutup (12) setinggi/melebihi Klimaks (4)"))
        assertTrue(msg, Regex("Scene (\\d+)").containsMatchIn(msg))
    }
}
