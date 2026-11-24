package com.tourderun.core.di

import com.tourderun.core.data.run.RunActivity
import com.tourderun.feature.run.session.RunSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RunActivityModule {
    @Binds abstract fun bindRunActivity(impl: RunSessionManager): RunActivity
}
