package com.nfcraft.core.chip

/** Parse a hex string (whitespace ignored, case-insensitive) into bytes. */
internal fun hexToBytes(hex: String): ByteArray {
    val clean = hex.filterNot { it.isWhitespace() }
    require(clean.length % 2 == 0) { "Odd hex length: ${clean.length}" }
    val out = ByteArray(clean.length / 2)
    var i = 0
    while (i < clean.length) {
        out[i / 2] = ((hexDigit(clean[i]) shl 4) or hexDigit(clean[i + 1])).toByte()
        i += 2
    }
    return out
}

private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> throw IllegalArgumentException("Not a hex digit: $c")
}

/** Unsigned byte value at [index], or -1 when out of range. */
internal fun ByteArray.u(index: Int): Int = if (index in indices) this[index].toInt() and 0xFF else -1
