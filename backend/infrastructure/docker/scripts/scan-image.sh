#!/bin/bash

# Docker镜像安全扫描脚本
# 用途: 扫描Docker镜像中的漏洞
# 作者: devops-engineer
# 创建日期: 2026-04-27

set -e

# 配置
IMAGE_NAME=${1:-"aiready/api-gateway:test"}
SCAN_SEVERITY=${2:-"CRITICAL,HIGH"}
OUTPUT_FORMAT=${3:-"table"}

echo "=========================================="
echo "  Docker镜像安全扫描"
echo "=========================================="
echo ""
echo "🔍 扫描目标: $IMAGE_NAME"
echo "🚀 扫描严重级别: $SCAN_SEVERITY"
echo "📊 输出格式: $OUTPUT_FORMAT"
echo ""

# 执行扫描
echo "开始扫描..."
echo ""

trivy image \
    --severity "$SCAN_SEVERITY" \
    --format "$OUTPUT_FORMAT" \
    --exit-code 1 \
    --ignore-unfixed \
    "$IMAGE_NAME"

echo ""
echo "=========================================="
echo "  ✅ 镜像扫描完成"
echo "=========================================="
echo ""
echo "扫描结果:"
echo "- 镜像: $IMAGE_NAME"
echo "- 扫描时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "- 扫描工具: Trivy"
echo "- 严重级别: $SCAN_SEVERITY"
echo ""
echo "建议:"
echo "1. 如果发现高危漏洞，请及时修复"
echo "2. 使用基础镜像的最新版本"
echo "3. 移除不必要的软件包"
echo "4. 非root用户运行容器"
