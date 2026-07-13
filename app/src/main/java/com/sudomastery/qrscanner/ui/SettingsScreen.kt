package com.sudomastery.qrscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingGroup("Scanning") {
            SettingSwitch(
                icon = Icons.Filled.Vibration,
                title = "Vibrate on scan",
                subtitle = "Short buzz when a code is detected",
                checked = settings.vibrateOnScan,
                onCheckedChange = viewModel::setVibrateOnScan
            )
            SettingSwitch(
                icon = Icons.Filled.VolumeUp,
                title = "Beep on scan",
                subtitle = "Play a short tone when a code is detected",
                checked = settings.beepOnScan,
                onCheckedChange = viewModel::setBeepOnScan
            )
        }

        SettingGroup("Links") {
            SettingSwitch(
                icon = Icons.Filled.OpenInNew,
                title = "Auto open links",
                subtitle = "Open scanned URLs in the browser right away " +
                    "instead of showing them first",
                checked = settings.autoOpenLinks,
                onCheckedChange = viewModel::setAutoOpenLinks
            )
            SettingSwitch(
                icon = Icons.Filled.LinkOff,
                title = "Remove trackers from URLs",
                subtitle = "Strip utm, fbclid, gclid and other tracking " +
                    "parameters before opening or copying",
                checked = settings.removeTrackers,
                onCheckedChange = viewModel::setRemoveTrackers
            )
        }

        SettingGroup("Vault") {
            SettingSwitch(
                icon = Icons.Filled.Lock,
                title = "Auto vault authenticator scans",
                subtitle = "Scanned otpauth codes go straight to the vault, " +
                    "encrypted and kept out of history",
                checked = settings.autoVaultOtp,
                onCheckedChange = viewModel::setAutoVaultOtp
            )
        }

        SettingGroup("History") {
            SettingSwitch(
                icon = Icons.Filled.History,
                title = "Save scan history",
                subtitle = "Keep a local record of everything you scan",
                checked = settings.saveHistory,
                onCheckedChange = viewModel::setSaveHistory
            )
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
