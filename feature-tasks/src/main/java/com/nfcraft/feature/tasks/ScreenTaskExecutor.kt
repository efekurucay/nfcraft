package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import com.nfcraft.core.model.*

class ScreenTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.SCREEN, ((1..12) + 18).toSet(), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        val number = record.descriptor.number
        if (number in setOf(1, 2, 5, 6, 7)) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(context))
                return TaskResult.Failure("System settings write access has not been granted")
            val (key, value) = when (number) {
                1 -> Settings.System.SCREEN_BRIGHTNESS to record.int(0).also { require(it in 0..255) { "Brightness must be 0..255" } }
                2 -> Settings.System.SCREEN_BRIGHTNESS_MODE to if (record.bool(0)) 1 else 0
                5 -> Settings.System.ACCELEROMETER_ROTATION to if (record.bool(0)) 1 else 0
                6 -> Settings.System.SCREEN_OFF_TIMEOUT to record.int(0).also { require(it > 0) { "Timeout must be positive" } }
                else -> Settings.System.USER_ROTATION to when (record.int(0)) {
                    0 -> 0; 90 -> 1; 180 -> 2; 270 -> 3
                    else -> return TaskResult.Failure("Rotation must be 0, 90, 180 or 270")
                }
            }
            return if (Settings.System.putInt(context.contentResolver, key, value)) TaskResult.Success
            else TaskResult.Failure("System setting rejected")
        }
        return when (number) {
            4 -> {
                if (Build.VERSION.SDK_INT < 23) return TaskResult.Failure("Flashlight requires Android 6 or higher")
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val id = manager.cameraIdList.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return TaskResult.Failure("Flashlight unavailable")
                manager.setTorchMode(id, record.bool(0))
                TaskResult.Success
            }
            8 -> context.launch(Intent(Intent.ACTION_SET_WALLPAPER))
            18 -> context.view(record.args[0], "image/*")
            else -> context.launch(Intent(when (number) {
                3 -> Settings.ACTION_DISPLAY_SETTINGS
                9 -> Settings.ACTION_SECURITY_SETTINGS
                10 -> Settings.ACTION_DREAM_SETTINGS
                11 -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                else -> Settings.ACTION_CAST_SETTINGS
            }))
        }
    }
}
