package com.nfcraft.core.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle

/**
 * Foreground tag acquisition. Prefers reader mode (all tech flags, no platform
 * sounds, skip NDEF check) and falls back to foreground dispatch on older
 * devices. Debounces repeat reads of the same tag.
 */
class TagReader(private val activity: Activity) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcSupported: Boolean get() = adapter != null
    val isNfcEnabled: Boolean get() = adapter?.isEnabled == true

    fun enable(onTag: (Tag) -> Unit) {
        val a = adapter ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val options = Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY)
            }
            a.enableReaderMode(activity, { tag -> onTag(tag) }, READER_FLAGS, options)
        } else {
            enableForegroundDispatch()
        }
    }

    fun disable() {
        val a = adapter ?: return
        a.disableReaderMode(activity)
        runCatching { a.disableForegroundDispatch(activity) }
    }

    /** Debounce helper: ignore the same tag for [DEBOUNCE_MS] after a read. */
    fun ignore(tag: Tag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            adapter?.ignore(tag, DEBOUNCE_MS, null, null)
        }
    }

    private fun enableForegroundDispatch() {
        val a = adapter ?: return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pending = PendingIntent.getActivity(activity, 0, intent, flags)
        a.enableForegroundDispatch(activity, pending, null, null)
    }

    private companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        const val PRESENCE_CHECK_DELAY = 250
        const val DEBOUNCE_MS = 1000
    }
}
