# ERP采购管理模块Dockerfile
# 多阶段构建：构建阶段和运行阶段

# 第一阶段：构建阶段
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml文件
COPY pom.xml .
COPY ../pom.xml ../pom.xml

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests -Pprod

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用目录和非root用户
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/*.jar app.jar

# 更改文件所有权
RUN chown -R spring:spring /app
USER spring:spring

# 健康检查配置
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 暴露端口（与application.yml中的server.port一致）
EXPOSE 8080

# JVM参数配置
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+ParallelRefProcEnabled \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/heapdump.hprof \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 标签信息
LABEL maintainer="ai-ready-devops@ai-ready.com"
LABEL version="1.0.0"
LABEL description="ERP采购管理模块"
LABEL environment="production"