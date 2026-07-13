package com.sudomastery.qrscanner.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sudomastery.qrscanner.QrApp
import com.sudomastery.qrscanner.data.ScanRecord
import com.sudomastery.qrscanner.data.Settings
import com.sudomastery.qrscanner.data.VaultEntry
import com.sudomastery.qrscanner.parsing.ScanContent
import com.sudomastery.qrscanner.vault.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QrApp
    private val dao = app.database.scanDao()
    private val vaultDao = app.database.vaultDao()
    private val settingsRepo = app.settings

    val settings: StateFlow<Settings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<ScanRecord>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) dao.observeAll() else dao.search(escapeLike(query))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun escapeLike(query: String): String = query
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    /**
     * Reads the persisted settings directly, unlike [settings].value which
     * holds hardcoded defaults until DataStore's first emission.
     */
    suspend fun awaitSettings(): Settings = settingsRepo.settings.first()

    fun recordScan(raw: String, format: String, content: ScanContent) {
        viewModelScope.launch {
            if (!awaitSettings().saveHistory) return@launch
            dao.insert(
                ScanRecord(
                    rawValue = raw,
                    format = format,
                    type = content.typeName,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    val vaultEntries: StateFlow<List<VaultEntry>> = vaultDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Encrypts the secret and moves the scan into the vault. Any history rows
     * holding the same otpauth URI are removed so the secret never shows up
     * outside the vault.
     */
    fun vaultOtp(content: ScanContent.Otp) {
        viewModelScope.launch(Dispatchers.Default) {
            val entry = runCatching {
                VaultEntry(
                    issuer = content.issuer,
                    account = content.account,
                    type = content.type,
                    algorithm = content.algorithm.ifEmpty { "SHA1" },
                    digits = content.digits.toIntOrNull() ?: 6,
                    period = content.period.toIntOrNull() ?: 30,
                    secretEnc = VaultCrypto.encrypt(content.secret),
                    uriEnc = VaultCrypto.encrypt(content.raw),
                    createdAt = System.currentTimeMillis()
                )
            }.getOrNull()
            if (entry == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(app, "Could not save to vault", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            vaultDao.insert(entry)
            dao.deleteByRawValue(content.raw)
        }
    }

    fun deleteVaultEntry(id: Long) {
        viewModelScope.launch { vaultDao.delete(id) }
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { dao.setFavorite(id, favorite) }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { dao.clearAll() }
    }

    fun setVibrateOnScan(value: Boolean) = viewModelScope.launch {
        settingsRepo.setVibrateOnScan(value)
    }

    fun setBeepOnScan(value: Boolean) = viewModelScope.launch {
        settingsRepo.setBeepOnScan(value)
    }

    fun setAutoOpenLinks(value: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoOpenLinks(value)
    }

    fun setRemoveTrackers(value: Boolean) = viewModelScope.launch {
        settingsRepo.setRemoveTrackers(value)
    }

    fun setSaveHistory(value: Boolean) = viewModelScope.launch {
        settingsRepo.setSaveHistory(value)
    }

    fun setAutoVaultOtp(value: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoVaultOtp(value)
    }

    fun setThemeColor(value: String) = viewModelScope.launch {
        settingsRepo.setThemeColor(value)
    }
}
