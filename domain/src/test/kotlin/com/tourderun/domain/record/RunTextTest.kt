package com.tourderun.domain.record

import org.junit.Assert.assertEquals
import org.junit.Test

class RunTextTest {
    private fun chars(n: Int) = "가".repeat(n)

    private fun errorsOf(title: String, memo: String? = null): Set<RunTextError> =
        when (val r = checkRunText(title, memo)) {
            is RunTextCheck.Valid -> emptySet()
            is RunTextCheck.Invalid -> r.errors
        }

    @Test fun `0자 제목 → 빈 제목 오류`() {
        assertEquals(setOf(RunTextError.TITLE_BLANK), errorsOf(""))
    }

    @Test fun `공백만 있는 제목 → 빈 제목 오류`() {
        assertEquals(setOf(RunTextError.TITLE_BLANK), errorsOf("   \t "))
    }

    @Test fun `1자 제목 → 통과`() {
        assertEquals(RunTextCheck.Valid("가", null), checkRunText(chars(1), null))
    }

    @Test fun `30자 제목 → 통과`() {
        assertEquals(RunTextCheck.Valid(chars(30), null), checkRunText(chars(30), null))
    }

    @Test fun `31자 제목 → 너무 긴 제목 오류`() {
        assertEquals(setOf(RunTextError.TITLE_TOO_LONG), errorsOf(chars(31)))
    }

    @Test fun `앞뒤 공백은 지우고 센다`() {
        assertEquals(RunTextCheck.Valid(chars(30), null), checkRunText("  ${chars(30)}  ", null))
    }

    @Test fun `이모지는 한 글자로 센다`() {
        val title = "🏃".repeat(30)
        assertEquals(RunTextCheck.Valid(title, null), checkRunText(title, null))
    }

    @Test fun `0자 메모 → 통과하고 null로 저장`() {
        assertEquals(RunTextCheck.Valid("제목", null), checkRunText("제목", ""))
    }

    @Test fun `공백만 있는 메모 → null로 저장`() {
        assertEquals(RunTextCheck.Valid("제목", null), checkRunText("제목", "  \n "))
    }

    @Test fun `200자 메모 → 통과`() {
        assertEquals(RunTextCheck.Valid("제목", chars(200)), checkRunText("제목", chars(200)))
    }

    @Test fun `201자 메모 → 너무 긴 메모 오류`() {
        assertEquals(setOf(RunTextError.MEMO_TOO_LONG), errorsOf("제목", chars(201)))
    }

    @Test fun `제목과 메모가 모두 틀리면 오류 두 개`() {
        assertEquals(
            setOf(RunTextError.TITLE_BLANK, RunTextError.MEMO_TOO_LONG),
            errorsOf(" ", chars(201)),
        )
    }
}
