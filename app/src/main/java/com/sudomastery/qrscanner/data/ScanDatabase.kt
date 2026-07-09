package com.sudomastery.qrscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanRecord::class], version = 1, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    companion object {
        fun build(context: Context): ScanDatabase =
            Room.databaseBuilder(context, ScanDatabase::class.java, "scans.db").build()
    }
}
