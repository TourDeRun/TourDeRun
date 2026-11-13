package com.tourderun.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TdrTabBar
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.core.ui.model.RunRowUi
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.TeamUi

/** @Preview 전용 가짜 데이터 (와이어프레임 값과 같다). 실제 화면에서는 쓰지 않는다. */
object PreviewSamples {
    val team = TeamUi(
        name = "뛰뛰크루",
        appearance = TeamAppearance(TeamColor.BLUE, TeamIcon.RUNNER),
        season = 1,
        memberCount = 4,
        distanceM = 66_800,
    )

    val stage = StageProgressUi(
        stageNumber = 3,
        stageCount = 7,
        fromCity = "오산",
        toCity = "천안",
        startKm = 48,
        endKm = 85,
        teamDistanceM = 66_800,
    )

    val members = listOf(
        MemberUi("u1", "민지", TeamColor.BLUE, 22_400, isLeader = true, isMvp = true),
        MemberUi("u2", "준호", TeamColor.NAVY, 19_100),
        MemberUi("u3", "하은", TeamColor.GREEN, 15_000),
        MemberUi("u4", "도윤", TeamColor.PURPLE, 10_300),
    )

    val rivals = listOf(
        RivalTeamUi("주황이들", TeamAppearance(TeamColor.ORANGE), 3, 69_200),
        RivalTeamUi("보라돌이", TeamAppearance(TeamColor.PURPLE), 2, 65_700),
        RivalTeamUi("새벽러너스", TeamAppearance(TeamColor.GREEN), 4, 58_300),
        RivalTeamUi("한솔이네", TeamAppearance(TeamColor.TEAL), 1, 50_900),
    )

    val runs = listOf(
        RunRowUi("r6", 11, 6, "11월 6일 저녁 러닝", 1_122, 346, 3_240),
        RunRowUi("r5", 11, 5, "한강 한 바퀴", 2_470, 363, 6_800),
        RunRowUi("r4", 11, 3, "11월 3일 아침 러닝", 1_735, 347, 5_000),
        RunRowUi("r3", 11, 1, "11월 1일 저녁 러닝", 920, 383, 2_400),
        RunRowUi("r2", 10, 30, "팀 첫 러닝", 2_102, 350, 6_000),
        RunRowUi("r1", 10, 28, "10월 28일 저녁 러닝", 1_351, 375, 3_600),
    )
}

/** 하단 탭이 붙는 화면 미리보기. 실제 탭바는 AppNavHost의 Scaffold가 붙인다. */
@Composable
fun TabScreenPreview(tab: TdrTab, content: @Composable () -> Unit) {
    TdrTheme {
        Column(Modifier.fillMaxSize().background(TdrTheme.colors.bgCanvas)) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            TdrTabBar(selected = tab, onSelect = {})
        }
    }
}
