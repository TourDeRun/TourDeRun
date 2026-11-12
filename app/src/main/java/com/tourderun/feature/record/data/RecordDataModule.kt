package com.tourderun.feature.record.data

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
