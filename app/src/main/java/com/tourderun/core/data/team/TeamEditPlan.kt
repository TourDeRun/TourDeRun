package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.WriteMode

/**
 * TEAM-05 팀 이름·대표 이미지 수정. `teams/{teamId}`의 name·icon·color 세 필드만 바꾼다
 * (보안 규칙 `onlyChanged(['name','icon','color'])`, 팀장만).
 *
 * @param icon 0~7 (`TeamIcon.ordinal`), @param color 0~7 (`TeamColor.ordinal`) (M-06)
 */
fun teamEditPlan(teamId: String, name: String, icon: Int, color: Int): List<DocWrite> = listOf(
    DocWrite(
        path = "${Collections.TEAMS}/$teamId",
        mode = WriteMode.UPDATE,
        fields = mapOf(TeamFields.NAME to name, TeamFields.ICON to icon, TeamFields.COLOR to color),
    ),
)
