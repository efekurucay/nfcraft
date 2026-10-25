package com.nfcraft.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.ui.advanced.AdvancedScreen
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.emulate.EmulateScreen
import com.nfcraft.app.ui.profiles.ProfilesScreen
import com.nfcraft.app.ui.read.ReadScreen
import com.nfcraft.app.ui.settings.SettingsScreen
import com.nfcraft.app.ui.tasks.TasksScreen
import com.nfcraft.app.ui.tools.ToolsScreen
import com.nfcraft.app.ui.write.WriteScreen
import kotlinx.coroutines.launch

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val TopDestinations = listOf(
    TopDest("read", "Read", Icons.Filled.Nfc),
    TopDest("write", "Write", Icons.Filled.Edit),
    TopDest("tasks", "Tasks", Icons.Filled.PlayArrow),
    TopDest("emulate", "Emulate", Icons.Filled.Wifi),
    TopDest("more", "More", Icons.Filled.Apps),
)

@Composable
fun NfcraftRoot(
    container: AppContainer,
    startRoute: String = "read",
    nfcSupported: Boolean,
    nfcEnabled: Boolean,
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        container.nfcController.results.collect { r ->
            snackbar.showSnackbar("${r.label}: ${r.message}")
        }
    }

    Scaffold(
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val current = backStack?.destination
            NavigationBar {
                TopDestinations.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable("read") {
                ReadScreen(container.nfcController, container.taskRunController, nfcSupported, nfcEnabled)
            }
            composable("write") { WriteScreen(container) }
            composable("tasks") { TasksScreen(container) }
            composable("emulate") { EmulateScreen(container) }
            composable("more") { MoreHub(onNavigate = { navController.navigate(it) }) }
            composable("profiles") { ProfilesScreen(container) }
            composable("advanced") { AdvancedScreen(container) }
            composable("tools") { ToolsScreen(container) }
            composable("settings") { SettingsScreen(container) }
        }

        DangerousTaskDialog(container.taskConfirmations)
    }
}

@Composable
private fun DangerousTaskDialog(confirmations: com.nfcraft.app.tasks.TaskConfirmationController) {
    val pending by confirmations.pending.collectAsState()
    val request = pending ?: return
    AlertDialog(
        onDismissRequest = { confirmations.resolve(false) },
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Run privileged task?") },
        text = {
            Text("\"${request.descriptor.label}\" runs a privileged or destructive action (${request.descriptor.mimeType}). Only continue if you trust this tag.")
        },
        confirmButton = {
            TextButton(onClick = { confirmations.resolve(true) }) { Text("Run") }
        },
        dismissButton = {
            TextButton(onClick = { confirmations.resolve(false) }) { Text("Cancel") }
        },
    )
}

@Composable
private fun MoreHub(onNavigate: (String) -> Unit) {
    ScreenScaffold("More", subtitle = "Profiles, advanced commands, tag tools and settings.") {
        val entries = listOf(
            "profiles" to "Profiles",
            "tools" to "Tag tools",
            "advanced" to "Advanced commands",
            "settings" to "Settings",
        )
        entries.forEach { (route, label) ->
            Card(
                onClick = { onNavigate(route) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}
