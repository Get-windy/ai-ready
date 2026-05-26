#!/bin/bash

# 优化测试执行脚本
# 支持并行执行、智能测试选择、详细报告生成

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTS_DIR="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="$TESTS_DIR/reports"
RESULTS_DIR="$TESTS_DIR/results"
LOG_DIR="$TESTS_DIR/logs"
TEST_DATA_DIR="$TESTS_DIR/test-data"

# 创建目录
mkdir -p "$REPORTS_DIR" "$RESULTS_DIR" "$LOG_DIR" "$TEST_DATA_DIR"

# 日期时间戳
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOG_DIR/test_execution_${TIMESTAMP}.log"
JUNIT_REPORT="$REPORTS_DIR/junit_${TIMESTAMP}.xml"
HTML_REPORT="$REPORTS_DIR/report_${TIMESTAMP}.html"
COVERAGE_REPORT="$REPORTS_DIR/coverage_${TIMESTAMP}.html"

# 函数定义
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 参数解析
TEST_TYPE="all"
PARALLEL="auto"
VERBOSE=false
COVERAGE=false
MARKERS=""
SKIP_MARKERS=""
FAIL_FAST=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --type)
            TEST_TYPE="$2"
            shift 2
            ;;
        --parallel)
            PARALLEL="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        --markers)
            MARKERS="$2"
            shift 2
            ;;
        --skip-markers)
            SKIP_MARKERS="$2"
            shift 2
            ;;
        --fail-fast)
            FAIL_FAST=true
            shift
            ;;
        --help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  --type <type>        测试类型: all, unit, api, integration, e2e, performance"
            echo "  --parallel <n>       并行进程数 (默认: auto)"
            echo "  --verbose            详细输出模式"
            echo "  --coverage           生成覆盖率报告"
            echo "  --markers <expr>     测试标记表达式"
            echo "  --skip-markers <expr> 跳过测试标记表达式"
            echo "  --fail-fast          快速失败模式"
            echo "  --help               显示帮助信息"
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            exit 1
            ;;
    esac
done

# 根据测试类型设置默认标记
case $TEST_TYPE in
    "unit")
        DEFAULT_MARKERS="unit"
        ;;
    "api")
        DEFAULT_MARKERS="api"
        ;;
    "integration")
        DEFAULT_MARKERS="integration"
        ;;
    "e2e")
        DEFAULT_MARKERS="e2e"
        ;;
    "performance")
        DEFAULT_MARKERS="performance"
        ;;
    "fast")
        DEFAULT_MARKERS="fast"
        ;;
    "smoke")
        DEFAULT_MARKERS="smoke"
        ;;
    "regression")
        DEFAULT_MARKERS="regression"
        ;;
    *)
        DEFAULT_MARKERS=""
        ;;
esac

# 合并标记
if [ -n "$MARKERS" ] && [ -n "$DEFAULT_MARKERS" ]; then
    FINAL_MARKERS="$DEFAULT_MARKERS and $MARKERS"
elif [ -n "$MARKERS" ]; then
    FINAL_MARKERS="$MARKERS"
elif [ -n "$DEFAULT_MARKERS" ]; then
    FINAL_MARKERS="$DEFAULT_MARKERS"
else
    FINAL_MARKERS=""
fi

# 构建pytest命令
PYTEST_CMD="pytest"

# 添加标记过滤
if [ -n "$FINAL_MARKERS" ]; then
    PYTEST_CMD="$PYTEST_CMD -m \"$FINAL_MARKERS\""
fi

# 添加跳过标记
if [ -n "$SKIP_MARKERS" ]; then
    PYTEST_CMD="$PYTEST_CMD -m \"not $SKIP_MARKERS\""
fi

# 添加其他选项
PYTEST_CMD="$PYTEST_CMD -n $PARALLEL"
PYTEST_CMD="$PYTEST_CMD --junitxml=\"$JUNIT_REPORT\""
PYTEST_CMD="$PYTEST_CMD --html=\"$HTML_REPORT\""
PYTEST_CMD="$PYTEST_CMD --self-contained-html"
PYTEST_CMD="$PYTEST_CMD --tb=short"
PYTEST_CMD="$PYTEST_CMD --durations=10"

if [ "$FAIL_FAST" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --maxfail=1"
fi

if [ "$VERBOSE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD -v"
fi

if [ "$COVERAGE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --cov=backend --cov-report=term-missing --cov-report=html:\"$COVERAGE_REPORT\""
fi

# 执行测试
print_header "开始执行测试"
log_message "测试类型: $TEST_TYPE"
log_message "并行进程: $PARALLEL"
log_message "测试标记: ${FINAL_MARKERS:-全部}"
log_message "跳过标记: ${SKIP_MARKERS:-无}"
log_message "详细模式: $VERBOSE"
log_message "覆盖率: $COVERAGE"
log_message "快速失败: $FAIL_FAST"

print_info "执行命令: $PYTEST_CMD"

START_TIME=$(date +%s)

# 执行测试
cd "$TESTS_DIR"
eval $PYTEST_CMD 2>&1 | tee -a "$LOG_FILE"
EXIT_CODE=${PIPESTATUS[0]}

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# 生成执行摘要
print_header "测试执行摘要"
log_message "执行时间: ${DURATION}秒"
log_message "退出代码: $EXIT_CODE"

if [ $EXIT_CODE -eq 0 ]; then
    print_success "所有测试通过!"
    
    # 生成成功报告
    REPORT_FILE="$REPORTS_DIR/test_summary_${TIMESTAMP}.md"
    cat > "$REPORT_FILE" << EOF
# 测试执行报告

**执行时间**: $(date)
**持续时间**: ${DURATION}秒
**测试类型**: $TEST_TYPE
**并行进程**: $PARALLEL
**测试标记**: ${FINAL_MARKERS:-全部}
**结果**: ✅ 全部通过

## 详细报告
- JUnit报告: $JUNIT_REPORT
- HTML报告: $HTML_REPORT
$([ "$COVERAGE" = true ] && echo "- 覆盖率报告: $COVERAGE_REPORT")
- 日志文件: $LOG_FILE

## 执行统计
\`\`\`
$(tail -20 "$LOG_FILE" | grep -E "(passed|failed|skipped|errors)" | tail -5)
\`\`\`

## 性能统计
\`\`\`
$(tail -15 "$LOG_FILE" | grep -A5 "slowest test durations")
\`\`\`
EOF
    
    print_success "报告已生成: $REPORT_FILE"
else
    print_error "测试失败!"
    
    # 生成失败报告
    REPORT_FILE="$REPORTS_DIR/test_failure_${TIMESTAMP}.md"
    cat > "$REPORT_FILE" << EOF
# 测试失败报告

**执行时间**: $(date)
**持续时间**: ${DURATION}秒
**测试类型**: $TEST_TYPE
**并行进程**: $PARALLEL
**测试标记**: ${FINAL_MARKERS:-全部}
**结果**: ❌ 测试失败

## 失败详情
\`\`\`
$(tail -50 "$LOG_FILE" | grep -B5 -A5 "FAILED\|ERROR")
\`\`\`

## 详细报告
- JUnit报告: $JUNIT_REPORT
- HTML报告: $HTML_REPORT
$([ "$COVERAGE" = true ] && echo "- 覆盖率报告: $COVERAGE_REPORT")
- 日志文件: $LOG_FILE

## 建议操作
1. 查看失败测试的详细日志
2. 检查测试环境配置
3. 验证测试数据是否正确
4. 运行单个失败测试进行调试
\`\`\`bash
pytest path/to/failed_test.py::test_function -v
\`\`\`
EOF
    
    print_error "失败报告已生成: $REPORT_FILE"
fi

# 清理临时文件（保留最近7天的报告）
find "$REPORTS_DIR" -name "*.xml" -mtime +7 -delete
find "$REPORTS_DIR" -name "*.html" -mtime +7 -delete
find "$LOG_DIR" -name "*.log" -mtime +7 -delete

print_header "测试执行完成"
log_message "报告文件位置: $REPORTS_DIR"
log_message "日志文件位置: $LOG_DIR"

exit $EXIT_CODE