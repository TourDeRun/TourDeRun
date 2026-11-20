package com.tourderun.core.di

import com.tourderun.core.data.journey.ArrivalRepository
import com.tourderun.core.data.journey.FirestoreArrivalRepository
import com.tourderun.core.data.race.FakeRaceRepository
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.stats.FakeStatsRepository
import com.tourderun.core.data.stats.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 공유 Repository 바인딩. Firestore 구현이 생기면 해당 줄의 구현 클래스만 바꾼다.
 * 가짜 구현은 Firebase를 쓰지 않아 google-services.json 없이도 앱이 실행된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // TODO(RACE-01): 로그인(AUTH-01)과 google-services.json이 준비되면 FirestoreRaceRepository로 바꾼다.
    @Binds abstract fun bindRaceRepository(impl: FakeRaceRepository): RaceRepository

    // TODO(STAT-01): 로그인(AUTH-01)과 google-services.json이 준비되면 FirestoreStatsRepository로 바꾼다.
    @Binds abstract fun bindStatsRepository(impl: FakeStatsRepository): StatsRepository

    // 실제 members 문서로 계산한다. 가짜를 두면 앱을 열 때마다 연출이 뜬다.
    // 지금은 MyTeamSource가 고정 팀(Firestore에 없음)이라 항상 null(연출 없음). 진짜 팀 연결은 #45.
    // Firestore는 로그인 uid가 있을 때 처음 만들어진다(StageClearViewModel이 dagger.Lazy로 받는다).
    @Binds abstract fun bindArrivalRepository(impl: FirestoreArrivalRepository): ArrivalRepository
}
