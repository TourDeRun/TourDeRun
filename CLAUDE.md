# Tour De Run

팀원이 달린 거리를 합쳐 서울 → 부산 경로(8개 도시, 7개 스테이지, 350km)를 여행하고, 같은 스테이지의 팀끼리 경쟁하는 Android 러닝 앱. 캡스톤 팀 프로젝트(4명), 12/08 시연.

- 기준 문서: **기능 명세서 v2.1**(기능 ID, 화면 ID, 미결 사항 M-01~07)과 **기술 명세서**(구조·스키마·보안 규칙·컨벤션). 동작은 기능 명세, 구현 방식은 기술 명세가 기준이고 둘이 다르면 코드를 바꾸기 전에 사용자에게 알린다.
- 답변과 커밋 메시지, 코드 주석은 한국어로 쓴다.

## 명령

```
./gradlew :domain:test                       # 핵심 로직 단위 테스트 (PR 전에 반드시)
./gradlew :app:assembleDebug :app:lintDebug  # CI app-build와 같은 검사
./gradlew signingReport                      # 디버그 SHA-1 확인
```

- CI(`.github/workflows/ci.yml`)는 `domain-test`, `app-build` 두 작업. 둘 다 통과해야 `main`에 합칠 수 있다.
- `google-services.json`이 없으면 Firebase 플러그인이 꺼진 채로 빌드된다. 이 상태에서 Firebase를 호출하는 코드가 실행되면 앱이 꺼지므로, 파일 없이 Firebase 코드를 시작 경로에 연결하지 않는다.

## 구조

| 모듈 | 내용 | 의존 |
| --- | --- | --- |
| `:domain` | 순수 Kotlin. 모델, 경로 상수, 순수 함수(필터·스테이지·순위·통계·검사) | 없음 |
| `:app` | 화면, ViewModel, Repository, Firestore·Room·DataStore, 서비스, Hilt | `:domain` |

- 의존은 `ui → data → domain` 한 방향. **`:domain`에 `android.*`, Firebase, Compose import 금지.**
- `:domain` 패키지: `model/`, `route/`(`routeFor(season)`, `Season1Route`), `run/`, `journey/`(`locate`·`arrivals`·`newArrivals`·`mapPosition`·`haversineM`), `race/`, `stats/`, `team/`, `card/`
- `:app` 패키지: `core/di`, `core/data/firestore`(`Fields.kt`, DTO, `callbackFlow` 확장), `core/data/local`, `core/ui/theme|component|format`, `core/navigation`, `feature/<영역>/{ui,data}`
- 다른 feature의 `ui`·`data` 내부 클래스를 import하지 않는다. 여러 feature가 쓰는 Repository는 `core/data`로 올린다.
- 공유 Repository: `SessionRepository`·`TeamRepository`(권민찬), `RaceRepository`·`StatsRepository`·`RunRepository`·`RunSaver`(오영훈), `RunSessionManager`(최한솔)

## 도메인 규칙 (자주 틀리는 것)

- **단위**: Firestore와 도메인 경계의 거리는 **정수 m(`Long`)**, 측정 중 누적만 `Double` m(저장 직전 `roundToLong()`). 시각은 epoch ms(`Long`) 또는 `Instant`, 시간 길이는 초(`Long`). 이름에 단위 접미어(`distanceM`, `movingSec`, `startedAtMs`, `paceSecPerKm`).
- **경로**: 구간 거리는 고정값 19/29/37/40/56/81/88km(누적 19/48/85/125/181/262/350). 좌표로 다시 계산하지 않는다. 도시 ID `seoul, anyang, osan, cheonan, sejong, yeongdong, daegu, busan`은 지역 카드와 공용.
- **스테이지 경계**: 시작 이상, 끝 미만. 19000m = 스테이지 2의 시작, 18999m = 스테이지 1의 끝, 350000m 이상 = 완주(위치는 부산에 멈추고 거리는 계속 쌓인다). 스테이지 번호는 화면 1부터, 코드 0부터.
- **도착 수**: 팀 거리 이하인 누적 거리를 가진 도시 수(서울 제외, 0~7).
- **거리 계산**은 `haversineM()` 하나만(지구 반지름 6,371,000m). `Location.distanceTo` 금지.
- **현재 시각을 함수 안에서 읽지 않는다**(`System.currentTimeMillis()` 금지). 항상 인자로 받는다.
- **순위**: 거리 내림차순, 같으면 `reachedAt`이 이른 팀이 앞. 스테이지 안에 앞·뒤 팀이 없으면 다른 스테이지에서 거리가 가장 가까운 팀.
- **상위 %** = `ceil((더 많이 달린 팀 수 + 1) / 전체 팀 수 × 100)`. 전체 팀 수는 시즌 1 팀 전부(이번 주 0km 팀 포함). 우리 팀이 0km면 숨긴다.
- **주간 ID**: 한국 시간, 월요일 0시 시작, ISO 주차 연도(`IsoFields`). 러닝 **시작 시각** 기준. 2027-01-01 → `2026-W53`.
- **팀 거리는 한 번 쌓이면 줄지 않는다**(M-01·M-02 기본안): 기록 삭제·팀 탈퇴·내보내기 때 팀 거리와 통계를 빼지 않는다. 나간 팀원 몫은 "이전 팀원"으로 표시.
- 팀은 1~4명, 팀이 있어야 러닝 가능. 팀장 승계는 가장 먼저 가입한 팀원(M-04 기본안). 최소 저장 기준 100m 또는 1분(M-05).

## Firestore

- 컬렉션: `users/{uid}`, `teams/{teamId}`, `teams/{teamId}/members/{uid}`, `inviteCodes/{code}`, `runs/{runId}`, `teamStats/{teamId}_{periodId}`. 필드 이름은 **`core/data/firestore/Fields.kt` 상수만** 쓴다(문자열 직접 입력 금지).
- 현재 스테이지는 저장하지 않는다(팀 거리로 계산).
- **REC-01 저장은 배치 쓰기 한 번**: `runs` 생성 → `teams`(`distanceM` increment, `reachedAt` serverTimestamp, `lastRunId`) → 내 `members`(`contributionM`, `lastRunId`) → `teamStats` 이번 주 문서(set merge: `distanceM`, `runCount`, `memberM.{uid}`, `lastRunId`, `teamId`, `season`, `periodId`) → 내 `users`(`totalM`, `runCount`). 숫자는 모두 `FieldValue.increment`.
- **오프라인 대기 쓰기는 `await()`하지 않는다**(REC-01, 제목·메모 수정, 기록 삭제). 커밋을 시작하고 바로 화면을 넘긴다. 결과 화면 값은 기기에서 계산(`before = 캐시된 팀 거리`, `after = before + distanceM`).
- 보안 규칙이 `lastRunId`로 증가량을 같은 배치의 새 러닝과 대조한다. 거리 증가 쓰기에는 항상 `lastRunId`를 함께 쓴다.
- 문서를 만들 때 null 필드도 명시한다(`teamId: null`, `lastRunId: null`).
- `runs` 목록 조회는 반드시 `whereEqualTo(uid)`를 넣는다(규칙은 필터가 아니다). 러닝 ID는 기기에서 `collection("runs").document().id`로 미리 만든다.
- 팀 참여(TEAM-03)는 트랜잭션(온라인 필수), 팀 만들기는 배치(`teams`·`members`·`inviteCodes`·`users`를 한 배치에).
- 실시간 리스너는 `callbackFlow` + `awaitClose { remove() }`, ViewModel에서 `stateIn(viewModelScope, WhileSubscribed(5_000), 초기값)`. 같은 스테이지 리스너는 스테이지 번호가 바뀔 때만 다시 연결.
- `firestore.rules`·인덱스 배포는 권민찬만 한다. 규칙·`Fields.kt`를 바꾸는 PR의 리뷰어는 권민찬 + 오영훈.

## 코드 컨벤션

- 화면: `XxxScreen`(ViewModel 받음) / `XxxContent(state, onAction)`(상태 없음, Preview 대상). `XxxViewModel`, `XxxUiState`(불변 data class 또는 sealed `Loading/Content/Error`), `XxxAction`, 일회성 이벤트는 `Channel<XxxEvent>`.
- Repository: 인터페이스 `XxxRepository`, 구현 `FirestoreXxxRepository`. 실패는 예외로 던지지 않고 `Result<T>` 또는 `sealed interface DataError`로. 예외를 잡는 곳은 Repository 한 층.
- Compose: 상태 수집은 `collectAsStateWithLifecycle()`. 공개 컴포저블은 `modifier: Modifier = Modifier`를 첫 번째 선택 인자로. 색·글자·간격은 테마 토큰(`MaterialTheme`, `Dimens`), `Color(0xFF…)`·`16.dp` 직접 사용 금지. 문자열은 `strings.xml`(feature 접두어 `run_`, `team_` …). 숫자 표시는 `core/ui/format`에서만. 화면마다 `@Preview`(기본 + 빈 상태).
- ViewModel은 `Context`·Activity·Compose 타입을 받지 않는다.
- 코루틴: `GlobalScope`·`runBlocking` 금지(테스트 제외). 디스패처는 Hilt 주입(`@IoDispatcher`), `Dispatchers.IO` 직접 사용 금지.
- 금지: `!!`(테스트 제외, 대신 `requireNotNull(x) { "이유" }`), 위치 좌표·uid 로그, 주석 처리된 코드 커밋. 로그 태그 `TDR/<feature>`. TODO는 `// TODO(RUN-07): …`처럼 기능 ID를 붙인다.
- 버전은 `gradle/libs.versions.toml`에서만. 버전 변경은 `chore` PR 단독으로. **AGP·Kotlin 업그레이드 제안은 따르지 않는다**(팀이 정할 때까지 고정).

## 테스트

- `:domain` 순수 함수는 JUnit 4 테스트 필수. 이름은 백틱 한글 "조건 → 결과": ``@Test fun `19000m는 스테이지 2의 시작`()``.
- 기능 명세의 테스트 예시는 전부 테스트로 옮긴다(경계값, 동점, 팀이 하나뿐인 경우, 주차 연도 경계 등).
- 테스트 대상 담당: GPS 필터·페이스(최한솔), `locate`·도착 수·`newArrivals`·`rankInStage`·`topPercent`·`weekId`(오영훈), 초대 코드·닉네임 검사·기여 비율·MVP(권민찬).

## Git

- GitHub Flow. `main` 직접 push 불가, PR + 리뷰 1명 + CI 통과, **Squash merge**.
- 브랜치 `<type>/<기능ID>-<짧은-설명>`(예: `feat/RACE-01-rank-in-stage`, `chore/setup-hilt`). type: `feat fix refactor test docs chore style`.
- 커밋·PR 제목 `<type>(<기능ID>): <한국어 요약, 50자 이내, 마침표 없음>`. 기능 ID가 없으면 영역(`chore(build): …`). PR 본문은 `.github/pull_request_template.md`를 채운다.
- PR은 변경 400줄 이하 권장. 연결 지점(공유 시그니처, `Fields.kt`, 규칙)을 바꾸면 영향받는 담당자를 리뷰어로.
- `AppNavHost.kt`·`Routes.kt`는 전병국 관리(라우트 한 줄 추가만). `strings.xml`은 자기 영역 블록에만 추가.
- **커밋하면 안 되는 것**: `local.properties`, `app/google-services.json`, `*.jks`, `*.keystore`, `CLAUDE.local.md`.

## 담당 영역

| 이름 | 개발 영역 | 소유 코드 |
| --- | --- | --- |
| 권민찬 (PM, Firebase) | 사용자·팀 | `feature/auth`, `feature/team`, `SessionRepository`, `TeamRepository`, `firebase/`, `domain/team`, EXT-03 |
| 오영훈 (GitHub 관리) | 여정·경쟁 엔진 | `:domain`(경로·`locate`·`rankInStage`·`topPercent`·`weekId`), `RunSaver`, `RunRepository`, `RaceRepository`, `StatsRepository`, S15·S16, EXT-01 획득 판정 |
| 전병국 (QA·시연) | 지도·기록·UI | S07 홈, S08 로드뷰, S10~S12, `core/navigation`, `core/ui/theme`, 지도 공용 컴포저블, S17·S18 |
| 최한솔 (디자인·발표) | 러닝 측정 | S09, `GpsDistanceFilter`·페이스(`:domain/run`), `RunSessionManager`, `RunTrackingService`, Room·DataStore, EXT-02 |

다른 사람의 소유 코드를 고쳐야 하면 바꾸기 전에 사용자에게 알리고, PR에서 소유자를 리뷰어로 지정한다.

## 개발 환경 메모 (Windows)

- Gradle JDK는 21(Settings → Build Tools → Gradle). JDK 25로는 Gradle 8.11.1이 돌지 않는다.
- Windows 사용자 이름이 한글이면 `GRADLE_USER_HOME`을 영어 경로(예: `C:\Gradle`)로 둬야 `:domain:test`가 돈다. AVD도 `ANDROID_USER_HOME`으로 영어 경로에 둔다.
- 에뮬레이터가 창 없이 꺼지고 "too many emulator instances"가 나오면 `netsh interface ipv4 show excludedportrange protocol=tcp`로 5554~5585 예약 여부를 확인한다.
