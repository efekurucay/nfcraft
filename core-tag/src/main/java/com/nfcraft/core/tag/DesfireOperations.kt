package com.nfcraft.core.tag

import android.nfc.tech.IsoDep
import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import com.nfcraft.core.model.TagDump

/**
 * MIFARE DESFire / NTAG4xx DNA plain walk.
 *
 * Native DESFire commands are carried inside the ISO7816 wrapper
 * `90 <cmd> 00 00 <Lc> <data> 00`; the response ends in the status word
 * `91 <sw>`, where `0x00` is success and `0xAF` requests the next frame. The
 * walk enumerates applications, their files, and reads every standard file with
 * free (plain) read access. Files that need authentication answer with an error
 * status and are skipped, so no key handling is attempted.
 */
internal object DesfireOperations {

    private val CLA: Byte = 0x90.toByte()
    private const val GET_APPLICATION_IDS: Byte = 0x6A
    private const val SELECT_APPLICATION: Byte = 0x5A
    private const val GET_FILE_IDS: Byte = 0x6F
    private val READ_DATA: Byte = 0xBD.toByte()
    private val ADDITIONAL_FRAME: Byte = 0xAF.toByte()

    private const val STATUS_OK: Int = 0x00
    private const val STATUS_ADDITIONAL: Int = 0xAF

    private const val BLOCK_SIZE = 16

    fun dump(io: NfcIo, chip: ChipType): TagDump {
        if (!io.useTechnology(IsoDep::class.java.name) && !io.isConnected) {
            throw NfcException(NfcException.IO_ERROR, "DESFire requires IsoDep")
        }
        val collected = ArrayList<Byte>()

        selectApplication(io, byteArrayOf(0x00, 0x00, 0x00))
        val aids = runCatching { command(io, GET_APPLICATION_IDS) }.getOrDefault(ByteArray(0))

        for (aid in aids.toList().chunked(3)) {
            if (aid.size < 3) continue
            val selected = runCatching {
                selectApplication(io, byteArrayOf(aid[0], aid[1], aid[2]))
                true
            }.getOrDefault(false)
            if (!selected) continue

            val fileIds = runCatching { command(io, GET_FILE_IDS) }.getOrDefault(ByteArray(0))
            for (fileNo in fileIds) {
                runCatching { readStandardFile(io, fileNo) }
                    .getOrNull()
                    ?.let { collected.addAll(it.toList()) }
            }
        }

        val blocks = collected.toByteArray().toList().chunked(BLOCK_SIZE).map { it.toByteArray() }
        return TagDump(chip = chip, blockSize = BLOCK_SIZE, blocks = blocks)
    }

    private fun selectApplication(io: NfcIo, aid: ByteArray) {
        command(io, SELECT_APPLICATION, aid)
    }

    /** ReadData with offset 0 and length 0, which returns the whole file. */
    private fun readStandardFile(io: NfcIo, fileNo: Byte): ByteArray {
        val params = byteArrayOf(fileNo, 0, 0, 0, 0, 0, 0)
        return command(io, READ_DATA, params)
    }

    /**
     * Send one native command and collect its payload, following `0xAF`
     * continuation frames. Throws [NfcException.IO_ERROR] on any non-success,
     * non-continuation status so the caller can skip the item.
     */
    private fun command(io: NfcIo, cmd: Byte, data: ByteArray = ByteArray(0)): ByteArray {
        val payload = ArrayList<Byte>()
        var response = io.transceive(wrap(cmd, data))
        while (true) {
            if (response.size < 2) {
                throw NfcException(NfcException.IO_ERROR, "Short DESFire response")
            }
            val status = response[response.size - 1].toInt() and 0xFF
            for (i in 0 until response.size - 2) payload.add(response[i])
            when (status) {
                STATUS_OK -> return payload.toByteArray()
                STATUS_ADDITIONAL -> response = io.transceive(wrap(ADDITIONAL_FRAME))
                else -> throw NfcException(NfcException.IO_ERROR, "DESFire status 0x%02X".format(status))
            }
        }
    }

    private fun wrap(cmd: Byte, data: ByteArray = ByteArray(0)): ByteArray {
        val out = ArrayList<Byte>(6 + data.size)
        out.add(CLA)
        out.add(cmd)
        out.add(0x00)
        out.add(0x00)
        if (data.isNotEmpty()) {
            out.add(data.size.toByte())
            for (b in data) out.add(b)
        }
        out.add(0x00)
        return out.toByteArray()
    }
}
