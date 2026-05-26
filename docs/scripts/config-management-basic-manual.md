# AI-Ready 测试环境配置管理脚本使用手册

## 目录

1. [概述](#概述)
2. [脚本结构](#脚本结构)
3. [快速开始](#快速开始)
4. [环境变量管理](#环境变量管理)
5. [配置文件管理](#配置文件管理)
6. [高级功能](#高级功能)
7. [常见问题](#常见问题)

## 概述

AI-Ready测试环境配置管理脚本是一套PowerShell工具，用于简化测试环境的配置管理。主要功能包括：

- **环境变量管理**：读取、验证、生成、备份环境变量配置
- **配置文件生成**：Docker Compose、Application、Nginx等配置模板
- **配置验证**：YAML、JSON、Env配置文件验证
- **配置备份与还原**：保护配置不丢失

## 脚本结构

```
scripts/config-management/
├── basic/                              # 基础脚本目录
│   ├── env-manager.ps1                # 环境变量管理脚本
│   ├── config-manager.ps1             # 配置文件管理脚本
│   └── basic-config.ps1               # 基础配置工具
├── config/                             # 配置文件目录
│   ├── environment.json               # 环境变量配置
│   ├── appsettings.json               # 应用配置
│   └── .env                           # Docker环境变量
├── backups/                            # 备份目录
│   ├── env/                           # 环境变量备份
│   └── config/                        # 配置文件备份
└── logs/                               # 日志目录
    ├── env-manager-YYYY-MM.log        # 环境变量日志
    └── config-manager-YYYY-MM.log     # 配置管理日志
```

## 快速开始

### 前提条件

- PowerShell 5.1 或更高版本
- Windows操作系统
- 适当的文件系统权限

### 第一次使用

1. **解压或克隆脚本到目标位置**

```powershell
# 假设脚本在 I:\AI-Ready\scripts\config-management\
cd I:\AI-Ready\scripts\config-management\basic\
```

2. **初始化环境**

```powershell
# 创建必要的目录
.\basic-config.ps1 app-config test

# 初始化Docker环境变量
.\basic-config.ps1 docker-env test
```

3. **验证配置**

```powershell
# 验证应用配置
.\basic-config.ps1 validate .\config\appsettings.json

# 验证Docker环境变量
.\basic-config.ps1 validate .\config\.env
```

## 环境变量管理

### 1. 环境变量读取

```powershell
# 使用 env-manager.ps1 读取配置
.\env-manager.ps1 read -ConfigFile environment.json
```

### 2. 环境变量验证

```powershell
# 验证配置文件
.\env-manager.ps1 validate -ConfigFile environment.json -Env test

# 检查特定环境配置
.\env-manager.ps1 get -Key "test.database.host" -ConfigFile environment.json
```

### 3. 环境变量模板生成

```powershell
# 生成开发环境模板
.\env-manager.ps1 init -Env development -OutputFile environment.json

# 生成测试环境模板
.\env-manager.ps1 init -Env test -OutputFile environment.json

# 生成生产环境模板
.\env-manager.ps1 init -Env production -OutputFile environment.json
```

### 4. 环境变量备份

```powershell
# 备份当前配置
.\env-manager.ps1 backup -ConfigFile environment.json

# 还原配置
.\env-manager.ps1 restore -BackupFile .\backups\env\env-backup-20260427-120000.json
```

### 5. 环境变量同步

```powershell
# 从测试环境同步到开发环境
.\env-manager.ps1 sync -SourceEnv test -TargetEnv development
```

## 配置文件管理

### 1. 配置模板生成

```powershell
# 生成Docker Compose配置
.\config-manager.ps1 template docker-compose

# 生成Application配置
.\config-manager.ps1 template application

# 生成Nginx配置
.\config-manager.ps1 template nginx

# 生成Redis配置
.\config-manager.ps1 template redis

# 生成PostgreSQL配置
.\config-manager.ps1 template postgresql
```

### 2. 配置验证

```powershell
# 验证YAML配置
.\config-manager.ps1 validate -FilePath .\templates\docker-compose.yml -Type yml

# 验证JSON配置
.\config-manager.ps1 validate -FilePath .\config\appsettings.json -Type json

# 验证Env配置
.\config-manager.ps1 validate -FilePath .\config\.env -Type env
```

### 3. 配置备份

```powershell
# 备份单个配置文件
.\config-manager.ps1 backup -ConfigFile docker-compose.yml

# 备份所有配置文件
.\config-manager.ps1 backup

# 还原配置
.\config-manager.ps1 restore -BackupFile .\backups\config\config-backup-20260427-120000.yml
```

### 4. 显示配置

```powershell
# 显示配置文件内容
.\config-manager.ps1 show -ConfigFile docker-compose.yml

# 列出所有配置文件
.\config-manager.ps1 list
```

## 高级功能

### 1. 快速配置（basic-config.ps1）

```powershell
# 创建应用配置
.\basic-config.ps1 app-config -Env test

# 创建Docker环境变量
.\basic-config.ps1 docker-env -Env production

# 验证配置
.\basic-config.ps1 validate -FilePath .\config\appsettings.json

# 查看配置摘要
.\basic-config.ps1 summary -ConfigDir .\config
```

### 2. 批量操作

```powershell
# 批量生成所有环境模板
$environments = @('development', 'test', 'production')
foreach ($env in $environments) {
    Write-Host "生成 $env 环境配置..."
    .\env-manager.ps1 init -Env $env
    .\basic-config.ps1 app-config -Env $env
    .\basic-config.ps1 docker-env -Env $env
}
```

### 3. 自动化脚本集成

```powershell
# 配置管理自动化示例
$script:CONFIG_VERSION = "1.0.0"

function Deploy-TestEnvironment {
    param (
        [string] $Environment = "test"
    )
    
    Write-Host "=== 部署测试环境: $Environment ===" -ForegroundColor Cyan
    
    # 1. 初始化配置
    Write-Host "初始化环境配置..." -ForegroundColor Yellow
    .\env-manager.ps1 init -Env $Environment
    .\basic-config.ps1 docker-env -Env $Environment
    
    # 2. 验证配置
    Write-Host "验证配置文件..." -ForegroundColor Yellow
    $configValid = .\basic-config.ps1 validate -FilePath .\config\.env
    if (-not $configValid) {
        Write-Host "配置验证失败！" -ForegroundColor Red
        return $false
    }
    
    # 3. 备份当前配置
    Write-Host "备份当前配置..." -ForegroundColor Yellow
    .\env-manager.ps1 backup
    .\config-manager.ps1 backup
    
    # 4. 部署
    Write-Host "执行部署..." -ForegroundColor Yellow
    # ... 部署逻辑 ...
    
    Write-Host "部署完成！" -ForegroundColor Green
    return $true
}

# 使用示例
Deploy-TestEnvironment -Environment "test"
```

### 4. 配置检查清单

在部署前使用以下检查清单：

- [ ] 环境变量配置文件存在（environment.json）
- [ ] Docker环境变量文件存在（.env）
- [ ] 应用配置文件存在（appsettings.json）
- [ ] 所有配置文件通过验证
- [ ] 敏感信息已替换（密码、密钥等）
- [ ] 数据库连接字符串正确
- [ ] Redis连接配置正确
- [ ] 日志目录已创建
- [ ] 配置已备份

## 常见问题

### Q1: 如何处理配置文件中的敏感信息？

**A:** 禁止将包含真实密码的配置文件提交到版本控制系统。使用以下方法：

1. **使用模板文件**

```bash
# .env.example (提交到git)
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_password
REDIS_PASSWORD=your_redis_password
```

```bash
# .env (不提交到git)
POSTGRES_USER=actual_user
POSTGRES_PASSWORD=actual_password
REDIS_PASSWORD=actual_redis_password
```

2. **使用环境变量**

```powershell
# 从系统环境变量读取
$postgresPassword = $env:POSTGRES_PASSWORD
if ([string]::IsNullOrEmpty($postgresPassword)) {
    $postgresPassword = Read-Host "请输入PostgreSQL密码"
}
```

3. **使用配置文件加密**

对于更高安全要求，可以使用PowerShell的 `ConvertFrom-SecureString` 和 `ConvertTo-SecureString`：

```powershell
# 加密密码
$securePassword = Read-Host "请输入密码" -AsSecureString
$encryptedPassword = ConvertFrom-SecureString $securePassword
$encryptedPassword | Out-File "secret.txt"

# 解密密码
$encryptedPassword = Get-Content "secret.txt"
$securePassword = ConvertTo-SecureString $encryptedPassword
$credential = New-Object System.Management.Automation.PSCredential("user", $securePassword)
```

### Q2: 如何在不同环境之间切换？

**A:** 修改 environment.json 中的 environment 字段：

```json
{
  "projectName": "AI-Ready",
  "version": "1.0.0",
  "environment": "test",
  ...
}
```

或修改 .env 文件中的相关配置：

```bash
# .env
SPRING_PROFILES_ACTIVE=test
APP_ENVIRONMENT=test
```

### Q3: 如何恢复误操作的配置？

**A:** 配置文件会自动备份到 `backups/` 目录：

```powershell
# 查看备份文件
ls .\backups\env\

# 还原配置
.\env-manager.ps1 restore -BackupFile .\backups\env\env-backup-20260427-120000.json

# 或使用配置管理脚本
.\config-manager.ps1 restore -BackupFile .\backups\config\config-backup-20260427-120000.yml
```

### Q4: 配置验证失败怎么办？

**A:** 根据验证结果检查：

1. **JSON格式错误**

```powershell
# 使用在线JSON验证器检查格式
# 或使用 PowerShell 检查
$content = Get-Content .\config\appsettings.json -Raw
try {
    $json = $content | ConvertFrom-Json
    Write-Host "JSON格式正确" -ForegroundColor Green
} catch {
    Write-Host "JSON格式错误: $_" -ForegroundColor Red
}
```

2. **YAML格式问题**

确保缩进正确，使用空格而非Tab：

```yaml
# 正确的缩进
services:
  postgresql:
    image: postgres:14
    ports:
      - "5432:5432"
```

3. **Env格式问题**

确保每行都是 KEY=VALUE 格式，空行或以#开头的为注释：

```bash
# 正确格式
POSTGRES_USER=postgres
POSTGRES_PASSWORD=yourpassword

# 这是注释
REDIS_HOST=localhost
```

### Q5: 如何自动化配置管理？

**A:** 创建自动化脚本 `deploy.ps1`：

```powershell
param (
    [ValidateSet('development', 'test', 'production')] [string] $Environment = "test"
)

Write-Host "=== 部署自动化脚本 ===" -ForegroundColor Cyan
Write-Host "环境: $Environment" -ForegroundColor Yellow

# 导入配置管理模块（如果需要）
# . .\env-manager.ps1
# . .\config-manager.ps1
# . .\basic-config.ps1

# 执行部署流程
try {
    # 1. 验证环境
    Write-Host "步骤1/4: 验证环境..." -ForegroundColor Yellow
    $configValid = .\basic-config.ps1 validate -FilePath .\config\.env
    if (-not $configValid) {
        throw "配置验证失败"
    }
    
    # 2. 备份配置
    Write-Host "步骤2/4: 备份配置..." -ForegroundColor Yellow
    .\env-manager.ps1 backup
    .\config-manager.ps1 backup
    
    # 3. 部署服务
    Write-Host "步骤3/4: 部署服务..." -ForegroundColor Yellow
    docker-compose -f .\docker-compose.yml -f .\docker-compose.$Environment.yml up -d
    
    # 4. 验证部署
    Write-Host "步骤4/4: 验证部署..." -ForegroundColor Yellow
    # ... 部署验证逻辑 ...
    
    Write-Host "部署完成！" -ForegroundColor Green
}
catch {
    Write-Host "部署失败: $_" -ForegroundColor Red
    exit 1
}
```

### Q6: 如何管理多个项目的配置？

**A:** 使用项目特定的目录结构：

```
projects/
├── project1/
│   └── config-management/
│       ├── env-manager.ps1
│       ├── config-manager.ps1
│       └── config/
└── project2/
    └── config-management/
        ├── env-manager.ps1
        ├── config-manager.ps1
        └── config/
```

在项目根目录创建 `config-link.ps1`：

```powershell
# 链接到共享配置管理脚本
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sharedConfigPath = "I:\AI-Ready\scripts\config-management\basic"

# 创建符号链接（需要管理员权限）
New-Item -ItemType SymbolicLink -Path "$scriptDir\config-management" -Target "$sharedConfigPath"
```

## 支持

如遇到问题，请检查：

1. PowerShell版本：`$PSVersionTable.PSVersion`
2. 文件权限：确保有读写权限
3. 路径长度：Windows路径不能超过260字符
4. 防病毒软件：可能阻止脚本执行

### 获取帮助

```powershell
# 获取命令帮助
Get-Help .\env-manager.ps1 -Detailed
Get-Help .\config-manager.ps1 -Detailed
Get-Help .\basic-config.ps1 -Detailed
```

## 更新日志

### v1.0.0 (2026-04-27)

- 初始版本发布
- 支持环境变量管理
- 支持配置文件生成和验证
- 支持配置备份和还原

---

**作者**: AI-Ready Team  
**版本**: 1.0.0  
**最后更新**: 2026-04-27
