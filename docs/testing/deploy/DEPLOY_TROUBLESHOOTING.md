# 部署失败处理指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**适用环境**: Sprint 27+1 测试环境  
**最后更新**: 2026-04-27

---

## 目录

1. [概述](#1-概述)
2. [错误分类与诊断](#2-错误分类与诊断)
3. [常见错误处理](#3-常见错误处理)
4. [回滚操作流程](#4-回滚操作流程)
5. [紧急处理预案](#5-紧急处理预案)
6. [预防措施](#6-预防措施)
7. [故障记录与报告](#7-故障记录与报告)
8. [附录](#8-附录)

---

## 1. 概述

### 1.1 目的

本文档提供 Sprint 27+1 测试环境部署失败时的详细处理指南，帮助运维人员快速定位问题、执行回滚、恢复服务。

### 1.2 适用范围

- Docker Compose部署失败
- 服务启动失败
- 健康检查失败
- 数据库连接失败
- 网络连接失败
- 端口冲突问题

### 1.3 处理原则

| 原则 | 说明 |
|------|------|
| **快速响应** | 部署失败后立即介入处理 |
| **数据优先** | 优先保护和恢复数据 |
| **回滚优先** | 无法快速修复时立即回滚 |
| **日志记录** | 详细记录故障信息和处理过程 |
| **根因分析** | 解决后进行根因分析防止复发 |

### 1.4 处理流程总览

```
┌─────────────────────────────────────────────────────────┐
│           部署失败处理流程                                │
└─────────────────────────────────────────────────────────┘
         │
         ├─→ [1] 发现失败
         │    ├─ 脚本自动检测失败
         │    ├─ 自动触发回滚
         │    └─ 记录失败日志
         │
         ├─→ [2] 问题诊断
         │    ├─ 查看部署日志
         │    ├─ 查看容器日志
         │    ├─ 检查系统资源
         │    └─ 确定失败原因
         │
         ├─→ [3] 决策处理
         │    ├─ 可快速修复 → 执行修复
         │    ├─ 无法快速修复 → 执行回滚
         │    └─ 严重故障 → 紧急预案
         │
         ├─→ [4] 执行回滚（如需）
         │    ├─ 自动回滚（脚本已执行）
         │    ├─ 手动回滚补充
         │    └─ 验证回滚结果
         │
         ├─→ [5] 验证恢复
         │    ├─ 检查服务状态
         │    ├─ 验证数据完整性
         │    ├─ 功能测试验证
         │    └─ 用户可用性验证
         │
         ├─→ [6] 根因分析
         │    ├─ 分析失败原因
         │    ├─ 制定预防措施
         │    ├─ 更新处理流程
         │    └─ 记录经验教训
         │
         └─────────────────────────────────────────────────────┘
```

---

## 2. 错误分类与诊断

### 2.1 错误分类

**按严重程度分类**：

| 级别 | 严重程度 | 说明 | 处理优先级 |
|------|---------|------|------------|
| **P0 - 致命** | 服务完全不可用 | 所有服务启动失败、数据丢失 | 立即处理 |
| **P1 - 严重** | 核心服务不可用 | PostgreSQL/API服务失败 | 15分钟内处理 |
| **P2 - 中等** | 部分服务不可用 | 单个服务失败、健康检查超时 | 30分钟内处理 |
| **P3 - 轻微** | 功能受限 | 部分功能异常、性能下降 | 1小时内处理 |

**按失败阶段分类**：

| 阶段 | 常见错误 | 诊断方法 |
|------|---------|---------|
| **依赖检查** | Docker未安装、版本不兼容 | 检查版本、安装日志 |
| **配置加载** | 配置文件缺失、参数错误 | 检查配置文件内容 |
| **数据备份** | 备份失败、磁盘空间不足 | 检查备份目录、磁盘空间 |
| **镜像拉取** | 网络问题、镜像不存在 | 检查网络、镜像仓库 |
| **服务启动** | 容器启动失败、端口冲突 | 检查容器日志、端口占用 |
| **健康检查** | 服务无响应、超时 | 检查健康端点、服务日志 |
| **部署验证** | 数据库连接失败、Redis失败 | 检查连接配置、服务状态 |

### 2.2 诊断工具

**日志检查**：

```bash
# 部署日志
tail -100 logs/deploy/deploy_*.md

# 容器启动日志
docker-compose logs --tail=100

# 单个服务日志
docker-compose logs postgres --tail=50
docker-compose logs backend --tail=50

# Docker系统日志
journalctl -u docker --since "1 hour ago"
```

**状态检查**：

```bash
# 容器状态
docker-compose ps
docker ps -a

# 容器详情
docker inspect <container_name>

# 系统资源
docker system df
df -h
free -m
```

**连接检查**：

```bash
# 端口检查
netstat -tlnp | grep LISTEN
ss -tlnp

# 数据库连接测试
docker-compose exec postgres pg_isready -U appuser

# Redis连接测试
docker-compose exec redis redis-cli ping

# API健康检查
curl -v http://localhost:80/health
```

---

## 3. 常见错误处理

### 3.1 依赖检查失败

**错误表现**：

```
[ERROR] docker 未安装，请先安装 docker
[ERROR] docker-compose 未安装，请先安装 docker-compose
```

**诊断步骤**：

```bash
# 检查Docker版本
docker --version
# 预期: Docker version 20.10+

# 检查Docker Compose版本
docker-compose --version
# 预期: Docker Compose version 1.29+

# 检查Docker服务状态
systemctl status docker
# 或
sudo service docker status
```

**解决方案**：

**Linux (Ubuntu/Debian)**：

```bash
# 安装Docker
sudo apt-get update
sudo apt-get install -y docker.io

# 安装Docker Compose
sudo apt-get install -y docker-compose

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
docker-compose --version
```

**Linux (CentOS/RHEL)**：

```bash
# 安装Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker
```

**macOS**：

```bash
# 使用Homebrew安装
brew install docker docker-compose

# 或下载Docker Desktop
# https://www.docker.com/products/docker-desktop
```

**Windows**：

- 下载安装 Docker Desktop: https://www.docker.com/products/docker-desktop
- 安装完成后重启计算机
- 验证安装: `docker --version`

**预防措施**：

- 部署前检查依赖版本
- 定期更新Docker和Docker Compose
- 记录版本信息在部署报告中

### 3.2 配置文件缺失

**错误表现**：

```
[WARN] 环境配置文件不存在: config/deploy/test.env，使用默认配置
```

**诊断步骤**：

```bash
# 检查配置目录
ls -la config/deploy/

# 检查配置文件内容
cat config/deploy/test.env

# 检查配置文件权限
stat config/deploy/test.env
```

**解决方案**：

```bash
# 创建配置目录
mkdir -p config/deploy

# 从模板创建配置文件
cp config/deploy/test.env.template config/deploy/test.env

# 编辑配置文件
vim config/deploy/test.env
```

**配置文件模板**：

```bash
# config/deploy/test.env
# 数据库配置
DB_HOST=postgres
DB_PORT=5432
DB_NAME=qizhilian
DB_USER=appuser
DB_PASSWORD=test_password_123

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# RabbitMQ配置
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# 服务端口配置
API_PORT=8080
NGINX_PORT=80
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
```

### 3.3 服务启动失败

**错误表现**：

```
[ERROR] 服务启动失败
[INFO] 自动回滚到备份版本...
```

**诊断步骤**：

```bash
# 查看容器状态
docker-compose ps -a

# 查看失败容器日志
docker-compose logs --tail=100

# 查看具体服务日志
docker-compose logs backend --tail=50
docker-compose logs postgres --tail=50

# 检查容器详情
docker inspect <failed_container>

# 检查资源使用
docker stats --no-stream
df -h
free -m
```

**常见原因及解决方案**：

**原因1: 端口冲突**

```bash
# 检查端口占用
netstat -tlnp | grep -E '80|5432|6379|9090|3000'

# 停止占用进程
sudo kill -9 <PID>

# 或修改配置使用不同端口
vim config/deploy/test.env
# NGINX_PORT=8081
```

**原因2: 资源不足**

```bash
# 检查磁盘空间
df -h
# 需要至少10GB可用空间

# 清理Docker资源
docker system prune -a -f

# 清理旧备份
ls -t backups/test/ | tail -n +6 | xargs rm -rf

# 检查内存
free -m
# 需要至少4GB可用内存
```

**原因3: 镜像问题**

```bash
# 检查镜像是否存在
docker images | grep <image_name>

# 手动拉取镜像
docker pull <image_name>:<tag>

# 检查镜像仓库连接
ping registry-1.docker.io
```

**原因4: 配置错误**

```bash
# 检查Docker Compose配置
docker-compose config

# 验证配置语法
docker-compose config --quiet

# 检查环境变量
docker-compose config | grep -A 5 environment
```

### 3.4 健康检查超时

**错误表现**：

```
[INFO] 健康检查尝试 1/10...
[INFO] 健康检查尝试 2/10...
...
[INFO] 健康检查尝试 10/10...
[ERROR] 健康检查超时
```

**诊断步骤**：

```bash
# 手动健康检查
curl -v http://localhost:80/health

# 检查容器日志
docker-compose logs backend --tail=50

# 检查容器状态
docker-compose ps backend

# 检查网络连接
docker-compose exec backend ping localhost

# 检查应用启动日志
docker-compose exec backend cat /var/log/app.log
```

**解决方案**：

**方案1: 增加等待时间**

编辑 `scripts/deploy/optimized/deploy.sh`:

```bash
# 修改健康检查参数
max_attempts=15  # 从10增加到15
sleep_time=10    # 从5增加到10秒
```

**方案2: 手动重启服务**

```bash
# 重启后端服务
docker-compose restart backend

# 等待服务就绪
sleep 20

# 手动验证
curl http://localhost:80/health
```

**方案3: 检查应用启动问题**

```bash
# 查看应用启动日志
docker-compose exec backend tail -100 /var/log/app.log

# 检查Java应用状态
docker-compose exec backend jps -l

# 检查数据库连接
docker-compose exec backend nc -zv postgres 5432
```

### 3.5 数据库连接失败

**错误表现**：

```
[ERROR] PostgreSQL 连接失败
[ERROR] Redis 连接失败
```

**诊断步骤**：

```bash
# PostgreSQL诊断
docker-compose ps postgres
docker-compose logs postgres --tail=50
docker-compose exec postgres pg_isready -U appuser -d qizhilian

# Redis诊断
docker-compose ps redis
docker-compose logs redis --tail=50
docker-compose exec redis redis-cli ping

# 网络连通性测试
docker-compose exec backend ping postgres
docker-compose exec backend ping redis

# 端口连通性测试
docker-compose exec backend nc -zv postgres 5432
docker-compose exec backend nc -zv redis 6379
```

**解决方案**：

**PostgreSQL问题**：

```bash
# 重启PostgreSQL容器
docker-compose restart postgres

# 检查数据库是否创建
docker-compose exec postgres psql -U appuser -l

# 创建数据库（如缺失）
docker-compose exec postgres psql -U appuser -c "CREATE DATABASE qizhilian;"

# 检查用户权限
docker-compose exec postgres psql -U appuser -d qizhilian -c "SELECT current_user;"
```

**Redis问题**：

```bash
# 重启Redis容器
docker-compose restart redis

# 检查Redis状态
docker-compose exec redis redis-cli info

# 检查Redis配置
docker-compose exec redis redis-cli config get "*"

# 清除Redis数据（谨慎）
docker-compose exec redis redis-cli FLUSHALL
```

**网络问题**：

```bash
# 检查Docker网络
docker network ls
docker network inspect <network_name>

# 重建网络
docker-compose down
docker-compose up -d
```

### 3.6 部署验证失败

**错误表现**：

```
[ERROR] 部署验证失败
[ERROR] 关键服务验证未通过
```

**诊断步骤**：

```bash
# 检查所有容器状态
docker-compose ps

# 检查容器健康状态
docker inspect --format='{{.State.Health.Status}}' <container>

# 检查服务日志
docker-compose logs --tail=100

# 使用验证脚本诊断
cd scripts/validation
./validate-deployment.sh -m docker -v
```

**解决方案**：

```bash
# 重启失败服务
docker-compose restart <failed_service>

# 重启所有服务
docker-compose restart

# 完全重新部署
docker-compose down
docker-compose up -d

# 使用回滚脚本恢复
cd scripts/deploy/optimized
./rollback.sh
```

---

## 4. 回滚操作流程

### 4.1 自动回滚机制

**触发条件**：
- 服务启动失败
- 健康检查超时
- 部署验证失败

**自动执行流程**：

```
[INFO] 自动回滚到备份版本...
[INFO] 回滚到备份: backups/test/20260427_120000
[INFO] 恢复PostgreSQL数据...
[INFO] 恢复Redis数据...
[INFO] 重启服务...
[INFO] 回滚完成
```

### 4.2 手动回滚操作

**查看可用备份**：

```bash
# 列出所有备份
ls -la backups/test/

# 查看备份详情
ls -la backups/test/20260427_120000/

# 查看备份内容
cat backups/test/20260427_120000/postgres_20260427_120000.sql | head -20
```

**执行回滚**：

```bash
# 方法1: 使用rollback脚本
cd scripts/deploy/optimized
./rollback.sh 20260427_120000

# 方法2: 使用deploy脚本
./deploy.sh test rollback 20260427_120000

# 方法3: 手动回滚（高级）
BACKUP_DIR="backups/test/20260427_120000"

# 恢复PostgreSQL
docker-compose exec -T postgres psql -U appuser -d qizhilian < "$BACKUP_DIR/postgres_*.sql"

# 恢复Redis
docker-compose cp "$BACKUP_DIR/redis_dump.rdb" redis:/data/dump.rdb

# 重启服务
docker-compose restart
```

### 4.3 回滚验证

**验证步骤**：

```bash
# 1. 检查服务状态
docker-compose ps

# 2. 检查数据完整性
docker-compose exec postgres psql -U appuser -d qizhilian -c "SELECT count(*) FROM users;"

# 3. 检查Redis数据
docker-compose exec redis redis-cli DBSIZE

# 4. 功能验证
curl http://localhost:80/api/health
curl http://localhost:80/api/users

# 5. 使用验证脚本
cd scripts/validation
./validate-deployment.sh -m docker
```

**回滚报告**：

```markdown
# 回滚报告

## 基本信息
- 回滚时间: 2026-04-27 12:30:00
- 备份版本: 20260427_120000
- 操作者: devops-engineer
- 原因: 部署失败

## 回滚结果
- PostgreSQL恢复: ✅ 成功
- Redis恢复: ✅ 成功
- 服务重启: ✅ 成功
- 数据验证: ✅ 通过
- 功能测试: ✅ 正常

## 服务状态
- backend: running
- postgres: running
- redis: running
- nginx: running

## 验证结果
- API健康检查: ✅ 正常
- 数据库连接: ✅ 正常
- Redis连接: ✅ 正常
```

---

## 5. 紧急处理预案

### 5.1 P0级故障处理

**场景**: 所有服务不可用、数据丢失风险

**处理步骤**：

```bash
# 1. 立即停止部署操作
docker-compose down

# 2. 评估数据损失
ls -la backups/test/

# 3. 恢复最近备份
cd scripts/deploy/optimized
./rollback.sh <latest_backup>

# 4. 紧急通知团队
# 使用消息工具通知coordinator和团队

# 5. 启动应急监控
docker-compose up -d prometheus grafana

# 6. 验证核心功能
curl http://localhost:80/api/health

# 7. 记录详细日志
cat logs/deploy/deploy_*.md > emergency_log.txt
```

### 5.2 P1级故障处理

**场景**: 核心服务（数据库/API）不可用

**处理步骤**：

```bash
# 1. 诊断失败服务
docker-compose ps
docker-compose logs <failed_service> --tail=50

# 2. 快速重启
docker-compose restart <failed_service>

# 3. 检查资源
docker stats
df -h
free -m

# 4. 检查配置
cat config/deploy/test.env

# 5. 重新部署单个服务
docker-compose up -d <failed_service> --force-recreate

# 6. 验证恢复
curl http://localhost:80/health
```

### 5.3 数据恢复预案

**场景**: 数据库数据丢失或损坏

**处理步骤**：

```bash
# 1. 立即停止写入
docker-compose stop backend nginx

# 2. 检查备份可用性
ls -la backups/test/

# 3. 恢复PostgreSQL数据
BACKUP_FILE="backups/test/20260427_120000/postgres_*.sql"
docker-compose exec -T postgres psql -U appuser -d qizhilian < "$BACKUP_FILE"

# 4. 恢复Redis数据
docker-compose cp backups/test/20260427_120000/redis_dump.rdb redis:/data/dump.rdb
docker-compose restart redis

# 5. 验证数据完整性
docker-compose exec postgres psql -U appuser -d qizhilian -c "SELECT count(*) FROM users;"

# 6. 重启服务
docker-compose start backend nginx
```

---

## 6. 预防措施

### 6.1 部署前检查清单

**环境检查**：

- [ ] Docker版本 ≥ 20.10
- [ ] Docker Compose版本 ≥ 1.29
- [ ] 磁盘空间 ≥ 10GB
- [ ] 内存 ≥ 4GB
- [ ] 网络连接正常
- [ ] 端口未被占用

**配置检查**：

- [ ] 配置文件存在 (test.env / prod.env)
- [ ] 配置参数正确
- [ ] 环境变量设置正确
- [ ] Docker Compose配置正确

**数据准备**：

- [ ] 数据已备份
- [ ] 备份文件可访问
- [ ] 数据库用户权限正确
- [ ] Redis密码设置正确

### 6.2 部署监控措施

**实时监控**：

```bash
# 监控部署日志
tail -f logs/deploy/deploy_*.log

# 监控容器状态
watch -n 5 docker-compose ps

# 监控资源使用
docker stats

# 监控服务日志
docker-compose logs -f --tail=10
```

**自动告警配置**：

```yaml
# prometheus/alerts.yml
groups:
  - name: deploy_alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.instance }} 已停止"
          
      - alert: HealthCheckFailed
        expr: http_response_status != 200
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "健康检查失败"
```

### 6.3 定期维护措施

**每周维护**：

```bash
# 清理旧备份（保留5个）
ls -t backups/test/ | tail -n +6 | xargs rm -rf

# 清理旧日志（保留30天）
find logs/deploy/ -mtime +30 -delete

# 更新镜像
docker-compose pull

# 检查磁盘空间
df -h | grep -E 'Filesystem|/$'
```

**每月维护**：

```bash
# 全面验证部署
cd scripts/validation
./validate-deployment.sh -m docker -s all

# 性能测试
./deploy.sh test status

# 安全检查
docker scan <image_name>

# 更新文档
vim docs/testing/deploy/DEPLOY_TROUBLESHOOTING.md
```

---

## 7. 故障记录与报告

### 7.1 故障记录模板

```markdown
# 故障记录

## 基本信息
- 故障时间: YYYY-MM-DD HH:MM:SS
- 故障级别: P0/P1/P2/P3
- 发现人员: <name>
- 影响范围: <services>

## 故障表现
- 错误信息: <error_message>
- 服务状态: <status>
- 用户影响: <impact>

## 诊断过程
1. 查看日志: <log_file>
2. 检查状态: <status_check>
3. 确定原因: <root_cause>

## 处理过程
1. 执行操作: <action>
2. 恢复时间: <recovery_time>
3. 验证结果: <verification_result>

## 根因分析
- 直接原因: <direct_cause>
- 根本原因: <root_cause>
- 影响因素: <factors>

## 预防措施
1. <measure_1>
2. <measure_2>
3. <measure_3>

## 经验教训
- <lesson_1>
- <lesson_2>
```

### 7.2 故障报告流程

**报告步骤**：

1. **立即报告**: 发现故障后立即通知coordinator和团队
2. **处理记录**: 详细记录诊断和处理过程
3. **恢复报告**: 恢复后提交恢复报告
4. **根因分析**: 24小时内完成根因分析
5. **改进措施**: 制定预防措施并落实

**报告渠道**：

- 项目群组通知: `group:group_1775281918084_4yhkbw`
- 任务报告: `task_report_to_supervisor`
- 文档记录: `docs/operations/fault-records/`

### 7.3 故障统计与分析

**月度统计**：

```markdown
# 月度故障统计 - YYYY-MM

## 故障总数
- P0级故障: X次
- P1级故障: X次
- P2级故障: X次
- P3级故障: X次

## 平均恢复时间
- P0: X分钟
- P1: X分钟
- P2: X分钟
- P3: X分钟

## 主要故障类型
1. 服务启动失败: X次
2. 数据库连接失败: X次
3. 健康检查超时: X次
4. 端口冲突: X次

## 改进效果
- 上月故障次数: X次
- 本月故障次数: X次
- 减少比例: X%
```

---

## 8. 附录

### 8.1 快速诊断命令速查

```bash
# 状态检查
docker-compose ps
docker ps -a
docker stats

# 日志查看
docker-compose logs --tail=100
docker-compose logs <service> --tail=50

# 连接测试
docker-compose exec postgres pg_isready
docker-compose exec redis redis-cli ping
curl http://localhost:80/health

# 资源检查
df -h
free -m
netstat -tlnp

# 配置检查
docker-compose config
cat config/deploy/test.env

# 回滚操作
./rollback.sh <timestamp>
./deploy.sh test rollback <timestamp>
```

### 8.2 常用恢复命令

```bash
# 重启单个服务
docker-compose restart <service>

# 重启所有服务
docker-compose restart

# 重新创建容器
docker-compose up -d --force-recreate

# 恢复数据
docker-compose exec -T postgres psql -U appuser -d qizhilian < backup.sql

# 清理重建
docker-compose down
docker-compose up -d
```

### 8.3 紧急联系信息

| 角色 | Agent ID | 职责 |
|------|---------|------|
| Coordinator | main | 整体协调、决策 |
| DevOps Engineer | devops-engineer | 部署技术支持 |
| Backend Developer | backend-dev | 应用层问题排查 |
| QA Engineer | qa-engineer | 验证测试 |
| Doc Writer | doc-writer | 文档维护 |

### 8.4 相关文档链接

- [一键部署脚本使用说明](ONE_CLICK_DEPLOY.md)
- [部署验证指南](DEPLOYMENT_VALIDATION.md)
- [Docker Compose部署指南](DOCKER_COMPOSE_DEPLOY.md)
- [健康检查文档](HEALTH_CHECK.md)
- [用户操作手册](../operations/USER_GUIDE.md)

---

## 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| 1.0.0 | 2026-04-27 | 初始版本，完整故障处理指南 | doc-writer |

---

**文档维护**: 本文档随部署脚本更新同步维护。如有疑问请联系文档专家 (doc-writer)。