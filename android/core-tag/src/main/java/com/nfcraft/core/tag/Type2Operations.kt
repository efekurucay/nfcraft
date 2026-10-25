package com.nfcraft.core.tag

import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import com.nfcraft.core.model.TagDump
import com.nfcraft.core.tag.NfcBytes.PAGE_SIZE
import com.nfcraft.core.tag.NfcBytes.T2_PWD_AUTH
import com.nfcraft.core.tag.NfcBytes.T2_READ
import com.nfcraft.core.tag.NfcBytes.T2_WRITE
import com.nfcraft.core.tag.NfcBytes.expectAck

/**
 * Raw page-level engine for NFC Forum Type 2 tags (MIFARE Ultralight / NTAG).
 *
 * All methods assume a connected [NfcIo] on a transceive-capable NfcA /
 * MifareUltralight technology and throw [NfcException] on failure so the public
 * [com.nfcraft.core.model.TagWriter] can wrap them in a result.
 */
internal object Type2Operations {

    /**
     * Per-chip memory map. [userPages] is the count of 4-byte user data pages
     * starting at [userStartPage]; the password / config region is present only
     * on NTAG213 / 215 / 216 (otherwise [cfg0Page] is null).
     *
     * [cc] is the default capability container (byte 3 = 0x00, read/write).
     */
    private data class Layout(
        val ccPage: Int,
        val cc: ByteArray,
        val userStartPage: Int,
        val userPages: Int,
        val staticLockPage: Int,
        val dynamicLockPage: Int?,
        val cfg0Page: Int?,
        val pwdPage: Int?,
    )

    private fun cc(sizeByte: Int): ByteArray =
        byteArrayOf(0xE1.toByte(), 0x10, sizeByte.toByte(), 0x00)

    private fun layoutOf(chip: ChipType): Layout? = when (chip) {
        ChipType.NTAG213 -> Layout(3, cc(0x12), 4, 36, 2, 0x28, 0x29, 0x2B)
        ChipType.NTAG215 -> Layout(3, cc(0x3E), 4, 126, 2, 0x82, 0x83, 0x85)
        ChipType.NTAG216 -> Layout(3, cc(0x6D), 4, 222, 2, 0xE2, 0xE3, 0xE5)
        ChipType.NTAG210 -> Layout(3, cc(0x06), 4, 13, 2, 0x10, null, null)
        ChipType.NTAG212 -> Layout(3, cc(0x10), 4, 32, 2, 0x24, null, null)
        ChipType.NTAG203 -> Layout(3, cc(0x12), 4, 36, 2, 0x28, null, null)
        ChipType.ULTRALIGHT -> Layout(3, cc(0x06), 4, 12, 2, null, null, null)
        ChipType.ULTRALIGHT_C -> Layout(3, cc(0x12), 4, 36, 2, 0x28, null, null)
        ChipType.ULTRALIGHT_EV1_48 -> Layout(3, cc(0x06), 4, 12, 2, null, null, null)
        ChipType.ULTRALIGHT_EV1_128 -> Layout(3, cc(0x10), 4, 32, 2, null, null, null)
        else -> null
    }

    private fun requireLayout(chip: ChipType): Layout =
        layoutOf(chip) ?: throw NfcException(
            NfcException.NOT_WRITABLE,
            "${chip.displayName} is not a page-addressable Type 2 tag",
        )

    // ---- public operations ------------------------------------------------

    fun write(io: NfcIo, chip: ChipType, message: ByteArray) {
        val layout = requireLayout(chip)
        val tlv = buildNdefTlv(message)
        val capacity = layout.userPages * PAGE_SIZE
        if (tlv.size > capacity) {
            throw NfcException(
                NfcException.MESSAGE_TOO_LARGE,
                "NDEF needs ${tlv.size} bytes; ${chip.displayName} holds $capacity",
            )
        }
        writePages(io, layout.userStartPage, tlv.padToPage())
    }

    /**
     * Write the capability container and an empty NDEF TLV directly through raw
     * WRITE commands. This is the repair path for the Android 7.1 formatting
     * defect, where the platform [android.nfc.tech.NdefFormatable] path leaves a
     * malformed TLV: driving the pages by hand produces a clean empty tag.
     */
    fun format(io: NfcIo, chip: ChipType) {
        val layout = requireLayout(chip)
        writePage(io, layout.ccPage, layout.cc)
        // 0x03 = NDEF TLV, length 0, 0xFE = terminator.
        writePage(io, layout.userStartPage, byteArrayOf(0x03, 0x00, 0xFE.toByte(), 0x00))
    }

    /**
     * Set the static lock bytes, the capability-container read-only bit, and the
     * dynamic lock bytes. Irreversible on hardware: a locked Type 2 tag can
     * never be written again.
     */
    fun makeReadOnly(io: NfcIo, chip: ChipType) {
        val layout = requireLayout(chip)

        // Static lock bytes live in bytes 2..3 of page 2; bytes 0..1 are
        // UID/internal and must be preserved. Lock bits are OR-ed by the chip.
        val lock = readPage(io, layout.staticLockPage)
        lock[2] = 0xFF.toByte()
        lock[3] = 0xFF.toByte()
        writePage(io, layout.staticLockPage, lock)

        // CC byte 3 low nibble = write access; 0x0F forbids further writes.
        val cc = readPage(io, layout.ccPage)
        cc[3] = 0x0F
        writePage(io, layout.ccPage, cc)

        // Dynamic lock bytes (NTAG / UL-C) cover the pages above page 15;
        // byte 3 is RFUI and is preserved.
        layout.dynamicLockPage?.let { page ->
            val dyn = readPage(io, page)
            dyn[0] = 0xFF.toByte()
            dyn[1] = 0xFF.toByte()
            dyn[2] = 0xFF.toByte()
            writePage(io, page, dyn)
        }
    }

    /**
     * Protect writes with [password]. Writes the PWD page then sets CFG0 to
     * `04 00 00 00` (AUTH0 = 0, protection from page 0). PACK is intentionally
     * left at its factory value and the CFG1 PROT bit stays 0, so reads remain
     * open and only writes demand the password.
     */
    fun setPassword(io: NfcIo, chip: ChipType, password: String) {
        val layout = requirePasswordLayout(chip)
        val pwd = NfcBytes.derivePassword(password)
        writePage(io, layout.pwdPage!!, pwd)
        writePage(io, layout.cfg0Page!!, byteArrayOf(0x04, 0x00, 0x00, 0x00))
    }

    /** Authenticate with [password] then disable protection (AUTH0 = 0xFF). */
    fun removePassword(io: NfcIo, chip: ChipType, password: String) {
        val layout = requirePasswordLayout(chip)
        val pwd = NfcBytes.derivePassword(password)
        authenticate(io, pwd)
        writePage(io, layout.cfg0Page!!, byteArrayOf(0x04, 0x00, 0x00, 0xFF.toByte()))
    }

    fun dump(io: NfcIo, chip: ChipType): TagDump {
        val total = if (chip.totalPages > 0) chip.totalPages else layoutTotalGuess(chip)
        val pages = ArrayList<ByteArray>(total)
        var page = 0
        while (page < total) {
            val block = try {
                readPage4(io, page)
            } catch (e: NfcException) {
                if (e.code == NfcException.TAG_LOST) throw e
                break
            }
            val take = minOf(PAGE_SIZE, total - page)
            for (i in 0 until take) {
                val offset = i * PAGE_SIZE
                pages.add(block.copyOfRange(offset, offset + PAGE_SIZE))
            }
            page += take
        }
        return TagDump(chip = chip, blockSize = PAGE_SIZE, blocks = pages)
    }

    /**
     * Restore user data from [dump]. Only the user pages are rewritten; the
     * manufacturer block, capability container, lock bytes, and config pages are
     * skipped so a restore cannot brick the tag.
     */
    fun writeDump(io: NfcIo, chip: ChipType, dump: TagDump) {
        if (dump.blockSize != PAGE_SIZE) {
            throw NfcException(NfcException.WRITE_FAILED, "Type 2 dump must use 4-byte blocks")
        }
        val layout = requireLayout(chip)
        val end = layout.userStartPage + layout.userPages
        for (page in layout.userStartPage until end) {
            val block = dump.blocks.getOrNull(page) ?: break
            writePage(io, page, block.copyOf(PAGE_SIZE))
        }
    }

    // ---- internals --------------------------------------------------------

    private fun requirePasswordLayout(chip: ChipType): Layout {
        val layout = requireLayout(chip)
        if (layout.cfg0Page == null || layout.pwdPage == null) {
            throw NfcException(
                NfcException.NOT_WRITABLE,
                "${chip.displayName} has no password feature",
            )
        }
        return layout
    }

    /** NFC Forum NDEF message TLV: `0x03 <len> <message> 0xFE`. */
    private fun buildNdefTlv(message: ByteArray): ByteArray {
        val out = ArrayList<Byte>(message.size + 4)
        out.add(0x03)
        if (message.size < 0xFF) {
            out.add(message.size.toByte())
        } else {
            out.add(0xFF.toByte())
            out.add((message.size ushr 8).toByte())
            out.add((message.size and 0xFF).toByte())
        }
        for (b in message) out.add(b)
        out.add(0xFE.toByte())
        return out.toByteArray()
    }

    private fun authenticate(io: NfcIo, pwd: ByteArray) {
        val response = io.transceive(byteArrayOf(T2_PWD_AUTH, pwd[0], pwd[1], pwd[2], pwd[3]))
        if (response.size < 2) {
            throw NfcException(NfcException.NOT_WRITABLE, "Password authentication rejected")
        }
    }

    private fun writePages(io: NfcIo, startPage: Int, data: ByteArray) {
        var page = startPage
        var offset = 0
        while (offset < data.size) {
            writePage(io, page, data.copyOfRange(offset, offset + PAGE_SIZE))
            page++
            offset += PAGE_SIZE
        }
    }

    private fun writePage(io: NfcIo, page: Int, data: ByteArray) {
        val cmd = byteArrayOf(T2_WRITE, page.toByte(), data[0], data[1], data[2], data[3])
        expectAck(io.transceive(cmd))
    }

    /** READ returns four pages (16 bytes); the first page is [page]. */
    private fun readPage(io: NfcIo, page: Int): ByteArray =
        readPage4(io, page).copyOf(PAGE_SIZE)

    private fun readPage4(io: NfcIo, page: Int): ByteArray =
        io.transceive(byteArrayOf(T2_READ, page.toByte()))

    private fun layoutTotalGuess(chip: ChipType): Int =
        (layoutOf(chip)?.let { it.userStartPage + it.userPages } ?: 16)
}
