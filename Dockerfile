# AI-Ready Java 应用 Dockerfile
# Optimized for Spring Boot 3.x with JVM tuning
# Version: v1.0

# Stage 1: Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY pom.xml .
COPY core-common/pom.xml core-common/
COPY core-base/pom.xml core-base/
COPY core-api/pom.xml core-api/

# Download dependencies (cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build with Maven (skip tests)
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -B

# Stage 2: Runtime Stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user
RUN addgroup -S appgroup && adduser -S -G appgroup appuser

WORKDIR /app

# Create necessary directories
RUN mkdir -p logs config && chown -R appuser:appgroup /app

# Copy built artifact from build stage
COPY --from=build /build/core-api/target/*.jar /app/app.jar

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heap-dump.hprof"
ENV SPRING_PROFILES_ACTIVE=production
ENV APP_VERSION=1.0.0
ENV TZ=Asia/Shanghai

# Expose port
EXPOSE 8080

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# ENTRYPOINT
ENTRYPOINT ["java","$JAVA_OPTS","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]

# Labels for image metadata
LABEL maintainer="ai-ready-team@ai-ready.cn"
LABEL version="1.0.0"
LABEL description="AI-Ready Enterprise Management System"
LABEL org.opencontainers.image.source="https://github.com/ai-ready/ai-ready"
LABEL org.opencontainers.image.ref.name="ai-ready-api"
LABEL org.opencontainers.image.created="${BUILD_TIME:-unknown}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT:-unknown}"
LABEL org.opencontainers.image.version="${APP_VERSION}"