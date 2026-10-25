package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.nfcraft.core.model.*

class NetworkTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.NETWORK, setOf(1, 2, 3, 10, 11, 12, 18, 19), guard) {
    @Suppress("DEPRECATION")
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult = when (record.descriptor.number) {
        1 -> {
            val uri = Uri.parse(record.args[0])
            require(uri.scheme?.lowercase() in setOf("http", "https")) { "Expected HTTP(S) URL" }
            context.view(uri.toString())
        }
        2 -> {
            if (Build.VERSION.SDK_INT >= 29) TaskResult.Failure("WiFi changes require the system WiFi panel on this device")
            else if ((context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                    .setWifiEnabled(record.bool(0))) TaskResult.Success
            else TaskResult.Failure("WiFi change rejected")
        }
        else -> context.launch(Intent(when (record.descriptor.number) {
            3, 12 -> Settings.ACTION_WIFI_SETTINGS
            10 -> Settings.ACTION_WIRELESS_SETTINGS
            11 -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS
            18 -> "android.settings.VPN_SETTINGS"
            else -> Settings.ACTION_DATA_ROAMING_SETTINGS
        }))
    }
}
