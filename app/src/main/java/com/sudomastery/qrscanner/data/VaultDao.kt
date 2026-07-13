package com.sudomastery.qrscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_entries ORDER BY issuer COLLATE NOCASE, account COLLATE NOCASE")
    fun observeAll(): Flow<List<VaultEntry>>

    @Insert
    suspend fun insert(entry: VaultEntry): Long

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
