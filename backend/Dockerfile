# ============================================================
# Multi-stage Docker build for the Spring Boot backend
# ============================================================
# Stage 1: Build the JAR with Maven
# Stage 2: Run the JAR with a slim JRE image
#
# Multi-stage = smaller final image (no Maven, no source code,
# just the compiled JAR + JRE). Goes from ~800MB to ~300MB.
# ============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy pom.xml first and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B 2>/dev/null || \
    (apt-get update && apt-get install -y maven && mvn package -DskipTests -B)

# --- Stage 2: Run ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot default port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
