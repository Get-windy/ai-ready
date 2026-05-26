# 监控告警模块部署指南

## 概述

本文档提供AI-Ready项目监控告警模块的完整部署指南。监控告警模块包括Prometheus、Grafana、AlertManager、自定义导出器、监控API服务等组件。

## 系统要求

### 硬件要求
- **CPU**: 4核或以上
- **内存**: 8GB或以上
- **磁盘**: 50GB可用空间（建议SSD）
- **网络**: 稳定的网络连接

### 软件要求
- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 8+), macOS 10.15+, Windows 10/11 (WSL2)
- **Docker**: 20.10.0+
- **Docker Compose**: 2.0.0+
- **Git**: 2.30.0+

### 端口要求
| 端口 | 服务 | 描述 | 必需 |
|------|------|------|------|
| 5433 | PostgreSQL | 监控数据库 | 是 |
| 6380 | Redis | 监控缓存 | 是 |
| 5673 | RabbitMQ | AMQP消息队列 | 是 |
| 15673 | RabbitMQ | 管理界面 | 否 |
| 9090 | Prometheus | 监控服务 | 是 |
| 9093 | AlertManager | 告警服务 | 是 |
| 3000 | Grafana | 可视化界面 | 是 |
| 9101 | 自定义导出器 | 业务指标 | 是 |
| 8081 | 监控API | REST API | 是 |
| 80 | Nginx | Web门户 | 是 |
| 443 | Nginx | HTTPS | 否 |

## 快速开始

### 1. 环境准备

```bash
# 克隆项目（如果尚未克隆）
git clone <repository-url>
cd AI-Ready/deploy

# 检查Docker环境
docker --version
docker-compose --version

# 检查端口占用
./deploy-monitoring.sh check-ports
```

### 2. 一键部署

```bash
# 执行部署脚本
./deploy-monitoring.sh

# 或者分步执行
./deploy-monitoring.sh create-directories
./deploy-monitoring.sh create-configs
./deploy-monitoring.sh start-services
```

### 3. 启动服务

```bash
# 启动所有服务
./start-monitoring.sh

# 指定环境启动
./start-monitoring.sh development
./start-monitoring.sh staging
./start-monitoring.sh production
```

### 4. 验证部署

```bash
# 健康检查
./health-check.sh

# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps

# 查看日志
docker-compose -f monitoring-alerting-deployment.yml logs -f
```

## 详细部署步骤

### 步骤1: 环境检查

```bash
# 检查系统资源
free -h
df -h
lscpu

# 检查Docker
docker info
docker-compose version

# 检查端口占用
netstat -tulpn | grep -E ':5433|:6380|:5673|:9090|:3000|:8081'
```

### 步骤2: 配置准备

```bash
# 创建配置目录
mkdir -p {prometheus,rules,alertmanager,grafana,nginx,logs}

# 生成配置文件
./deploy-monitoring.sh create-configs

# 检查配置文件
ls -la prometheus/ alertmanager/ grafana/ nginx/
```

### 步骤3: 启动服务

```bash
# 启动基础服务
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring rabbitmq-monitoring

# 等待数据库就绪
sleep 30

# 启动监控服务
docker-compose -f monitoring-alerting-deployment.yml up -d prometheus alertmanager grafana

# 启动应用服务
docker-compose -f monitoring-alerting-deployment.yml up -d custom-exporter monitoring-api nginx
```

### 步骤4: 初始化配置

```bash
# 导入Grafana仪表盘
curl -X POST http://localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <api-key>" \
  -d @grafana/dashboards/system-monitoring.json

# 配置告警规则
docker-compose -f monitoring-alerting-deployment.yml exec prometheus promtool check rules /etc/prometheus/rules/*.yml

# 重新加载Prometheus配置
curl -X POST http://localhost:9090/-/reload
```

### 步骤5: 验证功能

```bash
# 测试Prometheus查询
curl "http://localhost:9090/api/v1/query?query=up"

# 测试Grafana API
curl -u admin:admin123 http://localhost:3000/api/health

# 测试监控API
curl http://localhost:8081/actuator/health

# 测试告警
curl "http://localhost:9093/api/v2/alerts"
```

## 环境配置

### 开发环境

```bash
# 使用默认配置
export NODE_ENV=development
export COMPOSE_PROJECT_NAME=ai-ready-monitoring-dev

# 启动开发环境
./start-monitoring.sh development
```

### 测试环境

```bash
# 测试环境配置
export NODE_ENV=staging
export COMPOSE_PROJECT_NAME=ai-ready-monitoring-staging
export PROMETHEUS_RETENTION=15d

# 启动测试环境
./start-monitoring.sh staging
```

### 生产环境

```bash
# 生产环境配置
export NODE_ENV=production
export COMPOSE_PROJECT_NAME=ai-ready-monitoring-prod
export PROMETHEUS_RETENTION=30d
export GRAFANA_ADMIN_PASSWORD=$(openssl rand -base64 32)

# 启动生产环境
./start-monitoring.sh production
```

## 服务配置详解

### Prometheus配置

```yaml
# 存储配置
storage:
  tsdb:
    retention: 30d
    retention.size: 10GB

# 抓取配置
scrape_configs:
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 30s
```

### AlertManager配置

```yaml
# 路由配置
route:
  receiver: 'default'
  group_wait: 30s
  group_interval: 5m

# 接收器配置
receivers:
  - name: 'default'
    email_configs:
      - to: 'admin@example.com'
```

### Grafana配置

```ini
# 数据源配置
[datasources]
[datasources.prometheus]
url = http://prometheus:9090

# 安全配置
[security]
admin_password = admin123
```

## 监控目标集成

### 1. 系统监控

```yaml
# Node Exporter
scrape_configs:
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```

### 2. 应用监控

```yaml
# Spring Boot应用
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app-service:8080']
```

### 3. 数据库监控

```yaml
# PostgreSQL
scrape_configs:
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
```

### 4. 中间件监控

```yaml
# Redis
scrape_configs:
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

## 告警规则配置

### 系统告警

```yaml
groups:
  - name: system.alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance)(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
```

### 应用告警

```yaml
groups:
  - name: application.alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
```

### 业务告警

```yaml
groups:
  - name: business.alerts
    rules:
      - alert: OrderCreationFailure
        expr: rate(order_creation_failed_total[5m]) / rate(order_creation_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
```

## 维护操作

### 日常维护

```bash
# 查看服务状态
./health-check.sh

# 备份数据
./backup-monitoring.sh

# 清理旧数据
docker exec prometheus prometheus tsdb clean --max-block-duration=30d
```

### 故障排查

```bash
# 查看日志
docker-compose logs -f [service-name]

# 进入容器调试
docker exec -it [container-name] /bin/bash

# 检查网络
docker network inspect monitoring-network

# 检查存储
docker volume ls
```

### 升级操作

```bash
# 备份当前配置
cp -r prometheus prometheus-backup-$(date +%Y%m%d)

# 停止服务
./stop-monitoring.sh

# 更新镜像
docker-compose pull

# 启动服务
./start-monitoring.sh
```

## 性能调优

### Prometheus调优

```yaml
# 增加存储保留时间
storage:
  tsdb:
    retention: 60d

# 优化抓取配置
scrape_configs:
  - job_name: 'high-frequency'
    scrape_interval: 15s
    scrape_timeout: 10s
```

### 资源限制

```yaml
# Docker Compose资源限制
services:
  prometheus:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

## 安全配置

### 网络隔离

```yaml
# 创建专用网络
networks:
  monitoring:
    driver: bridge
    internal: true
```

### 认证授权

```bash
# 启用基本认证
htpasswd -c nginx/.htpasswd admin

# 配置TLS证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/private.key -out nginx/ssl/certificate.crt
```

### 访问控制

```nginx
# Nginx访问控制
location /admin {
    allow 192.168.1.0/24;
    deny all;
    auth_basic "Restricted";
    auth_basic_user_file /etc/nginx/.htpasswd;
}
```

## 备份与恢复

### 数据备份

```bash
#!/bin/bash
# 备份脚本
BACKUP_DIR="/backup/monitoring-$(date +%Y%m%d)"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份Prometheus数据
docker exec prometheus tar czf /prometheus.tar.gz /prometheus
docker cp prometheus:/prometheus.tar.gz $BACKUP_DIR/

# 备份PostgreSQL数据
docker exec postgres-monitoring pg_dump -U monitoring_user monitoring_db > $BACKUP_DIR/database.sql

# 备份配置文件
cp -r prometheus alertmanager grafana nginx $BACKUP_DIR/config/

# 创建备份清单
tar czf monitoring-backup-$(date +%Y%m%d).tar.gz $BACKUP_DIR
```

### 数据恢复

```bash
#!/bin/bash
# 恢复脚本
BACKUP_FILE="monitoring-backup-20240427.tar.gz"
RESTORE_DIR="/tmp/restore"

# 解压备份
tar xzf $BACKUP_FILE -C $RESTORE_DIR

# 恢复Prometheus数据
docker cp $RESTORE_DIR/prometheus.tar.gz prometheus:/
docker exec prometheus tar xzf /prometheus.tar.gz -C /

# 恢复数据库
docker exec -i postgres-monitoring psql -U monitoring_user monitoring_db < $RESTORE_DIR/database.sql

# 恢复配置
cp -r $RESTORE_DIR/config/* ./
```

## 监控仪表盘

### 系统仪表盘
- **系统概览**: CPU、内存、磁盘、网络使用情况
- **服务健康**: 所有服务的健康状态
- **性能指标**: 响应时间、吞吐量、错误率

### 业务仪表盘
- **订单监控**: 订单创建、支付、发货状态
- **用户行为**: 活跃用户、转化率、留存率
- **业务指标**: 营收、毛利、客户满意度

### 基础设施仪表盘
- **数据库监控**: 连接数、查询性能、锁等待
- **缓存监控**: 命中率、内存使用、网络流量
- **消息队列**: 队列长度、消费速率、积压情况

## 故障排除

### 常见问题

#### 1. 服务启动失败
```bash
# 检查日志
docker-compose logs [service-name]

# 检查端口占用
lsof -i :[port]

# 检查资源
docker stats
```

#### 2. 监控数据缺失
```bash
# 检查目标状态
curl http://localhost:9090/api/v1/targets

# 检查抓取配置
docker exec prometheus cat /etc/prometheus/prometheus.yml

# 检查网络连接
docker exec prometheus ping [target-host]
```

#### 3. 告警不工作
```bash
# 检查告警规则
docker exec prometheus promtool check rules /etc/prometheus/rules/*.yml

# 检查AlertManager配置
docker exec alertmanager amtool check-config /etc/alertmanager/alertmanager.yml

# 测试告警
curl -X POST http://localhost:9093/api/v2/alerts -d '[{"labels":{"alertname":"TestAlert"}}]'
```

#### 4. Grafana无法访问
```bash
# 检查服务状态
docker-compose ps grafana

# 检查日志
docker-compose logs grafana

# 检查数据库连接
docker exec grafana sqlite3 /var/lib/grafana/grafana.db "SELECT * FROM user;"
```

### 性能问题

#### 高CPU使用率
```bash
# 检查Prometheus查询
curl "http://localhost:9090/api/v1/query?query=topk(10, rate(prometheus_engine_query_duration_seconds_sum[5m]))"

# 优化查询
# 使用 recording rules
# 增加抓取间隔
# 减少指标数量
```

#### 高内存使用率
```bash
# 检查内存使用
curl "http://localhost:9090/api/v1/query?query=process_resident_memory_bytes"

# 优化配置
# 减少保留时间
# 调整块大小
# 增加内存限制
```

#### 磁盘空间不足
```bash
# 检查磁盘使用
df -h

# 清理旧数据
docker exec prometheus prometheus tsdb clean --max-block-duration=30d

# 调整保留策略
# 减少保留时间
# 启用压缩
```

## 扩展与集成

### 集成现有系统

```yaml
# 集成现有Prometheus
remote_write:
  - url: "http://existing-prometheus:9090/api/v1/write"

# 集成外部告警
webhook_configs:
  - url: "http://external-system:8080/webhook"
```

### 添加自定义监控

```python
# 自定义导出器示例
from prometheus_client import start_http_server, Gauge
import time

# 定义指标
custom_metric = Gauge('custom_metric', '自定义业务指标')

# 更新指标值
def update_metric():
    custom_metric.set(42)

# 启动HTTP服务器
start_http_server(9101)
while True:
    update_metric()
    time.sleep(30)
```

### 扩展告警渠道

```yaml
# 添加企业微信告警
wechat_configs:
  - agent_id: '1000002'
    to_user: '@all'
    message: '{{ template "wechat.message" . }}'

# 添加短信告警
webhook_configs:
  - url: "http://sms-gateway:8080/send"
    send_resolved: true
```

## 附录

### 配置文件模板
所有配置文件模板可在 `config-templates/` 目录找到。

### 脚本说明
- `deploy-monitoring.sh`: 主部署脚本
- `start-monitoring.sh`: 启动脚本
- `stop-monitoring.sh`: 停止脚本
- `health-check.sh`: 健康检查脚本
- `backup-monitoring.sh`: 备份脚本
- `restore-monitoring.sh`: 恢复脚本

### 参考文档
- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [AlertManager官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Docker Compose文档](https://docs.docker.com/compose/)

### 技术支持
- 问题反馈: infrastructure-team@ai-ready.local
- 紧急支持: +86-XXX-XXXX-XXXX
- 文档更新: https://wiki.ai-ready.local/monitoring

---

**最后更新**: 2026-04-27  
**版本**: v1.0.0  
**作者**: AI-Ready DevOps团队