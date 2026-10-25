package com.nfcraft.app.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.nfc.NfcMode
import com.nfcraft.app.nfc.OpResult
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.TagTapPrompt
import com.nfcraft.app.ui.components.Tone
import com.nfcraft.core.model.TagDump

@Composable
fun ToolsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val controller = container.nfcController
    val mode by controller.mode.collectAsState()
    val busy by controller.busy.collectAsState()

    // Per-tool feedback captured from the controller's one-shot results.
    var dumpResult by remember { mutableStateOf<OpResult?>(null) }
    var dump by remember { mutableStateOf<TagDump?>(null) }
    var passwordResult by remember { mutableStateOf<OpResult?>(null) }
    var lockResult by remember { mutableStateOf<OpResult?>(null) }

    LaunchedEffect(controller) {
        controller.results.collect { op ->
            when (op.label) {
                "Dump" -> {
                    dumpResult = op
                    if (op.dump != null) dump = op.dump
                }
                "Set password", "Remove password" -> passwordResult = op
                "Lock" -> lockResult = op
            }
        }
    }

    ScreenScaffold(
        title = "Tools",
        modifier = modifier,
        subtitle = "Read a full memory dump, protect a tag with a password, or lock it for good.",
    ) {
        DumpCard(
            armed = mode is NfcMode.Dump,
            busy = busy,
            result = dumpResult,
            dump = dump,
            onArm = {
                dumpResult = null
                controller.arm(NfcMode.Dump)
            },
            onCancel = { controller.toRead() },
        )

        PasswordCard(
            armed = mode is NfcMode.SetPassword || mode is NfcMode.RemovePassword,
            busy = busy,
            result = passwordResult,
            onSet = { pw ->
                passwordResult = null
                controller.arm(NfcMode.SetPassword(pw))
            },
            onRemove = { pw ->
                passwordResult = null
                controller.arm(NfcMode.RemovePassword(pw))
            },
            onCancel = { controller.toRead() },
        )

        ReadOnlyCard(
            armed = mode is NfcMode.MakeReadOnly,
            busy = busy,
            result = lockResult,
            onArm = {
                lockResult = null
                controller.arm(NfcMode.MakeReadOnly)
            },
            onCancel = { controller.toRead() },
        )
    }
}

@Composable
private fun DumpCard(
    armed: Boolean,
    busy: Boolean,
    result: OpResult?,
    dump: TagDump?,
    onArm: () -> Unit,
    onCancel: () -> Unit,
) {
    SectionCard(title = "Dump memory") {
        Text(
            "Capture every block of the tag's memory as raw hex. This only reads the tag; it changes nothing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (armed) {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                TagTapPrompt("Hold the tag to the back of your phone to read the full dump.")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Cancel")
            }
        } else {
            Button(onClick = onArm, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Read full dump")
            }
        }

        if (result != null && !result.success) {
            StatusBanner("Dump failed · ${result.message}", Tone.Negative)
        }

        if (dump != null) {
            HexDump(dump)
        }
    }
}

@Composable
private fun HexDump(dump: TagDump) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val fullHex = remember(dump) {
        dump.blocks.mapIndexed { i, block -> "%03d  %s".format(i, block.toHex()) }.joinToString("\n")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "${dump.chip.displayName} · ${dump.blocks.size} blocks × ${dump.blockSize} B",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = {
                clipboard.setText(AnnotatedString(fullHex))
                copied = true
            },
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copy")
        }
    }

    if (copied) {
        StatusBanner("Full hex dump copied to clipboard.", Tone.Positive)
    }

    Column(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        dump.blocks.forEachIndexed { index, block ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "%03d".format(index),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    block.toHex(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun PasswordCard(
    armed: Boolean,
    busy: Boolean,
    result: OpResult?,
    onSet: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    SectionCard(title = "Password protection") {
        StatusBanner(
            "Caution: this permanently changes the tag. A wrong or forgotten password can soft-lock writes and leave the tag unchangeable. Record the password somewhere safe before you continue.",
            Tone.Negative,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            supportingText = { Text("8 hex characters (e.g. A1B2C3D4) or any passphrase.") },
            singleLine = true,
            enabled = !armed && !busy,
            modifier = Modifier.fillMaxWidth(),
        )

        if (armed) {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                TagTapPrompt("Hold the tag to the back of your phone to apply the change.")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Cancel")
            }
        } else {
            val canAct = password.isNotBlank() && !busy
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSet(password) },
                    enabled = canAct,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Set password")
                }
                OutlinedButton(
                    onClick = { onRemove(password) },
                    enabled = canAct,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Remove password")
                }
            }
        }

        if (result != null) {
            StatusBanner(
                "${result.label} · ${result.message}",
                if (result.success) Tone.Positive else Tone.Negative,
            )
        }
    }
}

@Composable
private fun ReadOnlyCard(
    armed: Boolean,
    busy: Boolean,
    result: OpResult?,
    onArm: () -> Unit,
    onCancel: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    SectionCard(title = "Make read-only") {
        StatusBanner(
            "Irreversible: locking the tag permanently disables all future writes. There is no way to undo this, and the tag can never be rewritten or reset.",
            Tone.Negative,
        )

        if (armed) {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                TagTapPrompt("Hold the tag to the back of your phone to lock it permanently.")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Cancel")
            }
        } else {
            Button(
                onClick = { showConfirm = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lock tag forever")
            }
        }

        if (result != null) {
            StatusBanner(
                "${result.label} · ${result.message}",
                if (result.success) Tone.Positive else Tone.Negative,
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text("Lock this tag?") },
            text = {
                Text("This permanently locks the tag; it can never be written again. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onArm()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Lock forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
