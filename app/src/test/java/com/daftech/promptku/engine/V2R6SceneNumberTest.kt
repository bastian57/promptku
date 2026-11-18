package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tes v2.2 fix4 — pesan R6 tidak membawa nomor scene, jadi tidak pernah
// tampil di export (temuan lapangan Langit New York 19 Nov 00:57).
// gateSceneNumbers (V2ViewModel) hanya membaca regex "Scene (\d+)" dari
// teks kegagalan; R6 satu-satunya gerbang yang pesannya tanpa "Scene N"
// ("Kurva intensitas: Penutup (10) setinggi/melebihi Klimaks (5)").
// Akibat: R6 sendirian gagal -> stillFailing kosong -> export nol flag +
// status "lolos 7 gerbang" palsu. Korban kedua: vonis "G7-clean" Peak of
// Destiny 18 Nov ikut gugur (inti Peak gagal R6 11 vs 10, ketahuan setelah
// port audit diperbaiki). Obat: kedua pesan R6 membawa nomor scene yang
// skornya paling tinggi di rentangnya.
// (Nama file V2R6SceneNumberTest — V2Fix4Test sudah dipakai fix4 v2.1.)
class V2R6SceneNumberTest {

    // ---------- fixture: film Langit New York verbatim ----------

    private fun nyBible(): StoryBible = StoryBible(
        title = "Gotres vs Golden Friz: Langit New York",
        logline = "Gotres wins after evolving into Super Saiyan 7 above New York.",
        genre = Genre.ACTION,
        characters = listOf(
            V2Character("Gotres", listOf("Gotres"))
        ),
        antagonistName = "Golden Friz",
        antagonistDescEn = "A golden tyrant with glowing red eyes.",
        worldName = "New York City Sky",
        worldDescEn = "Soaring skyscrapers pierce through chaotic storm clouds above the bustling streets of New York City.",
        lightEn = "Dramatic sunset casting fiery orange and purple hues across the turbulent sky and metallic surfaces.",
        ambienceEn = "Deafening sonic booms, crackling energy discharges, and howling wind whipping through towers.",
        beats = listOf(
            V2Beat(0, "Pembuka", "the rivals face off"),
            V2Beat(1, "Pemicu", "first exchange"),
            V2Beat(2, "Eskalasi", "the fight grows"),
            V2Beat(3, "Klimaks", "the final blow"),
            V2Beat(4, "Penutup", "aftermath")
        ),
        episodes = listOf(V2Episode(1, "Gotres vs Golden Friz: Langit New York", "Gotres vs Golden Friz.", 6)),
        durationMinutes = 1,
        dialogueLang = "EN",
        aspectRatio = "9:16"
    )

    private fun nyPlan(
        num: Int,
        action: String,
        narrationId: String = "Narasi per scene."
    ): V2ScenePlan = V2ScenePlan(
        sceneNumber = num, actionEn = action,
        cameraMove = "tracking shot", lensMm = 50, shotSize = "medium", angle = "eye",
        dialogueId = "", dialogueEn = "", audioEn = "wind", effectId = "",
        wantEn = "win", obstacleEn = "Golden Friz", transitionEn = "",
        narrationId = narrationId, dialogueSpeaker = "Gotres"
    )

    private fun flowPrompt(plan: V2ScenePlan, flowText: String = ""): V2ScenePrompt =
        V2ScenePrompt(plan.sceneNumber, plan, flowText, "", "", "")

    // Aksi inti + narasi VERBATIM dari export Langit New York 19 Nov 00:57
    // (aksi inti = rendered minus fragmen; skor port independen: 0, 2, 0,
    // 2, 5, 10 — R6 Penutup 10 vs Klimaks 5).
    private fun nyFilm(): List<V2ScenePrompt> {
        return listOf(
            flowPrompt(
                nyPlan(
                    1,
                    "Golden Friz floats menacingly above the skyline, descending to block Gotres with glowing red eyes radiating lethal energy while harsh wind whips through towers under the dramatic sunset casting fiery orange and purple hues across the turbulent sky and metallic surfaces",
                    "Di atas langit New York yang kelam, ancaman emas bangkit menantang."
                )
            ),
            flowPrompt(
                nyPlan(
                    2,
                    "Gotres strikes Golden Friz with a furious jab, shattering glass on a nearby tower, then spins into a fierce roundhouse kick that rattles metal siding as howling wind whips through towers under the dramatic sunset casting fiery orange and purple hues across the turbulent sky and metallic surfaces",
                    "Pertempuran kecepatan tinggi meledak di antara gedung-gedung tinggi."
                )
            ),
            flowPrompt(
                nyPlan(
                    3,
                    "Gotres bursts upward from the concrete ledge, instantly closing the distance with a high-speed shoulder charge that drives straight into Golden Friz's chest plate. Golden Friz's eyes widen in stark shock as time dilates into a held heartbeat, the impact snapping outward with a deafening crack that sends shockwaves rippling across the towering glass facades of New York City",
                    "Ketegangan melambung tinggi di udara saat benturan keras mengguncang angkasa."
                )
            ),
            flowPrompt(
                nyPlan(
                    4,
                    "Golden Friz immediately answers the strike by whipping around with a vicious tail swipe that slams squarely into Gotres's side, sending him stumbling backward and reeling through the air. Gotres clutches his bruised ribs tightly, his jaw sets hard with grim endurance, and he instantly surges upward again to maintain the relentless aerial assault",
                    "Serangan balasan yang mematikan menguji ketahanan fisik di tengah badai."
                )
            ),
            flowPrompt(
                nyPlan(
                    5,
                    "Gotres unleashes the final, decisive blow of Super Saiyan 7 energy that shatters Golden Friz's armor into dust, sending the tyrant plummeting as time holds for a heartbeat under the dramatic sunset casting fiery orange and purple hues across the turbulent sky and metallic surfaces",
                    "Kekuatan mutlak meluluhlantakkan sang tiran dalam detik penentuan."
                )
            ),
            flowPrompt(
                nyPlan(
                    6,
                    "Gotres hovers motionless with a triumphant expression, mouth slightly open in a relaxed exhale as golden energy ripples outward into the atmosphere, while the glowing debris of the defeated enemy fades away into nothingness above the skyscrapers",
                    "Pertempuran telah usai, dan kedamaian abadi kini menyelimuti langit New York."
                )
            )
        )
    }

    // ---------- fix4: R6 membawa nomor scene ----------

    @Test
    fun r6CloseFailureCarriesClosingSceneNumber() {
        // Kasus lapangan: R6 Penutup 10 vs Klimaks 5 — SATU-SATUNYA
        // kegagalan film ini, dan export menampilkannya NOL karena pesan
        // lama tidak punya "Scene N". Setelah fix, pesannya harus membawa
        // Scene 6 (Penutup) — persis yang dibaca gateSceneNumbers.
        val result = ValidationGates.runArcGate(nyBible(), nyFilm())
        assertFalse(result.failures.toString(), result.passed)
        assertTrue(result.failures.toString(), result.failures.size == 1)
        val msg = result.failures.single()
        assertTrue(msg, msg.contains("Scene 6"))
        assertTrue(msg, msg.contains("Penutup (10) setinggi/melebihi Klimaks (5)"))
        assertTrue(msg, Regex("Scene (\\d+)").containsMatchIn(msg))
    }

    @Test
    fun r6PreFailureCarriesOffendingSceneNumber() {
        // Cabang kedua: puncak pra-Klimaks mengalahkan Klimaks. Jangkarnya
        // scene yang skornya paling tinggi SEBELUM Klimaks (S2, Pemicu) —
        // bukan scene Penutup, supaya retry G6 menulis ulang scene yang
        // benar (yang kelewat kuat), bukan Penutup yang tidak bersalah.
        val scenes = listOf(
            flowPrompt(nyPlan(1, "Gotres stands on a rooftop, watching the city below.", "Angin malam berhembus pelan.")),
            flowPrompt(nyPlan(2, "Gotres unleashes a blazing plasma bolt at Golden Friz.", "Energi dahsyat meledak di udara.")),
            flowPrompt(nyPlan(3, "Gotres ducks under a swinging cable, rolling across the rooftop.", "Pertarungan terus bergerak.")),
            flowPrompt(
                nyPlan(4, "Golden Friz strikes Gotres, sending him stumbling backward across the rooftop.", "Serangan balasan mengguncang.")
            ),
            flowPrompt(nyPlan(5, "Gotres stands still, watching the horizon.", "Angin berhembus pelan di kejauhan.")),
            flowPrompt(nyPlan(6, "Gotres breathes slowly as the city lights flicker below.", "Angin berhembus pelan di antara gedung-gedung."))
        )
        val result = ValidationGates.runArcGate(nyBible(), scenes)
        assertFalse(result.failures.toString(), result.passed)
        val msg = result.failures.firstOrNull { it.contains("puncak pra-Klimaks") }
        assertTrue(result.failures.toString(), msg != null)
        assertTrue(msg!!, msg.contains("Scene 2"))
        assertTrue(msg, msg.contains("puncak pra-Klimaks (3) mengalahkan Klimaks (0)"))
        assertTrue(msg, Regex("Scene (\\d+)").containsMatchIn(msg))
    }

    @Test
    fun r6PassesWhenClosingIsCalm() {
        // Arah negatif: film NY yang sama dengan Penutup yang ditenangkan
        // (tanpa kata resolusi di aksi maupun narasi) harus lolos G7 penuh
        // — pesan R6 tidak boleh muncul kalau kurvanya benar.
        val calm = nyFilm().map { scene ->
            if (scene.sceneNumber == 6) {
                flowPrompt(
                    nyPlan(
                        6,
                        "Gotres hovers motionless, breathing slowly as the city lights flicker below.",
                        "Angin berhembus pelan di antara gedung-gedung."
                    )
                )
            } else scene
        }
        val result = ValidationGates.runArcGate(nyBible(), calm)
        assertTrue(result.failures.toString(), result.passed)
        assertFalse(result.failures.toString(), result.failures.any { it.contains("Kurva intensitas") })
    }
}
