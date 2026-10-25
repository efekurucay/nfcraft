package com.nfcraft.feature.profiles

import kotlinx.serialization.Serializable

/** One task item on the wire: MIME type "w<cat>/<no>" + pipe-delimited payload. */
@Serializable
data class ProfileItem(val mime: String, val payload: String)

/** A named, saved set of task items (a NFC Tasks-style profile). */
@Serializable
data class TaskProfile(
    val name: String,
    val createdAtEpoch: Long,
    val items: List<ProfileItem>,
)

/** Persistence + import/export of [TaskProfile]s. Impl owned by :feature-profiles. */
interface ProfileRepository {
    fun list(): List<TaskProfile>
    fun get(name: String): TaskProfile?
    fun save(profile: TaskProfile)
    fun delete(name: String)
    /** Serialize a profile to JSON text (for sharing / file export). */
    fun exportJson(profile: TaskProfile): String
    /** Parse JSON text back into a profile; throws on malformed input. */
    fun importJson(json: String): TaskProfile

    /**
     * Serialize a profile's items to CSV (RFC 4180): a `mime,payload` header
     * plus one row per item. The profile name is carried by the file, not the rows.
     */
    fun exportCsv(profile: TaskProfile): String
    /**
     * Parse CSV rows back into a profile under [name]; a leading `mime,payload`
     * header row is skipped. Throws on malformed input.
     */
    fun importCsv(name: String, csv: String): TaskProfile
}
