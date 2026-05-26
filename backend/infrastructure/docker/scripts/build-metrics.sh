#!/bin/bash

# Docker构建性能监控脚本
# 用途: 记录和分析Docker构建性能指标
# 作者: devops-engineer
# 创建日期: 2026-04-27

set -e

# 配置
IMAGE_NAME=${1:-"aiready/api-gateway:test"}
BUILD_LOG="build_metrics_$(date '+%Y%m%d_%H%M%S').log"

# 开始计时
echo "=========================================="
echo "  Docker构建性能监控"
echo "=========================================="
echo ""
echo "🚀 开始构建: $(date '+%Y-%m-%d %H:%M:%S')"
echo "镜像名称: $IMAGE_NAME"
echo ""

START_TIME=$(date +%s)

# 执行构建
echo "构建日志: $BUILD_LOG"
echo ""
docker build \
    --progress=plain \
    -t "$IMAGE_NAME" \
    -f ./backend/core/api/core-api/Dockerfile \
    . 2>&1 | tee -a "$BUILD_LOG"

# 结束计时
END_TIME=$(date +%s)
BUILD_DURATION=$((END_TIME - START_TIME))

echo ""
echo "=========================================="
echo "  ✅ 构建完成"
echo "=========================================="
echo ""
echo "构建结果:"
echo "- 构建时间: ${BUILD_DURATION}秒"
echo "- 镜像名称: $IMAGE_NAME"
echo "- 镜像大小: $(docker images $IMAGE_NAME --format '{{.Size}}')"
echo "- 构建日志: $BUILD_LOG"
echo ""

# 分析构建时间
echo "=========================================="
echo "  构建时间分析"
echo "=========================================="
echo ""
echo "总体构建时间: ${BUILD_DURATION}秒"

# 构建时间阈值检查
if [ $BUILD_DURATION -gt 180 ]; then
    echo ""
    echo "⚠️  警告: 构建时间超过180秒"
    echo ""
    echo "建议优化措施:"
    echo "1. 检查Dockerfile是否充分利用了多阶段构建"
    echo "2. 检查.dockerignore文件是否排除了不必要的文件"
    echo "3. 考虑使用Docker BuildKit加速构建"
    echo "4. 分离依赖和源码的缓存层"
    echo "5. 减小基础镜像大小"
else
    echo ""
    echo "✅ 构建时间在可接受范围内"
fi

# 性能趋势分析（如果存在历史记录）
echo ""
echo "=========================================="
echo "  性能趋势分析"
echo "=========================================="
echo ""

# 统计构建日志数量
LOG_COUNT=$(ls -1 build_metrics_*.log 2>/dev/null | wc -l)
if [ $LOG_COUNT -gt 1 ]; then
    echo "历史构建记录: $LOG_COUNT条"
    echo ""
    echo "最近5次构建时间:"
    ls -t build_metrics_*.log | head -n 5 | while read LOG_FILE; do
        TIME=$(grep -oP 'Build duration: \K\d+' "$LOG_FILE" 2>/dev/null || echo "N/A")
        DATE=$(echo "$LOG_FILE" | grep -oP 'build_metrics_\K[0-9_]+')
        echo "  - $DATE: ${TIME}s"
    done
fi

echo ""
echo "监控完成: $(date '+%Y-%m-%d %H:%M:%S')"
