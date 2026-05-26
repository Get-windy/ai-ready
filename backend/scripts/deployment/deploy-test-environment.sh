#!/bin/bash
# AI-Ready 测试环境自动化部署脚本
# 创建日期: 2026-04-24

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/../.."

echo "=== AI-Ready 测试环境自动化部署 ==="
echo "项目根目录: ${PROJECT_ROOT}"
echo "时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查依赖
check_dependencies() {
  echo "🔍 检查依赖..."
  
  if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装${NC}"
    exit 1
  fi
  
  if ! command -v kubectl &> /dev/null; then
    echo -e "${YELLOW}⚠️  kubectl 未安装，跳过 Kubernetes 部署${NC}"
    K8S_AVAILABLE=false
  else
    K8S_AVAILABLE=true
    if ! kubectl cluster-info &> /dev/null; then
      echo -e "${YELLOW}⚠️  Kubernetes 集群不可用，跳过 Kubernetes 部署${NC}"
      K8S_AVAILABLE=false
    fi
  fi
  
  echo -e "${GREEN}✅ 依赖检查通过${NC}"
  echo ""
}

# 部署 Docker 环境
deploy_docker_environment() {
  echo "🐳 部署 Docker 测试环境..."
  
  cd "${PROJECT_ROOT}/docker/test-environment"
  
  # 启动主环境
  echo "启动主测试环境..."
  docker-compose -f docker-compose.yml up -d
  
  # 启动 PostgreSQL 复制
  echo "启动 PostgreSQL 主从复制..."
  docker-compose -f postgresql-replication.yml up -d
  
  echo -e "${GREEN}✅ Docker 环境部署完成${NC}"
  echo ""
}

# 部署 Kubernetes 环境
deploy_kubernetes_environment() {
  if [ "${K8S_AVAILABLE}" = false ]; then
    echo "⏭️  跳过 Kubernetes 部署"
    return 0
  fi
  
  echo "☸️  部署 Kubernetes 测试环境..."
  
  cd "${PROJECT_ROOT}/k8s/test-environment"
  
  # 创建命名空间
  echo "创建命名空间..."
  kubectl apply -f namespace.yaml
  
  # 部署数据库
  echo "部署 PostgreSQL 数据库..."
  kubectl apply -f postgresql-main.yaml
  kubectl apply -f postgresql-main-replicas.yaml
  
  # 部署缓存
  echo "部署 Redis 缓存..."
  kubectl apply -f redis-main.yaml
  
  # 部署应用
  echo "部署 API Gateway..."
  kubectl apply -f api-gateway.yaml
  
  # 等待部署完成
  echo "等待部署完成..."
  sleep 30
  
  echo -e "${GREEN}✅ Kubernetes 环境部署完成${NC}"
  echo ""
}

# 配置监控
configure_monitoring() {
  echo "📊 配置监控系统..."
  
  cd "${PROJECT_ROOT}/monitoring/test-environment"
  
  # 部署 Prometheus 配置
  if [ "${K8S_AVAILABLE}" = true ]; then
    echo "部署 Prometheus 配置到 Kubernetes..."
    kubectl apply -f prometheus-config.yaml
  else
    echo "复制 Prometheus 配置到 Docker 环境..."
    cp prometheus-config.yaml "${PROJECT_ROOT}/backend/infrastructure/docker/prometheus-test/"
  fi
  
  echo -e "${GREEN}✅ 监控系统配置完成${NC}"
  echo ""
}

# 验证部署
verify_deployment() {
  echo "✅ 验证部署结果..."
  
  if [ "${K8S_AVAILABLE}" = true ]; then
    echo "验证 Kubernetes 服务..."
    kubectl get pods -n ai-ready-test
    kubectl get services -n ai-ready-test
  else
    echo "验证 Docker 容器..."
    docker-compose -f "${PROJECT_ROOT}/docker/test-environment/docker-compose.yml" ps
    docker-compose -f "${PROJECT_ROOT}/docker/test-environment/postgresql-replication.yml" ps
  fi
  
  echo -e "${GREEN}✅ 部署验证完成${NC}"
  echo ""
}

# 主函数
main() {
  echo "🚀 开始 AI-Ready 测试环境自动化部署"
  echo "========================================"
  
  check_dependencies
  deploy_docker_environment
  deploy_kubernetes_environment
  configure_monitoring
  verify_deployment
  
  echo "========================================"
  echo -e "${GREEN}🎉 AI-Ready 测试环境部署成功！${NC}"
  echo ""
  echo "访问地址:"
  echo "Docker 环境:"
  echo "  - 前端: http://localhost:3000"
  echo "  - API Gateway: http://localhost:8080"
  echo "  - Prometheus: http://localhost:9090"
  echo "  - Grafana: http://localhost:3000 (admin/admin_test_2026)"
  echo ""
  if [ "${K8S_AVAILABLE}" = true ]; then
    echo "Kubernetes 环境:"
    echo "  - 使用 kubectl port-forward 访问服务"
    echo "  - kubectl port-forward svc/api-gateway 8080:8080 -n ai-ready-test"
  fi
  echo ""
  echo "部署完成时间: $(date)"
}

# 执行主函数
main "$@"