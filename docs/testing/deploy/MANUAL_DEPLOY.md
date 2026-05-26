# 手动部署操作手册

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [前置条件](#前置条件)
3. [手动部署步骤](#手动部署步骤)
4. [服务配置](#服务配置)
5. [服务启动](#服务启动)
6. [服务验证](#服务验证)
7. [故障排查](#故障排查)

---

## 概述

### 手动部署场景

手动部署适用于以下场景：

- **开发环境**: 快速部署单个服务
- **故障恢复**: 快速启动特定服务
- **调试测试**: 逐个启动服务，方便调试
- **学习目的**: 了解服务启动流程

### 手动部署特点

- **灵活性**: 可以逐个启动服务
- **可控性**: 每个服务手动配置参数
- **可调试性**: 容易定位问题
- **复杂性**: 需要管理多个服务的启动顺序和依赖

---

## 前置条件

### 系统要求

| 项目 | 最低要求 |
|------|---------|
| 操作系统 | Ubuntu 20.04+ / CentOS 7+ / Windows 10 |
| Docker | 20.10+ |
| Docker Compose | 2.0+ |
| 内存 | 8GB |
| 磁盘 | 50GB |

### 软件安装

```bash
# 检查Docker版本
docker --version

# 检查Docker Compose版本
docker-compose --version

# 检查Docker状态
docker info
```

### 目录准备

```bash
# 创建部署目录
mkdir -p I:\AI-Ready\deploy
cd I:\AI-Ready\deploy

# 创建数据卷目录
mkdir -p volumes/{postgres,redis,rabbitmq,prometheus,grafana,alertmanager}
mkdir -p init-scripts
mkdir -p prometheus/{rules,file_sd}
mkdir -p alertmanager/templates
mkdir -p grafana/{provisioning,dashboards}
mkdir -p nginx/{conf.d,ssl,html}
mkdir -p custom-exporter/config
mkdir -p logs/monitoring-api
```

---

## 手动部署步骤

### 步骤1: 创建Docker网络

```bash
# 创建Docker网络
docker network create \
  --driver bridge \
  --subnet 172.30.0.0/24 \
  --gateway 172.30.0.1 \
  monitoring-network
```

### 步骤2: 创建数据卷

```bash
# 创建数据卷
docker volume create prometheus-data
docker volume create grafana-data
docker volume create alertmanager-data
docker volume create postgres-data
docker volume create redis-data
docker volume create rabbitmq-data
```

### 步骤3: 创建配置文件

#### PostgreSQL初始化脚本

创建 `init-scripts/postgres-init.sql`:

```sql
-- PostgreSQL初始化脚本
-- 创建自定义扩展

-- 创建监控用户
CREATE USER monitoring_user WITH PASSWORD 'monitoring_pass_123';

-- 创建数据库
CREATE DATABASE monitoring_db;

-- 授权
GRANT ALL PRIVILEGES ON DATABASE monitoring_db TO monitoring_user;
ALTER DATABASE monitoring_db OWNER TO monitoring_user;

-- 创建监控表
CREATE TABLE IF NOT EXISTS metrics (
    id SERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value FLOAT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    labels JSONB
);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO monitoring_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO monitoring_user;
```

#### Prometheus配置

创建 `prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    monitor: 'monitoring-alerting'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager-monitoring:9093']

rule_files:
  - /etc/prometheus/rules/*.yml

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-monitoring:5432']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-monitoring:6379']

  - job_name: 'rabbitmq'
    static_configs:
      - targets: ['rabbitmq-monitoring:9091']

  - job_name: 'custom-exporter'
    static_configs:
      - targets: ['custom-exporter-monitoring:9101']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['prometheus-monitoring:9100']

  - job_name: 'alertmanager'
    static_configs:
      - targets: ['alertmanager-monitoring:9093']

  - job_name: 'grafana'
    static_configs:
      - targets: ['grafana-monitoring:3000']

  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-monitoring:9113']
```

创建 `prometheus/rules/system-alerts.yml`:

```yaml
groups:
  - name: system-alerts
    rules:
      - alert: HighCPUUsage
        expr: node_cpu_seconds_total > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
          description: "CPU usage is above 80% for more than 5 minutes"

      - alert: HighMemoryUsage
        expr: node_memory_MemAvailable_bytes < 2147483648
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
          description: "Available memory is less than 2GB for more than 5 minutes"

      - alert: HighDiskUsage
        expr: node_filesystem_avail_bytes < 10737418240
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High disk usage detected"
          description: "Available disk space is less than 10GB for more than 5 minutes"
```

#### AlertManager配置

创建 `alertmanager/alertmanager.yml`:

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'webhook-dingding'
  routes:
    - match:
        severity: critical
      receiver: 'webhook-enterprise-wechat'
    - match:
        severity: warning
      receiver: 'webhook-dingding'

receivers:
  - name: 'webhook-dingding'
    webhook_configs:
      - url: 'http://dingtalk-webhook-url'
        send_resolved: true

  - name: 'webhook-enterprise-wechat'
    webhook_configs:
      - url: 'http://enterprise-wechat-webhook-url'
        send_resolved: true

  - name: 'email'
    email_configs:
      - to: 'admin@example.com'
        send_resolved: true
```

创建 `alertmanager/templates/default.tmpl`:

```go
{{ define "default.title" }}
[{{ .Status | toUpper }}] {{ .CommonAnnotations.summary }}
{{ end }}

{{ define "default.description" }}
{{ range .Alerts }}
* Alert:* {{ .Annotations.summary }}
* Description:* {{ .Annotations.description }}
* Severity:* {{ .Labels.severity }}
* Starts:* {{ .StartsAt }}
* Ends:* {{ .EndsAt }}
{{ end }}
{{ end }}

{{ define "default.message" }}
{{ template "default.title" . }}
{{ template "default.description" . }}
{{ end }}
```

### 步骤4: 启动PostgreSQL

```bash
# 启动PostgreSQL容器
docker run -d \
  --name postgres-monitoring \
  --network monitoring-network \
  -e POSTGRES_DB=monitoring_db \
  -e POSTGRES_USER=monitoring_user \
  -e POSTGRES_PASSWORD=monitoring_pass_123 \
  -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" \
  -p 5433:5432 \
  -v postgres-data:/var/lib/postgresql/data \
  -v $(pwd)/init-scripts:/docker-entrypoint-initdb.d:ro \
  --restart unless-stopped \
  --health-cmd="pg_isready -U monitoring_user -d monitoring_db" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=40s \
  postgres:15-alpine
```

### 步骤5: 启动Redis

```bash
# 启动Redis容器
docker run -d \
  --name redis-monitoring \
  --network monitoring-network \
  -p 6380:6379 \
  -v redis-data:/data \
  --restart unless-stopped \
  --health-cmd="redis-cli --raw incr ping" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=20s \
  redis:7-alpine \
  redis-server \
    --requirepass redis_monitoring_pass_123 \
    --maxmemory 256mb \
    --maxmemory-policy allkeys-lru
```

### 步骤6: 启动RabbitMQ

```bash
# 启动RabbitMQ容器
docker run -d \
  --name rabbitmq-monitoring \
  --network monitoring-network \
  -p 5673:5672 \
  -p 15673:15672 \
  -v rabbitmq-data:/var/lib/rabbitmq \
  --restart unless-stopped \
  --health-cmd="rabbitmq-diagnostics -q ping" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=40s \
  -e RABBITMQ_DEFAULT_USER=monitoring_rabbit \
  -e RABBITMQ_DEFAULT_PASS=rabbit_monitoring_pass_123 \
  -e RABBITMQ_DEFAULT_VHOST=monitoring_vhost \
  rabbitmq:3.12-management-alpine
```

### 步骤7: 启动Prometheus

```bash
# 启动Prometheus容器
docker run -d \
  --name prometheus-monitoring \
  --network monitoring-network \
  -p 9090:9090 \
  -u "65534:65534" \
  -v prometheus-data:/prometheus \
  -v $(pwd)/prometheus:/etc/prometheus:ro \
  --restart unless-stopped \
  --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=30s \
  prom/prometheus:v2.45.0 \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --storage.tsdb.retention.time=30d \
  --storage.tsdb.retention.size=10GB \
  --web.enable-lifecycle \
  --web.enable-admin-api
```

### 步骤8: 启动AlertManager

```bash
# 启动AlertManager容器
docker run -d \
  --name alertmanager-monitoring \
  --network monitoring-network \
  -p 9093:9093 \
  -u "65534:65534" \
  -v alertmanager-data:/alertmanager \
  -v $(pwd)/alertmanager:/etc/alertmanager:ro \
  --restart unless-stopped \
  --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:9093/-/healthy" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=30s \
  prom/alertmanager:v0.25.0 \
  --config.file=/etc/alertmanager/alertmanager.yml \
  --storage.path=/alertmanager \
  --web.external-url=http://localhost:9093
```

### 步骤9: 启动Grafana

```bash
# 启动Grafana容器
docker run -d \
  --name grafana-monitoring \
  --network monitoring-network \
  -p 3000:3000 \
  -u "472:472" \
  -v grafana-data:/var/lib/grafana \
  -e GF_SECURITY_ADMIN_PASSWORD=admin123 \
  -e GF_INSTALL_PLUGINS=grafana-piechart-panel,grafana-clock-panel,grafana-simple-json-datasource \
  -e GF_SERVER_ROOT_URL=http://localhost:3000 \
  -e GF_SERVER_SERVE_FROM_SUB_PATH=false \
  -e GF_USERS_ALLOW_SIGN_UP=false \
  -e GF_AUTH_ANONYMOUS_ENABLED=false \
  --restart unless-stopped \
  --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:3000/api/health" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=60s \
  grafana/grafana:10.0.3
```

### 步骤10: 启动custom-exporter

```bash
# 构建custom-exporter镜像
docker build -f custom-exporter/Dockerfile -t custom-exporter:latest .

# 启动custom-exporter容器
docker run -d \
  --name custom-exporter-monitoring \
  --network monitoring-network \
  -p 9101:9101 \
  -e POSTGRES_CONNECTION_STRING="postgresql://monitoring_user:monitoring_pass_123@postgres-monitoring:5432/monitoring_db" \
  -e REDIS_CONNECTION_STRING="redis://:redis_monitoring_pass_123@redis-monitoring:6379" \
  -e RABBITMQ_CONNECTION_STRING="amqp://monitoring_rabbit:rabbit_monitoring_pass_123@rabbitmq-monitoring:5672/monitoring_vhost" \
  -e EXPORTER_PORT=9101 \
  -e METRICS_PATH=/metrics \
  -e COLLECT_INTERVAL=30 \
  -v $(pwd)/custom-exporter/config:/app/config:ro \
  --restart unless-stopped \
  --health-cmd="curl -f http://localhost:9101/health" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=40s \
  custom-exporter:latest
```

### 步骤11: 启动monitoring-api

```bash
# 获取monitoring-api镜像
docker pull your-registry/monitoring-api:latest

# 启动monitoring-api容器
docker run -d \
  --name monitoring-api-service \
  --network monitoring-network \
  -p 8081:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres-monitoring:5432/monitoring_db" \
  -e SPRING_DATASOURCE_USERNAME=monitoring_user \
  -e SPRING_DATASOURCE_PASSWORD=monitoring_pass_123 \
  -e SPRING_REDIS_HOST=redis-monitoring \
  -e SPRING_REDIS_PORT=6379 \
  -e SPRING_REDIS_PASSWORD=redis_monitoring_pass_123 \
  -e SPRING_RABBITMQ_HOST=rabbitmq-monitoring \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_USERNAME=monitoring_rabbit \
  -e SPRING_RABBITMQ_PASSWORD=rabbit_monitoring_pass_123 \
  -e SPRING_RABBITMQ_VIRTUAL_HOST=monitoring_vhost \
  -e PROMETHEUS_METRICS_ENABLED=true \
  -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health,info,metrics,prometheus" \
  -v $(pwd)/logs/monitoring-api:/app/logs \
  --restart unless-stopped \
  --health-cmd="curl -f http://localhost:8080/actuator/health" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --health-start-period=90s \
  your-registry/monitoring-api:latest
```

### 步骤12: 启动Nginx

```bash
# 启动Nginx容器
docker run -d \
  --name nginx-monitoring \
  --network monitoring-network \
  -p 80:80 \
  -p 443:443 \
  -v $(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v $(pwd)/nginx/conf.d:/etc/nginx/conf.d:ro \
  -v $(pwd)/nginx/ssl:/etc/nginx/ssl:ro \
  -v $(pwd)/nginx/html:/usr/share/nginx/html:ro \
  --restart unless-stopped \
  --health-cmd="nginx -t" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  nginx:alpine
```

---

## 服务配置

### PostgreSQL配置

```bash
# 连接到PostgreSQL
docker exec -it postgres-monitoring psql -U monitoring_user -d monitoring_db

# 创建监控表
CREATE TABLE IF NOT EXISTS metrics (
    id SERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value FLOAT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    labels JSONB
);

# 退出PostgreSQL
\q
```

### Prometheus配置

```bash
# 进入Prometheus容器
docker exec -it prometheus-monitoring sh

# 编辑配置文件（如果需要）
vi /etc/prometheus/prometheus.yml

# 重新加载配置（如果支持热加载）
curl -X POST http://localhost:9090/-/reload

# 退出容器
exit
```

### Grafana配置

1. 访问Grafana: http://localhost:3000
2. 登录: admin/admin123
3. 配置数据源: Prometheus
4. 导入仪表盘
5. 配置告警通知

### AlertManager配置

1. 访问AlertManager: http://localhost:9093
2. 配置告警路由
3. 配置通知渠道
4. 测试告警发送

---

## 服务启动

### 启动顺序

```
Step 1: 创建Docker网络 → monitoring-network
Step 2: 创建数据卷 → prometheus-data, grafana-data, alertmanager-data, postgres-data, redis-data, rabbitmq-data
Step 3: 创建配置文件 → prometheus.yml, alertmanager.yml, init-scripts
Step 4: 启动PostgreSQL
Step 5: 启动Redis
Step 6: 启动RabbitMQ
Step 7: 等待基础服务就绪 (30秒)
Step 8: 启动Prometheus
Step 9: 启动AlertManager
Step 10: 启动Grafana
Step 11: 等待监控服务就绪 (60秒)
Step 12: 启动custom-exporter
Step 13: 启动monitoring-api
Step 14: 启动Nginx
Step 15: 等待所有服务就绪 (30秒)
```

### 启动验证

```bash
# 检查所有容器状态
docker ps

# 检查特定容器状态
docker inspect --format='{{.State.Status}}' postgres-monitoring
docker inspect --format='{{.State.Status}}' redis-monitoring
docker inspect --format='{{.State.Status}}' prometheus-monitoring
```

---

## 服务验证

### 验证步骤

#### Step 1: 检查容器状态

```bash
docker ps
```

预期输出包含所有9个容器，状态为Running。

#### Step 2: 检查健康状态

```bash
# PostgreSQL
docker exec postgres-monitoring pg_isready -U monitoring_user -d monitoring_db

# Redis
docker exec redis-monitoring redis-cli --raw incr ping

# RabbitMQ
docker exec rabbitmq-monitoring rabbitmq-diagnostics -q ping

# Prometheus
docker exec prometheus wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy

# AlertManager
docker exec alertmanager wget --no-verbose --tries=1 --spider http://localhost:9093/-/healthy

# Grafana
docker exec grafana wget --no-verbose --tries=1 --spider http://localhost:3000/api/health

# custom-exporter
docker exec custom-exporter curl -f http://localhost:9101/health

# monitoring-api
docker exec monitoring-api curl -f http://localhost:8080/actuator/health

# Nginx
docker exec nginx nginx -t
```

#### Step 3: 检查网络连接

```bash
# PostgreSQL → Prometheus
docker exec prometheus nc -zv postgres-monitoring 5432

# Redis → custom-exporter
docker exec custom-exporter nc -zv redis-monitoring 6379

# RabbitMQ → custom-exporter
docker exec custom-exporter nc -zv rabbitmq-monitoring 5672

# Prometheus → AlertManager
docker exec alertmanager nc -zv prometheus-monitoring 9090

# Grafana → Prometheus
docker exec grafana nc -zv prometheus-monitoring 9090
```

#### Step 4: 测试功能

```bash
# PostgreSQL - 创建测试表
docker exec postgres-monitoring psql -U monitoring_user -d monitoring_db -c "CREATE TABLE test (id INT); DROP TABLE test;"

# Redis - 设置测试键
docker exec redis-monitoring redis-cli -a redis_monitoring_pass_123 SET test_key "test_value"

# RabbitMQ - 列出队列
docker exec rabbitmq-monitoring rabbitmqctl list_queues

# Prometheus - 查询状态
curl http://localhost:9090/api/v1/status/config

# Grafana - 查询API
curl http://localhost:3000/api/health

# AlertManager - 查询状态
curl http://localhost:9093/api/v2/status

# custom-exporter - 获取指标
curl http://localhost:9101/metrics

# monitoring-api - 查询API
curl http://localhost:8081/actuator/health

# Nginx - 测试配置
docker exec nginx nginx -t
```

### 验证清单

| 服务 | 健康检查 | 网络连接 | 功能测试 | 总体状态 |
|------|---------|---------|---------|---------|
| PostgreSQL | ✅ | ✅ | ✅ | ✅ |
| Redis | ✅ | ✅ | ✅ | ✅ |
| RabbitMQ | ✅ | ✅ | ✅ | ✅ |
| Prometheus | ✅ | ✅ | ✅ | ✅ |
| AlertManager | ✅ | ✅ | ✅ | ✅ |
| Grafana | ✅ | ✅ | ✅ | ✅ |
| custom-exporter | ✅ | ✅ | ✅ | ✅ |
| monitoring-api | ✅ | ✅ | ✅ | ✅ |
| Nginx | ✅ | ✅ | ✅ | ✅ |

---

## 服务管理

### 启动服务

```bash
# 启动单个服务
docker start postgres-monitoring

# 启动多个服务
docker start postgres-monitoring redis-monitoring rabbitmq-monitoring
```

### 停止服务

```bash
# 停止单个服务
docker stop postgres-monitoring

# 停止多个服务
docker stop postgres-monitoring redis-monitoring rabbitmq-monitoring
```

### 重启服务

```bash
# 重启单个服务
docker restart postgres-monitoring

# 重启多个服务
docker restart postgres-monitoring redis-monitoring rabbitmq-monitoring
```

### 删除服务

```bash
# 删除单个服务
docker stop postgres-monitoring
docker rm postgres-monitoring

# 删除所有服务
docker stop postgres-monitoring redis-monitoring rabbitmq-monitoring prometheus-monitoring alertmanager-monitoring grafana-monitoring custom-exporter-monitoring monitoring-api-service nginx-monitoring
docker rm postgres-monitoring redis-monitoring rabbitmq-monitoring prometheus-monitoring alertmanager-monitoring grafana-monitoring custom-exporter-monitoring monitoring-api-service nginx-monitoring
```

### 查看日志

```bash
# 查看所有容器日志
docker logs -f postgres-monitoring &
docker logs -f redis-monitoring &
docker logs -f prometheus-monitoring &

# 查看特定服务日志
docker logs -f postgres-monitoring
```

---

## 故障排查

### 常见问题

#### 问题1: 容器无法启动

**症状**: 容器状态为Created或Restarting

**排查步骤**:

```bash
# 查看容器日志
docker logs <container_name>

# 检查端口占用
netstat -tlnp | grep <port>

# 检查配置
docker run --rm -v $(pwd):/config <image> cat /path/to/config

# 强制启动
docker run -d --name <container_name> <image>
```

#### 问题2: 健康检查失败

**症状**: 容器状态为unhealthy

**排查步骤**:

```bash
# 查看健康检查日志
docker inspect --format='{{.State.Health.Log}}' <container>

# 手动测试健康检查
docker exec <container> <healthcheck_command>

# 检查依赖服务
docker ps
```

#### 问题3: 端口冲突

**症状**: 启动失败，提示端口已被占用

**排查步骤**:

```bash
# 检查端口占用
netstat -tlnp | grep 5433
netstat -tlnp | grep 6380
netstat -tlnp | grep 9090

# 修改端口映射
# 重新启动容器，使用不同的端口
docker run -p 5434:5432 ...
```

#### 问题4: 数据持久化失败

**症状**: 数据在容器重启后丢失

**排查步骤**:

```bash
# 检查数据卷
docker volume ls

# 检查容器挂载
docker inspect <container> | grep Mounts

# 手动挂载数据卷
docker run -v <volume_name>:/path ...
```

### 快速诊断脚本

```bash
#!/bin/bash

echo "========================================"
echo "服务手动部署诊断工具"
echo "========================================"

# 检查容器状态
echo "1. 检查容器状态..."
docker ps

# 检查健康状态
echo ""
echo "2. 检查健康状态..."
docker inspect --format='{{.State.Status}}' postgres-monitoring
docker inspect --format='{{.State.Status}}' redis-monitoring
docker inspect --format='{{.State.Status}}' prometheus-monitoring

# 测试网络连接
echo ""
echo "3. 测试网络连接..."
docker exec prometheus nc -zv postgres-monitoring 5432

# 测试服务功能
echo ""
echo "4. 测试服务功能..."
curl -s http://localhost:3000/api/health

echo ""
echo "========================================"
```

---

## 部署脚本

### 自动化部署脚本

创建脚本 `manual-deploy.sh`:

```bash
#!/bin/bash

echo "========================================"
echo "手动部署脚本"
echo "========================================"

cd I:\AI-Ready\deploy

# 创建Docker网络
echo "1. 创建Docker网络..."
docker network create --driver bridge --subnet 172.30.0.0/24 --gateway 172.30.0.1 monitoring-network 2>/dev/null || echo "   网络已存在"

# 创建数据卷
echo "2. 创建数据卷..."
for vol in prometheus-data grafana-data alertmanager-data postgres-data redis-data rabbitmq-data; do
    docker volume create $vol 2>/dev/null || echo "   卷 $vol 已存在"
done

# 启动服务
echo "3. 启动PostgreSQL..."
docker run -d --name postgres-monitoring --network monitoring-network ... postgres:15-alpine

echo "4. 启动Redis..."
docker run -d --name redis-monitoring --network monitoring-network ... redis:7-alpine

echo "5. 启动RabbitMQ..."
docker run -d --name rabbitmq-monitoring --network monitoring-network ... rabbitmq:3.12-management-alpine

echo "6. 启动Prometheus..."
docker run -d --name prometheus-monitoring --network monitoring-network ... prom/prometheus:v2.45.0

echo "7. 启动AlertManager..."
docker run -d --name alertmanager-monitoring --network monitoring-network ... prom/alertmanager:v0.25.0

echo "8. 启动Grafana..."
docker run -d --name grafana-monitoring --network monitoring-network ... grafana/grafana:10.0.3

echo "9. 启动custom-exporter..."
docker run -d --name custom-exporter-monitoring --network monitoring-network ... custom-exporter:latest

echo "10. 启动monitoring-api..."
docker run -d --name monitoring-api-service --network monitoring-network ... your-registry/monitoring-api:latest

echo "11. 启动Nginx..."
docker run -d --name nginx-monitoring --network monitoring-network ... nginx:alpine

# 等待服务就绪
echo "12. 等待服务就绪..."
sleep 120

# 验证部署
echo "13. 验证部署..."
docker ps
echo "========================================"
```

---

## 部署最佳实践

### 1. 逐步部署

- 先启动基础设施服务
- 再启动监控服务
- 最后启动应用服务
- 每步验证后再继续下一步

### 2. 健康检查

- 所有服务配置健康检查
- 设置合理的start_period
- 定期运行健康检查脚本

### 3. 日志管理

- 配置日志驱动
- 设置日志轮转
- 集中收集日志

### 4. 监控告警

- Prometheus监控所有服务
- AlertManager告警通知
- Grafana可视化监控数据

### 5. 自动化脚本

- 创建自动化部署脚本
- 创建健康检查脚本
- 创建恢复脚本

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker官方文档](https://docs.docker.com/)
- [Docker最佳实践](https://docs.docker.com/develop/BestPractices/)
- [Docker网络配置](https://docs.docker.com/network/)
- [Docker Volume管理](https://docs.docker.com/storage/volumes/)