#!/bin/bash
# 验证ERP模块部署配置

set -e

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$DEPLOY_DIR")/backend/erp"

echo "验证ERP模块部署配置..."

# 检查模块目录
modules=("erp-batch-sn" "erp-invoice" "erp-purchase-exchange" "erp-sales-exchange" "erp-supplier-portal")
docker_modules=("erp-batch-sn" "erp-invoice" "erp-purchase-exchange" "erp-sales-exchange" "supplier-portal")

for i in "${!modules[@]}"; do
    module="${modules[$i]}"
    docker_module="${docker_modules[$i]}"
    
    echo "检查模块: $module"
    
    # 检查后端代码目录
    if [ -d "$BACKEND_DIR/$module" ]; then
        echo "  ✓ 后端代码目录存在: $BACKEND_DIR/$module"
    else
        echo "  ✗ 后端代码目录不存在: $BACKEND_DIR/$module"
        exit 1
    fi
    
    # 检查Dockerfile
    if [ -f "$DEPLOY_DIR/$docker_module/Dockerfile" ]; then
        echo "  ✓ Dockerfile存在: $DEPLOY_DIR/$docker_module/Dockerfile"
    else
        echo "  ✗ Dockerfile不存在: $DEPLOY_DIR/$docker_module/Dockerfile"
        exit 1
    fi
    
    # 检查pom.xml
    if [ -f "$BACKEND_DIR/$module/pom.xml" ]; then
        echo "  ✓ pom.xml存在: $BACKEND_DIR/$module/pom.xml"
    else
        echo "  ✗ pom.xml不存在: $BACKEND_DIR/$module/pom.xml"
        exit 1
    fi
done

# 检查docker-compose.yml
if [ -f "$DEPLOY_DIR/docker-compose.yml" ]; then
    echo "✓ docker-compose.yml存在"
else
    echo "✗ docker-compose.yml不存在"
    exit 1
fi

# 检查环境配置文件
if [ -f "$DEPLOY_DIR/.env.production" ]; then
    echo "✓ .env.production存在"
else
    echo "✗ .env.production不存在"
    exit 1
fi

# 检查部署脚本
if [ -f "$DEPLOY_DIR/deploy-production.sh" ]; then
    echo "✓ deploy-production.sh存在"
else
    echo "✗ deploy-production.sh不存在"
    exit 1
fi

if [ -f "$DEPLOY_DIR/deploy-production.ps1" ]; then
    echo "✓ deploy-production.ps1存在"
else
    echo "✗ deploy-production.ps1不存在"
    exit 1
fi

# 检查文档
if [ -f "$DEPLOY_DIR/deployment-guide.md" ]; then
    echo "✓ deployment-guide.md存在"
else
    echo "✗ deployment-guide.md不存在"
    exit 1
fi

if [ -f "$DEPLOY_DIR/operations-guide.md" ]; then
    echo "✓ operations-guide.md存在"
else
    echo "✗ operations-guide.md不存在"
    exit 1
fi

# 检查监控配置
if [ -f "$DEPLOY_DIR/monitoring/prometheus.yml" ]; then
    echo "✓ Prometheus配置存在"
else
    echo "✗ Prometheus配置不存在"
    exit 1
fi

if [ -f "$DEPLOY_DIR/monitoring/alerts/erp-modules-alerts.yml" ]; then
    echo "✓ 系统告警规则存在"
else
    echo "✗ 系统告警规则不存在"
    exit 1
fi

if [ -f "$DEPLOY_DIR/monitoring/alerts/business-metrics-alerts.yml" ]; then
    echo "✓ 业务告警规则存在"
else
    echo "✗ 业务告警规则不存在"
    exit 1
fi

echo ""
echo "✅ 所有部署配置验证通过！"
echo "部署环境已准备就绪。"