package com.tourderun.core.di

import com.tourderun.core.data.team.FixedMyTeamSource
import com.tourderun.core.data.team.MyTeamSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MyTeamModule {
    // TODO(RACE-03): TeamRepository가 생기면 이 바인딩을 지우고 StageRankViewModel·WeeklyStatsViewModel·RunSaver 호출부가 TeamRepository를 받는다.
    @Binds abstract fun bindMyTeamSource(impl: FixedMyTeamSource): MyTeamSource
}
