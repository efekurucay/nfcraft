package com.nfcraft.core.model

/** Uniform result for all NFC operations. */
sealed interface NfcResult<out T> {
    data class Success<T>(val value: T) : NfcResult<T>
    data class Failure(val code: Int, val message: String) : NfcResult<Nothing>

    companion object {
        inline fun <T> of(block: () -> T): NfcResult<T> =
            try {
                Success(block())
            } catch (e: NfcException) {
                Failure(e.code, e.message ?: "NFC error")
            } catch (e: Exception) {
                Failure(NfcException.IO_ERROR, e.message ?: "NFC error")
            }
    }
}

inline fun <T> NfcResult<T>.onSuccess(block: (T) -> Unit): NfcResult<T> {
    if (this is NfcResult.Success) block(value)
    return this
}

inline fun <T> NfcResult<T>.onFailure(block: (Int, String) -> Unit): NfcResult<T> {
    if (this is NfcResult.Failure) block(code, message)
    return this
}
