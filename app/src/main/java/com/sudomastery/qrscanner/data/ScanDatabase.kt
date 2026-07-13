package com.sudomastery.qrscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScanRecord::class, VaultEntry::class], version = 2, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    abstract fun vaultDao(): VaultDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vault_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`issuer` TEXT NOT NULL, " +
                        "`account` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`algorithm` TEXT NOT NULL, " +
                        "`digits` INTEGER NOT NULL, " +
                        "`period` INTEGER NOT NULL, " +
                        "`secretEnc` TEXT NOT NULL, " +
                        "`uriEnc` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        fun build(context: Context): ScanDatabase =
            Room.databaseBuilder(context, ScanDatabase::class.java, "scans.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
