package com.tourderun.feature.race.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RaceDataModule {
    // TODO(RACE-03): TeamRepository가 생기면 이 바인딩을 지우고 StageRankViewModel이 TeamRepository를 받는다.
    @Binds abstract fun bindMyTeamSource(impl: FixedMyTeamSource): MyTeamSource
}
