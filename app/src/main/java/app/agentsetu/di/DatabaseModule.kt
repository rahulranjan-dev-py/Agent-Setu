package app.agentsetu.di

import android.content.Context
import androidx.room.Room
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.DatabaseKeyProvider
import app.agentsetu.data.db.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): AgentSetuDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, AgentSetuDatabase::class.java, AgentSetuDatabase.FILE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyProvider.passphrase()))
            .build()
    }

    @Provides
    fun provideUserProfileDao(db: AgentSetuDatabase): UserProfileDao = db.userProfileDao()
}
