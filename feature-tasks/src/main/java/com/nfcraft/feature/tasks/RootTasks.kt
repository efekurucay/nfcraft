package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object RootTasks {
    val catalog = listOf(
        task(TaskCategory.ROOT, 1, "Execute privileged command", "command:TEXT", dangerous = true),
        task(TaskCategory.ROOT, 2, "Reboot", "", dangerous = true),
        task(TaskCategory.ROOT, 3, "Shut down", "", dangerous = true),
        task(TaskCategory.ROOT, 4, "Send key event", "keycode:NUMBER", dangerous = true),
        task(TaskCategory.ROOT, 5, "Force stop app", "package:PACKAGE", dangerous = true),
        task(TaskCategory.ROOT, 6, "Take screenshot", "path:FILE", dangerous = true),
        task(TaskCategory.ROOT, 7, "Do not disturb off", "", dangerous = true),
        task(TaskCategory.ROOT, 8, "Do not disturb priority", "", dangerous = true),
        task(TaskCategory.ROOT, 9, "Do not disturb total silence", "", dangerous = true),
        task(TaskCategory.ROOT, 10, "Reboot recovery", "", dangerous = true),
        task(TaskCategory.ROOT, 11, "Reboot bootloader", "", dangerous = true),
        task(TaskCategory.ROOT, 12, "Set global setting", "name:TEXT value:TEXT", dangerous = true),
        task(TaskCategory.ROOT, 13, "Set secure setting", "name:TEXT value:TEXT", dangerous = true),
        task(TaskCategory.ROOT, 14, "Set system setting", "name:TEXT value:TEXT", dangerous = true),
        task(TaskCategory.ROOT, 15, "Clear app data", "package:PACKAGE", dangerous = true),
        task(TaskCategory.ROOT, 16, "Uninstall package", "package:PACKAGE", dangerous = true),
        task(TaskCategory.ROOT, 17, "Tap screen", "x:NUMBER y:NUMBER", dangerous = true),
        task(TaskCategory.ROOT, 18, "Swipe screen", "x1:NUMBER y1:NUMBER x2:NUMBER y2:NUMBER duration:NUMBER?", dangerous = true),
        task(TaskCategory.ROOT, 19, "Input text", "text:TEXT", dangerous = true),
        task(TaskCategory.ROOT, 20, "Set airplane mode", "enabled:BOOLEAN", dangerous = true),
    )
}
