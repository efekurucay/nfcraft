package com.nfcraft.app.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.nfcraft.app.engine.TagEngine
import com.nfcraft.core.model.ChipIdentifier
import com.nfcraft.core.model.NdefCodec
import com.nfcraft.core.model.NdefRecordModel
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcResult
import com.nfcraft.core.model.TagDump
import com.nfcraft.core.model.TagInfo
import com.nfcraft.core.model.TaskCodec
import com.nfcraft.core.model.TaskRecord
import com.nfcraft.core.model.TagWriter
import com.nfcraft.core.nfc.NfcIoHelper

/**
 * Bridges tag-tap actions to the engine modules. NDEF writes go through the
 * platform Ndef / NdefFormatable technologies; protection and dump go through
 * [TagWriter] over a raw [NfcIoHelper] session with chip identification first.
 */
class TagOperations(
    private val tagEngine: TagEngine,
    private val ndefCodec: NdefCodec,
    private val taskCodec: TaskCodec,
    private val chipIdentifier: ChipIdentifier,
    private val tagWriter: TagWriter,
) {
    fun read(tag: Tag): NfcResult<TagInfo> = tagEngine.read(tag)

    fun writeNdef(tag: Tag, records: List<NdefRecordModel>, lock: Boolean): NfcResult<Unit> =
        NfcResult.of {
            if (records.isEmpty()) throw NfcException(NfcException.WRITE_FAILED, "Nothing to write")
            val message = ndefCodec.encode(records)
            val size = message.toByteArray().size

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    if (!ndef.isWritable) throw NfcException(NfcException.NOT_WRITABLE, "Tag is read-only")
                    if (size > ndef.maxSize) throw NfcException(NfcException.MESSAGE_TOO_LARGE, "Message too large (${size}/${ndef.maxSize})")
                    ndef.writeNdefMessage(message)
                    if (lock && ndef.canMakeReadOnly()) ndef.makeReadOnly()
                } finally {
                    runCatching { ndef.close() }
                }
                return@of
            }

            val formatable = NdefFormatable.get(tag)
                ?: throw NfcException(NfcException.NOT_WRITABLE, "Tag does not support NDEF")
            formatable.connect()
            try {
                if (lock) formatable.formatReadOnly(message) else formatable.format(message)
            } finally {
                runCatching { formatable.close() }
            }
        }

    fun writeTask(tag: Tag, record: TaskRecord, lock: Boolean): NfcResult<Unit> {
        val (mime, payload) = taskCodec.serialize(record)
        val rec = NdefRecordModel.MimeData(mime, payload.toByteArray(Charsets.UTF_8))
        return writeNdef(tag, listOf(rec), lock)
    }

    fun setPassword(tag: Tag, password: String): NfcResult<Unit> =
        withIo(tag) { io, chip -> tagWriter.setPassword(io, chip, password) }

    fun removePassword(tag: Tag, password: String): NfcResult<Unit> =
        withIo(tag) { io, chip -> tagWriter.removePassword(io, chip, password) }

    fun makeReadOnly(tag: Tag): NfcResult<Unit> =
        withIo(tag) { io, chip -> tagWriter.makeReadOnly(io, chip) }

    fun rawApdu(tag: Tag, command: ByteArray): NfcResult<ByteArray> {
        val io = NfcIoHelper(tag)
        return try {
            io.connect()
            NfcResult.Success(io.transceive(command))
        } catch (e: NfcException) {
            NfcResult.Failure(e.code, e.message ?: "IO error")
        } finally {
            io.close()
        }
    }

    fun dump(tag: Tag): NfcResult<TagDump> {
        val io = NfcIoHelper(tag)
        return try {
            io.connect()
            val chip = chipIdentifier.identify(io).type
            tagWriter.dump(io, chip)
        } catch (e: NfcException) {
            NfcResult.Failure(e.code, e.message ?: "IO error")
        } catch (e: Exception) {
            NfcResult.Failure(NfcException.IO_ERROR, e.message ?: "Read error")
        } finally {
            io.close()
        }
    }

    private inline fun withIo(
        tag: Tag,
        block: (io: NfcIoHelper, chip: com.nfcraft.core.model.ChipType) -> NfcResult<Unit>,
    ): NfcResult<Unit> {
        val io = NfcIoHelper(tag)
        return try {
            io.connect()
            val chip = chipIdentifier.identify(io).type
            block(io, chip)
        } catch (e: NfcException) {
            NfcResult.Failure(e.code, e.message ?: "IO error")
        } catch (e: Exception) {
            NfcResult.Failure(NfcException.IO_ERROR, e.message ?: "IO error")
        } finally {
            io.close()
        }
    }
}
