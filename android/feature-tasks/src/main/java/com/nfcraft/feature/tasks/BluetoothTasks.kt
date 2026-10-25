package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object BluetoothTasks {
    val catalog = listOf(
        task(TaskCategory.BLUETOOTH, 1, "Bluetooth enabled", "enabled:BOOLEAN"),
        task(TaskCategory.BLUETOOTH, 2, "Bluetooth settings", ""),
        task(TaskCategory.BLUETOOTH, 3, "Bluetooth discoverable", "seconds:NUMBER?"),
        task(TaskCategory.BLUETOOTH, 4, "Request Bluetooth enable", ""),
        task(TaskCategory.BLUETOOTH, 5, "Pair Bluetooth device", "address:TEXT"),
        task(TaskCategory.BLUETOOTH, 6, "Unpair Bluetooth device", "address:TEXT", dangerous = true),
        task(TaskCategory.BLUETOOTH, 7, "Connect audio device", "address:TEXT"),
        task(TaskCategory.BLUETOOTH, 8, "Disconnect audio device", "address:TEXT"),
        task(TaskCategory.BLUETOOTH, 9, "Bluetooth tethering", "enabled:BOOLEAN"),
        task(TaskCategory.BLUETOOTH, 10, "Send file over Bluetooth", "uri:FILE"),
        task(TaskCategory.BLUETOOTH, 11, "Bluetooth scan", ""),
        task(TaskCategory.BLUETOOTH, 12, "Bluetooth device name", "name:TEXT"),
        task(TaskCategory.BLUETOOTH, 13, "Bluetooth audio settings", ""),
        task(TaskCategory.BLUETOOTH, 14, "Connect input device", "address:TEXT"),
        task(TaskCategory.BLUETOOTH, 15, "Disconnect input device", "address:TEXT"),
        task(TaskCategory.BLUETOOTH, 16, "Bluetooth battery status", "address:TEXT"),
    )
}
