package com.tourderun.core.data.session

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 로그인한 내 uid. FirebaseAuth를 직접 부르지 않고 이 인터페이스로 받는다.
 *
 * TODO(JRN-05): `SessionRepository`(권민찬, #62)가 머지되면 `state`의 `uid`로 바꾸고 이 인터페이스와 [NoUidSource]는 지운다.
 */
interface MyUidSource {
    /** 내 uid. 로그인 전이거나 아직 모르면 null. */
    fun observeUid(): Flow<String?>
}

/** 로그인이 아직 없어서 항상 null(로그인 전과 같다). 이 값이면 도착 연출을 띄우지 않고 확인 쓰기도 하지 않는다. */
class NoUidSource @Inject constructor() : MyUidSource {
    override fun observeUid(): Flow<String?> = flowOf(null)
}
