# Runtime-only packaging of the separately built f090103 rollback jar.
FROM eclipse-temurin:21-jre
# Runs as uid 10001 on a read-only root filesystem: only /app/data (a volume) and /tmp (a tmpfs) are
# writable at run time, see compose.yaml (NFR-S5, ADR-0017). The exec-form ENTRYPOINT makes the JVM
# PID 1, so it receives SIGTERM and drains for the 10 s shutdown phase.
WORKDIR /app
RUN useradd --system --uid 10001 --home /app urlshort \
 && mkdir -p /app/data && chown -R urlshort:urlshort /app
COPY urlshort.jar /app/urlshort.jar
USER urlshort
EXPOSE 8080
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/urlshort.jar"]
