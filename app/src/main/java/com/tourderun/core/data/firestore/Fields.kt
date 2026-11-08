package com.tourderun.core.data.firestore

/**
 * Firestore 컬렉션·필드 이름 (기술명세 4장이 기준).
 * 코드에서 필드 이름 문자열을 직접 쓰지 않는다. 변경은 4·5장 수정과 같은 PR에서만, 리뷰어 권민찬 + 오영훈.
 * 거리는 정수 m(Long), 시각은 Timestamp, 시간 길이는 초(Long).
 */
object Collections {
    const val USERS = "users"
    const val TEAMS = "teams"
    const val MEMBERS = "members"          // teams/{teamId}/members/{uid}
    const val INVITE_CODES = "inviteCodes"
    const val RUNS = "runs"
    const val TEAM_STATS = "teamStats"     // 문서 ID: {teamId}_{periodId}
}

object UserFields {
    const val NICKNAME = "nickname"
    const val CREATED_AT = "createdAt"
    const val TEAM_ID = "teamId"           // 없으면 null을 명시해서 쓴다
    const val TOTAL_M = "totalM"
    const val RUN_COUNT = "runCount"
    const val CARDS = "cards"              // cards.{cityId} = { at, teamName } (EXT-01)
    const val CARD_AT = "at"
    const val CARD_TEAM_NAME = "teamName"
}

object TeamFields {
    const val NAME = "name"
    const val ICON = "icon"
    const val COLOR = "color"
    const val MEMBER_COUNT = "memberCount"
    const val SEASON = "season"
    const val DISTANCE_M = "distanceM"
    const val REACHED_AT = "reachedAt"
    const val LAST_RUN_ID = "lastRunId"
}

object MemberFields {
    const val NICKNAME = "nickname"
    const val ROLE = "role"
    const val JOINED_AT = "joinedAt"
    const val INVITE_CODE = "inviteCode"
    const val CONTRIBUTION_M = "contributionM"
    const val SEEN_ARRIVALS = "seenArrivals"
    const val LAST_RUN_ID = "lastRunId"

    const val ROLE_LEADER = "leader"
    const val ROLE_MEMBER = "member"
}

object InviteCodeFields {
    const val TEAM_ID = "teamId"
    const val CREATED_AT = "createdAt"
}

object RunFields {
    const val UID = "uid"
    const val TEAM_ID = "teamId"
    const val STARTED_AT = "startedAt"
    const val ENDED_AT = "endedAt"
    const val MOVING_SEC = "movingSec"
    const val DISTANCE_M = "distanceM"
    const val PACE_SEC_PER_KM = "paceSecPerKm"
    const val PERIOD_ID = "periodId"
    const val TITLE = "title"
    const val MEMO = "memo"
    const val PATH = "path"                // List<GeoPoint>, 최대 5,000개
}

object TeamStatFields {
    const val TEAM_ID = "teamId"
    const val SEASON = "season"
    const val PERIOD_ID = "periodId"
    const val DISTANCE_M = "distanceM"
    const val RUN_COUNT = "runCount"
    const val MEMBER_M = "memberM"         // memberM.{uid}
    const val LAST_RUN_ID = "lastRunId"

    fun docId(teamId: String, periodId: String) = "${teamId}_$periodId"
    fun memberKey(uid: String) = "$MEMBER_M.$uid"
}
