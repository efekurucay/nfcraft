package com.nfcraft.core.tag

import android.nfc.NdefMessage
import com.nfcraft.core.model.ChipFamily
import com.nfcraft.core.model.ChipType
import com.nfcraft.core.model.NfcException
import com.nfcraft.core.model.NfcIo
import com.nfcraft.core.model.NfcResult
import com.nfcraft.core.model.TagDump
import com.nfcraft.core.model.TagWriter

/**
 * Write / format / protect / dump engine, dispatched by chip family.
 *
 * The Type 2 family (Ultralight / NTAG) is driven end to end over raw page
 * commands. MIFARE Classic exposes block dump / restore only, because sector
 * authentication is not reachable through the transceive-only [NfcIo]. DESFire
 * exposes a plain-walk dump. All IO is wrapped in [NfcResult.of] so every
 * [NfcException] surfaces as a typed [NfcResult.Failure].
 */
class DefaultTagWriter : TagWriter {

    override fun write(io: NfcIo, chip: ChipType, message: NdefMessage): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.write(io, chip, message.toByteArray())
                else -> throw notWritable(chip, "raw NDEF write")
            }
        }

    override fun format(io: NfcIo, chip: ChipType): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.format(io, chip)
                else -> throw NfcException(
                    NfcException.FORMAT_FAILED,
                    "Format not supported for ${chip.displayName}",
                )
            }
        }

    override fun makeReadOnly(io: NfcIo, chip: ChipType): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.makeReadOnly(io, chip)
                else -> throw notWritable(chip, "make read-only")
            }
        }

    override fun setPassword(io: NfcIo, chip: ChipType, password: String): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.setPassword(io, chip, password)
                else -> throw notWritable(chip, "password protection")
            }
        }

    override fun removePassword(io: NfcIo, chip: ChipType, password: String): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.removePassword(io, chip, password)
                else -> throw notWritable(chip, "password removal")
            }
        }

    override fun dump(io: NfcIo, chip: ChipType): NfcResult<TagDump> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.dump(io, chip)
                ChipFamily.CLASSIC -> ClassicOperations.dump(io, chip)
                ChipFamily.DESFIRE -> DesfireOperations.dump(io, chip)
                else -> throw notWritable(chip, "dump")
            }
        }

    override fun writeDump(io: NfcIo, chip: ChipType, dump: TagDump): NfcResult<Unit> =
        NfcResult.of {
            when (chip.family) {
                ChipFamily.ULTRALIGHT -> Type2Operations.writeDump(io, chip, dump)
                ChipFamily.CLASSIC -> ClassicOperations.writeDump(io, chip, dump)
                else -> throw notWritable(chip, "dump restore")
            }
        }

    private fun notWritable(chip: ChipType, operation: String): NfcException =
        NfcException(
            NfcException.NOT_WRITABLE,
            "$operation not supported for ${chip.displayName}",
        )
}
