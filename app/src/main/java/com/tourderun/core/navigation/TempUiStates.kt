package com.tourderun.core.navigation

import com.tourderun.core.data.record.FakeRunRepository
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.domain.card.RegionCards
import com.tourderun.feature.collection.ui.CardDetailUiState
import com.tourderun.feature.collection.ui.CollectionUiState
import com.tourderun.feature.collection.ui.LandmarkUi
import com.tourderun.feature.collection.ui.RegionCardStatus
import com.tourderun.feature.collection.ui.RegionCardUi
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.profile.ui.CollectionSummaryUi
import com.tourderun.feature.profile.ui.ProfileUiState
import com.tourderun.feature.record.ui.toRowUi
import com.tourderun.feature.team.ui.TeamEditUiState
import com.tourderun.feature.team.ui.TeamUiState

/**
 * ViewModel이 아직 없는 화면을 [AppNavHost]에 띄우기 위한 임시 고정 상태. 값은 와이어프레임([PreviewSamples])과 같다.
 *
 * 화면 주인이 ViewModel을 붙이면 그 화면의 항목을 지운다. 전부 지워지면 이 파일도 지운다.
 */
internal object TempUiStates {
    // TODO(AUTH-03): 프로필 ViewModel로 바꾼다.
    val profile = ProfileUiState(
        nickname = "민지",
        teamName = PreviewSamples.team.name,
        isLeader = true,
        totalDistanceM = 86_400,
        runCount = 21,
        // 누르면 S12가 열리므로 가짜 Repository에 실제로 있는 기록이어야 한다.
        latestRun = FakeRunRepository.SAMPLE_RUNS.first().toRowUi(),
        collection = CollectionSummaryUi(3, 8, "천안"),
    )

    // TODO(TEAM-04): 팀 ViewModel로 바꾼다.
    val team = TeamUiState(PreviewSamples.team, PreviewSamples.members, "4K7Q2M", isLeader = true, myUid = "u1")

    // TODO(TEAM-05): 팀 수정 ViewModel로 바꾼다.
    val teamEdit = TeamEditUiState(PreviewSamples.team.name, TeamColor.BLUE, TeamIcon.RUNNER)

    // TODO(EXT-01): 도감 ViewModel로 바꾼다.
    val collection = CollectionUiState(
        routeFrom = "서울",
        routeTo = "부산",
        cards = listOf(
            RegionCardUi("seoul", "서울", RegionCardStatus.Collected("10월 28일")),
            RegionCardUi("anyang", "안양", RegionCardStatus.Collected("10월 31일")),
            RegionCardUi("osan", "오산", RegionCardStatus.Collected("11월 4일")),
            RegionCardUi("cheonan", "천안", RegionCardStatus.Next(18_200)),
            RegionCardUi("sejong", "세종", RegionCardStatus.Locked(4, 125)),
            RegionCardUi("yeongdong", "영동", RegionCardStatus.Locked(5, 181)),
            RegionCardUi("daegu", "대구", RegionCardStatus.Locked(6, 262)),
            RegionCardUi("busan", "부산", RegionCardStatus.Locked(7, 350)),
        ),
    )

    // 글 콘텐츠는 M-07(RegionCards). 획득 날짜·팀 이름은 TODO(EXT-01): 도감 ViewModel에서 users.cards로 바꾼다.
    fun cardDetail(cityId: String): CardDetailUiState {
        val card = collection.cards.firstOrNull { it.cityId == cityId } ?: collection.cards.first()
        val content = RegionCards.of(card.cityId)
        val index = collection.cards.indexOf(card)
        return CardDetailUiState(
            name = card.name,
            nameEn = content?.nameEn ?: card.cityId.replaceFirstChar { it.uppercase() },
            stageNumber = index.takeIf { it > 0 },
            startCity = collection.routeFrom,
            distanceFromStartKm = routeFor(1).stages.take(index).sumOf { it.lengthKm },
            description = content?.intro.orEmpty(),
            landmarks = content?.landmarks.orEmpty().map { LandmarkUi(it.name, it.description) },
            // 사진이 정해지기 전에는 null이라 S18이 출처 줄을 숨긴다. TODO(EXT-01): 사진과 출처를 채운다.
            photoCredit = content?.photoCredit,
            acquiredDateText = (card.status as? RegionCardStatus.Collected)?.dateText.orEmpty(),
            teamName = PreviewSamples.team.name,
        )
    }
}
