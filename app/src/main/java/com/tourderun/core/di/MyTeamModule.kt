package com.tourderun.core.di

import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.data.team.SessionMyTeamSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MyTeamModule {
    @Binds abstract fun bindMyTeamSource(impl: SessionMyTeamSource): MyTeamSource
}
