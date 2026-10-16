# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------------------------
# Stage 1: build. Dependencies are resolved in their own layer so that editing source code does
# not download the world again.
# ---------------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------------------------------------------------------------------------------------------
# Stage 2: runtime. Only the JRE and the jar, running as a non root user.
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app
COPY --from=build /workspace/target/franquicias-api-*.jar /app/app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD ["sh", "-c", "wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
