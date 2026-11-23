package com.tourderun.core.data.session

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/**
 * AUTH-03 닉네임 바꾸기. `users/{uid}`와, 팀이 있으면 내 `teams/{teamId}/members/{uid}`의 닉네임을 같이 바꾼다
 * (팀원 목록 S14에도 바로 보이게). 둘 다 닉네임 필드 하나만 바꾼다(보안 규칙 `onlyChanged`).
 */
fun nicknameUpdatePlan(uid: String, teamId: String?, nickname: String): List<DocWrite> {
    val user = DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(UserFields.NICKNAME to nickname),
    )
    if (teamId == null) return listOf(user)
    val member = DocWrite(
        path = "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(MemberFields.NICKNAME to nickname),
    )
    return listOf(user, member)
}
