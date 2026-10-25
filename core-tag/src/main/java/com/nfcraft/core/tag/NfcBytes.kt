package com.nfcraft.core.tag

import com.nfcraft.core.model.NfcException
import java.security.MessageDigest

/**
 * Low-level byte helpers shared by the per-family tag operations.
 *
 * Type 2 command set (NTAG / Ultralight), issued over the connected NfcA /
 * MifareUltralight technology:
 * - [T2_READ] `{0x30, page}` returns 16 bytes (four pages, wrapping at the end).
 * - [T2_WRITE] `{0xA2, page, d0, d1, d2, d3}` writes one 4-byte page.
 * - [T2_PWD_AUTH] `{0x1B, p0, p1, p2, p3}` returns the 2-byte PACK on success.
 * A successful WRITE / PWD_AUTH answers with the 4-bit [T2_ACK].
 */
internal object NfcBytes {
    const val T2_READ: Byte = 0x30
    val T2_WRITE: Byte = 0xA2.toByte()
    const val T2_PWD_AUTH: Byte = 0x1B
    const val T2_ACK: Byte = 0x0A

    const val PAGE_SIZE = 4

    /**
     * Derive the 4-byte tag password. [password] is used verbatim when it is
     * exactly eight hex characters; otherwise it is treated as a passphrase and
     * reduced to the first four bytes of its MD5 digest.
     */
    fun derivePassword(password: String): ByteArray {
        val trimmed = password.trim()
        if (trimmed.length == 8 && trimmed.all { it.isHex() }) {
            return ByteArray(4) { trimmed.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }
        val digest = MessageDigest.getInstance("MD5").digest(trimmed.toByteArray(Charsets.UTF_8))
        return digest.copyOf(4)
    }

    private fun Char.isHex(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    /** Fail with [NfcException.WRITE_FAILED] when the response is an explicit NAK. */
    fun expectAck(response: ByteArray) {
        if (response.isNotEmpty() && response[0] != T2_ACK) {
            throw NfcException(NfcException.WRITE_FAILED, "Tag NAK on write (0x${response[0].hex()})")
        }
    }
}

internal fun Byte.hex(): String = "%02X".format(this)

internal fun ByteArray.hex(): String = joinToString("") { it.hex() }

/** Round up to the next multiple of [NfcBytes.PAGE_SIZE], zero-padding the tail. */
internal fun ByteArray.padToPage(): ByteArray {
    val remainder = size % NfcBytes.PAGE_SIZE
    if (remainder == 0) return this
    return copyOf(size + (NfcBytes.PAGE_SIZE - remainder))
}
