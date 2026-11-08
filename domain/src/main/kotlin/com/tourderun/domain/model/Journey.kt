package com.tourderun.domain.model

/** 위경도 값. Android LatLng를 :domain에서 쓸 수 없어 따로 둔다. :app에서 LatLng로 변환한다. */
data class LatLngValue(val lat: Double, val lng: Double)

/** 경로 위 도시. id는 지역 카드(EXT-01)와 공용 (seoul, anyang, ...). */
data class City(
    val id: String,
    val nameKo: String,
    val lat: Double,
    val lng: Double,
)

/** 두 도시 사이 한 구간. 길이는 좌표로 다시 계산하지 않는 고정값 (기능 명세 8장 JRN-01). */
data class Stage(
    val from: City,
    val to: City,
    val lengthKm: Int,
) {
    val lengthM: Long get() = lengthKm * 1_000L
}

/**
 * 시즌 하나의 경로.
 * cities[0] = 출발 도시, cities[i] = i번째 도착 도시. stages.size == cities.size - 1.
 */
data class Route(
    val season: Int,
    val cities: List<City>,
    val stages: List<Stage>,
) {
    init {
        require(stages.isNotEmpty()) { "스테이지가 하나 이상 있어야 한다" }
        require(cities.size == stages.size + 1) { "도시 수는 스테이지 수 + 1이어야 한다" }
    }

    /** 각 도착 도시의 누적 거리(m). cumulativeEndM[i] = 스테이지 i의 끝. */
    val cumulativeEndM: List<Long> = stages.runningFold(0L) { acc, s -> acc + s.lengthM }.drop(1)

    val totalM: Long get() = cumulativeEndM.last()

    /** 스테이지 i의 시작 누적 거리(m). */
    fun stageStartM(stageIndex: Int): Long = if (stageIndex == 0) 0L else cumulativeEndM[stageIndex - 1]

    fun stageEndM(stageIndex: Int): Long = cumulativeEndM[stageIndex]
}

/**
 * 팀 거리로 계산한 현재 위치 (JRN-02).
 *
 * @property stageIndex 0부터. 화면에는 +1 해서 보여준다. 완주면 마지막 스테이지.
 * @property progress 스테이지 안 진행률 0.0 ~ 1.0
 * @property toNextM 다음 도시까지 남은 거리(m). 완주면 0
 * @property arrivals 도착한 도시 수 0 ~ 스테이지 수 (출발 도시 제외)
 */
data class TeamPosition(
    val stageIndex: Int,
    val progress: Double,
    val toNextM: Long,
    val arrivals: Int,
    val finished: Boolean,
)
