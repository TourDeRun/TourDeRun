package com.tourderun.core.di

import com.tourderun.core.data.journey.ArrivalRepository
import com.tourderun.core.data.journey.FirestoreArrivalRepository
import com.tourderun.core.data.race.FirestoreRaceRepository
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.stats.FirestoreStatsRepository
import com.tourderun.core.data.stats.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 공유 Repository 바인딩. 모두 Firestore 구현이고, 기준 팀은 `MyTeamSource`(세션 teamId로 읽은 `teams` 문서)다.
 * 가짜 구현(`FakeRaceRepository`·`FakeStatsRepository`·`FakeArrivalRepository`)은 디버그 화면·테스트에서 직접 만든다.
 *
 * Race·Stats 구현은 `FirebaseFirestore`를 `dagger.Lazy`로 받아 리스너를 붙일 때 처음 꺼낸다.
 * 그래서 저장소를 주입받는 것만으로는 Firebase를 만들지 않는다(google-services.json 없는 빌드에서 주입 때 죽지 않음).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindRaceRepository(impl: FirestoreRaceRepository): RaceRepository

    @Binds abstract fun bindStatsRepository(impl: FirestoreStatsRepository): StatsRepository

    // 실제 members 문서의 seenArrivals로 계산한다. 가짜를 두면 앱을 열 때마다 연출이 뜬다.
    // Firestore는 로그인 uid가 있을 때 처음 만들어진다(StageClearViewModel이 dagger.Lazy로 받는다).
    @Binds abstract fun bindArrivalRepository(impl: FirestoreArrivalRepository): ArrivalRepository
}
