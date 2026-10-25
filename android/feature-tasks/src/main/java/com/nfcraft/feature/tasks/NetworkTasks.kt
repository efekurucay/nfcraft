package com.nfcraft.feature.tasks

import com.nfcraft.core.model.TaskCategory

/** Stable task identities; parameter order defines the wire payload. */
object NetworkTasks {
    val catalog = listOf(
        task(TaskCategory.NETWORK, 1, "Open URL", "url:TEXT"),
        task(TaskCategory.NETWORK, 2, "WiFi enabled", "enabled:BOOLEAN"),
        task(TaskCategory.NETWORK, 3, "WiFi settings", ""),
        task(TaskCategory.NETWORK, 4, "HTTP GET", "url:TEXT username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 5, "HTTP POST", "url:TEXT body:TEXT contentType:TEXT? username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 6, "HTTP PUT", "url:TEXT body:TEXT contentType:TEXT? username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 7, "HTTP DELETE", "url:TEXT username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 8, "HTTP PATCH", "url:TEXT body:TEXT contentType:TEXT? username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 9, "HTTP HEAD", "url:TEXT username:TEXT? password:TEXT?"),
        task(TaskCategory.NETWORK, 10, "Wireless settings", ""),
        task(TaskCategory.NETWORK, 11, "Internet settings", ""),
        task(TaskCategory.NETWORK, 12, "WiFi network settings", ""),
        task(TaskCategory.NETWORK, 13, "Add WiFi network", "ssid:TEXT password:TEXT? security:ENUM?"),
        task(TaskCategory.NETWORK, 14, "Remove WiFi network", "ssid:TEXT", dangerous = true),
        task(TaskCategory.NETWORK, 15, "WiFi hotspot", "enabled:BOOLEAN"),
        task(TaskCategory.NETWORK, 16, "Mobile data", "enabled:BOOLEAN"),
        task(TaskCategory.NETWORK, 17, "Airplane mode", "enabled:BOOLEAN"),
        task(TaskCategory.NETWORK, 18, "VPN settings", ""),
        task(TaskCategory.NETWORK, 19, "Data roaming settings", ""),
        task(TaskCategory.NETWORK, 20, "Wake on LAN", "mac:TEXT host:TEXT port:NUMBER?"),
        task(TaskCategory.NETWORK, 21, "Ping host", "host:TEXT"),
        task(TaskCategory.NETWORK, 22, "DNS lookup", "host:TEXT"),
        task(TaskCategory.NETWORK, 23, "Proxy settings", "host:TEXT port:NUMBER"),
        task(TaskCategory.NETWORK, 24, "WiFi scan", ""),
    )
}
