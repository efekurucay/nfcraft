package com.nfcraft.feature.tasks

import android.content.Context
import com.nfcraft.core.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Executes balanced, nested condition blocks in order and stops on failure or abort. */
class TaskSequenceRunner(private val factory: TaskExecutorFactory = DefaultTaskExecutorFactory()) {
    private data class Frame(val parentActive: Boolean, val taken: Boolean, var inElse: Boolean = false) {
        val active: Boolean get() = parentActive && if (inElse) !taken else taken
    }

    suspend fun execute(context: Context, records: List<TaskRecord>): TaskResult {
        // Validate structure before any actions, including blocks that will be skipped.
        val structure = mutableListOf<Boolean>()
        for (record in records) {
            val descriptor = factory.catalog().firstOrNull { it.mimeType == record.descriptor.mimeType }
                ?: return TaskResult.Failure("Unknown task ${record.descriptor.mimeType}")
            try { validate(record.copy(descriptor = descriptor)) } catch (e: IllegalArgumentException) {
                return TaskResult.Failure(e.message ?: "Invalid parameters")
            }
            if (descriptor.category != TaskCategory.CONDITION) continue
            when (descriptor.number) {
                19 -> {
                    if (structure.isEmpty() || structure.last()) return TaskResult.Failure("Unmatched or repeated else")
                    structure[structure.lastIndex] = true
                }
                20 -> {
                    if (structure.isEmpty()) return TaskResult.Failure("Unmatched end if")
                    structure.removeAt(structure.lastIndex)
                }
                else -> structure.add(false)
            }
        }
        if (structure.isNotEmpty()) return TaskResult.Failure("Unclosed if block")
        val stack = mutableListOf<Frame>()
        for (record in records) {
            currentCoroutineContext().ensureActive()
            val active = stack.lastOrNull()?.active ?: true
            if (record.descriptor.category == TaskCategory.CONDITION) {
                when (record.descriptor.number) {
                    19 -> stack.last().inElse = true
                    20 -> stack.removeAt(stack.lastIndex)
                    else -> {
                        val result = if (active) factory.executorFor(record)?.execute(context, record)
                            ?: TaskResult.Failure("Condition executor unavailable") else TaskResult.ConditionBranch(false)
                        if (result !is TaskResult.ConditionBranch) return result
                        stack.add(Frame(active, result.taken))
                    }
                }
            } else if (active) {
                val result = factory.executorFor(record)?.execute(context, record)
                    ?: TaskResult.Failure("Task is catalog-only: ${record.descriptor.label}")
                if (result != TaskResult.Success) return result
            }
        }
        return TaskResult.Success
    }
}
