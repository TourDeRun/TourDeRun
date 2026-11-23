package com.tourderun.core.di

import com.tourderun.core.data.card.CardRepository
import com.tourderun.core.data.card.FirestoreCardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 지역 카드(EXT-01) 바인딩. `FirestoreCardRepository`는 Firestore를 `dagger.Lazy`로 받아
 * `observeOwned(uid)`를 uid와 함께 부를 때 처음 만든다. google-services.json 없는 빌드에서도 주입만으로는 죽지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CardModule {
    @Binds abstract fun bindCardRepository(impl: FirestoreCardRepository): CardRepository
}
