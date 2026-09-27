package app.agentsetu.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.agentsetu.core.model.StaffType
import java.util.UUID

/** Single row per install: who the user is, used to pick the commission rules that apply to them. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    val staffType: StaffType,
    val designation: String,
    val officeName: String,
    val division: String,
    val agentCodes: List<String> = emptyList(),
    val language: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity
