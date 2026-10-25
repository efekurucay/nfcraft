package com.nfcraft.app.ui.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcraft.app.nfc.NfcController
import com.nfcraft.app.nfc.ReadState
import com.nfcraft.app.tasks.TaskRunController
import com.nfcraft.app.ui.components.EmptyState
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.TagTapPrompt
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.TagInfo
import com.nfcraft.core.model.TaskRecord
import com.nfcraft.core.model.TaskResult

@Composable
fun ReadScreen(
    controller: NfcController,
    runController: TaskRunController,
    nfcSupported: Boolean,
    nfcEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by controller.read.collectAsState()

    when {
        !nfcSupported -> EmptyState(Icons.Filled.Nfc, "No NFC", "This device has no NFC hardware.", modifier.fillMaxSize())
        !nfcEnabled -> EmptyState(Icons.Filled.Nfc, "NFC is off", "Enable NFC in system settings to scan tags.", modifier.fillMaxSize())
        else -> when (val s = state) {
            ReadState.Waiting -> TagTapPrompt("Hold a tag to the back of your phone to read it.", modifier)
            ReadState.Reading -> TagTapPrompt("Reading…", modifier)
            is ReadState.Error -> ScreenScaffold("Read failed", modifier, subtitle = "Try again.") {
                StatusBanner("(${s.code}) ${s.message}", Tone.Negative)
            }
            is ReadState.Loaded -> TagDetail(s.tag, runController, modifier)
        }
    }
}

@Composable
private fun TagDetail(tag: TagInfo, runController: TaskRunController, modifier: Modifier = Modifier) {
    val tasks = remember(tag) { runController.detect(tag.ndef?.records ?: emptyList()) }

    ScreenScaffold(tag.chip.type.displayName, modifier, subtitle = tag.serial) {
        if (tasks.isNotEmpty()) {
            AutomationCard(tasks, runController)
        }

        SectionCard(title = "Chip") {
            LabeledValue("Serial", tag.serial)
            LabeledValue("Type", tag.chip.type.displayName)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tag.chip.signatureVerified) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = MaterialTheme.colorScheme.tertiary)
                }
                LabeledValue("Originality signature", if (tag.chip.signatureVerified) "verified (genuine NXP)" else "not verified")
            }
            tag.chip.manufacturer?.let { LabeledValue("Manufacturer", it) }
            LabeledValue("Technologies", tag.techList.joinToString().ifEmpty { "—" })
        }

        val ndef = tag.ndef
        if (ndef != null) {
            SectionCard(title = "NDEF") {
                LabeledValue("Content", "${ndef.records.size} record(s) · ${ndef.byteSize} / ${ndef.maxSize} bytes")
                LabeledValue("Writable", if (ndef.writable) "yes" else "no")
                ndef.records.forEachIndexed { i, r ->
                    LabeledValue("Record ${i + 1}", r.toString())
                }
            }
        } else {
            SectionCard(title = "NDEF") {
                Text("No NDEF data on this tag.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AutomationCard(tasks: List<TaskRecord>, runController: TaskRunController) {
    val runState by runController.state.collectAsState()
    val hasDangerous = tasks.any { it.descriptor.dangerous }

    SectionCard(title = "Automation") {
        Text(
            "This tag carries ${tasks.size} task(s).",
            style = MaterialTheme.typography.bodyMedium,
        )
        tasks.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (t.descriptor.dangerous) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
                Text("• ${t.descriptor.label}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (hasDangerous) {
            StatusBanner(
                "Includes privileged/root tasks. They run only if enabled in Settings and confirmed.",
                Tone.Negative,
            )
        }

        when (val r = runState) {
            TaskRunController.RunState.Idle -> Unit
            is TaskRunController.RunState.Running -> StatusBanner("Running ${r.count} task(s)…", Tone.Neutral)
            is TaskRunController.RunState.Finished -> StatusBanner(describe(r.result), toneOf(r.result))
        }

        Button(
            onClick = { runController.run(tasks) },
            enabled = runState !is TaskRunController.RunState.Running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Text("Run tasks", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun describe(result: TaskResult): String = when (result) {
    TaskResult.Success -> "All tasks completed."
    is TaskResult.Failure -> "Stopped: ${result.message}"
    TaskResult.Aborted -> "Aborted (not confirmed or not allowed)."
    is TaskResult.ConditionBranch -> "Finished."
}

private fun toneOf(result: TaskResult): Tone = when (result) {
    TaskResult.Success -> Tone.Positive
    is TaskResult.Failure -> Tone.Negative
    else -> Tone.Neutral
}
