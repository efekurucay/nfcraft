package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object ConfigTasks {
    val catalog = listOf(
        task(TaskCategory.CONFIG, 1, "Open settings", ""),
        task(TaskCategory.CONFIG, 2, "Application settings", ""),
        task(TaskCategory.CONFIG, 3, "Location settings", ""),
        task(TaskCategory.CONFIG, 4, "Security settings", ""),
        task(TaskCategory.CONFIG, 5, "Date and time settings", ""),
        task(TaskCategory.CONFIG, 6, "Locale settings", ""),
        task(TaskCategory.CONFIG, 7, "Input method settings", ""),
        task(TaskCategory.CONFIG, 8, "Battery saver settings", ""),
        task(TaskCategory.CONFIG, 9, "Device information", ""),
        task(TaskCategory.CONFIG, 10, "Manage write settings permission", ""),
        task(TaskCategory.CONFIG, 11, "Notification policy settings", ""),
        task(TaskCategory.CONFIG, 12, "Application notification settings", "package:PACKAGE"),
        task(TaskCategory.CONFIG, 13, "Ignore battery optimization settings", ""),
        task(TaskCategory.CONFIG, 14, "Storage settings", ""),
        task(TaskCategory.CONFIG, 15, "Privacy settings", ""),
        task(TaskCategory.CONFIG, 16, "Account settings", ""),
        task(TaskCategory.CONFIG, 17, "Sync settings", ""),
        task(TaskCategory.CONFIG, 18, "NFC settings", ""),
        task(TaskCategory.CONFIG, 19, "Developer settings", ""),
        task(TaskCategory.CONFIG, 20, "Usage access settings", ""),
        task(TaskCategory.CONFIG, 21, "Notification listener settings", ""),
        task(TaskCategory.CONFIG, 22, "Set time zone", "zone:TEXT"),
        task(TaskCategory.CONFIG, 23, "Set system locale", "locale:TEXT"),
        task(TaskCategory.CONFIG, 24, "Backup settings", ""),
    )
}
