package com.nfcraft.feature.tasks

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.nfcraft.core.model.*
import java.util.Calendar

/** Else/end are structural markers; use TaskSequenceRunner to execute a sequence. */
class ConditionTaskExecutor : GuardedTaskExecutor(TaskCategory.CONDITION, (1..20).toSet()) {
    @Suppress("DEPRECATION")
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        fun battery() = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: error("Battery state unavailable")
        val taken = when (record.descriptor.number) {
            1 -> true
            2 -> false
            3 -> record.bool(0)
            4 -> record.args[0] == record.args[1]
            5 -> record.args[0] != record.args[1]
            6 -> record.args[0].toDouble() > record.args[1].toDouble()
            7 -> record.args[0].toDouble() < record.args[1].toDouble()
            8 -> record.args[0].contains(record.args[1])
            9 -> (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
            10 -> (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true
            11 -> (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnected == true
            12 -> battery().getIntExtra(BatteryManager.EXTRA_STATUS, -1) in setOf(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL)
            13 -> {
                val minimum = record.int(0)
                require(minimum in 0..100) { "Battery percentage must be 0..100" }
                val state = battery()
                val level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = state.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                require(level >= 0 && scale > 0) { "Battery level unavailable" }
                level.toDouble() * 100 / scale >= minimum
            }
            14 -> try {
                context.packageManager.getApplicationInfo(record.args[0], 0)
                true
            } catch (_: PackageManager.NameNotFoundException) { false }
            15 -> {
                val start = record.int(0); val end = record.int(1)
                require(start in 0..23 && end in 0..23) { "Hour must be 0..23" }
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (start <= end) hour in start..end else hour >= start || hour <= end
            }
            16 -> {
                val mode = when (record.args[0].lowercase()) {
                    "normal" -> AudioManager.RINGER_MODE_NORMAL
                    "silent" -> AudioManager.RINGER_MODE_SILENT
                    "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                    else -> error("Expected normal, silent or vibrate")
                }
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode == mode
            }
            17 -> (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
            18 -> Settings.System.getString(context.contentResolver, record.args[0]) == record.args[1]
            else -> true
        }
        return TaskResult.ConditionBranch(taken)
    }
}
