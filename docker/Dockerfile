# --- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Dependency layer: cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system lms && useradd --system --gid lms --create-home lms

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R lms:lms /app

USER lms
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "wget -q -O - http://localhost:8080/actuator/health | grep -q UP"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
