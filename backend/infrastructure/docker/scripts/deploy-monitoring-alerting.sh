#!/bin/bash
# AI-Ready 测试环境监控告警系统部署脚本
# Version: v2.0.0
# 创建日期: 2026-04-28
# 用途: 部署完整的监控告警系统，包括Prometheus、Grafana、AlertManager、ELK等

set -e

# ==================== 配置参数 ====================
PROJECT_NAME="ai-ready"
ENVIRONMENT="test"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="${BASE_DIR}/../docker-compose-test.yml"
LOG_DIR="${BASE_DIR}/../logs"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==================== 辅助函数 ====================
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装"
        exit 1
    fi
}

# ==================== 预检查 ====================
step_pre_check() {
    log_step "执行预检查..."

    # 检查必要命令
    check_command docker
    check_command docker-compose

    # 检查Docker是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker未运行，请先启动Docker"
        exit 1
    fi

    # 检查配置文件是否存在
    if [ ! -f "${DOCKER_DIR}" ]; then
        log_error "Docker Compose配置文件不存在: ${DOCKER_DIR}"
        exit 1
    fi

    # 检查监控配置目录
    if [ ! -d "${BASE_DIR}/../prometheus-test" ]; then
        log_error "Prometheus配置目录不存在"
        exit 1
    fi

    if [ ! -d "${BASE_DIR}/../grafana-test" ]; then
        log_error "Grafana配置目录不存在"
        exit 1
    fi

    if [ ! -d "${BASE_DIR}/../alertmanager-test" ]; then
        log_error "AlertManager配置目录不存在"
        exit 1
    fi

    log_info "预检查通过"
}

# ==================== 创建必要目录 ====================
step_create_directories() {
    log_step "创建必要目录..."

    # 创建日志目录
    mkdir -p "${LOG_DIR}"

    # 创建Grafana provisioning目录
    mkdir -p "${BASE_DIR}/../grafana-test/provisioning/datasources"
    mkdir -p "${BASE_DIR}/../grafana-test/provisioning/dashboards"
    mkdir -p "${BASE_DIR}/../grafana-test/dashboards/ai-ready"
    mkdir -p "${BASE_DIR}/../grafana-test/dashboards/system"

    # 创建ELK配置目录
    mkdir -p "${BASE_DIR}/../elasticsearch"
    mkdir -p "${BASE_DIR}/../logstash/patterns"
    mkdir -p "${BASE_DIR}/../kibana"
    mkdir -p "${BASE_DIR}/../filebeat"

    log_info "目录创建完成"
}

# ==================== 配置企业微信通知 ====================
step_configure_wecom() {
    log_step "配置企业微信通知..."

    # 提示用户输入企业微信Webhook URL
    if [ -z "${WECOM_WEBHOOK_URL}" ]; then
        log_warn "未配置企业微信Webhook URL"
        log_warn "请设置环境变量 WECOM_WEBHOOK_URL 或在 alertmanager.yml 中手动配置"
        log_warn "例如: export WECOM_WEBHOOK_URL='https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'"
    else
        log_info "企业微信Webhook URL已配置"
        # 更新AlertManager配置中的Webhook URL
        sed -i "s|\${WECOM_WEBHOOK_URL}|${WECOM_WEBHOOK_URL}|g" "${BASE_DIR}/../alertmanager-test/alertmanager.yml"
    fi
}

# ==================== 启动监控组件 ====================
step_start_monitoring() {
    log_step "启动监控组件..."

    cd "${BASE_DIR}/.."

    # 启动Prometheus
    log_info "启动Prometheus..."
    docker-compose -f docker-compose-test.yml up -d prometheus
    sleep 10

    # 检查Prometheus健康状态
    if docker-compose -f docker-compose-test.yml ps prometheus | grep -q "Up"; then
        log_info "Prometheus启动成功"
    else
        log_error "Prometheus启动失败"
        docker-compose -f docker-compose-test.yml logs prometheus
        exit 1
    fi

    # 启动Node Exporter
    log_info "启动Node Exporter..."
    docker-compose -f docker-compose-test.yml up -d node-exporter
    sleep 5

    # 启动cAdvisor
    log_info "启动cAdvisor..."
    docker-compose -f docker-compose-test.yml up -d cadvisor
    sleep 5

    # 启动数据库Exporter
    log_info "启动PostgreSQL/Redis/Kafka Exporter..."
    docker-compose -f docker-compose-test.yml up -d postgres-exporter redis-exporter kafka-exporter
    sleep 10

    # 启动Grafana
    log_info "启动Grafana..."
    docker-compose -f docker-compose-test.yml up -d grafana
    sleep 15

    # 检查Grafana健康状态
    if docker-compose -f docker-compose-test.yml ps grafana | grep -q "Up"; then
        log_info "Grafana启动成功"
    else
        log_error "Grafana启动失败"
        docker-compose -f docker-compose-test.yml logs grafana
        exit 1
    fi

    # 启动AlertManager
    log_info "启动AlertManager..."
    docker-compose -f docker-compose-test.yml up -d alertmanager
    sleep 10

    log_info "监控组件启动完成"
}

# ==================== 启动日志收集系统 ====================
step_start_logging() {
    log_step "启动日志收集系统..."

    cd "${BASE_DIR}/.."

    # 启动Elasticsearch
    log_info "启动Elasticsearch..."
    docker-compose -f docker-compose-test.yml up -d elasticsearch
    sleep 30

    # 检查Elasticsearch健康状态
    for i in {1..10}; do
        if curl -f http://localhost:9200/_cluster/health &> /dev/null; then
            log_info "Elasticsearch启动成功"
            break
        fi
        if [ $i -eq 10 ]; then
            log_error "Elasticsearch启动失败"
            docker-compose -f docker-compose-test.yml logs elasticsearch
            exit 1
        fi
        sleep 10
    done

    # 启动Logstash
    log_info "启动Logstash..."
    docker-compose -f docker-compose-test.yml up -d logstash
    sleep 15

    # 启动Kibana
    log_info "启动Kibana..."
    docker-compose -f docker-compose-test.yml up -d kibana
    sleep 20

    # 检查Kibana健康状态
    for i in {1..10}; do
        if curl -f http://localhost:5601/api/status &> /dev/null; then
            log_info "Kibana启动成功"
            break
        fi
        if [ $i -eq 10 ]; then
            log_error "Kibana启动失败"
            docker-compose -f docker-compose-test.yml logs kibana
            exit 1
        fi
        sleep 10
    done

    # 启动Filebeat
    log_info "启动Filebeat..."
    docker-compose -f docker-compose-test.yml up -d filebeat
    sleep 10

    log_info "日志收集系统启动完成"
}

# ==================== 配置Grafana ====================
step_configure_grafana() {
    log_step "配置Grafana..."

    # 等待Grafana完全启动
    sleep 30

    # 检查数据源是否自动配置
    log_info "检查Grafana数据源配置..."
    if curl -f http://localhost:3000/api/datasources &> /dev/null; then
        log_info "Grafana数据源配置正常"
    else
        log_warn "Grafana数据源可能未自动配置，请手动检查"
    fi

    # 检查Dashboard是否自动导入
    log_info "检查Grafana Dashboard配置..."
    if curl -f http://localhost:3000/api/search?type=dashboard &> /dev/null; then
        log_info "Grafana Dashboard配置正常"
    else
        log_warn "Grafana Dashboard可能未自动导入，请手动检查"
    fi

    log_info "Grafana配置完成"
}

# ==================== 配置告警规则 ====================
step_configure_alerts() {
    log_step "配置告警规则..."

    # 检查Prometheus告警规则是否加载
    log_info "检查Prometheus告警规则..."
    if curl -f http://localhost:9090/api/v1/rules &> /dev/null; then
        log_info "Prometheus告警规则已加载"
        
        # 显示告警规则数量
        RULES_COUNT=$(curl -s http://localhost:9090/api/v1/rules | jq '.data.groups[].rules | length' | awk '{sum+=$1} END {print sum}')
        log_info "已加载 ${RULES_COUNT} 条告警规则"
    else
        log_error "Prometheus告警规则未加载"
        exit 1
    fi

    # 检查AlertManager配置
    log_info "检查AlertManager配置..."
    if curl -f http://localhost:9093/api/v2/status &> /dev/null; then
        log_info "AlertManager配置正常"
    else
        log_error "AlertManager配置异常"
        exit 1
    fi

    log_info "告警规则配置完成"
}

# ==================== 验证系统健康 ====================
step_health_check() {
    log_step "验证系统健康..."

    # 检查所有监控组件状态
    COMPONENTS=(
        "prometheus:9090/-/healthy"
        "grafana:3000/api/health"
        "alertmanager:9093/-/healthy"
        "node-exporter:9100/metrics"
        "cadvisor:8080/healthz"
        "elasticsearch:9200/_cluster/health"
        "kibana:5601/api/status"
    )

    FAILED=0
    for component in "${COMPONENTS[@]}"; do
        name=$(echo $component | cut -d':' -f1)
        url=$(echo $component | cut -d':' -f2-)
        
        if curl -f http://localhost:${url} &> /dev/null; then
            log_info "${name} 健康检查通过"
        else
            log_error "${name} 健康检查失败"
            FAILED=1
        fi
    done

    if [ $FAILED -eq 1 ]; then
        log_error "部分组件健康检查失败，请查看日志"
        return 1
    fi

    log_info "所有组件健康检查通过"
}

# ==================== 显示访问信息 ====================
step_show_info() {
    log_step "显示访问信息..."

    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  AI-Ready 测试环境监控告警系统部署完成${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${BLUE}监控服务访问地址:${NC}"
    echo "  Prometheus:       http://localhost:9090"
    echo "  Grafana:          http://localhost:3000 (admin/admin_test_2026)"
    echo "  AlertManager:     http://localhost:9093"
    echo ""
    echo -e "${BLUE}日志服务访问地址:${NC}"
    echo "  Elasticsearch:    http://localhost:9200"
    echo "  Kibana:           http://localhost:5601"
    echo "  Logstash:         http://localhost:9600"
    echo ""
    echo -e "${BLUE}数据采集服务:${NC}"
    echo "  Node Exporter:    http://localhost:9100"
    echo "  cAdvisor:         http://localhost:8085"
    echo "  PostgreSQL Exporter: http://localhost:9187"
    echo "  Redis Exporter:   http://localhost:9121"
    echo "  Kafka Exporter:   http://localhost:9308"
    echo ""
    echo -e "${BLUE}告警通知:${NC}"
    if [ -n "${WECOM_WEBHOOK_URL}" ]; then
        echo "  企业微信通知:     已配置"
    else
        echo -e "${YELLOW}  企业微信通知:     未配置 (请设置WECOM_WEBHOOK_URL环境变量)${NC}"
    fi
    echo "  邮件通知:         admin@ai-ready.com"
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo ""
}

# ==================== 主流程 ====================
main() {
    log_info "开始部署AI-Ready测试环境监控告警系统..."
    echo ""

    # 执行各步骤
    step_pre_check
    step_create_directories
    step_configure_wecom
    step_start_monitoring
    step_start_logging
    step_configure_grafana
    step_configure_alerts
    step_health_check
    step_show_info

    log_info "部署完成"
    echo ""
}

# ==================== 执行入口 ====================
# 解析参数
while [ $# -gt 0 ]; do
    case "$1" in
        --skip-logging)
            SKIP_LOGGING=true
            shift
            ;;
        --skip-grafana-config)
            SKIP_GRAFANA_CONFIG=true
            shift
            ;;
        --help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  --skip-logging         跳过日志系统部署"
            echo "  --skip-grafana-config  跳过Grafana配置"
            echo "  --help                 显示帮助信息"
            exit 0
            ;;
        *)
            log_error "未知参数: $1"
            exit 1
            ;;
    esac
done

# 执行主流程
main

exit 0