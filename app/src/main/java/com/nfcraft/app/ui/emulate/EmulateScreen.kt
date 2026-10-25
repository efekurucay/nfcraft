package com.nfcraft.app.ui.emulate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.NdefRecordModel
import com.nfcraft.feature.hce.EmulatedTagStore

private enum class RecordType(val label: String) { Uri("URI"), Text("Text") }

@Composable
fun EmulateScreen(container: AppContainer, modifier: Modifier = Modifier) {
    // Draft the user edits; the committed payload lives in EmulatedTagStore.
    val draft = remember { mutableStateListOf<NdefRecordModel>().apply { addAll(EmulatedTagStore.records) } }
    var served by remember { mutableStateOf(EmulatedTagStore.records) }
    var type by remember { mutableStateOf(RecordType.Uri) }
    var field by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Emulate",
        subtitle = "Serve this device as an NFC Forum Type 4 Tag over Host Card Emulation.",
        modifier = modifier,
    ) {
        SectionCard(title = "Emulated NDEF content") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        leadingIcon = {
                            Icon(
                                if (t == RecordType.Uri) Icons.Filled.Link else Icons.Filled.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = type == RecordType.Uri,
                label = { Text(if (type == RecordType.Uri) "URI" else "Text") },
                placeholder = {
                    Text(if (type == RecordType.Uri) "https://example.com" else "Hello from NFCraft")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (type == RecordType.Uri) KeyboardType.Uri else KeyboardType.Text,
                ),
            )
            Button(
                onClick = {
                    val value = field.trim()
                    if (value.isNotEmpty()) {
                        draft.add(
                            when (type) {
                                RecordType.Uri -> NdefRecordModel.Uri(value)
                                RecordType.Text -> NdefRecordModel.Text(value, languageCode = "en")
                            },
                        )
                        field = ""
                    }
                },
                enabled = field.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Add record")
            }

            if (draft.isEmpty()) {
                Text(
                    "No records in the draft. The emulated tag will serve an empty message.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.forEachIndexed { index, record ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LabeledValue(
                                label = recordLabel(record),
                                value = recordValue(record),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { draft.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove record")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        draft.clear()
                        draft.add(NdefRecordModel.Uri(EmulatedTagStore.DEFAULT_URI))
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset default") }
                Button(
                    onClick = {
                        EmulatedTagStore.records = draft.toList()
                        served = EmulatedTagStore.records
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
        }

        SectionCard(title = "Currently serving") {
            if (served.isEmpty()) {
                Text(
                    "An empty NDEF message is being served.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                served.forEach { record ->
                    LabeledValue(label = recordLabel(record), value = recordValue(record))
                }
            }
        }

        SectionCard(title = "How emulation works") {
            LabeledValue(label = "AID", value = "D2760000850101 · NFC Forum NDEF Tag App")
            LabeledValue(label = "Mode", value = "Read-only emulation")
            StatusBanner(
                "Another NFC reader or phone can read this device as a tag while NFCraft is in the " +
                    "foreground, subject to the system's HCE routing. The reader only reads the NDEF " +
                    "message above; it cannot write back. The emulation service is always registered, " +
                    "so no start/stop control is needed.",
                tone = Tone.Neutral,
            )
        }
    }
}

private fun recordLabel(record: NdefRecordModel): String = when (record) {
    is NdefRecordModel.Uri -> "URI"
    is NdefRecordModel.Text -> "Text · ${record.languageCode}"
    else -> "Record"
}

private fun recordValue(record: NdefRecordModel): String = when (record) {
    is NdefRecordModel.Uri -> record.uri
    is NdefRecordModel.Text -> record.text
    else -> record.toString()
}
