# Multi-stage build: Gradle + JDK 21 to build, JRE 21 to run. Plain RUN steps
# (no BuildKit cache mounts) so it builds with any Docker daemon.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon --version >/dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test -x functionalTest

FROM eclipse-temurin:21-jre
# Runs as uid 10001 on a read-only root filesystem: only /app/data (a volume) and /tmp (a tmpfs) are
# writable at run time, see compose.yaml (NFR-S5, ADR-0017). The exec-form ENTRYPOINT makes the JVM
# PID 1, so it receives SIGTERM and drains for the 10 s shutdown phase.
WORKDIR /app
RUN useradd --system --uid 10001 --home /app urlshort \
 && mkdir -p /app/data && chown -R urlshort:urlshort /app
COPY --from=build /src/build/libs/urlshort.jar /app/urlshort.jar
USER urlshort
EXPOSE 8080
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/urlshort.jar"]
