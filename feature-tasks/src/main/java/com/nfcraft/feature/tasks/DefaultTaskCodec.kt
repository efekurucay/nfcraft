package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCodec
import com.nfcraft.core.model.TaskRecord
import com.nfcraft.core.model.TaskExecutorFactory

/** Parses/serializes the "w<cat>/<no>" + pipe-delimited wire format. */
class DefaultTaskCodec(private val factory: TaskExecutorFactory = DefaultTaskExecutorFactory()) : TaskCodec {
    override fun parse(mimeType: String, payload: String): TaskRecord? {
        val descriptor = factory.catalog().firstOrNull { it.mimeType == mimeType } ?: return null
        val args = if (payload.isEmpty()) emptyList() else payload.split('|')
        return TaskRecord(descriptor, args).takeIf { record ->
            runCatching { validate(record) }.isSuccess
        }
    }

    override fun serialize(record: TaskRecord): Pair<String, String> {
        val descriptor = requireNotNull(factory.catalog().firstOrNull {
            it.mimeType == record.descriptor.mimeType
        }) { "Unknown task" }
        validate(record.copy(descriptor = descriptor))
        require(record.args.none { '|' in it }) { "The task protocol cannot escape pipe characters" }
        require(record.args != listOf("")) { "An empty single argument is ambiguous on the wire" }
        return descriptor.mimeType to record.args.joinToString("|")
    }
}
