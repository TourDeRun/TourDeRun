package com.tourderun.core.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/** 화면에 보이는 숫자 모양. 소수점은 항상 '.'(Locale.US). */
object Format {
    /** 66_800 → "66.8" */
    fun km(meters: Long, decimals: Int = 1): String =
        String.format(Locale.US, "%.${decimals}f", meters / 1000.0)

    /**
     * 내림. 84_960 → "84.9". 지나온 거리용: 도착하지 않았는데 끝 거리로 보이지 않게 한다.
     */
    fun kmDown(meters: Long, decimals: Int = 1): String = kmRounded(meters, decimals, RoundingMode.FLOOR)

    /**
     * 올림. 40 → "0.1", 0 → "0.0". 남은 거리용: 도착 전에 "0.0"으로 보이지 않게 한다.
     */
    fun kmUp(meters: Long, decimals: Int = 1): String = kmRounded(meters, decimals, RoundingMode.CEILING)

    /**
     * 팀 누적 거리(소수 한 자리, 내림). 84_960 → "84.9", 85_000 → "85.0".
     * 도착 거리(85km)에 닿기 전에 "85.0"이 보이면 "천안까지 0.1km"와 어긋나서 내림으로 쓴다.
     * 개인 러닝 거리(RUN-03)는 [km]을 그대로 쓴다.
     */
    fun teamKm(meters: Long): String = kmDown(meters, decimals = 1)

    private fun kmRounded(meters: Long, decimals: Int, mode: RoundingMode): String =
        BigDecimal.valueOf(meters, 3).setScale(decimals, mode).toPlainString()

    /** 2_400 → "+2.4", -1_100 → "−1.1" (유니코드 빼기 기호). */
    fun signedKm(meters: Long, decimals: Int = 1): String =
        (if (meters >= 0) "+" else "−") + km(abs(meters), decimals)

    /** 1_122 → "0:18:42" */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    /** 346 → "5'46\"". 0 이하면 "-'--\"". */
    fun pace(secondsPerKm: Long): String =
        if (secondsPerKm <= 0) "-'--\"" else String.format(Locale.US, "%d'%02d\"", secondsPerKm / 60, secondsPerKm % 60)
}
