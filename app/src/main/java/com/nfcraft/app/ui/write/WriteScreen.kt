package com.nfcraft.app.ui.write

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.nfc.NfcMode
import com.nfcraft.app.nfc.OpResult
import com.nfcraft.app.ui.components.EmptyState
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.TagTapPrompt
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.NdefRecordModel

@Composable
fun WriteScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val controller = container.nfcController
    val records = remember { mutableStateListOf<NdefRecordModel>() }
    var lock by remember { mutableStateOf(false) }

    // Add/edit sheet state.
    var showChooser by remember { mutableStateOf(false) }
    var editKind by remember { mutableStateOf<RecordKind?>(null) }
    var editIndex by remember { mutableStateOf(-1) } // -1 = adding a new record

    // Armed / result feedback.
    var armed by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<OpResult?>(null) }
    val busy by controller.busy.collectAsState()

    LaunchedEffect(Unit) {
        controller.results.collect { r ->
            if (armed) {
                lastResult = r
                armed = false
            }
        }
    }

    if (armed) {
        ArmedView(
            busy = busy,
            recordCount = records.size,
            onCancel = {
                controller.toRead()
                armed = false
            },
            modifier = modifier,
        )
    } else {
        val totalSize = container.ndefCodec.encodedSize(records.toList())
        ScreenScaffold(
            title = "Write",
            modifier = modifier,
            subtitle = "Build an NDEF message, then write it to a tag.",
        ) {
            lastResult?.let { r ->
                StatusBanner(
                    "${r.label}: ${r.message}",
                    if (r.success) Tone.Positive else Tone.Negative,
                )
            }

            SectionCard(title = "Records") {
                if (records.isEmpty()) {
                    EmptyState(
                        Icons.Filled.PostAdd,
                        "No records yet",
                        "Add a record to start building the message.",
                    )
                } else {
                    records.forEachIndexed { i, record ->
                        RecordRow(
                            index = i,
                            count = records.size,
                            record = record,
                            onEdit = {
                                kindOf(record)?.let { k ->
                                    editKind = k
                                    editIndex = i
                                }
                            },
                            onDelete = { records.removeAt(i) },
                            onMoveUp = { if (i > 0) records.add(i - 1, records.removeAt(i)) },
                            onMoveDown = { if (i < records.size - 1) records.add(i + 1, records.removeAt(i)) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showChooser = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add record", modifier = Modifier.padding(start = 8.dp))
                }
            }

            SectionCard(title = "Options") {
                LabeledValue("Encoded size", "$totalSize bytes · ${records.size} record(s)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Lock tag after writing (permanent)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = lock, onCheckedChange = { lock = it })
                }
                if (lock) {
                    StatusBanner(
                        "Locking is irreversible: the tag can never be rewritten after this.",
                        Tone.Negative,
                    )
                }
            }

            Button(
                onClick = {
                    lastResult = null
                    controller.arm(NfcMode.WriteNdef(records.toList(), lock))
                    armed = true
                },
                enabled = records.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Write to tag")
            }
        }
    }

    if (showChooser) {
        RecordTypeChooserSheet(
            onPick = { kind ->
                showChooser = false
                editKind = kind
                editIndex = -1
            },
            onDismiss = { showChooser = false },
        )
    }

    editKind?.let { kind ->
        val initial = editIndex.takeIf { it >= 0 }?.let { records[it] }
        RecordFormSheet(
            kind = kind,
            initial = initial,
            onSave = { built ->
                if (editIndex >= 0) records[editIndex] = built else records.add(built)
                editKind = null
                editIndex = -1
            },
            onDismiss = {
                editKind = null
                editIndex = -1
            },
        )
    }
}

@Composable
private fun RecordRow(
    index: Int,
    count: Int,
    record: NdefRecordModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val kind = kindOf(record)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (kind != null) {
            Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(typeLabel(record), style = MaterialTheme.typography.titleSmall)
            Text(
                summary(record),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = index < count - 1) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
        }
        // Only families the builder understands can be edited; others support delete only.
        if (kind != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
private fun ArmedView(
    busy: Boolean,
    recordCount: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (busy) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Writing…",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            } else {
                TagTapPrompt("Hold a tag to the back of your phone to write $recordCount record(s).")
            }
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text("Cancel")
        }
    }
}
