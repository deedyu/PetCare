# PetCare

반려동물 일정, 건강 기록, 가족 공유를 한 계정에서 보는 웹 서비스입니다. 화면은 Thymeleaf로 그리고, 같은 서버에 JSON API가 있습니다.

이 저장소는 수업에서 진행한 Docker와 Kubernetes 실습 결과도 함께 담습니다. 아래 배포 수치는 로컬 Docker Desktop에서 실제로 확인한 값입니다.

## 개발 목적

반려동물의 반복 일정과 건강 기록을 가족 구성원과 나누되, 계정과 비밀번호는 소스 코드에 남기지 않는 것이 목표입니다. 컨테이너로 포장한 뒤 Docker Hub에 올리고, Kubernetes에서 장애 복구, 수동 확장, 무중단 교체, CPU 기준 자동 확장을 단계별로 확인했습니다.

## 주요 기능

| 경로 | 내용 |
| --- | --- |
| `/` | 오늘 일정, 공지, 안 읽은 알림 수 |
| `/calendar` | 월·주·일 달력과 날짜별 완료 |
| `/pets` | 반려동물 등록·수정. 프로필은 사진, 이모지, 이름 첫 글자 중 하나 |
| `/pets/{id}/tasks` | 일정. 반복은 없음, 매일, 매주, 매월, 평일 |
| `/pets/{id}/health` | 건강 기록과 체중 그래프 |
| `/family` | 가족 생성, 초대 코드, 반려동물 공유 |
| `/inquiries`, `/notifications` | 문의와 앱 안 알림 |
| `/mypage` | 표시 이름 수정, 비밀번호 변경, 계정 탈퇴 |
| `/admin` | 관리자 전용. 회원 상태, 공지, 문의 답변 |

로그인에 성공하면 회원은 `/`, 관리자는 `/admin`으로 이동합니다. 같은 아이디로 15분 안에 5번 실패하면 15분 동안 로그인만 막습니다. 계정은 지우지 않습니다. 새 비밀번호는 영문과 숫자를 포함한 8자 이상입니다.

세션 쿠키는 `HttpOnly`, `SameSite=Lax`입니다. 로컬 HTTP에서는 `Secure`를 끄고, HTTPS일 때만 켭니다. 화면을 연 채 30분 동안 사용하지 않으면 로그아웃합니다. 로그인 유지 쿠키는 해시만 데이터베이스에 저장합니다.

사진은 JPG, PNG, WebP를 받고 최대 5MB입니다. 서버가 이미지를 다시 연 뒤 JPEG로 저장하고, 데이터베이스에는 파일 이름만 남깁니다.

## 기술 스택

| 영역 | 사용 |
| --- | --- |
| 언어 | Kotlin, Java 17 |
| 서버 | Spring Boot 4.0.1, Spring Security, Spring Data JPA |
| 화면 | Thymeleaf, HTML, CSS, JavaScript |
| 데이터베이스 | MySQL 8. 테스트는 H2 |
| 빌드 | Gradle |
| 컨테이너 | Docker, Docker Compose, Docker Hub |
| 오케스트레이션 | Kubernetes. Docker Desktop, 서버 `v1.34.1`, 노드 `linux/arm64` |
| 지표 | metrics-server `v0.9.0` |

## 폴더 구조

```text
src/main/kotlin        애플리케이션 코드
src/main/resources     설정, Thymeleaf, 정적 파일
src/test               H2 테스트
Dockerfile             멀티스테이지 이미지 빌드
docker-compose.yml     기존 Compose. 앱 8081, MySQL 3307
docker-compose.lab.yml 수업용 Compose. 앱 8083, MySQL 3308
k8s/                   기존 petcare 네임스페이스 매니페스트
k8s/lab/               수업용 petcare-lab 네임스페이스 매니페스트
k8s/lab/examples/      Secret 예시. 값은 REPLACE_ME
.env.example           환경 변수 이름만 있는 예제
```

## 로컬 실행

Java 17과 MySQL 8이 필요합니다. 기본 포트는 `8081`이고, 데이터베이스 이름은 `petcare`입니다.

```bash
cp .env.example .env
./gradlew bootRun
./gradlew test
```

`.env`에는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `PETCARE_ADMIN_INITIAL_PASSWORD`를 채웁니다. 이 파일은 Git에 올리지 않습니다. 값이 비어 있으면 서버는 비밀번호를 출력하지 않고, 빠진 변수 이름만 알려 준 뒤 멈춥니다.

스키마는 `ddl-auto=update`로 맞춥니다. 없는 테이블과 컬럼은 추가하고, 기존 행은 지우지 않습니다.

## Docker

`Dockerfile`은 두 단계입니다. `gradle:jdk17`에서 테스트를 제외하고 JAR을 만들고, `eclipse-temurin:17-jre-jammy`에서 그 JAR만 실행합니다. 컨테이너 포트는 `8081`입니다. `.dockerignore`가 `.env`와 테스트 소스를 이미지 밖으로 둡니다.

로컬 태그 `petcare:lab`의 이미지 ID는 `sha256:ddd77632153463e0415bcdff9239d55dfa22e4c2b4cc9a7d1b13025614ccaa93`입니다.

## Docker Compose

기존 `docker-compose.yml`은 수정하지 않았습니다. 수업은 별도 파일로 실행합니다.

```bash
docker compose --env-file .env.lab -f docker-compose.lab.yml up -d
```

| 컨테이너 | 호스트 포트 | 역할 |
| --- | --- | --- |
| `petcare-lab-mysql` | `3308` | MySQL 8.0, 데이터베이스 `petcare_lab` |
| `petcare-lab-app` | `8083` | `petcare:lab` |

데이터는 이름 있는 볼륨 `petcare_lab_mysql_data`에 있습니다. 기존 볼륨 `petcare_mysql_data`와는 다릅니다. 비밀번호는 Git에 없는 `.env.lab`에서 읽습니다. MySQL이 healthy가 된 뒤에 앱이 시작됩니다.

## Docker Hub

공개 저장소는 [jeesyu/petcare](https://hub.docker.com/r/jeesyu/petcare)입니다.

| 태그 | 구분 |
| --- | --- |
| `lab` | 첫 업로드 이미지. 로컬 ID `sha256:ddd776321534...` |
| `lab-v2` | 같은 애플리케이션에 빌드 라벨만 추가한 이미지 |

`lab-v2`는 소스와 화면을 바꾸지 않고 `docker build --label petcare.image.version=lab-v2`로 만들었습니다. 로컬 이미지 ID는 `sha256:c8a1f87e41aacfe39ca4a3c7fbf00ff7a408441d45280a84a374465e5358110f`입니다.

Docker Hub에서 따로 조회한 태그 digest도 같은 인덱스 값입니다. 그 안의 `linux/arm64` 매니페스트 digest는 `sha256:9abf11d0315cb152d038aa569c55018d17bc70dac52d34a2af93740f61c462a1`로, 인덱스 digest와 다릅니다. 이미지 ID와 레지스트리 digest는 항상 같은 값으로 보지 않았습니다.

## Kubernetes

수업용 클러스터는 기존 `petcare` 네임스페이스와 분리된 `petcare-lab`입니다. 기존 네임스페이스의 MySQL, PVC, 복제본 0인 앱 Deployment는 유지했습니다.

```text
Namespace petcare-lab
  ConfigMap petcare-lab-config     주소, DB 이름 petcare_lab, 포트
  Secret    앱 계정과 MySQL 비밀번호. Git에는 예시만 있음
  PVC       petcare-lab-mysql-pvc  1Gi, hostpath
  Deployment mysql                 복제본 1, mysql:8.0
  Service   mysql                  ClusterIP 3306
  Deployment petcare-app           jeesyu/petcare:lab-v2
  Service   petcare-service        LoadBalancer, 호스트 8084 → 컨테이너 8081
  HPA       petcare-lab-hpa        CPU 50%, 최소 2, 최대 3
```

앱은 MySQL Service 이름 `mysql`로 접속합니다. 이 이름은 `petcare-lab` 안에서만 이 실습용 데이터베이스를 가리킵니다. 준비 검사는 `GET /`입니다. 로그인 전에는 302가 돌아오고, Kubernetes는 그 응답을 준비 완료로 봅니다.

`k8s/lab/petcare-deployment.yaml`의 복제본과 이미지는 실습이 끝난 상태인 복제본 2, `jeesyu/petcare:lab-v2`로 적어 두었습니다. 이 문서 작업에서는 그 파일을 클러스터에 다시 적용하지 않았습니다.

## 실습 결과

모두 `docker-desktop` context, 네임스페이스 `petcare-lab`에서 확인했습니다.

### Self-healing

Deployment가 유지하려는 PetCare Pod는 1개였습니다. Pod `petcare-app-5596fbccd5-xrgbq`를 삭제한 뒤, 같은 ReplicaSet이 `petcare-app-5596fbccd5-j8q5v`를 만들었습니다. 삭제 시작부터 READY 1/1까지 **18초**였습니다. 회원과 반려동물 `실습별`은 MySQL 볼륨에 남아 있었습니다. 로그인 세션은 삭제된 Pod 메모리에 있었기 때문에 다시 로그인해야 했습니다.

### 수동 확장

`kubectl scale deployment/petcare-app --replicas=2`로 1개에서 2개로 늘렸습니다. 기존 Pod는 지우지 않았고, `petcare-app-5596fbccd5-lq72g`가 추가되었습니다. 두 Pod의 데이터베이스 주소는 같았고 Service Endpoint는 2개였습니다. MySQL 복제본은 1개로 두었습니다.

### Rolling Update

`maxSurge 25%`, `maxUnavailable 25%`입니다. 복제본 2개에서는 새 Pod를 1개까지 먼저 만들고, 그 Pod가 준비되기 전에는 기존 Pod를 지우지 않습니다.

`kubectl set image`로 `jeesyu/petcare:lab-v2`로 바꾼 뒤 **37.3초** 만에 새 Pod 2개가 READY가 되었습니다. 순서는 새 Pod 1개 추가, 준비 완료, 기존 Pod 1개 종료, 두 번째 새 Pod 준비, 남은 기존 Pod 종료였습니다. 회원과 `실습별`은 유지됐습니다. Rollback은 하지 않았습니다.

### HPA

metrics-server `v0.9.0`을 설치한 뒤 `petcare-lab-hpa`를 만들었습니다. CPU request `100m`의 50%가 목표이므로, Pod 평균 사용량이 `50m`을 넘으면 늘립니다. 최소 2개, 최대 3개입니다.

읽기 전용 `GET /login`을 Pod마다 동시 10개로 보냈습니다. 약 21초 뒤 앱 Pod CPU가 각각 약 `900m`까지 올라 limit에 가까워져 부하를 멈췄고, 2차 부하는 실행하지 않았습니다. HPA 대상 사용률은 `914%/50%`까지 표시됐습니다.

약 1분 안에 Pod가 2개에서 3개로 늘고 READY 3/3이 되었습니다. 부하가 끝난 뒤 약 6분 후 추가 Pod만 종료되고 다시 2개가 되었습니다. HPA는 최근 5분의 높은 사용량을 기억한 뒤 최소 개수로 줄입니다.

## 문제 해결과 기술적 한계

- 포트가 겹치지 않게 나누었습니다. 기존 Kubernetes는 `8081`, 로컬 실행은 설정에 따라 `8081`, 수업 Compose는 `8083`, 수업 Kubernetes는 `8084`입니다.
- 수업용 MySQL 볼륨과 PVC는 기존 데이터와 다른 이름입니다. 앱 Pod를 지워도 데이터베이스 행은 남습니다.
- 로그인 세션은 Pod 메모리에 있습니다. Pod가 바뀌면 그 세션은 사라집니다. Service의 `sessionAffinity: ClientIP`는 같은 컴퓨터의 요청을 한 Pod로 붙이려 하지만, 그 Pod가 교체되면 다시 로그인해야 합니다.
- 사진 파일은 Pod 안 디스크에 있고 공유 PVC가 없습니다. Pod가 사라지면 그 사진 파일도 사라집니다. 데이터베이스에는 파일 이름만 있습니다. `실습별`은 이름 첫 글자라 사진 파일이 없었습니다.
- Docker Desktop kubelet 인증서에 노드 IP가 없어 metrics-server가 사용량을 읽지 못했습니다. 이 로컬 실습에서만 `--kubelet-insecure-tls`를 추가했습니다. 통신 암호화는 유지되고, 인증서가 클러스터 CA와 주소에 맞는지만 확인하지 않습니다. 인터넷에 공개된 클러스터용 설정이 아닙니다.
- 메일 발송과 기기 푸시는 없습니다. 알림은 앱 안 목록입니다.
- 운영 마이그레이션 도구는 넣지 않았습니다. `ddl-auto=update`를 `create`나 `create-drop`으로 바꾸면 기존 행이 사라질 수 있습니다.
