package com.tourderun.domain.route

import com.tourderun.domain.model.City
import com.tourderun.domain.model.Route
import com.tourderun.domain.model.Stage

/** 시즌별 경로. 지금은 시즌 1(서울 → 부산)만 있다. */
fun routeFor(season: Int): Route = when (season) {
    1 -> Season1Route.route
    else -> throw IllegalArgumentException("지원하지 않는 시즌: $season")
}

/**
 * 시즌 1: 서울 → 부산, 8개 도시, 7개 스테이지, 350km (기능 명세 8장 JRN-01).
 *
 * 좌표는 각 시·군청 근처의 대략적인 값이다. 지도 표시에만 쓰이고, 구간 거리는 아래 고정값을 쓴다.
 * TODO(JRN-01): 오영훈이 좌표를 최종 확인한다 (지도에서 마커 위치 확인).
 */
object Season1Route {
    val SEOUL = City("seoul", "서울", 37.5665, 126.9780)
    val ANYANG = City("anyang", "안양", 37.3943, 126.9568)
    val OSAN = City("osan", "오산", 37.1498, 127.0772)
    val CHEONAN = City("cheonan", "천안", 36.8151, 127.1139)
    val SEJONG = City("sejong", "세종", 36.4800, 127.2890)
    val YEONGDONG = City("yeongdong", "영동", 36.1750, 127.7834)
    val DAEGU = City("daegu", "대구", 35.8714, 128.6014)
    val BUSAN = City("busan", "부산", 35.1796, 129.0756)

    val route: Route = Route(
        season = 1,
        cities = listOf(SEOUL, ANYANG, OSAN, CHEONAN, SEJONG, YEONGDONG, DAEGU, BUSAN),
        stages = listOf(
            Stage(SEOUL, ANYANG, 19),
            Stage(ANYANG, OSAN, 29),
            Stage(OSAN, CHEONAN, 37),
            Stage(CHEONAN, SEJONG, 40),
            Stage(SEJONG, YEONGDONG, 56),
            Stage(YEONGDONG, DAEGU, 81),
            Stage(DAEGU, BUSAN, 88),
        ),
    )
}
