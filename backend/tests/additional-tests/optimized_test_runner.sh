#!/bin/bash
# ============================================
# AI-Ready 项目优化测试执行脚本
# 版本：v2.0 - Sprint 28+1
# ============================================

set -euo pipefail

# 颜色输出定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="I:/AI-Ready"
BACKEND_DIR="${PROJECT_ROOT}/backend"
TESTS_DIR="${PROJECT_ROOT}/tests"
REPORTS_DIR="${PROJECT_ROOT}/tests/reports"

# 配置参数
TEST_TIMEOUT="${TEST_TIMEOUT:-60}"  # 单个测试超时时间（秒）
PARALLEL_WORKERS="${PARALLEL_WORKERS:-auto}"  # 并行工作进程数
MAX_FAILURES="${MAX_FAILURES:-3}"  # 最大失败数
RETRY_COUNT="${RETRY_COUNT:-2}"  # 失败重试次数

# 创建报告目录
create_report_dirs() {
    echo -e "${BLUE}[INFO] 创建报告目录...${NC}"
    
    mkdir -p "${REPORTS_DIR}/html"
    mkdir -p "${REPORTS_DIR}/json"
    mkdir -p "${REPORTS_DIR}/junit"
    mkdir -p "${REPORTS_DIR}/coverage"
    mkdir -p "${REPORTS_DIR}/logs"
    
    echo -e "${GREEN}[SUCCESS] 报告目录创建完成${NC}"
}

# 清理旧报告
cleanup_old_reports() {
    echo -e "${BLUE}[INFO] 清理旧报告...${NC}"
    
    # 保留最新的5份报告
    for dir in "${REPORTS_DIR}/html" "${REPORTS_DIR}/json" "${REPORTS_DIR}/junit"; do
        if [ -d "$dir" ]; then
            # 查找超过7天的报告文件
            find "$dir" -name "*.html" -mtime +7 -delete 2>/dev/null || true
            find "$dir" -name "*.json" -mtime +7 -delete 2>/dev/null || true
            find "$dir" -name "*.xml" -mtime +7 -delete 2>/dev/null || true
        fi
    done
    
    echo -e "${GREEN}[SUCCESS] 旧报告清理完成${NC}"
}

# 环境检查
check_environment() {
    echo -e "${BLUE}[INFO] 检查测试环境...${NC}"
    
    # 检查Python环境
    if ! command -v python &> /dev/null; then
        echo -e "${RED}[ERROR] Python未安装${NC}"
        exit 1
    fi
    
    # 检查pytest安装
    if ! python -m pytest --version &> /dev/null; then
        echo -e "${RED}[ERROR] pytest未安装，请执行: pip install pytest${NC}"
        exit 1
    fi
    
    # 检查pytest-xdist（并行测试）
    if ! python -c "import xdist" &> /dev/null; then
        echo -e "${YELLOW}[WARNING] pytest-xdist未安装，并行测试将不可用${NC}"
        echo -e "${YELLOW}          请执行: pip install pytest-xdist${NC}"
    fi
    
    # 检查pytest-html（HTML报告）
    if ! python -c "import pytest_html" &> /dev/null; then
        echo -e "${YELLOW}[WARNING] pytest-html未安装，HTML报告将不可用${NC}"
        echo -e "${YELLOW}          请执行: pip install pytest-html${NC}"
    fi
    
    echo -e "${GREEN}[SUCCESS] 环境检查完成${NC}"
}

# 执行单元测试
run_unit_tests() {
    local test_path="$1"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    echo -e "${BLUE}[INFO] 执行单元测试: ${test_path}${NC}"
    
    cd "${BACKEND_DIR}"
    
    python -m pytest "${test_path}" \
        --config-file="${TESTS_DIR}/optimized_pytest.ini" \
        --tb=short \
        --disable-warnings \
        --maxfail=${MAX_FAILURES} \
        --timeout=${TEST_TIMEOUT} \
        --html="${REPORTS_DIR}/html/unit-tests_${timestamp}.html" \
        --json-report --json-report-file="${REPORTS_DIR}/json/unit-tests_${timestamp}.json" \
        --junitxml="${REPORTS_DIR}/junit/unit-tests_${timestamp}.xml" \
        --cov=backend \
        --cov-report=html:"${REPORTS_DIR}/coverage/unit_${timestamp}" \
        --cov-report=xml:"${REPORTS_DIR}/coverage/unit-coverage_${timestamp}.xml" \
        --cov-report=term \
        -m "unit and not slow" \
        2>&1 | tee "${REPORTS_DIR}/logs/unit-tests_${timestamp}.log"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] 单元测试执行完成${NC}"
    else
        echo -e "${YELLOW}[WARNING] 单元测试执行完成，但有失败${NC}"
    fi
    
    return $exit_code
}

# 执行API测试
run_api_tests() {
    local test_path="$1"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    echo -e "${BLUE}[INFO] 执行API测试: ${test_path}${NC}"
    
    cd "${BACKEND_DIR}"
    
    python -m pytest "${test_path}" \
        --config-file="${TESTS_DIR}/optimized_pytest.ini" \
        --tb=short \
        --disable-warnings \
        --maxfail=${MAX_FAILURES} \
        --timeout=${TEST_TIMEOUT} \
        --html="${REPORTS_DIR}/html/api-tests_${timestamp}.html" \
        --json-report --json-report-file="${REPORTS_DIR}/json/api-tests_${timestamp}.json" \
        --junitxml="${REPORTS_DIR}/junit/api-tests_${timestamp}.xml" \
        --cov=backend \
        --cov-report=html:"${REPORTS_DIR}/coverage/api_${timestamp}" \
        --cov-report=xml:"${REPORTS_DIR}/coverage/api-coverage_${timestamp}.xml" \
        --cov-report=term \
        -m "api and not slow" \
        2>&1 | tee "${REPORTS_DIR}/logs/api-tests_${timestamp}.log"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] API测试执行完成${NC}"
    else
        echo -e "${YELLOW}[WARNING] API测试执行完成，但有失败${NC}"
    fi
    
    return $exit_code
}

# 执行并行测试
run_parallel_tests() {
    local test_path="$1"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    echo -e "${BLUE}[INFO] 执行并行测试: ${test_path}${NC}"
    
    cd "${BACKEND_DIR}"
    
    python -m pytest "${test_path}" \
        --config-file="${TESTS_DIR}/optimized_pytest.ini" \
        -n ${PARALLEL_WORKERS} \
        --dist=loadscope \
        --tb=short \
        --disable-warnings \
        --maxfail=${MAX_FAILURES} \
        --timeout=${TEST_TIMEOUT} \
        --html="${REPORTS_DIR}/html/parallel-tests_${timestamp}.html" \
        --json-report --json-report-file="${REPORTS_DIR}/json/parallel-tests_${timestamp}.json" \
        --junitxml="${REPORTS_DIR}/junit/parallel-tests_${timestamp}.xml" \
        -m "not slow" \
        2>&1 | tee "${REPORTS_DIR}/logs/parallel-tests_${timestamp}.log"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] 并行测试执行完成${NC}"
    else
        echo -e "${YELLOW}[WARNING] 并行测试执行完成，但有失败${NC}"
    fi
    
    return $exit_code
}

# 执行所有测试
run_all_tests() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    echo -e "${BLUE}[INFO] 执行所有测试...${NC}"
    
    cd "${BACKEND_DIR}"
    
    python -m pytest "tests/tests" \
        --config-file="${TESTS_DIR}/optimized_pytest.ini" \
        -n ${PARALLEL_WORKERS} \
        --dist=loadscope \
        --tb=short \
        --disable-warnings \
        --maxfail=${MAX_FAILURES} \
        --timeout=${TEST_TIMEOUT} \
        --html="${REPORTS_DIR}/html/all-tests_${timestamp}.html" \
        --json-report --json-report-file="${REPORTS_DIR}/json/all-tests_${timestamp}.json" \
        --junitxml="${REPORTS_DIR}/junit/all-tests_${timestamp}.xml" \
        --cov=backend \
        --cov-report=html:"${REPORTS_DIR}/coverage/all_${timestamp}" \
        --cov-report=xml:"${REPORTS_DIR}/coverage/all-coverage_${timestamp}.xml" \
        --cov-report=term \
        -m "not slow" \
        2>&1 | tee "${REPORTS_DIR}/logs/all-tests_${timestamp}.log"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] 所有测试执行完成${NC}"
    else
        echo -e "${YELLOW}[WARNING] 所有测试执行完成，但有失败${NC}"
    fi
    
    return $exit_code
}

# 生成测试报告摘要
generate_report_summary() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    echo -e "${BLUE}[INFO] 生成测试报告摘要...${NC}"
    
    # 查找最新的JSON报告
    local latest_report=$(ls -t "${REPORTS_DIR}/json"/*.json 2>/dev/null | head -1)
    
    if [ -z "$latest_report" ] || [ ! -f "$latest_report" ]; then
        echo -e "${YELLOW}[WARNING] 未找到测试报告${NC}"
        return 1
    fi
    
    # 解析JSON报告
    local total_tests=$(jq -r '.summary.total' "$latest_report" 2>/dev/null || echo "0")
    local passed=$(jq -r '.summary.passed' "$latest_report" 2>/dev/null || echo "0")
    local failed=$(jq -r '.summary.failed' "$latest_report" 2>/dev/null || echo "0")
    local skipped=$(jq -r '.summary.skipped' "$latest_report" 2>/dev/null || echo "0")
    local duration=$(jq -r '.summary.duration' "$latest_report" 2>/dev/null || echo "0")
    
    # 计算通过率
    local pass_rate=0
    if [ "$total_tests" -gt 0 ]; then
        pass_rate=$((passed * 100 / total_tests))
    fi
    
    # 生成摘要报告
    cat > "${REPORTS_DIR}/summary_${timestamp}.md" << EOF
# 测试执行摘要报告
## 基本信息
- **项目**: AI-Ready
- **执行时间**: $(date)
- **测试框架**: pytest (优化版本)
- **执行模式**: 并行执行 (${PARALLEL_WORKERS} workers)

## 测试结果统计
| 指标 | 数量 | 百分比 |
|------|------|--------|
| 总测试用例 | ${total_tests} | 100% |
| 通过 | ${passed} | ${pass_rate}% |
| 失败 | ${failed} | $((failed * 100 / total_tests))% |
| 跳过 | ${skipped} | $((skipped * 100 / total_tests))% |
| 执行时长 | ${duration}秒 | - |

## 详细报告链接
- HTML报告: [file://${REPORTS_DIR}/html/$(basename $(ls -t "${REPORTS_DIR}/html"/*.html 2>/dev/null | head -1))]
- JSON报告: ${latest_report}
- JUnit报告: ${REPORTS_DIR}/junit/$(basename $(ls -t "${REPORTS_DIR}/junit"/*.xml 2>/dev/null | head -1))
- 覆盖率报告: [file://${REPORTS_DIR}/coverage/all_${timestamp}/index.html]

## 建议
1. 通过率 ${pass_rate}%，${([ $pass_rate -ge 90 ] && echo "优秀") || ([ $pass_rate -ge 80 ] && echo "良好") || ([ $pass_rate -ge 70 ] && echo "一般") || echo "需要改进"} 
2. 失败用例: ${failed}个，请查看详细报告分析原因
3. 执行效率: ${duration}秒，${([ $(echo "$duration < 300" | bc) -eq 1 ] && echo "优秀") || ([ $(echo "$duration < 600" | bc) -eq 1 ] && echo "良好") || echo "需要优化"}

EOF
    
    echo -e "${GREEN}[SUCCESS] 报告摘要生成完成${NC}"
    echo -e "${BLUE}[INFO] 摘要报告位置: ${REPORTS_DIR}/summary_${timestamp}.md${NC}"
}

# 主函数
main() {
    local mode="${1:-all}"
    local test_path="${2:-}"
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  AI-Ready 测试框架优化执行脚本${NC}"
    echo -e "${GREEN}  版本: 2.0 | Sprint 28+1${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    # 初始化
    create_report_dirs
    cleanup_old_reports
    check_environment
    
    # 根据模式执行测试
    case "$mode" in
        "unit")
            if [ -z "$test_path" ]; then
                test_path="tests/tests/unit"
            fi
            run_unit_tests "$test_path"
            ;;
        "api")
            if [ -z "$test_path" ]; then
                test_path="tests/tests/api"
            fi
            run_api_tests "$test_path"
            ;;
        "parallel")
            if [ -z "$test_path" ]; then
                test_path="tests/tests"
            fi
            run_parallel_tests "$test_path"
            ;;
        "all")
            run_all_tests
            ;;
        *)
            echo -e "${RED}[ERROR] 未知模式: $mode${NC}"
            echo -e "${YELLOW}可用模式: unit, api, parallel, all${NC}"
            exit 1
            ;;
    esac
    
    # 生成报告摘要
    generate_report_summary
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  测试执行完成${NC}"
    echo -e "${GREEN}========================================${NC}"
}

# 执行主函数
main "$@"