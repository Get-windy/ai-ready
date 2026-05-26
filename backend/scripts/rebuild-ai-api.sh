#!/bin/bash

# AI-API服务重建脚本
# 用于修复JAR manifest问题并重新启动服务

echo "=== AI-API服务重建脚本 ==="
echo "开始时间: $(date)"
echo ""

# 1. 停止并删除现有容器
echo "1. 停止并删除现有容器..."
docker stop ai-ready-api 2>/dev/null || true
docker rm ai-ready-api 2>/dev/null || true
echo "   完成"

# 2. 构建项目
echo "2. 构建core-api项目..."
cd I:/AI-Ready/backend/core/api/core-api
mvn clean package -DskipTests
echo "   构建完成"

# 3. 检查JAR文件
echo "3. 检查JAR文件..."
JAR_FILE="target/core-api-1.0.0-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    echo "   JAR文件存在: $JAR_FILE"
    # 检查JAR文件是否有主清单属性
    echo "   检查JAR主清单属性..."
    java -jar "$JAR_FILE" --version 2>&1 | head -5
    echo "   JAR文件检查完成"
else
    echo "   错误: JAR文件不存在"
    exit 1
fi

# 4. 构建Docker镜像
echo "4. 构建Docker镜像..."
docker build -t ai-ready:latest -f Dockerfile.optimized .
echo "   Docker镜像构建完成"

# 5. 启动容器
echo "5. 启动ai-ready-api容器..."
docker run -d \
    --name ai-ready-api \
    --network bridge \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=test \
    -e JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    ai-ready:latest
echo "   容器已启动"

# 6. 等待服务启动
echo "6. 等待服务启动..."
sleep 10

# 7. 检查服务状态
echo "7. 检查服务状态..."
docker ps | grep ai-ready-api
echo ""
echo "容器日志:"
docker logs ai-ready-api --tail 20

# 8. 健康检查
echo "8. 执行健康检查..."
sleep 5
curl -f http://localhost:8080/actuator/health 2>/dev/null && echo "   服务健康检查通过" || echo "   服务健康检查失败"

echo ""
echo "=== 重建完成 ==="
echo "完成时间: $(date)"