package com.tourderun.domain.record

/** 러닝 제목 최대 글자 수 (REC-03). */
const val RUN_TITLE_MAX_CHARS = 30

/** 러닝 메모 최대 글자 수 (REC-03). */
const val RUN_MEMO_MAX_CHARS = 200

enum class RunTextError { TITLE_BLANK, TITLE_TOO_LONG, MEMO_TOO_LONG }

/** 제목·메모 검사 결과. */
sealed interface RunTextCheck {
    /** 저장할 값. 앞뒤 공백을 지웠고, 메모가 비었으면 null. */
    data class Valid(val title: String, val memo: String?) : RunTextCheck

    /** 화면에서 칸마다 오류를 보여 줄 수 있게 오류를 모두 담는다. */
    data class Invalid(val errors: Set<RunTextError>) : RunTextCheck
}

/**
 * 러닝 제목·메모 검사 (REC-03). 제목 1~30자, 메모 0~200자.
 * 앞뒤 공백을 지운 뒤 센다. 공백만 있는 제목은 빈 제목. 글자 수는 코드 포인트 기준(이모지 하나 = 1자).
 */
fun checkRunText(title: String, memo: String?): RunTextCheck {
    val t = title.trim()
    val m = memo?.trim().orEmpty()
    val errors = buildSet {
        when {
            t.isEmpty() -> add(RunTextError.TITLE_BLANK)
            t.charCount() > RUN_TITLE_MAX_CHARS -> add(RunTextError.TITLE_TOO_LONG)
        }
        if (m.charCount() > RUN_MEMO_MAX_CHARS) add(RunTextError.MEMO_TOO_LONG)
    }
    return if (errors.isEmpty()) {
        RunTextCheck.Valid(title = t, memo = m.ifEmpty { null })
    } else {
        RunTextCheck.Invalid(errors)
    }
}

private fun String.charCount(): Int = codePointCount(0, length)
