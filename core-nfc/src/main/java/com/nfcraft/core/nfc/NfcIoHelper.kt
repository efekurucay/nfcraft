package com.nfcraft.core.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.nfc.tech.TagTechnology
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import java.io.IOException

/**
 * [NfcIo] over android.nfc technology classes. Chooses a transceive-capable
 * technology by priority, reconnects once on tag loss, and exposes a uniform
 * raw command interface regardless of the connected class.
 */
class NfcIoHelper(private val tag: Tag) : NfcIo {

    override val uid: ByteArray = tag.id
    override val techList: List<String> = tag.techList.toList()

    override val atqa: ByteArray? = NfcA.get(tag)?.atqa
    override val sak: Byte? = NfcA.get(tag)?.sak?.toByte()
    override val historicalBytes: ByteArray? = IsoDep.get(tag)?.let { it.historicalBytes ?: it.hiLayerResponse }

    private var tech: TagTechnology? = null

    override val isConnected: Boolean
        get() = tech?.isConnected == true

    override val maxTransceiveLength: Int
        get() = when (val t = tech) {
            is IsoDep -> t.maxTransceiveLength
            is NfcA -> t.maxTransceiveLength
            is NfcB -> t.maxTransceiveLength
            is NfcF -> t.maxTransceiveLength
            is NfcV -> t.maxTransceiveLength
            else -> 0
        }

    override fun connect() {
        if (isConnected) return
        tech = openPreferred() ?: throw NfcException(NfcException.IO_ERROR, "No transceive technology")
        connectTech()
    }

    override fun useTechnology(techName: String): Boolean {
        if (techName !in techList) return false
        close()
        tech = openByName(techName) ?: return false
        return try {
            connectTech()
            true
        } catch (e: NfcException) {
            false
        }
    }

    override fun transceive(command: ByteArray): ByteArray {
        if (!isConnected) connect()
        return try {
            rawTransceive(command)
        } catch (e: TagLostException) {
            // One reconnect + retry, mirroring the analyzed helper's policy.
            reconnect()
            try {
                rawTransceive(command)
            } catch (e2: TagLostException) {
                throw NfcException(NfcException.TAG_LOST, "Tag lost", e2)
            } catch (e2: IOException) {
                throw NfcException(NfcException.IO_ERROR, e2.message ?: "IO error", e2)
            }
        } catch (e: IOException) {
            throw NfcException(NfcException.IO_ERROR, e.message ?: "IO error", e)
        }
    }

    override fun close() {
        try {
            tech?.close()
        } catch (_: IOException) {
        }
        tech = null
    }

    private fun reconnect() {
        close()
        connect()
    }

    private fun connectTech() {
        val t = tech ?: throw NfcException(NfcException.IO_ERROR, "No technology to connect")
        try {
            t.connect()
        } catch (e: IOException) {
            throw NfcException(NfcException.IO_ERROR, e.message ?: "Connect failed", e)
        }
    }

    private fun rawTransceive(command: ByteArray): ByteArray = when (val t = tech) {
        is IsoDep -> t.transceive(command)
        is NfcA -> t.transceive(command)
        is NfcB -> t.transceive(command)
        is NfcF -> t.transceive(command)
        is NfcV -> t.transceive(command)
        is MifareUltralight -> t.transceive(command)
        is MifareClassic -> t.transceive(command)
        else -> throw NfcException(NfcException.IO_ERROR, "Connected technology cannot transceive")
    }

    private fun openPreferred(): TagTechnology? {
        for (name in TRANSCEIVE_PRIORITY) {
            if (name in techList) openByName(name)?.let { return it }
        }
        return null
    }

    private fun openByName(name: String): TagTechnology? = when (name) {
        IsoDep::class.java.name -> IsoDep.get(tag)
        NfcA::class.java.name -> NfcA.get(tag)
        NfcB::class.java.name -> NfcB.get(tag)
        NfcF::class.java.name -> NfcF.get(tag)
        NfcV::class.java.name -> NfcV.get(tag)
        MifareUltralight::class.java.name -> MifareUltralight.get(tag)
        MifareClassic::class.java.name -> MifareClassic.get(tag)
        else -> null
    }

    private companion object {
        val TRANSCEIVE_PRIORITY = listOf(
            IsoDep::class.java.name,
            NfcA::class.java.name,
            NfcB::class.java.name,
            NfcF::class.java.name,
            NfcV::class.java.name,
            MifareUltralight::class.java.name,
            MifareClassic::class.java.name,
        )
    }
}
