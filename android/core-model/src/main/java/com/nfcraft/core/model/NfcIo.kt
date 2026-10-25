package com.nfcraft.core.model

/**
 * Low-level transceive abstraction over a connected tag technology.
 *
 * Mirrors the "NFCIOHelper" role from the analyzed suite: a single wrapper that
 * exposes raw command exchange across whichever [android.nfc.tech] class is
 * connected, with reconnect-on-TagLost handling owned by the implementation.
 *
 * All calls are blocking and must run off the main thread. Implementations are
 * tied to one tag session; a new tag means a new instance.
 */
interface NfcIo {

    /** Tag UID as discovered. */
    val uid: ByteArray

    /** Android tech names present on the tag (e.g. "android.nfc.tech.NfcA"). */
    val techList: List<String>

    /** 2-byte SENS_RES (ATQA) from NfcA, or null when the tech is absent. */
    val atqa: ByteArray?

    /** 1-byte SEL_RES (SAK) from NfcA, or null when the tech is absent. */
    val sak: Byte?

    /** IsoDep ATS / historical bytes, or null when unavailable. */
    val historicalBytes: ByteArray?

    /** True once a transceive-capable technology is connected. */
    val isConnected: Boolean

    /** Maximum transceive length for the connected technology, or 0 if unknown. */
    val maxTransceiveLength: Int

    /** Connect the preferred technology. Idempotent. */
    fun connect()

    /** Close the current technology connection. */
    fun close()

    /**
     * Exchange a raw command and return the response.
     *
     * On [android.nfc.TagLostException] the implementation attempts one
     * reconnect + retry before surfacing [NfcException].
     */
    @Throws(NfcException::class)
    fun transceive(command: ByteArray): ByteArray

    /**
     * Request a specific technology for subsequent [transceive] calls, e.g.
     * switching between IsoDep and NfcA. Returns true if available & connected.
     */
    fun useTechnology(techName: String): Boolean
}

/** Raised for any IO failure after the implementation's own retry policy. */
class NfcException(
    val code: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val TAG_LOST = -6
        const val MESSAGE_TOO_LARGE = -9
        const val WRITE_FAILED = -10
        const val NOT_WRITABLE = -12
        const val FORMAT_FAILED = -14
        const val IO_ERROR = -1
    }
}
