package com.sudomastery.qrscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sudomastery.qrscanner.QrApp
import com.sudomastery.qrscanner.data.ScanRecord
import com.sudomastery.qrscanner.data.Settings
import com.sudomastery.qrscanner.parsing.ScanContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QrApp
    private val dao = app.database.scanDao()
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
}
