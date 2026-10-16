# PetCare

반려동물 일정, 건강, 가족 공유를 한 계정으로 보는 웹 서비스입니다. 화면은 Thymeleaf이고, 같은 서버에 JSON API가 있습니다.

## 실행

- Java 17, MySQL 8 (`localhost:3306/petcare`)
- 주소: http://localhost:8081
- 스키마는 기동 시 Hibernate `ddl-auto=update`로 컬럼과 테이블을 추가합니다. 기존 행은 지우지 않습니다.

데이터베이스 비밀번호는 소스에 두지 않습니다. 처음 한 번만 아래처럼 로컬 설정 파일을 만듭니다.

```bash
cp .env.example .env
```

프로젝트 루트의 `.env`를 열고 `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `PETCARE_ADMIN_INITIAL_PASSWORD`에 본인 MySQL 계정과 비밀번호를 넣습니다. Docker Compose를 쓸 때는 `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`도 채웁니다. `.env`는 Git에 올라가지 않습니다. 값이 비어 있으면 서버는 비밀번호를 출력하지 않고, 어떤 환경 변수가 없는지 알려 준 뒤 멈춥니다.

`PETCARE_ADMIN_INITIAL_PASSWORD`는 `admin` 계정이 아직 없을 때만 씁니다. 이미 만들어진 관리자와 회원의 비밀번호 해시는 서버를 다시 켜도 바뀌지 않습니다.

```bash
./gradlew bootRun
./gradlew test
```

테스트는 H2를 쓰고, 실행 중인 MySQL 데이터와 분리됩니다.

## 로그인

세션 로그인입니다. `POST /login`에 `username`, `password`, `_csrf`를 보냅니다.

- 회원은 `/`
- 관리자는 `/admin`
- 정지된 계정은 로그인되지 않습니다.
- 새 비밀번호는 영문과 숫자를 포함한 8자 이상입니다. 그 전에 만들어진 짧은 비밀번호는 그대로 로그인됩니다.
- 같은 아이디로 15분 안에 5번 실패하면 15분 동안 로그인만 막습니다. 계정과 비밀번호 해시는 바꾸지 않습니다. 서버를 다시 켜면 횟수는 초기화됩니다.
- 세션 쿠키는 `HttpOnly`, `SameSite=Lax`입니다. 로컬은 HTTP라 `Secure`는 꺼 둡니다. HTTPS로 배포할 때만 켜면 됩니다.

## 회원 화면

| 경로 | 내용 |
| --- | --- |
| `/` | 오늘 일정, 공지, 안 읽은 알림 수 |
| `/calendar` | 월·주·일, 날짜별 완료 |
| `/family` | 가족 만들기, 초대 코드, 반려동물 공유 |
| `/pets`, `/pets/{id}/tasks` | 반려동물과 일정 |
| `/pets/{id}/health` | 체중 그래프, 접종 예정, 작성자 |
| `/inquiries` | 내 문의 |
| `/notifications` | 일정 알림, 문의 답변 |
| `/mypage` | 정보 수정, 계정 삭제 |

일정 반복은 `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `WEEKDAYS`입니다. 완료는 날짜마다 따로 저장됩니다. 미리 알림은 정시, 10분 전, 30분 전, 1시간 전입니다. 담당자가 있으면 담당자에게, 없으면 주인에게 알림이 갑니다. 같은 일정은 하루에 한 번만 쌓입니다.

가족 구성원은 공유된 반려동물의 건강 기록을 보고 추가할 수 있습니다. 수정과 삭제는 주인이거나 그 기록을 쓴 사람만 할 수 있습니다.

## 관리자

`/admin/login`으로 들어갑니다. 관리자만 `/admin`을 볼 수 있습니다.

- 회원 상태: 정상, 정지
- 통계는 건수만 보여 줍니다. 일정 제목과 시각은 표시하지 않습니다.
- `/admin/notices` 공지. 고정하면 회원 홈 위에 나옵니다.
- `/admin/inquiries` 문의 답변

## API

로그인한 세션이 필요합니다. 인증이 없으면 401, 권한이 없으면 403입니다. 변경 요청에는 `X-CSRF-TOKEN` 헤더 또는 `_csrf`가 필요합니다.

같은 반려동물, 일정, 건강 기록, 회원 정보를 두 사람이 저장하면 먼저 저장한 값만 남고 나중 저장은 거절됩니다. API는 409를 반환합니다.

자세한 경로는 [docs/api.md](docs/api.md), 테이블은 [docs/erd.md](docs/erd.md), 테스트 목록은 [docs/test-results.md](docs/test-results.md)에 있습니다.

## Docker

프로젝트 루트의 `.env`를 채운 뒤 `docker compose up --build`로 실행합니다. 앱 컨테이너의 데이터베이스 주소는 `mysql` 호스트로 고정되어 있고, 비밀번호는 `.env`에서만 읽습니다. 쿠버네티스 Secret은 `k8s/create-secrets.sh`가 만듭니다. 예시 파일의 `REPLACE_ME`는 실제 값이 아닙니다.

## 아직 아닌 것

- 메일 발송과 기기 알림 권한 창은 없습니다. 알림은 앱 안 목록입니다.
- `ddl-auto=update`를 유지합니다. `create`나 `create-drop`으로 바꾸면 기존 행이 사라질 수 있습니다. Flyway는 기존 스키마를 기준선으로 잡는 계획만 있고, 자동 마이그레이션은 넣지 않았습니다.
- 데이터베이스 비밀번호 값 자체는 바꾸지 않았습니다. 소스에서 뺀 것뿐입니다.
