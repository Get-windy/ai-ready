# Sprint 27+1 测试环境部署验证指南

## 目录

1. [概述](#1-概述)
2. [验证流程设计](#2-验证流程设计)
3. [验证脚本使用说明](#3-验证脚本使用说明)
4. [Docker Compose部署验证流程](#4-docker-compose部署验证流程)
5. [手动部署验证流程](#5-手动部署验证流程)
6. [服务启动验证](#6-服务启动验证)
7. [服务间通信验证](#7-服务间通信验证)
8. [验证报告填写指南](#8-验证报告填写指南)
9. [问题排查指南](#9-问题排查指南)
10. [附录](#10-附录)

---

## 1. 概述

### 1.1 目的

本文档定义了 Sprint 27+1 测试环境部署完成后的验证流程，确保所有服务正确部署并可正常运行。

### 1.2 适用范围

- Docker Compose 部署的测试环境
- 手动部署的测试环境
- Sprint 27+1 迭代内所有测试环境验证

### 1.3 验证指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 部署验证覆盖率 | ≥95% | 所有关键服务均需验证 |
| 验证脚本可用率 | 100% | 所有验证脚本需可正常执行 |
| 验证报告完整性 | ≥90% | 报告需覆盖所有验证项 |
| 验证问题解决率 | ≥95% | 发现问题需及时解决 |

### 1.4 前置条件

- 测试环境已完成部署（Docker Compose 或手动）
- 验证人员具有环境访问权限
- 已安装验证工具（curl, docker, nc）

---

## 2. 验证流程设计

### 2.1 验证流程总览

```
┌─────────────────────────────────────────────────────────────┐
│                    部署验证流程                              │
├─────────────────────────────────────────────────────────────┤
│  1. 前置条件检查                                             │
│     └── Docker/服务状态 / 工具可用性 / 网络连通性              │
│                          ↓                                  │
│  2. 容器/服务状态验证                                        │
│     └── 各服务容器是否正常运行                               │
│                          ↓                                  │
│  3. 端口连通性验证                                           │
│     └── 各服务端口是否可访问                                 │
│                          ↓                                  │
│  4. 健康检查端点验证                                         │
│     └── HTTP健康检查端点是否返回200                          │
│                          ↓                                  │
│  5. 数据库连接验证                                           │
│     └── PostgreSQL / Redis 可连接性                         │
│                          ↓                                  │
│  6. 服务间通信验证                                           │
│     └── 服务间网络是否互通                                   │
│                          ↓                                  │
│  7. 日志验证                                                 │
│     └── 服务日志是否正常输出                                 │
│                          ↓                                  │
│  8. 资源使用验证                                             │
│     └── CPU / 内存 / 磁盘是否在合理范围                      │
│                          ↓                                  │
│  9. 生成验证报告                                             │
│     └── Markdown格式验证报告                                │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 验证流程对照表

| 验证步骤 | Docker Compose | 手动部署 | 验证工具 | 预计时间 |
|----------|---------------|----------|----------|----------|
| 前置条件检查 | ✅ | ✅ | validate-deployment.sh | 30秒 |
| 容器状态验证 | ✅ | ❌ | validate-deployment.sh | 30秒 |
| 端口连通性验证 | ✅ | ✅ | validate-deployment.sh | 1分钟 |
| 健康检查端点 | ✅ | ✅ | validate-deployment.sh | 1分钟 |
| 数据库连接验证 | ✅ | ✅ | health-check.sh | 30秒 |
| 服务间通信验证 | ✅ | ✅ | validate-deployment.sh | 1分钟 |
| 日志验证 | ✅ | ✅ | validate-deployment.sh | 30秒 |
| 资源使用验证 | ✅ | ✅ | validate-deployment.sh | 30秒 |
| API接口验证 | ✅ | ✅ | api-validation.sh | 1分钟 |

---

## 3. 验证脚本使用说明

### 3.1 脚本清单

| 脚本名称 | 路径 | 功能 | 适用场景 |
|----------|------|------|----------|
| validate-deployment.sh | `scripts/validation/validate-deployment.sh` | 综合部署验证 | 常规验证 |
| health-check.sh | `scripts/validation/health-check.sh` | 服务健康检查 | 快速检查 |
| api-validation.sh | `scripts/validation/api-validation.sh` | API接口验证 | 接口测试 |
| quick-validate.sh | `scripts/validation/quick-validate.sh` | 一键综合验证 | 完整验证 |

### 3.2 validate-deployment.sh

**功能**：综合部署验证，覆盖容器状态、端口、健康检查、数据库、日志、资源等。

**用法**：
```bash
# 验证所有Docker服务（默认）
./validate-deployment.sh

# 验证手动部署环境
./validate-deployment.sh -m manual

# 仅验证指定服务
./validate-deployment.sh -s postgres

# 输出报告到文件
./validate-deployment.sh -o validation-report.md

# 静默模式
./validate-deployment.sh -q
```

**参数说明**：
| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| --mode | -m | 验证模式 (docker/manual) | docker |
| --service | -s | 指定服务名称 | all |
| --output | -o | 报告输出路径 | stdout |
| --quiet | -q | 静默模式 | false |
| --help | -h | 显示帮助 | - |

**验证服务列表**：
- postgres (5432)
- redis (6379)
- rabbitmq (5672)
- app (8080)
- nginx (80)
- prometheus (9090)
- grafana (3000)

### 3.3 health-check.sh

**功能**：对单个或多个服务执行深度健康检查。

**用法**：
```bash
# 检查所有服务
./health-check.sh all

# 检查指定服务
./health-check.sh postgres
./health-check.sh redis
./health-check.sh app
```

**支持服务**：all, postgres, redis, rabbitmq, app, nginx, prometheus, grafana

### 3.4 api-validation.sh

**功能**：验证API接口的可用性和响应正确性。

**用法**：
```bash
# 使用默认基础URL
./api-validation.sh

# 指定基础URL
API_BASE_URL=http://test-env:8080 ./api-validation.sh
```

**验证接口**：
| 方法 | 端点 | 期望状态码 | 说明 |
|------|------|-----------|------|
| GET | /actuator/health | 200 | 健康检查 |
| GET | /actuator/info | 200 | 应用信息 |
| POST | /api/auth/login | 400 | 登录(参数缺失) |
| GET | /api/users | 401 | 用户列表(未认证) |
| GET | /api/system/status | 200 | 系统状态 |

### 3.5 quick-validate.sh

**功能**：一键执行所有验证并生成综合报告。

**用法**：
```bash
# 执行完整验证
./quick-validate.sh

# 指定输出报告路径
./quick-validate.sh /path/to/report.md
```

**输出**：
- 综合Markdown报告：`reports/validation_report_YYYYMMDD_HHMMSS.md`
- 分项日志文件：`reports/deploy_*.md`, `reports/health_*.log`, `reports/api_*.log`

---

## 4. Docker Compose部署验证流程

### 4.1 验证前准备

```bash
# 1. 进入项目目录
cd /path/to/ai-ready

# 2. 确认Docker Compose文件存在
ls docker-compose.yml docker-compose.test.yml

# 3. 确认环境变量文件
ls .env .env.test

# 4. 检查Docker状态
docker info
docker-compose ps
```

### 4.2 执行验证

```bash
# 方式1：使用一键验证脚本
./scripts/validation/quick-validate.sh

# 方式2：分步验证
cd scripts/validation

# 步骤1：综合部署验证
./validate-deployment.sh -o deploy-report.md

# 步骤2：健康检查
./health-check.sh all

# 步骤3：API验证
./api-validation.sh
```

### 4.3 验证检查清单

**容器状态**：
- [ ] postgres 容器运行中
- [ ] redis 容器运行中
- [ ] rabbitmq 容器运行中
- [ ] app 容器运行中
- [ ] nginx 容器运行中
- [ ] prometheus 容器运行中
- [ ] grafana 容器运行中

**端口连通性**：
- [ ] PostgreSQL 5432 端口可访问
- [ ] Redis 6379 端口可访问
- [ ] RabbitMQ 5672/15672 端口可访问
- [ ] 应用 8080 端口可访问
- [ ] Nginx 80/443 端口可访问
- [ ] Prometheus 9090 端口可访问
- [ ] Grafana 3000 端口可访问

**健康检查**：
- [ ] /actuator/health 返回 HTTP 200
- [ ] /actuator/info 返回应用信息
- [ ] Prometheus /-/healthy 返回 HTTP 200
- [ ] Grafana /api/health 返回 HTTP 200

**数据库**：
- [ ] PostgreSQL 可连接并执行查询
- [ ] Redis 可连接并响应 PING

**服务间通信**：
- [ ] App → PostgreSQL 可通信
- [ ] App → Redis 可通信
- [ ] Nginx → App 可通信

---

## 5. 手动部署验证流程

### 5.1 验证前准备

```bash
# 1. 确认各服务已手动安装并启动
systemctl status postgresql
systemctl status redis
systemctl status nginx

# 2. 确认配置文件正确
ls /etc/postgresql/*/main/postgresql.conf
ls /etc/redis/redis.conf
ls /etc/nginx/nginx.conf

# 3. 检查进程状态
ps aux | grep -E "postgres|redis|nginx|java"
```

### 5.2 执行验证

```bash
# 使用手动模式验证
cd scripts/validation
./validate-deployment.sh -m manual

# 指定服务验证
./validate-deployment.sh -m manual -s postgres

# API验证（需要指定正确的基础URL）
API_BASE_URL=http://localhost:8080 ./api-validation.sh
```

### 5.3 手动部署专项检查

**进程检查**：
```bash
# PostgreSQL
ps aux | grep postgres
pg_isready -U test_user

# Redis
ps aux | grep redis-server
redis-cli ping

# Nginx
ps aux | grep nginx
curl -I http://localhost

# 应用服务
ps aux | grep java
curl http://localhost:8080/actuator/health
```

**配置文件检查**：
- [ ] 数据库连接配置正确
- [ ] Redis连接配置正确
- [ ] 应用端口配置正确
- [ ] Nginx代理配置正确
- [ ] 日志路径配置正确

---

## 6. 服务启动验证

### 6.1 启动顺序验证

正确的服务启动顺序：

```
1. 基础设施层
   ├── PostgreSQL (5432)
   ├── Redis (6379)
   └── RabbitMQ (5672, 15672)
   
2. 应用层
   └── App Service (8080)
   
3. 网关/代理层
   └── Nginx (80, 443)
   
4. 监控层
   ├── Prometheus (9090)
   └── Grafana (3000)
```

### 6.2 启动验证命令

```bash
# 检查启动顺序日志
docker logs postgres --tail 20
docker logs redis --tail 20
docker logs app --tail 50

# 检查服务启动时间
docker ps --format "{{.Names}}\t{{.Status}}"
```

### 6.3 启动超时处理

| 服务 | 正常启动时间 | 超时判断 | 处理措施 |
|------|-------------|----------|----------|
| PostgreSQL | 5-10秒 | >30秒 | 检查数据目录权限 |
| Redis | 1-3秒 | >10秒 | 检查内存和配置 |
| RabbitMQ | 10-20秒 | >60秒 | 检查erlang.cookie |
| App | 20-60秒 | >120秒 | 检查数据库连接 |
| Nginx | 1-3秒 | >10秒 | 检查配置文件语法 |
| Prometheus | 5-10秒 | >30秒 | 检查配置文件 |
| Grafana | 10-20秒 | >60秒 | 检查数据库迁移 |

---

## 7. 服务间通信验证

### 7.1 通信矩阵

| 源服务 | 目标服务 | 端口 | 协议 | 用途 |
|--------|----------|------|------|------|
| app | postgres | 5432 | TCP | 数据持久化 |
| app | redis | 6379 | TCP | 缓存/会话 |
| app | rabbitmq | 5672 | TCP | 消息队列 |
| nginx | app | 8080 | HTTP | 反向代理 |
| prometheus | app | 8080 | HTTP | 指标采集 |
| grafana | prometheus | 9090 | HTTP | 数据源 |

### 7.2 验证方法

**Docker Compose环境**：
```bash
# App → PostgreSQL
docker exec app nc -z postgres 5432

# App → Redis
docker exec app nc -z redis 6379

# App → RabbitMQ
docker exec app nc -z rabbitmq 5672

# Nginx → App
docker exec nginx curl -s http://app:8080/actuator/health
```

**手动部署环境**：
```bash
# 从应用服务器测试
curl http://postgres-server:5432
redis-cli -h redis-server ping
```

### 7.3 网络诊断命令

```bash
# 查看容器网络
docker network ls
docker network inspect ai-ready_default

# 测试DNS解析
docker exec app nslookup postgres

# 查看路由表
docker exec app ip route

# 抓包分析
docker exec app tcpdump -i any -n port 5432
```

---

## 8. 验证报告填写指南

### 8.1 报告模板位置

`docs/templates/validation-report.md`

### 8.2 报告填写规范

**必填字段**：
- 验证时间
- 验证人员
- 验证环境
- 验证模式 (docker/manual)
- 总检查项数
- 通过/失败/警告数量
- 验证结论

**详细结果**：
- 每个验证项需记录：期望结果、实际结果、是否通过
- 失败项需记录：错误信息、影响范围、建议措施
- 警告项需记录：警告内容、风险等级、是否需处理

### 8.3 报告生成方式

**自动生成**：
```bash
# 使用验证脚本自动生成
./validate-deployment.sh -o report.md

# 一键生成完整报告
./quick-validate.sh report.md
```

**手动填写**：
复制 `docs/templates/validation-report.md`，按模板要求填写各字段。

### 8.4 报告示例

```markdown
# 部署验证报告

| 项目 | 值 |
|------|-----|
| 验证时间 | 2026-04-27 10:00:00 |
| 验证人员 | 测试工程师 |
| 验证环境 | Sprint 27+1 测试环境 |
| 验证模式 | docker |

## 验证结果汇总

| 指标 | 值 |
|------|-----|
| 总检查项 | 25 |
| 通过 | 24 |
| 失败 | 0 |
| 警告 | 1 |
| 通过率 | 96% |

## 结论

✅ 部署验证通过，测试环境可正常使用。
⚠️ 建议关注Grafana内存使用率（当前75%）。
```

---

## 9. 问题排查指南

### 9.1 常见问题速查表

| 现象 | 可能原因 | 排查命令 | 解决方案 |
|------|----------|----------|----------|
| 容器无法启动 | 端口冲突 | `docker logs <容器名>` | 修改端口映射 |
| | 配置错误 | `docker-compose config` | 检查配置文件 |
| | 镜像不存在 | `docker images` | 重新构建镜像 |
| 端口不通 | 防火墙 | `iptables -L` | 开放端口 |
| | 服务未监听 | `netstat -tlnp` | 启动服务 |
| | 网络隔离 | `docker network inspect` | 检查网络配置 |
| 健康检查失败 | 依赖未就绪 | `docker logs` | 等待依赖启动 |
| | 配置错误 | `curl -v` | 修正配置 |
| 数据库连接失败 | 认证错误 | `docker logs postgres` | 检查用户名密码 |
| | 网络不通 | `docker exec app nc` | 检查网络 |
| | 数据库未创建 | `docker exec postgres psql` | 创建数据库 |
| 服务间通信失败 | DNS解析失败 | `docker exec app nslookup` | 检查容器名 |
| | 网络隔离 | `docker network ls` | 加入同一网络 |

### 9.2 详细排查流程

**问题1：容器启动后立即退出**

```bash
# 1. 查看容器日志
docker logs <container_name>

# 2. 检查退出码
docker inspect <container_name> --format='{{.State.ExitCode}}'

# 3. 常见退出码含义
# 0 - 正常退出
# 1 - 通用错误
# 137 - OOMKilled (内存不足)
# 143 - 收到SIGTERM

# 4. 排查OOM
 docker inspect <container_name> --format='{{.State.OOMKilled}}'
```

**问题2：端口不可访问**

```bash
# 1. 检查服务是否监听端口
docker exec <container> netstat -tlnp

# 2. 检查端口映射
docker port <container>

# 3. 测试本机连通性
curl localhost:<port>

# 4. 测试容器内连通性
docker exec <container> curl localhost:<port>

# 5. 检查防火墙
sudo iptables -L -n | grep <port>
```

**问题3：健康检查端点返回非200**

```bash
# 1. 直接访问健康端点
curl -v http://localhost:8080/actuator/health

# 2. 查看应用日志
docker logs <app_container> --tail 100

# 3. 检查依赖服务状态
./health-check.sh all

# 4. 查看详细健康信息
curl http://localhost:8080/actuator/health | jq .
```

**问题4：数据库连接超时**

```bash
# 1. 检查数据库服务状态
docker exec postgres pg_isready

# 2. 检查网络连通性
docker exec app nc -zv postgres 5432

# 3. 检查连接配置
docker exec app env | grep DB_

# 4. 检查连接池状态
curl http://localhost:8080/actuator/metrics/jdbc.connections.active
```

### 9.3 紧急恢复步骤

如果验证发现严重问题，需要快速恢复：

```bash
# 1. 停止所有服务
docker-compose down

# 2. 清理容器和数据卷（谨慎操作）
docker-compose down -v

# 3. 重新构建镜像
docker-compose build --no-cache

# 4. 重新启动服务
docker-compose up -d

# 5. 等待服务就绪
sleep 30

# 6. 重新验证
./quick-validate.sh
```

---

## 10. 附录

### 10.1 验证脚本目录结构

```
scripts/validation/
├── validate-deployment.sh      # 综合部署验证
├── health-check.sh             # 服务健康检查
├── api-validation.sh           # API接口验证
├── quick-validate.sh           # 一键综合验证
└── reports/                    # 验证报告输出目录
    ├── validation_report_*.md  # 综合报告
    ├── deploy_*.md             # 部署验证日志
    ├── health_*.log            # 健康检查日志
    └── api_*.log               # API验证日志
```

### 10.2 相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| Docker Compose部署指南 | `docs/testing/deploy/DOCKER_COMPOSE_DEPLOY.md` | 部署步骤 |
| 手动部署手册 | `docs/testing/deploy/MANUAL_DEPLOY.md` | 手动部署 |
| 健康检查指南 | `docs/testing/deploy/HEALTH_CHECK.md` | 健康检查配置 |
| 初始化脚本说明 | `docs/testing/deploy/INIT_SCRIPTS.md` | 初始化脚本 |
| 验证报告模板 | `docs/templates/validation-report.md` | 报告模板 |
| 验证操作手册 | `docs/operations/validation/validation-guide.md` | 操作手册 |

### 10.3 版本历史

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| 1.0.0 | 2026-04-27 | 初始版本 | doc-writer |

### 10.4 联系方式

- 问题反馈：项目群组 `group_1775281918084_4yhkbw`
- 文档维护：doc-writer
