package com.tourderun.core.di

import android.content.Context
import androidx.room.Room
import com.tourderun.BuildConfig
import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.RunDatabase
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RunLocalDataModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): RunDatabase =
        Room.databaseBuilder(context, RunDatabase::class.java, "run_points.db")
            .apply {
                // 개발 중 스키마 변경을 허용한다. 기존 Room 데이터는 삭제될 수 있다.
                if (BuildConfig.DEBUG) fallbackToDestructiveMigration(dropAllTables = true)
            }
            .build()

    @Provides fun pointDao(database: RunDatabase): RunPointDao = database.runPointDao()
    @Provides fun activeRunStore(store: RunSessionStore): ActiveRunStore = store
}
