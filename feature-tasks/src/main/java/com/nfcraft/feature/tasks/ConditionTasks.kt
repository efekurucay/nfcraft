package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object ConditionTasks {
    val catalog = listOf(
        task(TaskCategory.CONDITION, 1, "If true", ""),
        task(TaskCategory.CONDITION, 2, "If false", ""),
        task(TaskCategory.CONDITION, 3, "If boolean", "value:BOOLEAN"),
        task(TaskCategory.CONDITION, 4, "If text equals", "left:TEXT right:TEXT"),
        task(TaskCategory.CONDITION, 5, "If text differs", "left:TEXT right:TEXT"),
        task(TaskCategory.CONDITION, 6, "If number greater", "left:NUMBER right:NUMBER"),
        task(TaskCategory.CONDITION, 7, "If number less", "left:NUMBER right:NUMBER"),
        task(TaskCategory.CONDITION, 8, "If text contains", "text:TEXT value:TEXT"),
        task(TaskCategory.CONDITION, 9, "If WiFi enabled", ""),
        task(TaskCategory.CONDITION, 10, "If Bluetooth enabled", ""),
        task(TaskCategory.CONDITION, 11, "If network connected", ""),
        task(TaskCategory.CONDITION, 12, "If charging", ""),
        task(TaskCategory.CONDITION, 13, "If battery at least", "percent:NUMBER"),
        task(TaskCategory.CONDITION, 14, "If app installed", "package:PACKAGE"),
        task(TaskCategory.CONDITION, 15, "If hour in range", "start:NUMBER end:NUMBER"),
        task(TaskCategory.CONDITION, 16, "If ringer mode", "mode:ENUM"),
        task(TaskCategory.CONDITION, 17, "If screen interactive", ""),
        task(TaskCategory.CONDITION, 18, "If system setting equals", "name:TEXT value:TEXT"),
        task(TaskCategory.CONDITION, 19, "Else", ""),
        task(TaskCategory.CONDITION, 20, "End if", ""),
    )
}
