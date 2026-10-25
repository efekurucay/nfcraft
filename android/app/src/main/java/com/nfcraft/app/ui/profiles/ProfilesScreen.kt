@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nfcraft.app.ui.profiles

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.nfc.NfcMode
import com.nfcraft.app.ui.components.EmptyState
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.TagTapPrompt
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.NdefRecordModel
import com.nfcraft.feature.profiles.ProfileItem
import com.nfcraft.feature.profiles.TaskProfile
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfilesScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val repo = container.profileRepository
    val context = LocalContext.current

    var profiles by remember { mutableStateOf(repo.list()) }
    fun refresh() { profiles = repo.list() }

    var showCreate by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showImportCsv by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<TaskProfile?>(null) }
    var deleting by remember { mutableStateOf<TaskProfile?>(null) }
    var exporting by remember { mutableStateOf<TaskProfile?>(null) }

    var writingName by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<Pair<String, Tone>?>(null) }

    LaunchedEffect(Unit) {
        container.nfcController.results.collect { r ->
            if (writingName != null) {
                banner = "${r.label}: ${r.message}" to if (r.success) Tone.Positive else Tone.Negative
                writingName = null
            }
        }
    }

    // While a write is armed, the whole screen becomes the tap affordance.
    val armedName = writingName
    if (armedName != null) {
        Box(modifier.fillMaxSize()) {
            TagTapPrompt("Hold a tag to write \"$armedName\".")
            TextButton(
                onClick = {
                    container.nfcController.toRead()
                    writingName = null
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            ) { Text("Cancel") }
        }
        return
    }

    ScreenScaffold("Profiles", modifier, subtitle = "Save, write, import and export task profiles.") {
        banner?.let { (text, tone) -> StatusBanner(text, tone) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Text("New profile", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = { showImport = true }) { Text("Import JSON") }
            OutlinedButton(onClick = { showImportCsv = true }) { Text("Import CSV") }
        }

        if (profiles.isEmpty()) {
            EmptyState(
                Icons.Filled.FolderOpen,
                "No profiles yet",
                "Create a profile to save a reusable set of task items you can write to a tag.",
            )
        } else {
            profiles.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    onView = { viewing = profile },
                    onWrite = {
                        banner = null
                        val records = profile.items.map {
                            NdefRecordModel.MimeData(it.mime, it.payload.toByteArray())
                        }
                        container.nfcController.arm(NfcMode.WriteNdef(records))
                        writingName = profile.name
                    },
                    onExport = { exporting = profile },
                    onDelete = { deleting = profile },
                )
            }
        }
    }

    if (showCreate) {
        CreateProfileDialog(
            onDismiss = { showCreate = false },
            onSave = { profile ->
                repo.save(profile)
                refresh()
                showCreate = false
                banner = "Saved \"${profile.name}\"." to Tone.Positive
            },
        )
    }

    if (showImport) {
        ImportProfileDialog(
            onDismiss = { showImport = false },
            onImport = { text ->
                val parsed = runCatching { repo.importJson(text) }
                parsed.onSuccess { p ->
                    repo.save(p)
                    refresh()
                    showImport = false
                    banner = "Imported \"${p.name}\"." to Tone.Positive
                }
                parsed.exceptionOrNull()?.message ?: ""
            },
        )
    }

    viewing?.let { profile ->
        ViewProfileDialog(profile = profile, onDismiss = { viewing = null })
    }

    exporting?.let { profile ->
        ExportProfileDialog(
            json = repo.exportJson(profile),
            csv = repo.exportCsv(profile),
            onShare = { text, extension ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${profile.name}.$extension")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Share profile"))
            },
            onDismiss = { exporting = null },
        )
    }

    if (showImportCsv) {
        ImportCsvDialog(
            onDismiss = { showImportCsv = false },
            onImport = { name, csv ->
                val parsed = runCatching { repo.importCsv(name, csv) }
                parsed.onSuccess { p ->
                    repo.save(p)
                    refresh()
                    showImportCsv = false
                    banner = "Imported \"${p.name}\"." to Tone.Positive
                }
                parsed.exceptionOrNull()?.message ?: ""
            },
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete profile?") },
            text = { Text("\"${profile.name}\" will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.delete(profile.name)
                    refresh()
                    banner = "Deleted \"${profile.name}\"." to Tone.Neutral
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileCard(
    profile: TaskProfile,
    onView: () -> Unit,
    onWrite: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard(title = profile.name) {
        LabeledValue("Created", DateFormat.getDateTimeInstance().format(Date(profile.createdAtEpoch)))
        LabeledValue("Items", "${profile.items.size} item(s)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onWrite, enabled = profile.items.isNotEmpty()) {
                Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                Text("Write to tag", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onView) { Text("View") }
            OutlinedButton(onClick = onExport) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                Text("Export", modifier = Modifier.padding(start = 6.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ViewProfileDialog(profile: TaskProfile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(profile.name) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (profile.items.isEmpty()) {
                    Text("This profile has no items.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    profile.items.forEachIndexed { i, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LabeledValue("Item ${i + 1} · mime", item.mime)
                            Text(
                                item.payload.ifEmpty { "(empty payload)" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ExportProfileDialog(
    json: String,
    csv: String,
    onShare: (text: String, extension: String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("JSON", style = MaterialTheme.typography.titleSmall)
                Text(json, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Text("CSV", style = MaterialTheme.typography.titleSmall)
                Text(csv.ifEmpty { "(no items)" }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onShare(json, "json") }) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                    Text("JSON", modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = { onShare(csv, "csv") }) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                    Text("CSV", modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ImportCsvDialog(onDismiss: () -> Unit, onImport: (name: String, csv: String) -> String) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import CSV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A \"mime,payload\" header row is optional.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Profile name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("CSV") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                )
                error?.let { Text("Invalid CSV: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = { error = onImport(name.trim(), text).ifEmpty { null } },
                enabled = name.isNotBlank() && text.isNotBlank(),
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ImportProfileDialog(onDismiss: () -> Unit, onImport: (String) -> String) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paste exported profile JSON below.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("JSON") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp),
                )
                error?.let { Text("Invalid JSON: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = { error = onImport(text).ifEmpty { null } },
                enabled = text.isNotBlank(),
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class ItemDraft(val mime: String, val payload: String)

@Composable
private fun CreateProfileDialog(onDismiss: () -> Unit, onSave: (TaskProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    val rows = remember { mutableListOf(ItemDraft("", "")).toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Items", style = MaterialTheme.typography.titleSmall)
                rows.forEachIndexed { i, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = row.mime,
                            onValueChange = { rows[i] = row.copy(mime = it) },
                            label = { Text("MIME (e.g. w1/1)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = row.payload,
                                onValueChange = { rows[i] = row.copy(payload = it) },
                                label = { Text("Payload") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (rows.size > 1) {
                                IconButton(
                                    onClick = { rows.removeAt(i) },
                                    modifier = Modifier.align(Alignment.TopEnd),
                                ) { Icon(Icons.Filled.Delete, "Remove item") }
                            }
                        }
                    }
                }
                TextButton(onClick = { rows.add(ItemDraft("", "")) }) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Text("Add item", modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val items = rows
                        .filter { it.mime.isNotBlank() }
                        .map { ProfileItem(it.mime.trim(), it.payload) }
                    onSave(TaskProfile(name.trim(), System.currentTimeMillis(), items))
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
