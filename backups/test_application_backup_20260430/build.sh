#!/bin/bash

# AI-Ready 测试应用构建脚本
# 用于解除devops-engineer和test-agent-2的阻塞

echo "========================================"
echo "AI-Ready 测试应用构建脚本"
echo "========================================"

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装，请先安装Maven"
    exit 1
fi

# 清理旧构建
echo "1. 清理旧构建文件..."
rm -rf target/

# 构建应用
echo "2. 构建应用..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "错误: Maven构建失败"
    exit 1
fi

# 检查JAR文件
if [ ! -f "target/ai-ready-test-app-1.0.0.jar" ]; then
    echo "错误: JAR文件未生成"
    exit 1
fi

echo "3. 构建成功！"
echo "   JAR文件: target/ai-ready-test-app-1.0.0.jar"
echo "   大小: $(du -h target/ai-ready-test-app-1.0.0.jar | cut -f1)"

# 运行测试
echo "4. 运行应用测试..."
java -jar target/ai-ready-test-app-1.0.0.jar --version

echo "========================================"
echo "构建完成！"
echo "你可以使用以下命令运行应用："
echo "  java -jar target/ai-ready-test-app-1.0.0.jar"
echo "或者使用Docker："
echo "  docker-compose up -d"
echo "========================================"