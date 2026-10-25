package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object SoundTasks {
    val catalog = listOf(
        task(TaskCategory.SOUND, 1, "Set ringer mode", "mode:ENUM"),
        task(TaskCategory.SOUND, 2, "Set ring volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 3, "Set media volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 4, "Set alarm volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 5, "Set notification volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 6, "Set call volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 7, "Set system volume", "volume:NUMBER"),
        task(TaskCategory.SOUND, 8, "Vibrate", "milliseconds:NUMBER"),
        task(TaskCategory.SOUND, 9, "Stop vibration", ""),
        task(TaskCategory.SOUND, 10, "Sound settings", ""),
        task(TaskCategory.SOUND, 11, "Play media", "uri:TEXT mime:TEXT?"),
        task(TaskCategory.SOUND, 12, "Play default ringtone", ""),
        task(TaskCategory.SOUND, 13, "Mute media", ""),
        task(TaskCategory.SOUND, 14, "Unmute media", ""),
        task(TaskCategory.SOUND, 15, "Raise media volume", ""),
        task(TaskCategory.SOUND, 16, "Lower media volume", ""),
        task(TaskCategory.SOUND, 17, "Audio effects settings", ""),
        task(TaskCategory.SOUND, 18, "Do not disturb settings", ""),
        task(TaskCategory.SOUND, 19, "Set ringtone", "uri:FILE"),
        task(TaskCategory.SOUND, 20, "Set notification tone", "uri:FILE"),
        task(TaskCategory.SOUND, 21, "Text to speech", "text:TEXT language:TEXT?"),
        task(TaskCategory.SOUND, 22, "Media play", ""),
        task(TaskCategory.SOUND, 23, "Media pause", ""),
        task(TaskCategory.SOUND, 24, "Media next", ""),
    )
}
