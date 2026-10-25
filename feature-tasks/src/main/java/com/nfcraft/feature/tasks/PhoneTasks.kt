package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object PhoneTasks {
    val catalog = listOf(
        task(TaskCategory.PHONE, 1, "Dial number", "number:TEXT"),
        task(TaskCategory.PHONE, 2, "Compose SMS", "number:TEXT message:TEXT?"),
        task(TaskCategory.PHONE, 3, "Compose email", "address:TEXT subject:TEXT? body:TEXT?"),
        task(TaskCategory.PHONE, 4, "Open contact", "uri:TEXT"),
        task(TaskCategory.PHONE, 5, "Insert contact", "name:TEXT number:TEXT? email:TEXT?"),
        task(TaskCategory.PHONE, 6, "Call settings", ""),
        task(TaskCategory.PHONE, 7, "Compose MMS", "number:TEXT uri:FILE?"),
        task(TaskCategory.PHONE, 8, "Place call", "number:TEXT"),
        task(TaskCategory.PHONE, 9, "Send SMS", "number:TEXT message:TEXT"),
        task(TaskCategory.PHONE, 10, "Call forwarding", "number:TEXT enabled:BOOLEAN"),
        task(TaskCategory.PHONE, 11, "Call voicemail", ""),
        task(TaskCategory.PHONE, 12, "Open call log", ""),
        task(TaskCategory.PHONE, 13, "Open contacts", ""),
        task(TaskCategory.PHONE, 14, "SIM settings", ""),
        task(TaskCategory.PHONE, 15, "Mobile network settings", ""),
        task(TaskCategory.PHONE, 16, "Answer call", ""),
        task(TaskCategory.PHONE, 17, "End call", ""),
        task(TaskCategory.PHONE, 18, "Speakerphone", "enabled:BOOLEAN"),
        task(TaskCategory.PHONE, 19, "Microphone mute", "enabled:BOOLEAN"),
        task(TaskCategory.PHONE, 20, "Phone account settings", ""),
    )
}
