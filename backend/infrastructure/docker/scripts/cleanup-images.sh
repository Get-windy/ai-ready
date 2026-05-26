#!/bin/bash

# Docker镜像清理脚本
# 用途: 清理不再使用的Docker镜像，释放存储空间
# 作者: devops-engineer
# 创建日期: 2026-04-27

set -e

echo "=========================================="
echo "  Docker镜像清理"
echo "=========================================="
echo ""

# 1. 显示当前镜像使用情况
echo "📊 当前Docker镜像使用情况:"
echo ""
docker system df -v

# 2. 清理悬空镜像（无线头镜像）
echo ""
echo "🗑️  清理悬空镜像（<none>）..."
docker image prune -f

# 3. 清理未使用的镜像（超过7天）
echo ""
echo "🗑️  清理7天前的未使用镜像..."
docker image prune -a --filter "until=168h" -f

# 4. 显示清理后的使用情况
echo ""
echo "📊 清理后的Docker镜像使用情况:"
echo ""
docker system df -v

# 5. 清理构建缓存（超过24小时）
echo ""
echo "🗑️  清理旧的构建缓存（24小时以上）..."
docker builder prune -a --filter "until=24h" -f

# 6. 显示清理结果
echo ""
echo "=========================================="
echo "  ✅ 镜像清理完成"
echo "=========================================="
echo ""

# 获取清理后的存储信息
USAGE=$(docker system df | tail -n 1)
echo "当前存储使用情况: $USAGE"
echo ""
echo "清理时间: $(date '+%Y-%m-%d %H:%M:%S')"
