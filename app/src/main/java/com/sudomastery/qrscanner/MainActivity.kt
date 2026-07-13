package com.sudomastery.qrscanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sudomastery.qrscanner.ui.HistoryScreen
import com.sudomastery.qrscanner.ui.MainViewModel
import com.sudomastery.qrscanner.ui.ScanScreen
import com.sudomastery.qrscanner.ui.SettingsScreen
import com.sudomastery.qrscanner.ui.VaultScreen
import com.sudomastery.qrscanner.ui.theme.QrScannerTheme
import com.sudomastery.qrscanner.vault.VaultLock

private data class Destination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// FragmentActivity is required by BiometricPrompt for the vault unlock
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            QrScannerTheme(themeColor = settings.themeColor) {
                AppScaffold(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Relock the vault whenever the app leaves the foreground
        VaultLock.lock()
    }
}

@Composable
private fun AppScaffold(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val destinations = listOf(
        Destination("scan", "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
        Destination("history", "History", Icons.Filled.History, Icons.Outlined.History),
        Destination("vault", "Vault", Icons.Filled.Lock, Icons.Outlined.Lock),
        Destination("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo("scan") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) destination.selectedIcon
                                else destination.unselectedIcon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "scan",
            modifier = Modifier.padding(padding)
        ) {
            composable("scan") { ScanScreen(viewModel) }
            composable("history") { HistoryScreen(viewModel) }
            composable("vault") { VaultScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }
}
