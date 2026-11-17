package com.tourderun.core.di

import com.tourderun.core.data.session.FakeSessionRepository
import com.tourderun.core.data.session.FirebaseSessionRepository
import com.tourderun.core.data.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    abstract fun bindSessionRepository(impl: FirebaseSessionRepository): SessionRepository
}
