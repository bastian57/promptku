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
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.GlobalLock
import com.daftech.promptku.engine.LightKind
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

@Database(
    entities = [HistoryItem::class, DnaItem::class, ProjectItem::class, SceneItem::class],
    version = 2,
    exportSchema = false
)
abstract class PromptKuDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun dnaDao(): DnaDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var instance: PromptKuDatabase? = null

        fun get(context: Context): PromptKuDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PromptKuDatabase::class.java,
                    "promptku.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
