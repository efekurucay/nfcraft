package com.nfcraft.feature.tasks

import android.content.Context
import com.nfcraft.core.model.*

/** These tasks run arbitrary privileged commands; every invocation requires guard confirmation. */
class RootTaskExecutor(
    guard: DangerousTaskGuard = DangerousTaskGuard.Deny,
    private val shell: RootShell = RootShell(),
) : GuardedTaskExecutor(TaskCategory.ROOT, (1..20).toSet(), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        fun quoted(index: Int) = "'" + record.args[index].replace("'", "'\"'\"'") + "'"
        fun number(index: Int) = record.args[index].toInt().also { require(it >= 0) }.toString()
        fun pkg(): String = quoted(0).also {
            require(record.args[0].matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))) { "Invalid package" }
        }
        val command = when (record.descriptor.number) {
            1 -> record.args[0]
            2 -> "reboot"
            3 -> "reboot -p"
            4 -> "input keyevent ${number(0)}"
            5 -> "am force-stop ${pkg()}"
            6 -> "screencap -p ${quoted(0)}"
            7 -> "settings put global zen_mode 0"
            8 -> "settings put global zen_mode 1"
            9 -> "settings put global zen_mode 2"
            10 -> "reboot recovery"
            11 -> "reboot bootloader"
            12, 13, 14 -> "settings put ${when(record.descriptor.number) { 12 -> "global"; 13 -> "secure"; else -> "system" }} ${quoted(0)} ${quoted(1)}"
            15 -> "pm clear ${pkg()}"
            16 -> "pm uninstall ${pkg()}"
            17 -> "input tap ${number(0)} ${number(1)}"
            18 -> "input swipe ${number(0)} ${number(1)} ${number(2)} ${number(3)} ${record.arg(4, "300").toInt().also { require(it in 1..60_000) }}"
            19 -> "input text ${quoted(0)}"
            else -> "settings put global airplane_mode_on ${if (record.bool(0)) 1 else 0}" +
                " && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${record.bool(0)}"
        }
        val result = shell.run(command)
        return if (result.exitCode == 0) TaskResult.Success else TaskResult.Failure(
            "Root command exited ${result.exitCode}: ${result.stderr.take(512)}",
        )
    }
}
