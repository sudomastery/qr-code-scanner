package com.sudomastery.qrscanner.vault

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Session lock for the vault: one successful authentication keeps it open
 * until the timeout passes or the app goes to background, whichever comes
 * first. Interaction with the vault extends the deadline.
 */
object VaultLock {

    private const val TIMEOUT_MS = 60_000L

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    private var deadline = 0L

    fun unlock() {
        deadline = System.currentTimeMillis() + TIMEOUT_MS
        _unlocked.value = true
    }

    fun touch() {
        if (_unlocked.value) deadline = System.currentTimeMillis() + TIMEOUT_MS
    }

    fun lock() {
        _unlocked.value = false
    }

    /** Called from the vault screen's ticker to enforce the timeout. */
    fun lockIfExpired() {
        if (_unlocked.value && System.currentTimeMillis() > deadline) lock()
    }
}
