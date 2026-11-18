package com.tourderun.core.di

import com.tourderun.core.data.team.FirestoreTeamRepository
import com.tourderun.core.data.team.TeamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamModule {
    @Binds abstract fun bindTeamRepository(impl: FirestoreTeamRepository): TeamRepository
}
