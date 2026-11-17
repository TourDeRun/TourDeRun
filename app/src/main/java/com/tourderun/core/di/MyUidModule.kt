package com.tourderun.core.di

import com.tourderun.core.data.session.MyUidSource
import com.tourderun.core.data.session.NoUidSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MyUidModule {
    // TODO(JRN-05): SessionRepository(권민찬, #62)가 머지되면 이 바인딩을 지우고 FirestoreArrivalRepository가 SessionRepository를 받는다.
    @Binds abstract fun bindMyUidSource(impl: NoUidSource): MyUidSource
}
