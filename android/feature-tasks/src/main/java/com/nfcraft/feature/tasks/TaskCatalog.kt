package com.nfcraft.feature.tasks

import com.nfcraft.core.model.*

internal fun task(
    category: TaskCategory,
    number: Int,
    label: String,
    schema: String,
    dangerous: Boolean = false,
) = TaskDescriptor(category, number, label, schema.split(' ').filter { it.isNotEmpty() }.map {
    val (name, type) = it.split(':')
    TaskParam(name, TaskParamType.valueOf(type.removeSuffix("?")), type.endsWith('?'))
}, dangerous)

object TaskCatalog {
    val descriptors: List<TaskDescriptor> = listOf(
        NetworkTasks.catalog, BluetoothTasks.catalog, PhoneTasks.catalog, AlarmFileTasks.catalog,
        SoundTasks.catalog, ScreenTasks.catalog, ConfigTasks.catalog, AppMiscTasks.catalog,
        ConditionTasks.catalog, RootTasks.catalog,
    ).flatten()
}

internal fun validate(record: TaskRecord) {
    val params = record.descriptor.params
    require(record.args.size <= params.size) { "Too many parameters" }
    params.forEachIndexed { index, param ->
        val value = record.args.getOrNull(index)
        require(param.optional || value != null) { "Missing ${param.name}" }
        if (value != null && !(param.optional && value.isEmpty())) {
            when (param.type) {
                TaskParamType.NUMBER -> require(value.toDoubleOrNull()?.isFinite() == true) { "Invalid ${param.name}" }
                TaskParamType.BOOLEAN -> require(value == "true" || value == "false") { "Invalid ${param.name}" }
                else -> Unit
            }
        }
    }
}
