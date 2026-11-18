package com.tourderun.core.di

import com.tourderun.core.data.record.FirestoreRunRepository
import com.tourderun.core.data.record.RunRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * [RunRepository] 바인딩. 로그인한 계정의 실제 기록을 읽는다(uid는 화면 ViewModel이 `SessionRepository`에서 읽어 넘긴다).
 * Firestore는 S11·S12를 열 때 처음 만들어지므로 앱 시작 경로에는 없다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RunRepositoryModule {
    @Binds abstract fun bindRunRepository(impl: FirestoreRunRepository): RunRepository
}
