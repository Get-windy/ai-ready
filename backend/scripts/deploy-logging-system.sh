#!/bin/bash
# 测试环境日志收集系统部署脚本
# 版本: 1.0.0
# 环境: test
# Sprint: 28+1

set -e

# 颜色定义
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

# 配置变量
NAMESPACE="logging-test"
INFRA_DIR="I:\AI-Ready\infra"
CONFIG_DIRS=(
    "logging/fluentd"
    "logging/storage/elasticsearch"
    "logging/parsing-rules"
    "logging/retention"
)

# 检查Kubectl
check_kubectl() {
    log_info "检查kubectl配置..."
    
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl未安装"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到Kubernetes集群"
        exit 1
    fi
    
    log_success "kubectl配置正常"
}

# 创建命名空间
create_namespace() {
    log_info "创建命名空间: $NAMESPACE..."
    
    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_warning "命名空间已存在: $NAMESPACE"
    else
        kubectl create namespace "$NAMESPACE"
        log_success "命名空间创建成功: $NAMESPACE"
    fi
}

# 部署配置
deploy_configs() {
    log_info "部署日志收集系统配置..."
    
    for config_dir in "${CONFIG_DIRS[@]}"; do
        local full_path="$INFRA_DIR/$config_dir"
        
        if [ -d "$full_path" ]; then
            log_info "处理配置目录: $config_dir"
            
            # 查找所有YAML文件
            for yaml_file in "$full_path"/*.yml "$full_path"/*.yaml; do
                if [ -f "$yaml_file" ]; then
                    log_info "部署配置文件: $(basename "$yaml_file")"
                    
                    # 应用配置到指定命名空间
                    if kubectl apply -f "$yaml_file" -n "$NAMESPACE"; then
                        log_success "配置文件部署成功: $(basename "$yaml_file")"
                    else
                        log_error "配置文件部署失败: $(basename "$yaml_file")"
                        exit 1
                    fi
                fi
            done
        else
            log_warning "配置目录不存在: $full_path"
        fi
    done
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local timeout=300  # 5分钟
    local interval=10
    local elapsed=0
    
    # 等待Elasticsearch
    log_info "等待Elasticsearch服务..."
    while [ $elapsed -lt $timeout ]; do
        if kubectl get pods -n "$NAMESPACE" -l app=elasticsearch 2>/dev/null | grep -q "1/1"; then
            log_success "Elasticsearch服务就绪"
            break
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        
        if [ $elapsed -ge $timeout ]; then
            log_error "Elasticsearch服务启动超时"
            kubectl describe pods -n "$NAMESPACE" -l app=elasticsearch
            exit 1
        fi
    done
    
    # 等待Kibana
    log_info "等待Kibana服务..."
    elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if kubectl get pods -n "$NAMESPACE" -l app=kibana 2>/dev/null | grep -q "1/1"; then
            log_success "Kibana服务就绪"
            break
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        
        if [ $elapsed -ge $timeout ]; then
            log_error "Kibana服务启动超时"
            kubectl describe pods -n "$NAMESPACE" -l app=kibana
            exit 1
        fi
    done
    
    # 等待Fluentd
    log_info "等待Fluentd服务..."
    elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if kubectl get pods -n "$NAMESPACE" -l app=fluentd 2>/dev/null | grep -q "1/1"; then
            log_success "Fluentd服务就绪"
            break
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        
        if [ $elapsed -ge $timeout ]; then
            log_error "Fluentd服务启动超时"
            kubectl describe pods -n "$NAMESPACE" -l app=fluentd
            exit 1
        fi
    done
}

# 验证部署
verify_deployment() {
    log_info "验证部署状态..."
    
    # 检查所有Pod状态
    local all_pods_ready=true
    
    for app in elasticsearch kibana fluentd curator; do
        if kubectl get pods -n "$NAMESPACE" -l app=$app 2>/dev/null | grep -q "1/1"; then
            log_success "$app Pod运行正常"
        else
            log_error "$app Pod运行异常"
            all_pods_ready=false
        fi
    done
    
    if [ "$all_pods_ready" = true ]; then
        log_success "所有Pod运行正常"
    else
        log_error "部分Pod运行异常，请检查日志"
        exit 1
    fi
    
    # 检查服务状态
    for service in elasticsearch kibana fluentd; do
        if kubectl get service -n "$NAMESPACE" $service &> /dev/null; then
            log_success "$service Service配置正常"
        else
            log_error "$service Service配置异常"
            all_pods_ready=false
        fi
    done
}

# 获取访问信息
get_access_info() {
    log_info "获取服务访问信息..."
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}    日志收集系统部署完成               ${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    
    echo -e "${BLUE}服务访问信息:${NC}"
    echo "----------------------------------------"
    
    # 获取服务信息
    local es_service=$(kubectl get service elasticsearch -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}:{.spec.ports[0].port}')
    local kibana_service=$(kubectl get service kibana -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}:{.spec.ports[0].port}')
    local fluentd_service=$(kubectl get service fluentd -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}:{.spec.ports[?(@.name=="http")].port}')
    
    echo -e "${YELLOW}Elasticsearch:${NC}"
    echo "  Cluster内部访问: http://$es_service"
    echo "  API端点: http://elasticsearch.$NAMESPACE.svc.cluster.local:9200"
    echo ""
    
    echo -e "${YELLOW}Kibana:${NC}"
    echo "  Cluster内部访问: http://$kibana_service"
    echo "  管理界面: http://kibana.$NAMESPACE.svc.cluster.local:5601"
    echo ""
    
    echo -e "${YELLOW}Fluentd:${NC}"
    echo "  HTTP日志接收: http://$fluentd_service/app.log"
    echo "  Forward接收: fluentd.$NAMESPACE.svc.cluster.local:24224"
    echo ""
    
    echo -e "${BLUE}使用说明:${NC}"
    echo "----------------------------------------"
    echo "1. 发送测试日志:"
    echo "   curl -X POST http://$fluentd_service/app.log \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"level\":\"INFO\",\"message\":\"测试日志\",\"application\":\"test-app\"}'"
    echo ""
    echo "2. 查询日志:"
    echo "   curl http://$es_service/logs-*/_search"
    echo ""
    echo "3. 访问Kibana:"
    echo "   需要在集群内部或通过端口转发访问"
    echo ""
    
    echo -e "${BLUE}配置验证:${NC}"
    echo "----------------------------------------"
    echo "1. 检查Pod状态:"
    echo "   kubectl get pods -n $NAMESPACE"
    echo ""
    echo "2. 查看日志:"
    echo "   kubectl logs -n $NAMESPACE -l app=elasticsearch"
    echo ""
    echo "3. 测试服务连通性:"
    echo "   kubectl exec -n $NAMESPACE -it \$(kubectl get pods -n $NAMESPACE -l app=elasticsearch -o name) -- curl http://localhost:9200"
    echo ""
}

# 端口转发（可选）
port_forward() {
    log_info "设置端口转发（可选）..."
    
    echo -e "${YELLOW}是否设置端口转发到本地？ (y/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo ""
        echo -e "${BLUE}端口转发命令:${NC}"
        echo "----------------------------------------"
        echo "# Elasticsearch (9200)"
        echo "kubectl port-forward -n $NAMESPACE svc/elasticsearch 9200:9200 &"
        echo ""
        echo "# Kibana (5601)"
        echo "kubectl port-forward -n $NAMESPACE svc/kibana 5601:5601 &"
        echo ""
        echo "# Fluentd HTTP (9880)"
        echo "kubectl port-forward -n $NAMESPACE svc/fluentd 9880:9880 &"
        echo ""
        echo "设置后可通过以下地址访问:"
        echo "- Elasticsearch: http://localhost:9200"
        echo "- Kibana: http://localhost:5601"
        echo "- Fluentd HTTP: http://localhost:9880"
        echo ""
    fi
}

# 清理资源（用于测试）
cleanup() {
    log_info "清理测试资源..."
    
    echo -e "${YELLOW}警告: 这将删除所有日志收集系统资源！${NC}"
    echo -e "${YELLOW}是否继续？ (y/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        for config_dir in "${CONFIG_DIRS[@]}"; do
            local full_path="$INFRA_DIR/$config_dir"
            
            if [ -d "$full_path" ]; then
                for yaml_file in "$full_path"/*.yml "$full_path"/*.yaml; do
                    if [ -f "$yaml_file" ]; then
                        kubectl delete -f "$yaml_file" -n "$NAMESPACE" --ignore-not-found=true
                    fi
                done
            fi
        done
        
        kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
        log_success "资源清理完成"
    else
        log_info "取消清理操作"
    fi
}

# 显示帮助
show_help() {
    echo -e "${GREEN}测试环境日志收集系统部署脚本${NC}"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  deploy      部署日志收集系统（默认）"
    echo "  cleanup     清理所有相关资源"
    echo "  help        显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 deploy      # 部署系统"
    echo "  $0 cleanup     # 清理系统"
    echo "  $0 help        # 显示帮助"
    echo ""
}

# 主函数
main() {
    local action=${1:-deploy}
    
    case "$action" in
        deploy)
            echo -e "${GREEN}========================================${NC}"
            echo -e "${GREEN}  测试环境日志收集系统部署              ${NC}"
            echo -e "${GREEN}  环境: test | Sprint: 28+1            ${NC}"
            echo -e "${GREEN}========================================${NC}"
            echo ""
            
            check_kubectl
            create_namespace
            deploy_configs
            wait_for_services
            verify_deployment
            get_access_info
            port_forward
            
            log_success "部署完成！"
            ;;
        
        cleanup)
            cleanup
            ;;
        
        help|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@"