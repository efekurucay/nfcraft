package com.nfcraft.feature.tasks

import java.io.InputStream
import java.io.IOException
import kotlinx.coroutines.*

/** Runs arbitrary privileged commands. Call only after explicit user confirmation. */
class RootShell {
    data class Result(val exitCode: Int, val stdout: String, val stderr: String)

    suspend fun run(command: String): Result = withContext(Dispatchers.IO) {
        require(command.isNotBlank() && '\u0000' !in command) { "Invalid shell command" }
        var process: Process? = null
        var lastError: IOException? = null
        for (candidate in listOf("su", "/system/xbin/su", "/system/bin/su")) {
            try {
                process = Runtime.getRuntime().exec(arrayOf(candidate, "-c", command))
                break
            } catch (e: IOException) {
                lastError = e
            }
        }
        val running = process ?: throw IOException("No root shell available", lastError)
        coroutineScope {
            val out = async(Dispatchers.IO) { running.inputStream.capture() }
            val err = async(Dispatchers.IO) { running.errorStream.capture() }
            try {
                running.outputStream.close()
                withTimeoutOrNull(30_000) {
                    var exit: Int? = null
                    while (exit == null) {
                        currentCoroutineContext().ensureActive()
                        exit = try { running.exitValue() } catch (_: IllegalThreadStateException) { null }
                        if (exit == null) delay(25)
                    }
                    Result(requireNotNull(exit), out.await(), err.await())
                } ?: throw IOException("Root command timed out")
            } finally {
                // Release readers before coroutineScope waits for its children on cancellation.
                running.destroy()
                runCatching { running.inputStream.close() }
                runCatching { running.errorStream.close() }
            }
        }
    }

    private fun InputStream.capture(): String = use { stream ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = stream.read(buffer)
            if (size < 0) break
            val remaining = 65_536 - output.size()
            if (remaining > 0) output.write(buffer, 0, minOf(size, remaining))
        }
        output.toString("UTF-8")
    }
}
