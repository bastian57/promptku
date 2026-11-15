package com.daftech.promptku.engine

// Galeri Template v1.0.1 — 14 template siap pakai, semua ditulis orisinal
// mengikuti pola struktur v2 (bukan salinan prompt kurator).
// Setiap template mengisi SEMUA field wizard, jadi skornya 10/10.
data class GalleryTemplate(
    val id: String,
    val titleId: String,
    val noteId: String,
    val input: ShotInput
)

object Templates {

    // DNA verbatim dari DANCUK Cinematic Prompt v2 (Goku vs Cell).
    val gokuDna: CharacterDna = CharacterDna(
        name = "Goku",
        descriptionEn = "muscular young man, spiky black hair, torn dirty orange martial arts gi, " +
            "dark blue undershirt, blue sash belt, blue wristbands, black kanji in a circle on chest"
    )

    val cellDna: CharacterDna = CharacterDna(
        name = "Cell",
        descriptionEn = "tall bio-android, green spotted skin, black glossy carapace armor, " +
            "purple face markings, red eyes, tall pointed head-crown, black insect wings on his back, " +
            "yellow boots, cold arrogant smirk"
    )

    val gokuVsCellWorld: WorldLock = WorldLock(
        name = "Arena Cell Games",
        descriptionEn = "the ruined Cell Games arena, a vast stone fighting ring in a desert valley " +
            "at golden hour, cracked pillars and torn tournament banners"
    )

    fun gokuVsCell(): GalleryTemplate {
        return GalleryTemplate(
            id = "goku-vs-cell-01",
            titleId = "Goku vs Cell — Scene 01 Establishing",
            noteId = "Preset dari DANCUK Cinematic Prompt v2. Peringatan: nama berlisensi bisa ditolak Flow — ganti jadi the hero / the bio-android villain kalau ditolak.",
            input = ShotInput(
                title = "Goku vs Cell — Scene 01",
                genre = Genre.ACTION,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = gokuDna,
                secondCharacter = cellDna,
                world = gokuVsCellWorld,
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.CRANE_UP,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "lands hard in a burst of dust, stands up slowly and faces Cell across the ring",
                poseStart = "standing at the far edge of the arena",
                poseEnd = "standing tall, facing Cell",
                materialEn = "dust explodes outward, ground cracks propagate from his boots",
                audioEn = "desert wind, distant crowd murmur fading, low orchestral drone",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        )
    }

    // v1.0.1: template dari video referensi DBZ 8:49 (bedah 15 Nov 2026).
    // BEDA dari goku-vs-cell-01: itu arena Cell Games; ini KOTA HANCUR
    // pasca-apokaliptik + Piccolo sebagai hero. Bar metal + logo di
    // atas-bawah video itu kerjaan edit (CapCut), BUKAN prompt — jangan
    // pernah ditulis di prompt. Nama berlisensi bisa ditolak Flow.
    fun dbzKotaHancur(): GalleryTemplate {
        return GalleryTemplate(
            id = "dbz-kota-hancur",
            titleId = "DBZ — Kota Hancur (Referensi Video)",
            noteId = "Dari video referensi 8:49: kota hancur + golden hour + grade teal-orange. Bar metal atas-bawah itu kerjaan edit (CapCut), bukan prompt. Nama berlisensi bisa ditolak Flow — ganti jadi the green warrior / the bio-android villain kalau ditolak.",
            input = ShotInput(
                title = "Kota Hancur — Piccolo vs Cell",
                genre = Genre.ACTION,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Piccolo",
                    "tall green-skinned Namekian warrior, pointed ears, two antennae, " +
                        "white turban and cape over a purple gi, weighted training clothes, calm fierce eyes"
                ),
                secondCharacter = cellDna,
                world = WorldLock(
                    "Kota hancur",
                    "a ruined post-apocalyptic city at golden hour, collapsed skyscrapers, " +
                        "cracked streets, drifting smoke, fires glowing in the distance"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "stands with his back to camera among the ruins, then turns slowly and faces Cell across the rubble",
                poseStart = "standing with his back to camera, cape moving in the wind",
                poseEnd = "facing Cell, fists clenched, aura rising",
                materialEn = "dust drifts through golden light, smoke curls from cracked streets, a spiral aura rises around him",
                audioEn = "deserted city wind, distant rumble, low orchestral drone",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.BACK_SHOT
            )
        )
    }

    val all: List<GalleryTemplate> = listOf(
        gokuVsCell(),
        dbzKotaHancur(),
        GalleryTemplate(
            id = "drama-hujan-jakarta",
            titleId = "Drama — Hujan di Jakarta",
            noteId = "Drama diam: satu aksi, satu kamera, wajah yang bercerita.",
            input = ShotInput(
                title = "Hujan di Jakarta",
                genre = Genre.DRAMA,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Rani",
                    "a woman in her thirties, wet black hair, plain grey raincoat, tired gentle eyes"
                ),
                world = WorldLock(
                    "Jalanan Jakarta",
                    "a quiet Jakarta street at dusk, rain on asphalt, closed shop fronts, one street lamp"
                ),
                shotSize = ShotSize.MEDIUM_CLOSEUP,
                angle = CameraAngle.EYE,
                lensMm = 85,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.REMBRANDT,
                actionEn = "lowers her head, then looks up with tears in her eyes",
                poseStart = "standing under the street lamp, head bowed",
                poseEnd = "looking up, rain on her face",
                materialEn = "raindrops slide down her coat, puddles ripple around her shoes",
                audioEn = "rain on asphalt, distant traffic, a soft piano note",
                dialogueName = "Rani",
                dialogueLine = "I should have stayed.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        ),
        GalleryTemplate(
            id = "romansa-senja-pantai",
            titleId = "Romansa — Senja di Pantai",
            noteId = "Kling: dialog ditulis Nama (tone): kalimat.",
            input = ShotInput(
                title = "Senja di Pantai",
                genre = Genre.ROMANSA,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Alya",
                    "a young woman with long brown hair, white summer dress, bare feet on sand"
                ),
                secondCharacter = CharacterDna(
                    "Bimo",
                    "a young man with short black hair, linen shirt, rolled sleeves, warm smile"
                ),
                world = WorldLock(
                    "Pantai senja",
                    "a seaside promenade at sunset, golden light on calm water, empty benches"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "reaches out slowly and takes the other hand",
                poseStart = "walking side by side along the water",
                poseEnd = "holding hands, smiling at each other",
                materialEn = "sand shifts under bare feet, sea breeze lifts her hair",
                audioEn = "soft waves, gulls in the distance, gentle guitar",
                dialogueName = "Alya",
                dialogueLine = "Stay a little longer.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "horor-rumah-tua",
            titleId = "Horor — Rumah Tua",
            noteId = "WanGP: prompt pendek, gerakan + kamera saja. Negative Mandarin otomatis.",
            input = ShotInput(
                title = "Rumah Tua",
                genre = Genre.HOROR,
                style = VisualStyle.NOIR,
                engine = TargetEngine.WAN,
                character = CharacterDna(
                    "Sari",
                    "a young woman with a flashlight, pale face, dark coat, trembling hands"
                ),
                world = WorldLock(
                    "Rumah tua",
                    "an old abandoned house at night, peeling walls, dust in the air, one broken window"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.CHIAROSCURO,
                actionEn = "freezes, then turns slowly toward the dark doorway",
                poseStart = "standing in the hallway, flashlight shaking",
                poseEnd = "facing the doorway, breath held",
                materialEn = "dust drifts through the flashlight beam, floorboards creak",
                audioEn = "silence, then a floorboard creak, heartbeat",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "scifi-stasiun-luar-angkasa",
            titleId = "Sci-Fi — Stasiun Luar Angkasa",
            noteId = "Seedance: timeline 3 segmen, tanpa negative prompt.",
            input = ShotInput(
                title = "Stasiun Luar Angkasa",
                genre = Genre.SCI_FI,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.SEEDANCE,
                character = CharacterDna(
                    "Kapten Nadia",
                    "a woman astronaut in a white flight suit, short silver hair, calm focused eyes"
                ),
                world = WorldLock(
                    "Stasiun luar angkasa",
                    "a space station corridor, cold metal walls, blinking lights, Earth visible through a window"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.VOLUMETRIC,
                actionEn = "presses the glowing panel and the door slides open",
                poseStart = "floating still in the corridor, hand near the panel",
                poseEnd = "drifting through the open doorway toward the light",
                materialEn = "dust motes float in the light beam, cables sway gently",
                audioEn = "low station hum, soft beeps, air circulation",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "fantasi-hutan-larangan",
            titleId = "Fantasi — Hutan Larangan",
            noteId = "Anime: gaya cel shading, warna datar yang hidup.",
            input = ShotInput(
                title = "Hutan Larangan",
                genre = Genre.FANTASI,
                style = VisualStyle.ANIME,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Kira",
                    "a young mage with long silver hair, deep blue robe, wooden staff with a glowing crystal"
                ),
                world = WorldLock(
                    "Hutan larangan",
                    "an ancient forest at dawn, mist between tall trees, mossy stones, fireflies"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.CRANE_UP,
                light = LightKind.VOLUMETRIC,
                actionEn = "raises a hand and light gathers in the palm",
                poseStart = "kneeling among mossy stones, staff planted",
                poseEnd = "standing tall, palm glowing bright",
                materialEn = "mist swirls around her robe, leaves drift down",
                audioEn = "birdsong, soft wind through leaves, faint chime",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        ),
        GalleryTemplate(
            id = "thriller-detektif-hujan",
            titleId = "Thriller — Detektif Hujan",
            noteId = "Noir: satu cahaya keras, latar jatuh ke hitam.",
            input = ShotInput(
                title = "Detektif Hujan",
                genre = Genre.THRILLER,
                style = VisualStyle.NOIR,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Detektif Arman",
                    "a man in his forties, grey trench coat, fedora, stubbled jaw, sharp eyes"
                ),
                world = WorldLock(
                    "Gang hujan",
                    "a narrow rainy alley at night, flickering street lamp, wet asphalt, brick walls"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.CHIAROSCURO,
                actionEn = "glances over his shoulder and quickens his pace",
                poseStart = "walking slowly down the alley, collar up",
                poseEnd = "turning the corner, gone from sight",
                materialEn = "rain splashes on asphalt, coat drips water",
                audioEn = "rain, footsteps on wet stone, distant siren",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "komedi-warung-kopi",
            titleId = "Komedi — Warung Kopi",
            noteId = "Komedi: aksi fisik satu ketukan, kamera diam supaya leluconnya kebaca.",
            input = ShotInput(
                title = "Warung Kopi",
                genre = Genre.KOMEDI,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.WAN,
                character = CharacterDna(
                    "Pak Darto",
                    "a cheerful old man with a moustache, batik shirt, sandals, wide grin"
                ),
                world = WorldLock(
                    "Warung kopi",
                    "a busy small coffee shop in the morning, warm light, wooden tables, steaming cups"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.STATIC,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "slips on the wet floor and catches himself, grinning",
                poseStart = "carrying two cups across the shop",
                poseEnd = "standing upright, laughing, cups still safe",
                materialEn = "coffee sloshes but stays in the cups, floor shines wet",
                audioEn = "laughter, clinking cups, morning chatter",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "dokumenter-nelayan-subuh",
            titleId = "Dokumenter — Nelayan Subuh",
            noteId = "Dokumenter: observasional, tanpa drama buatan.",
            input = ShotInput(
                title = "Nelayan Subuh",
                genre = Genre.DOKUMENTER,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.SEEDANCE,
                character = CharacterDna(
                    "Pak Slamet",
                    "an old fisherman with sun-darkened skin, worn cap, faded blue shirt, strong hands"
                ),
                world = WorldLock(
                    "Pelabuhan subuh",
                    "a fishing harbor at dawn, wooden boats, mist over calm water, gulls circling"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "casts the net and watches it sink into the water",
                poseStart = "standing at the bow, net coiled in both hands",
                poseEnd = "watching the net sink, hand shielding his eyes",
                materialEn = "mist drifts over the water, rope uncoils smoothly",
                audioEn = "water lapping, gulls, wooden boat creaks",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "sejarah-majapahit",
            titleId = "Sejarah — Majapahit",
            noteId = "Vintage: film 1970-an, warna pudar, grain halus.",
            input = ShotInput(
                title = "Majapahit",
                genre = Genre.SEJARAH,
                style = VisualStyle.VINTAGE,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Raden Wijaya",
                    "a young king with gold ornaments, white and gold royal cloth, calm proud face"
                ),
                world = WorldLock(
                    "Candi Majapahit",
                    "an ancient stone temple courtyard at sunrise, carved pillars, incense smoke"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.CRANE_UP,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "kneels before the altar and bows his head",
                poseStart = "walking slowly toward the altar, hands folded",
                poseEnd = "kneeling, head bowed in prayer",
                materialEn = "incense smoke curls upward, dust floats in sunbeams",
                audioEn = "gamelan far away, birdsong, temple bell",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "action-kejar-kejaran",
            titleId = "Action — Kejar-kejaran",
            noteId = "Cyberpunk: neon magenta-cyan, jalan basah memantulkan cahaya.",
            input = ShotInput(
                title = "Kejar-kejaran",
                genre = Genre.ACTION,
                style = VisualStyle.CYBERPUNK,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Rex",
                    "a courier with a glowing jacket, short mohawk, augmented arm, determined eyes"
                ),
                world = WorldLock(
                    "Jalan neon",
                    "a neon-lit street at night, rain-slicked surfaces, holographic signs, steam vents"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.NEON,
                actionEn = "sprints down the street and vaults over a barrier",
                poseStart = "crouching at the start line, breath steady",
                poseEnd = "landing past the barrier, running on",
                materialEn = "rain sprays from his shoes, neon reflections streak past",
                audioEn = "rain, footsteps, distant hover traffic, synth hum",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "scifi-gadis-robot",
            titleId = "Sci-Fi — Gadis Robot",
            noteId = "Anime + WanGP: first-frame mengunci desain, prompt hanya gerakan.",
            input = ShotInput(
                title = "Gadis Robot",
                genre = Genre.SCI_FI,
                style = VisualStyle.ANIME,
                engine = TargetEngine.WAN,
                character = CharacterDna(
                    "Aiko",
                    "a robot girl with short white hair, glowing blue eyes, white and silver body panels"
                ),
                world = WorldLock(
                    "Laboratorium",
                    "a quiet laboratory at night, soft blue screens, cables on the floor, one window with rain"
                ),
                shotSize = ShotSize.CLOSEUP,
                angle = CameraAngle.EYE,
                lensMm = 85,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.RIM,
                actionEn = "opens her eyes slowly and looks at her own hand",
                poseStart = "lying still on the table, eyes closed",
                poseEnd = "sitting up, studying her hand with wonder",
                materialEn = "dust drifts in the blue screen light, cables sway slightly",
                audioEn = "soft beeps, rain on the window, quiet hum",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        ),
        GalleryTemplate(
            id = "romansa-senja-cyberpunk",
            titleId = "Romansa — Senja Cyberpunk",
            noteId = "Seedance: audio native — dialog dan SFX ditulis di prompt.",
            input = ShotInput(
                title = "Senja Cyberpunk",
                genre = Genre.ROMANSA,
                style = VisualStyle.CYBERPUNK,
                engine = TargetEngine.SEEDANCE,
                character = CharacterDna(
                    "Mira",
                    "a young woman with neon-pink hair, transparent raincoat, hopeful eyes"
                ),
                secondCharacter = CharacterDna(
                    "Ken",
                    "a young man with dark hair, grey coat, gentle smile"
                ),
                world = WorldLock(
                    "Atap neon",
                    "a rooftop at dusk, neon signs below, rain clouds, city lights stretching far"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.NEON,
                actionEn = "reaches out slowly and takes the other hand",
                poseStart = "standing apart at the rooftop edge, rain falling",
                poseEnd = "standing close, hands joined, smiling",
                materialEn = "rain beads on the raincoat, neon light ripples on wet floor",
                audioEn = "rain, distant city hum, soft synth",
                dialogueName = "Mira",
                dialogueLine = "The city looks kind tonight.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.BACK_SHOT
            )
        )
    )

    fun byGenre(genre: Genre): List<GalleryTemplate> {
        return all.filter { it.input.genre == genre }
    }
}
