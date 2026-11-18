package com.tourderun.feature.record.data

import com.tourderun.core.data.record.RunSaver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordDataModule {
    @Binds
    abstract fun bindRunSaver(impl: FirestoreRunSaver): RunSaver
}
