# Docker Compose部署指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: devops-engineer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [部署前准备](#部署前准备)
3. [快速部署](#快速部署)
4. [详细部署步骤](#详细部署步骤)
5. [配置说明](#配置说明)
6. [部署验证](#部署验证)
7. [故障排查](#故障排查)

---

## 概述

### 部署方式

测试环境提供两种部署方式：

#### 一键部署（推荐）

使用Docker Compose一键启动所有服务：

```bash
docker-compose -f monitoring-alerting-deployment.yml up -d
```

#### 分步部署

逐步启动各服务，便于问题排查：

```bash
# 第一步：基础设施服务
docker-compose up -d postgres-monitoring redis-monitoring rabbitmq-monitoring

# 第二步：监控服务
docker-compose up -d prometheus alertmanager grafana

# 第三步：应用服务
docker-compose up -d custom-exporter monitoring-api nginx
```

### 部署特点

- **自动化**: 一键启动所有服务
- **可重复**: 配置文件可版本控制
- **易维护**: 使用Docker Compose管理
- **高可用**: 支持服务重启和健康检查

---

## 部署前准备

### 系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| 操作系统 | Ubuntu 20.04+ / CentOS 7+ / Windows 10 | Ubuntu 22.04 |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |
| 内存 | 8GB | 16GB |
| 磁盘 | 50GB | 100GB |

### 软件安装

#### Docker安装

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# CentOS/RHEL
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker

# Windows
# 下载docker-desktop安装程序
# 安装并启动Docker Desktop
```

#### Docker Compose安装

```bash
# Linux
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

### 环境检查

```bash
# 检查Docker版本
docker --version

# 检查Docker Compose版本
docker-compose --version

# 检查Docker状态
docker info

# 检查磁盘空间
df -h

# 检查内存
free -h
```

### 目录结构准备

```bash
# 创建项目目录
mkdir -p I:\AI-Ready\deploy
cd I:\AI-Ready\deploy

# 创建数据卷目录
mkdir -p volumes/{postgres,redis,rabbitmq,prometheus,grafana,alertmanager}

# 创建配置目录
mkdir -p init-scripts
mkdir -p prometheus/{rules,file_sd}
mkdir -p alertmanager/templates
mkdir -p grafana/{provisioning,dashboards}
mkdir -p nginx/{conf.d,ssl,html}
mkdir -p custom-exporter/config
mkdir -p logs/monitoring-api
```

### 配置文件检查

确保以下配置文件存在：

```
deploy/
├── monitoring-alerting-deployment.yml  # 主配置文件
├── volumes/                             # 数据卷目录
│   ├── postgres/
│   ├── redis/
│   ├── rabbitmq/
│   ├── prometheus/
│   ├── grafana/
│   └── alertmanager/
├── init-scripts/
│   └── postgres-init.sql               # PostgreSQL初始化脚本
├── prometheus/
│   ├── prometheus.yml                  # Prometheus配置
│   ├── rules/                          # 告警规则
│   └── file_sd/                        # 文件服务发现
├── alertmanager/
│   ├── alertmanager.yml                # AlertManager配置
│   └── templates/                      # 告警模板
├── grafana/
│   ├── provisioning/                   # 配置供应
│   └── dashboards/                     # 仪表盘
├── nginx/                              # Nginx配置
│   ├── nginx.conf
│   ├── conf.d/
│   ├── ssl/
│   └── html/
├── custom-exporter/                    # 自定义导出器
│   ├── Dockerfile
│   └── config/
└── logs/monitoring-api/                # API服务日志
```

---

## 快速部署

### 一键部署

```bash
# 进入部署目录
cd I:\AI-Ready\deploy

# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 查看服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 分步部署

```bash
# 第一步：基础设施服务（0s）
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring rabbitmq-monitoring

# 第二步：等待基础设施服务就绪（30s）
sleep 30

# 第三步：监控服务（启动后等待健康检查）
docker-compose -f monitoring-alerting-deployment.yml up -d prometheus alertmanager grafana

# 第四步：等待监控服务就绪（60s）
sleep 60

# 第五步：应用服务
docker-compose -f monitoring-alerting-deployment.yml up -d custom-exporter monitoring-api nginx

# 第六步：等待所有服务就绪（30s）
sleep 30

# 第七步：检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 验证部署

```bash
# 检查所有容器状态
docker-compose -f monitoring-alerting-deployment.yml ps

# 检查健康状态
docker-compose -f monitoring-alerting-deployment.yml ps --format "table {{.Name}}\t{{.Status}}"
```

---

## 详细部署步骤

### 步骤1: 下载配置文件

```bash
# 创建部署目录
mkdir -p I:\AI-Ready\deploy
cd I:\AI-Ready\deploy

# 配置文件已在前面创建
# 确认文件结构正确
ls -la
```

### 步骤2: 初始化数据卷

```bash
# 创建数据卷
docker-compose -f monitoring-alerting-deployment.yml volume create prometheus-data
docker-compose -f monitoring-alerting-deployment.yml volume create grafana-data
docker-compose -f monitoring-alerting-deployment.yml volume create alertmanager-data
docker-compose -f monitoring-alerting-deployment.yml volume create postgres-data
docker-compose -f monitoring-alerting-deployment.yml volume create redis-data
docker-compose -f monitoring-alerting-deployment.yml volume create rabbitmq-data

# 或使用目录（推荐）
mkdir -p volumes/{postgres,redis,rabbitmq,prometheus,grafana,alertmanager}
```

### 步骤3: 启动基础设施服务

```bash
# 启动PostgreSQL、Redis、RabbitMQ
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring rabbitmq-monitoring

# 等待服务就绪
sleep 30

# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 步骤4: 启动监控服务

```bash
# 启动Prometheus、AlertManager、Grafana
docker-compose -f monitoring-alerting-deployment.yml up -d prometheus alertmanager grafana

# 等待健康检查
sleep 60

# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 步骤5: 启动应用服务

```bash
# 启动custom-exporter、monitoring-api、Nginx
docker-compose -f monitoring-alerting-deployment.yml up -d custom-exporter monitoring-api nginx

# 等待所有服务就绪
sleep 30

# 检查服务状态
docker-compose -f monitoring-alerting-deployment.yml ps
```

### 步骤6: 验证部署

```bash
# 检查所有服务状态
docker-compose -f monitoring-alerting-deployment.yml ps

# 检查健康状态
docker inspect --format='{{.State.Health.Status}}' postgres-monitoring
docker inspect --format='{{.State.Health.Status}}' redis-monitoring
docker inspect --format='{{.State.Health.Status}}' prometheus-monitoring
```

---

## 配置说明

### Docker Compose配置说明

#### 服务定义

```yaml
services:
  # PostgreSQL
  postgres-monitoring:
    image: postgres:15-alpine
    container_name: postgres-monitoring
    restart: unless-stopped
    environment:
      POSTGRES_DB: monitoring_db
      POSTGRES_USER: monitoring_user
      POSTGRES_PASSWORD: monitoring_pass_123
    ports:
      - "5433:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - monitoring-network
```

#### 网络定义

```yaml
networks:
  monitoring-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.0.0/24
          gateway: 172.30.0.1
```

#### 卷定义

```yaml
volumes:
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
```

### 关键配置项

#### 资源限制

```yaml
# PostgreSQL配置
command: >
  postgres
  -c max_connections=200
  -c shared_buffers=256MB
  -c effective_cache_size=1GB
  -c maintenance_work_mem=64MB
  -c checkpoint_completion_target=0.9
  -c wal_buffers=16MB
  -c default_statistics_target=100
```

#### Prometheus配置

```yaml
command:
  - '--config.file=/etc/prometheus/prometheus.yml'
  - '--storage.tsdb.path=/prometheus'
  - '--storage.tsdb.retention.time=30d'
  - '--storage.tsdb.retention.size=10GB'
```

#### Grafana配置

```yaml
environment:
  GF_SECURITY_ADMIN_PASSWORD: admin123
  GF_INSTALL_PLUGINS: grafana-piechart-panel,grafana-clock-panel
```

### 环境变量配置

```bash
# PostgreSQL
POSTGRES_DB=monitoring_db
POSTGRES_USER=monitoring_user
POSTGRES_PASSWORD=monitoring_pass_123

# Redis
redis-server --requirepass redis_monitoring_pass_123 --maxmemory 256mb

# RabbitMQ
RABBITMQ_DEFAULT_USER=monitoring_rabbit
RABBITMQ_DEFAULT_PASS=rabbit_monitoring_pass_123
RABBITMQ_DEFAULT_VHOST=monitoring_vhost
```

---

## 部署验证

### 部署验证步骤

#### 步骤1: 检查容器状态

```bash
docker-compose -f monitoring-alerting-deployment.yml ps
```

期望输出:
```
NAME                            STATUS         PORTS
postgres-monitoring             running        0.0.0.0:5433->5432/tcp
redis-monitoring                running        0.0.0.0:6380->6379/tcp
rabbitmq-monitoring             running        0.0.0.0:5673->5672/tcp, 0.0.0.0:15673->15672/tcp
prometheus-monitoring           running        0.0.0.0:9090->9090/tcp
alertmanager-monitoring         running        0.0.0.0:9093->9093/tcp
grafana-monitoring              running        0.0.0.0:3000->3000/tcp
custom-exporter-monitoring      running        0.0.0.0:9101->9101/tcp
monitoring-api-service          running        0.0.0.0:8081->8080/tcp
nginx-monitoring                running        0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
```

#### 步骤2: 检查健康状态

```bash
# pg_isready - PostgreSQL
docker-compose exec postgres-monitoring pg_isready -U monitoring_user -d monitoring_db

# redis-cli - Redis
docker-compose exec redis-monitoring redis-cli --raw incr ping

# rabbitmq-diagnostics - RabbitMQ
docker-compose exec rabbitmq-monitoring rabbitmq-diagnostics -q ping

# wget - Prometheus
docker-compose exec prometheus wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy

# wget - AlertManager
docker-compose exec alertmanager wget --no-verbose --tries=1 --spider http://localhost:9093/-/healthy

# wget - Grafana
docker-compose exec grafana wget --no-verbose --tries=1 --spider http://localhost:3000/api/health

# curl - custom-exporter
docker-compose exec custom-exporter curl -f http://localhost:9101/health

# curl - monitoring-api
docker-compose exec monitoring-api curl -f http://localhost:8080/actuator/health

# nginx - Nginx
docker-compose exec nginx nginx -t
```

#### 步骤3: 检查网络连接

```bash
# 测试PostgreSQL连接
docker-compose exec prometheus nc -zv postgres-monitoring 5432

# 测试Redis连接
docker-compose exec prometheus nc -zv redis-monitoring 6379

# 测试RabbitMQ连接
docker-compose exec prometheus nc -zv rabbitmq-monitoring 5672

# 测试Prometheus连接
docker-compose exec grafana nc -zv prometheus-monitoring 9090
```

#### 步骤4: 检查服务功能

```bash
# PostgreSQL - 创建测试表
docker-compose exec postgres-monitoring psql -U monitoring_user -d monitoring_db -c "CREATE TABLE test (id INT); DROP TABLE test;"

# Redis - 设置测试键
docker-compose exec redis-monitoring redis-cli -a redis_monitoring_pass_123 SET test_key "test_value"

# RabbitMQ - 列出队列
docker-compose exec rabbitmq-monitoring rabbitmqctl list_queues

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
docker-compose exec nginx nginx -t
```

### 部署验证清单

| 服务 | 健康检查 | 端口监听 | 网络连接 | 数据库 | 服务功能 | 总体状态 |
|------|---------|---------|---------|--------|---------|---------|
| PostgreSQL | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Redis | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| RabbitMQ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Prometheus | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| AlertManager | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Grafana | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| custom-exporter | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| monitoring-api | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Nginx | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 服务管理

### 启动服务

```bash
# 启动所有服务
docker-compose -f monitoring-alerting-deployment.yml up -d

# 启动特定服务
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring

# 启动多个服务
docker-compose -f monitoring-alerting-deployment.yml up -d postgres-monitoring redis-monitoring
```

### 停止服务

```bash
# 停止所有服务
docker-compose -f monitoring-alerting-deployment.yml down

# 停止并删除数据卷
docker-compose -f monitoring-alerting-deployment.yml down -v

# 停止特定服务
docker-compose -f monitoring-alerting-deployment.yml stop postgres-monitoring
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f monitoring-alerting-deployment.yml restart

# 重启特定服务
docker-compose -f monitoring-alerting-deployment.yml restart postgres-monitoring

# 强制重建服务
docker-compose -f monitoring-alerting-deployment.yml up -d --force-recreate
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f monitoring-alerting-deployment.yml logs -f

# 查看特定服务日志
docker-compose -f monitoring-alerting-deployment.yml logs -f postgres-monitoring

# 实时跟踪日志
docker-compose -f monitoring-alerting-deployment.yml logs -f --tail=100
```

### 进入容器

```bash
# 进入PostgreSQL容器
docker-compose exec postgres-monitoring psql -U monitoring_user -d monitoring_db

# 进入Redis容器
docker-compose exec redis-monitoring redis-cli -a redis_monitoring_pass_123

# 进入RabbitMQ容器
docker-compose exec rabbitmq-monitoring rabbitmq-diagnostics -q ping

# 进入Prometheus容器
docker-compose exec prometheus sh

# 进入Grafana容器
docker-compose exec grafana sh
```

---

## 故障排查

### 常见部署问题

#### 问题1: 服务无法启动

**症状**: 容器处于Created或Restarting状态

**排查步骤**:

```bash
# 查看容器日志
docker-compose logs <service>

# 检查端口占用
netstat -tlnp | grep <port>

# 检查配置
docker-compose config

# 强制重建
docker-compose up -d --force-recreate
```

#### 问题2: 健康检查失败

**症状**: 容器状态为unhealthy

**排查步骤**:

```bash
# 查看健康检查日志
docker inspect --format='{{.State.Health.Log}}' <container>

# 手动测试健康检查
docker-compose exec <service> <healthcheck_command>

# 检查依赖服务
docker-compose ps
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
# 编辑 monitoring-alerting-deployment.yml
# 将 "5433:5432" 改为 "5434:5432"
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
docker-compose down
docker-compose up -d
```

### 快速诊断脚本

创建诊断脚本 `deploy-verify.sh`:

```bash
#!/bin/bash

echo "========================================"
echo "Docker Compose部署验证工具"
echo "========================================"
echo ""

cd I:\AI-Ready\deploy

echo "1. 检查Docker状态..."
docker info > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ Docker服务正常"
else
    echo "   ✗ Docker服务异常"
    exit 1
fi

echo ""
echo "2. 检查Docker Compose..."
docker-compose -f monitoring-alerting-deployment.yml config > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ 配置文件正确"
else
    echo "   ✗ 配置文件错误"
    exit 1
fi

echo ""
echo "3. 检查服务状态..."
docker-compose -f monitoring-alerting-deployment.yml ps
if [ $? -eq 0 ]; then
    echo "   ✓ 服务状态检查通过"
else
    echo "   ✗ 服务状态检查失败"
fi

echo ""
echo "4. 检查健康状态..."
docker-compose -f monitoring-alerting-deployment.yml ps --format "table {{.Name}}\t{{.Status}}"

echo ""
echo "5. 测试网络连接..."
docker-compose exec prometheus nc -zv postgres-monitoring 5432 > /dev/null 2>&1
echo "   ✓ Prometheus → PostgreSQL"

docker-compose exec grafana nc -zv prometheus-monitoring 9090 > /dev/null 2>&1
echo "   ✓ Grafana → Prometheus"

echo ""
echo "6. 测试服务功能..."
curl -s http://localhost:3000/api/health > /dev/null
echo "   ✓ Grafana API正常"

echo ""
echo "========================================"
echo "部署验证完成"
echo "========================================"
```

---

## 部署脚本

### 自动化部署脚本

创建部署脚本 `deploy.sh`:

```bash
#!/bin/bash

echo "========================================"
echo "测试环境自动化部署脚本"
echo "========================================"
echo ""

cd I:\AI-Ready\deploy

# 检查配置文件
echo "1. 检查配置文件..."
if [ ! -f monitoring-alerting-deployment.yml ]; then
    echo "   错误: 找不到 monitoring-alerting-deployment.yml"
    exit 1
fi
echo "   ✓ 配置文件存在"

# 检查数据卷目录
echo ""
echo "2. 检查数据卷目录..."
for dir in postgres redis rabbitmq prometheus grafana alertmanager; do
    if [ ! -d "volumes/$dir" ]; then
        mkdir -p volumes/$dir
        echo "   创建目录: volumes/$dir"
    fi
done
echo "   ✓ 数据卷目录检查完成"

# 检查Docker
echo ""
echo "3. 检查Docker..."
docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "   错误: Docker服务未运行"
    exit 1
fi
echo "   ✓ Docker服务正常"

# 启动服务
echo ""
echo "4. 启动所有服务..."
docker-compose -f monitoring-alerting-deployment.yml up -d
if [ $? -ne 0 ]; then
    echo "   错误: 启动服务失败"
    exit 1
fi
echo "   ✓ 服务启动成功"

# 等待服务就绪
echo ""
echo "5. 等待服务就绪..."
sleep 120

# 检查服务状态
echo ""
echo "6. 检查服务状态..."
docker-compose -f monitoring-alerting-deployment.yml ps
if [ $? -eq 0 ]; then
    echo "   ✓ 所有服务正常运行"
else
    echo "   ✗ 部分服务异常"
    exit 1
fi

echo ""
echo "========================================"
echo "部署完成！"
echo "========================================"
echo ""
echo "访问地址："
echo "  - Grafana: http://localhost:3000"
echo "  - Prometheus: http://localhost:9090"
echo "  - AlertManager: http://localhost:9093"
echo "  - API服务: http://localhost:8081"
echo ""
echo "默认账号："
echo "  - Grafana: admin/admin123"
echo ""
```

### 自动化脚本使用

```bash
# 给脚本执行权限
chmod +x deploy.sh

# 执行部署
./deploy.sh
```

---

## 服务监控

### 服务监控指标

| 服务 | 健康检查端点 | 告警阈值 |
|------|-------------|---------|
| PostgreSQL | pg_isready | - |
| Redis | redis-cli ping | 内存 > 80% |
| RabbitMQ | rabbitmq-diagnostics ping | 队列积压 > 1000 |
| Prometheus | /-/healthy | 无 |
| AlertManager | /-/healthy | 无 |
| Grafana | /api/health | 无 |
| custom-exporter | /health | - |
| monitoring-api | /actuator/health | 无 |
| Nginx | nginx -t | 无 |

### 服务健康检查脚本

创建健康检查脚本 `healthcheck.sh`:

```bash
#!/bin/bash

cd I:\AI-Ready\deploy

echo "========================================"
echo "服务健康检查"
echo "========================================"

# PostgreSQL
echo "检查 PostgreSQL..."
docker-compose exec postgres-monitoring pg_isready -U monitoring_user -d monitoring_db > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ PostgreSQL 健康"
else
    echo "  ✗ PostgreSQL 不健康"
fi

# Redis
echo "检查 Redis..."
docker-compose exec redis-monitoring redis-cli --raw incr ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ Redis 健康"
else
    echo "  ✗ Redis 不健康"
fi

# Prometheus
echo "检查 Prometheus..."
docker-compose exec prometheus wget --no-verbose --tries=1 --spider http://localhost:9090/-/healthy > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ Prometheus 健康"
else
    echo "  ✗ Prometheus 不健康"
fi

# Grafana
echo "检查 Grafana..."
docker-compose exec grafana wget --no-verbose --tries=1 --spider http://localhost:3000/api/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ Grafana 健康"
else
    echo "  ✗ Grafana 不健康"
fi

echo "========================================"
```

---

## 部署最佳实践

### 1. 配置管理

- 使用版本控制管理Docker Compose配置
- 环境变量通过环境文件管理
- 敏感信息使用Docker secrets

### 2. 健康检查

- 所有服务配置健康检查
- 设置合理的start_period
- 定期运行健康检查脚本

### 3. 数据持久化

- 使用Docker卷管理数据
- 定期备份数据卷
- 配置数据恢复策略

### 4. 日志管理

- 配置日志驱动
- 设置日志轮转
- 集中收集日志

### 5. 监控告警

- Prometheus监控所有服务
- AlertManager告警通知
- Grafana可视化监控数据

---

## 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|-------|
| 1.0.0 | 2026-04-27 | 初始版本 | devops-engineer |

---

## 参考资料

- [Docker Compose官方文档](https://docs.docker.com/compose/)
- [Docker Compose参考](https://docs.docker.com/compose/compose-file/)
- [Docker Best Practices](https://docs.docker.com/develop/BestPractices/)