package com.nfcraft.core.model

/**
 * High-level NDEF record representation decoded from / encoded to raw records.
 * Covers the record families offered by the analyzed record builders.
 */
sealed interface NdefRecordModel {
    data class Uri(val uri: String) : NdefRecordModel
    data class Text(val text: String, val languageCode: String = "en") : NdefRecordModel
    data class Wifi(
        val ssid: String,
        val password: String,
        val authType: String = "WPA2PSK",
        val encryption: String = "AES",
        val hidden: Boolean = false,
    ) : NdefRecordModel
    data class Bluetooth(val mac: String, val name: String? = null) : NdefRecordModel
    data class Contact(val vcard: String) : NdefRecordModel
    data class Email(val address: String, val subject: String = "", val body: String = "") : NdefRecordModel
    data class Phone(val number: String) : NdefRecordModel
    data class Sms(val number: String, val body: String) : NdefRecordModel
    data class Geo(val latitude: Double, val longitude: Double) : NdefRecordModel
    data class Application(val packageName: String) : NdefRecordModel
    data class MimeData(val mimeType: String, val payload: ByteArray) : NdefRecordModel
    data class External(val domainType: String, val payload: ByteArray) : NdefRecordModel
    data object Empty : NdefRecordModel
    data class Unknown(val tnf: Short, val type: ByteArray, val payload: ByteArray) : NdefRecordModel
}

/** A decoded message plus metadata used by the UI. */
data class NdefData(
    val records: List<NdefRecordModel>,
    val byteSize: Int,
    val maxSize: Int,
    val writable: Boolean,
    val canMakeReadOnly: Boolean,
)
