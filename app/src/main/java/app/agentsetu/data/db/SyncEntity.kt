package app.agentsetu.data.db

/**
 * Every table carries these fields so a later move to cloud sync (roadmap Part B) needs no rebuild:
 * a UUID instead of an auto-number, timestamps in epoch millis, and a soft-delete flag.
 */
interface SyncEntity {
    val id: String
    val createdAt: Long
    val updatedAt: Long
    val deleted: Boolean
}
