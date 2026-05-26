#!/bin/bash

# AI-Ready 测试应用快速启动脚本

echo "========================================"
echo "AI-Ready 测试应用快速启动"
echo "========================================"

# 检查JAR文件是否存在
if [ ! -f "target/ai-ready-test-app-1.0.0.jar" ]; then
    echo "JAR文件不存在，正在构建..."
    ./build.sh
    if [ $? -ne 0 ]; then
        echo "构建失败，无法启动应用"
        exit 1
    fi
fi

echo "启动应用..."
echo "应用将在 http://localhost:8080 启动"
echo "按 Ctrl+C 停止应用"
echo ""

# 启动应用
java -jar target/ai-ready-test-app-1.0.0.jar