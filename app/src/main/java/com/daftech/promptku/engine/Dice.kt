package com.daftech.promptku.engine

import kotlin.random.Random

// Dadu Sutradara — mengacak kombinasi yang masih masuk akal sinematik.
// Pasangan genre-gaya-cahaya dikunci masuk akal (horor tidak dapat neon
// pink, romansa tidak dapat chiaroscuro), supaya hasil acakan langsung
// bisa dipakai, bukan sekadar lucu-lucuan.
object Dice {

    private val genreStyles: Map<Genre, List<VisualStyle>> = mapOf(
        Genre.ACTION to listOf(VisualStyle.REALISTIC, VisualStyle.CYBERPUNK, VisualStyle.ANIME),
        Genre.DRAMA to listOf(VisualStyle.REALISTIC, VisualStyle.VINTAGE, VisualStyle.NOIR),
        Genre.ROMANSA to listOf(VisualStyle.REALISTIC, VisualStyle.VINTAGE, VisualStyle.ANIME),
        Genre.HOROR to listOf(VisualStyle.NOIR, VisualStyle.REALISTIC, VisualStyle.VINTAGE),
        Genre.SCI_FI to listOf(VisualStyle.CYBERPUNK, VisualStyle.REALISTIC, VisualStyle.PIXAR_3D),
        Genre.FANTASI to listOf(VisualStyle.ANIME, VisualStyle.PIXAR_3D, VisualStyle.REALISTIC),
        Genre.THRILLER to listOf(VisualStyle.NOIR, VisualStyle.REALISTIC, VisualStyle.CYBERPUNK),
        Genre.KOMEDI to listOf(VisualStyle.REALISTIC, VisualStyle.PIXAR_3D, VisualStyle.ANIME),
        Genre.DOKUMENTER to listOf(VisualStyle.REALISTIC, VisualStyle.VINTAGE),
        Genre.SEJARAH to listOf(VisualStyle.VINTAGE, VisualStyle.REALISTIC, VisualStyle.NOIR)
    )

    private val genreLights: Map<Genre, List<LightKind>> = mapOf(
        Genre.ACTION to listOf(LightKind.GOLDEN_HOUR, LightKind.VOLUMETRIC, LightKind.RIM),
        Genre.DRAMA to listOf(LightKind.GOLDEN_HOUR, LightKind.REMBRANDT, LightKind.OVERCAST),
        Genre.ROMANSA to listOf(LightKind.GOLDEN_HOUR, LightKind.CANDLE, LightKind.VOLUMETRIC),
        Genre.HOROR to listOf(LightKind.CHIAROSCURO, LightKind.MOONLIGHT, LightKind.CANDLE),
        Genre.SCI_FI to listOf(LightKind.NEON, LightKind.VOLUMETRIC, LightKind.RIM),
        Genre.FANTASI to listOf(LightKind.VOLUMETRIC, LightKind.GOLDEN_HOUR, LightKind.MOONLIGHT),
        Genre.THRILLER to listOf(LightKind.CHIAROSCURO, LightKind.NEON, LightKind.MOONLIGHT),
        Genre.KOMEDI to listOf(LightKind.GOLDEN_HOUR, LightKind.OVERCAST, LightKind.STUDIO_SOFT),
        Genre.DOKUMENTER to listOf(LightKind.GOLDEN_HOUR, LightKind.OVERCAST),
        Genre.SEJARAH to listOf(LightKind.GOLDEN_HOUR, LightKind.VOLUMETRIC, LightKind.REMBRANDT)
    )

    private val genreActions: Map<Genre, String> = mapOf(
        Genre.ACTION to "charges forward and lands a decisive blow",
        Genre.DRAMA to "lowers his head, then looks up with tears in his eyes",
        Genre.ROMANSA to "reaches out slowly and takes the other hand",
        Genre.HOROR to "freezes, then turns slowly toward the dark doorway",
        Genre.SCI_FI to "presses the glowing panel and the door slides open",
        Genre.FANTASI to "raises a hand and light gathers in the palm",
        Genre.THRILLER to "glances over his shoulder and quickens his pace",
        Genre.KOMEDI to "slips on the wet floor and catches himself, grinning",
        Genre.DOKUMENTER to "casts the net and watches it sink into the water",
        Genre.SEJARAH to "kneels before the altar and bows his head"
    )

    private val safeMoves = listOf(
        CameraMove.STATIC,
        CameraMove.DOLLY_IN,
        CameraMove.TRACKING,
        CameraMove.CRANE_UP,
        CameraMove.ORBIT,
        CameraMove.AERIAL,
        CameraMove.HANDHELD
    )

    fun roll(random: Random = Random.Default): ShotInput {
        val genre = Genre.values().random(random)
        val style = genreStyles.getValue(genre).random(random)
        val light = genreLights.getValue(genre).random(random)
        val engine = TargetEngine.values().random(random)
        val shotSize = ShotSize.values().random(random)
        val angle = CameraAngle.values().random(random)
        val lens = listOf(24, 35, 50, 85).random(random)
        val move = safeMoves.random(random)
        val hide = HideTactic.values().random(random)
        val focus = FocusMode.values().random(random)
        val action = genreActions.getValue(genre)
        return ShotInput(
            title = "Dadu: ${genre.labelId} - ${style.labelId}",
            genre = genre,
            style = style,
            engine = engine,
            character = CharacterDna(
                "The wanderer",
                "a young traveler with weathered clothes, tired eyes, a worn leather bag"
            ),
            world = WorldLock("Dice world", PromptEngine.defaultWorld(genre)),
            shotSize = shotSize,
            angle = angle,
            lensMm = lens,
            cameraMove = move,
            light = light,
            actionEn = action,
            poseStart = "standing still, watching",
            poseEnd = "standing firm, changed",
            materialEn = "dust drifts slowly through the air",
            audioEn = "wind, quiet ambience",
            focusMode = focus,
            hideTactic = hide
        )
    }
}
