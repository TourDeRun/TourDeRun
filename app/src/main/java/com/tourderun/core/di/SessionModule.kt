package com.tourderun.core.di

import com.tourderun.core.data.session.FakeSessionRepository
import com.tourderun.core.data.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    // TODO(AUTH-01): Firebase 구현이 생기면 FirebaseSessionRepository로 바꾼다.
    @Binds abstract fun bindSessionRepository(impl: FakeSessionRepository): SessionRepository
}
