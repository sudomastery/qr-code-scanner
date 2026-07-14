package com.sudomastery.qrscanner.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sudomastery.qrscanner.data.VaultEntry
import com.sudomastery.qrscanner.util.Actions
import com.sudomastery.qrscanner.vault.Totp
import com.sudomastery.qrscanner.vault.VaultAuth
import com.sudomastery.qrscanner.vault.VaultCrypto
import com.sudomastery.qrscanner.vault.VaultLock
import kotlinx.coroutines.delay

@Composable
fun VaultScreen(viewModel: MainViewModel) {
    val unlocked by VaultLock.unlocked.collectAsState()
    val entries by viewModel.vaultEntries.collectAsState()

    if (unlocked) {
        UnlockedVault(entries = entries, viewModel = viewModel)
    } else {
        LockedVault(entryCount = entries.size)
    }
}

@Composable
private fun LockedVault(entryCount: Int) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun requestUnlock() {
        if (activity == null) return
        if (!VaultAuth.isAvailable(context)) {
            Toast.makeText(
                context,
                "Set up a screen lock or fingerprint in system settings to use the vault",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        VaultAuth.prompt(
            activity = activity,
            onSuccess = { VaultLock.unlock() },
            onError = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        )
    }

    // Prompt right away when the tab opens, like banking apps do
    LaunchedEffect(Unit) {
        if (VaultAuth.isAvailable(context)) requestUnlock()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Vault is locked",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = if (entryCount == 0) "No keys saved yet. Scan an authenticator " +
                "code and tap Add to Vault."
            else "$entryCount saved ${if (entryCount == 1) "key" else "keys"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = { requestUnlock() }) {
            Icon(Icons.Filled.LockOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Unlock")
        }
    }
}

@Composable
private fun UnlockedVault(entries: List<VaultEntry>, viewModel: MainViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingDelete by remember { mutableStateOf<VaultEntry?>(null) }

    // Drives the live codes and enforces the session timeout
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            VaultLock.lockIfExpired()
            delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Vault", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { VaultLock.lock() }) {
                Icon(Icons.Filled.Lock, contentDescription = "Lock vault")
            }
        }

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nothing here yet. Scan an authenticator QR code and " +
                        "tap Add to Vault, or turn on automatic vaulting in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    VaultEntryCard(
                        entry = entry,
                        now = now,
                        onDelete = { pendingDelete = entry }
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete key?") },
            text = {
                Text(
                    "The secret for ${entry.issuer.ifEmpty { entry.account }} will be " +
                        "removed permanently. Make sure it still exists in your " +
                        "authenticator app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVaultEntry(entry.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VaultEntryCard(entry: VaultEntry, now: Long, onDelete: () -> Unit) {
    val context = LocalContext.current
    val secret = remember(entry.id) { VaultCrypto.decrypt(entry.secretEnc) }
    val keyBytes = remember(entry.id) { secret?.let { Totp.decodeBase32(it) } }
    val isTotp = entry.type == "totp" && keyBytes != null
    var showSecret by remember(entry.id) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.issuer.ifEmpty { entry.account.ifEmpty { "Unnamed" } },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (entry.issuer.isNotEmpty() && entry.account.isNotEmpty()) {
                        Text(
                            text = entry.account,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    if (secret != null) {
                        showSecret = !showSecret
                        VaultLock.touch()
                    }
                }) {
                    Icon(
                        Icons.Filled.Key,
                        contentDescription = if (showSecret) "Hide secret key"
                        else "Show secret key",
                        tint = if (showSecret) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (secret == null) {
                Text(
                    text = "Could not decrypt this entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (isTotp) {
                val code = Totp.code(keyBytes!!, now, entry.period, entry.digits, entry.algorithm)
                val remaining = Totp.secondsRemaining(now, entry.period)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = code?.chunked(3)?.joinToString(" ") ?: "------",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${remaining}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = {
                        if (code != null) {
                            Actions.copy(context, "OTP code", code)
                            VaultLock.touch()
                        }
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code")
                    }
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (entry.period > 0) remaining.toFloat() / entry.period else 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "${entry.type.uppercase()} key, no live code. Use the key " +
                        "icon to view the secret.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (showSecret && secret != null) {
                Text(
                    text = "Secret key",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = secret.chunked(4).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        Actions.copy(context, "OTP secret", secret)
                        VaultLock.touch()
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy secret key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
