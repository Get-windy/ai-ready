# 测试环境自动化配置检查脚本使用手册

## 概述

本脚本集用于自动化检查AI-Ready测试环境的配置正确性和服务可用性，确保测试环境处于可正常运行状态。

## 脚本清单

| 脚本名 | 功能 | 关键检查项 |
|--------|------|-----------|
| `check-database.ps1` | 数据库配置检查 | PostgreSQL服务、端口、连接、关键数据库、磁盘空间 |
| `check-middleware.ps1` | 中间件配置检查 | Redis、Nginx、RabbitMQ服务及端口 |
| `check-application.ps1` | 应用配置校验 | 配置文件存在性、关键属性、日志配置 |
| `check-services.ps1` | 服务启动检查 | Windows服务、Java/Node进程、Docker |
| `check-ports.ps1` | 端口监听检查 | 5432/6379/80/443/8080等关键端口 |
| `check-connectivity.ps1` | 连通性检查 | TCP连通测试、DNS解析、防火墙状态 |
| `run-all-checks.ps1` | 一键检查 | 运行所有检查并生成综合报告 |

## 快速开始

### 一键检查

```powershell
# 运行所有检查
.\run-all-checks.ps1

# 静默模式（只输出汇总）
.\run-all-checks.ps1 -Quiet

# 指定报告目录
.\run-all-checks.ps1 -ReportDir "C:\reports"
```

### 单独运行某项检查

```powershell
# 数据库检查
.\check-database.ps1

# 带报告输出
.\check-database.ps1 -ReportPath "C:\reports\db-check.json"

# 指定自定义配置
.\check-database.ps1 -PgHost "192.168.1.100" -PgPort 5433 -PgUser "testuser"
```

## 检查项说明

### 数据库检查 (check-database.ps1)

**参数：**
- `-ConfigPath` - 数据库配置文件路径（默认：`I:\AI-Ready\backend\config\database.yml`）
- `-PgHost` - PostgreSQL主机（默认：localhost）
- `-PgPort` - PostgreSQL端口（默认：5432）
- `-PgUser` - 数据库用户（默认：postgres）
- `-PgPassword` - 密码（默认从环境变量`PGPASSWORD`读取）

**检查内容：**
1. 配置文件是否存在并可读
2. PostgreSQL Windows服务是否运行
3. 5432端口是否在监听
4. 数据库连接是否成功
5. 关键数据库是否存在（ai_ready, ai_ready_test, postgres）
6. 磁盘空间是否充足（>20%）

### 中间件检查 (check-middleware.ps1)

**参数：**
- `-ConfigDir` - 中间件配置目录（默认：`I:\AI-Ready\infra`）

**检查内容：**
1. Redis服务及6379端口
2. Nginx服务及80/443端口
3. RabbitMQ服务及5672/15672端口
4. Docker Desktop运行状态
5. 配置目录结构完整性

### 应用配置检查 (check-application.ps1)

**参数：**
- `-AppConfigDir` - 应用配置目录（默认：`I:\AI-Ready\backend\config`）

**检查内容：**
1. 关键配置文件存在性（application.yml, database.yml, redis.yml等）
2. 配置文件中关键属性完整性
3. 日志配置文件存在性
4. API文档注解检查
5. 健康检查端点配置

### 服务检查 (check-services.ps1)

**参数：**
- `-RequiredServices` - 必需服务列表（默认：postgresql, redis, nginx, rabbitmq）

**检查内容：**
1. Windows服务运行状态
2. Java/Spring Boot进程
3. Node.js前端进程
4. Docker Desktop进程

### 端口检查 (check-ports.ps1)

**参数：**
- `-ExpectedPorts` - 期望端口哈希表

**检查内容：**
1. 各服务端口监听状态
2. 端口冲突检测

### 连通性检查 (check-connectivity.ps1)

**检查内容：**
1. 本地服务端点TCP连通测试
2. DNS解析测试
3. Windows防火墙状态

## 报告格式

### JSON报告示例

```json
{
  "timestamp": "2026-04-29 12:35:00",
  "category": "database",
  "status": "PASS",
  "checks": [
    {
      "name": "配置文件存在性",
      "status": "PASS",
      "message": "配置文件存在: I:\\AI-Ready\\backend\\config\\database.yml"
    }
  ],
  "errors": []
}
```

### 综合报告

运行 `run-all-checks.ps1` 会生成包含所有检查类别的综合JSON报告：

```json
{
  "timestamp": "2026-04-29 12:35:00",
  "overallStatus": "PASS",
  "summary": {
    "total": 25,
    "passed": 20,
    "failed": 2,
    "warnings": 3
  },
  "categories": [ ... ]
}
```

## 退出码

| 退出码 | 含义 |
|--------|------|
| 0 | 所有检查通过 |
| 1 | 至少有一项检查失败 |

## CI/CD集成

在持续集成流程中使用：

```powershell
# 在构建前检查环境
& .\scripts\test-env\check-config\run-all-checks.ps1 -Quiet
if ($LASTEXITCODE -ne 0) {
    Write-Error "环境检查失败，中止构建"
    exit 1
}
```

## 故障排查

### 常见问题

1. **PowerShell执行策略限制**
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

2. **psql命令未找到**
   - 安装PostgreSQL客户端
   - 或将psql目录添加到PATH环境变量

3. **服务未找到**
   - 确认服务已安装
   - 检查服务名称是否匹配（脚本使用通配符匹配）

4. **权限不足**
   - 以管理员身份运行PowerShell

## 维护

### 添加新的检查项

1. 编辑对应的检查脚本
2. 使用 `Add-CheckResult` 函数添加检查结果
3. 保持JSON输出格式一致

### 修改端口或服务配置

编辑对应脚本顶部的参数默认值，或在运行时通过参数传入。
