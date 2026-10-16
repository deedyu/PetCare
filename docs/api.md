# API

기준 주소는 `http://localhost:8081`입니다. 쿠키 세션으로 인증합니다. `/api/**`는 로그인하지 않으면 401, 대상에 권한이 없으면 403입니다. `POST`, `PUT`, `PATCH`, `DELETE`에는 CSRF 토큰이 필요합니다.

화면용 폼 경로도 같은 규칙을 따릅니다. 아래 JSON API만 요청 본문을 사용합니다. 반려동물, 일정, 건강 기록, 회원 정보를 동시에 수정하면 나중 저장은 409입니다. 먼저 저장된 행은 유지됩니다.

## 사용자 `/api/users`

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/api/users/{userId}` | 본인 또는 관리자 조회 |
| GET | `/api/users/username/{username}` | 본인 또는 관리자 조회. 없으면 404 |
| POST | `/api/users` | 관리자만 사용자 생성. 회원 가입은 `POST /register` |
| PUT | `/api/users/{userId}` | 본인 또는 관리자 수정. 빈 비밀번호는 기존 해시를 유지 |
| DELETE | `/api/users/{userId}` | 본인 또는 관리자 삭제. 관리자 계정은 거절 |

## 반려동물 `/api/pets`

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/api/pets` | 내가 보거나 가족이 공유한 반려동물 |
| GET | `/api/pets/{petId}` | 볼 수 있는 반려동물 하나 |
| POST | `/api/pets` | 등록. 소유자는 로그인 사용자 |
| PUT | `/api/pets/{petId}` | 주인만 수정 |
| DELETE | `/api/pets/{petId}` | 주인만 삭제 |

응답에는 `familyName`이 포함될 수 있습니다.

## 일정 `/api/tasks`

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/api/tasks` | 내가 볼 수 있는 일정 |
| GET | `/api/tasks/pet/{petId}` | 그 반려동물의 일정 |
| GET | `/api/tasks/{taskId}` | 일정 하나 |
| POST | `/api/tasks` | 생성. category, description, weekdays, assigneeId |
| PUT | `/api/tasks/{taskId}` | 수정 |
| PATCH | `/api/tasks/{taskId}/toggle` | 선택 날짜의 완료를 바꿈. 날짜가 없으면 오늘 |
| DELETE | `/api/tasks/{taskId}` | 삭제 |

담당자는 공유된 반려동물의 가족 구성원만 지정할 수 있습니다. 반복 일정을 오늘 완료해도 다른 날짜는 완료되지 않습니다.

`notificationTime`: `ON_TIME`, `10_MIN_BEFORE`, `30_MIN_BEFORE`, `1_HOUR_BEFORE`, 빈 값.

`repeatType`: `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `WEEKDAYS`. 요일 지정은 `weekdays`에 `MON,WED`처럼 둡니다.

`category`: `MEAL`, `WALK`, `HEALTH`, `MEDICINE`, `GROOMING`, `HOSPITAL`, `OTHER`.

## 건강 `/api/health-records`

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/api/health-records/pet/{petId}` | 가족 구성원 포함 조회 |
| GET | `/api/health-records/pet/{petId}/type/{recordType}` | 종류별 조회 |
| GET | `/api/health-records/{recordId}` | 볼 수 있는 기록 하나 |
| POST | `/api/health-records` | 추가. author는 로그인 사용자. nextDueDate 선택 |
| PUT | `/api/health-records/{recordId}` | 주인 또는 작성자만 수정 |
| DELETE | `/api/health-records/{recordId}` | 주인 또는 작성자만 삭제 |

`recordType`: `WEIGHT`, `VACCINATION`, `HOSPITAL`, `MEDICATION`.

응답에는 `authorName`, `createdAt`, `nextDueDate`가 있습니다. 예전 기록은 작성자가 비어 있습니다.

## 화면 경로

JSON이 아닌 화면입니다. 로그인 세션과 CSRF를 사용합니다.

| 영역 | 경로 |
| --- | --- |
| 인증 | `GET /login`, `POST /login`, `GET|POST /register`, `POST /logout` |
| 홈·달력 | `GET /`, `GET /calendar`, `POST /calendar/complete` |
| 가족 | `GET|POST /family`, `POST /family/join`, `GET /family/{id}`, 초대·탈퇴·삭제·공유 |
| 문의 | `GET|POST /inquiries`, `GET /inquiries/{id}` |
| 알림 | `GET /notifications`, `POST /notifications/{id}/open`, `POST /notifications/read-all` |
| 관리자 | `GET /admin`, 회원 상태·삭제, `GET|POST /admin/notices`, `GET|POST /admin/inquiries` |

관리자 API로 다른 사람의 건강 기록이나 일정 내용을 읽지는 않습니다.
