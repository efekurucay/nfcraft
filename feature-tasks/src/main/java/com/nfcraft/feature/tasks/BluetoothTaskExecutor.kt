package com.nfcraft.feature.tasks

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.nfcraft.core.model.*

class BluetoothTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.BLUETOOTH, setOf(1, 2, 3, 4, 13), guard) {
    @Suppress("DEPRECATION")
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        return when (record.descriptor.number) {
            1 -> {
                if (Build.VERSION.SDK_INT >= 33) return TaskResult.Failure("Use Bluetooth settings to change Bluetooth on this device")
                val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                    ?: return TaskResult.Failure("Bluetooth unavailable")
                val accepted = if (record.bool(0)) adapter.enable() else adapter.disable()
                if (accepted) TaskResult.Success else TaskResult.Failure("Bluetooth change rejected")
            }
            2, 13 -> context.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            3 -> context.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, record.arg(0, "120").toInt().coerceIn(1, 300)))
            else -> context.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
