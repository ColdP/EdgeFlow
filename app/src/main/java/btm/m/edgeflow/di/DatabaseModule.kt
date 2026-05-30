package btm.m.edgeflow.di

import android.content.Context
import androidx.room.Room
import btm.m.edgeflow.data.AppDatabase
import btm.m.edgeflow.data.dao.CustomLinkDao
import btm.m.edgeflow.data.dao.SelectedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "edgeflow.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSelectedAppDao(db: AppDatabase): SelectedAppDao {
        return db.selectedAppDao()
    }

    @Provides
    fun provideCustomLinkDao(db: AppDatabase): CustomLinkDao {
        return db.customLinkDao()
    }
}
