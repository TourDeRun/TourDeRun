package com.tourderun.core.di

import com.tourderun.core.data.record.FakeRunRepository
import com.tourderun.core.data.record.RunRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * [RunRepository] 바인딩. 로그인(AUTH-01) 전에는 실제 조회가 규칙에 막혀서 가짜 구현을 쓴다.
 * 가짜 구현은 Firebase를 쓰지 않아 google-services.json 없이도 앱이 실행된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RunRepositoryModule {
    // TODO(REC-02): 로그인 머지 후 구현 클래스를 FirestoreRunRepository로 바꾼다(#20).
    @Binds abstract fun bindRunRepository(impl: FakeRunRepository): RunRepository
}
