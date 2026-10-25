package com.nfcraft.core.model

import android.nfc.NdefMessage

/** Encodes/decodes between raw NDEF and the high-level model. Owned by :core-ndef. */
interface NdefCodec {
    fun decode(message: NdefMessage): List<NdefRecordModel>
    fun encode(records: List<NdefRecordModel>): NdefMessage
    /** Byte length an encoded message would occupy on a tag. */
    fun encodedSize(records: List<NdefRecordModel>): Int
}

/** Resolves the connected tag's silicon. Owned by :core-chip. */
interface ChipIdentifier {
    fun identify(io: NfcIo): ChipInfo
}

/** Verifies originality signature against the manufacturer public key. Owned by :core-chip. */
interface SignatureVerifier {
    /** Returns true when the tag's READ_SIG response validates against the embedded key. */
    fun verify(io: NfcIo, chip: ChipType): Boolean
}

/**
 * Write / format / protect / dump operations. Owned by :core-tag.
 * Each call assumes a connected [NfcIo] and returns a uniform result.
 */
interface TagWriter {
    fun write(io: NfcIo, chip: ChipType, message: NdefMessage): NfcResult<Unit>
    fun format(io: NfcIo, chip: ChipType): NfcResult<Unit>
    fun makeReadOnly(io: NfcIo, chip: ChipType): NfcResult<Unit>

    /**
     * Protect writes with a password.
     * [password] is either 8 hex chars or an arbitrary passphrase (reduced to a
     * 4-byte key by the implementation). PACK is not written; only writes are
     * guarded (AUTH0=0).
     */
    fun setPassword(io: NfcIo, chip: ChipType, password: String): NfcResult<Unit>
    fun removePassword(io: NfcIo, chip: ChipType, password: String): NfcResult<Unit>

    /** Read the full memory map as raw pages/blocks. */
    fun dump(io: NfcIo, chip: ChipType): NfcResult<TagDump>
    /** Restore a captured dump back onto a tag (Pro-tier capability). */
    fun writeDump(io: NfcIo, chip: ChipType, dump: TagDump): NfcResult<Unit>
}

/** Raw memory capture: ordered blocks with their byte offsets. */
data class TagDump(
    val chip: ChipType,
    val blockSize: Int,
    val blocks: List<ByteArray>,
)
