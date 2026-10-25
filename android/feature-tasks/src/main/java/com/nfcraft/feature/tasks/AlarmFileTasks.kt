package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object AlarmFileTasks {
    val catalog = listOf(
        task(TaskCategory.ALARM_FILE, 1, "Set alarm", "hour:NUMBER minute:NUMBER label:TEXT?"),
        task(TaskCategory.ALARM_FILE, 2, "Start timer", "seconds:NUMBER label:TEXT?"),
        task(TaskCategory.ALARM_FILE, 3, "Show alarms", ""),
        task(TaskCategory.ALARM_FILE, 4, "Open file", "uri:FILE mime:TEXT?"),
        task(TaskCategory.ALARM_FILE, 5, "Share file", "uri:FILE mime:TEXT?"),
        task(TaskCategory.ALARM_FILE, 6, "Choose file", "mime:TEXT?"),
        task(TaskCategory.ALARM_FILE, 7, "Create document", "name:TEXT mime:TEXT?"),
        task(TaskCategory.ALARM_FILE, 8, "Open calendar", ""),
        task(TaskCategory.ALARM_FILE, 9, "Insert calendar event", "title:TEXT beginMillis:NUMBER? endMillis:NUMBER?"),
        task(TaskCategory.ALARM_FILE, 10, "Dismiss alarm", ""),
        task(TaskCategory.ALARM_FILE, 11, "Snooze alarm", ""),
        task(TaskCategory.ALARM_FILE, 12, "Show timers", ""),
        task(TaskCategory.ALARM_FILE, 13, "Delete file", "uri:FILE", dangerous = true),
        task(TaskCategory.ALARM_FILE, 14, "Copy file", "source:FILE destination:FILE"),
        task(TaskCategory.ALARM_FILE, 15, "Move file", "source:FILE destination:FILE", dangerous = true),
        task(TaskCategory.ALARM_FILE, 16, "Rename file", "uri:FILE name:TEXT", dangerous = true),
        task(TaskCategory.ALARM_FILE, 17, "Create directory", "uri:FILE name:TEXT"),
        task(TaskCategory.ALARM_FILE, 18, "Read text file", "uri:FILE"),
        task(TaskCategory.ALARM_FILE, 19, "Write text file", "uri:FILE text:TEXT", dangerous = true),
        task(TaskCategory.ALARM_FILE, 20, "Append text file", "uri:FILE text:TEXT", dangerous = true),
        task(TaskCategory.ALARM_FILE, 21, "Download file", "url:TEXT destination:FILE"),
        task(TaskCategory.ALARM_FILE, 22, "Zip files", "source:FILE destination:FILE"),
        task(TaskCategory.ALARM_FILE, 23, "Extract archive", "source:FILE destination:FILE"),
        task(TaskCategory.ALARM_FILE, 24, "Open document tree", ""),
    )
}
