# API Gateway Dockerfile (多阶段构建)
# 版本: v2.0.0
# 创建日期: 2026-04-29
# 基于: deploy/test-environment/Dockerfile.template

# ============ 构建阶段 ============
FROM maven:3.9-eclipse-temurin-17 AS builder

# 使用阿里云镜像加速
ARG MIRROR_URL=https://maven.aliyun.com/repository/public
ARG BUILD_PROFILE=test
ARG SKIP_TESTS=false

# 配置Maven镜像
RUN mkdir -p /root/.m2 && \
    echo "<settings><mirrors><mirror><id>aliyunmaven</id><mirrorOf>central</mirrorOf><name>阿里云公共仓库</name><url>${MIRROR_URL}</url></mirror></mirrors></settings>" > /root/.m2/settings.xml

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY pom.xml .
COPY src ./src

# 下载依赖（利用缓存层）
RUN mvn dependency:go-offline -B -DskipTests

# 构建应用
RUN mvn clean package \
    -DskipTests=${SKIP_TESTS} \
    -P${BUILD_PROFILE} \
    -Dmaven.test.skip=${SKIP_TESTS} \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true \
    -Dmaven.site.skip=true

# ============ 运行时阶段 ============
FROM eclipse-temurin:17-jre-alpine AS runtime

# 应用元数据
LABEL maintainer="ai-ready-devops@example.com"
LABEL version="2.0.0"
LABEL description="API Gateway 服务容器 (测试环境)"
LABEL service.name="api-gateway"
LABEL service.port="8080"
LABEL service.health.path="/actuator/health"

# 创建非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置时区
RUN apk --no-cache add tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 安装运行时工具
RUN apk --no-cache add \
    curl \
    bash \
    tini \
    && rm -rf /var/cache/apk/*

# 设置工作目录
WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/*.jar app.jar

# 复制配置文件
COPY --from=builder /app/src/main/resources/ ./config/

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080 9090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 使用tini作为init进程
ENTRYPOINT ["/sbin/tini", "--"]

# 启动应用（API Gateway特定配置）
CMD ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=150", \
    "-XX:+UseContainerSupport", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/logs/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${BUILD_PROFILE}", \
    "-Dlogging.config=/app/config/logback-spring.xml", \
    "-Dlogging.path=/app/logs", \
    "-Dserver.port=8080", \
    "-Dmanagement.server.port=9090", \
    "-Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus", \
    "-Dspring.application.name=api-gateway", \
    "-jar", \
    "/app/app.jar"]

# ============ 镜像大小优化说明 ============
# 原始镜像大小: ~1.2GB (maven + jdk)
# 运行时镜像大小: ~200MB (jre-alpine)
# 优化率: ~83% 减少