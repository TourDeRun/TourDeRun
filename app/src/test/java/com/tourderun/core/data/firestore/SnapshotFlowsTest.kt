package com.tourderun.core.data.firestore

import com.google.firebase.firestore.ListenerRegistration
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 리스너 Flow의 오류 처리. `FirebaseFirestoreException`·`Code`는 JVM 테스트에서 만들 수 없어서
 * 같은 번호(gRPC 상태 코드)를 가진 가짜 오류로 [listenerFlow]를 직접 시험한다.
 */
class SnapshotFlowsTest {

    /** 가짜 스냅샷: 값과 캐시 출처. */
    private data class Snap(val value: Int, val fromCache: Boolean = false)

    /** 가짜 Firestore 오류. [code]는 `FirebaseFirestoreException.Code.value()`와 같은 번호. */
    private class FakeError(val code: Int) : Exception()

    private class Recorded {
        val errors = mutableListOf<Pair<Int, Boolean>>()
        var removed = false
    }

    /** 리스너를 붙이자마자 [events]를 차례로 보내는 가짜 등록. */
    private fun listen(vararg events: Pair<Snap?, FakeError?>, rec: Recorded = Recorded()) =
        listenerFlow<Snap, FakeError>(
            register = { onEvent ->
                events.forEach { (s, e) -> onEvent(s, e) }
                ListenerRegistration { rec.removed = true }
            },
            isFromCache = { it.fromCache },
            toDataError = { dataErrorOf(it.code) },
            onError = { e, afterValue -> rec.errors += e.code to afterValue },
        )

    @Test fun `권한 거절·로그인 필요 → PermissionDenied`() {
        assertEquals(DataError.PermissionDenied, dataErrorOf(GrpcCode.PERMISSION_DENIED))
        assertEquals(DataError.PermissionDenied, dataErrorOf(GrpcCode.UNAUTHENTICATED))
    }

    @Test fun `연결 불가·시간 초과 → Unavailable`() {
        assertEquals(DataError.Unavailable, dataErrorOf(GrpcCode.UNAVAILABLE))
        assertEquals(DataError.Unavailable, dataErrorOf(GrpcCode.DEADLINE_EXCEEDED))
    }

    @Test fun `인덱스 누락 등 그 밖 → Other`() {
        assertEquals(DataError.Other, dataErrorOf(GrpcCode.FAILED_PRECONDITION))
    }

    @Test fun `값과 캐시 출처를 Ready로 낸다`() = runBlocking {
        val values = listen(Snap(1) to null, Snap(2, fromCache = true) to null, null to FakeError(GrpcCode.UNAVAILABLE)).toList()
        assertEquals(listOf(Loaded.Ready(Snap(1), false), Loaded.Ready(Snap(2, fromCache = true), true)), values)
    }

    @Test fun `첫 값 전 권한 거절 → Failed 한 번 내고 끝`() = runBlocking {
        val rec = Recorded()
        val values = listen(null to FakeError(GrpcCode.PERMISSION_DENIED), rec = rec).toList()
        assertEquals(listOf(Loaded.Failed(DataError.PermissionDenied)), values)
        assertEquals(listOf(GrpcCode.PERMISSION_DENIED to false), rec.errors)
        assertTrue("끝나면 리스너를 뗀다", rec.removed)
    }

    @Test fun `값을 받은 뒤 오류면 Failed 없이 마지막 값만 남고 로그`() = runBlocking {
        val rec = Recorded()
        val values = listen(Snap(7) to null, null to FakeError(GrpcCode.PERMISSION_DENIED), rec = rec).toList()
        assertEquals(listOf(Loaded.Ready(Snap(7), false)), values)
        assertEquals(listOf(GrpcCode.PERMISSION_DENIED to true), rec.errors)
    }

    @Test fun `오류 뒤에 온 스냅샷은 버린다`() = runBlocking {
        val values = listen(null to FakeError(GrpcCode.UNAVAILABLE), Snap(3) to null).toList()
        assertEquals(listOf(Loaded.Failed(DataError.Unavailable)), values)
    }
}
