package com.tourderun.core.ui

import java.util.Locale
import kotlin.math.abs

/** 화면에 보이는 숫자 모양. 소수점은 항상 '.'(Locale.US). */
object Format {
    /** 66_800 → "66.8" */
    fun km(meters: Long, decimals: Int = 1): String =
        String.format(Locale.US, "%.${decimals}f", meters / 1000.0)

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
