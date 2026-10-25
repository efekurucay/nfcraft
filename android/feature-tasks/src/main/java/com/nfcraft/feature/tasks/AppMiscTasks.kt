package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object AppMiscTasks {
    val catalog = listOf(
        task(TaskCategory.APP_MISC, 1, "Launch app", "package:PACKAGE"),
        task(TaskCategory.APP_MISC, 2, "Start navigation", "latitude:NUMBER longitude:NUMBER label:TEXT?"),
        task(TaskCategory.APP_MISC, 3, "Show toast", "text:TEXT"),
        task(TaskCategory.APP_MISC, 4, "Delay", "milliseconds:NUMBER"),
        task(TaskCategory.APP_MISC, 5, "Share text", "text:TEXT"),
        task(TaskCategory.APP_MISC, 6, "Search web", "query:TEXT"),
        task(TaskCategory.APP_MISC, 7, "Open app details", "package:PACKAGE"),
        task(TaskCategory.APP_MISC, 8, "Open app store", "package:PACKAGE"),
        task(TaskCategory.APP_MISC, 9, "Open map", "latitude:NUMBER longitude:NUMBER label:TEXT?"),
        task(TaskCategory.APP_MISC, 10, "Go home", ""),
        task(TaskCategory.APP_MISC, 11, "Copy text", "text:TEXT"),
        task(TaskCategory.APP_MISC, 12, "Open camera", ""),
        task(TaskCategory.APP_MISC, 13, "Capture video", ""),
        task(TaskCategory.APP_MISC, 14, "Open image gallery", ""),
        task(TaskCategory.APP_MISC, 15, "Uninstall app", "package:PACKAGE", dangerous = true),
        task(TaskCategory.APP_MISC, 16, "Launch explicit activity", "package:PACKAGE class:TEXT"),
        task(TaskCategory.APP_MISC, 17, "Send broadcast", "action:TEXT", dangerous = true),
        task(TaskCategory.APP_MISC, 18, "Start service", "package:PACKAGE class:TEXT", dangerous = true),
        task(TaskCategory.APP_MISC, 19, "Stop service", "package:PACKAGE class:TEXT", dangerous = true),
        task(TaskCategory.APP_MISC, 20, "Show notification", "title:TEXT text:TEXT"),
        task(TaskCategory.APP_MISC, 21, "Read clipboard", ""),
        task(TaskCategory.APP_MISC, 22, "Run shortcut", "id:TEXT package:PACKAGE"),
        task(TaskCategory.APP_MISC, 23, "Open voice search", ""),
        task(TaskCategory.APP_MISC, 24, "Open calculator", ""),
        task(TaskCategory.APP_MISC, 25, "Scan barcode", ""),
        task(TaskCategory.APP_MISC, 26, "Send intent", "uri:TEXT", dangerous = true),
        task(TaskCategory.APP_MISC, 27, "Abort sequence", ""),
        task(TaskCategory.APP_MISC, 28, "Open document", "uri:FILE mime:TEXT?"),
    )
}
