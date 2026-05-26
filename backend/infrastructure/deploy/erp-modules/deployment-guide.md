# ERP Modules 部署手册

## 概述

本文档提供AI-Ready项目5个P0 ERP核心模块的生产环境部署指南。

## 模块清单

1. **批次/序列号管理模块** (erp-batch-sn)
   - 端口: 8081
   - 功能: 批次追溯、序列号管理

2. **发票管理模块** (erp-invoice)
   - 端口: 8082
   - 功能: 发票申请、审批、生成

3. **采购询价/报价管理模块** (erp-purchase)
   - 端口: 8083
   - 功能: 询价、报价管理

4. **销售价格策略管理模块** (erp-sale)
   - 端口: 8084
   - 功能: 价格策略、价格计算

5. **供应商协同门户模块** (supplier-portal)
   - 端口: 8085
   - 功能: 供应商门户、订单协同

## 部署架构

```
┌─────────────────────────────────────────────────┐
│                  生产环境                         │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ PostgreSQL│  │  Redis   │  │ RabbitMQ │      │
│  │  :5432   │  │  :6379   │  │  :5672   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Batch/SN  │  │ Invoice  │  │Purchase  │      │
│  │  :8081   │  │  :8082   │  │Exchange  │      │
│  └──────────┘  └──────────┘  │  :8083   │      │
│                              └──────────┘      │
│  ┌──────────┐  ┌──────────┐                   │
│  │Sales     │  │Supplier  │                   │
│  │Exchange  │  │Portal    │                   │
│  │  :8084   │  │  :8085   │                   │
│  └──────────┘  └──────────┘                   │
│                                                 │
│  ┌──────────┐  ┌──────────┐                   │
│  │Prometheus│  │ Grafana  │                   │
│  │  :9090   │  │  :3000   │                   │
│  └──────────┘  └──────────┘                   │
└─────────────────────────────────────────────────┘
```

## 部署前准备

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- Maven 3.9+
- Java 17+
- PostgreSQL 15+
- Redis 7+

### 2. 配置文件准备

创建生产环境配置文件 `.env.production`:

```bash
# 数据库配置
DB_USER=erpadmin
DB_PASSWORD=<安全密码>
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ai_ready_erp

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<安全密码>

# Grafana配置
GRAFANA_USER=admin
GRAFANA_PASSWORD=<安全密码>

# JVM配置
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# Spring配置
SPRING_PROFILES_ACTIVE=prod
```

### 3. 数据库初始化

准备数据库初始化脚本 `init-scripts/`:

```sql
-- 创建数据库
CREATE DATABASE ai_ready_erp;

-- 创建用户
CREATE USER erpadmin WITH PASSWORD 'secure_password';

-- 授权
GRANT ALL PRIVILEGES ON DATABASE ai_ready_erp TO erpadmin;

-- 创建表空间
CREATE SCHEMA IF NOT EXISTS erp_core;
CREATE SCHEMA IF NOT EXISTS erp_batch;
CREATE SCHEMA IF NOT EXISTS erp_invoice;
CREATE SCHEMA IF NOT EXISTS erp_purchase;
CREATE SCHEMA IF NOT EXISTS erp_sales;
CREATE SCHEMA IF NOT EXISTS erp_supplier;
```

## 部署步骤

### 方式一：使用自动化脚本

```bash
# 进入部署目录
cd I:\AI-Ready\deploy\erp-modules

# 执行部署脚本
bash deploy-production.sh
```

### 方式二：手动部署

#### 1. 构建模块

```bash
# 构建所有模块
cd I:\AI-Ready\backend\erp

for module in erp-batch-sn erp-invoice erp-purchase-exchange erp-sales-exchange supplier-portal; do
    cd $module
    mvn clean package -DskipTests -Pprod
    cd ..
done
```

#### 2. 构建Docker镜像

```bash
cd I:\AI-Ready\deploy\erp-modules

docker-compose build
```

#### 3. 启动服务

```bash
# 使用生产环境配置
docker-compose --env-file .env.production up -d
```

#### 4. 验证部署

```bash
# 检查容器状态
docker-compose ps

# 测试API健康检查
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

## 部署验证

### 1. 容器状态检查

```bash
docker-compose --env-file .env.production ps
```

所有容器应该处于 `running` 状态。

### 2. 健康检查

```bash
# 批次/序列号管理
curl http://localhost:8081/actuator/health

# 发票管理
curl http://localhost:8082/actuator/health

# 采购询价/报价管理
curl http://localhost:8083/actuator/health

# 销售价格策略管理
curl http://localhost:8084/actuator/health

# 供应商协同门户
curl http://localhost:8085/actuator/health
```

应该返回 `{"status":"UP"}`

### 3. 数据库连接检查

```bash
# 连接PostgreSQL
docker exec -it ai-ready-erp-postgres psql -U erpadmin -d ai_ready_erp

# 检查表
\dt

# 检查连接数
SELECT count(*) FROM pg_stat_activity;
```

### 4. Redis连接检查

```bash
# 连接Redis
docker exec -it ai-ready-erp-redis redis-cli -a <密码>

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

### 监控指标

- CPU使用率
- 内存使用率
- JVM堆内存
- 响应时间
- 错误率
- 数据库连接池
- Redis连接状态
- 业务指标（批次追溯、发票处理、询价响应、价格计算、供应商协同）

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
- 服务是否启用 `/actuator/prometheus` 端点
- 网络连接是否正常

## 回滚方案

```bash
# 停止当前服务
docker-compose down

# 使用备份镜像恢复
docker load -i backups/<timestamp>/<module-latest.tar>

# 启动服务
docker-compose --env-file .env-production up -d
```

## 附录

### 端口映射

| 服务 | 容器端口 | 主机端口 |
|------|---------|---------|
| PostgreSQL | 5432 | 5432 |
| Redis | 6379 | 6379 |
| Batch/SN | 8080 | 8081 |
| Invoice | 8080 | 8082 |
| Purchase Exchange | 8080 | 8083 |
| Sales Exchange | 8080 | 8084 |
| Supplier Portal | 8080 | 8085 |
| Prometheus | 9090 | 9090 |
| Grafana | 3000 | 3000 |

### 日志路径

- 容器日志: `/var/log/ai-ready/erp/`
- Docker日志: `docker logs <container_name>`

### 配置路径

- Docker Compose: `I:\AI-Ready\deploy\erp-modules\docker-compose.yml`
- 环境配置: `I:\AI-Ready\deploy\erp-modules\.env.production`
- 监控配置: `I:\AI-Ready\deploy\erp-modules\monitoring\`