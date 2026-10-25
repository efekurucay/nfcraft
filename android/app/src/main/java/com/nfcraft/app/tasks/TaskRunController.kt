package com.nfcraft.app.tasks

import android.content.Context
import com.nfcraft.core.model.NdefRecordModel
import com.nfcraft.core.model.TaskCodec
import com.nfcraft.core.model.TaskRecord
import com.nfcraft.core.model.TaskResult
import com.nfcraft.feature.tasks.TaskSequenceRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detects task records on a scanned tag and runs them through the
 * [TaskSequenceRunner]. Execution runs on an app-scoped coroutine so it survives
 * screen navigation; dangerous tasks are gated by the factory's guard, which
 * drives the confirmation dialog.
 */
class TaskRunController(
    private val runner: TaskSequenceRunner,
    private val codec: TaskCodec,
    private val appContext: Context,
) {
    sealed interface RunState {
        data object Idle : RunState
        data class Running(val count: Int) : RunState
        data class Finished(val result: TaskResult) : RunState
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<RunState>(RunState.Idle)
    val state: StateFlow<RunState> = _state.asStateFlow()

    /** Parse the task records carried by a tag's NDEF content, in order. */
    fun detect(records: List<NdefRecordModel>): List<TaskRecord> =
        records.mapNotNull { record ->
            if (record is NdefRecordModel.MimeData) {
                codec.parse(record.mimeType, String(record.payload, Charsets.UTF_8))
            } else {
                null
            }
        }

    fun run(records: List<TaskRecord>) {
        if (records.isEmpty() || _state.value is RunState.Running) return
        _state.value = RunState.Running(records.size)
        scope.launch {
            val result = runner.execute(appContext, records)
            _state.value = RunState.Finished(result)
        }
    }

    fun reset() {
        _state.value = RunState.Idle
    }
}
