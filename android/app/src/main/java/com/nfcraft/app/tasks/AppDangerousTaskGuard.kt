package com.nfcraft.app.tasks

import com.nfcraft.app.settings.AppSettings
import com.nfcraft.core.model.TaskDescriptor
import com.nfcraft.feature.tasks.DangerousTaskGuard

/**
 * Authorizes dangerous/root tasks only when the user has opted in (Settings),
 * the task passes the optional allow-list, and the user confirms this specific
 * run in a dialog. Deny-by-default at every layer.
 */
class AppDangerousTaskGuard(
    private val settings: AppSettings,
    private val confirmations: TaskConfirmationController,
) : DangerousTaskGuard {
    override suspend fun confirm(descriptor: TaskDescriptor): Boolean {
        if (!settings.dangerousEnabled.value) return false
        val whitelist = settings.whitelist.value
        if (whitelist.isNotEmpty() && descriptor.mimeType !in whitelist) return false
        return confirmations.requestConfirmation(descriptor)
    }
}
