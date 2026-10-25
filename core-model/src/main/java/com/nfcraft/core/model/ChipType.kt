package com.nfcraft.core.model

/**
 * Known tag silicon, as resolved by the chip-identification pipeline
 * (GET_VERSION, ATQA/SAK, ISO7816 SELECT, FeliCa PMm, ICODE family byte).
 *
 * [userMemoryBytes] is usable NDEF memory where applicable; 0 when variable or
 * unknown. [totalPages] applies to page-addressable Type 2 tags.
 */
enum class ChipType(
    val displayName: String,
    val family: ChipFamily,
    val userMemoryBytes: Int,
    val totalPages: Int,
) {
    NTAG210("NTAG210", ChipFamily.ULTRALIGHT, 48, 20),
    NTAG212("NTAG212", ChipFamily.ULTRALIGHT, 128, 41),
    NTAG213("NTAG213", ChipFamily.ULTRALIGHT, 144, 45),
    NTAG215("NTAG215", ChipFamily.ULTRALIGHT, 504, 135),
    NTAG216("NTAG216", ChipFamily.ULTRALIGHT, 888, 231),
    ULTRALIGHT("MIFARE Ultralight", ChipFamily.ULTRALIGHT, 48, 16),
    ULTRALIGHT_C("MIFARE Ultralight C", ChipFamily.ULTRALIGHT, 144, 48),
    ULTRALIGHT_EV1_48("MIFARE Ultralight EV1 (MF0UL11)", ChipFamily.ULTRALIGHT, 48, 20),
    ULTRALIGHT_EV1_128("MIFARE Ultralight EV1 (MF0UL21)", ChipFamily.ULTRALIGHT, 128, 41),
    NTAG203("NTAG203", ChipFamily.ULTRALIGHT, 144, 42),

    CLASSIC_MINI("MIFARE Classic Mini", ChipFamily.CLASSIC, 320, 0),
    CLASSIC_1K("MIFARE Classic 1K", ChipFamily.CLASSIC, 1024, 0),
    CLASSIC_4K("MIFARE Classic 4K", ChipFamily.CLASSIC, 4096, 0),
    PLUS("MIFARE Plus", ChipFamily.CLASSIC, 0, 0),

    DESFIRE("MIFARE DESFire", ChipFamily.DESFIRE, 0, 0),
    NTAG413_DNA("NTAG413 DNA", ChipFamily.DESFIRE, 0, 0),
    NTAG424_DNA("NTAG424 DNA", ChipFamily.DESFIRE, 0, 0),

    ICODE_SLI("ICODE SLI", ChipFamily.ISO15693, 112, 0),
    ICODE_SLIX("ICODE SLIX", ChipFamily.ISO15693, 112, 0),

    ST25("ST25 / ST", ChipFamily.ST, 0, 0),
    FELICA("FeliCa", ChipFamily.FELICA, 0, 0),

    UNKNOWN("Unknown", ChipFamily.UNKNOWN, 0, 0);
}

enum class ChipFamily {
    ULTRALIGHT, CLASSIC, DESFIRE, ISO15693, ST, FELICA, UNKNOWN
}

/** Resolved identification result with raw evidence kept for display. */
data class ChipInfo(
    val type: ChipType,
    val atqa: ByteArray? = null,
    val sak: Byte? = null,
    val manufacturer: String? = null,
    /** Raw GET_VERSION response when available. */
    val versionBytes: ByteArray? = null,
    /** True when hardware NXP signature was cryptographically verified. */
    val signatureVerified: Boolean = false,
)
