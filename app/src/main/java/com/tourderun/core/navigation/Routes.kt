package com.tourderun.core.navigation

import kotlinx.serialization.Serializable

/*
 * 화면 경로. 화면 ID는 기능 명세서 3장 표와 같다.
 * 경로 이름은 저장된 백스택을 되살릴 때 쓰이므로, 인자를 바꾸면 그 화면으로 가는 호출부도 같이 고친다.
 */

/** S07 홈 (탭, 시작 화면). */
@Serializable data object HomeRoute

/** S08 로드뷰 (탭). */
@Serializable data object RoadViewRoute

/** S13 프로필 (탭). */
@Serializable data object ProfileRoute

/** S09 러닝. 탭 가운데 버튼으로 들어가고 하단 탭은 숨긴다. */
@Serializable data object RunningRoute

/** S10 러닝 결과. */
@Serializable data object RunResultRoute

/** S11 기록 목록. */
@Serializable data object RecordsRoute

/** S12 기록 상세. */
@Serializable data class RecordDetailRoute(val runId: String)

/** S14 팀. */
@Serializable data object TeamRoute

/** 팀 이름·대표 이미지 수정 (TEAM-05). */
@Serializable data object TeamEditRoute

/** S15 스테이지 순위. */
@Serializable data object StageRankRoute

/** S16 주간 통계. */
@Serializable data object WeeklyStatsRoute

/** S17 지역 도감. */
@Serializable data object CollectionRoute

/** S18 지역 카드 상세. */
@Serializable data class CardDetailRoute(val cityId: String)
