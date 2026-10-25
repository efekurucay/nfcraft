package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nfcraft.core.model.*

class ConfigTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.CONFIG, (1..21).toSet(), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        val action = when (record.descriptor.number) {
            1 -> "SETTINGS"
            2 -> "MANAGE_APPLICATIONS_SETTINGS"
            3 -> "LOCATION_SOURCE_SETTINGS"
            4 -> "SECURITY_SETTINGS"
            5 -> "DATE_SETTINGS"
            6 -> "LOCALE_SETTINGS"
            7 -> "INPUT_METHOD_SETTINGS"
            8 -> "BATTERY_SAVER_SETTINGS"
            9 -> "DEVICE_INFO_SETTINGS"
            10 -> "action.MANAGE_WRITE_SETTINGS"
            11 -> "NOTIFICATION_POLICY_ACCESS_SETTINGS"
            12 -> "APP_NOTIFICATION_SETTINGS"
            13 -> "IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
            14 -> "INTERNAL_STORAGE_SETTINGS"
            15 -> "PRIVACY_SETTINGS"
            16 -> "ADD_ACCOUNT_SETTINGS"
            17 -> "SYNC_SETTINGS"
            18 -> "NFC_SETTINGS"
            19 -> "APPLICATION_DEVELOPMENT_SETTINGS"
            20 -> "USAGE_ACCESS_SETTINGS"
            21 -> "ACTION_NOTIFICATION_LISTENER_SETTINGS"
            else -> "PRIVACY_SETTINGS"
        }
        return context.launch(Intent("android.settings.$action").apply {
            if (record.descriptor.number == 10) data = Uri.parse("package:${context.packageName}")
            if (record.descriptor.number == 12) putExtra("android.provider.extra.APP_PACKAGE", record.args[0])
        })
    }
}
