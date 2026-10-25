package com.nfcraft.core.tag

import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import com.nfcraft.core.model.TagDump

/**
 * MIFARE Classic block engine.
 *
 * Classic sectors are gated by Crypto1 authentication, which the NFC controller
 * performs through `MifareClassic.authenticateSectorWithKeyA/B` — not through
 * raw [NfcIo.transceive]. With only the transceive contract available here,
 * block access succeeds solely on sectors the controller already left open, so
 * reads and writes are framed but will fail on any authenticated sector.
 */
internal object ClassicOperations {

    private const val READ: Byte = 0x30
    private val WRITE: Byte = 0xA0.toByte()
    private const val BLOCK_SIZE = 16

    private fun blockCount(chip: ChipType): Int = when (chip) {
        ChipType.CLASSIC_MINI -> 20
        ChipType.CLASSIC_1K -> 64
        ChipType.CLASSIC_4K -> 256
        else -> chip.userMemoryBytes / BLOCK_SIZE
    }

    fun dump(io: NfcIo, chip: ChipType): TagDump {
        val count = blockCount(chip)
        if (count <= 0) {
            throw NfcException(NfcException.NOT_WRITABLE, "${chip.displayName} block count unknown")
        }
        val blocks = ArrayList<ByteArray>(count)
        for (block in 0 until count) {
            val data = try {
                io.transceive(byteArrayOf(READ, block.toByte()))
            } catch (e: NfcException) {
                if (e.code == NfcException.TAG_LOST) throw e
                // Unauthenticated sector: record a zero block and keep walking.
                ByteArray(BLOCK_SIZE)
            }
            blocks.add(data.copyOf(BLOCK_SIZE))
        }
        return TagDump(chip = chip, blockSize = BLOCK_SIZE, blocks = blocks)
    }

    fun writeDump(io: NfcIo, chip: ChipType, dump: TagDump) {
        if (dump.blockSize != BLOCK_SIZE) {
            throw NfcException(NfcException.WRITE_FAILED, "Classic dump must use 16-byte blocks")
        }
        val count = blockCount(chip)
        for (block in 0 until count) {
            val data = dump.blocks.getOrNull(block) ?: break
            // Skip sector trailers (every 4th block in the 1K layout) and the
            // manufacturer block 0, which are not plain user data.
            if (block == 0 || isSectorTrailer(block)) continue
            writeBlock(io, block, data.copyOf(BLOCK_SIZE))
        }
    }

    private fun isSectorTrailer(block: Int): Boolean =
        if (block < 128) (block % 4) == 3 else ((block - 128) % 16) == 15

    private fun writeBlock(io: NfcIo, block: Int, data: ByteArray) {
        io.transceive(byteArrayOf(WRITE, block.toByte()))
        NfcBytes.expectAck(io.transceive(data))
    }
}
