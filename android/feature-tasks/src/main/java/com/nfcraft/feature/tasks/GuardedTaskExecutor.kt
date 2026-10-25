package com.nfcraft.feature.tasks

import android.content.Context
import com.nfcraft.core.model.*
import kotlinx.coroutines.CancellationException

/** Canonicalizes metadata before authorization so a tag cannot remove the dangerous flag. */
abstract class GuardedTaskExecutor(
    final override val category: TaskCategory,
    private val supported: Set<Int>,
    private val guard: DangerousTaskGuard = DangerousTaskGuard.Deny,
) : TaskExecutor {
    final override fun handles(descriptor: TaskDescriptor) =
        descriptor.category == category && descriptor.number in supported &&
            TaskCatalog.descriptors.any { it.category == category && it.number == descriptor.number }

    final override suspend fun execute(context: Context, record: TaskRecord): TaskResult {
        if (!handles(record.descriptor)) return TaskResult.Failure("Task is not implemented")
        val descriptor = TaskCatalog.descriptors.first {
            it.category == category && it.number == record.descriptor.number
        }
        return try {
            if (descriptor.dangerous && !guard.confirm(descriptor)) return TaskResult.Aborted
            val canonical = record.copy(descriptor = descriptor)
            validate(canonical)
            perform(context, canonical)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TaskResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    protected abstract suspend fun perform(context: Context, record: TaskRecord): TaskResult
}
