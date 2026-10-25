package com.nfcraft.core.model

/** Everything surfaced after a successful read of a tag. */
data class TagInfo(
    val uid: ByteArray,
    val techList: List<String>,
    val chip: ChipInfo,
    val ndef: NdefData?,
    /** Serial number formatted for display (hex, colon-separated). */
    val serial: String,
)
