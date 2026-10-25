package com.nfcraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nfcraft.app.di.AppContainer
import com.nfcraft.app.ui.NfcraftRoot
import com.nfcraft.app.ui.theme.NfcraftTheme
import com.nfcraft.core.nfc.TagReader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var reader: TagReader
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = (application as NfcraftApp).container
        reader = TagReader(this)

        val startRoute = when (intent?.data?.host) {
            "write" -> "write"
            "tasks" -> "tasks"
            else -> "read"
        }

        setContent {
            NfcraftTheme {
                NfcraftRoot(
                    container = container,
                    startRoute = startRoute,
                    nfcSupported = reader.isNfcSupported,
                    nfcEnabled = reader.isNfcEnabled,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reader.enable { tag ->
            lifecycleScope.launch { container.nfcController.onTag(tag) }
        }
    }

    override fun onPause() {
        super.onPause()
        reader.disable()
    }
}
