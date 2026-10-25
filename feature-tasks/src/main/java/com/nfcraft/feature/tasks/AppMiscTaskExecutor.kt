package com.nfcraft.feature.tasks

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.nfcraft.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AppMiscTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.APP_MISC, ((1..16) + listOf(23, 24, 27, 28)).toSet(), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult = when (record.descriptor.number) {
        1 -> context.packageManager.getLaunchIntentForPackage(record.args[0])?.let { context.launch(it) }
            ?: TaskResult.Failure("App unavailable or hidden by package visibility")
        2 -> context.view(geo(record, true))
        3 -> withContext(Dispatchers.Main) { Toast.makeText(context.applicationContext, record.args[0], Toast.LENGTH_LONG).show(); TaskResult.Success }
        4 -> { val ms = record.args[0].toLong(); require(ms in 0..86_400_000) { "Delay out of range" }; delay(ms); TaskResult.Success }
        5 -> context.launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, record.args[0]), "Share text"))
        6 -> context.launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, record.args[0]))
        7 -> context.launch(Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:${record.args[0]}")))
        8 -> context.view("market://details?id=${Uri.encode(record.args[0])}")
        9 -> context.view(geo(record, false))
        10 -> context.launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        11 -> withContext(Dispatchers.Main) {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("NFC task", record.args[0]))
            TaskResult.Success
        }
        12 -> context.launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        13 -> context.launch(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA))
        14 -> context.launch(Intent(Intent.ACTION_VIEW).setType("image/*"))
        15 -> context.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:${record.args[0]}")))
        16 -> context.launch(Intent().setClassName(record.args[0], record.args[1]))
        23 -> context.launch(Intent("android.speech.action.WEB_SEARCH"))
        24 -> context.launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR))
        27 -> TaskResult.Aborted
        else -> context.view(record.args[0], record.arg(1, "*/*"))
    }
}
