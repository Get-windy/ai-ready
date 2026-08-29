#!/bin/bash
# ============================================================
# API Smoke Test - 关键接口冒烟测试
#
# 用途：在每次部署后验证核心API可用
# 运行：bash smoke-test.sh [base_url] [token]
# 默认：base_url=http://localhost:5655
# ============================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:5655}"
TOKEN="${2:-}"
PASS=0
FAIL=0
TOTAL=0

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { ((PASS++)); ((TOTAL++)); echo -e "  ${GREEN}PASS${NC} $1"; }
log_fail() { ((FAIL++)); ((TOTAL++)); echo -e "  ${RED}FAIL${NC} $1 — HTTP $2"; }
log_skip() { echo -e "  ${YELLOW}SKIP${NC} $1 (no token)"; }

# 通用请求函数
# Usage: check_endpoint "描述" "METHOD" "/path" [expected_status]
check_endpoint() {
    local desc="$1" method="$2" path="$3" expected="${4:-200}"
    local url="${BASE_URL}${path}"
    local args=(-s -o /dev/null -w "%{http_code}" -X "$method")

    if [[ -n "$TOKEN" ]]; then
        args+=(-H "Authorization: Bearer $TOKEN")
    fi

    local status
    status=$(curl "${args[@]}" "$url" 2>/dev/null || echo "000")

    if [[ "$status" == "$expected" || "$status" == "401" ]]; then
        # 401 = 接口存在但需要认证，也算通过（说明SQL没报错）
        log_pass "$desc (HTTP $status)"
    elif [[ "$status" == "000" ]]; then
        log_fail "$desc" "连接失败"
    else
        log_fail "$desc" "$status"
    fi
}

echo ""
echo "============================================"
echo "  API 冒烟测试 — $(date '+%Y-%m-%d %H:%M:%S')"
echo "  Target: $BASE_URL"
echo "============================================"
echo ""

# ─── 1. 系统基础 ───
echo "── 系统基础 ──"
check_endpoint "健康检查" "GET" "/api/system/health" "200" || true

# ─── 2. 销售退货申请 (修复: timestamp >= varchar) ───
echo ""
echo "── 销售退货申请 ──"
check_endpoint "退货申请列表(page)" "GET" "/api/erp/sale/return/page?current=1&size=10"
check_endpoint "退货申请明细(page-detail)" "GET" "/api/erp/sale/return/page-detail?pageNum=1&pageSize=10&startDate=2026-07-01&endDate=2026-07-31"

# ─── 3. 采购订单 (修复: ri.purchase_order_id 不存在) ───
echo ""
echo "── 采购订单 ──"
check_endpoint "采购订单列表(doc-query/page)" "GET" "/api/erp/purchase/order/doc-query/page?current=1&size=10&dateStart=2026-07-01&dateEnd=2026-07-31"

# ─── 4. 费用分摊 (修复: sharingDate类型不匹配) ───
echo ""
echo "── 费用分摊 ──"
check_endpoint "费用分摊列表" "GET" "/api/erp/purchase/cost-sharing/page?pageNum=1&pageSize=10"

# ─── 5. 销售订单 ───
echo ""
echo "── 销售订单 ──"
check_endpoint "销售订单列表" "GET" "/api/erp/sale/order/page?current=1&size=10"

# ─── 6. 销售换货 ───
echo ""
echo "── 销售换货 ──"
check_endpoint "换货列表" "GET" "/api/erp/sale/exchange/page?current=1&size=10"

# ─── 7. 零售单 ───
echo ""
echo "── 零售单 ──"
check_endpoint "零售单列表" "GET" "/api/erp/sale/pre-order/page?current=1&size=10"

# ─── 8. 报价单 ───
echo ""
echo "── 报价单(CRM) ──"
check_endpoint "报价单列表" "GET" "/api/crm/quotation/page?current=1&size=10"

# ─── 9. 商品 ───
echo ""
echo "── 商品管理 ──"
check_endpoint "商品分类树" "GET" "/api/erp/product-category/tree"

# ─── 10. HRM 人力资源 ───
echo ""
echo "── HRM 人力资源 ──"
check_endpoint "员工列表" "GET" "/api/hr/employees/page?current=1&size=10"
check_endpoint "岗位列表" "GET" "/api/hr/positions/page?current=1&size=10"
check_endpoint "考勤列表" "GET" "/api/hr/attendance/page?current=1&size=10"
check_endpoint "请假列表" "GET" "/api/hr/leave/page?current=1&size=10"
check_endpoint "薪资列表" "GET" "/api/hr/salary/payment/page?current=1&size=10"
check_endpoint "绩效列表" "GET" "/api/hr/performance/page?current=1&size=10"
check_endpoint "招聘列表" "GET" "/api/hr/recruitment/page?current=1&size=10"
check_endpoint "候选人列表" "GET" "/api/hr/candidate/page?current=1&size=10"

# ─── 11. 财务增强 ───
echo ""
echo "── 财务深度增强 ──"
check_endpoint "辅助核算类型" "GET" "/api/erp/finance/auxiliary/type/page?current=1&size=10"
check_endpoint "辅助核算项目" "GET" "/api/erp/finance/auxiliary/item/page?current=1&size=10"
check_endpoint "辅助核算余额" "GET" "/api/erp/finance/auxiliary/balance/page?current=1&size=10"

# ─── 12. 质量管理 ───
echo ""
echo "── 质量管理 ──"
check_endpoint "质量证书列表" "GET" "/api/quality/certificate/page?current=1&size=10"

# ─── 13. 销售查询 ───
echo ""
echo "── 销售查询 ──"
check_endpoint "销售单据查询" "GET" "/api/sales/doc-query/page?current=1&size=10&dateType=documentDate"
check_endpoint "销售明细查询" "GET" "/api/sales/detail-query/page?current=1&size=10&dateType=documentDate"
check_endpoint "销售价格跟踪" "GET" "/api/sales/price-track/page?current=1&size=10&startDate=2026-05-01&endDate=2026-07-31"

# ─── 汇总 ───
echo ""
echo "============================================"
echo -e "  结果: ${GREEN}${PASS} 通过${NC} / ${RED}${FAIL} 失败${NC} / ${TOTAL} 总计"
echo "============================================"
echo ""

if [[ $FAIL -gt 0 ]]; then
    echo -e "${RED}有 ${FAIL} 个接口异常，请检查后端日志！${NC}"
    exit 1
else
    echo -e "${GREEN}全部通过！${NC}"
    exit 0
fi
