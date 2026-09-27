package app.agentsetu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AgentSetuDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val FILE_NAME = "agentsetu.db"
    }
}
