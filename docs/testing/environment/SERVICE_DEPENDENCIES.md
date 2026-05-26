# 测试环境服务依赖关系文档

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [服务列表](#服务列表)
3. [依赖关系图](#依赖关系图)
4. [启动顺序](#启动顺序)
5. [健康检查](#健康检查)
6. [故障依赖](#故障依赖)
7. [服务发现](#服务发现)

---

## 概述

### 依赖关系重要性

测试环境包含多个服务，正确理解服务间的依赖关系对于：

- 正确启动服务序列至关重要
- 故障排查时快速定位问题
- 优化服务启动时间
- 确保系统稳定性

### 依赖类型

| 类型 | 描述 | 示例 |
|------|------|------|
| 强依赖 | 目标服务必须先启动 | Prometheus → PostgreSQL |
| 弱依赖 | 目标服务启动后可重连 |应用服务 → Redis |
| 可选依赖 | 无依赖关系 | Grafana → AlertManager |

---

## 服务列表

### 基础设施服务

| 服务名 | 端口 | 容器名 | 类型 | 必需 | 健康检查 |
|--------|------|--------|------|------|----------|
| PostgreSQL | 5432 | postgres-monitoring | 数据库 | 是 | pg_isready |
| Redis | 6379 | redis-monitoring | 缓存 | 是 | redis-cli |
| RabbitMQ | 5672 | rabbitmq-monitoring | 消息队列 | 是 | rabbitmq-diagnostics |

### 监控服务

| 服务名 | 端口 | 容器名 | 类型 | 必需 | 健康检查 |
|--------|------|--------|------|------|----------|
| Prometheus | 9090 | prometheus-monitoring | 监控 | 是 | wget健康检查 |
| AlertManager | 9093 | alertmanager-monitoring | 告警 | 是 | wget健康检查 |
| Grafana | 3000 | grafana-monitoring | 可视化 | 是 | API健康检查 |
| custom-exporter | 9101 | custom-exporter-monitoring | 业务导出 | 是 | curl健康检查 |

### 应用服务

| 服务名 | 端口 | 容器名 | 类型 | 必需 | 健康检查 |
|--------|------|--------|------|------|----------|
| monitoring-api | 8080 | monitoring-api-service | API服务 | 是 | Actuator健康检查 |
| Nginx | 80/443 | nginx-monitoring | 网关 | 是 | nginx -t |

### 服务配置

| 服务 | 容器网络IP | 宿主机端口 | 健康检查间隔 | 健康检查超时 |
|------|-----------|-----------|-------------|-------------|
| PostgreSQL | 172.30.0.2 | 5433 | 30s | 10s |
| Redis | 172.30.0.3 | 6380 | 30s | 10s |
| RabbitMQ | 172.30.0.4 | 5673/15673 | 30s | 10s |
| Prometheus | 172.30.0.5 | 9090 | 30s | 10s |
| AlertManager | 172.30.0.6 | 9093 | 30s | 10s |
| Grafana | 172.30.0.7 | 3000 | 30s | 10s |
| custom-exporter | 172.30.0.8 | 9101 | 30s | 10s |
| monitoring-api | 172.30.0.9 | 8081 | 30s | 10s |
| Nginx | 172.30.0.10 | 80/443 | 30s | 10s |

---

## 依赖关系图

### Docker Compose 依赖拓扑

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Compose 依赖关系                        │
│                                                                 │
│  PostgreSQL (172.30.0.2)                                        │
│  └── PostgreSQL 服务 (5432)                                    │
│                                                                 │
│  Redis (172.30.0.3)                                            │
│  └── Redis 服务 (6379)                                          │
│                                                                 │
│  RabbitMQ (172.30.0.4)                                         │
│  └── RabbitMQ 服务 (5672, 15672)                               │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   启动层 (0s)                             │   │
│  │    PostgreSQL + Redis + RabbitMQ                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   监控层 (40s+)                           │   │
│  │    Prometheus     ────────► AlertManager                 │   │
│  │    (需数据库连接)    (需Prometheus)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   应用层 (30s-90s)                       │   │
│  │    custom-exporter    → 数据库 + Redis + RabbitMQ        │   │
│  │    monitoring-api     → 数据库 + Redis + RabbitMQ        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   网关层                                  │   │
│  │    Nginx → 所有上游服务                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 服务依赖详细图

```
┌─────────────────────────────────────────────────────────────────┐
│                    服务依赖详细关系                               │
│                                                                 │
│  PostgreSQL (172.30.0.2:5432)                                  │
│     │                                                            │
│     ├─► Prometheus (监控数据存储)                               │
│     ├─► AlertManager (告警数据存储)                            │
│     ├─► custom-exporter (业务指标查询)                         │
│     └─► monitoring-api (应用数据存储)                          │
│                                                                 │
│  Redis (172.30.0.3:6379)                                       │
│     │                                                            │
│     ├─► custom-exporter (缓存指标)                             │
│     └─► monitoring-api (缓存服务)                              │
│                                                                 │
│  RabbitMQ (172.30.0.4:5672)                                    │
│     │                                                            │
│     ├─► custom-exporter (队列指标)                             │
│     └─► monitoring-api (消息服务)                              │
│                                                                 │
│  Prometheus (172.30.0.5:9090)                                  │
│     │                                                            │
│     ├─► AlertManager (告警发送)                                │
│     └─► Grafana (数据源)                                       │
│                                                                 │
│  AlertManager (172.30.0.6:9093)                                │
│     │                                                            │
│     └─► Grafana (告警视图)                                     │
│                                                                 │
│  Grafana (172.30.0.7:3000)                                     │
│     │                                                            │
│     └─► 所有服务可视化展示                                     │
│                                                                 │
│  custom-exporter (172.30.0.8:9101)                             │
│     │                                                            │
│     ├─► Prometheus (指标采集)                                  │
│     └─► Grafana (数据源)                                       │
│                                                                 │
│  monitoring-api (172.30.0.9:8080)                              │
│     │                                                            │
│     ├─► PostgreSQL (数据库操作)                                │
│     ├─► Redis (缓存操作)                                       │
│     ├─► RabbitMQ (消息操作)                                    │
│     └─► Prometheus (指标上报)                                  │
│                                                                 │
│  Nginx (172.30.0.10:80/443)                                    │
│     │                                                            │
│     ├─► Grafana (反向代理)                                     │
│     ├─► monitoring-api (反向代理)                              │
│     ├─► Prometheus (反向代理)                                  │
│     └─► AlertManager (反向代理)                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 启动顺序

### 启动时间线

```
时间轴 (秒)
0s    ██████████████████████████████████████████████████████████
      │
      ├─► PostgreSQL 启动
      │   └─ 等待40s (start_period)
      │
      ├─► Redis 启动
      │   └─ 等待20s (start_period)
      │
      └─► RabbitMQ 启动
          └─ 等待40s (start_period)

40s   ██████████████████████████████████████████████████████████
      │
      ├─► PostgreSQL 健康 ✓
      ├─► Redis 健康 ✓
      └─► RabbitMQ 健康 ✓

40s+  ██████████████████████████████████████████████████████████
      │
      ├─► Prometheus 启动 (需数据库连接)
      │   └─ 等待30s (start_period)
      │
      ├─► AlertManager 启动 (需Prometheus)
      │   └─ 等待30s (start_period)
      │
      └─► Grafana 启动 (需Prometheus)
          └─ 等待60s (start_period)

70s+  ██████████████████████████████████████████████████████████
      │
      ├─► Prometheus 健康 ✓
      ├─► AlertManager 健康 ✓
      └─► Grafana 健康 ✓

70s+  ██████████████████████████████████████████████████████████
      │
      ├─► custom-exporter 启动 (需数据库/Redis/RabbitMQ)
      │   └─ 等待40s (start_period)
      │
      └─► monitoring-api 启动 (需所有服务)
          └─ 等待90s (start_period)

110s+ ██████████████████████████████████████████████████████████
      │
      ├─► custom-exporter 健康 ✓
      └─► monitoring-api 健康 ✓

110s+ ██████████████████████████████████████████████████████████
      │
      └─► Nginx 启动 (需所有上游服务)

120s+ ██████████████████████████████████████████████████████████
      │
      └─► Nginx 健康 ✓ (服务全部就绪)
```

### 启动分组

#### 第一组：基础设施服务 (0s启动，30-40s健康)

| 服务 | 启动时间 | 健康时间 | 启动命令 |
|------|----------|----------|----------|
| PostgreSQL | 0s | 40s | docker-compose up -d postgres-monitoring |
| Redis | 0s | 20s | docker-compose up -d redis-monitoring |
| RabbitMQ | 0s | 40s | docker-compose up -d rabbitmq-monitoring |

#### 第二组：监控服务 (启动后需等待)

| 服务 | 启动时间 | 健康时间 | 依赖服务 |
|------|----------|----------|----------|
| Prometheus | 0s | 30s | PostgreSQL, Redis, RabbitMQ |
| AlertManager | 0s | 30s | Prometheus |
| Grafana | 0s | 60s | Prometheus |

#### 第三组：应用服务 (最长健康检查时间)

| 服务 | 启动时间 | 健康时间 | 依赖服务 |
|------|----------|----------|----------|
| custom-exporter | 0s | 40s | PostgreSQL, Redis, RabbitMQ |
| monitoring-api | 0s | 90s | PostgreSQL, Redis, RabbitMQ, Prometheus |
| Nginx | 0s | - | 所有上游服务 |

---

## 健康检查

### 健康检查配置

#### PostgreSQL

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U monitoring_user -d monitoring_db"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

**检查命令**:
```bash
pg_isready -U monitoring_user -d monitoring_db
```

#### Redis

```yaml
healthcheck:
  test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 20s
```

**检查命令**:
```bash
redis-cli --raw incr ping
```

#### RabbitMQ

```yaml
healthcheck:
  test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

**检查命令**:
```bash
rabbitmq-diagnostics -q ping
```

#### Prometheus

```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 30s
```

**检查命令**:
```bash
wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy
```

#### Grafana

```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**检查命令**:
```bash
wget --no-verbose --tries=1 --spider http://localhost:3000/api/health
```

#### custom-exporter

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:9101/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

**检查命令**:
```bash
curl -f http://localhost:9101/health
```

#### monitoring-api

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 90s
```

**检查命令**:
```bash
curl -f http://localhost:8080/actuator/health
```

### 健康检查脚本

创建健康检查脚本 `healthcheck-service.sh`:

```bash
#!/bin/bash

# 健康检查超时（秒）
TIMEOUT=120

# 检查服务是否健康
check_service() {
    local service=$1
    local command=$2
    
    echo "检查服务: $service"
    
    local count=0
    while [ $count -lt $TIMEOUT ]; do
        if eval "$command" > /dev/null 2>&1; then
            echo "服务 $service 已就绪"
            return 0
        fi
        sleep 5
        count=$((count + 5))
        echo "  等待中... ($count/$TIMEOUT)"
    done
    
    echo "错误: 服务 $service 超时"
    return 1
}

# 检查所有服务
echo "开始检查测试环境服务健康状态..."

check_service "PostgreSQL" "docker-compose exec postgres-monitoring pg_isready -U monitoring_user -d monitoring_db"
check_service "Redis" "docker-compose exec redis-monitoring redis-cli --raw incr ping"
check_service "RabbitMQ" "docker-compose exec rabbitmq-monitoring rabbitmq-diagnostics -q ping"
check_service "Prometheus" "docker-compose exec prometheus wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy"
check_service "AlertManager" "docker-compose exec alertmanager wget --no-verbose --tries=1 --spider http://localhost:9093/-/healthy"
check_service "Grafana" "docker-compose exec grafana wget --no-verbose --tries=1 --spider http://localhost:3000/api/health"
check_service "custom-exporter" "docker-compose exec custom-exporter curl -f http://localhost:9101/health"
check_service "monitoring-api" "docker-compose exec monitoring-api curl -f http://localhost:8080/actuator/health"
check_service "Nginx" "docker-compose exec nginx nginx -t"

echo "所有服务健康检查完成"
```

---

## 故障依赖

### 故障影响矩阵

| 故障服务 | 影响的服务 | 影响程度 | 恢复建议 |
|---------|-----------|---------|---------|
| PostgreSQL | Prometheus, AlertManager, custom-exporter, monitoring-api | 严重 | 立即恢复数据库 |
| Redis | custom-exporter, monitoring-api | 中等 | 重启Redis，重连应用 |
| RabbitMQ | custom-exporter, monitoring-api | 中等 | 重启RabbitMQ |
| Prometheus | AlertManager, Grafana | 严重 | 重启Prometheus |
| AlertManager | Grafana (告警视图) | 轻微 | 重启AlertManager |
| Grafana | 所有服务 (可视化) | 轻微 | 重启Grafana |
| custom-exporter | Prometheus | 轻微 | 重启custom-exporter |
| monitoring-api | 所有服务 (API) | 中等 | 重启API服务 |
| Nginx | 所有服务 (访问) | 严重 | 重启Nginx或检查配置 |

### 故障链分析

#### 故障场景1: PostgreSQL 故障

```
PostgreSQL 故障
    ↓
    │
    ├─► Prometheus 无法写入数据 (严重)
    │
    ├─► AlertManager 无法存储告警 (严重)
    │
    ├─► custom-exporter 无法查询数据 (严重)
    │
    └─► monitoring-api 无法连接数据库 (严重)
```

**影响**: 整个监控系统失效

**恢复步骤**:
1. 检查PostgreSQL容器状态: `docker-compose ps postgres-monitoring`
2. 查看PostgreSQL日志: `docker-compose logs postgres-monitoring`
3. 检查数据卷权限: `ls -la ./volumes/postgres`
4. 恢复数据库: `docker-compose restart postgres-monitoring`

#### 故障场景2: Prometheus 故障

```
Prometheus 故障
    ↓
    │
    ├─► AlertManager 无法接收告警 (严重)
    │
    ├─► Grafana 无法查询数据 (严重)
    │
    └─► custom-exporter 无法上报数据 (轻微)
```

**影响**: 告警系统失效

**恢复步骤**:
1. 检查Prometheus容器状态
2. 查看Prometheus日志
3. 检查存储卷空间: `df -h ./volumes/prometheus`
4. 重启Prometheus: `docker-compose restart prometheus`

#### 故障场景3: Nginx 故障

```
Nginx 故障
    ↓
    │
    └─► 所有服务外部访问中断 (严重)
```

**影响**: 外部无法访问

**恢复步骤**:
1. 检查Nginx配置: `docker-compose exec nginx nginx -t`
2. 查看Nginx日志: `docker-compose logs nginx`
3. 重启Nginx: `docker-compose restart nginx`

---

## 服务发现

### Docker Compose 服务发现

Docker Compose 内置服务发现机制，容器之间通过服务名通信：

```yaml
# Prometheus 配置中使用服务名
scrape_configs:
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-monitoring:5432']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-monitoring:6379']
  
  - job_name: 'custom-exporter'
    static_configs:
      - targets: ['custom-exporter:9101']
```

### 健康检查中的服务发现

```yaml
# monitoring-api 依赖检查
depends_on:
  postgres-monitoring:
    condition: service_healthy
  redis-monitoring:
    condition: service_healthy
  rabbitmq-monitoring:
    condition: service_healthy
  prometheus:
    condition: service_healthy
```

### 自定义健康检查脚本

创建 `wait-for-services.sh`:

```bash
#!/bin/bash

# 等待服务启动完成
wait_for_service() {
    local service=$1
    local port=$2
    local timeout=${3:-60}
    
    echo "等待 $service:$port..."
    
    for i in $(seq 1 $timeout); do
        if nc -z $service $port 2>/dev/null; then
            echo "$service:$port 已就绪"
            return 0
        fi
        sleep 1
    done
    
    echo "错误: $service:$port 超时"
    return 1
}

# 等待所有服务
wait_for_service postgres-monitoring 5432
wait_for_service redis-monitoring 6379
wait_for_service rabbitmq-monitoring 5672
wait_for_service prometheus 9090

echo "所有服务已就绪"
```

---

## 启动命令

### 快速启动

```bash
# 一键启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 等待服务启动
sleep 120

# 检查所有服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 逐步启动

```bash
# 第一步：启动基础服务
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring rabbitmq-monitoring

# 等待基础服务就绪
sleep 40

# 第二步：启动监控服务
docker-compose -f monitoring-alerting-deployment.yml up -d prometheus alertmanager grafana

# 等待监控服务就绪
sleep 60

# 第三步：启动应用服务
docker-compose -f monitoring-alerting-deployment.yml up -d custom-exporter monitoring-api nginx

# 检查所有服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 重启服务

```bash
# 重启单个服务
docker-compose -f monitoring-alerting-deployment.yml restart postgres-monitoring

# 重启所有服务
docker-compose -f monitoring-alerting-deployment.yml restart

# 强制重建服务
docker-compose -f monitoring-alerting-deployment.yml up -d --force-recreate
```

---

## 故障排查

### 检查服务依赖

```bash
# 查看服务依赖关系
docker-compose -f monitoring-alerting-deployment.yml config --services

# 查看容器状态
docker-compose -f monitoring-alerting-deployment.yml ps

# 查看容器日志
docker-compose -f monitoring-alerting-deployment.yml logs <service>

# 进入容器
docker-compose -f monitoring-alerting-deployment.yml exec <service> /bin/sh
```

### 排查步骤

1. **检查容器状态**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml ps
   ```

2. **查看日志**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml logs <service>
   ```

3. **检查健康状态**
   ```bash
   docker inspect --format='{{.State.Health.Status}}' <container_name>
   ```

4. **测试连接**
   ```bash
   docker-compose -f monitoring-alerting-deployment.yml exec postgres-monitoring pg_isready
   ```

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker Compose Dependency Management](https://docs.docker.com/compose/startup-order/)
- [Heathy Checks in Docker](https://docs.docker.com/engine/reference/builder/#healthcheck)
- [Prometheus Health Checking](https://prometheus.io/docs/guides/health-check/)