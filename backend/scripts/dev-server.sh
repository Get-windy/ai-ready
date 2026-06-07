#!/bin/bash
# ============================================================
# AI-Ready 开发服务器启动脚本 (Git Bash / Linux)
# 特性: 主进程退出时自动清理所有 Java 子进程
# ============================================================
# 用法:
#   ./scripts/dev-server.sh                  # 启动后端
#   ./scripts/dev-server.sh -m user          # 启动指定模块
#   ./scripts/dev-server.sh -a               # 启动所有模块
#   ./scripts/dev-server.sh -k               # 仅清理进程
# ============================================================

set -euo pipefail

BACKEND_DIR="I:/AI-Ready/backend"
MODULE="core/api/core-api"
ALL=false
SKIP_BUILD=false
KILL_ONLY=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -m|--module) MODULE="$2"; shift 2 ;;
        -a|--all) ALL=true; shift ;;
        -s|--skip-build) SKIP_BUILD=true; shift ;;
        -k|--kill-only) KILL_ONLY=true; shift ;;
        *) echo "未知参数: $1"; exit 1 ;;
    esac
done

# ── 颜色 ──
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${YELLOW}$1${NC}"; }

# ── 记录子进程 PID ──
CHILD_PIDS=()

# ── 清理函数：杀掉所有 Java 子进程 ──
cleanup() {
    echo ""
    log_warn "🧹 清理 Java 进程中..."

    local count=0

    # 杀掉记录的 PID
    for pid in "${CHILD_PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill -TERM "$pid" 2>/dev/null || true
            log_info "   ✅ 已终止 PID: $pid"
            ((count++))
        fi
    done

    # 额外：查找并杀掉本项目相关的残留 Java 进程
    if command -v jps &>/dev/null; then
        local java_pids
        java_pids=$(jps -l 2>/dev/null | grep -iE "core-api|ai-ready|spring-boot" | awk '{print $1}' || true)
        if [[ -n "$java_pids" ]]; then
            for pid in $java_pids; do
                kill -9 "$pid" 2>/dev/null || true
                log_info "   ✅ 已清理残留 PID: $pid"
                ((count++))
            done
        fi
    fi

    # Windows 兜底: 用 taskkill（Git Bash 环境下）
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        local win_pids
        win_pids=$(tasklist //FI "IMAGENAME eq java.exe" //FO CSV //NH 2>/dev/null | grep -iE "core-api|ai-ready" || true)
        # 通过 jps 已经处理了，但 taskkill 可以杀掉所有 java.exe（太粗暴，不用）
    fi

    if [[ $count -eq 0 ]]; then
        echo -e "   ℹ️  没有需要清理的进程"
    else
        log_info "   ✅ 共清理 $count 个进程"
    fi

    exit 0
}

# ── 注册信号处理 ──
trap cleanup EXIT INT TERM

# ── 仅清理模式 ──
if [[ "$KILL_ONLY" == true ]]; then
    cleanup
    exit 0
fi

echo "=========================================="
echo "  AI-Ready 开发服务器启动脚本"
echo "=========================================="
echo ""

# ── 第一步：清理残留 ──
log_step "[Step 1] 清理残留 Java 进程..."
# 先取消 trap 再手动清理，避免 cleanup 导致 exit
if command -v jps &>/dev/null; then
    old_pids=$(jps -l 2>/dev/null | grep -iE "core-api|ai-ready|spring-boot" | awk '{print $1}' || true)
    if [[ -n "$old_pids" ]]; then
        for pid in $old_pids; do
            kill -9 "$pid" 2>/dev/null || true
            log_info "   ✅ 已清理旧进程 PID: $pid"
        done
    fi
fi
log_info "   清理完成"
echo ""

# ── 第二步：编译 ──
if [[ "$SKIP_BUILD" == false ]]; then
    log_step "[Step 2] 编译项目..."
    cd "$BACKEND_DIR"
    if mvn clean compile -DskipTests -q; then
        log_info "   ✅ 编译成功"
    else
        log_error "   ❌ 编译失败"
        exit 1
    fi
    echo ""
fi

# ── 第三步：启动 ──
log_step "[Step 3] 启动后端服务..."

cd "$BACKEND_DIR"

if [[ "$ALL" == true ]]; then
    log_info "   启动所有模块..."
    # shellcheck disable=SC2068
    mvn spring-boot:run &
    CHILD_PIDS+=($!)
else
    log_info "   启动模块: $MODULE"
    # shellcheck disable=SC2068
    mvn spring-boot:run -pl "$MODULE" &
    CHILD_PIDS+=($!)
fi

log_info "   🟢 后端服务启动中 (PID: ${CHILD_PIDS[-1]})"
echo -e "${GRAY}   按 Ctrl+C 停止服务（将自动清理所有进程）${NC}"
echo ""

# ── 等待子进程 ──
wait
