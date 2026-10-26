package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "test_history")
data class TestRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long,
    val avgLatencyMs: Long,
    val jitterMs: Long,
    val connectionType: String,
    val ratingTitle: String,
    val serverName: String
)

@Dao
interface TestRecordDao {
    @Query("SELECT * FROM test_history ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TestRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TestRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: TestRecordEntity)

    @Query("DELETE FROM test_history")
    suspend fun clearAll()
}

@Database(entities = [TestRecordEntity::class], version = 1, exportSchema = false)
abstract class TestHistoryDatabase : RoomDatabase() {
    abstract fun testRecordDao(): TestRecordDao
}
