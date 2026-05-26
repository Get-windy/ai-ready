# AI-Ready 自动化部署脚本使用文档

> 版本: v1.0.0  
> 更新日期: 2026-04-09  
> 作者: devops-engineer

---

## 目录

1. [概述](#概述)
2. [快速开始](#快速开始)
3. [脚本说明](#脚本说明)
4. [环境配置](#环境配置)
5. [部署流程](#部署流程)
6. [回滚操作](#回滚操作)
7. [健康检查](#健康检查)
8. [故障排查](#故障排查)

---

## 概述

AI-Ready 自动化部署脚本提供了一键部署、回滚和健康检查功能，支持以下环境：

- **dev**: 开发环境
- **staging**: 测试环境  
- **prod**: 生产环境

### 功能特性

- ✅ 一键部署到指定环境
- ✅ 自动备份当前版本
- ✅ 部署失败自动回滚
- ✅ 健康检查验证
- ✅ 多环境配置管理
- ✅ 详细的日志记录

---

## 快速开始

### 1. 前置要求

```bash
# 确保已安装
- Bash 4.0+
- SSH 客户端
- Docker & Docker Compose
- curl
```

### 2. 配置环境

```bash
# 复制并编辑环境配置
cp config/dev.env.example config/dev.env
vim config/dev.env
```

### 3. 执行部署

```bash
# 部署到开发环境
./scripts/deploy.sh -e dev -v 1.0.0

# 部署到生产环境（需要确认）
./scripts/deploy.sh -e prod -v 2.0.0

# 强制部署（跳过确认）
./scripts/deploy.sh -e prod -v 2.0.0 --force
```

---

## 脚本说明

### deploy.sh - 主部署脚本

**用法:**
```bash
./deploy.sh [选项]
```

**选项:**
| 选项 | 说明 | 示例 |
|------|------|------|
| `-e, --env` | 部署环境 | `-e prod` |
| `-v, --version` | 部署版本 | `-v 1.2.0` |
| `-s, --skip-health` | 跳过健康检查 | `-s` |
| `-f, --force` | 强制部署 | `-f` |
| `--no-rollback` | 禁用自动回滚 | `--no-rollback` |
| `-h, --help` | 显示帮助 | `-h` |

**示例:**
```bash
# 基础部署
./deploy.sh -e dev

# 指定版本部署
./deploy.sh -e staging -v 1.5.0

# 生产环境部署（强制）
./deploy.sh -e prod -v 2.0.0 -f
```

### rollback.sh - 回滚脚本

**用法:**
```bash
./rollback.sh [选项] [备份ID]
```

**选项:**
| 选项 | 说明 | 示例 |
|------|------|------|
| `-e, --env` | 部署环境 | `-e prod` |
| `-l, --list` | 列出可用备份 | `-l` |
| `-f, --force` | 强制回滚 | `-f` |
| `-h, --help` | 显示帮助 | `-h` |

**示例:**
```bash
# 列出所有备份
./rollback.sh -e prod -l

# 回滚到最新版本
./rollback.sh -e prod

# 回滚到指定版本
./rollback.sh -e prod 20260409_120000
```

### health-check.sh - 健康检查脚本

**用法:**
```bash
./health-check.sh [选项]
```

**选项:**
| 选项 | 说明 | 示例 |
|------|------|------|
| `-e, --env` | 部署环境 | `-e prod` |
| `-h, --host` | 目标主机 | `-h localhost` |
| `-p, --port` | 服务端口 | `-p 8080` |
| `-t, --timeout` | 超时时间 | `-t 60` |
| `-v, --verbose` | 详细输出 | `-v` |
| `-q, --quiet` | 静默模式 | `-q` |

**示例:**
```bash
# 检查生产环境
./health-check.sh -e prod

# 检查指定主机
./health-check.sh -e dev -h localhost -p 8080

# 静默检查（用于CI/CD）
./health-check.sh -e prod -q
```

---

## 环境配置

### 配置文件结构

```
config/
├── dev.env      # 开发环境配置
├── staging.env  # 测试环境配置
└── prod.env     # 生产环境配置
```

### 配置项说明

```bash
# 基础配置
ENV=dev                          # 环境标识
APP_NAME=ai-ready               # 应用名称
APP_PORT=8080                   # 服务端口

# 远程主机
DEPLOY_HOST=localhost           # 部署主机
DEPLOY_USER=developer           # 部署用户
REMOTE_APP_DIR=/opt/ai-ready    # 远程应用目录

# 数据库
DB_HOST=localhost               # 数据库主机
DB_PORT=5432                    # 数据库端口
DB_NAME=ai_ready_dev            # 数据库名称

# Redis
REDIS_HOST=localhost            # Redis主机
REDIS_PORT=6379                 # Redis端口

# JVM配置
JAVA_OPTS="-Xms512m -Xmx1024m"  # JVM参数

# 通知
NOTIFY_ENABLED=true             # 启用通知
NOTIFY_WEBHOOK=xxx              # Webhook地址
```

---

## 部署流程

### 标准部署流程

```
1. 加载环境配置
   ↓
2. 部署前检查
   ↓
3. 备份当前版本
   ↓
4. 构建应用
   ↓
5. 部署到远程主机
   ↓
6. 健康检查
   ↓
7. 发送通知
```

### 部署日志

日志文件位置: `logs/deploy-YYYYMMDD.log`

```bash
# 查看今日部署日志
tail -f logs/deploy-$(date +%Y%m%d).log

# 查看所有日志
ls -la logs/
```

---

## 回滚操作

### 自动回滚

部署失败时自动触发（可通过 `--no-rollback` 禁用）

### 手动回滚

```bash
# 1. 查看可用备份
./rollback.sh -e prod -l

# 2. 执行回滚
./rollback.sh -e prod 20260409_143022

# 3. 验证回滚
./health-check.sh -e prod
```

### 备份管理

备份存储位置:
- 本地: `backups/`
- 远程: `$REMOTE_BACKUP_DIR/`

---

## 健康检查

### 检查端点

| 服务 | 端点 | 说明 |
|------|------|------|
| API | `/actuator/health` | Spring Boot健康检查 |
| Agent | `/health` | Agent健康检查 |
| NLP | `/api/nlp/health` | NLP服务健康检查 |

### 健康状态

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 故障排查

### 常见问题

#### 1. SSH连接失败

```bash
# 检查SSH密钥
ssh -i ~/.ssh/id_rsa $DEPLOY_USER@$DEPLOY_HOST

# 测试连接
./deploy.sh -e dev --dry-run
```

#### 2. 健康检查失败

```bash
# 手动检查
./health-check.sh -e prod -v

# 查看远程日志
ssh $DEPLOY_USER@$DEPLOY_HOST "tail -f /var/log/ai-ready/app.log"
```

#### 3. 部署失败回滚

```bash
# 查看部署日志
cat logs/deploy-$(date +%Y%m%d).log | grep ERROR

# 执行回滚
./rollback.sh -e prod -f
```

### 日志位置

| 日志类型 | 位置 |
|----------|------|
| 部署日志 | `logs/deploy-YYYYMMDD.log` |
| 回滚日志 | `logs/rollback-YYYYMMDD.log` |
| 健康检查 | `logs/health-YYYYMMDD.log` |
| 远程应用 | `/var/log/ai-ready/` |

---

## 目录结构

```
deploy/
├── scripts/
│   ├── deploy.sh         # 主部署脚本
│   ├── rollback.sh       # 回滚脚本
│   └── health-check.sh   # 健康检查脚本
├── config/
│   ├── dev.env          # 开发环境配置
│   ├── staging.env      # 测试环境配置
│   └── prod.env         # 生产环境配置
├── backups/             # 本地备份目录
├── logs/                # 日志目录
└── README.md            # 本文档
```

---

## 最佳实践

1. **部署前**
   - 确保代码已合并到正确分支
   - 更新版本号
   - 在staging环境验证通过

2. **部署时**
   - 使用 `-f` 强制部署生产环境
   - 关注部署日志输出
   - 验证健康检查通过

3. **部署后**
   - 验证核心功能
   - 监控错误日志
   - 保留备份至少7天

---

*文档版本: v1.0.0 | 最后更新: 2026-04-09*
