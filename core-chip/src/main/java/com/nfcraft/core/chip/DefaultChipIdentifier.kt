package com.nfcraft.core.chip

import com.nfcraft.core.model.ChipIdentifier
import com.nfcraft.core.model.ChipInfo
import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo

/**
 * Resolves tag silicon through an ordered probe chain:
 *
 * 1. GET_VERSION over NfcA — resolves the NTAG21x and Ultralight EV1 lines.
 * 2. Page-wrap fallback — NTAG203 vs Ultralight C when GET_VERSION is absent.
 * 3. ISO7816 SELECT (`90 60 ...`) over IsoDep — DESFire / NTAG4xx.
 * 4. ISO15693 Get System Info over NfcV — the ICODE family.
 *
 * ATQA and SAK are not exposed by [NfcIo], so Classic-family resolution and the
 * precise NTAG203/Ultralight-C split depend on evidence gathered over
 * [NfcIo.transceive]; fields that cannot be obtained are left null.
 */
class DefaultChipIdentifier : ChipIdentifier {

    override fun identify(io: NfcIo): ChipInfo {
        val manufacturer = ChipTables.manufacturerOf(io.uid)
        val atqa = io.atqa
        val sak = io.sak

        // Active probes first: they carry the richest evidence (GET_VERSION, DNA
        // GET_VERSION, ICODE system info) and resolve the NTAG / EV1 / DESFire-DNA
        // and ICODE lines precisely.
        if (Tech.NFC_A in io.techList || Tech.MIFARE_ULTRALIGHT in io.techList) {
            identifyNfcA(io, manufacturer)?.let { return it.withAtqaSak(atqa, sak) }
        }
        if (Tech.ISO_DEP in io.techList) {
            identifyIsoDep(io, manufacturer)?.let { return it.withAtqaSak(atqa, sak) }
        }
        if (Tech.NFC_V in io.techList) {
            identifyIcode(io, manufacturer)?.let { return it }
        }

        // Passive fallback: Classic / Plus / plain Ultralight answer none of the
        // probes above and are resolved from ATQA + SAK.
        if (atqa != null && atqa.size >= 2 && sak != null) {
            val atqaLe = (atqa.u(0)) or (atqa.u(1) shl 8)
            val type = ChipTables.byAtqaSak(atqaLe, sak.toInt() and 0xFF)
            if (type != ChipType.UNKNOWN) {
                return ChipInfo(type = type, atqa = atqa, sak = sak, manufacturer = manufacturer)
            }
        }
        return ChipInfo(type = ChipType.UNKNOWN, atqa = atqa, sak = sak, manufacturer = manufacturer)
    }

    private fun ChipInfo.withAtqaSak(atqa: ByteArray?, sak: Byte?): ChipInfo =
        if (this.atqa == null && this.sak == null) copy(atqa = atqa, sak = sak) else this

    /** GET_VERSION first, then the NTAG203/Ultralight-C page-wrap fallback. */
    private fun identifyNfcA(io: NfcIo, manufacturer: String?): ChipInfo? {
        if (!io.useTechnology(Tech.NFC_A) && Tech.NFC_A in io.techList) return null

        val version = tryTransceive(io, CMD_GET_VERSION)
        if (version != null && version.size >= 7 && version.u(1) == ChipTables.VENDOR_NXP) {
            val type = ChipTables.VERSION_TABLE[version.u(2) to version.u(STORAGE_BYTE)]
            if (type != null) {
                return ChipInfo(
                    type = type,
                    manufacturer = manufacturer,
                    versionBytes = version,
                )
            }
        }

        val fallback = ultralightFallback(io)
        if (fallback != ChipType.UNKNOWN) {
            return ChipInfo(type = fallback, manufacturer = manufacturer, versionBytes = version)
        }
        return null
    }

    /**
     * Tags that answer ATQA `0x0044` / SAK `0x00` are either NTAG203 (42 pages)
     * or Ultralight C (48 pages). A Type 2 READ wraps modulo the page count, so
     * reading page 41 returns page 0 as its second page on NTAG203 but a real
     * page 42 on Ultralight C.
     */
    private fun ultralightFallback(io: NfcIo): ChipType {
        val page0 = tryTransceive(io, byteArrayOf(CMD_READ, 0x00)) ?: return ChipType.UNKNOWN
        if (page0.size < 4) return ChipType.UNKNOWN
        val wrapped = tryTransceive(io, byteArrayOf(CMD_READ, 41)) ?: return ChipType.UNKNOWN
        if (wrapped.size < 16) return ChipType.UNKNOWN

        val secondPage = wrapped.copyOfRange(4, 8)
        return if (secondPage.contentEquals(page0.copyOfRange(0, 4))) {
            ChipType.NTAG203
        } else {
            ChipType.ULTRALIGHT_C
        }
    }

    /**
     * DESFire-family native GET_VERSION wrapped in ISO7816 (`90 60 00 00 00`).
     * A compliant tag answers with a 7-byte frame plus the `91 AF`
     * additional-frames status. The hardware type/subtype/storage fields pick
     * the NTAG4xx DNA parts out of the generic DESFire line.
     */
    private fun identifyIsoDep(io: NfcIo, manufacturer: String?): ChipInfo? {
        if (!io.useTechnology(Tech.ISO_DEP)) return null

        val response = tryTransceive(io, CMD_ISO7816_GET_VERSION) ?: return null
        if (response.size < 9) return null
        val sw1 = response.u(response.size - 2)
        val sw2 = response.u(response.size - 1)
        if (sw1 != 0x91 || sw2 != 0xAF) return null

        val vendor = response.u(0)
        val hwType = response.u(1)
        val storage = response.u(5)
        val type = when {
            vendor != ChipTables.VENDOR_NXP -> ChipType.DESFIRE
            hwType == 0x04 && storage == 0x0F -> ChipType.NTAG413_DNA
            hwType == 0x04 && storage == 0x11 -> ChipType.NTAG424_DNA
            else -> ChipType.DESFIRE
        }
        return ChipInfo(type = type, manufacturer = manufacturer, versionBytes = response)
    }

    /**
     * ISO15693 Get System Info (`0x2B`). The NXP IC-reference byte (when the
     * info flags report one) separates ICODE SLIX from plain SLI. NXP ICODE UIDs
     * carry `0xE0` as the top byte and `0x04` as the manufacturer byte.
     */
    private fun identifyIcode(io: NfcIo, manufacturer: String?): ChipInfo? {
        if (!io.useTechnology(Tech.NFC_V)) return null

        val info = tryTransceive(io, CMD_ISO15693_SYSTEM_INFO)
        val icManufacturer = ChipTables.MANUFACTURERS[io.uid.u(io.uid.size - 2)] ?: manufacturer
        val type = classifyIcode(info)
        return ChipInfo(type = type, manufacturer = icManufacturer)
    }

    private fun classifyIcode(info: ByteArray?): ChipType {
        // Info flags bit3 set => IC reference present as the last data byte.
        if (info == null || info.isEmpty() || info.u(0) != 0x00) return ChipType.ICODE_SLI
        val flags = info.u(1)
        if (flags and 0x08 == 0) return ChipType.ICODE_SLI
        val icRef = info.u(info.size - 1)
        return if (icRef and 0x80 != 0) ChipType.ICODE_SLIX else ChipType.ICODE_SLI
    }

    /** Run a command, treating any [NfcException] as "feature not supported". */
    private fun tryTransceive(io: NfcIo, command: ByteArray): ByteArray? = try {
        io.transceive(command)
    } catch (_: NfcException) {
        null
    }

    private companion object {
        /** NTAG / Ultralight EV1 GET_VERSION. */
        val CMD_GET_VERSION = byteArrayOf(0x60)

        /** Type 2 READ (returns 4 pages = 16 bytes with page-count wrap-around). */
        const val CMD_READ: Byte = 0x30

        /** ISO7816-wrapped DESFire native GET_VERSION. */
        val CMD_ISO7816_GET_VERSION =
            byteArrayOf(0x90.toByte(), 0x60, 0x00, 0x00, 0x00)

        /** ISO15693 Get System Info, flags `0x02` (high data rate). */
        val CMD_ISO15693_SYSTEM_INFO = byteArrayOf(0x02, 0x2B)

        /** GET_VERSION storage-size index; product type sits in byte[2]. */
        const val STORAGE_BYTE = 6
    }
}
