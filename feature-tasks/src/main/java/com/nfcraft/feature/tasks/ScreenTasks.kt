package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object ScreenTasks {
    val catalog = listOf(
        task(TaskCategory.SCREEN, 1, "Set brightness", "brightness:NUMBER"),
        task(TaskCategory.SCREEN, 2, "Set automatic brightness", "enabled:BOOLEAN"),
        task(TaskCategory.SCREEN, 3, "Display settings", ""),
        task(TaskCategory.SCREEN, 4, "Flashlight", "enabled:BOOLEAN"),
        task(TaskCategory.SCREEN, 5, "Rotate automatically", "enabled:BOOLEAN"),
        task(TaskCategory.SCREEN, 6, "Screen timeout", "milliseconds:NUMBER"),
        task(TaskCategory.SCREEN, 7, "Set rotation", "degrees:ENUM"),
        task(TaskCategory.SCREEN, 8, "Wallpaper chooser", ""),
        task(TaskCategory.SCREEN, 9, "Lock screen settings", ""),
        task(TaskCategory.SCREEN, 10, "Dream settings", ""),
        task(TaskCategory.SCREEN, 11, "Accessibility settings", ""),
        task(TaskCategory.SCREEN, 12, "Cast settings", ""),
        task(TaskCategory.SCREEN, 13, "Night display settings", ""),
        task(TaskCategory.SCREEN, 14, "Font size", "scale:NUMBER"),
        task(TaskCategory.SCREEN, 15, "Keep screen awake", "enabled:BOOLEAN"),
        task(TaskCategory.SCREEN, 16, "Lock screen", ""),
        task(TaskCategory.SCREEN, 17, "Wake screen", ""),
        task(TaskCategory.SCREEN, 18, "Show image", "uri:FILE"),
        task(TaskCategory.SCREEN, 19, "Set wallpaper", "uri:FILE"),
        task(TaskCategory.SCREEN, 20, "Screen saver", "enabled:BOOLEAN"),
    )
}
