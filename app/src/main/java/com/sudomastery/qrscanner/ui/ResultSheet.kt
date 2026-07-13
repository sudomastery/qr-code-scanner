package com.sudomastery.qrscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sudomastery.qrscanner.data.Settings
import com.sudomastery.qrscanner.parsing.ScanContent
import com.sudomastery.qrscanner.parsing.UrlCleaner
import com.sudomastery.qrscanner.util.Actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    content: ScanContent,
    settings: Settings,
    vaulted: Boolean,
    onAddToVault: (ScanContent.Otp) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (content) {
                is ScanContent.Url -> UrlResult(content, settings)
                is ScanContent.Otp -> OtpResult(content, vaulted, onAddToVault)
                is ScanContent.Wifi -> WifiResult(content)
                is ScanContent.Email -> SimpleResult("Email", content.address, content)
                is ScanContent.Phone -> SimpleResult("Phone number", content.number, content)
                is ScanContent.Sms -> SimpleResult("SMS to ${content.number}", content.message, content)
                is ScanContent.Geo -> SimpleResult("Location", "${content.lat}, ${content.lng}", content)
                is ScanContent.Contact -> SimpleResult("Contact", content.raw, content)
                is ScanContent.Passkey -> PasskeyResult(content)
                is ScanContent.PlainText -> SimpleResult("Text", content.raw, content)
            }
        }
    }
}

@Composable
private fun UrlResult(content: ScanContent.Url, settings: Settings) {
    val context = LocalContext.current
    val hadTrackers = UrlCleaner.hasTrackers(content.raw)
    val displayUrl = if (settings.removeTrackers) content.cleaned else content.raw

    SheetTitle("Link")
    if (settings.removeTrackers && hadTrackers) {
        AssistChip(
            onClick = {},
            label = { Text("Trackers removed") },
            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) }
        )
    }
    ValueCard(displayUrl)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { Actions.openUrl(context, displayUrl) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open")
        }
        FilledTonalButton(onClick = { Actions.copy(context, "URL", displayUrl) }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
        }
        FilledTonalButton(onClick = { Actions.share(context, displayUrl) }) {
            Icon(Icons.Filled.Share, contentDescription = "Share")
        }
    }
    if (settings.removeTrackers && hadTrackers) {
        Text(
            text = "Original: ${content.raw}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OtpResult(
    content: ScanContent.Otp,
    vaulted: Boolean,
    onAddToVault: (ScanContent.Otp) -> Unit
) {
    val context = LocalContext.current
    var secretVisible by remember { mutableStateOf(false) }
    var inVault by remember { mutableStateOf(vaulted) }

    SheetTitle("Authenticator key")

    if (content.type == "migration") {
        Text(
            text = "This is a Google Authenticator export code. It bundles several " +
                "accounts in a compressed format, so individual secrets cannot be " +
                "shown here. You can copy the raw value below.",
            style = MaterialTheme.typography.bodyMedium
        )
        ValueCard(content.raw)
        FilledTonalButton(onClick = { Actions.copy(context, "OTP export", content.raw) }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Copy raw value")
        }
        return
    }

    if (content.issuer.isNotEmpty()) LabeledValue("Issuer", content.issuer)
    if (content.account.isNotEmpty()) LabeledValue("Account", content.account)
    LabeledValue(
        "Type",
        "${content.type.uppercase()} / ${content.algorithm} / " +
            "${content.digits} digits / ${content.period}s"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Secret key",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { secretVisible = !secretVisible }) {
                    Icon(
                        if (secretVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                        contentDescription = "Toggle secret visibility",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = if (secretVisible) content.secret else "•".repeat(16),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { Actions.copy(context, "OTP secret", content.secret) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy secret key")
            }
        }
    }

    if (content.secret.isNotEmpty()) {
        Button(
            onClick = {
                onAddToVault(content)
                inVault = true
            },
            enabled = !inVault,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (inVault) "In vault" else "Add to Vault")
        }
        if (inVault) {
            Text(
                text = "Saved to the vault and kept out of history. Unlock the " +
                    "Vault tab to see it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    FilledTonalButton(onClick = { Actions.copy(context, "OTP URI", content.raw) }) {
        Text("Copy full otpauth URI")
    }
}

@Composable
private fun PasskeyResult(content: ScanContent.Passkey) {
    val context = LocalContext.current

    SheetTitle("Passkey sign in")
    Text(
        text = "This code starts a passkey sign in on this phone, like the QR " +
            "shown when signing in on Windows or in a browser. Passkey codes " +
            "expire quickly, so continue right away.",
        style = MaterialTheme.typography.bodyMedium
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { Actions.openRaw(context, content.raw) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Continue sign in")
        }
        FilledTonalButton(onClick = { Actions.copy(context, "Passkey link", content.raw) }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
        }
    }
}

@Composable
private fun WifiResult(content: ScanContent.Wifi) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    SheetTitle("Wi-Fi network")
    LabeledValue("Network", content.ssid)
    LabeledValue("Security", content.security + if (content.hidden) " (hidden)" else "")

    if (content.password.isNotEmpty()) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
                Text(
                    text = if (passwordVisible) content.password else "•".repeat(12),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { Actions.copy(context, "Wi-Fi password", content.password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy password")
                }
            }
        }
    }
}

@Composable
private fun SimpleResult(title: String, value: String, content: ScanContent) {
    val context = LocalContext.current
    SheetTitle(title)
    ValueCard(value)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
            onClick = { Actions.copy(context, "Scan", value) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Copy")
        }
        FilledTonalButton(
            onClick = { Actions.share(context, value) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
        if (content !is ScanContent.PlainText && content !is ScanContent.Contact) {
            FilledTonalButton(onClick = { Actions.openRaw(context, content.raw) }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open")
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ValueCard(value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
