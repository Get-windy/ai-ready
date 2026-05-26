#!/bin/bash
# =============================================================================
# Sprint 27+1 - API接口验证脚本
# =============================================================================
# 功能：验证测试环境API接口可用性和响应正确性
# 用法：./api-validation.sh [options]
# =============================================================================

set -euo pipefail

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
FAILED=0
PASSED=0

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

ok() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASSED++)) || true; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; ((FAILED++)) || true; }
log() { echo -e "${BLUE}[INFO]${NC} $1"; }

# 测试API端点
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_code="${3:-200}"
    local description="${4:-}"
    
    local url="${BASE_URL}${endpoint}"
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" 2>/dev/null || echo "000")
    
    if [ "$response" = "$expected_code" ]; then
        ok "${method} ${endpoint} ${description:+($description) }→ HTTP ${response}"
        return 0
    else
        fail "${method} ${endpoint} ${description:+($description) }→ HTTP ${response} (期望 ${expected_code})"
        return 1
    fi
}

# 测试API并验证响应体
test_endpoint_with_body() {
    local method="$1"
    local endpoint="$2"
    local expected_code="${3:-200}"
    local expected_field="$4"
    local description="${5:-}"
    
    local url="${BASE_URL}${endpoint}"
    local response_body
    local response_code
    
    response_body=$(curl -s -X "$method" "$url" 2>/dev/null || echo "")
    response_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" 2>/dev/null || echo "000")
    
    if [ "$response_code" != "$expected_code" ]; then
        fail "${method} ${endpoint} → HTTP ${response_code} (期望 ${expected_code})"
        return 1
    fi
    
    if [ -n "$expected_field" ] && echo "$response_body" | grep -q "$expected_field"; then
        ok "${method} ${endpoint} ${description:+($description) }→ HTTP ${response_code}, 包含字段 '${expected_field}'"
        return 0
    elif [ -n "$expected_field" ]; then
        fail "${method} ${endpoint} → 响应中未找到字段 '${expected_field}'"
        return 1
    else
        ok "${method} ${endpoint} ${description:+($description) }→ HTTP ${response_code}"
        return 0
    fi
}

log "========================================"
log "Sprint 27+1 API接口验证"
log "基础URL: ${BASE_URL}"
log "========================================"

# 健康检查端点
log "--- 健康检查端点 ---"
test_endpoint "GET" "/actuator/health" 200 "健康检查"
test_endpoint "GET" "/actuator/info" 200 "应用信息"

# 认证相关
log "--- 认证接口 ---"
test_endpoint "POST" "/api/auth/login" 400 "登录(无参数返回400)"
test_endpoint "POST" "/api/auth/register" 400 "注册(无参数返回400)"

# 用户相关
log "--- 用户接口 ---"
test_endpoint "GET" "/api/users" 401 "获取用户列表(未认证)"
test_endpoint "GET" "/api/users/profile" 401 "获取用户资料(未认证)"

# 系统相关
log "--- 系统接口 ---"
test_endpoint "GET" "/api/system/status" 200 "系统状态"

# 测试报告
log "========================================"
log "验证完成: 通过 ${PASSED}, 失败 ${FAILED}"
log "========================================"

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
exit 0
