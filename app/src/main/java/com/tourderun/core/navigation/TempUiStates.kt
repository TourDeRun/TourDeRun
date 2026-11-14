package com.tourderun.core.navigation

import com.tourderun.core.designsystem.component.SampleRoutePoints
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.feature.collection.ui.CardDetailUiState
import com.tourderun.feature.collection.ui.CollectionUiState
import com.tourderun.feature.collection.ui.LandmarkUi
import com.tourderun.feature.collection.ui.RegionCardStatus
import com.tourderun.feature.collection.ui.RegionCardUi
import com.tourderun.feature.home.ui.HomeUiState
import com.tourderun.feature.home.ui.WeeklySummaryUi
import com.tourderun.feature.journey.ui.RoadViewUiState
import com.tourderun.feature.profile.ui.CollectionSummaryUi
import com.tourderun.feature.profile.ui.ProfileUiState
import com.tourderun.feature.record.ui.RecordDetailUiState
import com.tourderun.feature.record.ui.RecordsUiState
import com.tourderun.feature.run.ui.OvertakeUi
import com.tourderun.feature.run.ui.RunResultUiState
import com.tourderun.feature.run.ui.RunningUiState
import com.tourderun.feature.team.ui.TeamEditUiState
import com.tourderun.feature.team.ui.TeamUiState

/**
 * ViewModel이 아직 없는 화면을 [AppNavHost]에 띄우기 위한 임시 고정 상태. 값은 와이어프레임([PreviewSamples])과 같다.
 *
 * 화면 주인이 ViewModel을 붙이면 그 화면의 항목을 지운다. 전부 지워지면 이 파일도 지운다.
 */
internal object TempUiStates {
    private val stageAfterRun = StageProgressUi(3, 7, "오산", "천안", 48, 85, 70_040)

    // TODO(JRN-03): HomeViewModel(#48)로 바꾼다.
    val home = HomeUiState(
        team = PreviewSamples.team,
        stage = PreviewSamples.stage,
        rank = 2,
        totalTeams = 5,
        ahead = PreviewSamples.rivals[0],
        behind = PreviewSamples.rivals[1],
        weekly = WeeklySummaryUi(42_500, 9, 6_100, 38, 11),
        members = PreviewSamples.members,
    )

    // TODO(JRN-04): 로드뷰 ViewModel(#50)로 바꾼다.
    val roadView = RoadViewUiState(
        team = PreviewSamples.team,
        stage = PreviewSamples.stage,
        rank = 2,
        totalTeams = 5,
        rivalsInStage = PreviewSamples.rivals.take(2),
    )

    // TODO(AUTH-03): 프로필 ViewModel로 바꾼다.
    val profile = ProfileUiState(
        nickname = "민지",
        teamName = PreviewSamples.team.name,
        isLeader = true,
        totalDistanceM = 86_400,
        runCount = 21,
        latestRun = PreviewSamples.runs.first(),
        collection = CollectionSummaryUi(3, 8, "천안"),
    )

    // TODO(RUN-03): 러닝 ViewModel(RunSessionManager)로 바꾼다.
    val running = RunningUiState(
        distanceM = 3_240.0,
        movingSec = 1_122,
        avgPaceSecPerKm = 346,
        currentPaceSecPerKm = 331,
        stageWithRun = stageAfterRun,
        path = SampleRoutePoints,
    )

    // TODO(REC-06): 결과 화면 ViewModel(#51)로 바꾼다.
    val runResult = RunResultUiState(
        title = "11월 6일 저녁 러닝",
        distanceM = 3_240,
        movingSec = 1_122,
        paceSecPerKm = 346,
        teamBeforeM = 66_800,
        stageAfter = stageAfterRun,
        overtake = OvertakeUi("주황이들", 2, 1, 2_400, 840),
    )

    // TODO(REC-02): RecordsViewModel(#49)로 바꾼다.
    val records = RecordsUiState(totalRuns = 21, totalDistanceM = 86_400, runs = PreviewSamples.runs)

    // TODO(REC-03): RecordDetailViewModel(#49)로 바꾼다.
    fun recordDetail(runId: String): RecordDetailUiState {
        val run = PreviewSamples.runs.firstOrNull { it.runId == runId } ?: PreviewSamples.runs.first()
        return RecordDetailUiState(
            startedAtText = "2026. ${run.month}. ${run.day} 19:02",
            title = run.title,
            memo = "",
            distanceM = run.distanceM,
            movingSec = run.movingSec,
            paceSecPerKm = run.paceSecPerKm,
            path = SampleRoutePoints,
        )
    }

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

    // TODO(EXT-01): 카드 콘텐츠(M-07)가 준비되면 cityId로 실제 카드를 찾는다.
    fun cardDetail(cityId: String): CardDetailUiState {
        val card = collection.cards.firstOrNull { it.cityId == cityId } ?: collection.cards.first()
        return CardDetailUiState(
            name = card.name,
            nameEn = card.cityId.replaceFirstChar { it.uppercase() },
            stageNumber = collection.cards.indexOf(card).takeIf { it > 0 },
            startCity = collection.routeFrom,
            distanceFromStartKm = 0,
            description = "[지역 소개 준비 중]",
            landmarks = listOf(LandmarkUi("[랜드마크]", "[설명 준비 중]")),
            photoCredit = "[출처]",
            acquiredDateText = (card.status as? RegionCardStatus.Collected)?.dateText.orEmpty(),
            teamName = PreviewSamples.team.name,
        )
    }
}
