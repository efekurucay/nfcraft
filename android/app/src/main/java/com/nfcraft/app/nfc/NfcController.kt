package com.nfcraft.app.nfc

import android.nfc.Tag
import com.nfcraft.core.model.NdefRecordModel
import com.nfcraft.core.model.NfcResult
import com.nfcraft.core.model.TagDump
import com.nfcraft.core.model.TagInfo
import com.nfcraft.core.model.TaskRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** What the next tag tap should do. Screens arm a mode; a tap consumes it. */
sealed interface NfcMode {
    data object Read : NfcMode
    data class WriteNdef(val records: List<NdefRecordModel>, val lock: Boolean = false) : NfcMode
    data class WriteTask(val record: TaskRecord, val lock: Boolean = false) : NfcMode
    data class SetPassword(val password: String) : NfcMode
    data class RemovePassword(val password: String) : NfcMode
    data object MakeReadOnly : NfcMode
    data object Dump : NfcMode
    data class RawApdu(val command: ByteArray) : NfcMode
}

sealed interface ReadState {
    data object Waiting : ReadState
    data object Reading : ReadState
    data class Loaded(val tag: TagInfo) : ReadState
    data class Error(val code: Int, val message: String) : ReadState
}

/** One-shot outcome of an armed operation, delivered after a tap. */
data class OpResult(
    val label: String,
    val success: Boolean,
    val message: String,
    val dump: TagDump? = null,
)

/**
 * Single owner of tag-tap routing. The activity keeps the reader enabled and
 * forwards every tag to [onTag]; the armed [mode] decides what runs. Read is the
 * resting mode; a one-shot write/protect/dump reverts to Read once done.
 */
class NfcController(private val ops: TagOperations) {

    private val _mode = MutableStateFlow<NfcMode>(NfcMode.Read)
    val mode: StateFlow<NfcMode> = _mode.asStateFlow()

    private val _read = MutableStateFlow<ReadState>(ReadState.Waiting)
    val read: StateFlow<ReadState> = _read.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _results = MutableSharedFlow<OpResult>(extraBufferCapacity = 4)
    val results: SharedFlow<OpResult> = _results.asSharedFlow()

    /** Last raw APDU response (for the Advanced console), or null. */
    private val _apduResponse = MutableStateFlow<ByteArray?>(null)
    val apduResponse: StateFlow<ByteArray?> = _apduResponse.asStateFlow()

    fun arm(mode: NfcMode) {
        _mode.value = mode
    }

    fun toRead() {
        _mode.value = NfcMode.Read
        _read.value = ReadState.Waiting
    }

    suspend fun onTag(tag: Tag) {
        _busy.value = true
        try {
            when (val m = _mode.value) {
                NfcMode.Read -> runRead(tag)
                is NfcMode.WriteNdef -> emit("Write", withContext(Dispatchers.IO) { ops.writeNdef(tag, m.records, m.lock) })
                is NfcMode.WriteTask -> emit("Write task", withContext(Dispatchers.IO) { ops.writeTask(tag, m.record, m.lock) })
                is NfcMode.SetPassword -> emitThenRead(tag, "Set password", withContext(Dispatchers.IO) { ops.setPassword(tag, m.password) })
                is NfcMode.RemovePassword -> emitThenRead(tag, "Remove password", withContext(Dispatchers.IO) { ops.removePassword(tag, m.password) })
                NfcMode.MakeReadOnly -> emitThenRead(tag, "Lock", withContext(Dispatchers.IO) { ops.makeReadOnly(tag) })
                NfcMode.Dump -> emitDump(withContext(Dispatchers.IO) { ops.dump(tag) })
                is NfcMode.RawApdu -> emitApdu(withContext(Dispatchers.IO) { ops.rawApdu(tag, m.command) })
            }
        } finally {
            _busy.value = false
        }
    }

    private suspend fun runRead(tag: Tag) {
        _read.value = ReadState.Reading
        _read.value = when (val r = withContext(Dispatchers.IO) { ops.read(tag) }) {
            is NfcResult.Success -> ReadState.Loaded(r.value)
            is NfcResult.Failure -> ReadState.Error(r.code, r.message)
        }
    }

    private suspend fun emit(label: String, result: NfcResult<Unit>) {
        _results.emit(result.toOp(label))
        _mode.value = NfcMode.Read
    }

    private suspend fun emitThenRead(tag: Tag, label: String, result: NfcResult<Unit>) {
        _results.emit(result.toOp(label))
        _mode.value = NfcMode.Read
    }

    private suspend fun emitDump(result: NfcResult<TagDump>) {
        val op = when (result) {
            is NfcResult.Success -> OpResult("Dump", true, "${result.value.blocks.size} blocks read", result.value)
            is NfcResult.Failure -> OpResult("Dump", false, "(${result.code}) ${result.message}")
        }
        _results.emit(op)
        _mode.value = NfcMode.Read
    }

    private suspend fun emitApdu(result: NfcResult<ByteArray>) {
        val op = when (result) {
            is NfcResult.Success -> {
                _apduResponse.value = result.value
                OpResult("APDU", true, "${result.value.size} bytes received")
            }
            is NfcResult.Failure -> OpResult("APDU", false, "(${result.code}) ${result.message}")
        }
        _results.emit(op)
        _mode.value = NfcMode.Read
    }

    private fun NfcResult<Unit>.toOp(label: String): OpResult = when (this) {
        is NfcResult.Success -> OpResult(label, true, "Done")
        is NfcResult.Failure -> OpResult(label, false, "($code) $message")
    }
}
