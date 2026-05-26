#!/bin/bash
#
# 价格策略模块性能基准测试执行脚本
# 自动运行JMeter和JMH基准测试
#

set -e

# 配置参数
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JMETER_DIR="$BASE_DIR/jmeter"
JMH_DIR="$BASE_DIR/jmh"
REPORTS_DIR="$BASE_DIR/reports"
SCRIPTS_DIR="$BASE_DIR/scripts"
DATA_DIR="$BASE_DIR/test-data"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        log_error "Java未安装，请先安装Java 11+"
        exit 1
    fi
    
    # 检查JMeter
    if ! command -v jmeter &> /dev/null; then
        log_warning "JMeter未安装，将跳过API负载测试"
        SKIP_JMETER=true
    else
        SKIP_JMETER=false
    fi
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，JMH基准测试需要Maven"
        exit 1
    fi
    
    # 检查Python
    if ! command -v python3 &> /dev/null; then
        log_warning "Python3未安装，将跳过数据生成"
        SKIP_PYTHON=true
    else
        SKIP_PYTHON=false
    fi
    
    log_success "依赖检查完成"
}

# 准备测试环境
prepare_environment() {
    log_info "准备测试环境..."
    
    # 创建目录
    mkdir -p "$REPORTS_DIR"
    mkdir -p "$DATA_DIR"
    
    # 生成测试数据
    if [ "$SKIP_PYTHON" = false ]; then
        log_info "生成测试数据..."
        cd "$SCRIPTS_DIR"
        python3 setup-test-data.py --size 1000 --type strategy --output "$DATA_DIR/strategies.json"
        python3 setup-test-data.py --size 500 --type request --output "$DATA_DIR/requests.json"
        log_success "测试数据生成完成"
    fi
    
    log_success "环境准备完成"
}

# 运行JMeter测试
run_jmeter_tests() {
    if [ "$SKIP_JMETER" = true ]; then
        log_warning "跳过JMeter测试"
        return 0
    fi
    
    log_info "开始JMeter API负载测试..."
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local report_file="$REPORTS_DIR/jmeter_report_$timestamp.html"
    local jmx_file="$JMETER_DIR/api-load-test.jmx"
    
    if [ ! -f "$jmx_file" ]; then
        log_error "JMeter测试文件不存在: $jmx_file"
        return 1
    fi
    
    # 运行JMeter测试
    jmeter -n -t "$jmx_file" -l "$REPORTS_DIR/jmeter_results_$timestamp.jtl" -e -o "$report_file"
    
    if [ $? -eq 0 ]; then
        log_success "JMeter测试完成，报告: $report_file"
        
        # 生成简单的统计信息
        echo "=== JMeter测试统计 ===" > "$REPORTS_DIR/jmeter_summary_$timestamp.txt"
        echo "测试时间: $(date)" >> "$REPORTS_DIR/jmeter_summary_$timestamp.txt"
        echo "报告文件: $report_file" >> "$REPORTS_DIR/jmeter_summary_$timestamp.txt"
        
        # 这里可以添加更多的统计信息提取
        # 实际项目中可以解析JTL文件获取详细统计
        
    else
        log_error "JMeter测试失败"
        return 1
    fi
}

# 运行JMH基准测试
run_jmh_benchmarks() {
    log_info "开始JMH微基准测试..."
    
    cd "$BASE_DIR"
    
    # 检查是否是Maven项目
    if [ ! -f "pom.xml" ]; then
        log_error "当前目录不是Maven项目，请确保在正确的位置运行"
        return 1
    fi
    
    # 编译项目
    log_info "编译项目..."
    mvn clean compile -q
    
    # 运行基准测试
    log_info "运行规则引擎基准测试..."
    mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.RuleEngineBenchmark" -Dexec.classpathScope="compile" -q
    
    log_info "运行优化算法基准测试..."
    mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.OptimizationBenchmark" -Dexec.classpathScope="compile" -q
    
    log_info "运行缓存基准测试..."
    mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.CacheBenchmark" -Dexec.classpathScope="compile" -q
    
    log_success "JMH基准测试完成"
}

# 生成综合报告
generate_report() {
    log_info "生成性能测试综合报告..."
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local report_file="$REPORTS_DIR/performance_report_$timestamp.md"
    
    cat > "$report_file" << EOF
# ERP价格策略模块性能基准测试报告

## 测试概述
- 测试时间: $(date)
- 测试环境: $(uname -a)
- Java版本: $(java -version 2>&1 | head -n 1)

## 测试目标
建立价格策略模块的性能基准，为后续性能优化提供量化依据。

## 测试范围
1. API层性能测试
2. 规则引擎性能测试  
3. 优化算法性能测试
4. 缓存层性能测试

## 测试结果

### 1. API性能测试结果
- 测试工具: JMeter
- 并发用户数: 50 (查询) + 20 (创建)
- 总请求数: 5000 (查询) + 1000 (创建)
- 平均响应时间: [待实际测试填充]
- 吞吐量: [待实际测试填充]
- 错误率: [待实际测试填充]

### 2. 规则引擎性能测试结果
- 测试工具: JMH
- 单规则执行平均时间: [待实际测试填充]
- 多规则并发执行吞吐量: [待实际测试填充]
- 内存使用情况: [待实际测试填充]

### 3. 优化算法性能测试结果  
- 小数据集(100条)优化时间: [待实际测试填充]
- 中数据集(1000条)优化时间: [待实际测试填充]
- 大数据集(10000条)优化时间: [待实际测试填充]
- 并发优化性能: [待实际测试填充]

### 4. 缓存性能测试结果
- 缓存读取延迟: [待实际测试填充]
- 缓存写入延迟: [待实际测试填充]
- 批量读取性能: [待实际测试填充]
- 管道操作性能: [待实际测试填充]
- 并发访问性能: [待实际测试填充]

## 性能基准指标

| 指标类别 | 指标名称 | 基准值 | 单位 | 说明 |
|---------|---------|--------|------|------|
| API | 策略查询响应时间 | < 100 | ms | P95响应时间 |
| API | 策略创建响应时间 | < 200 | ms | P95响应时间 |
| API | 吞吐量 | > 500 | TPS | 每秒处理事务数 |
| 规则引擎 | 单规则执行时间 | < 5 | ms | 平均执行时间 |
| 规则引擎 | 并发规则执行吞吐量 | > 1000 | TPS | 每秒处理规则数 |
| 优化算法 | 1000条数据优化时间 | < 1000 | ms | 优化计算时间 |
| 缓存 | 读取延迟 | < 1 | ms | 平均读取时间 |
| 缓存 | 写入延迟 | < 2 | ms | 平均写入时间 |

## 测试结论与建议
1. [待实际测试填充]
2. [待实际测试填充]
3. [待实际测试填充]

## 后续行动计划
1. 定期运行性能基准测试，监控性能变化
2. 针对发现的性能瓶颈进行优化
3. 建立性能预警机制
4. 持续优化测试框架和工具

---

*本报告由自动化测试脚本生成，测试结果需根据实际运行数据进行填充。*
EOF
    
    log_success "报告生成完成: $report_file"
}

# 主函数
main() {
    log_info "开始执行价格策略模块性能基准测试"
    log_info "======================================"
    
    # 检查依赖
    check_dependencies
    
    # 准备环境
    prepare_environment
    
    # 运行JMeter测试
    run_jmeter_tests
    
    # 运行JMH基准测试
    run_jmh_benchmarks
    
    # 生成报告
    generate_report
    
    log_success "======================================"
    log_success "性能基准测试执行完成!"
    log_success "所有结果保存在: $REPORTS_DIR"
    
    # 显示报告位置
    echo ""
    log_info "重要文件位置:"
    echo "  1. 性能测试计划: $BASE_DIR/../docs/performance/price-strategy-benchmark-plan.md"
    echo "  2. JMeter测试配置: $JMETER_DIR/api-load-test.jmx"
    echo "  3. JMH基准测试代码: $JMH_DIR/"
    echo "  4. 测试数据脚本: $SCRIPTS_DIR/setup-test-data.py"
    echo "  5. 测试报告目录: $REPORTS_DIR/"
}

# 执行主函数
main "$@"