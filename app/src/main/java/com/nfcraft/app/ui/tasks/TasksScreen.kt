package com.nfcraft.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.nfc.NfcMode
import com.nfcraft.app.ui.components.EmptyState
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.TagTapPrompt
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.TaskCategory
import com.nfcraft.core.model.TaskDescriptor
import com.nfcraft.core.model.TaskRecord

@Composable
fun TasksScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val controller = container.nfcController
    val catalog = remember { container.taskFactory.catalog() }
    // executorFor only matches by mime/category/number, so a blank record is a safe probe.
    val runnable = remember(catalog) {
        catalog.count { container.taskFactory.executorFor(TaskRecord(it, emptyList())) != null }
    }

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var builderFor by remember { mutableStateOf<TaskDescriptor?>(null) }
    var armed by remember { mutableStateOf<TaskRecord?>(null) }
    var lastResult by remember { mutableStateOf<ResultBanner?>(null) }

    val busy by controller.busy.collectAsState()

    LaunchedEffect(controller) {
        controller.results.collect { op ->
            armed = null
            lastResult = ResultBanner(op.success, "${op.label}: ${op.message}")
        }
    }

    builderFor?.let { descriptor ->
        TaskBuilderSheet(
            descriptor = descriptor,
            onDismiss = { builderFor = null },
            onWrite = { record, lock ->
                builderFor = null
                lastResult = null
                armed = record
                controller.arm(NfcMode.WriteTask(record, lock))
            },
        )
    }

    val categories = remember(catalog) { catalog.map { it.category }.distinct() }
    val filtered = remember(query, selectedCategory, catalog) {
        catalog.filter { descriptor ->
            (selectedCategory == null || descriptor.category == selectedCategory) &&
                (query.isBlank() || descriptor.label.contains(query.trim(), ignoreCase = true))
        }.groupBy { it.category }
    }

    ScreenScaffold(
        title = "Tasks",
        subtitle = "$runnable of ${catalog.size} tasks runnable on this device  ·  ${categories.size} categories",
        modifier = modifier,
    ) {
        lastResult?.let {
            StatusBanner(it.text, tone = if (it.success) Tone.Positive else Tone.Negative)
        }

        if (armed != null) {
            ArmedPanel(
                record = armed!!,
                busy = busy,
                onCancel = {
                    armed = null
                    controller.toRead()
                },
            )
            return@ScreenScaffold
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search tasks") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All") },
            )
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    label = { Text(category.label) },
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Filled.Search,
                "No tasks found",
                "No task matches \"${query.trim()}\". Try a different term or category.",
            )
        } else {
            // Preserve the catalog's category order.
            categories.filter { filtered.containsKey(it) }.forEach { category ->
                val tasks = filtered.getValue(category)
                SectionCard(title = "${category.label}  (${tasks.size})") {
                    tasks.forEach { descriptor ->
                        TaskRow(descriptor = descriptor, onClick = { builderFor = descriptor })
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(descriptor: TaskDescriptor, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (descriptor.dangerous) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Dangerous task",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                descriptor.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (descriptor.dangerous) {
                Text(
                    "Privileged — requires Settings opt-in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArmedPanel(record: TaskRecord, busy: Boolean, onCancel: () -> Unit) {
    SectionCard(title = record.descriptor.label) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            TagTapPrompt(
                if (busy) "Writing to tag…" else "Hold a tag to the phone to write this task",
            )
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

private data class ResultBanner(val success: Boolean, val text: String)
