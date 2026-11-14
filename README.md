# TourDeRun

## 개발 환경 준비

저장소에 올리지 않는 파일 두 개를 각자 넣어야 한다.

| 파일 | 내용 | 없으면 |
| --- | --- | --- |
| `app/google-services.json` | Firebase 설정 (권민찬 공유) | Firebase 플러그인이 꺼진 채로 빌드된다 |
| `local.properties` | `MAPS_API_KEY=발급받은_키` 한 줄 추가 | 기본값 `DUMMY`로 빌드되고, 지도 바탕이 비어 보인다(앱은 꺼지지 않는다) |

- Google Maps 키는 Google Cloud 콘솔에서 **Maps SDK for Android**를 켜고, 앱 제한에 패키지 `com.tourderun`과 자기 디버그 SHA-1(`./gradlew signingReport`)을 넣는다.
- 지도 화면은 Google Play 서비스가 있는 기기·에뮬레이터 이미지(Google APIs 또는 Google Play)에서만 보인다.
