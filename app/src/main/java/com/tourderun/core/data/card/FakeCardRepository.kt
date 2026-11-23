package com.tourderun.core.data.card

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 화면 연결·디버그·테스트용 가짜 구현. Firebase를 쓰지 않는다.
 *
 * - uid가 null이면 `Ready(빈 목록)`.
 * - [failure]가 있으면 uid가 있을 때 `Failed(failure)` 한 번(다시 시도해도 실패).
 * - [owned]를 바꾸면 구독 중인 쪽에 새 목록이 간다.
 *
 * 예: S17 도감 Preview·디버그에서 `FakeCardRepository(listOf(OwnedCard("seoul", 0, "뛰뛰크루")))`.
 */
class FakeCardRepository(
    initial: List<OwnedCard> = emptyList(),
    private val failure: DataError? = null,
) : CardRepository {
    val owned = MutableStateFlow(initial)

    override fun observeOwned(uid: String?): Flow<Loaded<List<OwnedCard>>> = when {
        uid == null -> flowOf(Loaded.Ready(emptyList(), isFromCache = false))
        failure != null -> flowOf(Loaded.Failed(failure))
        else -> owned.map { Loaded.Ready(it, isFromCache = false) }
    }
}
