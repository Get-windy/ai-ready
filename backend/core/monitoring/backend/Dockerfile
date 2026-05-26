# AI-Ready 监控告警模块 - Docker 镜像构建文件
# 多阶段构建：构建阶段 + 运行阶段

# ============ 第一阶段：构建阶段 ============
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件和源码
COPY pom.xml .
COPY src ./src

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

# 构建应用（跳过测试）
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 提取构建产物
RUN mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../*.jar)

# ============ 第二阶段：运行阶段 ============
FROM eclipse-temurin:17-jre-alpine AS runner

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非root用户和组
RUN addgroup -S monitoring && \
    adduser -S monitoring -G monitoring

# 设置工作目录
WORKDIR /app

# 从构建阶段拷贝依赖
COPY --from=builder /app/target/dependency/BOOT-INF/lib ./app/lib
COPY --from=builder /app/target/dependency/META-INF ./app/META-INF
COPY --from=builder /app/target/dependency/BOOT-INF/classes ./app

# 创建必要的目录并设置权限
RUN mkdir -p /app/logs /app/data/metrics /app/data/alerts /app/config && \
    chown -R monitoring:monitoring /app && \
    chmod -R 755 /app

# 切换到非root用户
USER monitoring:monitoring

# JVM配置
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# 健康检查配置
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -f http://localhost:8081/api/monitoring/actuator/health || exit 1

# 暴露端口
EXPOSE 8081 9091

# 启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.qizhilian.monitoring.MonitoringApplication"]

# ============ 标签信息 ============
LABEL maintainer="AI-Ready Team <team@ai-ready.ai>"
LABEL version="1.0.0"
LABEL description="AI-Ready 监控告警模块后端服务"
LABEL org.opencontainers.image.source="https://github.com/ai-ready/monitoring-backend"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.created=${BUILD_DATE}
LABEL org.opencontainers.image.version=${BUILD_VERSION}

# ============ 构建参数 ============
ARG BUILD_VERSION=1.0.0
ARG BUILD_DATE=2026-04-29
ENV BUILD_VERSION=${BUILD_VERSION}
ENV BUILD_DATE=${BUILD_DATE}