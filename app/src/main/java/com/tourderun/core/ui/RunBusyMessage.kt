package com.tourderun.core.ui

import androidx.annotation.StringRes
import com.tourderun.R
import com.tourderun.core.data.run.RunBusyReason

/** RUN-02 막힌 이유 → 다음에 할 일을 알려 주는 안내 문구 (S13 로그아웃·탈퇴, S14 팀 나가기 공용). */
@StringRes
fun RunBusyReason.blockedMessageRes(): Int = when (this) {
    RunBusyReason.ACTIVE -> R.string.run_busy_active
    RunBusyReason.SAVING -> R.string.run_busy_saving
    RunBusyReason.SAVE_REJECTED -> R.string.run_busy_rejected
}
