# 빌드 스테이지는 러너 네이티브 아키텍처(=$BUILDPLATFORM)에 고정한다.
# 산출물인 jar 은 아키텍처 중립이므로, 멀티아키(amd64/arm64) 빌드에서도
# Gradle 은 한 번만 실행되고 QEMU 에뮬레이션 비용이 발생하지 않는다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar -x test

# 런타임 스테이지는 --platform 을 지정하지 않아 타깃 아키텍처를 따라간다.
# (prod 노드는 arm64/Graviton, dev 클러스터는 amd64/arm64 혼합)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
