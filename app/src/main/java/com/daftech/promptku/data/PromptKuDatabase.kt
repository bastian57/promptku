package com.daftech.promptku.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.daftech.promptku.engine.CameraAngle
import com.daftech.promptku.engine.CameraMove
import com.daftech.promptku.engine.CharacterCard
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.DialogueLang
import com.daftech.promptku.engine.DialogueLine
import com.daftech.promptku.engine.DraftScene
import com.daftech.promptku.engine.EffectLibrary
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.GlobalLock
import com.daftech.promptku.engine.LightKind
import com.daftech.promptku.engine.ScriptEnding
import com.daftech.promptku.engine.ScriptInput
import com.daftech.promptku.engine.ShotSize
import com.daftech.promptku.engine.TargetEngine
import com.daftech.promptku.engine.VisualStyle
import com.daftech.promptku.engine.WorldLock

// Room offline — riwayat prompt, DNA lock, proyek multi-scene.
// Tanpa login, tanpa akun, tanpa cloud. Semua data tinggal di HP.

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val engine: String,
    val promptText: String,
    val negativeText: String,
    val score: Int,
    val createdAt: Long
)

@Entity(tableName = "dna")
data class DnaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val name: String,
    val descriptionEn: String,
    val createdAt: Long
)

@Entity(tableName = "projects")
data class ProjectItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    // Global Lock v1.1 — semua kolom nullable: proyek lama (v1.0.x)
    // tetap kebaca, lock-nya kosong sampai user mengaturnya.
    val lockWorldName: String? = null,
    val lockWorldEn: String? = null,
    val lockLight: String? = null,
    val lockStyle: String? = null,
    val lockLensMm: Int? = null,
    val lockCharName: String? = null,
    val lockCharEn: String? = null,
    val lockChar2Name: String? = null,
    val lockChar2En: String? = null,
    // Ambience Lock v1.3 — suara latar yang sama di semua klip.
    val lockAmbience: String? = null
)

@Entity(
    tableName = "scenes",
    foreignKeys = [ForeignKey(
        entity = ProjectItem::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SceneItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val orderIndex: Int,
    val title: String,
    val engine: String,
    val promptText: String,
    val negativeText: String,
    val createdAt: Long,
    // Kontinuitas pose v1.1: pose akhir scene ini jadi pose awal scene berikut.
    val poseStart: String = "",
    val poseEnd: String = "",
    // v1.4.4 — naskah sumber scene ini (null = shot Wizard manual).
    // Dipakai generate-replace: generate ulang naskah yang sama
    // menghapus dulu scene lama milik naskah itu, bukan menumpuk.
    val scriptId: Long? = null
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(item: HistoryItem): Long

    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    suspend fun all(): List<HistoryItem>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR promptText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun search(query: String): List<HistoryItem>

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DnaDao {
    @Insert
    suspend fun insert(item: DnaItem): Long

    @Query("SELECT * FROM dna ORDER BY createdAt DESC")
    suspend fun all(): List<DnaItem>

    @Query("SELECT * FROM dna WHERE kind = :kind ORDER BY createdAt DESC")
    suspend fun byKind(kind: String): List<DnaItem>

    @Delete
    suspend fun delete(item: DnaItem)
}

// Naskah v1.3 — FLAT: 1 scene = 1 klip 10 detik = 1 prompt.
// Status: DRAFT -> APPROVED (per scene) -> GENERATED.
// Tabel script_shots peninggalan v1.2 (model 2 tingkat) TIDAK lagi
// dibaca app v1.3+ — DIHAPUS oleh MIGRATION_6_7 (v1.4.4).
@Entity(tableName = "scripts")
data class ScriptItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val synopsis: String,
    val genre: String,
    val durationMinutes: Int,
    val emotion: String,
    val protagonistName: String,
    val protagonistDescEn: String,
    val antagonist: String,
    val antagonistDescEn: String = "",
    val worldName: String,
    val worldDescEn: String,
    val conflict: String,
    val ending: String,
    val style: String,
    val engine: String,
    val status: String,
    val createdAt: Long,
    // v1.3 — bahasa dialog default naskah + kartu 3 jangkar tokoh.
    val dialogueLang: String = "EN",
    val cardTrait: String? = null,
    val cardGesture: String? = null,
    val cardObject: String? = null,
    val cardFace: String? = null,
    // v1.4.1 — ambience Mode Cepat (sebelumnya hanya in-memory, hilang
    // saat naskah dimuat ulang dari DB — cacat lapangan 16 Nov).
    val ambienceEn: String = ""
)

@Entity(
    tableName = "script_scenes",
    foreignKeys = [ForeignKey(
        entity = ScriptItem::class,
        parentColumns = ["id"],
        childColumns = ["scriptId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScriptSceneItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val orderIndex: Int,
    val sequenceIndex: Int,
    val beatName: String,
    val slugline: String,
    val want: String,
    val obstacle: String,
    val actionEn: String,
    val dialogueName: String,
    val dialogueLine: String,
    val poseStart: String,
    val poseEnd: String,
    val screenDirection: String,
    val axisSide: String,
    val approved: Boolean = false,
    // v1.3 — DialogueLine lengkap (ID/EN + taktik + nada + timing).
    // dialogueName/dialogueLine di atas tetap ditulis sebagai cermin
    // (speaker + teks aktif) untuk pembaca lama.
    val dialogueTextId: String = "",
    val dialogueTextEn: String = "",
    val dialogueLang: String = "EN",
    val dialogueTactic: String = "",
    val dialogueTone: String = "",
    val dialogueStartSec: Float = 0f,
    val dialogueEndSec: Float = 0f,
    // v1.3 — parameter shot scene flat (1 scene = 1 klip).
    val sceneNumber: Int = 0,
    val shotSize: String = "",
    val angle: String = "",
    val lensMm: Int = 0,
    val cameraMove: String = "",
    val light: String = "",
    val transition: String = "",
    val directorNote: String = "",
    val reAnchor: Boolean = false,
    // v1.4.2 — id efek Perpustakaan Efek scene ini ("" = tanpa efek).
    val effectId: String = ""
)

@Dao
interface ScriptDao {
    @Insert
    suspend fun insertScript(item: ScriptItem): Long

    @Update
    suspend fun updateScript(item: ScriptItem)

    @Query("SELECT * FROM scripts ORDER BY createdAt DESC")
    suspend fun allScripts(): List<ScriptItem>

    @Delete
    suspend fun deleteScript(item: ScriptItem)

    // v1.4.4 — hapus naskah basi sekaligus (beserta scene naskahnya
    // via FK CASCADE).
    @Delete
    suspend fun deleteScripts(items: List<ScriptItem>)

    @Insert
    suspend fun insertScene(item: ScriptSceneItem): Long

    @Insert
    suspend fun insertScenes(items: List<ScriptSceneItem>)

    @Update
    suspend fun updateScene(item: ScriptSceneItem)

    @Query("SELECT * FROM script_scenes WHERE scriptId = :scriptId ORDER BY orderIndex ASC")
    suspend fun scenesOf(scriptId: Long): List<ScriptSceneItem>

    @Delete
    suspend fun deleteScene(item: ScriptSceneItem)

    @Query("DELETE FROM script_scenes WHERE scriptId = :scriptId AND orderIndex = :orderIndex")
    suspend fun deleteScenesOfSequence(scriptId: Long, orderIndex: Int)
}

@Dao
interface ProjectDao {
    @Insert
    suspend fun insertProject(item: ProjectItem): Long

    @Update
    suspend fun updateProject(item: ProjectItem)

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun allProjects(): List<ProjectItem>

    @Delete
    suspend fun deleteProject(item: ProjectItem)

    @Insert
    suspend fun insertScene(item: SceneItem): Long

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun scenesOf(projectId: Long): List<SceneItem>

    @Delete
    suspend fun deleteScene(item: SceneItem)

    // v1.4.4 — hapus scene proyek milik satu naskah (generate ulang =
    // ganti, bukan tumpuk) + hapus scene dari naskah yang sudah
    // dihapus (orphan) + hapus scene duplikat per id.
    @Query("DELETE FROM scenes WHERE projectId = :projectId AND scriptId = :scriptId")
    suspend fun deleteScenesOfScript(projectId: Long, scriptId: Long)

    @Query("DELETE FROM scenes WHERE scriptId IN (:scriptIds)")
    suspend fun deleteScenesOfScripts(scriptIds: List<Long>)

    @Query("DELETE FROM scenes WHERE id IN (:ids)")
    suspend fun deleteScenesByIds(ids: List<Long>)
}

// Migrasi v1 -> v2 (v1.1): tambah kolom Global Lock di projects dan
// kolom pose di scenes. Semua kolom baru nullable / ber-default, jadi
// database lama (v1.0.x) naik tanpa hapus data.
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN lockWorldName TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockWorldEn TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockLight TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockStyle TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockLensMm INTEGER")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockCharName TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockCharEn TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockChar2Name TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN lockChar2En TEXT")
        db.execSQL("ALTER TABLE scenes ADD COLUMN poseStart TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE scenes ADD COLUMN poseEnd TEXT NOT NULL DEFAULT ''")
    }
}

// Konversi kolom proyek <-> GlobalLock. Enum dibaca dengan runCatching:
// nilai tak dikenal (misal dari versi lama) jadi null, bukan crash.
fun ProjectItem.toGlobalLock(): GlobalLock {
    return GlobalLock(
        world = if (lockWorldName != null && lockWorldEn != null) {
            WorldLock(lockWorldName, lockWorldEn)
        } else {
            null
        },
        light = lockLight?.let { runCatching { LightKind.valueOf(it) }.getOrNull() },
        style = lockStyle?.let { runCatching { VisualStyle.valueOf(it) }.getOrNull() },
        lensMm = lockLensMm,
        character = if (lockCharName != null && lockCharEn != null) {
            CharacterDna(lockCharName, lockCharEn)
        } else {
            null
        },
        secondCharacter = if (lockChar2Name != null && lockChar2En != null) {
            CharacterDna(lockChar2Name, lockChar2En)
        } else {
            null
        },
        ambience = lockAmbience
    )
}

fun ProjectItem.withLock(lock: GlobalLock): ProjectItem {
    return copy(
        lockWorldName = lock.world?.name,
        lockWorldEn = lock.world?.descriptionEn,
        lockLight = lock.light?.name,
        lockStyle = lock.style?.name,
        lockLensMm = lock.lensMm,
        lockCharName = lock.character?.name,
        lockCharEn = lock.character?.descriptionEn,
        lockChar2Name = lock.secondCharacter?.name,
        lockChar2En = lock.secondCharacter?.descriptionEn,
        lockAmbience = lock.ambience
    )
}

// Migrasi v2 -> v3 (v1.2): tambah 3 tabel naskah (scripts, script_scenes,
// script_shots). Tabel baru saja — database lama naik tanpa hapus data.
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scripts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `synopsis` TEXT NOT NULL, " +
                "`genre` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, " +
                "`emotion` TEXT NOT NULL, `protagonistName` TEXT NOT NULL, " +
                "`protagonistDescEn` TEXT NOT NULL, `antagonist` TEXT NOT NULL, " +
                "`worldName` TEXT NOT NULL, `worldDescEn` TEXT NOT NULL, " +
                "`conflict` TEXT NOT NULL, `ending` TEXT NOT NULL, " +
                "`style` TEXT NOT NULL, `engine` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `script_scenes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`scriptId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, " +
                "`sequenceIndex` INTEGER NOT NULL, `beatName` TEXT NOT NULL, " +
                "`slugline` TEXT NOT NULL, `want` TEXT NOT NULL, " +
                "`obstacle` TEXT NOT NULL, `actionEn` TEXT NOT NULL, " +
                "`dialogueName` TEXT NOT NULL, `dialogueLine` TEXT NOT NULL, " +
                "`poseStart` TEXT NOT NULL, `poseEnd` TEXT NOT NULL, " +
                "`screenDirection` TEXT NOT NULL, `axisSide` TEXT NOT NULL, " +
                "`approved` INTEGER NOT NULL, " +
                "FOREIGN KEY(`scriptId`) REFERENCES `scripts`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `script_shots` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`scriptId` INTEGER NOT NULL, `sceneOrder` INTEGER NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, `shotSize` TEXT NOT NULL, " +
                "`angle` TEXT NOT NULL, `lensMm` INTEGER NOT NULL, " +
                "`cameraMove` TEXT NOT NULL, `light` TEXT NOT NULL, " +
                "`actionEn` TEXT NOT NULL, `poseStart` TEXT NOT NULL, " +
                "`poseEnd` TEXT NOT NULL, `screenDirection` TEXT NOT NULL, " +
                "`axisSide` TEXT NOT NULL, `priority` TEXT NOT NULL, " +
                "`shotFunction` TEXT NOT NULL, `transition` TEXT NOT NULL, " +
                "`reAnchor` INTEGER NOT NULL, " +
                "FOREIGN KEY(`scriptId`) REFERENCES `scripts`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
    }
}

// Migrasi v3 -> v4 (v1.3): SEMUA kolom baru aditif dengan default,
// database lama (v1.2.x) naik tanpa hapus data.
// - projects: Ambience Lock.
// - scripts: bahasa dialog default + deskripsi antagonis + kartu 3 jangkar.
// - script_scenes: DialogueLine lengkap + parameter shot scene flat.
// Tabel script_shots peninggalan v1.2 TIDAK disentuh (tetap di file DB).
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN lockAmbience TEXT")
        db.execSQL("ALTER TABLE scripts ADD COLUMN antagonistDescEn TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE scripts ADD COLUMN dialogueLang TEXT NOT NULL DEFAULT 'EN'")
        db.execSQL("ALTER TABLE scripts ADD COLUMN cardTrait TEXT")
        db.execSQL("ALTER TABLE scripts ADD COLUMN cardGesture TEXT")
        db.execSQL("ALTER TABLE scripts ADD COLUMN cardObject TEXT")
        db.execSQL("ALTER TABLE scripts ADD COLUMN cardFace TEXT")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueTextId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueTextEn TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueLang TEXT NOT NULL DEFAULT 'EN'")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueTactic TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueTone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueStartSec REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN dialogueEndSec REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN sceneNumber INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN shotSize TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN angle TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN lensMm INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN cameraMove TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN light TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN transition TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN directorNote TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN reAnchor INTEGER NOT NULL DEFAULT 0")
    }
}

// v1.4.1 — Room v4 -> v5: ambienceEn Mode Cepat ikut tersimpan di
// tabel scripts (1 ALTER aditif; baris lama aman, default '').
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scripts ADD COLUMN ambienceEn TEXT NOT NULL DEFAULT ''")
    }
}

// v1.4.2 — Room v5 -> v6: effectId Perpustakaan Efek ikut tersimpan di
// tabel script_scenes (1 ALTER aditif; baris lama aman, default '').
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE script_scenes ADD COLUMN effectId TEXT NOT NULL DEFAULT ''")
    }
}

// v1.4.4 — Room v6 -> v7: Bersihkan Sampah.
// - scenes.scriptId (nullable): naskah sumber scene proyek.
// - Dedup satu kali: tiap (projectId, judul) hanya baris terbaru
//   (id terbesar) yang dipertahankan — membersihkan tumpukan hasil
//   generate ulang (contoh lapangan: proyek Bejo 12 scene -> 6).
// - DROP script_shots: tabel peninggalan v1.2, tidak dibaca app
//   v1.3+ — buang dari file DB.
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scenes ADD COLUMN scriptId INTEGER")
        db.execSQL(
            "DELETE FROM scenes WHERE id NOT IN " +
                "(SELECT MAX(id) FROM scenes GROUP BY projectId, title)"
        )
        db.execSQL("DROP TABLE IF EXISTS script_shots")
    }
}

// Konversi ScriptItem <-> ScriptInput (enum dibaca dengan runCatching).
fun ScriptItem.toScriptInput(): ScriptInput {
    val card = if (
        cardTrait.isNullOrBlank() && cardGesture.isNullOrBlank() &&
        cardObject.isNullOrBlank() && cardFace.isNullOrBlank()
    ) {
        null
    } else {
        CharacterCard(
            trait = cardTrait ?: "",
            gesture = cardGesture ?: "",
            objectProp = cardObject ?: "",
            faceAnchor = cardFace ?: ""
        )
    }
    return ScriptInput(
        title = title,
        synopsis = synopsis,
        genre = runCatching { Genre.valueOf(genre) }.getOrDefault(Genre.ACTION),
        durationMinutes = durationMinutes,
        emotion = emotion,
        protagonistName = protagonistName,
        protagonistDescEn = protagonistDescEn,
        antagonist = antagonist,
        antagonistDescEn = antagonistDescEn,
        worldName = worldName,
        worldDescEn = worldDescEn,
        conflict = conflict,
        ending = runCatching { ScriptEnding.valueOf(ending) }.getOrDefault(ScriptEnding.HAPPY),
        style = runCatching { VisualStyle.valueOf(style) }.getOrDefault(VisualStyle.REALISTIC),
        engine = runCatching { TargetEngine.valueOf(engine) }.getOrDefault(TargetEngine.FLOW),
        dialogueLang = runCatching { DialogueLang.valueOf(dialogueLang) }.getOrDefault(DialogueLang.EN),
        ambienceEn = ambienceEn,
        characterCard = card,
        // v1.4.2 — token efek dihitung ulang dari sinopsis (deterministik,
        // sama seperti saat build) supaya regenerateScene setelah reload
        // tetap dapat efek otomatisnya.
        effectIds = EffectLibrary.detect(synopsis).toTokens().joinToString(",")
    )
}

fun ScriptInput.toScriptItem(status: String = "DRAFT"): ScriptItem {
    return ScriptItem(
        title = title,
        synopsis = synopsis,
        genre = genre.name,
        durationMinutes = durationMinutes,
        emotion = emotion,
        protagonistName = protagonistName,
        protagonistDescEn = protagonistDescEn,
        antagonist = antagonist,
        antagonistDescEn = antagonistDescEn,
        worldName = worldName,
        worldDescEn = worldDescEn,
        conflict = conflict,
        ending = ending.name,
        style = style.name,
        engine = engine.name,
        status = status,
        createdAt = System.currentTimeMillis(),
        dialogueLang = dialogueLang.name,
        cardTrait = characterCard?.trait?.ifBlank { null },
        cardGesture = characterCard?.gesture?.ifBlank { null },
        cardObject = characterCard?.objectProp?.ifBlank { null },
        cardFace = characterCard?.faceAnchor?.ifBlank { null },
        ambienceEn = ambienceEn
    )
}

fun DraftScene.toScriptSceneItem(scriptId: Long): ScriptSceneItem {
    val line = dialogue
    return ScriptSceneItem(
        scriptId = scriptId,
        orderIndex = orderIndex,
        sequenceIndex = sequenceIndex,
        beatName = beatName,
        slugline = slugline,
        want = want,
        obstacle = obstacle,
        actionEn = actionEn,
        dialogueName = line?.speaker ?: "",
        dialogueLine = line?.activeText() ?: "",
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide,
        approved = false,
        dialogueTextId = line?.textId ?: "",
        dialogueTextEn = line?.textEn ?: "",
        dialogueLang = line?.language?.name ?: "EN",
        dialogueTactic = line?.tactic ?: "",
        dialogueTone = line?.tone ?: "",
        dialogueStartSec = line?.startSec ?: 0f,
        dialogueEndSec = line?.endSec ?: 0f,
        sceneNumber = sceneNumber,
        shotSize = shotSize.name,
        angle = angle.name,
        lensMm = lensMm,
        cameraMove = cameraMove.name,
        light = light.name,
        transition = transition,
        directorNote = directorNote,
        reAnchor = reAnchor,
        effectId = effectId
    )
}

fun ScriptSceneItem.toDraftScene(): DraftScene {
    // Baris lama (v1.2) tidak punya kolom dialog baru: dialogueLine lama
    // dibaca sebagai teks Inggris, shotSize kosong -> MEDIUM, lens 0 -> 35.
    val line = if (
        dialogueTextId.isBlank() && dialogueTextEn.isBlank() &&
        dialogueLine.isBlank()
    ) {
        null
    } else {
        DialogueLine(
            speaker = dialogueName,
            tactic = dialogueTactic,
            tone = dialogueTone,
            textId = dialogueTextId,
            textEn = dialogueTextEn.ifBlank { dialogueLine },
            language = runCatching { DialogueLang.valueOf(dialogueLang) }.getOrDefault(DialogueLang.EN),
            startSec = dialogueStartSec,
            endSec = dialogueEndSec
        )
    }
    return DraftScene(
        sceneNumber = if (sceneNumber > 0) sceneNumber else orderIndex + 1,
        orderIndex = orderIndex,
        sequenceIndex = sequenceIndex,
        beatName = beatName,
        slugline = slugline,
        want = want,
        obstacle = obstacle,
        actionEn = actionEn,
        dialogue = line,
        shotSize = runCatching { ShotSize.valueOf(shotSize) }.getOrDefault(ShotSize.MEDIUM),
        angle = runCatching { CameraAngle.valueOf(angle) }.getOrDefault(CameraAngle.EYE),
        lensMm = if (lensMm > 0) lensMm else 35,
        cameraMove = runCatching { CameraMove.valueOf(cameraMove) }.getOrDefault(CameraMove.STATIC),
        light = runCatching { LightKind.valueOf(light) }.getOrDefault(LightKind.GOLDEN_HOUR),
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide,
        transition = transition,
        directorNote = directorNote,
        reAnchor = reAnchor,
        effectId = effectId
    )
}

@Database(
    entities = [
        HistoryItem::class, DnaItem::class, ProjectItem::class, SceneItem::class,
        ScriptItem::class, ScriptSceneItem::class, V2BibleItem::class, V2SceneItem::class,
        V2DnaItem::class
    ],
    version = 9,
    exportSchema = false
)
abstract class PromptKuDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun dnaDao(): DnaDao
    abstract fun projectDao(): ProjectDao
    abstract fun scriptDao(): ScriptDao
    abstract fun v2BibleDao(): V2BibleDao
    abstract fun v2SceneDao(): V2SceneDao
    abstract fun v2DnaDao(): V2DnaDao

    companion object {
        @Volatile
        private var instance: PromptKuDatabase? = null

        fun get(context: Context): PromptKuDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PromptKuDatabase::class.java,
                    "promptku.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build().also { instance = it }
            }
        }
    }
}
