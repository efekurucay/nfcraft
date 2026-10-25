package com.nfcraft.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.TaskDescriptor
import com.nfcraft.core.model.TaskParam
import com.nfcraft.core.model.TaskParamType
import com.nfcraft.core.model.TaskRecord

/**
 * Builder for one [TaskDescriptor]: one input per declared [TaskParam], a
 * "lock after write" toggle, and a "Write to tag" action that assembles a
 * [TaskRecord] whose args line up with the params in payload order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBuilderSheet(
    descriptor: TaskDescriptor,
    onDismiss: () -> Unit,
    onWrite: (record: TaskRecord, lock: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // One text slot per param; booleans ride the same slot as "true"/"false".
    val values = remember(descriptor) {
        mutableStateListOf<String>().apply {
            descriptor.params.forEach { add(if (it.type == TaskParamType.BOOLEAN) "false" else "") }
        }
    }
    var lock by remember(descriptor) { mutableStateOf(false) }

    // Required params must carry a value; booleans are always valid.
    val canWrite = descriptor.params.indices.all { i ->
        val param = descriptor.params[i]
        param.optional || param.type == TaskParamType.BOOLEAN || values[i].isNotBlank()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (descriptor.dangerous) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Dangerous task",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(descriptor.label, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "${descriptor.category.label}  ·  ${descriptor.mimeType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (descriptor.dangerous) {
                StatusBanner(
                    "This task runs a privileged or destructive action. It is denied at run time unless dangerous tasks are enabled in Settings.",
                    tone = Tone.Negative,
                )
            }

            if (descriptor.params.isEmpty()) {
                Text(
                    "This task takes no parameters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SectionCard(title = "Parameters") {
                    descriptor.params.forEachIndexed { index, param ->
                        ParamField(
                            param = param,
                            value = values[index],
                            onValue = { values[index] = it },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lock tag after write", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Make the tag permanently read-only. This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = lock, onCheckedChange = { lock = it })
            }

            Button(
                onClick = {
                    // Args follow param order; drop trailing empty optionals so the
                    // wire payload stays compact and valid.
                    val args = descriptor.params
                        .indices
                        .map { values[it] }
                        .dropLastWhile { it.isEmpty() }
                    onWrite(TaskRecord(descriptor, args), lock)
                },
                enabled = canWrite,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Contactless, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Write to tag")
            }
        }
    }
}

@Composable
private fun ParamField(param: TaskParam, value: String, onValue: (String) -> Unit) {
    val label = param.name + if (param.optional) " (optional)" else ""
    when (param.type) {
        TaskParamType.BOOLEAN -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = value == "true",
                onCheckedChange = { onValue(if (it) "true" else "false") },
            )
        }

        TaskParamType.NUMBER -> OutlinedTextField(
            value = value,
            onValueChange = { onValue(it) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        // ENUM / PACKAGE / FILE render as plain text entry for now.
        else -> OutlinedTextField(
            value = value,
            onValueChange = { onValue(it) },
            label = { Text(label) },
            placeholder = { Text(param.type.name.lowercase()) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
