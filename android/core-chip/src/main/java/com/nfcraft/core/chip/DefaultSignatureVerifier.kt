package com.nfcraft.core.chip

import com.nfcraft.core.model.ChipFamily
import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import com.nfcraft.core.model.SignatureVerifier

/**
 * Verifies the NXP originality signature. The tag returns a 32-byte `secp128r1`
 * ECDSA signature over its own UID; [Secp128r1.verify] checks it against the
 * manufacturer public key embedded for that chip family. A genuine NXP tag
 * passes; a clone cannot produce a signature that validates against the key.
 *
 * READ_SIG differs by family: Ultralight / NTAG use the Type 2 `3C 00` command;
 * ICODE uses the ISO15693 NXP custom Read Signature command (`0xB2`, IC
 * manufacturer `0x04`).
 */
class DefaultSignatureVerifier : SignatureVerifier {

    override fun verify(io: NfcIo, chip: ChipType): Boolean = when (chip.family) {
        ChipFamily.ULTRALIGHT -> verifyType2(io)
        ChipFamily.ISO15693 -> verifyIcode(io)
        else -> false
    }

    /** NTAG / Ultralight EV1: `3C 00` returns a 32-byte signature over the UID. */
    private fun verifyType2(io: NfcIo): Boolean {
        if (!io.useTechnology(Tech.NFC_A) && Tech.NFC_A in io.techList) return false
        val signature = readSignature(io, CMD_READ_SIG) ?: return false
        return Secp128r1.verify(NXP_NTAG_KEY, io.uid, signature)
    }

    /** ICODE: the ISO15693 Read Signature custom command returns 32 bytes. */
    private fun verifyIcode(io: NfcIo): Boolean {
        if (!io.useTechnology(Tech.NFC_V)) return false
        val signature = readSignature(io, CMD_ICODE_READ_SIG) ?: return false
        return Secp128r1.verify(NXP_ICODE_KEY, io.uid, signature)
    }

    /**
     * Transceive the READ_SIG command and normalise the response to the bare
     * 32-byte signature, dropping a leading ISO15693 status byte when present.
     */
    private fun readSignature(io: NfcIo, command: ByteArray): ByteArray? {
        val response = try {
            io.transceive(command)
        } catch (_: NfcException) {
            return null
        }
        return when {
            response.size == 32 -> response
            response.size == 33 && response[0].toInt() == 0x00 -> response.copyOfRange(1, 33)
            else -> null
        }
    }

    private companion object {
        /** Type 2 READ_SIG. */
        val CMD_READ_SIG = byteArrayOf(0x3C, 0x00)

        /** ISO15693 NXP Read Signature: flags `0x02`, command `0xB2`, mfg `0x04`. */
        val CMD_ICODE_READ_SIG = byteArrayOf(0x02, 0xB2.toByte(), 0x04)

        /** NXP NTAG / Ultralight originality-signature public key (secp128r1, SEC uncompressed). */
        val NXP_NTAG_KEY = hexToBytes(
            "04494E1A386D3D3CFE3DC10E5DE68A499B1C202DB5B132393E89ED19FE5BE8BC61",
        )

        /** NXP ICODE originality-signature public key (secp128r1, SEC uncompressed). */
        val NXP_ICODE_KEY = hexToBytes(
            "048878A2A2D3EEF511B1B8A68D73CF9B9524D72C780F6E4AE93375B9F35147CB76",
        )
    }
}
