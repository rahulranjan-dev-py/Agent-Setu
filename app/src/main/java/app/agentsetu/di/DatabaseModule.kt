package app.agentsetu.di

import android.content.Context
import androidx.room.Room
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.DatabaseKeyProvider
import app.agentsetu.data.db.Migrations
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
            // Version 1 only ever existed in pre-release debug builds. From the first public
            // release onwards every schema change needs a real Migration: users' data must survive.
            .fallbackToDestructiveMigrationFrom(true, 1)
            .addMigrations(*Migrations.ALL)
            .build()
    }

    @Provides
    fun provideUserProfileDao(db: AgentSetuDatabase): UserProfileDao = db.userProfileDao()
}
