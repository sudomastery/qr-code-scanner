package com.sudomastery.qrscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A vaulted authenticator key. The secret and the original otpauth URI are
 * encrypted with the Keystore-backed vault key; everything else is plain
 * metadata for the list display.
 */
@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String,
    val account: String,
    val type: String,
    val algorithm: String,
    val digits: Int,
    val period: Int,
    val secretEnc: String,
    val uriEnc: String,
    val createdAt: Long
)
