# 测试环境自动化部署脚本使用手册

## 概述

本手册详细介绍了测试环境自动化部署脚本的使用方法、配置选项和最佳实践。

## 1. 环境初始化

### 1.1 环境预检查

**脚本文件**: `environment-init-check.ps1`

**功能**: 检查测试环境的基本配置、依赖和服务状态

**参数说明**:
```powershell
-verbose           # 显示详细输出
-configFile        # 指定配置文件路径（默认: .\deployment-config.json）
```

**检查项目**:
1. 操作系统要求
2. PowerShell版本
3. 内存和磁盘空间
4. 网络连接
5. 必需服务状态
6. 端口占用情况
7. 依赖工具检查
8. 目录权限检查
9. 环境变量检查

**输出文件**: `environment-precheck-report-<timestamp>.txt`

**示例**:
```powershell
# 基本检查
.\environment-init-check.ps1

# 详细检查
.\environment-init-check.ps1 -verbose

# 使用自定义配置
.\environment-init-check.ps1 -configFile ".\custom-config.json"
```

### 1.2 依赖安装

**脚本文件**: `dependency-installer.ps1`

**功能**: 自动安装测试环境所需的依赖

**参数说明**:
```powershell
-force             # 强制重新安装已存在的依赖
-skipJava          # 跳过Java安装
-skipNode          # 跳过Node.js安装
-skipDocker        # 跳过Docker安装
-skipGit           # 跳过Git安装
-installDir        # 指定安装目录（默认: C:\Program Files）
-tempDir           # 指定临时目录（默认: $env:TEMP）
```

**安装项目**:
1. Chocolatey包管理器
2. Git版本控制系统
3. Node.js运行时
4. Java开发工具包
5. Docker容器平台
6. PostgreSQL数据库
7. Redis缓存服务
8. 其他常用工具（VSCode, Postman, curl等）

**输出文件**: `dependency-installation-report-<timestamp>.txt`

**示例**:
```powershell
# 安装所有依赖
.\dependency-installer.ps1

# 跳过特定依赖
.\dependency-installer.ps1 -skipJava -skipDocker

# 强制重新安装
.\dependency-installer.ps1 -force

# 指定安装目录
.\dependency-installer.ps1 -installDir "D:\Programs"
```

### 1.3 目录结构创建

**脚本文件**: `directory-structure-creator.ps1`

**功能**: 创建标准化的测试环境目录结构

**参数说明**:
```powershell
-basePath          # 基础目录路径（默认: C:\AI-Ready-Test）
-configFile        # 目录配置文件（默认: .\directory-config.json）
-clean             # 清理现有目录
-verbose           # 显示详细输出
-dryRun            # 模拟运行，不实际创建
```

**目录结构**:
```
C:\AI-Ready-Test\
├── apps/              # 应用程序目录
│   ├── backend/       # 后端应用
│   ├── frontend/      # 前端应用
│   ├── mobile/        # 移动端应用
│   └── admin/         # 管理后台
├── data/              # 数据目录
│   ├── database/      # 数据库文件
│   ├── logs/          # 日志文件
│   ├── uploads/       # 上传文件
│   ├── temp/          # 临时文件
│   ├── backup/        # 备份文件
│   └── cache/         # 缓存文件
├── config/            # 配置文件目录
│   ├── environment/   # 环境配置
│   ├── security/      # 安全配置
│   ├── network/       # 网络配置
│   └── services/      # 服务配置
├── scripts/           # 脚本目录
│   ├── deployment/    # 部署脚本
│   ├── maintenance/   # 维护脚本
│   ├── monitoring/    # 监控脚本
│   └── backup/        # 备份脚本
├── tests/             # 测试目录
│   ├── unit/          # 单元测试
│   ├── integration/   # 集成测试
│   ├── e2e/           # 端到端测试
│   ├── performance/   # 性能测试
│   └── security/      # 安全测试
├── docs/              # 文档目录
│   ├── api/           # API文档
│   ├── user/          # 用户文档
│   ├── admin/         # 管理文档
│   ├── developer/     # 开发文档
│   └── deployment/    # 部署文档
├── tools/             # 工具目录
│   ├── database/      # 数据库工具
│   ├── monitoring/    # 监控工具
│   ├── backup/        # 备份工具
│   └── debug/         # 调试工具
└── reports/           # 报告目录
    ├── daily/         # 日报
    ├── weekly/        # 周报
    ├── monthly/       # 月报
    ├── performance/   # 性能报告
    └── security/      # 安全报告
```

**输出文件**:
- `structure-creation-report-<timestamp>.txt`
- `set-permissions.ps1`

**示例**:
```powershell
# 创建默认目录结构
.\directory-structure-creator.ps1

# 清理并重新创建
.\directory-structure-creator.ps1 -clean

# 指定基础路径
.\directory-structure-creator.ps1 -basePath "D:\AI-Ready-Test"

# 模拟运行
.\directory-structure-creator.ps1 -dryRun
```

### 1.4 权限配置

**脚本文件**: `permission-configurator.ps1`

**功能**: 为测试环境配置标准化的权限

**参数说明**:
```powershell
-basePath          # 基础目录路径（默认: C:\AI-Ready-Test）
-configFile        # 权限配置文件（默认: .\permission-config.json）
-apply             # 应用权限配置
-verify            # 验证权限配置
-verbose           # 显示详细输出
-dryRun            # 模拟运行，不实际修改
```

**默认权限规则**:
1. **apps目录**: 管理员完全控制，用户读取执行
2. **data目录**: 管理员完全控制，用户修改
3. **config目录**: 管理员完全控制，用户读取
4. **scripts目录**: 管理员完全控制，用户读取执行
5. **logs目录**: 管理员完全控制，用户修改
6. **backup目录**: 管理员完全控制，用户读取

**输出文件**:
- `permission-configuration-report-<timestamp>.txt`
- `apply-permissions.ps1`

**示例**:
```powershell
# 应用权限配置
.\permission-configurator.ps1 -apply

# 验证权限
.\permission-configurator.ps1 -verify

# 应用并验证
.\permission-configurator.ps1 -apply -verify

# 详细输出
.\permission-configurator.ps1 -apply -verbose
```

## 2. 服务部署脚本（待实现）

### 2.1 服务启动脚本
- **`service-starter.ps1`** - 启动所有服务
- **`service-stopper.ps1`** - 停止所有服务
- **`service-restarter.ps1`** - 重启所有服务
- **`service-checker.ps1`** - 检查服务状态

### 2.2 配置更新脚本
- **`config-updater.ps1`** - 配置热更新
- **`config-version-manager.ps1`** - 配置版本管理
- **`config-rollback.ps1`** - 配置回滚
- **`config-diff-checker.ps1`** - 配置差异比较

### 2.3 环境清理脚本
- **`temp-cleaner.ps1`** - 临时文件清理
- **`log-cleaner.ps1`** - 日志清理
- **`cache-cleaner.ps1`** - 缓存清理
- **`environment-resetter.ps1`** - 环境重置

## 3. 配置文件说明

### 3.1 部署配置（deployment-config.json）

```json
{
  "environment": {
    "basePath": "C:\\AI-Ready-Test",
    "tempDir": "C:\\Temp",
    "logDir": "C:\\Logs\\AI-Ready"
  },
  "services": {
    "web": {
      "port": 8080,
      "healthCheckPath": "/health",
      "startupTimeout": 60
    },
    "database": {
      "type": "postgresql",
      "port": 5432,
      "maxConnections": 100
    },
    "cache": {
      "type": "redis",
      "port": 6379,
      "maxMemory": "1GB"
    }
  },
  "dependencies": {
    "required": ["git", "nodejs", "java", "docker"],
    "optional": ["postgresql", "redis", "nginx", "rabbitmq"],
    "versions": {
      "nodejs": ">=18.0.0",
      "java": ">=11",
      "docker": ">=24.0.0"
    }
  }
}
```

### 3.2 权限配置（permission-config.json）

```json
{
  "basePath": "C:\\AI-Ready-Test",
  "permissions": [
    {
      "path": "apps",
      "description": "应用程序目录",
      "accessRules": [
        {
          "Identity": "BUILTIN\\Administrators",
          "Permission": "FullControl",
          "Type": "Allow"
        },
        {
          "Identity": "BUILTIN\\Users",
          "Permission": "ReadAndExecute",
          "Type": "Allow"
        }
      ],
      "inheritance": true
    }
  ]
}
```

## 4. 执行流程

### 4.1 标准部署流程

```powershell
# 步骤1: 环境预检查
.\environment-init-check.ps1 -verbose

# 步骤2: 安装依赖
.\dependency-installer.ps1

# 步骤3: 创建目录结构
.\directory-structure-creator.ps1 -clean

# 步骤4: 配置权限
.\permission-configurator.ps1 -apply -verify

# 步骤5: 验证部署
.\environment-init-check.ps1 -verbose
```

### 4.2 快速部署流程

```powershell
# 一键部署
.\environment-init-check.ps1
.\dependency-installer.ps1
.\directory-structure-creator.ps1 -clean
.\permission-configurator.ps1 -apply
```

### 4.3 增量更新流程

```powershell
# 仅更新依赖
.\dependency-installer.ps1

# 仅更新权限
.\permission-configurator.ps1 -apply
```

## 5. 最佳实践

### 5.1 环境隔离
- 为每个测试环境创建独立的目录
- 使用不同的端口和服务配置
- 隔离数据库和缓存实例

### 5.2 配置管理
- 将配置文件纳入版本控制
- 为不同环境提供不同的配置
- 定期备份配置文件

### 5.3 权限管理
- 遵循最小权限原则
- 定期审计权限配置
- 使用服务账户运行服务

### 5.4 监控和日志
- 启用服务监控
- 配置集中式日志收集
- 设置告警规则

## 6. 故障排除

### 6.1 常见问题

**问题1: 权限不足**
```
解决方案:
- 以管理员身份运行PowerShell
- 检查用户账户控制设置
- 验证目录所有权
```

**问题2: 网络连接失败**
```
解决方案:
- 检查防火墙设置
- 验证代理配置
- 使用离线安装包
```

**问题3: 依赖安装失败**
```
解决方案:
- 检查磁盘空间
- 验证网络连接
- 使用特定版本安装
```

**问题4: 服务启动失败**
```
解决方案:
- 检查端口占用
- 验证配置文件
- 查看服务日志
```

### 6.2 日志文件位置

1. **脚本执行日志**: 各脚本生成的报告文件
2. **系统事件日志**: Windows事件查看器
3. **服务日志**: 各服务的日志目录
4. **安装日志**: Chocolatey安装日志

### 6.3 调试方法

```powershell
# 启用详细日志
.\script.ps1 -verbose

# 模拟运行
.\script.ps1 -dryRun

# 逐步执行
Set-PSDebug -Trace 1
.\script.ps1
Set-PSDebug -Trace 0
```

## 7. 性能优化

### 7.1 脚本优化
- 减少不必要的网络请求
- 使用本地缓存
- 并行执行独立任务

### 7.2 资源优化
- 合理分配内存和CPU
- 优化磁盘I/O
- 控制并发连接数

### 7.3 网络优化
- 使用本地镜像源
- 启用压缩传输
- 优化DNS解析

## 8. 安全考虑

### 8.1 访问控制
- 限制管理访问
- 使用强密码策略
- 定期轮换凭证

### 8.2 数据保护
- 加密敏感数据
- 安全备份策略
- 数据脱敏处理

### 8.3 网络安全
- 配置防火墙规则
- 启用SSL/TLS
- 监控网络流量

## 9. 维护计划

### 9.1 日常维护
- 检查服务状态
- 清理临时文件
- 备份重要数据

### 9.2 定期维护
- 更新依赖版本
- 审计权限配置
- 优化性能设置

### 9.3 应急维护
- 故障恢复流程
- 数据恢复方案
- 回滚操作指南

## 10. 附录

### 10.1 脚本参数参考

| 参数 | 说明 | 默认值 |
|------|------|--------|
| -verbose | 显示详细输出 | $false |
| -configFile | 配置文件路径 | 脚本目录 |
| -basePath | 基础目录路径 | C:\AI-Ready-Test |
| -clean | 清理现有目录 | $false |
| -dryRun | 模拟运行 | $false |
| -force | 强制重新安装 | $false |
| -apply | 应用配置 | $false |
| -verify | 验证配置 | $false |

### 10.2 文件结构参考

```
scripts/deployment/
├── environment-init-check.ps1      # 环境预检查
├── dependency-installer.ps1        # 依赖安装
├── directory-structure-creator.ps1 # 目录创建
├── permission-configurator.ps1     # 权限配置
├── deployment-config.json          # 部署配置
├── permission-config.json          # 权限配置
└── README.md                      # 使用说明
```

### 10.3 联系支持

如有问题或建议，请联系：
- 项目负责人: coordinator@ai-ready.local
- 技术支持: support@ai-ready.local
- 文档维护: docs@ai-ready.local

---

**文档版本**: v1.0.0  
**最后更新**: 2026-04-27  
**维护团队**: AI-Ready DevOps Team