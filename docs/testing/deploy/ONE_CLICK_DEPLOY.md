# 一键部署脚本使用说明

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**适用环境**: Sprint 27+1 测试环境  
**最后更新**: 2026-04-27

---

## 目录

1. [概述](#1-概述)
2. [快速开始](#2-快速开始)
3. [部署脚本介绍](#3-部署脚本介绍)
4. [使用方法详解](#4-使用方法详解)
5. [部署流程说明](#5-部署流程说明)
6. [常见场景示例](#6-常见场景示例)
7. [部署验证](#7-部署验证)
8. [故障排查](#8-故障排查)
9. [最佳实践](#9-最佳实践)
10. [附录](#10-附录)

---

## 1. 概述

### 1.1 目的

本文档提供 Sprint 27+1 测试环境一键部署脚本的详细使用说明，帮助运维人员快速、准确地完成测试环境部署。

### 1.2 适用范围

- **环境**: 测试环境 (test) 和生产环境 (prod)
- **部署方式**: Docker Compose 容器化部署
- **操作系统**: Linux (bash) 和 Windows (bat)
- **用户角色**: 运维工程师、DevOps工程师、系统管理员

### 1.3 部署优势

| 优势 | 说明 |
|------|------|
| **一键操作** | 单命令完成整个环境部署 |
| **自动化备份** | 部署前自动备份现有数据 |
| **健康检查** | 部署后自动验证服务可用性 |
| **快速回滚** | 部署失败自动回滚到备份版本 |
| **日志记录** | 完整部署日志和报告生成 |
| **环境隔离** | 支持多环境配置隔离 |

### 1.4 前置条件

**必需条件**：
- Docker Engine 20.10+ 已安装并运行
- Docker Compose 1.29+ 已安装
- Git Bash（Windows环境）
- 环境访问权限和配置文件

**推荐条件**：
- curl 命令（健康检查需要）
- nc 命令（端口检查需要）
- 至少4GB可用内存
- 至少10GB可用磁盘空间

---

## 2. 快速开始

### 2.1 最简部署命令

**Linux/macOS环境**：

```bash
cd I:/AI-Ready/scripts/deploy/optimized
./deploy.sh
```

**Windows环境**：

```cmd
cd I:\AI-Ready\scripts\deploy\optimized
deploy.bat
```

**执行结果**：

```
[INFO] 2026-04-27 12:00:00 - ========== Docker Compose 一键部署脚本 ==========
[INFO] 2026-04-27 12:00:00 - 环境: test
[INFO] 2026-04-27 12:00:00 - 动作: deploy
[INFO] 2026-04-27 12:00:00 - ================================================
[INFO] 2026-04-27 12:00:01 - 检查依赖...
[INFO] 2026-04-27 12:00:02 - 加载环境配置: config/deploy/test.env
[INFO] 2026-04-27 12:00:03 - 开始备份数据到: backups/test/20260427_120000
[INFO] 2026-04-27 12:00:05 - 备份完成
[INFO] 2026-04-27 12:00:06 - 拉取最新镜像...
[INFO] 2026-04-27 12:00:10 - 启动新服务...
[INFO] 2026-04-27 12:00:15 - 等待服务就绪...
[INFO] 2026-04-27 12:00:20 - 健康检查通过
[INFO] 2026-04-27 12:00:21 - 所有关键服务验证通过
[INFO] 2026-04-27 12:00:22 - 部署完成！
```

### 2.2 验证部署

```bash
# 检查服务状态
./deploy.sh test status

# 使用验证脚本
cd I:/AI-Ready/scripts/validation
./validate-deployment.sh -m docker -o report.md
```

### 2.3 回滚部署（如有问题）

```bash
# 自动回滚到最新备份
./rollback.sh

# 或指定时间戳回滚
./rollback.sh 20260427_120000
```

---

## 3. 部署脚本介绍

### 3.1 脚本文件结构

```
I:/AI-Ready/scripts/deploy/optimized/
├── deploy.sh         # Linux/macOS一键部署脚本 (9082 bytes)
├── deploy.bat        # Windows一键部署脚本 (4578 bytes)
└── rollback.sh       # 自动回滚脚本 (1833 bytes)
```

### 3.2 deploy.sh 功能模块

| 功能模块 | 说明 |
|---------|------|
| **依赖检查** | 检查 Docker、Docker Compose 是否安装 |
| **环境配置** | 加载环境特定配置文件 (test.env / prod.env) |
| **数据备份** | 自动备份 PostgreSQL 和 Redis 数据 |
| **服务部署** | 拉取镜像、启动服务、等待就绪 |
| **健康检查** | 验证服务健康状态 (最多10次重试) |
| **部署验证** | 检查容器状态、数据库连接、Redis连接 |
| **报告生成** | 自动生成部署报告 Markdown 文件 |
| **回滚功能** | 部署失败自动回滚到备份版本 |

### 3.3 支持的操作命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `deploy` | 完整部署流程（默认） | `./deploy.sh test deploy` |
| `up` | 仅启动服务 | `./deploy.sh test up` |
| `down` | 仅停止服务 | `./deploy.sh test down` |
| `restart` | 重启服务 | `./deploy.sh test restart` |
| `status` | 查看状态和日志 | `./deploy.sh test status` |
| `backup` | 仅备份数据 | `./deploy.sh test backup` |
| `rollback` | 回滚到备份 | `./deploy.sh test rollback TIMESTAMP` |

---

## 4. 使用方法详解

### 4.1 基本用法

```bash
./deploy.sh [environment] [action] [parameters]
```

**参数说明**：

- **environment**: 目标环境
  - `test` - 测试环境（默认）
  - `prod` - 生产环境

- **action**: 执行动作
  - `deploy` - 完整部署（默认）
  - `up` - 启动服务
  - `down` - 停止服务
  - `restart` - 重启服务
  - `status` - 查看状态
  - `backup` - 备份数据
  - `rollback` - 回滚部署

- **parameters**: 可选参数
  - `TIMESTAMP` - 回滚时需要的备份时间戳

### 4.2 环境配置文件

脚本自动加载环境配置文件：

```
I:/AI-Ready/config/deploy/
├── test.env      # 测试环境配置
└── prod.env      # 生产环境配置
```

**配置文件示例**：

```bash
# config/deploy/test.env
DB_HOST=postgres
DB_PORT=5432
DB_NAME=qizhilian
DB_USER=appuser
DB_PASSWORD=test_password

REDIS_HOST=redis
REDIS_PORT=6379

RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672

API_PORT=8080
NGINX_PORT=80
```

### 4.3 Windows环境使用

**Git Bash方式（推荐）**：

```bash
# 在Git Bash中执行
cd /i/AI-Ready/scripts/deploy/optimized
./deploy.sh
```

**CMD方式**：

```cmd
# 使用deploy.bat脚本
cd I:\AI-Ready\scripts\deploy\optimized
deploy.bat test deploy
```

**PowerShell方式**：

```powershell
# 使用Git Bash调用
& "C:\Program Files\Git\bin\bash.exe" "I:\AI-Ready\scripts\deploy\optimized\deploy.sh"
```

---

## 5. 部署流程说明

### 5.1 部署流程图

```
┌─────────────────────────────────────────────────────────────┐
│                   一键部署流程                                │
└─────────────────────────────────────────────────────────────┘
         │
         ├─→ [1] 依赖检查
         │    ├─ Docker是否安装？
         │    ├─ Docker Compose是否安装？
         │    └─ 缺失依赖则报错退出
         │
         ├─→ [2] 环境配置加载
         │    ├─ 加载test.env或prod.env
         │    └─ 设置环境变量
         │
         ├─→ [3] 数据备份
         │    ├─ 备份PostgreSQL数据库
         │    ├─ 备份Redis数据
         │    └─ 备份路径: backups/{env}/{timestamp}/
         │
         ├─→ [4] 停止旧服务
         │    ├─ docker-compose down
         │    └─ 保留数据卷
         │
         ├─→ [5] 拉取镜像
         │    ├─ docker-compose pull
         │    └─ 检查镜像更新
         │
         ├─→ [6] 启动新服务
         │    ├─ docker-compose up -d
         │    └─ 启动失败自动回滚
         │
         ├─→ [7] 等待就绪
         │    ├─ sleep 10秒
         │    └─ 等待容器初始化
         │
         ├─→ [8] 健康检查
         │    ├─ curl检查 /health端点
         │    ├─ 最多10次重试
         │    └─ 超时则报错退出
         │
         ├─→ [9] 部署验证
         │    ├─ 检查容器状态
         │    ├─ 验证PostgreSQL连接
         │    ├─ 验证Redis连接
         │    └─ 生成部署报告
         │
         ├─→ [10] 部署完成
         │    └─ 输出部署日志和报告
         │
         └─────────────────────────────────────────────────────┘
```

### 5.2 各阶段耗时估算

| 阶段 | 预计耗时 | 说明 |
|------|---------|------|
| 依赖检查 | 1-2秒 | 快速验证 |
| 环境配置加载 | 1-2秒 | 文件读取 |
| 数据备份 | 5-30秒 | 视数据量而定 |
| 停止旧服务 | 2-5秒 | 容器停止 |
| 拉取镜像 | 10-60秒 | 视网络速度而定 |
| 启动新服务 | 5-15秒 | 容器启动 |
| 等待就绪 | 10秒 | 固定等待 |
| 健康检查 | 10-50秒 | 最多10次重试 |
| 部署验证 | 5-10秒 | 连接验证 |
| **总计** | **40-180秒** | 约1-3分钟 |

---

## 6. 常见场景示例

### 6.1 初次部署（全新环境）

**步骤**：

```bash
# 1. 检查依赖是否安装
docker --version
docker-compose --version

# 2. 确认配置文件存在
ls -la config/deploy/test.env

# 3. 执行一键部署
cd scripts/deploy/optimized
./deploy.sh test deploy

# 4. 验证部署结果
./deploy.sh test status

# 5. 使用验证脚本检查
cd scripts/validation
./validate-deployment.sh -m docker
```

**预期结果**：

- 所有容器正常运行
- 健康检查通过
- 数据库和Redis连接正常
- API服务可访问

### 6.2 更新部署（已有环境）

**场景**: 代码更新后重新部署测试环境

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 执行部署（自动备份旧数据）
cd scripts/deploy/optimized
./deploy.sh test deploy

# 3. 验证新版本
curl http://localhost:80/api/version

# 4. 查看部署报告
cat logs/deploy/deploy_*.md
```

**自动备份机制**：

- 每次部署前自动备份到 `backups/test/{timestamp}/`
- PostgreSQL: `postgres_{timestamp}.sql`
- Redis: `redis_dump.rdb`

### 6.3 快速重启服务

**场景**: 仅需重启服务，无需完整部署

```bash
# 快速重启
./deploy.sh test restart

# 或仅停止服务
./deploy.sh test down

# 再启动服务
./deploy.sh test up
```

### 6.4 数据备份与恢复

**备份数据**：

```bash
# 仅执行备份
./deploy.sh test backup

# 查看备份目录
ls -la backups/test/
```

**恢复数据**：

```bash
# 查看可用备份
ls -la backups/test/

# 回滚到指定备份
./rollback.sh 20260427_120000
```

### 6.5 生产环境部署

**⚠️ 注意**: 生产环境部署需要额外谨慎

```bash
# 1. 确认prod.env配置正确
cat config/deploy/prod.env

# 2. 执行生产部署
./deploy.sh prod deploy

# 3. 完整验证
./validate-deployment.sh -m docker -e prod

# 4. 查看生产日志
./deploy.sh prod status
```

---

## 7. 部署验证

### 7.1 自动验证机制

脚本自动执行以下验证：

**健康检查**：
```bash
# 脚本自动执行的检查
curl -s http://localhost:80/health
# 最多10次重试，每次间隔5秒
```

**服务验证**：
```bash
# PostgreSQL连接验证
docker-compose exec -T postgres pg_isready -U appuser -d qizhilian

# Redis连接验证
docker-compose exec -T redis redis-cli ping
```

### 7.2 手动验证步骤

**容器状态检查**：

```bash
docker-compose -f infra/docker/docker-compose.optimized.yml ps
```

**端口连通性检查**：

```bash
# 检查API端口
curl http://localhost:80/health

# 检查Prometheus
curl http://localhost:9090/-/healthy

# 检查Grafana
curl http://localhost:3000/api/health
```

**使用验证脚本**：

```bash
cd scripts/validation
./validate-deployment.sh -m docker -s all -o validation-report.md
```

### 7.3 部署报告解读

**报告位置**: `logs/deploy/deploy_{timestamp}.md`

**报告内容**：

```markdown
# 部署报告

## 基本信息
- 环境: test
- 时间: 2026-04-27 12:00:00
- 版本: 1.0.0
- 操作: deploy

## 部署服务
Name              Command               State   Ports
------------------------------------------------------
backend           java -jar app.jar     Up      8080/tcp
postgres          postgres              Up      5432/tcp
redis             redis-server          Up      6379/tcp
rabbitmq          rabbitmq-server       Up      5672/tcp
nginx             nginx                 Up      80/tcp
prometheus        prometheus            Up      9090/tcp
grafana           grafana-server        Up      3000/tcp

## 服务状态
- backend: running
- postgres: running
- redis: running
- rabbitmq: running
```

---

## 8. 故障排查

### 8.1 常见错误及解决方案

**错误1: Docker未安装**

```
[ERROR] docker 未安装，请先安装 docker
```

**解决方案**：
```bash
# Linux
sudo apt-get install docker.io docker-compose

# macOS
brew install docker docker-compose

# Windows
# 下载安装 Docker Desktop
```

**错误2: 配置文件不存在**

```
[WARN] 环境配置文件不存在: config/deploy/test.env
```

**解决方案**：
```bash
# 检查配置目录
ls -la config/deploy/

# 创建配置文件（如缺失）
cp config/deploy/test.env.template config/deploy/test.env
```

**错误3: 服务启动失败**

```
[ERROR] 服务启动失败
[INFO] 自动回滚到备份版本...
```

**解决方案**：
```bash
# 查看详细日志
docker-compose logs

# 检查资源限制
docker system df
df -h

# 手动排查后重新部署
./deploy.sh test deploy
```

**错误4: 健康检查超时**

```
[ERROR] 健康检查超时
```

**解决方案**：
```bash
# 手动检查服务状态
docker-compose ps

# 查看容器日志
docker-compose logs backend

# 检查端口占用
netstat -tlnp | grep 80

# 增加健康检查等待时间（修改脚本）
# max_attempts=15
```

**错误5: 数据库连接失败**

```
[ERROR] PostgreSQL 连接失败
```

**解决方案**：
```bash
# 检查PostgreSQL容器状态
docker-compose ps postgres

# 查看PostgreSQL日志
docker-compose logs postgres

# 重启PostgreSQL容器
docker-compose restart postgres

# 验证连接
docker-compose exec postgres pg_isready -U appuser
```

### 8.2 回滚流程

**自动回滚**：
- 部署失败时脚本自动执行回滚
- 回滚到最近一次备份版本

**手动回滚**：

```bash
# 查看可用备份
ls -la backups/test/

# 回滚到指定版本
./rollback.sh 20260427_120000
```

**回滚验证**：

```bash
# 回滚后验证服务状态
./deploy.sh test status

# 使用验证脚本
./validate-deployment.sh -m docker
```

---

## 9. 最佳实践

### 9.1 部署前准备

** checklist**：

- [ ] 确认Docker和Docker Compose已安装
- [ ] 确认环境配置文件正确 (test.env / prod.env)
- [ ] 确认有足够磁盘空间 (至少10GB)
- [ ] 确认网络连接稳定（拉取镜像）
- [ ] 确认端口未被占用 (80, 5432, 6379, 9090, 3000)
- [ ] 备份重要数据（已有备份机制）

### 9.2 部署时监控

**实时监控**：

```bash
# 查看部署日志
tail -f logs/deploy/deploy_*.log

# 查看容器启动日志
docker-compose logs -f

# 监控资源使用
docker stats
```

### 9.3 部署后验证

**完整验证流程**：

```bash
# 1. 检查容器状态
./deploy.sh test status

# 2. 使用验证脚本
cd scripts/validation
./validate-deployment.sh -m docker -o report.md

# 3. API功能验证
curl http://localhost:80/api/health
curl http://localhost:80/api/users

# 4. 监控系统验证
curl http://localhost:9090/-/healthy  # Prometheus
curl http://localhost:3000/api/health # Grafana
```

### 9.4 定期维护

**维护任务**：

```bash
# 定期清理旧备份（保留最近5个）
ls -t backups/test/ | tail -n +6 | xargs rm -rf

# 定期清理旧日志
find logs/deploy/ -mtime +30 -delete

# 定期更新镜像
docker-compose pull
```

### 9.5 生产环境注意事项

**额外措施**：

- 使用生产环境专用配置 (prod.env)
- 部署前完整备份生产数据
- 部署前通知相关人员
- 部署过程全程监控
- 准备应急预案和回滚流程
- 记录部署日志和版本信息

---

## 10. 附录

### 10.1 脚本参数完整列表

| 参数 | 默认值 | 说明 |
|------|--------|------|
| environment | test | 目标环境 (test/prod) |
| action | deploy | 执行动作 |
| TIMESTAMP | 最新备份 | 回滚时间戳 |

### 10.2 环境变量列表

| 变量 | 说明 | 示例值 |
|------|------|--------|
| DB_HOST | 数据库主机 | postgres |
| DB_PORT | 数据库端口 | 5432 |
| DB_NAME | 数据库名 | qizhilian |
| DB_USER | 数据库用户 | appuser |
| DB_PASSWORD | 数据库密码 | password |
| REDIS_HOST | Redis主机 | redis |
| REDIS_PORT | Redis端口 | 6379 |
| API_PORT | API端口 | 8080 |
| NGINX_PORT | Nginx端口 | 80 |

### 10.3 服务端口映射

| 服务 | 内部端口 | 外部端口 | 健康检查路径 |
|------|---------|---------|--------------|
| PostgreSQL | 5432 | 5432 | pg_isready |
| Redis | 6379 | 6379 | ping |
| RabbitMQ | 5672 | 5672 | /api/health |
| Backend API | 8080 | 8080 | /health |
| Nginx | 80 | 80 | /health |
| Prometheus | 9090 | 9090 | /-/healthy |
| Grafana | 3000 | 3000 | /api/health |

### 10.4 相关文档链接

- [部署故障处理指南](DEPLOY_TROUBLESHOOTING.md)
- [部署验证指南](DEPLOYMENT_VALIDATION.md)
- [Docker Compose部署指南](DOCKER_COMPOSE_DEPLOY.md)
- [健康检查文档](HEALTH_CHECK.md)

### 10.5 联系支持

**遇到问题时**：

1. 查看部署日志: `logs/deploy/deploy_*.md`
2. 查看容器日志: `docker-compose logs`
3. 参考[部署故障处理指南](DEPLOY_TROUBLESHOOTING.md)
4. 联系DevOps团队: devops-engineer

---

## 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| 1.0.0 | 2026-04-27 | 初始版本，完整使用说明 | doc-writer |

---

**文档维护**: 本文档随部署脚本更新同步维护。如有疑问请联系文档专家 (doc-writer)。