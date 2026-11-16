package com.daftech.promptku.engine

// Galeri Template v1.4.2 — 34 template siap pakai (14 v1.0.1 + 19 v1.4.0 + 1 v1.4.2),
// semua ditulis orisinal mengikuti pola struktur v2 (bukan salinan prompt
// kurator). Setiap template mengisi SEMUA field wizard, jadi skornya 10/10.
// Keluarga Superhuman Fight (3) = pola studi kasus Bejo 15 Nov 2026:
// beat per 2 detik + efek fisik + kamera orbit + klimaks di akhir.
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

    // v1.4.2 — cerita kurir + chip (pool Action lama v1.0–v1.4.1) pindah
    // ke sini sebagai TEMPLATE EKSPLISIT: hanya dipakai kalau user
    // memilihnya di tab Template, atau kalau kalimat Mode Cepat memang
    // tentang kurir/paket/chip (ScriptPools.isCourierStory). Bukan lagi
    // default tersembunyi semua film Action.
    fun kisahKurir(): GalleryTemplate {
        return GalleryTemplate(
            id = "kisah-kurir-01",
            titleId = "Kisah Kurir — Paket Misterius",
            noteId = "Template kisah kurir + chip (pool Action lama). Pilih ini kalau ceritamu memang tentang kurir — kalau tidak, pakai Mode Cepat dan biarkan efek yang menulis ceritanya.",
            input = ShotInput(
                title = "Kisah Kurir — Paket Misterius",
                genre = Genre.ACTION,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Raka",
                    "a young courier in his twenties, lean build, worn jacket, tired eyes"
                ),
                world = WorldLock(
                    "Kota Tua",
                    "an old port city at dusk, crumbling warehouses, neon signs, rain-slicked streets"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.NEON,
                actionEn = "opens the package and finds a glowing chip inside",
                poseStart = "standing at the door, package in hand",
                poseEnd = "staring at the glowing chip, hands shaking",
                materialEn = "rain on the wet street, neon reflections in puddles",
                audioEn = "distant traffic, rain, a low synth hum",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        )
    }

    val all: List<GalleryTemplate> = listOf(
        gokuVsCell(),
        dbzKotaHancur(),
        kisahKurir(),
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
        ),
        // v1.4.0 — Keluarga Superhuman Fight (pola Bejo, 15 Nov 2026):
        // beat per 2 detik + efek fisik + kamera orbit + klimaks di akhir.
        GalleryTemplate(
            id = "superhuman-duel-langit",
            titleId = "Superhuman — Duel di Langit",
            noteId = "Pola Bejo: 1v1 aerial, beat 2 detik, shockwave tiap impact, orbit, klimaks kepalan tangan. Tanpa kata mitos.",
            input = ShotInput(
                title = "Duel di Langit",
                genre = Genre.ACTION,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Bejo",
                    "a 1.8-meter-tall Indonesian man, short black hair, calm focused face, plain dark t-shirt, worn jeans, bare knuckles"
                ),
                secondCharacter = CharacterDna(
                    "Raksasa",
                    "a 2.8-meter-tall superhuman, broad shoulders, torn dark armor, heavy boots, cold eyes"
                ),
                world = WorldLock(
                    "Langit kota",
                    "high above a metropolitan city at noon, between glass skyscrapers, thin clouds, sunlight cutting through the towers"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.ORBIT,
                light = LightKind.VOLUMETRIC,
                actionEn = "pursues his foe through thin clouds with a speed burst, lands a powerful right cross that sends a ring-shaped shockwave outward, then chains a left hook, right body punch and uppercut as the camera orbits around them, catches a kick, spins and lands a devastating spinning back kick, then hovers still, fist clenched, air compressing around his knuckles before a final maximum-power straight punch",
                poseStart = "flying fast after his foe, fist pulled back",
                poseEnd = "hovering still, right fist clenched forward",
                materialEn = "dust and concrete fragments drift outward in a ring, clothes ripple from the pressure, windows tremble",
                audioEn = "rushing wind, deep impact booms, glass trembling, distant city hum",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "superhuman-melawan-raksasa",
            titleId = "Superhuman — Melawan Raksasa",
            noteId = "Underdog vs raksasa 2,8 m: menghindar sentimeter demi sentimeter, lalu membalik tempo.",
            input = ShotInput(
                title = "Melawan Raksasa",
                genre = Genre.ACTION,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Bejo",
                    "a small determined fighter, 1.7 meters tall, torn jacket, wrapped fists, bruised cheek, defiant eyes"
                ),
                secondCharacter = CharacterDna(
                    "Raksasa Batu",
                    "a 2.8-meter-tall giant, massive arms, cracked stone skin, heavy iron boots, slow cold stare"
                ),
                world = WorldLock(
                    "Alun-alun hancur",
                    "a ruined city square at dusk, toppled pillars, cracked ground, smoke rising, fires glowing far away"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.LOW,
                lensMm = 24,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "dodges a massive falling fist by a few centimeters, rolls under the giant's arm, climbs onto its back and strikes the neck three times fast, then leaps off as the giant stumbles forward and crashes into a pillar that shatters around them",
                poseStart = "standing small before the giant, fists raised",
                poseEnd = "landing in a crouch as the pillar collapses behind him",
                materialEn = "stone dust explodes from the pillar, cracks spread across the ground, debris rains down slowly",
                audioEn = "deep footsteps, stone cracking, heavy breathing, distant rumble",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "superhuman-amukan-jalanan",
            titleId = "Superhuman — Amukan di Jalanan",
            noteId = "Superhuman brawl level jalanan: neon, hujan, aspal basah, mobil jadi properti.",
            input = ShotInput(
                title = "Amukan di Jalanan",
                genre = Genre.ACTION,
                style = VisualStyle.CYBERPUNK,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Srikandi",
                    "a young woman with a shaved head, glowing tattoos, black combat gear, bare feet on wet asphalt"
                ),
                world = WorldLock(
                    "Jalan neon",
                    "a neon-lit street at night, rain-slicked surfaces, holographic signs, steam vents, parked cars"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.NEON,
                actionEn = "sprints down the wet street, vaults over a car hood, lands a double-palm impact that throws two attackers backward, spins low and sweeps the third off his feet, then stops and cracks her knuckles as rain steams off her glowing tattoos",
                poseStart = "running low between parked cars, rain on her face",
                poseEnd = "standing still in the rain, knuckles cracked, breathing steady",
                materialEn = "rain sprays from the asphalt, neon reflections streak past, steam rises from the vents",
                audioEn = "rain, footsteps, impacts, distant hover traffic, synth hum",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "drama-pulang-kampung",
            titleId = "Drama — Pulang Kampung",
            noteId = "Drama keluarga: satu senyum setelah perjalanan panjang, tanpa dialog.",
            input = ShotInput(
                title = "Pulang Kampung",
                genre = Genre.DRAMA,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Budi",
                    "a man in his thirties, worn backpack, batik shirt, tired hopeful eyes"
                ),
                world = WorldLock(
                    "Jalan desa",
                    "a village road at dawn, rice fields on both sides, mist, a small wooden house in the distance"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "walks slowly down the road, stops, then smiles as an old woman waves from the doorway",
                poseStart = "walking with a heavy backpack, head bowed",
                poseEnd = "standing still, smiling, arms opening",
                materialEn = "mist drifts over the rice fields, dust rises from his shoes",
                audioEn = "roosters, soft wind, distant prayer call",
                dialogueName = "Budi",
                dialogueLine = "I am home, Ibu.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "drama-surat-terakhir",
            titleId = "Drama — Surat Terakhir",
            noteId = "Drama diam: surat dibaca dua kali, lalu dipeluk. Lilin = cahaya bernama.",
            input = ShotInput(
                title = "Surat Terakhir",
                genre = Genre.DRAMA,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Ibu",
                    "an old woman with grey hair in a bun, faded kebaya, reading glasses, trembling hands"
                ),
                world = WorldLock(
                    "Ruang tamu",
                    "a quiet living room at night, one warm lamp, old photographs on the wall, rain on the window"
                ),
                shotSize = ShotSize.MEDIUM_CLOSEUP,
                angle = CameraAngle.EYE,
                lensMm = 85,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.CANDLE,
                actionEn = "opens the letter slowly, reads it twice, then presses it to her chest with closed eyes",
                poseStart = "sitting by the lamp, letter unopened on her lap",
                poseEnd = "holding the letter to her chest, eyes closed",
                materialEn = "lamplight flickers on the photographs, rain slides down the window",
                audioEn = "rain, paper rustling, a soft sigh, distant thunder",
                dialogueName = "Ibu",
                dialogueLine = "You kept your promise.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        ),
        GalleryTemplate(
            id = "romansa-hujan-pertama",
            titleId = "Romansa — Hujan Pertama",
            noteId = "Romansa: satu payung kecil, dua orang, bahu bersentuhan.",
            input = ShotInput(
                title = "Hujan Pertama",
                genre = Genre.ROMANSA,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Nadia",
                    "a young woman with wet hair, yellow raincoat, shy smile"
                ),
                secondCharacter = CharacterDna(
                    "Raka",
                    "a young man holding a small umbrella, wet shirt, nervous grin"
                ),
                world = WorldLock(
                    "Halte hujan",
                    "a bus stop in light rain at dusk, wet benches, city lights blurring behind"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.REMBRANDT,
                actionEn = "steps closer under the small umbrella and their shoulders touch",
                poseStart = "standing apart under the shelter, rain falling",
                poseEnd = "standing close under one umbrella, smiling",
                materialEn = "raindrops slide off the raincoat, puddles ripple at their feet",
                audioEn = "soft rain, distant traffic, a bus passing",
                dialogueName = "Nadia",
                dialogueLine = "You are soaked too.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "horor-kuntilanak-kebun",
            titleId = "Horor — Kuntilanak di Kebun",
            noteId = "Horor lokal: kebun pisang, kabut, bulan — jangan tunjuk wajahnya, siluet saja.",
            input = ShotInput(
                title = "Kuntilanak di Kebun",
                genre = Genre.HOROR,
                style = VisualStyle.NOIR,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Joko",
                    "a young man with a flashlight, pale face, trembling hands, wet shirt"
                ),
                world = WorldLock(
                    "Kebun pisang",
                    "a banana grove at midnight, thick mist, moonlight between the leaves, an old well"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.MOONLIGHT,
                actionEn = "freezes as a white figure sways between the banana trees, then slowly steps backward",
                poseStart = "walking between the trees, flashlight beam shaking",
                poseEnd = "stepping backward, breath held, eyes wide",
                materialEn = "mist curls around the banana leaves, a white cloth drifts in the wind",
                audioEn = "crickets, a baby crying far away, leaves rustling",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "horor-pocong-gang",
            titleId = "Horor — Pocong di Gang",
            noteId = "WanGP: prompt pendek, gerakan + kamera saja. Satu ketukan takut.",
            input = ShotInput(
                title = "Pocong di Gang",
                genre = Genre.HOROR,
                style = VisualStyle.NOIR,
                engine = TargetEngine.WAN,
                character = CharacterDna(
                    "Andi",
                    "a teenage boy with a bicycle, school uniform, frightened eyes"
                ),
                world = WorldLock(
                    "Gang desa",
                    "a narrow village alley at night, one flickering lamp, closed doors, wet stone"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.HIGH,
                lensMm = 35,
                cameraMove = CameraMove.STATIC,
                light = LightKind.CHIAROSCURO,
                actionEn = "drops his bicycle and runs as a wrapped figure hops after him down the alley",
                poseStart = "riding slowly, looking over his shoulder",
                poseEnd = "running away, bicycle left behind",
                materialEn = "the lamp flickers, shadows jump on the walls, dust rises from the stone",
                audioEn = "silence, then fast footsteps, a bicycle bell ringing alone",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "scifi-kontak-pertama",
            titleId = "Sci-Fi — Kontak Pertama",
            noteId = "Seedance: timeline 3 segmen, tanpa negative prompt. Cahaya turun = klimaks.",
            input = ShotInput(
                title = "Kontak Pertama",
                genre = Genre.SCI_FI,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.SEEDANCE,
                character = CharacterDna(
                    "Dr. Lestari",
                    "a young scientist in a white coat, short curly hair, wide curious eyes"
                ),
                world = WorldLock(
                    "Observatorium",
                    "a rooftop observatory at night, a huge telescope, stars above, city lights below"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.MOONLIGHT,
                actionEn = "looks up from the telescope screen as a bright light descends slowly from the sky",
                poseStart = "bent over the screen, hand on the dial",
                poseEnd = "standing straight, face lit by the descending light",
                materialEn = "dust drifts in the starlight, papers flutter on the desk",
                audioEn = "low hum, soft beeps, wind on the rooftop",
                dialogueName = "Dr. Lestari",
                dialogueLine = "It is answering us.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "fantasi-naga-awan",
            titleId = "Fantasi — Naga di Atas Awan",
            noteId = "Anime: extreme wide supaya naga + puncak + awan masuk satu frame.",
            input = ShotInput(
                title = "Naga di Atas Awan",
                genre = Genre.FANTASI,
                style = VisualStyle.ANIME,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Sari",
                    "a young girl with braided hair, red cloak, wooden sword on her back, brave eyes"
                ),
                world = WorldLock(
                    "Puncak gunung",
                    "a mountain peak above the clouds at sunrise, stone steps, prayer flags, a dragon circling"
                ),
                shotSize = ShotSize.EXTREME_WIDE,
                angle = CameraAngle.LOW,
                lensMm = 24,
                cameraMove = CameraMove.CRANE_UP,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "raises her wooden sword as the great dragon circles down toward her",
                poseStart = "climbing the last stone steps, cloak in the wind",
                poseEnd = "standing tall, sword raised to the sky",
                materialEn = "clouds part around the peak, prayer flags snap in the wind",
                audioEn = "wind, distant roar, flapping wings",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "fantasi-penyihir-muda",
            titleId = "Fantasi — Penyihir Muda",
            noteId = "Kling: fokus tangan — lilin melayang mengelilingi, wajah stabil.",
            input = ShotInput(
                title = "Penyihir Muda",
                genre = Genre.FANTASI,
                style = VisualStyle.ANIME,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Bimo",
                    "a teenage wizard with round glasses, oversized purple robe, messy black hair"
                ),
                world = WorldLock(
                    "Perpustakaan tersembunyi",
                    "a hidden library at night, floating candles, tall shelves, dust in the air"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.CANDLE,
                actionEn = "waves his wand and a dozen candles rise into the air around him",
                poseStart = "standing between the shelves, wand lowered",
                poseEnd = "arms spread, candles floating in a ring",
                materialEn = "dust sparkles in the candlelight, pages flutter on their own",
                audioEn = "soft chimes, paper rustling, a low magical hum",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.SMOKE_FOREGROUND
            )
        ),
        GalleryTemplate(
            id = "thriller-mata-mata",
            titleId = "Thriller — Mata-mata",
            noteId = "Noir: amplop + tempat sampah + pergi tanpa menoleh. Paranoia tanpa dialog.",
            input = ShotInput(
                title = "Mata-mata",
                genre = Genre.THRILLER,
                style = VisualStyle.NOIR,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "Sinta",
                    "a woman in a black coat, sunglasses at night, red lipstick, calm cold eyes"
                ),
                world = WorldLock(
                    "Stasiun tengah malam",
                    "a train station at midnight, empty platforms, fluorescent light, fog outside"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 50,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.CHIAROSCURO,
                actionEn = "slips a small envelope into the trash bin and walks away without looking back",
                poseStart = "standing by the bin, glancing at the clock",
                poseEnd = "walking down the platform, coat trailing",
                materialEn = "fog drifts across the platform, a newspaper page slides along the floor",
                audioEn = "distant train, footsteps, a loudspeaker announcement",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "thriller-penculikan",
            titleId = "Thriller — Penculikan",
            noteId = "Close-up + neon: satu kalimat dialog, sisanya wajah yang tegang.",
            input = ShotInput(
                title = "Penculikan",
                genre = Genre.THRILLER,
                style = VisualStyle.NOIR,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Andi",
                    "a father in a grey suit, sweat on his forehead, phone pressed to his ear, shaking hands"
                ),
                world = WorldLock(
                    "Telepon umum",
                    "a phone booth on a rainy street at night, neon reflections, passing headlights"
                ),
                shotSize = ShotSize.CLOSEUP,
                angle = CameraAngle.EYE,
                lensMm = 85,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.NEON,
                actionEn = "listens in silence, then whispers one word and hangs up",
                poseStart = "standing in the booth, phone ringing",
                poseEnd = "phone down, staring at the rain, jaw tight",
                materialEn = "rain streaks the glass, neon light flickers on his face",
                audioEn = "rain, phone dial tone, a car passing fast",
                dialogueName = "Andi",
                dialogueLine = "How much?",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "komedi-ojek-nyasar",
            titleId = "Komedi — Tukang Ojek Nyasar",
            noteId = "WanGP: komedi fisik satu ketukan, kamera diam supaya leluconnya kebaca.",
            input = ShotInput(
                title = "Tukang Ojek Nyasar",
                genre = Genre.KOMEDI,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.WAN,
                character = CharacterDna(
                    "Mang Udin",
                    "a cheerful ojek driver with a green jacket, helmet under his arm, big moustache"
                ),
                world = WorldLock(
                    "Perempatan siang",
                    "a busy intersection at noon, honking cars, street vendors, harsh sunlight"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.STATIC,
                light = LightKind.OVERCAST,
                actionEn = "looks at his phone, scratches his head, then turns his motorcycle around in the middle of traffic",
                poseStart = "parked at the corner, phone in hand, confused",
                poseEnd = "riding off the wrong way, grinning and waving",
                materialEn = "exhaust smoke puffs behind him, a vendor's umbrella wobbles",
                audioEn = "honking, laughter, street chatter, a dangling bell",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "komedi-mertua-datang",
            titleId = "Komedi — Mertua Datang",
            noteId = "Komedi situasi: wajan gosong disembunyikan di belakang punggung.",
            input = ShotInput(
                title = "Mertua Datang",
                genre = Genre.KOMEDI,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Dedi",
                    "a nervous young husband in an apron, holding a burnt pan, flour on his cheek"
                ),
                world = WorldLock(
                    "Dapur pagi",
                    "a small kitchen in the morning, steam, messy counters, sunlight through the window"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.STATIC,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "hides the burnt pan behind his back as the doorbell rings",
                poseStart = "standing at the stove, pan smoking",
                poseEnd = "smiling at the door, pan hidden behind his back",
                materialEn = "steam curls from the pan, flour dusts the counter",
                audioEn = "doorbell, sizzling, nervous laughter",
                dialogueName = "Dedi",
                dialogueLine = "Ibu! Come in, the food is... almost ready.",
                focusMode = FocusMode.FACE_ONLY,
                hideTactic = HideTactic.BACK_SHOT
            )
        ),
        GalleryTemplate(
            id = "dokumenter-pagi-pasar",
            titleId = "Dokumenter — Pagi di Pasar",
            noteId = "Seedance: observasional, tangan yang bekerja, tanpa drama buatan.",
            input = ShotInput(
                title = "Pagi di Pasar",
                genre = Genre.DOKUMENTER,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.SEEDANCE,
                character = CharacterDna(
                    "Bu Minah",
                    "an old vegetable seller with a headscarf, wrinkled hands, warm tired smile"
                ),
                world = WorldLock(
                    "Pasar tradisional",
                    "a traditional market at dawn, colorful stalls, steam from food carts, morning mist"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.EYE,
                lensMm = 35,
                cameraMove = CameraMove.TRACKING,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "arranges the vegetables in neat rows and calls out to the first buyers",
                poseStart = "unloading baskets from a cart, back bent",
                poseEnd = "standing behind the stall, hands on hips, smiling",
                materialEn = "mist drifts between the stalls, water drips from fresh leaves",
                audioEn = "market chatter, chopping, a rooster far away",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "dokumenter-panen-raya",
            titleId = "Dokumenter — Panen Raya",
            noteId = "Aerial: barisan pemanen + sawah emas + gunung di kejauhan.",
            input = ShotInput(
                title = "Panen Raya",
                genre = Genre.DOKUMENTER,
                style = VisualStyle.REALISTIC,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Pak Slamet",
                    "a farmer with a straw hat, sun-darkened skin, strong arms, white teeth smile"
                ),
                world = WorldLock(
                    "Sawah emas",
                    "a golden rice field at noon, workers in a line, mountains far away"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.HIGH,
                lensMm = 35,
                cameraMove = CameraMove.AERIAL,
                light = LightKind.OVERCAST,
                actionEn = "swings the sickle in rhythm and tosses the cut stalks onto the pile",
                poseStart = "standing in the field, sickle raised",
                poseEnd = "wiping sweat, looking at the full pile with pride",
                materialEn = "golden dust floats in the air, cut stalks fall in neat rows",
                audioEn = "sickle swishes, laughter, wind through the rice",
                focusMode = FocusMode.HANDS_ONLY,
                hideTactic = HideTactic.DISTANCE
            )
        ),
        GalleryTemplate(
            id = "sejarah-diponegoro",
            titleId = "Sejarah — Diponegoro",
            noteId = "Vintage: kabut terbelah, keris terangkat, pasukan bangkit.",
            input = ShotInput(
                title = "Diponegoro",
                genre = Genre.SEJARAH,
                style = VisualStyle.VINTAGE,
                engine = TargetEngine.FLOW,
                character = CharacterDna(
                    "Pangeran Diponegoro",
                    "a Javanese prince in white robes, turban, calm fierce eyes, a keris at his waist"
                ),
                world = WorldLock(
                    "Medan perang",
                    "a battlefield at dawn, mist over the field, banners, distant mountains"
                ),
                shotSize = ShotSize.WIDE,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.CRANE_UP,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "raises his keris toward the sky as his soldiers rise from the mist behind him",
                poseStart = "kneeling in prayer, head bowed",
                poseEnd = "standing tall, keris raised, army behind him",
                materialEn = "mist parts around the soldiers, banners snap in the wind",
                audioEn = "distant drums, horse hooves, wind",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.SILHOUETTE
            )
        ),
        GalleryTemplate(
            id = "sejarah-perang-puputan",
            titleId = "Sejarah — Perang Puputan",
            noteId = "Kling: gamelan berhenti, tombak ditanam, kepala tertunduk.",
            input = ShotInput(
                title = "Perang Puputan",
                genre = Genre.SEJARAH,
                style = VisualStyle.VINTAGE,
                engine = TargetEngine.KLING,
                character = CharacterDna(
                    "I Gusti Ngurah",
                    "a Balinese warrior in white cloth, gold headband, spear in hand, solemn face"
                ),
                world = WorldLock(
                    "Halaman istana",
                    "a palace courtyard at sunrise, stone walls, frangipani trees, smoke in the distance"
                ),
                shotSize = ShotSize.MEDIUM,
                angle = CameraAngle.LOW,
                lensMm = 35,
                cameraMove = CameraMove.DOLLY_IN,
                light = LightKind.GOLDEN_HOUR,
                actionEn = "plants the spear in the ground and bows his head as the gamelan stops",
                poseStart = "walking slowly into the courtyard, spear upright",
                poseEnd = "kneeling by the planted spear, head bowed",
                materialEn = "frangipani petals drift down, incense smoke curls upward",
                audioEn = "gamelan fading, birdsong, a single drum beat",
                focusMode = FocusMode.FULL_STABLE,
                hideTactic = HideTactic.SILHOUETTE
            )
        )
    )

    fun byGenre(genre: Genre): List<GalleryTemplate> {
        return all.filter { it.input.genre == genre }
    }
}
