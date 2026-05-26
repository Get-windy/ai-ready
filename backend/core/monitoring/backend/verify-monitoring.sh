#!/bin/bash

echo "AI-Ready 监控告警模块功能验证脚本"
echo "========================================"
echo ""

# 检查监控基础设施
echo "1. 检查监控基础设施状态..."
echo "----------------------------------------"

echo "检查 Prometheus..."
if curl -s http://localhost:9090/api/v1/status/runtimeinfo > /dev/null; then
    echo "  ✅ Prometheus 运行正常"
else
    echo "  ❌ Prometheus 未运行或无法访问"
fi

echo "检查 Grafana..."
if curl -s http://localhost:3000/api/health > /dev/null; then
    echo "  ✅ Grafana 运行正常"
else
    echo "  ❌ Grafana 未运行或无法访问"
fi

echo ""
echo "2. 检查监控告警模块服务状态..."
echo "----------------------------------------"

echo "检查监控告警后端服务..."
if curl -s http://localhost:8090/actuator/health > /dev/null; then
    echo "  ✅ 监控告警后端服务运行正常"
    
    # 测试API接口
    echo "  测试告警历史API..."
    if curl -s http://localhost:8090/api/v1/alerts/history > /dev/null; then
        echo "    ✅ 告警历史API可用"
    else
        echo "    ⚠️  告警历史API可能有问题"
    fi
    
    echo "  测试指标API..."
    if curl -s http://localhost:8090/api/v1/metrics > /dev/null; then
        echo "    ✅ 指标API可用"
    else
        echo "    ⚠️  指标API可能有问题"
    fi
else
    echo "  ❌ 监控告警后端服务未运行或无法访问"
fi

echo ""
echo "3. 检查数据库连接..."
echo "----------------------------------------"

echo "检查PostgreSQL连接..."
if docker exec ai-ready-monitoring-postgres-test pg_isready -U monitoring_test_user -d monitoring_test_db 2>/dev/null; then
    echo "  ✅ PostgreSQL 数据库连接正常"
else
    echo "  ❌ PostgreSQL 数据库连接失败"
fi

echo ""
echo "4. 检查告警规则配置..."
echo "----------------------------------------"

RULES_FILE="I:\AI-Ready\monitoring\backend\config\alert-rules\"
if [ -f "$RULES_FILE" ]; then
    echo "  ✅ 告警规则配置文件存在"
    echo "  规则文件位置: $RULES_FILE"
else
    echo "  ⚠️  告警规则配置文件不存在"
    echo "  请检查: $RULES_FILE"
fi

echo ""
echo "5. 资源使用情况..."
echo "----------------------------------------"

echo "检查容器资源使用..."
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep monitoring

echo ""
echo "6. 生成验证报告..."
echo "----------------------------------------"

REPORT_FILE="monitoring-validation-$(date +%Y%m%d-%H%M%S).txt"
{
    echo "AI-Ready 监控告警模块验证报告"
    echo "生成时间: $(date)"
    echo ""
    echo "=== 验证结果 ==="
    echo ""
    
    # 基础设施状态
    echo "基础设施状态:"
    curl -s http://localhost:9090/api/v1/status/runtimeinfo | grep -A2 '"status"' || echo "Prometheus 状态: 不可用"
    curl -s http://localhost:3000/api/health | grep -A2 '"database"' || echo "Grafana 状态: 不可用"
    
    echo ""
    echo "=== 建议 ==="
    echo "1. 确保所有监控组件正常运行"
    echo "2. 定期检查告警规则配置"
    echo "3. 监控系统资源使用情况"
    echo "4. 定期测试告警通知渠道"
} > "$REPORT_FILE"

echo "验证报告已生成: $REPORT_FILE"
echo ""
echo "========================================"
echo "验证完成!"