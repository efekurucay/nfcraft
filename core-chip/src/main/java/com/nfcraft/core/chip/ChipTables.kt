package com.nfcraft.core.chip

import com.nfcraft.core.model.ChipType

/** Android `android.nfc.tech` class names for requesting a transceive path. */
internal object Tech {
    const val NFC_A = "android.nfc.tech.NfcA"
    const val NFC_V = "android.nfc.tech.NfcV"
    const val ISO_DEP = "android.nfc.tech.IsoDep"
    const val MIFARE_ULTRALIGHT = "android.nfc.tech.MifareUltralight"
    const val MIFARE_CLASSIC = "android.nfc.tech.MifareClassic"
}

/**
 * Static identification tables derived from the behaviour reference: the
 * ISO/IEC 7816-6 IC-manufacturer registry, the GET_VERSION product/storage map,
 * and the ATQA/SAK → chip map. The ATQA/SAK table is keyed but only usable once
 * [com.nfcraft.core.model.NfcIo] exposes those low-level values (see report).
 */
internal object ChipTables {

    /** NXP vendor id in GET_VERSION byte[1] and the ISO15693 UID manufacturer byte. */
    const val VENDOR_NXP = 0x04

    /**
     * ISO/IEC 7816-6 IC manufacturer registration codes, keyed by the leading
     * UID byte. Covers the vendors reachable by this app's supported silicon.
     */
    val MANUFACTURERS: Map<Int, String> = mapOf(
        0x02 to "STMicroelectronics",
        0x04 to "NXP Semiconductors",
        0x05 to "Infineon Technologies",
        0x07 to "Texas Instruments",
        0x15 to "Nordic Semiconductor",
        0x16 to "EM Microelectronic-Marin",
        0x1D to "Ubisys Technologies",
        0x28 to "SpringCard",
        0x2B to "Maxim Integrated",
        0x33 to "AMIC Technology",
        0x44 to "Honeywell",
    )

    /**
     * GET_VERSION map: (product type byte, storage-size byte) → chip.
     *
     * Product type `0x04` is the NTAG21x line, `0x03` is MIFARE Ultralight EV1.
     * The storage-size byte encodes the memory tier. The two lines share storage
     * codes (e.g. `0x0B` = 48 bytes), so the product-type byte disambiguates.
     */
    val VERSION_TABLE: Map<Pair<Int, Int>, ChipType> = mapOf(
        (0x04 to 0x0B) to ChipType.NTAG210,
        (0x04 to 0x0E) to ChipType.NTAG212,
        (0x04 to 0x0F) to ChipType.NTAG213,
        (0x04 to 0x11) to ChipType.NTAG215,
        (0x04 to 0x13) to ChipType.NTAG216,
        (0x03 to 0x0B) to ChipType.ULTRALIGHT_EV1_48,
        (0x03 to 0x0E) to ChipType.ULTRALIGHT_EV1_128,
    )

    /**
     * ATQA/SAK → chip map. Keyed by `(atqa little-endian as int, sak as int)`.
     * Ready for the probe once ATQA/SAK are reachable; the `0x0044`/`0x00` row is
     * ambiguous (NTAG203 vs Ultralight C) and is resolved by a page-wrap probe.
     */
    val ATQA_SAK_TABLE: Map<Pair<Int, Int>, ChipType> = mapOf(
        (0x0004 to 0x08) to ChipType.CLASSIC_1K,
        (0x0002 to 0x18) to ChipType.CLASSIC_4K,
        (0x0004 to 0x09) to ChipType.CLASSIC_MINI,
        (0x0044 to 0x20) to ChipType.DESFIRE,
        (0x0344 to 0x20) to ChipType.DESFIRE,
        (0x0004 to 0x20) to ChipType.PLUS,
        (0x0044 to 0x00) to ChipType.ULTRALIGHT,
    )

    /** Resolve a Classic/DESFire tag from ATQA/SAK once those become available. */
    fun byAtqaSak(atqaLe: Int, sak: Int): ChipType =
        ATQA_SAK_TABLE[atqaLe to sak] ?: ChipType.UNKNOWN

    /** Human-readable manufacturer for a UID whose first byte is the IC code. */
    fun manufacturerOf(uid: ByteArray): String? {
        if (uid.isEmpty()) return null
        return MANUFACTURERS[uid.u(0)]
    }
}
