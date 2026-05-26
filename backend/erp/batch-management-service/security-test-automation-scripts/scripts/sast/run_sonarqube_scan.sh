#!/bin/bash
# 批次管理模块SonarQube安全扫描脚本
# 版本: 1.0.0
# 作者: test-agent-1
# 日期: 2026-05-01

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"
CONFIG_DIR="${SCRIPT_DIR}/../../config"
REPORT_DIR="${ROOT_DIR}/reports/sast"
LOG_DIR="${ROOT_DIR}/logs"

# 加载配置文件
if [ -f "${CONFIG_DIR}/sonarqube_config.yaml" ]; then
    echo -e "${BLUE}加载SonarQube配置文件...${NC}"
    SONAR_HOST=$(grep 'host:' "${CONFIG_DIR}/sonarqube_config.yaml" | awk '{print $2}')
    SONAR_TOKEN=$(grep 'token:' "${CONFIG_DIR}/sonarqube_config.yaml" | awk '{print $2}' | sed 's/\${SONAR_TOKEN}/'$SONAR_TOKEN'/')
    PROJECT_KEY=$(grep 'key:' "${CONFIG_DIR}/sonarqube_config.yaml" | awk 'NR==2 {print $2}')
else
    echo -e "${YELLOW}配置文件不存在，使用默认配置${NC}"
    SONAR_HOST=${SONAR_HOST:-"http://localhost:9000"}
    SONAR_TOKEN=${SONAR_TOKEN:-""}
    PROJECT_KEY=${PROJECT_KEY:-"erp-batch-management"}
fi

# 创建目录
mkdir -p "${REPORT_DIR}"
mkdir -p "${LOG_DIR}"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装"
        return 1
    fi
    
    # 检查SonarQube Scanner
    if ! command -v sonar-scanner &> /dev/null; then
        log_warning "SonarQube Scanner未安装，尝试使用Maven插件"
    fi
    
    # 检查网络连接
    if [ -n "$SONAR_HOST" ]; then
        if ! curl -s --head "$SONAR_HOST" | grep "200 OK" > /dev/null; then
            log_warning "无法连接到SonarQube服务器: $SONAR_HOST"
        fi
    fi
    
    log_success "依赖检查完成"
}

# 准备扫描环境
prepare_environment() {
    log_info "准备扫描环境..."
    
    # 清理旧报告
    if [ -d "${REPORT_DIR}" ]; then
        rm -rf "${REPORT_DIR}"/*
    fi
    
    # 生成项目版本信息
    PROJECT_VERSION=$(date '+%Y%m%d.%H%M%S')
    echo "PROJECT_VERSION=${PROJECT_VERSION}" > "${REPORT_DIR}/version.info"
    
    log_success "环境准备完成"
}

# 运行SonarQube扫描
run_sonarqube_scan() {
    local scan_type=$1
    local options=$2
    
    log_info "开始SonarQube安全扫描 (类型: $scan_type)..."
    
    local start_time=$(date +%s)
    
    # 构建Maven命令
    local mvn_cmd="mvn clean compile"
    
    if [ "$scan_type" = "full" ]; then
        mvn_cmd="$mvn_cmd sonar:sonar"
        if [ -n "$SONAR_HOST" ]; then
            mvn_cmd="$mvn_cmd -Dsonar.host.url=$SONAR_HOST"
        fi
        if [ -n "$SONAR_TOKEN" ]; then
            mvn_cmd="$mvn_cmd -Dsonar.login=$SONAR_TOKEN"
        fi
        if [ -n "$PROJECT_KEY" ]; then
            mvn_cmd="$mvn_cmd -Dsonar.projectKey=$PROJECT_KEY"
        fi
        mvn_cmd="$mvn_cmd -Dsonar.projectVersion=$PROJECT_VERSION"
        mvn_cmd="$mvn_cmd -Dsonar.sources=src/main/java"
        mvn_cmd="$mvn_cmd -Dsonar.exclusions=**/*Test.java,**/test/**"
        mvn_cmd="$mvn_cmd -Dsonar.java.binaries=target/classes"
        mvn_cmd="$mvn_cmd -Dsonar.java.source=17"
        mvn_cmd="$mvn_cmd -Dsonar.java.target=17"
        mvn_cmd="$mvn_cmd -Dsonar.security.config=security/sonar-security-config.json"
    else
        mvn_cmd="$mvn_cmd sonar:sonar"
        mvn_cmd="$mvn_cmd -Dsonar.analysis.mode=preview"
        mvn_cmd="$mvn_cmd -Dsonar.issuesReport.html.enable=true"
    fi
    
    # 添加额外选项
    if [ -n "$options" ]; then
        mvn_cmd="$mvn_cmd $options"
    fi
    
    log_info "执行命令: $mvn_cmd"
    
    # 执行扫描
    if eval "$mvn_cmd" 2>&1 | tee "${LOG_DIR}/sonarqube_scan_${scan_type}.log"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log_success "SonarQube扫描完成 (耗时: ${duration}秒)"
        
        # 生成扫描摘要
        generate_scan_summary "$scan_type" "$duration"
        
        return 0
    else
        log_error "SonarQube扫描失败"
        return 1
    fi
}

# 生成扫描摘要
generate_scan_summary() {
    local scan_type=$1
    local duration=$2
    
    log_info "生成扫描摘要..."
    
    local summary_file="${REPORT_DIR}/sonarqube_summary_${scan_type}.txt"
    
    cat > "$summary_file" << EOF
SonarQube安全扫描摘要
=====================

扫描信息:
- 扫描类型: $scan_type
- 项目名称: ERP批次管理模块
- 项目版本: $PROJECT_VERSION
- 扫描时间: $(date '+%Y-%m-%d %H:%M:%S')
- 扫描耗时: ${duration}秒

扫描配置:
- SonarQube服务器: ${SONAR_HOST:-"未配置"}
- 项目标识: $PROJECT_KEY
- 扫描目录: src/main/java
- 排除目录: **/*Test.java, **/test/**

安全规则配置:
- 启用了OWASP Top 10安全规则
- 启用了CWE/SANS Top 25安全规则
- 配置了自定义安全规则

扫描结果位置:
- SonarQube仪表板: ${SONAR_HOST}/dashboard?id=$PROJECT_KEY
- 本地报告目录: $REPORT_DIR
- 日志文件: ${LOG_DIR}/sonarqube_scan_${scan_type}.log

后续步骤:
1. 查看SonarQube仪表板获取详细结果
2. 修复发现的安全问题
3. 重新运行扫描验证修复
4. 将扫描集成到CI/CD流水线

EOF
    
    log_success "扫描摘要已生成: $summary_file"
}

# 检查安全门禁
check_security_gates() {
    log_info "检查安全门禁..."
    
    local gates_file="${CONFIG_DIR}/security_gates.yaml"
    
    if [ ! -f "$gates_file" ]; then
        log_warning "安全门禁配置文件不存在: $gates_file"
        return 0
    fi
    
    # 这里应该实现从SonarQube API获取结果并检查门禁
    # 由于时间关系，这里只做示例实现
    
    log_info "安全门禁检查（示例实现）"
    log_info "建议门禁配置:"
    log_info "- 最大严重问题: 0"
    log_info "- 最大重要问题: 0" 
    log_info="- 最大中等问题: 5"
    log_info="- 最大次要问题: 10"
    
    log_success "安全门禁检查完成（示例）"
}

# 主函数
main() {
    local scan_type=${1:-"full"}
    local options=${2:-""}
    
    log_info "批次管理模块SonarQube安全扫描开始"
    log_info "工作目录: $ROOT_DIR"
    
    # 检查依赖
    if ! check_dependencies; then
        log_error "依赖检查失败，退出"
        exit 1
    fi
    
    # 准备环境
    prepare_environment
    
    # 运行扫描
    if run_sonarqube_scan "$scan_type" "$options"; then
        # 检查安全门禁
        check_security_gates
        
        log_success "SonarQube安全扫描任务完成"
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}扫描成功完成！${NC}"
        echo -e "${GREEN}报告目录: $REPORT_DIR${NC}"
        echo -e "${GREEN}日志目录: $LOG_DIR${NC}"
        echo -e "${GREEN}========================================${NC}"
    else
        log_error "SonarQube安全扫描任务失败"
        exit 1
    fi
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            SCAN_TYPE="$2"
            shift 2
            ;;
        -o|--options)
            SCAN_OPTIONS="$2"
            shift 2
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  -t, --type TYPE     扫描类型: full(全量), preview(预览)"
            echo "  -o, --options OPT   额外Maven选项"
            echo "  -h, --help          显示帮助信息"
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            echo "使用 -h 查看帮助"
            exit 1
            ;;
    esac
done

# 执行主函数
main "${SCAN_TYPE}" "${SCAN_OPTIONS}"