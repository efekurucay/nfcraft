package com.nfcraft.core.model

import android.content.Context

/**
 * Task protocol model. A task is carried as an NDEF MIME record whose type is
 * "w<category>/<number>" and whose payload is pipe-delimited UTF-8 parameters.
 */
enum class TaskCategory(val code: Int, val label: String) {
    NETWORK(1, "Network / WiFi"),
    BLUETOOTH(2, "Bluetooth"),
    PHONE(3, "Phone"),
    ALARM_FILE(4, "Alarm & Files"),
    SOUND(5, "Sound"),
    SCREEN(6, "Screen"),
    CONFIG(7, "Configuration"),
    APP_MISC(8, "Apps & Misc"),
    CONDITION(9, "Conditions (if/else/end)"),
    ROOT(666, "Root / Dangerous");

    companion object {
        fun fromCode(code: Int): TaskCategory? = entries.firstOrNull { it.code == code }
    }
}

/** One task's identity and metadata, independent of a concrete tag payload. */
data class TaskDescriptor(
    val category: TaskCategory,
    val number: Int,
    val label: String,
    /** Declared parameter slots, in payload order. */
    val params: List<TaskParam>,
    /** True for tasks that require root (`su`) or are otherwise destructive. */
    val dangerous: Boolean = false,
) {
    val mimeType: String get() = "w${category.code}/$number"
}

data class TaskParam(val name: String, val type: TaskParamType, val optional: Boolean = false)

enum class TaskParamType { TEXT, NUMBER, BOOLEAN, ENUM, FILE, PACKAGE }

/** A concrete task instance read from a tag: descriptor + bound argument values. */
data class TaskRecord(
    val descriptor: TaskDescriptor,
    val args: List<String>,
)

sealed interface TaskResult {
    data object Success : TaskResult
    data class Failure(val message: String) : TaskResult
    /** Condition tasks steer control flow rather than acting. */
    data class ConditionBranch(val taken: Boolean) : TaskResult
    data object Aborted : TaskResult
}

/** Executes one task family. Owned by :feature-tasks. */
interface TaskExecutor {
    val category: TaskCategory
    fun handles(descriptor: TaskDescriptor): Boolean
    suspend fun execute(context: Context, record: TaskRecord): TaskResult
}

/** Resolves a [TaskRecord] to its [TaskExecutor]. Owned by :feature-tasks. */
interface TaskExecutorFactory {
    fun executorFor(record: TaskRecord): TaskExecutor?
    /** Full catalog for the builder UI. */
    fun catalog(): List<TaskDescriptor>
}

/** Parses/serializes the wire format "w<cat>/<no>" + pipe-delimited payload. */
interface TaskCodec {
    fun parse(mimeType: String, payload: String): TaskRecord?
    fun serialize(record: TaskRecord): Pair<String, String>
}
