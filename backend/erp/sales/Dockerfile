# ERP Sales Exchange Module Dockerfile
FROM openjdk:17-jre-slim

# 设置工作目录
WORKDIR /app

# 复制JAR文件
COPY target/*.jar app.jar

# 创建非root用户
RUN addgroup --system --gid 1001 spring && \
    adduser --system --ingroup spring --uid 1001 spring

# 更改文件所有权
RUN chown -R spring:spring /app

# 切换到非root用户
USER spring:spring

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]