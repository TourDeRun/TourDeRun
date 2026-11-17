package com.tourderun.core.data

/** Repository가 화면에 알리는 실패 종류. 화면 문구만 고르면 되므로 최소한으로 나눈다. */
sealed interface DataError {
    /** 보안 규칙 거절·로그인 필요. 다시 시도해도 같을 가능성이 크다. */
    data object PermissionDenied : DataError

    /** 서버에 닿지 못함(네트워크·서버 일시 장애). 연결을 확인하고 다시 시도하면 된다. */
    data object Unavailable : DataError

    /** 그 밖의 실패(인덱스 누락, 잘못된 요청 등). */
    data object Other : DataError
}
