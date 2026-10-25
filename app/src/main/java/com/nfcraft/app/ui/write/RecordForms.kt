package com.nfcraft.app.ui.write

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.nfcraft.core.model.NdefRecordModel

/** The record families the builder can compose, with their chooser presentation. */
enum class RecordKind(val title: String, val icon: ImageVector) {
    Uri("URL", Icons.Filled.Link),
    Text("Text", Icons.Filled.TextFields),
    Wifi("Wi-Fi", Icons.Filled.Wifi),
    Phone("Phone", Icons.Filled.Phone),
    Email("Email", Icons.Filled.Email),
    Sms("SMS", Icons.Filled.Sms),
    Geo("Location", Icons.Filled.Place),
    Application("App", Icons.Filled.Android),
    Mime("Custom MIME", Icons.Filled.DataObject),
    Contact("Contact", Icons.Filled.ContactPage),
    Bluetooth("Bluetooth", Icons.Filled.Bluetooth),
}

/** Maps an existing record back to the chooser kind, for editing. Null for families not built here. */
fun kindOf(record: NdefRecordModel): RecordKind? = when (record) {
    is NdefRecordModel.Uri -> RecordKind.Uri
    is NdefRecordModel.Text -> RecordKind.Text
    is NdefRecordModel.Wifi -> RecordKind.Wifi
    is NdefRecordModel.Phone -> RecordKind.Phone
    is NdefRecordModel.Email -> RecordKind.Email
    is NdefRecordModel.Sms -> RecordKind.Sms
    is NdefRecordModel.Geo -> RecordKind.Geo
    is NdefRecordModel.Application -> RecordKind.Application
    is NdefRecordModel.MimeData -> RecordKind.Mime
    is NdefRecordModel.Contact -> RecordKind.Contact
    is NdefRecordModel.Bluetooth -> RecordKind.Bluetooth
    else -> null
}

fun typeLabel(record: NdefRecordModel): String = kindOf(record)?.title ?: when (record) {
    is NdefRecordModel.External -> "External"
    NdefRecordModel.Empty -> "Empty"
    is NdefRecordModel.Unknown -> "Unknown"
    else -> "Record"
}

fun summary(record: NdefRecordModel): String = when (record) {
    is NdefRecordModel.Uri -> record.uri
    is NdefRecordModel.Text -> "“${record.text}” · ${record.languageCode}"
    is NdefRecordModel.Wifi -> "${record.ssid} · ${record.authType}"
    is NdefRecordModel.Phone -> record.number
    is NdefRecordModel.Email -> record.address
    is NdefRecordModel.Sms -> "to ${record.number}"
    is NdefRecordModel.Geo -> "${record.latitude}, ${record.longitude}"
    is NdefRecordModel.Application -> record.packageName
    is NdefRecordModel.MimeData -> record.mimeType
    is NdefRecordModel.Contact -> record.vcard.lineSequence().firstOrNull { it.startsWith("FN:") }?.removePrefix("FN:") ?: "vCard"
    is NdefRecordModel.Bluetooth -> record.name ?: record.mac
    else -> record.toString()
}

/** Bottom sheet listing the record types to add. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTypeChooserSheet(onPick: (RecordKind) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Add a record",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            RecordKind.entries.forEach { kind ->
                ListItem(
                    headlineContent = { Text(kind.title) },
                    leadingContent = {
                        Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onPick(kind) },
                )
            }
        }
    }
}

/**
 * Field form for one record [kind]. Prefilled from [initial] when editing.
 * Emits the built record via [onSave]; validates inputs lightly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormSheet(
    kind: RecordKind,
    initial: NdefRecordModel?,
    onSave: (NdefRecordModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(kind.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (initial == null) "New ${kind.title}" else "Edit ${kind.title}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            val built = RecordFields(kind, initial)

            Button(
                onClick = { built?.let(onSave) },
                enabled = built != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initial == null) "Add record" else "Save changes")
            }
        }
    }
}

/** Renders the per-kind inputs and returns the built record, or null when invalid. */
@Composable
private fun RecordFields(kind: RecordKind, initial: NdefRecordModel?): NdefRecordModel? = when (kind) {
    RecordKind.Uri -> {
        var uri by rememberField((initial as? NdefRecordModel.Uri)?.uri ?: "https://")
        Field("URL", uri, { uri = it }, KeyboardType.Uri)
        uri.trim().takeIf { it.length > 3 }?.let { NdefRecordModel.Uri(it) }
    }

    RecordKind.Text -> {
        val t = initial as? NdefRecordModel.Text
        var text by rememberField(t?.text ?: "")
        var lang by rememberField(t?.languageCode ?: "en")
        Field("Text", text, { text = it }, singleLine = false)
        Field("Language code", lang, { lang = it })
        if (text.isNotBlank() && lang.isNotBlank()) {
            NdefRecordModel.Text(text, lang.trim())
        } else null
    }

    RecordKind.Wifi -> {
        val w = initial as? NdefRecordModel.Wifi
        var ssid by rememberField(w?.ssid ?: "")
        var pass by rememberField(w?.password ?: "")
        var auth by rememberField(w?.authType ?: "WPA2PSK")
        var enc by rememberField(w?.encryption ?: "AES")
        var hidden by remember { mutableStateOf(w?.hidden ?: false) }
        Field("Network name (SSID)", ssid, { ssid = it })
        Field("Password", pass, { pass = it })
        Field("Authentication (e.g. WPA2PSK, NONE)", auth, { auth = it })
        Field("Encryption (e.g. AES, TKIP, NONE)", enc, { enc = it })
        SwitchRow("Hidden network", hidden) { hidden = it }
        if (ssid.isNotBlank() && auth.isNotBlank() && enc.isNotBlank()) {
            NdefRecordModel.Wifi(ssid, pass, auth.trim(), enc.trim(), hidden)
        } else null
    }

    RecordKind.Phone -> {
        var number by rememberField((initial as? NdefRecordModel.Phone)?.number ?: "")
        Field("Phone number", number, { number = it }, KeyboardType.Phone)
        number.trim().takeIf { it.isNotBlank() }?.let { NdefRecordModel.Phone(it) }
    }

    RecordKind.Email -> {
        val e = initial as? NdefRecordModel.Email
        var address by rememberField(e?.address ?: "")
        var subject by rememberField(e?.subject ?: "")
        var body by rememberField(e?.body ?: "")
        Field("Address", address, { address = it }, KeyboardType.Email)
        Field("Subject (optional)", subject, { subject = it })
        Field("Body (optional)", body, { body = it }, singleLine = false)
        address.trim().takeIf { it.contains("@") && it.length > 2 }
            ?.let { NdefRecordModel.Email(it, subject, body) }
    }

    RecordKind.Sms -> {
        val s = initial as? NdefRecordModel.Sms
        var number by rememberField(s?.number ?: "")
        var body by rememberField(s?.body ?: "")
        Field("Phone number", number, { number = it }, KeyboardType.Phone)
        Field("Message", body, { body = it }, singleLine = false)
        number.trim().takeIf { it.isNotBlank() }?.let { NdefRecordModel.Sms(it, body) }
    }

    RecordKind.Geo -> {
        val g = initial as? NdefRecordModel.Geo
        var lat by rememberField(g?.latitude?.toString() ?: "")
        var lng by rememberField(g?.longitude?.toString() ?: "")
        Field("Latitude", lat, { lat = it }, KeyboardType.Number)
        Field("Longitude", lng, { lng = it }, KeyboardType.Number)
        val latV = lat.trim().toDoubleOrNull()
        val lngV = lng.trim().toDoubleOrNull()
        if (latV != null && lngV != null && latV in -90.0..90.0 && lngV in -180.0..180.0) {
            NdefRecordModel.Geo(latV, lngV)
        } else null
    }

    RecordKind.Application -> {
        var pkg by rememberField((initial as? NdefRecordModel.Application)?.packageName ?: "")
        Field("Package name (e.g. com.example.app)", pkg, { pkg = it })
        pkg.trim().takeIf { it.contains(".") }?.let { NdefRecordModel.Application(it) }
    }

    RecordKind.Mime -> {
        val m = initial as? NdefRecordModel.MimeData
        var mimeType by rememberField(m?.mimeType ?: "text/plain")
        var payload by rememberField(m?.payload?.let { String(it, Charsets.UTF_8) } ?: "")
        Field("MIME type", mimeType, { mimeType = it })
        Field("Content", payload, { payload = it }, singleLine = false)
        if (mimeType.contains("/") && payload.isNotEmpty()) {
            NdefRecordModel.MimeData(mimeType.trim(), payload.toByteArray(Charsets.UTF_8))
        } else null
    }

    RecordKind.Contact -> {
        var vcard by rememberField(
            (initial as? NdefRecordModel.Contact)?.vcard
                ?: "BEGIN:VCARD\nVERSION:3.0\nFN:\nTEL:\nEMAIL:\nEND:VCARD",
        )
        Field("vCard text", vcard, { vcard = it }, singleLine = false)
        vcard.trim().takeIf { it.contains("BEGIN:VCARD") }?.let { NdefRecordModel.Contact(it) }
    }

    RecordKind.Bluetooth -> {
        val b = initial as? NdefRecordModel.Bluetooth
        var mac by rememberField(b?.mac ?: "")
        var name by rememberField(b?.name ?: "")
        Field("MAC address (00:11:22:33:44:55)", mac, { mac = it })
        Field("Device name (optional)", name, { name = it })
        mac.trim().takeIf { it.count { c -> c == ':' } == 5 }
            ?.let { NdefRecordModel.Bluetooth(it, name.trim().ifBlank { null }) }
    }
}

@Composable
private fun rememberField(initial: String) = remember { mutableStateOf(initial) }

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
