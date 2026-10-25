package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nfcraft.core.model.TaskRecord
import com.nfcraft.core.model.TaskResult

internal fun Context.launch(intent: Intent): TaskResult {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    return TaskResult.Success
}
internal fun Context.view(uri: String, mime: String? = null): TaskResult = launch(
    Intent(Intent.ACTION_VIEW).apply {
        if (mime == null) data = Uri.parse(uri) else setDataAndType(Uri.parse(uri), mime)
        if (uri.startsWith("content:")) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    },
)
internal fun TaskRecord.arg(index: Int, default: String = "") = args.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: default
internal fun TaskRecord.int(index: Int) = args[index].toInt()
internal fun TaskRecord.bool(index: Int) = args[index].toBooleanStrict()
internal fun geo(record: TaskRecord, navigation: Boolean): String {
    val lat = record.args[0].toDouble()
    val lon = record.args[1].toDouble()
    require(lat in -90.0..90.0 && lon in -180.0..180.0) { "Coordinates out of range" }
    val point = "$lat,$lon"
    return if (navigation) "google.navigation:q=$point" else
        "geo:$point?q=${Uri.encode(point + record.arg(2).takeIf { it.isNotEmpty() }?.let { "($it)" }.orEmpty())}"
}
