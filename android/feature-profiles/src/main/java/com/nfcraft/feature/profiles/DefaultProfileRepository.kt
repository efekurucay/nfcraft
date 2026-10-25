package com.nfcraft.feature.profiles

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-backed [ProfileRepository]. Each profile persists as one JSON file under
 * filesDir/profiles/<sanitized-name>.json. The on-disk name is a filesystem-safe
 * derivation; the profile's real [TaskProfile.name] always lives inside the JSON.
 */
class DefaultProfileRepository(private val context: Context) : ProfileRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dir: File
        get() = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }

    override fun list(): List<TaskProfile> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching { json.decodeFromString<TaskProfile>(file.readText()) }.getOrNull()
        }.sortedByDescending { it.createdAtEpoch }
    }

    override fun get(name: String): TaskProfile? {
        val file = fileFor(name)
        if (!file.isFile) return list().firstOrNull { it.name == name }
        return runCatching { json.decodeFromString<TaskProfile>(file.readText()) }.getOrNull()
            ?: list().firstOrNull { it.name == name }
    }

    override fun save(profile: TaskProfile) {
        val target = fileFor(profile.name)
        val text = json.encodeToString(profile)
        // Atomic write: fully materialize a temp file, then swap it into place.
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeText(text)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            // Fallback when rename across the same dir is refused: copy then drop temp.
            target.writeText(text)
            tmp.delete()
        }
    }

    override fun delete(name: String) {
        fileFor(name).takeIf { it.exists() }?.delete()
    }

    override fun exportJson(profile: TaskProfile): String = json.encodeToString(profile)

    override fun importJson(json: String): TaskProfile = this.json.decodeFromString(json)

    override fun exportCsv(profile: TaskProfile): String = buildString {
        append("mime,payload\r\n")
        for (item in profile.items) {
            append(csvField(item.mime))
            append(',')
            append(csvField(item.payload))
            append("\r\n")
        }
    }

    override fun importCsv(name: String, csv: String): TaskProfile {
        val rows = parseCsv(csv)
        val items = rows
            .filterIndexed { index, row -> !(index == 0 && row.getOrNull(0)?.trim() == "mime") }
            .filter { it.any { cell -> cell.isNotBlank() } }
            .map { row ->
                require(row.size >= 1) { "Empty CSV row" }
                ProfileItem(mime = row[0].trim(), payload = row.getOrElse(1) { "" })
            }
        require(items.isNotEmpty()) { "No rows found in CSV" }
        return TaskProfile(name = name, createdAtEpoch = System.currentTimeMillis(), items = items)
    }

    /** Quote a field when it contains a comma, quote, or line break (RFC 4180). */
    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Minimal RFC 4180 reader: handles quoted fields, escaped quotes, CRLF/LF. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        var sawAny = false
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.clear(); sawAny = true }
                c == '\r' -> { /* consumed with the following \n or alone */ }
                c == '\n' -> {
                    row.add(field.toString()); field.clear()
                    rows.add(row); row = mutableListOf(); sawAny = false
                }
                else -> { field.append(c); sawAny = true }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty() || sawAny) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }

    private fun fileFor(name: String): File = File(dir, "${sanitize(name)}.json")

    /** Reduce an arbitrary profile name to a safe, non-empty filename stem. */
    private fun sanitize(name: String): String {
        val cleaned = buildString {
            for (c in name.trim()) {
                append(if (c.isLetterOrDigit() || c == '-' || c == '_') c else '_')
            }
        }.trim('_').take(120)
        return cleaned.ifEmpty { "profile" }
    }
}
