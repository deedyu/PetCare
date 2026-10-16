# 테스트 결과

2026-10-16에 `./gradlew test --offline` 25건이 통과했습니다. 실패는 0건입니다. 테스트 데이터베이스는 H2이고, 실행 중인 MySQL과는 분리됩니다.

| 테스트 | 확인 내용 |
| --- | --- |
| `PasswordRulesTest` | 새 비밀번호는 영문과 숫자 포함 8자 |
| `AuthFlowTest` | 회원은 `/`, 관리자는 `/admin`, 열린 API는 401 |
| `ScheduleExpanderTest` | 매일·매주·매월·요일 지정과 `MON` 같은 짧은 요일 이름 |
| `TaskOccurrenceTest` | 오늘 완료가 다른 날짜를 완료로 만들지 않음 |
| `FamilyShareTest` | 공유 전후 조회, 담당자, 가족 활동, 소유자 탈퇴 제한 |
| `HealthRecordAuthorTest` | 체중 그래프 정렬, 가족 구성원의 작성자와 접종일, 작성자가 아닌 구성원의 수정 제한 |
| `AdminOpsTest` | 정지 계정은 로그인 불가, 고정 공지 순서, 다른 회원의 문의 차단, 답변 후 상태 |
| `NotificationTest` | 지난 일정 알림은 한 번만 생성, 아직 안 된 알림은 생성하지 않음, 문의 답변 알림, 탈퇴 시 알림 삭제 |
| `LocalEnvironmentTest` | MySQL 접속 값이 없으면 변수 이름만 안내. H2는 검사를 건너뜀 |
| `LoginAttemptTrackerTest` | 5번째 실패에서 잠그고, 성공하면 횟수를 지움 |
| `LoginThrottleTest` | 다섯 번 틀린 뒤의 로그인은 `/login?locked` |
| `OptimisticLockTest` | 먼저 저장한 반려동물 이름만 남고 나중 저장은 거절 |

화면에서 따로 확인한 흐름은 가족 공유, 체중 그래프와 접종 예정, 관리자 통계에서 일정 제목 숨김, 공지와 문의, 계정 정지, 알림을 열어 캘린더로 이동하는 동작입니다. 확인에 쓴 임시 계정은 삭제했습니다.
