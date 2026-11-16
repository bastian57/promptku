package com.daftech.promptku.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Cache v2.0 — bible + scene hasil LLM disimpan di Room supaya:
//  - generate ulang scene yang sama = 0 panggilan (cache hit);
//  - cerita tidak hilang kalau app ditutup di tengah jalan;
//  - Mode Manual (rule-based) tetap jalan tanpa cache ini.
// Tabel dibuat MIGRATION_7_8 (Room v7 -> v8).

@Entity(tableName = "v2_bibles")
data class V2BibleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val idea: String,
    val bibleJson: String,
    val createdAt: Long
)

@Entity(
    tableName = "v2_scenes",
    // v2.0 fix2 — index HARUS dideklarasikan di entity: Room memvalidasi
    // skema SETELAH migrasi, dan index yang dibuat migrasi tapi tidak ada
    // di entity = IllegalStateException ("Migration didn't properly
    // handle") → app crash tiap buka. Nama index Room untuk Index("bibleId")
    // = index_v2_scenes_bibleId, sama persis dengan CREATE INDEX di
    // MIGRATION_7_8 — jadi install baru (Room bikin dari entity) dan
    // upgrade (migrasi) konsisten.
    indices = [Index("bibleId")]
)
data class V2SceneItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bibleId: Long,
    val sceneNumber: Int,
    val planJson: String,
    val flowText: String,
    val klingText: String,
    val seedanceText: String,
    val wanText: String,
    val needsReview: Boolean,
    val reviewReason: String
)

@Dao
interface V2BibleDao {
    @Insert
    fun insert(bible: V2BibleItem): Long

    @Query("SELECT * FROM v2_bibles ORDER BY createdAt DESC")
    fun all(): List<V2BibleItem>

    @Query("SELECT * FROM v2_bibles WHERE id = :id LIMIT 1")
    fun byId(id: Long): V2BibleItem?

    @Update
    fun update(bible: V2BibleItem)

    @Query("DELETE FROM v2_bibles WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM v2_scenes WHERE bibleId = :bibleId")
    fun deleteScenesOf(bibleId: Long)
}

@Dao
interface V2SceneDao {
    @Insert
    fun insert(scene: V2SceneItem): Long

    @Query("SELECT * FROM v2_scenes WHERE bibleId = :bibleId ORDER BY sceneNumber ASC")
    fun ofBible(bibleId: Long): List<V2SceneItem>

    @Query("DELETE FROM v2_scenes WHERE bibleId = :bibleId")
    fun deleteOfBible(bibleId: Long)
}

val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `v2_bibles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `idea` TEXT NOT NULL, " +
                "`bibleJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `v2_scenes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`bibleId` INTEGER NOT NULL, `sceneNumber` INTEGER NOT NULL, " +
                "`planJson` TEXT NOT NULL, `flowText` TEXT NOT NULL, " +
                "`klingText` TEXT NOT NULL, `seedanceText` TEXT NOT NULL, " +
                "`wanText` TEXT NOT NULL, `needsReview` INTEGER NOT NULL, " +
                "`reviewReason` TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_v2_scenes_bibleId` ON `v2_scenes` (`bibleId`)")
    }
}
