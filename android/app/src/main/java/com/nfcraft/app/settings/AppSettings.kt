package com.nfcraft.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent app settings (SharedPreferences). Governs whether dangerous/root
 * tasks may run and the optional per-task allow-list.
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("nfcraft", Context.MODE_PRIVATE)

    private val _dangerousEnabled = MutableStateFlow(prefs.getBoolean(KEY_DANGEROUS, false))
    val dangerousEnabled: StateFlow<Boolean> = _dangerousEnabled.asStateFlow()

    private val _whitelist = MutableStateFlow(prefs.getStringSet(KEY_WHITELIST, emptySet())!!.toSet())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    fun setDangerousEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DANGEROUS, enabled).apply()
        _dangerousEnabled.value = enabled
    }

    fun setWhitelist(mimeTypes: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELIST, mimeTypes).apply()
        _whitelist.value = mimeTypes
    }

    private companion object {
        const val KEY_DANGEROUS = "dangerous_tasks_enabled"
        const val KEY_WHITELIST = "task_whitelist"
    }
}
