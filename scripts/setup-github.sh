#!/usr/bin/env bash
# 라벨·마일스톤·main 보호 규칙을 한 번에 만든다.
# 사용법: gh auth login 후, 저장소 폴더에서  ./scripts/setup-github.sh
# 여러 번 실행해도 된다 (라벨은 --force로 덮어씀, 마일스톤은 이미 있으면 건너뜀).
set -euo pipefail

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "대상 저장소: $REPO"

# ---------- 라벨 (기술명세 9장) ----------
label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null && echo "  label $1"; }

echo "라벨 생성"
label "area:auth"  "1d76db" "사용자 (권민찬)"
label "area:team"  "1d76db" "팀 (권민찬)"
label "area:run"   "0e8a16" "러닝 측정 (최한솔)"
label "area:rec"   "5319e7" "러닝 기록"
label "area:jrn"   "5319e7" "여정·지도"
label "area:race"  "5319e7" "스테이지 경쟁 (오영훈)"
label "area:stat"  "5319e7" "주간 통계 (오영훈)"
label "area:ext"   "c5def5" "확장 기능"
label "area:infra" "bfdadc" "빌드·CI·설정"
label "P0"         "b60205" "필수, 11/23까지"
label "P1"         "fbca04" "확장, 11/30까지"
label "feature"    "a2eeef" "새 기능·화면"
label "bug"        "d73a4a" "버그"
label "chore"      "ededed" "빌드·의존성·리소스"
label "docs"       "0075ca" "문서"
label "blocked"    "000000" "다른 사람 작업을 기다리는 중 (본문에 무엇을)"

# ---------- 마일스톤 (마감은 한국 시간 그날 23:59) ----------
echo "마일스톤 생성"
existing=$(gh api "repos/$REPO/milestones?state=all&per_page=100" -q '.[].title')
milestone() {
  if grep -qxF "$1" <<<"$existing"; then echo "  skip $1"; return; fi
  gh api "repos/$REPO/milestones" -f title="$1" -f due_on="$2" -f description="$3" >/dev/null && echo "  milestone $1"
}
milestone "M1 뼈대 연결"       "2026-11-09T14:59:59Z" "로그인 → 홈 → 러닝 → runs 저장 (v0.1.0)"
milestone "M2 팀·저장 연결"    "2026-11-16T14:59:59Z" "팀 거리 합산, 실기기 30분 측정, 규칙 1차 (v0.2.0)"
milestone "M3 P0 완성"         "2026-11-23T14:59:59Z" "P0 전부, 규칙 시나리오 14개, 통합 체크 1차 (v0.3.0)"
milestone "M4 확장 완성"       "2026-11-30T14:59:59Z" "EXT-01~03, 야외 러닝, 통합 체크 2차 (v0.9.0)"
milestone "M5 시연"            "2026-12-08T14:59:59Z" "12/07 리허설 통과, APK 릴리스 (v1.0.0)"

# ---------- main 보호 규칙 ----------
# CI 작업 이름(domain-test, app-build)이 한 번은 실행돼야 GitHub가 체크 이름을 인식한다.
# 비공개 저장소는 무료 플랜에서 보호 규칙이 막힐 수 있다 (공개 저장소 또는 GitHub Team/교육용 플랜 필요).
echo "main 보호 규칙"
gh api -X PUT "repos/$REPO/branches/main/protection" --input - <<'JSON' >/dev/null && echo "  done"
{
  "required_status_checks": { "strict": true, "contexts": ["domain-test", "app-build"] },
  "enforce_admins": false,
  "required_pull_request_reviews": { "required_approving_review_count": 1, "dismiss_stale_reviews": true },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON

# ---------- 병합 방식: Squash만, 합친 뒤 브랜치 삭제 ----------
gh api -X PATCH "repos/$REPO" \
  -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false \
  -F delete_branch_on_merge=true >/dev/null && echo "Squash merge 설정 완료"
