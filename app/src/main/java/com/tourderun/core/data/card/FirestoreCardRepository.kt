package com.tourderun.core.data.card

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.snapshotFlow
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

/**
 * 가진 카드의 Firestore 구현 (EXT-01). 규칙은 [CardRepository] 참고.
 *
 * `users/{uid}` 문서 리스너(공용 `snapshotFlow`)를 [ownedCardsOrNull]로 바꾼다. `at`은 서버 시각 추정값으로 읽는다.
 * Firestore는 [observeOwned]를 uid와 함께 처음 부를 때 꺼낸다([Lazy]). 주입만으로는 Firebase를 만들지 않는다.
 */
@Singleton
class FirestoreCardRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) : CardRepository {

    override fun observeOwned(uid: String?): Flow<Loaded<List<OwnedCard>>> {
        if (uid == null) return flowOf(Loaded.Ready(emptyList(), isFromCache = false))
        return firestore.get().collection(Collections.USERS).document(uid)
            .snapshotFlow(TAG)
            .mapNotNull { loaded ->
                when (loaded) {
                    is Loaded.Failed -> loaded
                    is Loaded.Ready -> loaded.value.ownedCards(loaded.isFromCache)
                        ?.let { Loaded.Ready(it, loaded.isFromCache) }
                }
            }
            .distinctUntilChanged()
    }

    private fun DocumentSnapshot.ownedCards(isFromCache: Boolean): List<OwnedCard>? =
        ownedCardsOrNull(exists(), isFromCache, getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))

    private companion object {
        const val TAG = "TDR/card"
    }
}
