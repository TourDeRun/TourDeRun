package com.tourderun.feature.stat.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StatDataModule {
    // TODO(STAT-01): TeamRepository가 생기면 이 바인딩을 지우고 WeeklyStatsViewModel이 TeamRepository를 받는다.
    @Binds abstract fun bindMyTeamSource(impl: FixedMyTeamSource): MyTeamSource
}
