package com.tourderun.feature.journey.ui

import com.tourderun.domain.card.RegionCards
import com.tourderun.domain.journey.PendingArrivals
import com.tourderun.domain.model.Route

/**
 * 못 본 도착 → 스테이지 클리어 연출 상태 (JRN-05). 값은 [route]에서 읽고 구간 거리를 직접 적지 않는다.
 *
 * - 한 번에 두 도시 이상 지났으면 마지막 도시만 보인다(오산·천안 → 천안, 스테이지 3 클리어).
 * - 완주(부산 도착)면 다음 스테이지·도시·거리가 모두 null.
 * - 지역 카드(EXT-01)는 [newCardCityIds]의 마지막 도시 하나만 보인다([earnedCardUi]). 새 카드가 없거나
 *   가진 카드를 못 읽었으면(null) card = null(카드 보기 버튼도 없다).
 *
 * 예: 안양 도착 → 스테이지 1 클리어, 다음 스테이지 2·오산·29km / 부산 도착 → 스테이지 7 클리어, 다음 없음.
 *
 * @param newCardCityIds 이번 확인에서 줄 카드 도시 ID(도착 순서). `:domain`의 `newCardCityIds()` 결과
 */
fun stageClearUiState(
    pending: PendingArrivals,
    route: Route,
    newCardCityIds: List<String>? = null,
): StageClearUiState {
    // 도착 수 = 마지막으로 도착한 도시의 번호 = 그 도시로 끝난 스테이지 번호(화면 기준, 1부터)
    val cleared = pending.current
    val next = if (pending.isFinish) null else route.stages[cleared]
    return StageClearUiState(
        clearedStage = cleared,
        arrivedCity = pending.lastCity.nameKo,
        nextStage = next?.let { cleared + 1 },
        nextCity = next?.to?.nameKo,
        nextStageKm = next?.lengthKm,
        card = newCardCityIds?.lastOrNull()?.let { earnedCardUi(it, route) },
    )
}

/**
 * 연출에 보일 카드 한 장. 한글 이름은 경로, 영문 이름은 [RegionCards]에서 읽는다.
 * 순번은 출발 도시 포함(서울 1 … 부산 8), 전체는 경로 도시 수(8).
 * 경로나 카드 콘텐츠에 없는 도시면 null(카드는 저장되지만 연출에는 보이지 않는다).
 */
fun earnedCardUi(cityId: String, route: Route): EarnedCardUi? {
    val index = route.cities.indexOfFirst { it.id == cityId }
    if (index < 0) return null
    val content = RegionCards.of(cityId) ?: return null
    return EarnedCardUi(
        cityName = route.cities[index].nameKo,
        cityNameEn = content.nameEn,
        index = index + 1,
        total = route.cities.size,
    )
}
