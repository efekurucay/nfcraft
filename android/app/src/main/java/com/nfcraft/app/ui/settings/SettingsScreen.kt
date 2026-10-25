package com.nfcraft.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.Tone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val settings = container.settings
    val dangerousEnabled by settings.dangerousEnabled.collectAsState()
    val whitelist by settings.whitelist.collectAsState()
    val dangerousTasks = remember(container) { container.taskFactory.catalog().filter { it.dangerous } }

    ScreenScaffold(
        title = "Settings",
        subtitle = "Privileged-task control and app info.",
        modifier = modifier,
    ) {
        SectionCard(title = "Dangerous tasks") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow dangerous tasks", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Off by default. Must stay off unless you understand the risk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dangerousEnabled,
                    onCheckedChange = { settings.setDangerousEnabled(it) },
                )
            }

            StatusBanner(
                "These tasks run privileged and root (su) actions and can perform destructive, " +
                    "irreversible operations on your device. They are denied by default. Enable only " +
                    "if you know exactly what each task does.",
                tone = Tone.Negative,
            )

            if (dangerousEnabled) {
                if (dangerousTasks.isEmpty()) {
                    Text(
                        "No dangerous tasks are registered in this build.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Allow-list",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Pick which dangerous tasks may run. With none selected, every dangerous task " +
                            "is allowed while the switch above is on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dangerousTasks.forEach { task ->
                            val selected = task.mimeType in whitelist
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = whitelist.toMutableSet()
                                    if (selected) next.remove(task.mimeType) else next.add(task.mimeType)
                                    settings.setWhitelist(next)
                                },
                                label = { Text(task.label) },
                            )
                        }
                    }
                }
            }
        }

        SectionCard(title = "About") {
            LabeledValue(label = "App", value = "NFCraft")
            Text(
                "A modern NFC toolkit: read, write, run tasks, and emulate tags.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Engine reverse-engineering notes live in the project wiki.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "NFC") {
            StatusBanner(
                "NFC and Host Card Emulation are provided by the system. If NFC is off, enable it in " +
                    "Android settings; reading, writing, and tag emulation need it on.",
                tone = Tone.Neutral,
            )
        }
    }
}
