package com.tourderun.domain.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinPathTest {
    @Test fun `5000개 이하면 그대로`() {
        val items = List(5_000) { it }
        assertSame(items, thinEvenly(items))
        assertEquals(emptyList<Int>(), thinEvenly(emptyList<Int>()))
    }

    @Test fun `5001개면 5000개로 줄이고 처음과 끝을 남김`() {
        val items = List(5_001) { it }
        val thinned = thinEvenly(items)
        assertEquals(5_000, thinned.size)
        assertEquals(0, thinned.first())
        assertEquals(5_000, thinned.last())
    }

    @Test fun `줄여도 순서가 유지되고 같은 점이 두 번 나오지 않음`() {
        val thinned = thinEvenly(List(12_345) { it })
        assertEquals(5_000, thinned.size)
        assertTrue(thinned.zipWithNext().all { (a, b) -> a < b })
    }

    @Test fun `고르게 솎음`() {
        // 9개 → 5개: 0, 2, 4, 6, 8
        assertEquals(listOf(0, 2, 4, 6, 8), thinEvenly(List(9) { it }, max = 5))
        // 간격이 최대 1 차이
        val gaps = thinEvenly(List(12_345) { it }).zipWithNext { a, b -> b - a }
        assertTrue(gaps.max() - gaps.min() <= 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `max가 2보다 작으면 예외`() {
        thinEvenly(List(10) { it }, max = 1)
    }
}
