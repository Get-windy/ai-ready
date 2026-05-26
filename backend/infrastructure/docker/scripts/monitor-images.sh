#!/bin/bash

# Docker镜像监控脚本
# 用途: 监控Docker镜像的存储使用情况
# 作者: devops-engineer
# 创建日期: 2026-04-27

echo "=========================================="
echo "  Docker镜像存储监控"
echo "=========================================="
echo "监控时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 镜像存储使用情况
echo "=== 镜像存储使用情况 ==="
docker system df -v

# 镜像数量统计
echo ""
echo "=== 镜像数量统计 ==="
TOTAL_IMAGES=$(docker images -q | wc -l)
DUPLICATE_IMAGES=$(docker images -f dangling=true -q | wc -l)
echo "总镜像数: $TOTAL_IMAGES"
echo "悬空镜像数: $DUPLICATE_IMAGES"

# 占用空间最大的10个镜像
echo ""
echo "=== 占用空间最大的10个镜像 ==="
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | \
    tail -n 11 | \
    tac

# 检查存储使用率
echo ""
echo "=== 存储使用率检查 ==="
DISK_USAGE=$(df -h /var/lib/docker 2>/dev/null | tail -n 1 | awk '{print $5}')
if [ -n "$DISK_USAGE" ]; then
    echo "Docker存储使用率: $DISK_USAGE"
    
    # 检查是否超过阈值
    USAGE_VALUE=${DISK_USAGE%\%}
    if [ "$USAGE_VALUE" -gt 80 ]; then
        echo "⚠️  警告: 存储使用率超过80%"
    elif [ "$USAGE_VALUE" -gt 90 ]; then
        echo "❌ 危险: 存储使用率超过90%"
    else
        echo "✅ 正常: 存储使用率在安全范围内"
    fi
else
    echo "无法获取存储使用率信息"
fi

# 镜像版本统计
echo ""
echo "=== 镜像版本统计 ==="
docker images --format "{{.Repository}}:{{.Tag}}" | \
    grep -v "<none>" | \
    wc -l | \
    xargs -I {} echo "总镜像Tag数: {}"

echo ""
echo "=========================================="
echo "  ✅ 监控完成"
echo "=========================================="
