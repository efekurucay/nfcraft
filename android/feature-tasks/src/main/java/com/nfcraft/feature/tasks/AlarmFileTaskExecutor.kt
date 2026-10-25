package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.nfcraft.core.model.*

class AlarmFileTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.ALARM_FILE, setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 24), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult = when (record.descriptor.number) {
        1 -> {
            require(record.int(0) in 0..23 && record.int(1) in 0..59) { "Invalid alarm time" }
            context.launch(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, record.int(0))
                .putExtra(AlarmClock.EXTRA_MINUTES, record.int(1)).putExtra(AlarmClock.EXTRA_MESSAGE, record.arg(2)))
        }
        2 -> {
            require(record.int(0) > 0) { "Timer must be positive" }
            context.launch(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, record.int(0))
                .putExtra(AlarmClock.EXTRA_MESSAGE, record.arg(1)))
        }
        3 -> context.launch(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        4 -> context.view(record.args[0], record.arg(1, "*/*"))
        5 -> {
            val uri = Uri.parse(record.args[0])
            require(uri.scheme == "content") { "Sharing requires a content URI" }
            context.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(record.arg(1, "*/*"))
                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share file"))
        }
        6 -> context.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).setType(record.arg(0, "*/*")).addCategory(Intent.CATEGORY_OPENABLE))
        7 -> context.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).setType(record.arg(1, "application/octet-stream"))
            .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, record.args[0]))
        8 -> context.launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        9 -> context.launch(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, record.args[0])
            record.arg(1).toLongOrNull()?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
            record.arg(2).toLongOrNull()?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
        })
        else -> context.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }
}
