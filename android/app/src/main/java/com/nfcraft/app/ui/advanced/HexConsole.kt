package com.nfcraft.app.ui.advanced

/** A named raw command the user can tap to fill the console input. */
data class ApduPreset(val label: String, val hex: String)

/** One send/receive exchange shown in the console history. */
data class ApduExchange(
    val sentHex: String,
    val success: Boolean,
    /** Received bytes rendered as hex (success), or the failure message. */
    val detail: String,
    /** Printable-ASCII rendering of the received bytes, or null on failure. */
    val ascii: String?,
)

/** Common ISO 14443-A / NFC Forum Type 2 commands, sent verbatim. */
val apduPresets: List<ApduPreset> = listOf(
    ApduPreset("GET_VERSION", "60"),
    ApduPreset("READ page0", "30 00"),
    ApduPreset("READ_SIG", "3C 00"),
    ApduPreset("READ_CNT", "39 02"),
    ApduPreset("HALT", "50 00"),
)

/** Strips all whitespace and returns the normalized hex digits, or null if invalid. */
fun normalizeHex(input: String): String? {
    val stripped = input.filterNot { it.isWhitespace() }
    if (stripped.isEmpty() || stripped.length % 2 != 0) return null
    if (!stripped.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    return stripped
}

/** Parses hex text (spaces/newlines allowed) into bytes, or null if not valid even-length hex. */
fun parseHex(input: String): ByteArray? {
    val hex = normalizeHex(input) ?: return null
    return ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/** Upper-case, space-separated hex, e.g. "30 00". Empty input yields "". */
fun formatHex(bytes: ByteArray): String =
    bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

/** Printable ASCII (0x20..0x7E) kept as-is, anything else shown as '.'. */
fun renderAscii(bytes: ByteArray): String =
    bytes.map { b ->
        val v = b.toInt() and 0xFF
        if (v in 0x20..0x7E) v.toChar() else '.'
    }.joinToString("")
