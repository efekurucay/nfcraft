package com.nfcraft.app.tasks

import com.nfcraft.core.model.TaskDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges a suspending [com.nfcraft.feature.tasks.DangerousTaskGuard] to a UI
 * confirmation dialog: the guard posts a [pending] request and suspends until
 * the dialog resolves it. One request is outstanding at a time.
 */
class TaskConfirmationController {
    data class Request(val descriptor: TaskDescriptor, val deferred: CompletableDeferred<Boolean>)

    private val _pending = MutableStateFlow<Request?>(null)
    val pending: StateFlow<Request?> = _pending.asStateFlow()

    suspend fun requestConfirmation(descriptor: TaskDescriptor): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        _pending.value = Request(descriptor, deferred)
        return try {
            deferred.await()
        } finally {
            _pending.value = null
        }
    }

    fun resolve(approved: Boolean) {
        _pending.value?.deferred?.complete(approved)
    }
}
