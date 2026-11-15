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
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.DraftScene
import com.daftech.promptku.engine.DraftShot
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.GlobalLock
import com.daftech.promptku.engine.LightKind
import com.daftech.promptku.engine.ScriptEnding
import com.daftech.promptku.engine.ScriptInput
import com.daftech.promptku.engine.ShotFunction
import com.daftech.promptku.engine.ShotPriority
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
    val lockChar2En: String? = null
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
    val poseEnd: String = ""
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

// Naskah v1.2 — 2 level: Scene dramatis -> Shot (1 klip = 1 prompt).
// Status: DRAFT -> APPROVED (per sequence) -> GENERATED.
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
    val worldName: String,
    val worldDescEn: String,
    val conflict: String,
    val ending: String,
    val style: String,
    val engine: String,
    val status: String,
    val createdAt: Long
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
    val approved: Boolean = false
)

@Entity(
    tableName = "script_shots",
    foreignKeys = [ForeignKey(
        entity = ScriptItem::class,
        parentColumns = ["id"],
        childColumns = ["scriptId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScriptShotItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val sceneOrder: Int,
    val orderIndex: Int,
    val shotSize: String,
    val angle: String,
    val lensMm: Int,
    val cameraMove: String,
    val light: String,
    val actionEn: String,
    val poseStart: String,
    val poseEnd: String,
    val screenDirection: String,
    val axisSide: String,
    val priority: String,
    val shotFunction: String,
    val transition: String,
    val reAnchor: Boolean = false
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

    @Insert
    suspend fun insertShot(item: ScriptShotItem): Long

    @Insert
    suspend fun insertShots(items: List<ScriptShotItem>)

    @Update
    suspend fun updateShot(item: ScriptShotItem)

    @Query("SELECT * FROM script_shots WHERE scriptId = :scriptId ORDER BY sceneOrder ASC, orderIndex ASC")
    suspend fun shotsOf(scriptId: Long): List<ScriptShotItem>

    @Delete
    suspend fun deleteShot(item: ScriptShotItem)

    @Query("DELETE FROM script_shots WHERE scriptId = :scriptId AND sceneOrder = :sceneOrder")
    suspend fun deleteShotsOfScene(scriptId: Long, sceneOrder: Int)
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
        }
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
        lockChar2En = lock.secondCharacter?.descriptionEn
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

// Konversi ScriptItem <-> ScriptInput (enum dibaca dengan runCatching).
fun ScriptItem.toScriptInput(): ScriptInput {
    return ScriptInput(
        title = title,
        synopsis = synopsis,
        genre = runCatching { Genre.valueOf(genre) }.getOrDefault(Genre.ACTION),
        durationMinutes = durationMinutes,
        emotion = emotion,
        protagonistName = protagonistName,
        protagonistDescEn = protagonistDescEn,
        antagonist = antagonist,
        worldName = worldName,
        worldDescEn = worldDescEn,
        conflict = conflict,
        ending = runCatching { ScriptEnding.valueOf(ending) }.getOrDefault(ScriptEnding.HAPPY),
        style = runCatching { VisualStyle.valueOf(style) }.getOrDefault(VisualStyle.REALISTIC),
        engine = runCatching { TargetEngine.valueOf(engine) }.getOrDefault(TargetEngine.FLOW)
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
        worldName = worldName,
        worldDescEn = worldDescEn,
        conflict = conflict,
        ending = ending.name,
        style = style.name,
        engine = engine.name,
        status = status,
        createdAt = System.currentTimeMillis()
    )
}

fun DraftScene.toScriptSceneItem(scriptId: Long): ScriptSceneItem {
    return ScriptSceneItem(
        scriptId = scriptId,
        orderIndex = orderIndex,
        sequenceIndex = sequenceIndex,
        beatName = beatName,
        slugline = slugline,
        want = want,
        obstacle = obstacle,
        actionEn = actionEn,
        dialogueName = dialogueName,
        dialogueLine = dialogueLine,
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide,
        approved = false
    )
}

fun ScriptSceneItem.toDraftScene(): DraftScene {
    return DraftScene(
        sequenceIndex = sequenceIndex,
        orderIndex = orderIndex,
        beatName = beatName,
        slugline = slugline,
        want = want,
        obstacle = obstacle,
        actionEn = actionEn,
        dialogueName = dialogueName,
        dialogueLine = dialogueLine,
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide
    )
}

fun DraftShot.toScriptShotItem(scriptId: Long): ScriptShotItem {
    return ScriptShotItem(
        scriptId = scriptId,
        sceneOrder = sceneOrder,
        orderIndex = orderIndex,
        shotSize = shotSize.name,
        angle = angle.name,
        lensMm = lensMm,
        cameraMove = cameraMove.name,
        light = light.name,
        actionEn = actionEn,
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide,
        priority = priority.name,
        shotFunction = shotFunction.name,
        transition = transition,
        reAnchor = reAnchor
    )
}

fun ScriptShotItem.toDraftShot(): DraftShot {
    return DraftShot(
        sceneOrder = sceneOrder,
        orderIndex = orderIndex,
        shotSize = runCatching { ShotSize.valueOf(shotSize) }.getOrDefault(ShotSize.MEDIUM),
        angle = runCatching { CameraAngle.valueOf(angle) }.getOrDefault(CameraAngle.EYE),
        lensMm = lensMm,
        cameraMove = runCatching { CameraMove.valueOf(cameraMove) }.getOrDefault(CameraMove.STATIC),
        light = runCatching { LightKind.valueOf(light) }.getOrDefault(LightKind.GOLDEN_HOUR),
        actionEn = actionEn,
        poseStart = poseStart,
        poseEnd = poseEnd,
        screenDirection = screenDirection,
        axisSide = axisSide,
        priority = runCatching { ShotPriority.valueOf(priority) }.getOrDefault(ShotPriority.B),
        shotFunction = runCatching { ShotFunction.valueOf(shotFunction) }.getOrDefault(ShotFunction.ACTION),
        transition = transition,
        reAnchor = reAnchor
    )
}

@Database(
    entities = [
        HistoryItem::class, DnaItem::class, ProjectItem::class, SceneItem::class,
        ScriptItem::class, ScriptSceneItem::class, ScriptShotItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PromptKuDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun dnaDao(): DnaDao
    abstract fun projectDao(): ProjectDao
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var instance: PromptKuDatabase? = null

        fun get(context: Context): PromptKuDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PromptKuDatabase::class.java,
                    "promptku.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}
