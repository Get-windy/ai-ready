# ERP Batch/SN Management Module Dockerfile
# 多阶段构建，优化镜像大小

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制构建产物（使用本地预构建的jar）
COPY target/erp-batch-sn-1.0.0-SNAPSHOT.jar app.jar

# 创建非root用户
RUN addgroup -S erpgroup && adduser -S erpuser -G erpgroup
USER erpuser:erpgroup

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 元数据
LABEL module.name="erp-batch-sn" \
      module.version="1.0.0-SNAPSHOT" \
      module.description="批次/序列号管理模块" \
      maintainer="devops-engineer" \
      build.date="2026-04-29"