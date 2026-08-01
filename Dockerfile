# 씩씩이 단일 이미지.
# React 빌드물을 Spring Boot 의 classpath:/static/ 에 구워 넣어 jar 하나로 만든다.
# fe/ 와 be/ 를 모두 COPY 하므로 빌드 컨텍스트는 레포 루트다.

# --- 1) FE 빌드 ---
FROM node:22-alpine AS fe
WORKDIR /fe

# 락파일을 먼저 넣어 의존성 레이어를 캐시한다.
COPY fe/package.json fe/package-lock.json ./
RUN npm ci

COPY fe/ ./
RUN npm run build

# --- 2) BE 빌드 (FE 산출물을 static 으로 굽는다) ---
FROM eclipse-temurin:25-jdk AS be
WORKDIR /be

# 래퍼와 빌드 스크립트를 먼저 넣어 의존성 레이어를 캐시한다.
COPY be/gradlew be/settings.gradle be/build.gradle ./
COPY be/gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY be/src ./src
COPY --from=fe /fe/dist ./src/main/resources/static
RUN ./gradlew bootJar --no-daemon

# --- 3) 런타임 ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# root 로 돌리지 않는다.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
USER spring

COPY --from=be --chown=spring:spring /be/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
