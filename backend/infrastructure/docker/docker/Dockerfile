# AI-Ready API 多阶段构建 Dockerfile
# 版本: v2.0
# 优化: 分层缓存 + 最小镜像体积

# ============ 构建阶段 ============
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 1. 复制Maven配置（利用缓存）
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 2. 下载依赖（缓存层）
RUN ./mvnw dependency:go-offline -B

# 3. 复制源代码
COPY src ./src

# 4. 构建应用
RUN ./mvnw clean package -DskipTests -B

# 5. 解压JAR（分层优化）
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ============ 运行阶段 ============
FROM eclipse-temurin:21-jre-alpine

# 安装必要工具
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非root用户
RUN addgroup -S ai-ready && adduser -S ai-ready -G ai-ready

WORKDIR /app

# 分层复制（优化缓存）
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# 设置权限
RUN chown -R ai-ready:ai-ready /app

# 切换用户
USER ai-ready

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="production"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]