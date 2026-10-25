package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import com.nfcraft.core.model.*

class PhoneTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.PHONE, setOf(1, 2, 3, 4, 5, 6, 12, 13, 15), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult = context.launch(
        when (record.descriptor.number) {
            1 -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", record.args[0], null))
            2 -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", record.args[0], null)).putExtra("sms_body", record.arg(1))
            3 -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(record.args[0], "@,.")}"))
                .putExtra(Intent.EXTRA_SUBJECT, record.arg(1)).putExtra(Intent.EXTRA_TEXT, record.arg(2))
            4 -> Intent(Intent.ACTION_VIEW, Uri.parse(record.args[0]))
            5 -> Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                .putExtra(ContactsContract.Intents.Insert.NAME, record.args[0])
                .putExtra(ContactsContract.Intents.Insert.PHONE, record.arg(1))
                .putExtra(ContactsContract.Intents.Insert.EMAIL, record.arg(2))
            6 -> Intent("android.settings.CALL_SETTINGS")
            12 -> Intent(Intent.ACTION_VIEW, Uri.parse("content://call_log/calls"))
            13 -> Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            else -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        },
    )
}
