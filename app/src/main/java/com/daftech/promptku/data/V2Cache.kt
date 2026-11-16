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
    val reviewReason: String,
    // v2.1 — scene yang sudah disetujui user (tombol Setuju di layar Prompt).
    val approved: Boolean = false
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

    @Query("UPDATE v2_scenes SET approved = :approved WHERE bibleId = :bibleId AND sceneNumber = :sceneNumber")
    fun setApproved(bibleId: Long, sceneNumber: Int, approved: Boolean)
}

// v2.1 — Bank Tokoh DNA: gambar referensi yang sudah dideskripsikan Vision
// disimpan di sini (nama + deskripsi + JPEG downscale Base64, format sama
// dengan V2Image.base64) supaya bisa dipakai di film berikutnya tanpa
// upload ulang. Dipakai Layar Ide ("Tokoh dari DNA"), BUKAN tab DNA lama.
@Entity(tableName = "v2_dna")
data class V2DnaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val descriptionEn: String,
    /** Jangkar visual, digabung ";" (maks 3, dari Vision). */
    val anchorsEn: String,
    val imageBase64: String,
    val createdAt: Long
)

@Dao
interface V2DnaDao {
    @Insert
    fun insert(item: V2DnaItem): Long

    @Query("SELECT * FROM v2_dna ORDER BY createdAt DESC")
    fun all(): List<V2DnaItem>

    @Query("DELETE FROM v2_dna WHERE id = :id")
    fun delete(id: Long)
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

// v2.1 — Room v8 -> v9: tabel bank tokoh DNA + kolom approved di v2_scenes.
// Keduanya aditif; database lama (fix7 ke bawah) naik tanpa hapus data.
// Aturan fix2 tetap berlaku: kolom/tabel baru di sini HARUS cocok dengan
// entity di atas (nama tabel + nama kolom persis) supaya Room tidak crash
// validasi skema setelah migrasi.
val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `v2_dna` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `descriptionEn` TEXT NOT NULL, " +
                "`anchorsEn` TEXT NOT NULL, `imageBase64` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE v2_scenes ADD COLUMN approved INTEGER NOT NULL DEFAULT 0")
    }
}
