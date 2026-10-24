# ==============================================================================
# Multi-Stage Dockerfile for CivicSense Spring Boot Application
# ==============================================================================

# ── Stage 1: Build & Package ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Install dos2unix to ensure executable scripts work across platforms
RUN apk add --no-cache dos2unix

# Copy Maven wrapper and dependencies configuration first (optimizes layer caching)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN dos2unix mvnw && chmod +x mvnw

# Resolve dependencies in a cached layer
RUN ./mvnw dependency:go-offline -B || true

# Copy source tree and compile application package
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Lightweight & Secure Runtime ─────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create a dedicated non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Prepare local file uploads directory with correct permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

# Switch to non-root user
USER appuser

# Document exposed container port
EXPOSE 8080

# Health check to ensure service readiness
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/complaints || exit 1

# Container-friendly JVM memory flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Launch Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
