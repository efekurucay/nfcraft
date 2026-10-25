package com.nfcraft.feature.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.nfcraft.core.ndef.DefaultNdefCodec

/**
 * NFC Forum Type 4 Tag emulation.
 *
 * Drives the minimal T4T state machine an external reader walks to pull an NDEF
 * message off a tag:
 *
 *  1. SELECT by AID `D2760000850101` (NDEF Tag Application) selects the app.
 *  2. SELECT by file id `E103` (Capability Container) or `E104` (NDEF file)
 *     picks the elementary file subsequent READ BINARY commands target.
 *  3. READ BINARY (`0xB0`) with an offset/length returns slices of the selected
 *     file: the 15-byte Capability Container, or the NDEF file (2-byte NLEN
 *     length prefix followed by the encoded NDEF message).
 *
 * The served message comes from [EmulatedTagStore], encoded with
 * [DefaultNdefCodec]. It is snapshotted when the application is selected and
 * cached for the session, so a read sequence sees a stable file even if the app
 * layer swaps the payload mid-tap.
 */
class NdefHostApduService : HostApduService() {

    private val codec = DefaultNdefCodec()

    /** Which elementary file the next READ BINARY targets. */
    private var selectedFile = SelectedFile.NONE

    /** Session snapshot of the NDEF file (NLEN prefix + message); lazily built. */
    private var ndefFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_WRONG_LENGTH
        if (apdu.size < HEADER_LEN) return SW_WRONG_LENGTH

        return when (apdu[OFFSET_INS].toInt() and 0xFF) {
            INS_SELECT -> handleSelect(apdu)
            INS_READ_BINARY -> handleReadBinary(apdu)
            else -> SW_INS_NOT_SUPPORTED
        }
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = SelectedFile.NONE
        ndefFile = null
    }

    /** SELECT by AID (application) or by file id (Capability Container / NDEF file). */
    private fun handleSelect(apdu: ByteArray): ByteArray {
        if (apdu.size < HEADER_LEN + 1) return SW_WRONG_LENGTH
        val p1 = apdu[OFFSET_P1].toInt() and 0xFF
        val p2 = apdu[OFFSET_P2].toInt() and 0xFF
        val lc = apdu[OFFSET_LC].toInt() and 0xFF
        val dataStart = OFFSET_LC + 1
        if (lc == 0 || apdu.size < dataStart + lc) return SW_WRONG_LENGTH
        val data = apdu.copyOfRange(dataStart, dataStart + lc)

        return when (p1) {
            P1_SELECT_BY_NAME -> {
                if (data.contentEquals(NDEF_TAG_AID)) {
                    selectedFile = SelectedFile.APPLICATION
                    ndefFile = null
                    SW_OK
                } else {
                    SW_FILE_NOT_FOUND
                }
            }
            P1_SELECT_BY_FILE_ID -> when {
                p2 != P2_SELECT_FIRST_ONLY -> SW_WRONG_P1P2
                data.contentEquals(FILE_ID_CC) -> {
                    selectedFile = SelectedFile.CAPABILITY_CONTAINER
                    SW_OK
                }
                data.contentEquals(FILE_ID_NDEF) -> {
                    selectedFile = SelectedFile.NDEF
                    SW_OK
                }
                else -> SW_FILE_NOT_FOUND
            }
            else -> SW_WRONG_P1P2
        }
    }

    /** READ BINARY against the selected file, honouring the P1P2 offset and Le. */
    private fun handleReadBinary(apdu: ByteArray): ByteArray {
        if (apdu.size < HEADER_LEN + 1) return SW_WRONG_LENGTH

        val file = when (selectedFile) {
            SelectedFile.CAPABILITY_CONTAINER -> CAPABILITY_CONTAINER
            SelectedFile.NDEF -> ndefFileContent()
            else -> return SW_FILE_NOT_FOUND
        }

        val offset = ((apdu[OFFSET_P1].toInt() and 0xFF) shl 8) or (apdu[OFFSET_P2].toInt() and 0xFF)
        if (offset > file.size) return SW_WRONG_P1P2

        // Le == 0 requests the maximum (256 bytes); otherwise the exact count.
        val le = apdu[OFFSET_LE].toInt() and 0xFF
        val requested = if (le == 0) MAX_LE else le
        val length = minOf(requested, file.size - offset)

        val response = ByteArray(length + SW_LEN)
        file.copyInto(response, destinationOffset = 0, startIndex = offset, endIndex = offset + length)
        SW_OK.copyInto(response, destinationOffset = length)
        return response
    }

    /**
     * NDEF file for the current session: a big-endian NLEN length prefix followed
     * by the encoded message from [EmulatedTagStore]. Built once per session and
     * reused across the read sequence.
     */
    private fun ndefFileContent(): ByteArray {
        ndefFile?.let { return it }
        val message = codec.encode(EmulatedTagStore.records).toByteArray()
        val nlen = message.size
        val file = ByteArray(NLEN_LEN + nlen)
        file[0] = (nlen ushr 8).toByte()
        file[1] = nlen.toByte()
        message.copyInto(file, destinationOffset = NLEN_LEN)
        ndefFile = file
        return file
    }

    private enum class SelectedFile { NONE, APPLICATION, CAPABILITY_CONTAINER, NDEF }

    private companion object {
        // C-APDU layout: CLA INS P1 P2 [Lc data] [Le].
        const val HEADER_LEN = 4
        const val OFFSET_INS = 1
        const val OFFSET_P1 = 2
        const val OFFSET_P2 = 3
        const val OFFSET_LC = 4
        const val OFFSET_LE = 4

        const val INS_SELECT = 0xA4
        const val INS_READ_BINARY = 0xB0

        const val P1_SELECT_BY_NAME = 0x04
        const val P1_SELECT_BY_FILE_ID = 0x00
        const val P2_SELECT_FIRST_ONLY = 0x0C

        /** Response data cap for a single READ BINARY with Le == 0. */
        const val MAX_LE = 256

        const val NLEN_LEN = 2
        const val SW_LEN = 2

        /** NFC Forum NDEF Tag Application identifier. */
        val NDEF_TAG_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
        )
        val FILE_ID_CC = byteArrayOf(0xE1.toByte(), 0x03)
        val FILE_ID_NDEF = byteArrayOf(0xE1.toByte(), 0x04)

        /**
         * Capability Container (15 bytes): CCLEN, mapping version 2.0, MLe, MLc,
         * then the NDEF File Control TLV (tag 0x04, len 0x06) pointing at file
         * E104 with its max size, read access granted (0x00) and write access
         * disabled (0xFF) since this service only serves reads.
         */
        val CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0x0F,             // CCLEN = 15
            0x20,                   // mapping version 2.0
            0x00, 0x3B,             // MLe = 59 (max response data bytes)
            0x00, 0x34,             // MLc = 52 (max command data bytes)
            0x04, 0x06,             // NDEF File Control TLV: T, L
            0xE1.toByte(), 0x04,    // NDEF file id
            0x7F, 0xFF.toByte(),    // max NDEF file size = 32767
            0x00,                   // read access granted
            0xFF.toByte(),          // write access disabled
        )

        val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val SW_WRONG_P1P2 = byteArrayOf(0x6A, 0x86.toByte())
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
    }
}
