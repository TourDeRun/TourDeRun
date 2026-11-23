package com.tourderun.feature.journey.ui

import com.tourderun.domain.journey.PendingArrivals
import com.tourderun.domain.model.Route

/**
 * 못 본 도착 → 스테이지 클리어 연출 상태 (JRN-05). 값은 [route]에서 읽고 구간 거리를 직접 적지 않는다.
 *
 * - 한 번에 두 도시 이상 지났으면 마지막 도시만 보인다(오산·천안 → 천안, 스테이지 3 클리어).
 * - 완주(부산 도착)면 다음 스테이지·도시·거리가 모두 null.
 * - 지역 카드는 아직 없다(card = null).
 *
 * 예: 안양 도착 → 스테이지 1 클리어, 다음 스테이지 2·오산·29km / 부산 도착 → 스테이지 7 클리어, 다음 없음.
 */
fun stageClearUiState(pending: PendingArrivals, route: Route): StageClearUiState {
    // 도착 수 = 마지막으로 도착한 도시의 번호 = 그 도시로 끝난 스테이지 번호(화면 기준, 1부터)
    val cleared = pending.current
    val next = if (pending.isFinish) null else route.stages[cleared]
    return StageClearUiState(
        clearedStage = cleared,
        arrivedCity = pending.lastCity.nameKo,
        nextStage = next?.let { cleared + 1 },
        nextCity = next?.to?.nameKo,
        nextStageKm = next?.lengthKm,
        // TODO(#21): 이번 도착으로 얻은 지역 카드(EXT-01)
        card = null,
    )
}
