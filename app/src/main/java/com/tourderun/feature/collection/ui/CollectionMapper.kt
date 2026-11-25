package com.tourderun.feature.collection.ui

import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.ui.Format
import com.tourderun.domain.card.RegionCards
import com.tourderun.domain.journey.arrivals
import com.tourderun.domain.model.Route
import com.tourderun.domain.stats.SEOUL
import java.time.ZoneId

/**
 * 가진 카드 + 팀 거리 → S17 도감 상태 (EXT-01). 카드는 경로 순서(출발 도시 포함)로 늘어놓는다.
 *
 * - 가진 카드 → [RegionCardStatus.Collected](받은 날짜. 시각이 없으면 날짜 없이)
 * - 팀이 다음에 도착할 도시이고 아직 없으면 → [RegionCardStatus.Next](남은 거리)
 * - 팀이 이미 도착한 도시인데 없는 카드 → [RegionCardStatus.Passed]. 팀에 들어오기 전에 팀이 지나간 도시라 카드 대상이 아니다
 *   (`newCardCityIds` 규칙). 출발 도시(서울)는 제외
 * - 그 밖에 없는 카드 → [RegionCardStatus.Locked]
 *
 * @param teamDistanceM 지금 팀 거리. 팀이 없으면 null(다음 카드를 표시하지 않는다)
 */
fun collectionUiState(
    route: Route,
    owned: List<OwnedCard>,
    teamDistanceM: Long?,
    zone: ZoneId = SEOUL,
): CollectionUiState {
    val ownedById = owned.associateBy { it.cityId }
    // 다음 도착 도시의 cities 번호(출발 도시가 0). 완주했거나 팀이 없으면 null.
    // 팀 도착 수(출발 도시 제외). cities 번호 1..arrived가 팀이 이미 도착한 도시다. 팀이 없으면 0.
    val arrived = teamDistanceM?.let { arrivals(it, route) } ?: 0
    val nextIndex = teamDistanceM?.let { arrived + 1 }?.takeIf { it < route.cities.size }
    return CollectionUiState(
        routeFrom = route.cities.first().nameKo,
        routeTo = route.cities.last().nameKo,
        cards = route.cities.mapIndexed { index, city ->
            val card = ownedById[city.id]
            val status = when {
                card != null -> RegionCardStatus.Collected(card.earnedAtMs.takeIf { it > 0 }?.let { Format.monthDay(it, zone) })
                index in 1..arrived -> RegionCardStatus.Passed
                index == nextIndex && teamDistanceM != null ->
                    RegionCardStatus.Next(route.cumulativeEndM[index - 1] - teamDistanceM)
                else -> RegionCardStatus.Locked(
                    stageNumber = index,
                    cumulativeKm = if (index == 0) 0 else (route.cumulativeEndM[index - 1] / 1_000).toInt(),
                )
            }
            RegionCardUi(city.id, city.nameKo, status)
        },
    )
}

/**
 * 도시 하나 → S18 카드 상세 (EXT-01). 글 콘텐츠는 M-07([RegionCards]), 받은 날짜·팀 이름은 가진 카드에서.
 * 아직 없는 카드(또는 받은 시각을 모르는 카드)면 받은 줄을 숨긴다([CardDetailUiState.acquiredDateText] = null).
 *
 * @return 경로에 없는 도시 ID면 null
 */
fun cardDetailUiState(
    route: Route,
    cityId: String,
    owned: List<OwnedCard>,
    zone: ZoneId = SEOUL,
): CardDetailUiState? {
    val index = route.cities.indexOfFirst { it.id == cityId }.takeIf { it >= 0 } ?: return null
    val city = route.cities[index]
    val content = RegionCards.of(cityId)
    val card = owned.firstOrNull { it.cityId == cityId }
    return CardDetailUiState(
        name = city.nameKo,
        nameEn = content?.nameEn ?: cityId.replaceFirstChar { it.uppercase() },
        stageNumber = index.takeIf { it > 0 },
        startCity = route.cities.first().nameKo,
        distanceFromStartKm = route.stages.take(index).sumOf { it.lengthKm },
        description = content?.intro.orEmpty(),
        landmarks = content?.landmarks.orEmpty().map { LandmarkUi(it.name, it.description) },
        // 사진이 정해지기 전에는 null이라 S18이 출처 줄을 숨긴다. TODO(EXT-01): 사진과 출처를 채운다.
        photoCredit = content?.photoCredit,
        acquiredDateText = card?.earnedAtMs?.takeIf { it > 0 }?.let { Format.monthDay(it, zone) },
        teamName = card?.teamName.orEmpty(),
    )
}
