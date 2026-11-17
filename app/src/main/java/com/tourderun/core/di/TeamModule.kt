package com.tourderun.core.di

import com.tourderun.core.data.team.FakeTeamRepository
import com.tourderun.core.data.team.TeamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamModule {
    // TODO(TEAM-01): FirestoreTeamRepository가 생기면 그 구현으로 바꾼다 (TEAM-02 초대 코드 함수 머지 후).
    @Binds abstract fun bindTeamRepository(impl: FakeTeamRepository): TeamRepository
}
