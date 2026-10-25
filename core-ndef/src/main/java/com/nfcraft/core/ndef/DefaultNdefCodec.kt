package com.nfcraft.core.ndef

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.nfcraft.core.model.NdefCodec
import com.nfcraft.core.model.NdefRecordModel
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * NFC Forum NDEF codec. Covers every [NdefRecordModel] variant in both directions:
 *
 * - Well-known URI (with the NFC Forum identifier-code abbreviation table) and Text.
 * - Scheme-specialized URIs: `tel:` -> Phone, `mailto:` -> Email, `sms:` -> Sms, `geo:` -> Geo.
 * - MIME payloads: WiFi Simple Config (`application/vnd.wfa.wsc`), Bluetooth OOB
 *   (`application/vnd.bluetooth.ep.oob`), and vCard contacts (`text/vcard`).
 * - Android Application Record, raw MIME, external types, empty and unknown records.
 */
class DefaultNdefCodec : NdefCodec {

    override fun decode(message: NdefMessage): List<NdefRecordModel> =
        message.records.map { decodeRecord(it) }

    override fun encode(records: List<NdefRecordModel>): NdefMessage {
        // NdefMessage requires at least one record; an empty set encodes to a
        // single empty record so callers never hit an exception.
        val encoded = if (records.isEmpty()) {
            arrayOf(encodeRecord(NdefRecordModel.Empty))
        } else {
            records.map { encodeRecord(it) }.toTypedArray()
        }
        return NdefMessage(encoded)
    }

    override fun encodedSize(records: List<NdefRecordModel>): Int =
        if (records.isEmpty()) 0 else encode(records).toByteArray().size

    // --- decode -----------------------------------------------------------

    private fun decodeRecord(record: NdefRecord): NdefRecordModel = when (record.tnf) {
        NdefRecord.TNF_WELL_KNOWN -> when {
            record.type.contentEquals(NdefRecord.RTD_URI) -> classifyUri(decodeUri(record.payload))
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> decodeText(record.payload)
            else -> NdefRecordModel.Unknown(record.tnf, record.type, record.payload)
        }
        NdefRecord.TNF_MIME_MEDIA -> decodeMime(String(record.type, Charsets.US_ASCII), record.payload)
        NdefRecord.TNF_EXTERNAL_TYPE -> {
            val type = String(record.type, Charsets.US_ASCII)
            if (type.equals(AAR_TYPE, ignoreCase = true)) {
                NdefRecordModel.Application(String(record.payload, Charsets.UTF_8))
            } else {
                NdefRecordModel.External(type, record.payload)
            }
        }
        NdefRecord.TNF_EMPTY -> NdefRecordModel.Empty
        else -> NdefRecordModel.Unknown(record.tnf, record.type, record.payload)
    }

    private fun decodeUri(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefix = URI_PREFIXES.getOrElse(payload[0].toInt() and 0xFF) { "" }
        return prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
    }

    /** Maps a decoded URI onto the most specific model its scheme allows. */
    private fun classifyUri(uri: String): NdefRecordModel = when {
        uri.startsWith("tel:") -> NdefRecordModel.Phone(uri.removePrefix("tel:"))
        uri.startsWith("mailto:") -> decodeMailto(uri)
        uri.startsWith("sms:") -> decodeSms(uri)
        uri.startsWith("geo:") -> decodeGeo(uri) ?: NdefRecordModel.Uri(uri)
        else -> NdefRecordModel.Uri(uri)
    }

    private fun decodeText(payload: ByteArray): NdefRecordModel.Text {
        if (payload.isEmpty()) return NdefRecordModel.Text("", "en")
        val status = payload[0].toInt()
        val langLen = status and 0x3F
        val lang = String(payload, 1, langLen, Charsets.US_ASCII)
        val text = String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
        return NdefRecordModel.Text(text, lang)
    }

    private fun decodeMime(mime: String, payload: ByteArray): NdefRecordModel = when (mime.lowercase()) {
        MIME_WIFI -> decodeWifi(payload)
        MIME_BLUETOOTH -> decodeBluetooth(payload)
        MIME_VCARD, MIME_VCARD_ALT -> NdefRecordModel.Contact(String(payload, Charsets.UTF_8))
        else -> NdefRecordModel.MimeData(mime, payload)
    }

    private fun decodeMailto(uri: String): NdefRecordModel.Email {
        val rest = uri.removePrefix("mailto:")
        val q = rest.indexOf('?')
        val address = if (q >= 0) rest.substring(0, q) else rest
        var subject = ""
        var body = ""
        if (q >= 0) {
            forEachQueryParam(rest.substring(q + 1)) { key, value ->
                when (key.lowercase()) {
                    "subject" -> subject = value
                    "body" -> body = value
                }
            }
        }
        return NdefRecordModel.Email(address, subject, body)
    }

    private fun decodeSms(uri: String): NdefRecordModel.Sms {
        val rest = uri.removePrefix("sms:")
        val q = rest.indexOf('?')
        val number = if (q >= 0) rest.substring(0, q) else rest
        var body = ""
        if (q >= 0) {
            forEachQueryParam(rest.substring(q + 1)) { key, value ->
                if (key.equals("body", ignoreCase = true)) body = value
            }
        }
        return NdefRecordModel.Sms(number, body)
    }

    private fun decodeGeo(uri: String): NdefRecordModel.Geo? {
        val coords = uri.removePrefix("geo:").substringBefore(';').substringBefore('?')
        val parts = coords.split(',')
        if (parts.size < 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lon = parts[1].trim().toDoubleOrNull() ?: return null
        return NdefRecordModel.Geo(lat, lon)
    }

    /**
     * Parses a WiFi Simple Config payload. SSID/key/auth/encryption live inside the
     * Credential TLV (0x100E); the walk recurses into it so top-level or nested
     * layouts both decode.
     */
    private fun decodeWifi(payload: ByteArray): NdefRecordModel.Wifi {
        var ssid = ""
        var key = ""
        var auth = 0
        var encryption = 0
        fun walk(data: ByteArray) {
            var i = 0
            while (i + 4 <= data.size) {
                val type = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
                val len = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
                val start = i + 4
                if (start + len > data.size) break
                val value = data.copyOfRange(start, start + len)
                when (type) {
                    WSC_CREDENTIAL -> walk(value)
                    WSC_SSID -> ssid = String(value, Charsets.UTF_8)
                    WSC_NETWORK_KEY -> key = String(value, Charsets.UTF_8)
                    WSC_AUTH_TYPE -> if (value.size >= 2) auth = readUShort(value, 0)
                    WSC_ENCRYPTION -> if (value.size >= 2) encryption = readUShort(value, 0)
                }
                i = start + len
            }
        }
        walk(payload)
        return NdefRecordModel.Wifi(ssid, key, authTypeName(auth), encryptionName(encryption))
    }

    /**
     * Parses a Bluetooth OOB payload: 2-byte little-endian total length, 6-byte MAC in
     * little-endian order, then optional EIR fields (local name is type 0x08/0x09).
     */
    private fun decodeBluetooth(payload: ByteArray): NdefRecordModel.Bluetooth {
        if (payload.size < 8) return NdefRecordModel.Bluetooth("00:00:00:00:00:00", null)
        val total = (payload[0].toInt() and 0xFF) or ((payload[1].toInt() and 0xFF) shl 8)
        val mac = macToString(payload.copyOfRange(2, 8).reversedArray())
        var name: String? = null
        var i = 8
        val end = minOf(total, payload.size)
        while (i < end) {
            val len = payload[i].toInt() and 0xFF
            if (len == 0 || i + 1 + len > payload.size) break
            val type = payload[i + 1].toInt() and 0xFF
            if (type == EIR_NAME_COMPLETE || type == EIR_NAME_SHORT) {
                name = String(payload, i + 2, len - 1, Charsets.UTF_8)
            }
            i += 1 + len
        }
        return NdefRecordModel.Bluetooth(mac, name)
    }

    // --- encode -----------------------------------------------------------

    private fun encodeRecord(model: NdefRecordModel): NdefRecord = when (model) {
        is NdefRecordModel.Uri -> encodeUri(model.uri)
        is NdefRecordModel.Text -> encodeText(model)
        is NdefRecordModel.Wifi -> NdefRecord.createMime(MIME_WIFI, encodeWifi(model))
        is NdefRecordModel.Bluetooth -> NdefRecord.createMime(MIME_BLUETOOTH, encodeBluetooth(model))
        is NdefRecordModel.Contact -> NdefRecord.createMime(MIME_VCARD, model.vcard.toByteArray(Charsets.UTF_8))
        is NdefRecordModel.Email -> encodeUri(buildMailto(model))
        is NdefRecordModel.Phone -> encodeUri("tel:" + model.number)
        is NdefRecordModel.Sms -> encodeUri(buildSms(model))
        is NdefRecordModel.Geo -> encodeUri("geo:${model.latitude},${model.longitude}")
        is NdefRecordModel.Application -> NdefRecord.createApplicationRecord(model.packageName)
        is NdefRecordModel.MimeData -> NdefRecord.createMime(model.mimeType, model.payload)
        is NdefRecordModel.External -> {
            val parts = model.domainType.split(":", limit = 2)
            NdefRecord.createExternal(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, model.payload)
        }
        is NdefRecordModel.Unknown -> NdefRecord(model.tnf, model.type, ByteArray(0), model.payload)
        NdefRecordModel.Empty -> NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
    }

    /** Builds a well-known URI record, abbreviating with the longest matching prefix code. */
    private fun encodeUri(uri: String): NdefRecord {
        var code = 0
        var matched = 0
        for (i in 1 until URI_PREFIXES.size) {
            val prefix = URI_PREFIXES[i]
            if (prefix.isNotEmpty() && uri.startsWith(prefix) && prefix.length > matched) {
                matched = prefix.length
                code = i
            }
        }
        val rest = uri.substring(matched).toByteArray(Charsets.UTF_8)
        val payload = ByteArray(rest.size + 1)
        payload[0] = code.toByte()
        System.arraycopy(rest, 0, payload, 1, rest.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), payload)
    }

    private fun encodeText(model: NdefRecordModel.Text): NdefRecord =
        NdefRecord.createTextRecord(model.languageCode, model.text)

    private fun encodeWifi(model: NdefRecordModel.Wifi): ByteArray {
        val credential = ByteArrayOutputStream().apply {
            writeTlv(WSC_NETWORK_INDEX, byteArrayOf(0x01))
            writeTlv(WSC_SSID, model.ssid.toByteArray(Charsets.UTF_8))
            writeTlv(WSC_AUTH_TYPE, uShortBytes(authTypeCode(model.authType)))
            writeTlv(WSC_ENCRYPTION, uShortBytes(encryptionCode(model.encryption)))
            writeTlv(WSC_NETWORK_KEY, model.password.toByteArray(Charsets.UTF_8))
            writeTlv(WSC_MAC_ADDRESS, ByteArray(6))
        }
        return ByteArrayOutputStream().apply {
            writeTlv(WSC_CREDENTIAL, credential.toByteArray())
        }.toByteArray()
    }

    private fun encodeBluetooth(model: NdefRecordModel.Bluetooth): ByteArray {
        val macLittleEndian = parseMac(model.mac).reversedArray()
        val nameEir = model.name?.takeIf { it.isNotEmpty() }?.let { name ->
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            ByteArray(nameBytes.size + 2).also {
                it[0] = (nameBytes.size + 1).toByte()
                it[1] = EIR_NAME_COMPLETE.toByte()
                System.arraycopy(nameBytes, 0, it, 2, nameBytes.size)
            }
        } ?: ByteArray(0)
        val total = 2 + macLittleEndian.size + nameEir.size
        return ByteArrayOutputStream().apply {
            write(total and 0xFF)
            write((total shr 8) and 0xFF)
            write(macLittleEndian)
            write(nameEir)
        }.toByteArray()
    }

    private fun buildMailto(model: NdefRecordModel.Email): String {
        val params = buildList {
            if (model.subject.isNotEmpty()) add("subject=" + encodeQueryValue(model.subject))
            if (model.body.isNotEmpty()) add("body=" + encodeQueryValue(model.body))
        }
        return buildString {
            append("mailto:").append(model.address)
            if (params.isNotEmpty()) append('?').append(params.joinToString("&"))
        }
    }

    private fun buildSms(model: NdefRecordModel.Sms): String = buildString {
        append("sms:").append(model.number)
        if (model.body.isNotEmpty()) append("?body=").append(encodeQueryValue(model.body))
    }

    // --- shared helpers ---------------------------------------------------

    private fun ByteArrayOutputStream.writeTlv(type: Int, value: ByteArray) {
        write((type shr 8) and 0xFF)
        write(type and 0xFF)
        write((value.size shr 8) and 0xFF)
        write(value.size and 0xFF)
        write(value)
    }

    private fun readUShort(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun uShortBytes(value: Int): ByteArray =
        byteArrayOf(((value shr 8) and 0xFF).toByte(), (value and 0xFF).toByte())

    private fun parseMac(mac: String): ByteArray {
        val tokens = mac.split(':', '-').filter { it.isNotEmpty() }
        return ByteArray(6) { i -> tokens.getOrNull(i)?.toIntOrNull(16)?.toByte() ?: 0 }
    }

    private fun macToString(bytes: ByteArray): String =
        bytes.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }

    private inline fun forEachQueryParam(query: String, action: (String, String) -> Unit) {
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq >= 0) action(part.substring(0, eq), decodeQueryValue(part.substring(eq + 1)))
        }
    }

    private fun encodeQueryValue(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decodeQueryValue(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun authTypeCode(name: String): Int = when (normalizeKey(name)) {
        "OPEN", "NONE" -> 0x0001
        "WPAPSK", "WPA", "WPAPERSONAL" -> 0x0002
        "SHARED" -> 0x0004
        "WPAEAP", "WPAENTERPRISE" -> 0x0008
        "WPA2EAP", "WPA2ENTERPRISE" -> 0x0010
        "WPA2PSK", "WPA2", "WPA2PERSONAL" -> 0x0020
        "WPAWPA2PSK", "WPAWPA2" -> 0x0022
        else -> 0x0020
    }

    private fun authTypeName(code: Int): String = when (code) {
        0x0001 -> "OPEN"
        0x0002 -> "WPAPSK"
        0x0004 -> "SHARED"
        0x0008 -> "WPA_EAP"
        0x0010 -> "WPA2_EAP"
        0x0022 -> "WPA/WPA2PSK"
        else -> "WPA2PSK"
    }

    private fun encryptionCode(name: String): Int = when (normalizeKey(name)) {
        "NONE" -> 0x0001
        "WEP" -> 0x0002
        "TKIP" -> 0x0004
        "AES", "CCMP" -> 0x0008
        "AESTKIP", "TKIPAES" -> 0x000C
        else -> 0x0008
    }

    private fun encryptionName(code: Int): String = when (code) {
        0x0001 -> "NONE"
        0x0002 -> "WEP"
        0x0004 -> "TKIP"
        0x000C -> "AES/TKIP"
        else -> "AES"
    }

    private fun normalizeKey(value: String): String =
        value.uppercase().replace("-", "").replace("_", "").replace("/", "").replace(" ", "")

    private companion object {
        const val AAR_TYPE = "android.com:pkg"

        const val MIME_WIFI = "application/vnd.wfa.wsc"
        const val MIME_BLUETOOTH = "application/vnd.bluetooth.ep.oob"
        const val MIME_VCARD = "text/vcard"
        const val MIME_VCARD_ALT = "text/x-vcard"

        // WiFi Simple Config TLV attribute types.
        const val WSC_CREDENTIAL = 0x100E
        const val WSC_SSID = 0x1045
        const val WSC_NETWORK_KEY = 0x1027
        const val WSC_AUTH_TYPE = 0x1003
        const val WSC_ENCRYPTION = 0x100F
        const val WSC_NETWORK_INDEX = 0x1026
        const val WSC_MAC_ADDRESS = 0x1020

        // Bluetooth OOB EIR local-name field types.
        const val EIR_NAME_SHORT = 0x08
        const val EIR_NAME_COMPLETE = 0x09

        /** NFC Forum URI identifier-code abbreviation table. */
        val URI_PREFIXES = listOf(
            "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
            "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
            "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://",
            "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
            "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
            "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
        )
    }
}
