@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nfcraft.app.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.nfc.NfcMode
import com.nfcraft.app.ui.components.LabeledValue
import com.nfcraft.app.ui.components.ScreenScaffold
import com.nfcraft.app.ui.components.SectionCard
import com.nfcraft.app.ui.components.StatusBanner
import com.nfcraft.app.ui.components.Tone
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AdvancedScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val controller = container.nfcController
    val busy by controller.busy.collectAsState()
    val mode by controller.mode.collectAsState()
    // Observed per the console contract; the live value is read when a result lands.
    val lastResponse by controller.apduResponse.collectAsState()

    var input by remember { mutableStateOf("") }
    val history = remember { emptyList<ApduExchange>().toMutableStateList() }
    var pendingSent by remember { mutableStateOf<String?>(null) }

    val parsed = parseHex(input)
    val showError = input.isNotBlank() && parsed == null
    val armed = mode is NfcMode.RawApdu

    androidx.compose.runtime.LaunchedEffect(Unit) {
        controller.results.collectLatest { r ->
            if (r.label != "APDU") return@collectLatest
            val sent = pendingSent ?: return@collectLatest
            pendingSent = null
            if (r.success) {
                val bytes = controller.apduResponse.value ?: ByteArray(0)
                history.add(
                    ApduExchange(
                        sentHex = sent,
                        success = true,
                        detail = if (bytes.isEmpty()) "(empty response)" else formatHex(bytes),
                        ascii = if (bytes.isEmpty()) null else renderAscii(bytes),
                    ),
                )
            } else {
                history.add(ApduExchange(sentHex = sent, success = false, detail = r.message, ascii = null))
            }
        }
    }

    ScreenScaffold(
        title = "Advanced",
        modifier = modifier,
        subtitle = "Raw APDU / hex console",
    ) {
        SectionCard(title = "Command") {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hex bytes") },
                placeholder = { Text("e.g. 30 00") },
                isError = showError,
                supportingText = {
                    Text(
                        if (showError) "Enter valid hex with an even number of digits."
                        else "Spaces and line breaks are ignored.",
                    )
                },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apduPresets.forEach { preset ->
                    AssistChip(
                        onClick = { input = preset.hex },
                        label = { Text(preset.label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        val bytes = parseHex(input) ?: return@Button
                        pendingSent = formatHex(bytes)
                        controller.arm(NfcMode.RawApdu(bytes))
                    },
                    enabled = parsed != null && !busy,
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Text("Send", modifier = Modifier.padding(start = 8.dp))
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = { history.clear() }) { Text("Clear") }
                }
            }

            Text(
                "Commands are sent as-is over transceive. The app adds no CRC and no timeout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            busy -> StatusBanner("Sending command to tag…", Tone.Neutral)
            armed -> StatusBanner("Armed — hold a tag to the back of your phone to send.", Tone.Neutral)
        }

        if (history.isNotEmpty()) {
            Text("History", style = MaterialTheme.typography.titleMedium)
            history.asReversed().forEach { exchange ->
                SectionCard {
                    LabeledValue("Sent", exchange.sentHex, Modifier.fillMaxWidth())
                    if (exchange.success) {
                        LabeledValue("Received", exchange.detail, Modifier.fillMaxWidth())
                        exchange.ascii?.let { LabeledValue("ASCII", it, Modifier.fillMaxWidth()) }
                    } else {
                        StatusBanner(exchange.detail, Tone.Negative)
                    }
                }
            }
        }
    }
}
