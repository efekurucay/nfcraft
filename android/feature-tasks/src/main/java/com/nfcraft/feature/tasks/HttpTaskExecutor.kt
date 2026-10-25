package com.nfcraft.feature.tasks

import android.content.Context
import android.util.Base64
import com.nfcraft.core.model.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Proxy
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** True when a destination must be blocked. Every resolved address is checked. */
internal fun h(host: String): Boolean {
    val normalized = host.trim().removePrefix("[").removeSuffix("]").trimEnd('.').lowercase()
    if (normalized == "localhost" || normalized.endsWith(".localhost")) return true
    val addresses = InetAddress.getAllByName(normalized)
    return addresses.isEmpty() || addresses.any { address ->
        val b = address.address.map { it.toInt() and 255 }
        address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress ||
            (b.size == 16 && (b[0] and 0xfe) == 0xfc) ||
            (b.size == 4 && (b[0] == 10 || b[0] == 127 ||
                b[0] == 172 && b[1] in 16..31 || b[0] == 192 && b[1] == 168 ||
                b[0] == 169 && b[1] == 254))
    }
}

/** HTTP status determines success; response bodies are bounded and discarded by this contract. */
class HttpTaskExecutor(guard: DangerousTaskGuard = DangerousTaskGuard.Deny) :
    GuardedTaskExecutor(TaskCategory.NETWORK, (4..9).toSet(), guard) {
    override suspend fun perform(context: Context, record: TaskRecord): TaskResult = withContext(Dispatchers.IO) {
        val method = when (record.descriptor.number) {
            4 -> "GET"; 5 -> "POST"; 6 -> "PUT"; 7 -> "DELETE"; 8 -> "PATCH"; else -> "HEAD"
        }
        val hasBody = method in setOf("POST", "PUT", "PATCH")
        val credentialIndex = if (hasBody) 3 else 1
        val url = URL(record.args[0])
        require(url.protocol in setOf("http", "https")) { "Only HTTP(S) is supported" }
        require(url.userInfo == null) { "Use the declared authentication parameters" }
        require(!h(url.host)) { "Private or local HTTP destinations are blocked" }
        // HttpURLConnection performs its own DNS resolution. Use a trusted resolver/network;
        // this preflight check cannot pin DNS answers against rebinding.
        val connection = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = method
            connection.useCaches = false
            if (record.arg(credentialIndex).isNotEmpty()) {
                require(url.protocol == "https") { "Basic authentication requires HTTPS" }
                val credentials = record.arg(credentialIndex) + ":" + record.arg(credentialIndex + 1)
                connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            }
            currentCoroutineContext().ensureActive()
            if (hasBody) {
                val body = record.args[1].toByteArray(Charsets.UTF_8)
                require(body.size <= 1_048_576) { "HTTP body exceeds 1 MiB" }
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.setRequestProperty("Content-Type", record.arg(2, "text/plain; charset=utf-8"))
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            stream?.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val size = input.read(buffer)
                    if (size < 0) break
                    total += size
                    require(total <= 1_048_576) { "HTTP response exceeds 1 MiB" }
                }
            }
            when (status) {
                in 200..299 -> TaskResult.Success
                in 300..399 -> TaskResult.Failure("HTTP redirect blocked ($status)")
                else -> TaskResult.Failure("HTTP $status")
            }
        } finally {
            connection.disconnect()
        }
    }
}
