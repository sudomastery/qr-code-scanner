package com.sudomastery.qrscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawValue: String,
    val format: String,
    val type: String,
    val timestamp: Long,
    val favorite: Boolean = false
)
