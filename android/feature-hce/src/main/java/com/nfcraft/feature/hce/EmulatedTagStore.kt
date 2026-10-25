package com.nfcraft.feature.hce

import com.nfcraft.core.model.NdefRecordModel

/**
 * Process-wide holder for the NDEF message that [NdefHostApduService] presents
 * to an external reader while the device emulates a Type 4 Tag.
 *
 * The app layer sets [records] to control what the emulated tag serves; the
 * service reads the current value whenever a reader selects the NDEF
 * application, so an update takes effect on the next tap. The reference is
 * [Volatile] and every assignment swaps in a fresh immutable list, which keeps
 * reads on the NFC dispatch thread consistent with writes from the UI thread.
 */
object EmulatedTagStore {

    /** URI served when the app layer has not set its own payload. */
    const val DEFAULT_URI: String = "https://wakdev-style-placeholder"

    @Volatile
    var records: List<NdefRecordModel> = listOf(NdefRecordModel.Uri(DEFAULT_URI))
}
