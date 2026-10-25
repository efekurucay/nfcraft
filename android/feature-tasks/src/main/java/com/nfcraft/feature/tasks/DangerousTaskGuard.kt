package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskDescriptor

/** Implement with an explicit user confirmation UI. Confirmation applies to one execution. */
fun interface DangerousTaskGuard {
    suspend fun confirm(descriptor: TaskDescriptor): Boolean

    companion object {
        val Deny = DangerousTaskGuard { false }
    }
}
