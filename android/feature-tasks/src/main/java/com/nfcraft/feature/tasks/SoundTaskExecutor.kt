package com.nfcraft.feature.tasks

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.nfcraft.core.model.*

class SoundTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.SOUND, ((1..11) + (13..18)).toSet(), guard) {
    @Suppress("DEPRECATION")
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (val number = record.descriptor.number) {
            1 -> audio.ringerMode = when (record.args[0].lowercase()) {
                "silent" -> AudioManager.RINGER_MODE_SILENT
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "normal" -> AudioManager.RINGER_MODE_NORMAL
                else -> return TaskResult.Failure("Expected silent, vibrate or normal")
            }
            in 2..7 -> {
                val stream = when (number) {
                    2 -> AudioManager.STREAM_RING
                    3 -> AudioManager.STREAM_MUSIC
                    4 -> AudioManager.STREAM_ALARM
                    5 -> AudioManager.STREAM_NOTIFICATION
                    6 -> AudioManager.STREAM_VOICE_CALL
                    else -> AudioManager.STREAM_SYSTEM
                }
                val volume = record.int(0)
                require(volume in 0..audio.getStreamMaxVolume(stream)) { "Volume out of range" }
                audio.setStreamVolume(stream, volume, 0)
            }
            8 -> {
                val ms = record.args[0].toLong()
                require(ms in 1..60_000) { "Vibration must be 1..60000 milliseconds" }
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (!vibrator.hasVibrator()) return TaskResult.Failure("Vibrator unavailable")
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                else vibrator.vibrate(ms)
            }
            9 -> (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
            10 -> return context.launch(Intent(Settings.ACTION_SOUND_SETTINGS))
            11 -> return context.view(record.args[0], record.arg(1, "audio/*"))
            13, 14 -> {
                if (Build.VERSION.SDK_INT >= 23) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    if (number == 13) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
                else return TaskResult.Failure("Media mute requires Android 6 or higher")
            }
            15, 16 -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                if (number == 15) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
            17 -> return context.launch(Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
                .putExtra("android.media.extra.PACKAGE_NAME", context.packageName))
            18 -> return context.launch(Intent("android.settings.ZEN_MODE_SETTINGS"))
        }
        return TaskResult.Success
    }
}
