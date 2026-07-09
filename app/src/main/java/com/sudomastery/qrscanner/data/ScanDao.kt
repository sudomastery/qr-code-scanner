package com.sudomastery.qrscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY favorite DESC, timestamp DESC")
    fun observeAll(): Flow<List<ScanRecord>>

    @Query(
        "SELECT * FROM scans WHERE rawValue LIKE '%' || :query || '%' " +
            "ORDER BY favorite DESC, timestamp DESC"
    )
    fun search(query: String): Flow<List<ScanRecord>>

    @Insert
    suspend fun insert(record: ScanRecord): Long

    @Query("UPDATE scans SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clearAll()
}
