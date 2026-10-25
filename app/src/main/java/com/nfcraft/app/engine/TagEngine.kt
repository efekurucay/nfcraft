package com.nfcraft.app.engine

import android.nfc.Tag
import android.nfc.tech.Ndef
import com.nfcraft.core.model.ChipInfo
import com.nfcraft.core.model.ChipIdentifier
import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NdefCodec
import com.nfcraft.core.model.NdefData
import com.nfcraft.core.model.NfcResult
import com.nfcraft.core.model.SignatureVerifier
import com.nfcraft.core.model.TagInfo
import com.nfcraft.core.nfc.NfcIoHelper

/**
 * Reads a discovered [Tag] into a [TagInfo]: identifies the chip over a raw
 * transceive session, reads any NDEF message via the Ndef technology, and
 * attempts originality-signature verification. Blocking; call off the main thread.
 */
class TagEngine(
    private val ndefCodec: NdefCodec,
    private val chipIdentifier: ChipIdentifier,
    private val signatureVerifier: SignatureVerifier,
) {
    fun read(tag: Tag): NfcResult<TagInfo> = NfcResult.of {
        val ndef = readNdef(tag)

        val io = NfcIoHelper(tag)
        val chip: ChipInfo = try {
            io.connect()
            val base = chipIdentifier.identify(io)
            val verified = runCatching { signatureVerifier.verify(io, base.type) }.getOrDefault(false)
            base.copy(signatureVerified = verified)
        } catch (e: Exception) {
            ChipInfo(type = ChipType.UNKNOWN)
        } finally {
            io.close()
        }

        TagInfo(
            uid = tag.id,
            techList = tag.techList.toList(),
            chip = chip,
            ndef = ndef,
            serial = tag.id.toHex(),
        )
    }

    private fun readNdef(tag: Tag): NdefData? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage
            val records = message?.let { ndefCodec.decode(it) } ?: emptyList()
            NdefData(
                records = records,
                byteSize = message?.toByteArray()?.size ?: 0,
                maxSize = ndef.maxSize,
                writable = ndef.isWritable,
                canMakeReadOnly = ndef.canMakeReadOnly(),
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(":") { "%02X".format(it) }
}
