package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskDescriptor
import com.nfcraft.core.model.TaskExecutor
import com.nfcraft.core.model.TaskExecutorFactory
import com.nfcraft.core.model.TaskRecord

/** Registry with a complete builder catalog and executors for supported actions. */
class DefaultTaskExecutorFactory(
    private val executors: List<TaskExecutor> = defaultTaskExecutors(),
    private val descriptors: List<TaskDescriptor> = TaskCatalog.descriptors,
) : TaskExecutorFactory {
    override fun executorFor(record: TaskRecord): TaskExecutor? =
        descriptors.firstOrNull { it.mimeType == record.descriptor.mimeType }?.let { descriptor ->
            executors.firstOrNull { it.category == descriptor.category && it.handles(descriptor) }
        }

    override fun catalog(): List<TaskDescriptor> = descriptors
}

fun createDefaultTaskExecutorFactory(
    guard: DangerousTaskGuard = DangerousTaskGuard.Deny,
): DefaultTaskExecutorFactory = DefaultTaskExecutorFactory(defaultTaskExecutors(guard))

internal fun defaultTaskExecutors(guard: DangerousTaskGuard = DangerousTaskGuard.Deny): List<TaskExecutor> =
    listOf(NetworkTaskExecutor(guard), HttpTaskExecutor(guard), BluetoothTaskExecutor(guard),
        PhoneTaskExecutor(guard), AlarmFileTaskExecutor(guard), SoundTaskExecutor(guard),
        ScreenTaskExecutor(guard), ConfigTaskExecutor(guard), AppMiscTaskExecutor(guard),
        ConditionTaskExecutor(), RootTaskExecutor(guard))
