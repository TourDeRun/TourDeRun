package com.tourderun.domain.team

/** 닉네임 검사 실패 이유 (AUTH-02). */
enum class NicknameError {
    TOO_SHORT,      // 2자 미만 (빈 값 포함)
    TOO_LONG,       // 10자 초과
    INVALID_CHARS,  // 한글 완성형·영문·숫자 이외 (공백, 'ㅋ', 이모지, 기호)
}

const val NICKNAME_MIN_LENGTH = 2
const val NICKNAME_MAX_LENGTH = 10

/**
 * AUTH-02 닉네임 규칙: 2~10자, 한글(완성형)·영문·숫자만, 공백 불가. 중복은 허용한다.
 * @return 통과하면 null, 실패하면 이유
 */
fun validateNickname(input: String): NicknameError? {
    return when {
        // 1) 허용하지 않는 글자가 하나라도 있으면
        input.any { !it.isNicknameChar() } -> NicknameError.INVALID_CHARS

        // 2) 최소 길이보다 짧으면
        input.length < NICKNAME_MIN_LENGTH -> NicknameError.TOO_SHORT

        // 3) 최대 길이보다 길면
        input.length > NICKNAME_MAX_LENGTH -> NicknameError.TOO_LONG

        // 4) 전부 통과
        else -> null
    }
}

private fun Char.isNicknameChar(): Boolean {
    return this in '가'..'힣' ||
            this in 'a'..'z' ||
            this in 'A'..'Z' ||
            this in '0'..'9'
}
