package com.tourderun.core.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tourderun.R
import com.tourderun.core.ui.Format

/**
 * "천안까지 0.1km" 문구. 남은 거리는 올림이라 도착 전에는 0.0km가 나오지 않는다(84,960m → 0.1km).
 * 완주면 "부산 도착 · 완주".
 */
@Composable
fun StageProgressUi.toNextCityText(): String =
    if (finished) {
        stringResource(R.string.stage_finished, toCity)
    } else {
        stringResource(R.string.to_next_city, toCity, Format.kmUp(toNextM))
    }
