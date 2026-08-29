#!/bin/bash
# ============================================================
# AI-Ready 后端一键部署脚本
#
# 流程: 构建 → 重启服务 → 冒烟测试 → 结果汇报
# 运行: bash deploy.sh [module]
# 默认: module=core/api/core-api, port=5655 (dev profile)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE="${1:-core/api/core-api}"
PORT="${PORT:-5655}"
SMOKE_URL="http://localhost:${PORT}"
LOG_DIR="${BACKEND_DIR}/logs"
LOG_FILE="${LOG_DIR}/deploy.log"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "  ${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "  ${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "  ${RED}[ERROR]${NC} $1"; }

# 检查依赖
if ! command -v java &>/dev/null; then
    log_error "未检测到 Java，请先安装 JDK 17"
    exit 1
fi

echo ""
echo "============================================"
echo "  AI-Ready 后端部署 — $(date '+%Y-%m-%d %H:%M:%S')"
echo "  模块: ${MODULE}"
echo "  端口: ${PORT}"
echo "============================================"

# ── 1. 构建 ──
echo ""
echo "── Step 1/3: 构建后端 ──"
cd "${BACKEND_DIR}"
if [[ -x ./mvnw ]]; then
    MVN="./mvnw"
else
    MVN="mvn"
fi
if ${MVN} package -DskipTests -B -q; then
    log_info "构建成功"
else
    log_error "构建失败"
    exit 1
fi

# ── 2. 重启服务 ──
echo ""
echo "── Step 2/3: 重启服务 ──"

# 2.1 停止旧进程
if command -v jps &>/dev/null; then
    OLD_PIDS="$(jps -l 2>/dev/null | grep -iE 'core-api|ai-ready|spring-boot' | awk '{print $1}' || true)"
    if [[ -n "${OLD_PIDS}" ]]; then
        for pid in ${OLD_PIDS}; do
            kill -9 "${pid}" 2>/dev/null || true
            log_info "已停止旧进程 PID: ${pid}"
        done
    else
        log_info "没有需要停止的旧进程"
    fi
fi

# 2.2 定位打包产物
JAR_FILE="$(ls "${BACKEND_DIR}/${MODULE}"/target/*-exec.jar 2>/dev/null | head -1 || true)"
if [[ -z "${JAR_FILE}" ]]; then
    log_error "未找到可执行 JAR: ${MODULE}/target/*-exec.jar"
    exit 1
fi
log_info "可执行 JAR: ${JAR_FILE}"

# 2.3 启动服务
mkdir -p "${LOG_DIR}"
nohup java -jar "${JAR_FILE}" --spring.profiles.active=dev > "${LOG_FILE}" 2>&1 &
BACKEND_PID=$!
log_info "服务已启动 (PID: ${BACKEND_PID})"

# 2.4 等待健康检查通过
# 与 smoke-test.sh 一致: 只要 HTTP 有响应(非 000)即视为启动成功
# (未认证接口返回 401 也属于正常启动)
log_info "等待服务启动 (超时 120s)..."
WAITED=0
until [[ "$(curl -s -o /dev/null -w "%{http_code}" "${SMOKE_URL}/api/system/health" 2>/dev/null || echo "000")" != "000" ]]; do
    if [[ ${WAITED} -ge 120 ]]; then
        log_error "服务启动超时，最近日志:"
        tail -50 "${LOG_FILE}" || true
        exit 1
    fi
    sleep 5
    WAITED=$((WAITED + 5))
done
log_info "服务启动成功 (${SMOKE_URL})"

# ── 3. 冒烟测试 ──
echo ""
echo "── Step 3/3: 冒烟测试 ──"
if bash "${SCRIPT_DIR}/smoke-test.sh" "${SMOKE_URL}"; then
    SMOKE_RESULT="通过"
else
    SMOKE_RESULT="失败"
fi

# ── 汇总 ──
echo ""
echo "============================================"
if [[ "${SMOKE_RESULT}" == "通过" ]]; then
    echo -e "  ${GREEN}部署成功！${NC}"
    log_info "服务地址: ${SMOKE_URL}"
    log_info "进程 PID: ${BACKEND_PID}"
    log_info "日志文件: ${LOG_FILE}"
    echo "============================================"
    exit 0
else
    echo -e "  ${RED}部署失败，冒烟测试未通过！${NC}"
    log_info "服务日志: ${LOG_FILE}"
    echo "============================================"
    exit 1
fi
