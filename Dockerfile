# Multi-stage build를 사용하여 최적화된 이미지 생성
# Spring Boot 4.0.1은 Gradle 8.14 이상 필요
# 최신 Gradle 버전 사용 (8.14 이상 보장)
FROM gradle:jdk17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle build --no-daemon -x test

# 실행 단계 (ARM64 호환을 위해 alpine 대신 jammy 사용)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# curl은 이미 jammy 이미지에 포함되어 있음

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8081

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/ || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

