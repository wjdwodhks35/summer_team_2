package com.example.whentoleave.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

// ── Entity ──────────────────────────────────────────────
@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,           // 경로 이름 (예: "집→학교")
    val startAddress: String,
    val endAddress: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ── DAO ──────────────────────────────────────────────────
@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<SavedRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: SavedRoute)

    @Delete
    suspend fun delete(route: SavedRoute)
}

// ── Database ─────────────────────────────────────────────
@Database(entities = [SavedRoute::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedRouteDao(): SavedRouteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whentoleave_db"
                ).build().also { INSTANCE = it }
            }
    }
}