#!/bin/bash
# =============================================================================
# Sprint 27+1 - 一键快速验证脚本
# =============================================================================
# 功能：一键执行所有验证并生成综合报告
# 用法：./quick-validate.sh [--output <file>]
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT="${1:-}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_DIR="${SCRIPT_DIR}/reports"
REPORT_FILE="${REPORT_DIR}/validation_report_${TIMESTAMP}.md"

mkdir -p "$REPORT_DIR"

echo "========================================"
echo "Sprint 27+1 一键部署验证"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# 执行各项验证
echo ""
echo "[1/4] 执行部署验证..."
"${SCRIPT_DIR}/validate-deployment.sh" -q -o "${REPORT_DIR}/deploy_${TIMESTAMP}.md"
DEPLOY_RESULT=$?

echo ""
echo "[2/4] 执行健康检查..."
"${SCRIPT_DIR}/health-check.sh" all > "${REPORT_DIR}/health_${TIMESTAMP}.log" 2>&1 || true
HEALTH_RESULT=$?

echo ""
echo "[3/4] 执行API验证..."
"${SCRIPT_DIR}/api-validation.sh" > "${REPORT_DIR}/api_${TIMESTAMP}.log" 2>&1 || true
API_RESULT=$?

echo ""
echo "[4/4] 生成综合报告..."

# 生成综合报告
cat > "$REPORT_FILE" << EOF
# Sprint 27+1 部署验证综合报告

**验证时间**: $(date '+%Y-%m-%d %H:%M:%S')  
**报告ID**: ${TIMESTAMP}

## 验证结果汇总

| 验证项 | 状态 | 详情 |
|--------|------|------|
| 部署验证 | $(if [ "$DEPLOY_RESULT" -eq 0 ]; then echo "✅ 通过"; else echo "❌ 失败"; fi) | 见 deploy_${TIMESTAMP}.md |
| 健康检查 | $(if [ "$HEALTH_RESULT" -eq 0 ]; then echo "✅ 通过"; else echo "❌ 失败"; fi) | 见 health_${TIMESTAMP}.log |
| API验证 | $(if [ "$API_RESULT" -eq 0 ]; then echo "✅ 通过"; else echo "❌ 失败"; fi) | 见 api_${TIMESTAMP}.log |

## 详细日志

### 部署验证日志
\`\`\`
$(cat "${REPORT_DIR}/deploy_${TIMESTAMP}.md" 2>/dev/null || echo "无日志")
\`\`\`

### 健康检查日志
\`\`\`
$(cat "${REPORT_DIR}/health_${TIMESTAMP}.log" 2>/dev/null || echo "无日志")
\`\`\`

### API验证日志
\`\`\`
$(cat "${REPORT_DIR}/api_${TIMESTAMP}.log" 2>/dev/null || echo "无日志")
\`\`\`

## 结论

$(if [ "$DEPLOY_RESULT" -eq 0 ] && [ "$HEALTH_RESULT" -eq 0 ] && [ "$API_RESULT" -eq 0 ]; then
    echo "✅ 所有验证项通过，测试环境部署成功。"
else
    echo "❌ 存在验证失败项，请查看详细日志并修复。"
fi)

---
*报告由 quick-validate.sh 自动生成*
EOF

echo ""
echo "========================================"
echo "验证完成"
echo "报告位置: ${REPORT_FILE}"
echo "========================================"

if [ -n "$OUTPUT" ]; then
    cp "$REPORT_FILE" "$OUTPUT"
    echo "报告已复制到: ${OUTPUT}"
fi

# 总体退出码
if [ "$DEPLOY_RESULT" -ne 0 ] || [ "$HEALTH_RESULT" -ne 0 ] || [ "$API_RESULT" -ne 0 ]; then
    exit 1
fi
exit 0
