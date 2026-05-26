# 监控告警模块部署手册

## 概述

本文档提供AI-Ready项目监控告警模块的生产环境部署指南。

## 模块清单

1. **Prometheus** - 监控数据收集和存储
   - 端口: 9090
   - 功能: 指标收集、存储、查询

2. **AlertManager** - 告警管理
   - 端口: 9093
   - 功能: 告警路由、抑制、通知

3. **Grafana** - 监控可视化
   - 端口: 3000
   - 功能: 仪表板、告警面板、数据可视化

4. **自定义导出器** - 业务指标监控
   - 端口: 9101
   - 功能: 业务指标收集、自定义指标导出

5. **监控API服务** - 监控数据接口
   - 端口: 8081
   - 功能: 监控数据查询、告警管理API

6. **Node Exporter** - 主机指标监控
   - 端口: 9100
   - 功能: 系统资源监控（CPU、内存、磁盘、网络）

7. **Nginx** - 负载均衡和反向代理
   - 端口: 80/443
   - 功能: 服务代理、SSL终止、负载均衡

## 部署架构

```
┌─────────────────────────────────────────────────┐
│                  生产环境                         │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ PostgreSQL│  │  Redis   │  │ RabbitMQ │      │
│  │  :5433   │  │  :6380   │  │  :5673   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Prometheus│  │AlertMgr  │  │ Grafana  │      │
│  │  :9090   │  │  :9093   │  │  :3000   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Custom    │  │Monitoring│  │Node      │      │
│  │Exporter  │  │API       │  │Exporter  │      │
│  │  :9101   │  │  :8081   │  │  :9100   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  ┌──────────┐                                   │
│  │  Nginx   │                                   │
│  │ :80/:443 │                                   │
│  └──────────┘                                   │
└─────────────────────────────────────────────────┘
```

## 部署前准备

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- Maven 3.9+ (可选，用于构建Java应用)
- Java 17+

### 2. 配置文件准备

创建生产环境配置文件 `.env.production`:

```bash
# 数据库配置
DB_USER=monitoring_user
DB_PASSWORD=<安全密码>
DB_NAME=monitoring_db

# Redis配置
REDIS_PASSWORD=<安全密码>

# RabbitMQ配置
RABBITMQ_USER=monitoring_rabbit
RABBITMQ_PASSWORD=<安全密码>
RABBITMQ_VHOST=monitoring_vhost

# Grafana配置
GRAFANA_PASSWORD=<安全密码>

# JVM配置
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# 保留时间配置
RETENTION_TIME=30d
RETENTION_SIZE=10GB
```

### 3. SSL证书准备（可选）

如果需要HTTPS访问，准备SSL证书：

```bash
# 创建SSL目录
mkdir -p nginx/ssl

# 复制证书文件
cp your-cert.crt nginx/ssl/
cp your-key.key nginx/ssl/
```

## 部署步骤

### 方式一：使用自动化脚本

```bash
# 进入部署目录
cd I:\AI-Ready\deploy\monitoring-alerts

# 执行部署脚本
bash deploy-production.sh
```

### 方式二：手动部署

#### 1. 构建模块（可选）

```bash
# 构建监控模块
cd I:\AI-Ready\backend\monitoring
mvn clean package -DskipTests -Pprod
```

#### 2. 构建Docker镜像

```bash
cd I:\AI-Ready\deploy\monitoring-alerts

# 构建自定义导出器
docker build -t ai-ready/custom-exporter:latest \
  -f ../backend/monitoring/src/main/docker/custom-exporter/Dockerfile \
  ../backend/monitoring

# 构建监控API服务
docker build -t ai-ready/monitoring-api:latest \
  -f ../backend/monitoring/src/main/docker/custom-exporter/Dockerfile \
  ../backend/monitoring
```

#### 3. 启动服务

```bash
# 使用生产环境配置
docker-compose --env-file .env.production up -d
```

#### 4. 验证部署

```bash
# 检查容器状态
docker-compose --env-file .env.production ps

# 测试服务健康检查
curl http://localhost:9090/-/healthy
curl http://localhost:9093/-/healthy
curl http://localhost:3000/api/health
curl http://localhost:8081/actuator/health
curl http://localhost:9101/health
curl http://localhost:9100/metrics
```

## 部署验证

### 1. 容器状态检查

```bash
docker-compose --env-file .env.production ps
```

所有容器应该处于 `running` 状态。

### 2. 健康检查

```bash
# Prometheus
curl http://localhost:9090/-/healthy

# AlertManager
curl http://localhost:9093/-/healthy

# Grafana
curl http://localhost:3000/api/health

# 监控API
curl http://localhost:8081/actuator/health

# 自定义导出器
curl http://localhost:9101/health

# Node Exporter
curl http://localhost:9100/metrics
```

应该返回相应的健康状态。

### 3. 数据库连接检查

```bash
# 连接PostgreSQL
docker exec -it ai-ready-postgres-monitoring psql -U monitoring_user -d monitoring_db

# 检查表
\dt

# 检查连接数
SELECT count(*) FROM pg_stat_activity;
```

### 4. Redis连接检查

```bash
# 连接Redis
docker exec -it ai-ready-redis-monitoring redis-cli -a <密码>

# 检查信息
INFO

# 检查连接数
CLIENT LIST
```

## 监控配置

### Prometheus访问

```
URL: http://localhost:9090
```

### Grafana访问

```
URL: http://localhost:3000
用户: admin
密码: <配置的密码>
```

### AlertManager访问

```
URL: http://localhost:9093
```

### 监控指标

- CPU使用率
- 内存使用率
- 磁盘使用率
- 网络流量
- JVM堆内存
- 响应时间
- 错误率
- 数据库连接池
- Redis连接状态
- 业务指标（自定义导出器提供）

## 常见问题

### 1. 容器启动失败

检查日志:
```bash
docker logs <container_name>
```

### 2. 数据库连接失败

检查:
- PostgreSQL容器是否运行
- 数据库配置是否正确
- 网络连接是否正常

### 3. Redis连接失败

检查:
- Redis容器是否运行
- Redis密码配置是否正确
- 网络连接是否正常

### 4. 监控数据缺失

检查:
- Prometheus配置是否正确
- 服务是否启用 `/metrics` 端点
- 网络连接是否正常

## 回滚方案

```bash
# 停止当前服务
docker-compose down

# 使用备份镜像恢复
docker load -i backups/<timestamp>/custom-exporter-latest.tar
docker load -i backups/<timestamp>/monitoring-api-latest.tar

# 启动服务
docker-compose --env-file .env.production up -d
```

## 附录

### 端口映射

| 服务 | 容器端口 | 主机端口 |
|------|---------|---------|
| PostgreSQL | 5432 | 5433 |
| Redis | 6379 | 6380 |
| RabbitMQ | 5672 | 5673 |
| Prometheus | 9090 | 9090 |
| AlertManager | 9093 | 9093 |
| Grafana | 3000 | 3000 |
| Custom Exporter | 9101 | 9101 |
| Monitoring API | 8080 | 8081 |
| Node Exporter | 9100 | 9100 |
| Nginx HTTP | 80 | 80 |
| Nginx HTTPS | 443 | 443 |

### 日志路径

- 容器日志: `docker logs <container_name>`
- 应用日志: `logs/monitoring-api/`

### 配置路径

- Docker Compose: `I:\AI-Ready\deploy\monitoring-alerts\docker-compose.yml`
- 环境配置: `I:\AI-Ready\deploy\monitoring-alerts\.env.production`
- Prometheus配置: `I:\AI-Ready\deploy\monitoring-alerts\prometheus\`
- AlertManager配置: `I:\AI-Ready\deploy\monitoring-alerts\alertmanager\`
- Grafana配置: `I:\AI-Ready\deploy\monitoring-alerts\grafana\`