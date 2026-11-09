package com.tourderun.core.di

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase 인스턴스 제공.
 * google-services.json이 없으면 FirebaseApp이 초기화되지 않아 [FirebaseFirestore.getInstance]가 예외를 던진다.
 * 그래서 앱 시작 경로에서 이 값을 바로 주입받지 않는다(필요하면 `dagger.Lazy`로 받는다).
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
