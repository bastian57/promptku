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
    val createdAt: Long
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
    val createdAt: Long
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

@Database(
    entities = [HistoryItem::class, DnaItem::class, ProjectItem::class, SceneItem::class],
    version = 1,
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
                ).build().also { instance = it }
            }
        }
    }
}
