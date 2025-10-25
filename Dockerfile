# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper & config
COPY gradlew gradlew.bat /app/
COPY gradle /app/gradle
COPY build.gradle settings.gradle /app/
RUN chmod +x gradlew

# Pre-fetch deps (cache-friendly)
RUN ./gradlew --no-daemon -v && ./gradlew --no-daemon dependencies || true

# Copy sources and build the jar
COPY src /app/src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tools for pg_isready (from postgresql-client) and bash
RUN apk add --no-cache bash postgresql-client

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy app jar & wait script
COPY --from=build /app/build/libs/*.jar /app/app.jar
COPY wait-for-postgres.sh /app/wait-for-postgres.sh
RUN chown spring:spring /app/app.jar /app/wait-for-postgres.sh \
    && chmod +x /app/wait-for-postgres.sh

USER spring:spring
EXPOSE 8080

# Optional JVM flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Start with wait
ENTRYPOINT ["/app/wait-for-postgres.sh"]
