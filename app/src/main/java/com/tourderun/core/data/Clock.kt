package com.tourderun.core.data

import javax.inject.Inject

/**
 * 현재 시각(epoch ms). ViewModel·Repository는 `System.currentTimeMillis()`를 직접 부르지 않고 이것을 주입받는다.
 * 테스트·Preview에서는 고정 시각 구현을 넣는다.
 */
fun interface Clock {
    fun nowMs(): Long
}

/** 기기 시계. 앱에서 현재 시각을 읽는 곳은 여기 하나다. */
class SystemClock @Inject constructor() : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
