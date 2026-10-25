package com.nfcraft.app.di

import android.content.Context
import com.nfcraft.app.engine.TagEngine
import com.nfcraft.app.nfc.NfcController
import com.nfcraft.app.nfc.TagOperations
import com.nfcraft.app.settings.AppSettings
import com.nfcraft.app.tasks.AppDangerousTaskGuard
import com.nfcraft.app.tasks.TaskConfirmationController
import com.nfcraft.app.tasks.TaskRunController
import com.nfcraft.core.chip.DefaultChipIdentifier
import com.nfcraft.core.chip.DefaultSignatureVerifier
import com.nfcraft.core.model.ChipIdentifier
import com.nfcraft.core.model.NdefCodec
import com.nfcraft.core.model.SignatureVerifier
import com.nfcraft.core.model.TagWriter
import com.nfcraft.core.ndef.DefaultNdefCodec
import com.nfcraft.core.tag.DefaultTagWriter
import com.nfcraft.feature.profiles.DefaultProfileRepository
import com.nfcraft.feature.profiles.ProfileRepository
import com.nfcraft.feature.tasks.DefaultTaskCodec
import com.nfcraft.feature.tasks.TaskSequenceRunner
import com.nfcraft.feature.tasks.createDefaultTaskExecutorFactory

/**
 * Manual dependency container. Single source of the engine singletons; held by
 * [com.nfcraft.app.NfcraftApp] and read by the UI.
 */
class AppContainer(context: Context) {
    val settings = AppSettings(context)

    val ndefCodec: NdefCodec = DefaultNdefCodec()
    val chipIdentifier: ChipIdentifier = DefaultChipIdentifier()
    val signatureVerifier: SignatureVerifier = DefaultSignatureVerifier()
    val tagWriter: TagWriter = DefaultTagWriter()

    val taskConfirmations = TaskConfirmationController()
    private val dangerousGuard = AppDangerousTaskGuard(settings, taskConfirmations)
    val taskFactory = createDefaultTaskExecutorFactory(dangerousGuard)
    val taskCodec = DefaultTaskCodec(taskFactory)
    private val taskRunner = TaskSequenceRunner(taskFactory)
    val taskRunController = TaskRunController(taskRunner, taskCodec, context.applicationContext)

    val profileRepository: ProfileRepository = DefaultProfileRepository(context)

    val tagEngine = TagEngine(ndefCodec, chipIdentifier, signatureVerifier)

    private val tagOperations = TagOperations(tagEngine, ndefCodec, taskCodec, chipIdentifier, tagWriter)
    val nfcController = NfcController(tagOperations)
}
