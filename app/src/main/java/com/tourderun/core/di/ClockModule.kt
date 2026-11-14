package com.tourderun.core.di

import com.tourderun.core.data.Clock
import com.tourderun.core.data.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {
    @Binds abstract fun bindClock(impl: SystemClock): Clock
}
