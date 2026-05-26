# 测试环境网络拓扑文档

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [网络架构](#网络架构)
3. [容器网络](#容器网络)
4. [端口映射](#端口映射)
5. [网络隔离](#网络隔离)
6. [DNS配置](#dns配置)
7. [网络诊断](#网络诊断)

---

## 概述

### 网络设计目标

测试环境网络拓扑设计遵循以下原则：

- **安全性**: 网络隔离，限制外部访问
- **可管理性**: 清晰的网络分区和IP分配
- **可扩展性**: 支持未来网络扩展
- **高性能**: 优化网络路径，减少延迟

### 网络组件

测试环境包含以下网络组件：

| 组件 | 类型 | 用途 |
|------|------|------|
| Docker Bridge | 桥接网络 | 容器间通信 |
| Network Namespace | 网络命名空间 | 网络隔离 |
| Port Mapping | 端口映射 | 外部访问 |
| DNS Service | DNS服务 | 服务发现 |

---

## 网络架构

### 整体网络拓扑

```
┌──────────────────────────────────────────────────────────────────────┐
│                        主机网络层                                     │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐     │
│  │                   Docker Host                                │     │
│  │  Host IP: 192.168.1.100 (示例)                              │     │
│  │                                                               │     │
│  │  ┌───────────────────────────────────────────────────────┐   │     │
│  │  │              Docker Bridge Network                    │   │     │
│  │  │           monitoring-bridge (172.30.0.0/24)            │   │     │     │
│  │  │                                                       │   │     │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  │   │     │
│  │  │  │PostgreSQL│  │  Redis  │  │RabbitMQ │  │Prometheus│  │   │     │
│  │  │  │ 172.30.0.2│  │172.30.0.3│  │172.30.0.4│  │172.30.0.5│  │   │     │
│  │  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  │   │     │
│  │  │        │              │              │              │        │   │     │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  │   │     │
│  │  │  │AlertMgr │  │  Grafana│  │CustomEx │  │  API    │  │   │     │
│  │  │  │172.30.0.6│  │172.30.0.7│  │172.30.0.8│  │172.30.0.9│  │   │     │
│  │  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  │   │     │
│  │  │        │              │              │              │        │   │     │
│  │  │  ┌─────────┐                                               │   │     │
│  │  │  │  Nginx  │                                               │   │     │
│  │  │  │172.30.0.10│                                             │   │     │
│  │  │  └─────┬─────┘                                             │   │     │
│  │  │        │                                                    │   │     │
│  │  └────────┼───────────────────────────────────────────────────┘   │     │
│  │           │                                                      │     │
│  │           ▼                                                      │     │
│  │  ┌───────────────────────────────────────────────────────┐       │     │
│  │  │          端口映射 (Host ↔ Container)                   │       │     │
│  │  │  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐ │       │     │
│  │  │  │5433 │  │6380 │  │5673 │  │9090 │  │3000 │  │80  │ │       │     │
│  │  │  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘ │       │     │
│  │  │  (5432)   (6379)   (5672)   (9090)   (3000)   (80)   │        │     │
│  │  └───────────────────────────────────────────────────────┘        │     │
│  └───────────────────────────────────────────────────────────────────┘     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
                              ┌───────────────────────┐
                              │       外部网络         │
                              │   用户访问            │
                              └───────────────────────┘
```

### 网络分层架构

```
第4层: 用户访问层
┌─────────────────────────────────────────────────────────────────────┐
│  Nginx (80/443) → 外部网络                                          │
│   - Reverse Proxy                                                   │
│   - Load Balancing                                                  │
│   - SSL Termination                                                 │
└─────────────────────────────────────────────────────────────────────┘

第3层: 应用服务层
┌─────────────────────────────────────────────────────────────────────┐
│  monitoring-api (8080) → custom-exporter (9101)                    │
│   - API服务                                                         │
│   - 业务指标导出                                                    │
└─────────────────────────────────────────────────────────────────────┘

第2层: 监控服务层
┌─────────────────────────────────────────────────────────────────────┐
│  Prometheus (9090) → AlertManager (9093) → Grafana (3000)          │
│   - 数据收集 → 告警处理 → 可视化                                    │
└─────────────────────────────────────────────────────────────────────┘

第1层: 基础设施服务层
┌─────────────────────────────────────────────────────────────────────┐
│  PostgreSQL (5432) + Redis (6379) + RabbitMQ (5672)                │
│   - 数据存储 → 缓存 → 消息队列                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 容器网络

### Docker Network 配置

```yaml
networks:
  monitoring-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.0.0/24
          gateway: 172.30.0.1
```

### IP地址分配

| 容器名 | IP地址 | DNS名称 | 用途 |
|--------|--------|---------|------|
| postgres-monitoring | 172.30.0.2 | postgres-monitoring | PostgreSQL数据库 |
| redis-monitoring | 172.30.0.3 | redis-monitoring | Redis缓存 |
| rabbitmq-monitoring | 172.30.0.4 | rabbitmq-monitoring | RabbitMQ消息队列 |
| prometheus-monitoring | 172.30.0.5 | prometheus-monitoring | Prometheus监控 |
| alertmanager-monitoring | 172.30.0.6 | alertmanager-monitoring | AlertManager告警 |
| grafana-monitoring | 172.30.0.7 | grafana-monitoring | Grafana可视化 |
| custom-exporter-monitoring | 172.30.0.8 | custom-exporter-monitoring | 自定义导出器 |
| monitoring-api-service | 172.30.0.9 | monitoring-api | API服务 |
| nginx-monitoring | 172.30.0.10 | nginx-monitoring | Nginx网关 |

### Docker Network 桥接

```
┌─────────────────────────────────────────────────────────────────────┐
│                Docker Bridge Network: monitoring-bridge             │
│                                                                       │
│  Gateway: 172.30.0.1                                                 │
│  Subnet: 172.30.0.0/24                                               │
│  Interface: docker0 (monitoring-bridge)                              │
│                                                                       │
│  容器网络接口:                                                         │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │  Container 172.30.0.2 (postgres-monitoring)                   │   │
│  │  ┌─────────────┐                                              │   │
│  │  │ eth0        │ 172.30.0.2                                   │   │
│  │  └─────────────┘                                              │   │
│  │        │                                                       │   │
│  └────────┼──────────────────────────────────────────────────────┘   │
│           │                                                          │
│  ┌────────┼──────────────────────────────────────────────────────┐   │
│  │        │                                                       │   │
│  │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐        │   │
│  │  │ eth0        │   │ eth0        │   │ eth0        │        │   │
│  │  │172.30.0.3   │   │172.30.0.4   │   │172.30.0.5   │        │   │
│  │  └─────────────┘   └─────────────┘   └─────────────┘        │   │
│  │     Redis           RabbitMQ         Prometheus              │   │
│  └──────────────────────────────────────────────────────────────┘   │
│           │                                                          │
│  ┌────────┴──────────────────────────────────────────────────────┐   │
│  │                                                                 │   │
│  │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐        │   │
│  │  │ eth0        │   │ eth0        │   │ eth0        │        │   │
│  │  │172.30.0.6   │   │172.30.0.7   │   │172.30.0.8   │        │   │
│  │  └─────────────┘   └─────────────┘   └─────────────┘        │   │
│  │ AlertManager      Grafana          CustomExporter          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│           │                                                          │
│  ┌────────┴──────────────────────────────────────────────────────┐   │
│  │                                                                 │   │
│  │  ┌─────────────┐   ┌─────────────┐                           │   │
│  │  │ eth0        │   │ eth0        │                           │   │
│  │  │172.30.0.9   │   │172.30.0.10  │                           │   │
│  │  └─────────────┘   └─────────────┘                           │   │
│  │ Monitoring-APi      Nginx                                     │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 容器网络通信

#### 容器间通信

```
容器间通信路径:

PostgreSQL (172.30.0.2)
    ↓
    ├─► Prometheus (172.30.0.5) - PostgreSQL连接
    ├─► AlertManager (172.30.0.6) - PostgreSQL连接
    ├─► custom-exporter (172.30.0.8) - PostgreSQL连接
    └─► monitoring-api (172.30.0.9) - PostgreSQL连接

Redis (172.30.0.3)
    ↓
    ├─► custom-exporter (172.30.0.8) - Redis连接
    └─► monitoring-api (172.30.0.9) - Redis连接

RabbitMQ (172.30.0.4)
    ↓
    ├─► custom-exporter (172.30.0.8) - RabbitMQ连接
    └─► monitoring-api (172.30.0.9) - RabbitMQ连接

Prometheus (172.30.0.5)
    ↓
    ├─► AlertManager (172.30.0.6) - 告警发送
    └─► Grafana (172.30.0.7) - 数据源

Grafana (172.30.0.7)
    ↓
    └─► Prometheus (172.30.0.5) - 查询数据

custom-exporter (172.30.0.8)
    ↓
    └─► Prometheus (172.30.0.5) - 指标上报

monitoring-api (172.30.0.9)
    ↓
    ├─► PostgreSQL (172.30.0.2) - 数据库操作
    ├─► Redis (172.30.0.3) - 缓存操作
    ├─► RabbitMQ (172.30.0.4) - 消息操作
    └─► Prometheus (172.30.0.5) - 指标上报

Nginx (172.30.0.10)
    ↓
    ├─► Grafana (172.30.0.7) - 反向代理
    ├─► monitoring-api (172.30.0.9) - 反向代理
    ├─► Prometheus (172.30.0.5) - 反向代理
    └─► AlertManager (172.30.0.6) - 反向代理
```

---

## 端口映射

### 端口映射表

| 容器服务 | 容器端口 | 主机端口 | 协议 | 说明 |
|---------|---------|---------|------|------|
| PostgreSQL | 5432 | 5433 | TCP | PostgreSQL数据库 |
| Redis | 6379 | 6380 | TCP | Redis缓存 |
| RabbitMQ | 5672 | 5673 | TCP | AMQP协议 |
| RabbitMQ | 15672 | 15673 | TCP | 管理界面 |
| Prometheus | 9090 | 9090 | TCP | Prometheus服务 |
| AlertManager | 9093 | 9093 | TCP | AlertManager服务 |
| Grafana | 3000 | 3000 | TCP | Grafana服务 |
| custom-exporter | 9101 | 9101 | TCP | 指标导出 |
| monitoring-api | 8080 | 8081 | TCP | API服务 |
| Nginx | 80 | 80 | TCP | HTTP服务 |
| Nginx | 443 | 443 | TCP | HTTPS服务 |

### 端口映射图

```
外部网络
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Docker Host (192.168.1.100)                       │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │              Docker Network Bridge                          │    │
│  │                  monitoring-bridge                          │    │
│  │              172.30.0.0/24 (内部网络)                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │       │       │       │       │       │       │          │
│         ▼       ▼       ▼       ▼       ▼       ▼       ▼          │
│    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐│
│    │5433:   │ │6380:   │ │5673:   │ │9090:   │ │3000:   │ │80:     ││
│    │5432    │ │6379    │ │5672    │ │9090    │ │3000    │ │80      ││
│    │PostgreSQL│ │Redis │ │RabbitMQ│ │Prometheus│ │Grafana│ │Nginx ││
│    └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘│
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
         │       │       │       │       │       │       │
         ▼       ▼       ▼       ▼       ▼       ▼       ▼
    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
    │外部访问 │ │外部访问 │ │外部访问 │ │外部访问 │ │外部访问 │ │外部访问 │
    │PostgreSQL│ │Redis  │ │RabbitMQ│ │Prometheus│ │Grafana│ │Nginx  │
    └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘
```

### 端口使用说明

#### 数据库端口

- **PostgreSQL**: `5433:5432`
  - 主机访问: `localhost:5433`
  - 容器访问: `postgres-monitoring:5432`

#### 缓存端口

- **Redis**: `6380:6379`
  - 主机访问: `localhost:6380`
  - 容器访问: `redis-monitoring:6379`

#### 消息队列端口

- **RabbitMQ AMQP**: `5673:5672`
  - 主机访问: `localhost:5673`
  - 容器访问: `rabbitmq-monitoring:5672`
  
- **RabbitMQ Management**: `15673:15672`
  - 主机访问: `localhost:15673`
  - 容器访问: `rabbitmq-monitoring:15672`

#### 监控端口

- **Prometheus**: `9090:9090`
  - 主机访问: `localhost:9090`
  - 容器访问: `prometheus-monitoring:9090`
  
- **AlertManager**: `9093:9093`
  - 主机访问: `localhost:9093`
  - 容器访问: `alertmanager-monitoring:9093`
  
- **Grafana**: `3000:3000`
  - 主机访问: `localhost:3000`
  - 容器访问: `grafana-monitoring:3000`

#### 应用端口

- **custom-exporter**: `9101:9101`
  - 主机访问: `localhost:9101`
  - 容器访问: `custom-exporter-monitoring:9101`
  
- **monitoring-api**: `8081:8080`
  - 主机访问: `localhost:8081`
  - 容器访问: `monitoring-api-service:8080`

#### 网关端口

- **Nginx HTTP**: `80:80`
  - 主机访问: `localhost:80`
  - 容器访问: `nginx-monitoring:80`
  
- **Nginx HTTPS**: `443:443`
  - 主机访问: `localhost:443`
  - 容器访问: `nginx-monitoring:443`

---

## 网络隔离

### 网络隔离策略

| 隔离级别 | 描述 | 实现方式 |
|---------|------|---------|
| 隔离外部 | 只允许特定端口访问 | Nginx反向代理 |
| 隔离内部 | 容器间通信使用私有网络 | Docker Bridge Network |
| 隔离服务 | 服务只监听特定接口 | 服务配置绑定IP |
| 隔离数据 | 数据库不暴露到外部 | 无端口映射 |

### 隔离配置

```yaml
# PostgreSQL - 不暴露到外部
services:
  postgres-monitoring:
    ports:
      - "5433:5432"  # 仅数据库管理端口暴露
    networks:
      - monitoring-network
    # 没有其他端口映射

# Redis - 不暴露到外部
services:
  redis-monitoring:
    command: redis-server --requirepass redis_monitoring_pass_123
    # 没有ports配置，只通过内部网络访问
```

### 安全组配置

```
┌─────────────────────────────────────────────────────────────────────┐
│                   防火墙安全组配置                                 │
│                                                                       │
│  外部网络                                                         │
│      │                                                              │
│      ▼                                                              │
│  ┌────────────────────┐                                             │
│  │      Nginx         │  (80, 443) - 允许外部访问                   │
│  │   - Reverse Proxy  │                                             │
│  │   - SSL Termination│                                             │
│  └────────────────────┘                                             │
│      │                                                              │
│      ├─► Grafana (3000)   - 内部网络                               │
│  ┌───┴─────────────────────────────────┐                            │
│  │       Docker Network                │                            │
│  │       172.30.0.0/24                 │                            │
│  └─────────────────────────────────────┘                            │
│      │                                                              │
│      ├─► PostgreSQL (5432) - 内部网络 (无外部访问)                 │
│  ┌───┴─────────────────────────────────┐                            │
│  │       容器内部网络                   │                            │
│  │       安全访问控制                   │                            │
│  └─────────────────────────────────────┘                            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## DNS配置

### Docker DNS服务

Docker内置DNS服务，容器可以通过服务名访问：

```bash
# Prometheus访问PostgreSQL
postgres-monitoring:5432

# Grafana访问Prometheus
prometheus-monitoring:9090

# custom-exporter访问Redis
redis-monitoring:6379
```

### DNS解析配置

```yaml
# Docker Compose自定义DNS
services:
  monitoring-api:
    dns:
      - 172.30.0.1
    dns_search:
      - monitoring-network
```

### DNS解析测试

```bash
# 测试容器间DNS解析
docker-compose exec prometheus nslookup postgres-monitoring
docker-compose exec grafana nslookup prometheus-monitoring
docker-compose exec custom-exporter nslookup redis-monitoring

# 测试服务发现
docker-compose exec monitoring-api dig postgres-monitoring
docker-compose exec monitoring-api dig redis-monitoring
```

---

## 网络诊断

### 网络诊断命令

#### 检查网络连接

```bash
# 检查Docker网络
docker network inspect monitoring-network

# 检查容器网络
docker-compose exec postgres-monitoring ip a
docker-compose exec redis-monitoring ip a

# 检查网络接口
docker-compose exec prometheus ip link show

# 检查路由表
docker-compose exec postgres-monitoring ip route
```

#### 测试网络连通性

```bash
# 测试PostgreSQL到Prometheus
docker-compose exec postgres-monitoring nc -zv prometheus-monitoring 9090

# 测试Redis到custom-exporter
docker-compose exec redis-monitoring nc -zv custom-exporter-monitoring 9101

# 测试Grafana到Prometheus
docker-compose exec grafana nc -zv prometheus-monitoring 9090

# 测试Nginx到所有服务
docker-compose exec nginx nc -zv postgres-monitoring 5432
docker-compose exec nginx nc -zv redis-monitoring 6379
docker-compose exec nginx nc -zv rabbitmq-monitoring 5672
docker-compose exec nginx nc -zv prometheus-monitoring 9090
docker-compose exec nginx nc -zv grafana-monitoring 3000
docker-compose exec nginx nc -zv custom-exporter-monitoring 9101
docker-compose exec nginx nc -zv monitoring-api-service 8080
docker-compose exec nginx nc -zv alertmanager-monitoring 9093
```

#### 测试端口监听

```bash
# 测试PostgreSQL端口监听
docker-compose exec postgres-monitoring netstat -tlnp | grep 5432
docker-compose exec postgres-monitoring ss -tlnp | grep 5432

# 测试Redis端口监听
docker-compose exec redis-monitoring netstat -tlnp | grep 6379
docker-compose exec redis-monitoring ss -tlnp | grep 6379

# 测试Prometheus端口监听
docker-compose exec prometheus netstat -tlnp | grep 9090
docker-compose exec prometheus ss -tlnp | grep 9090
```

#### 网络延迟测试

```bash
# 测试PostgreSQL到Prometheus延迟
docker-compose exec prometheus bash -c 'time echo "SELECT 1" | psql -h postgres-monitoring -U monitoring_user -d monitoring_db'

# 测试Redis延迟
docker-compose exec redis-monitoring bash -c 'time redis-cli ping'

# 测试RabbitMQ延迟
docker-compose exec rabbitmq-monitoring bash -c 'time rabbitmq-diagnostics -q ping'
```

### 网络诊断脚本

创建网络诊断脚本 `network-diagnose.sh`:

```bash
#!/bin/bash

echo "========================================"
echo "Docker网络诊断工具"
echo "========================================"
echo ""

# 检查Docker网络
echo "1. 检查Docker网络..."
docker network inspect monitoring-network > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ Docker网络 monitoring-network 存在"
else
    echo "   ✗ Docker网络 monitoring-network 不存在"
    exit 1
fi

# 检查容器网络
echo ""
echo "2. 检查容器网络接口..."
docker-compose exec postgres-monitoring ip a > /dev/null 2>&1
echo "   ✓ PostgreSQL 网络正常"

docker-compose exec redis-monitoring ip a > /dev/null 2>&1
echo "   ✓ Redis 网络正常"

docker-compose exec prometheus ip a > /dev/null 2>&1
echo "   ✓ Prometheus 网络正常"

# 检查容器间连通性
echo ""
echo "3. 检查容器间连通性..."

docker-compose exec prometheus nc -zv postgres-monitoring 5432 > /dev/null 2>&1
echo "   ✓ Prometheus → PostgreSQL"

docker-compose exec prometheus nc -zv redis-monitoring 6379 > /dev/null 2>&1
echo "   ✓ Prometheus → Redis"

docker-compose exec prometheus nc -zv rabbitmq-monitoring 5672 > /dev/null 2>&1
echo "   ✓ Prometheus → RabbitMQ"

docker-compose exec grafana nc -zv prometheus-monitoring 9090 > /dev/null 2>&1
echo "   ✓ Grafana → Prometheus"

# 检查DNS解析
echo ""
echo "4. 检查DNS解析..."

docker-compose exec prometheus bash -c 'getent hosts postgres-monitoring' > /dev/null 2>&1
echo "   ✓ PostgreSQL DNS解析正常"

docker-compose exec grafana bash -c 'getent hosts prometheus-monitoring' > /dev/null 2>&1
echo "   ✓ Prometheus DNS解析正常"

# 检查端口监听
echo ""
echo "5. 检查端口监听..."

docker-compose exec postgres-monitoring netstat -tlnp | grep 5432 > /dev/null 2>&1
echo "   ✓ PostgreSQL 5432 端口监听"

docker-compose exec redis-monitoring netstat -tlnp | grep 6379 > /dev/null 2>&1
echo "   ✓ Redis 6379 端口监听"

docker-compose exec prometheus netstat -tlnp | grep 9090 > /dev/null 2>&1
echo "   ✓ Prometheus 9090 端口监听"

echo ""
echo "========================================"
echo "网络诊断完成"
echo "========================================"
```

---

## 网络故障排查

### 常见网络问题

#### 问题1: 容器无法访问其他容器

**症状**:连接超时

**排查步骤**:
1. 检查容器状态: `docker-compose ps`
2. 检查网络: `docker network inspect monitoring-network`
3. 测试连通性: `docker-compose exec <src> nc -zv <dst> <port>`
4. 检查防火墙: `systemctl status firewalld`

#### 问题2: 容器无法访问外部网络

**症状**:无法拉取镜像

**排查步骤**:
1. 检查Docker DNS: `docker info | grep DNS`
2. 测试DNS解析: `docker-compose exec <container> nslookup google.com`
3. 检查代理设置: `docker-compose exec <container> env | grep -i proxy`

#### 问题3: 端口映射失败

**症状**:容器无法从外部访问

**排查步骤**:
1. 检查端口占用: `netstat -tlnp | grep <port>`
2. 检查容器端口: `docker-compose exec <container> netstat -tlnp`
3. 检查防火墙: `firewall-cmd --list-all`

### 网络配置优化

#### 优化1: 网络性能调优

```yaml
# Docker Compose网络配置优化
networks:
  monitoring-network:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.name: monitoring-bridge
      com.docker.network.bridge.enable_iptables: "true"
      com.docker.network.bridge.enable_ip_masquerade: "true"
    ipam:
      driver: default
      config:
        - subnet: 172.30.0.0/24
          gateway: 172.30.0.1
```

#### 优化2: DNS优化

```yaml
# 容器DNS优化
services:
  monitoring-api:
    dns:
      - 172.30.0.1
      - 8.8.8.8
    dns_search:
      - monitoring-network
```

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker Network Documentation](https://docs.docker.com/network/)
- [Docker Bridge Network](https://docs.docker.com/network/bridge/)
- [Docker DNS Service](https://docs.docker.com/config/containers/container-networking/#dns-services)
- [Network Troubleshooting](https://docs.docker.com/network/network-tutorial-host/)