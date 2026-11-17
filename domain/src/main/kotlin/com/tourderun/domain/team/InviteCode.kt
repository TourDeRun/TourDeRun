package com.tourderun.domain.team

import kotlin.random.Random

/** 초대 코드 길이 (TEAM-02). */
const val INVITE_CODE_LENGTH = 6

/** 초대 코드에 쓰는 글자 32개. 헷갈리는 0·O·1·I는 뺐다 (TEAM-02). */
const val INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/**
 * 새 초대 코드를 만든다. 같은 코드가 이미 있는지는 Repository가 확인하고, 있으면 다시 부른다.
 *
 * @param random 무작위 값. 테스트에서 결과를 고정하려고 인자로 받는다 (함수 안에서 시각·난수를 직접 만들지 않는다)
 */
fun generateInviteCode(random: Random): String {
    return (1..INVITE_CODE_LENGTH)
        .map { INVITE_CODE_CHARS[random.nextInt(INVITE_CODE_CHARS.length)] }
        .joinToString("")
}

/** 입력한 코드를 비교할 모양으로 바꾼다: 공백(띄어쓰기·탭·줄바꿈)을 위치와 상관없이 모두 지우고 대문자로 (TEAM-03). 붙여 넣기에서 "ABC 234"처럼 공백이 섞이는 경우를 받아 준다. */
fun normalizeInviteCode(input: String): String {
    return input.filterNot { it.isWhitespace() }.uppercase()
}

/** 6자리이고 허용된 글자만 쓰였는지. 서버에 묻기 전에 형식부터 거른다 (TEAM-03). */
fun isInviteCodeFormat(code: String): Boolean {
    return code.length == INVITE_CODE_LENGTH && code.all { it in INVITE_CODE_CHARS }
}