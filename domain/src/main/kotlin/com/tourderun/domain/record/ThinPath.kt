package com.tourderun.domain.record

/** `runs.path` 최대 점 수 (기술명세 4장). */
const val MAX_PATH_POINTS = 5_000

/**
 * 점이 [max]개를 넘으면 처음·끝을 포함해 고르게 [max]개만 남긴다. 순서는 유지한다.
 * [max]개 이하면 그대로 돌려준다.
 */
fun <T> thinEvenly(items: List<T>, max: Int = MAX_PATH_POINTS): List<T> {
    require(max >= 2) { "max는 2 이상: $max" }
    if (items.size <= max) return items
    val last = (items.size - 1).toLong()
    val steps = (max - 1).toLong()
    // i번째 = round(i × last / steps). 정수 연산이라 처음(0)과 끝(last)이 정확히 남는다.
    return List(max) { i -> items[((i * last * 2 + steps) / (steps * 2)).toInt()] }
}
