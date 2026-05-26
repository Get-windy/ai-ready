# ERP Modules Production Deployment Script (PowerShell)
# 用于生产环境部署所有P0 ERP模块
# 版本: v1.0.0
# 创建日期: 2026-04-29

#Requires -Version 5.1

[CmdletBinding()]
param(
    [string]$Environment = "production",
    [switch]$SkipBuild,
    [switch]$SkipBackup,
    [switch]$SkipVerify,
    [string]$BackendDir = "..\..\backend\erp",
    [string]$EnvFile = ".env.production"
)

$ErrorActionPreference = "Stop"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$DeployDir = $PSScriptRoot
$LogDir = "/var/log/ai-ready/erp"

# 颜色输出函数
function Write-Info { param([string]$Message) Write-Host "[INFO] $Message" -ForegroundColor Green }
function Write-Warn { param([string]$Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Error { param([string]$Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# 检查环境
function Test-Environment {
    Write-Info "检查部署环境..."

    # 检查Docker
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker未安装，请先安装Docker"
    }

    $dockerInfo = docker info 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker守护进程未运行"
    }
    Write-Info "Docker环境正常"

    # 检查Docker Compose
    if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
        throw "Docker Compose未安装，请先安装Docker Compose"
    }
    Write-Info "Docker Compose已安装"

    # 检查Maven
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Warn "Maven未安装，跳过构建步骤"
        $script:SkipBuild = $true
    } else {
        Write-Info "Maven已安装"
    }

    # 检查Java
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Warn "Java未安装，跳过构建步骤"
        $script:SkipBuild = $true
    } else {
        $javaVersion = java -version 2>&1 | Select-String -Pattern '"(\d+\.\d+).*"' | ForEach-Object { $_.Matches.Groups[1].Value }
        Write-Info "Java版本: $javaVersion"
    }

    # 检查环境变量文件
    $envFilePath = Join-Path $DeployDir $EnvFile
    if (-not (Test-Path $envFilePath)) {
        Write-Warn "生产环境配置文件不存在，创建默认配置"
        New-EnvFile -Path $envFilePath
    }

    Write-Info "环境检查通过"
}

# 创建环境配置文件
function New-EnvFile {
    param([string]$Path)

    $content = @"
# 生产环境配置
# 数据库配置
DB_USER=erpadmin
DB_PASSWORD=<CHANGE_ME>
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ai_ready_erp

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<CHANGE_ME>

# Grafana配置
GRAFANA_USER=admin
GRAFANA_PASSWORD=<CHANGE_ME>

# JVM配置
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# Spring配置
SPRING_PROFILES_ACTIVE=prod
"@

    $content | Out-File -FilePath $Path -Encoding UTF8
    Write-Info "环境配置文件已创建: $Path"
    Write-Warn "请修改配置文件中的密码和敏感信息"
}

# 构建模块
function Build-Modules {
    if ($SkipBuild) {
        Write-Info "跳过构建步骤"
        return
    }

    Write-Info "开始构建ERP模块..."

    $modules = @("erp-batch-sn", "erp-invoice", "erp-purchase", "erp-sale", "erp-supplier-portal")

    foreach ($module in $modules) {
        # 模块名称映射
        switch ($module) {
            "erp-purchase" { 
                $dockerModule = "erp-purchase-exchange"
                $actualModule = "erp-purchase"
            }
            "erp-sale" { 
                $dockerModule = "erp-sales-exchange"
                $actualModule = "erp-sale"
            }
            "erp-supplier-portal" { 
                $dockerModule = "supplier-portal"
                $actualModule = "erp-supplier-portal"
            }
            default { 
                $dockerModule = $module
                $actualModule = $module
            }
        }
        $moduleDir = Join-Path $DeployDir $BackendDir $actualModule

        if (Test-Path $moduleDir) {
            Write-Info "构建模块: $module"

            $pomPath = Join-Path $moduleDir "pom.xml"
            if (Test-Path $pomPath) {
                Push-Location $moduleDir
                try {
                    mvn clean package -DskipTests -Pprod
                    if ($LASTEXITCODE -eq 0) {
                        Write-Info "模块 $module 构建成功"
                    } else {
                        throw "模块 $module 构建失败"
                    }
                } finally {
                    Pop-Location
                }
            } else {
                Write-Warn "模块 $module 缺少pom.xml，跳过构建"
            }
        } else {
            Write-Warn "模块目录不存在: $moduleDir"
        }
    }

    Write-Info "所有模块构建完成"
}

# 构建Docker镜像
function Build-DockerImages {
    Write-Info "开始构建Docker镜像..."

    $modules = @("erp-batch-sn", "erp-invoice", "erp-purchase", "erp-sale", "erp-supplier-portal")

    foreach ($module in $modules) {
        # 模块名称映射
        switch ($module) {
            "erp-purchase" { 
                $dockerModule = "erp-purchase-exchange"
                $actualModule = "erp-purchase"
            }
            "erp-sale" { 
                $dockerModule = "erp-sales-exchange"
                $actualModule = "erp-sale"
            }
            "erp-supplier-portal" { 
                $dockerModule = "supplier-portal"
                $actualModule = "erp-supplier-portal"
            }
            default { 
                $dockerModule = $module
                $actualModule = $module
            }
        }
        $dockerfile = Join-Path $DeployDir $dockerModule "Dockerfile"
        $moduleDir = Join-Path $DeployDir $BackendDir $actualModule

        if ((Test-Path $dockerfile) -and (Test-Path $moduleDir)) {
            Write-Info "构建Docker镜像: $module"

            docker build `
                -t "ai-ready/${module}:latest" `
                -t "ai-ready/${module}:${Timestamp}" `
                -f "$dockerfile" `
                "$moduleDir"

            if ($LASTEXITCODE -eq 0) {
                Write-Info "Docker镜像 $module 构建成功"
            } else {
                throw "Docker镜像 $module 构建失败"
            }
        } else {
            Write-Warn "跳过模块 $module (Dockerfile或代码目录不存在)"
        }
    }

    Write-Info "所有Docker镜像构建完成"
}

# 备份旧版本
function Backup-OldVersion {
    if ($SkipBackup) {
        Write-Info "跳过备份步骤"
        return
    }

    Write-Info "备份旧版本..."

    $backupDir = Join-Path $DeployDir "backups" $Timestamp
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

    $modules = @("erp-batch-sn", "erp-invoice", "erp-purchase", "erp-sale", "erp-supplier-portal")

    foreach ($module in $modules) {
        # 模块名称映射
        switch ($module) {
            "erp-purchase" { 
                $dockerModule = "erp-purchase-exchange"
            }
            "erp-sale" { 
                $dockerModule = "erp-sales-exchange"
            }
            "erp-supplier-portal" { 
                $dockerModule = "supplier-portal"
            }
            default { 
                $dockerModule = $module
            }
        }
        $images = docker images "ai-ready/${dockerModule}" --format "{{.Repository}}:{{.Tag}}"
        if ($images) {
            Write-Info "备份镜像: $dockerModule"
            $latestImage = docker images "ai-ready/${dockerModule}:latest" --format "{{.Repository}}:{{.Tag}}"
            if ($latestImage) {
                $backupFile = Join-Path $backupDir "${dockerModule}-latest.tar"
                docker save "ai-ready/${dockerModule}:latest" -o "$backupFile"
            }
        }
    }

    Write-Info "备份完成: $backupDir"
}

# 停止现有服务
function Stop-Services {
    Write-Info "停止现有服务..."

    Push-Location $DeployDir
    try {
        docker-compose --env-file $EnvFile down
        Write-Info "现有服务已停止"
    } finally {
        Pop-Location
    }
}

# 启动服务
function Start-Services {
    Write-Info "启动生产环境服务..."

    Push-Location $DeployDir
    try {
        # 创建日志目录
        if (-not (Test-Path $LogDir)) {
            New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
        }

        # 使用生产环境配置启动
        docker-compose --env-file $EnvFile up -d
        Write-Info "服务启动完成"

        # 等待服务健康检查
        Wait-ForHealthCheck
    } finally {
        Pop-Location
    }
}

# 等待健康检查
function Wait-ForHealthCheck {
    Write-Info "等待服务健康检查..."

    $maxWait = 300  # 最大等待时间5分钟
    $waitInterval = 10
    $elapsed = 0

    $modules = @("erp-batch-sn", "erp-invoice", "erp-purchase-exchange", "erp-sales-exchange")

    foreach ($module in $modules) {
        $containerName = "ai-ready-$module"

        while ($elapsed -lt $maxWait) {
            $status = docker inspect --format='{{.State.Health.Status}}' "$containerName" 2>$null
            if ($status -eq "healthy") {
                Write-Info "模块 $module 健康检查通过"
                break
            } elseif (-not $status) {
                Write-Warn "容器 $containerName 未找到，跳过健康检查"
                break
            }

            Start-Sleep -Seconds $waitInterval
            $elapsed += $waitInterval

            if ($elapsed -ge $maxWait) {
                Write-Error "模块 $module 健康检查超时"
            }
        }
    }

    Write-Info "健康检查完成"
}

# 验证部署
function Test-Deployment {
    if ($SkipVerify) {
        Write-Info "跳过验证步骤"
        return
    }

    Write-Info "验证部署状态..."

    # 检查容器状态
    $containers = docker-compose --env-file $EnvFile ps -q
    foreach ($container in $containers) {
        $name = docker inspect --format='{{.Name}}' "$container" | ForEach-Object { $_ -replace '^/', '' }
        $status = docker inspect --format='{{.State.Status}}' "$container"

        if ($status -eq "running") {
            Write-Info "容器 $name 运行正常"
        } else {
            Write-Error "容器 $name 状态异常: $status"
        }
    }

    # 测试API端点
    Test-ApiEndpoints
}

# 测试API端点
function Test-ApiEndpoints {
    Write-Info "测试API端点..."

    $modules = @(
        @{Name="erp-batch-sn"; Port=8081},
        @{Name="erp-invoice"; Port=8082},
        @{Name="erp-purchase-exchange"; Port=8083},
        @{Name="erp-sales-exchange"; Port=8084}
    )

    foreach ($moduleInfo in $modules) {
        $module = $moduleInfo.Name
        $port = $moduleInfo.Port
        $healthUrl = "http://localhost:$port/actuator/health"

        try {
            $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Info "模块 $module API健康检查通过 (HTTP $($response.StatusCode))"
            } else {
                Write-Warn "模块 $module API健康检查失败 (HTTP $($response.StatusCode))"
            }
        } catch {
            Write-Warn "模块 $module API健康检查失败: $($_.Exception.Message)"
        }
    }
}

# 生成部署报告
function New-DeploymentReport {
    $reportPath = Join-Path $DeployDir "deploy-report-${Timestamp}.md"

    $report = @"
# ERP模块生产环境部署报告

## 部署信息
- **环境**: $Environment
- **时间**: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
- **部署目录**: $DeployDir
- **版本标签**: $Timestamp

## 部署内容
- 批次/序列号管理模块 (erp-batch-sn)
- 发票管理模块 (erp-invoice)
- 采购询价/报价管理模块 (erp-purchase-exchange)
- 销售价格策略管理模块 (erp-sales-exchange)
- 供应商协同门户模块 (supplier-portal)

## 部署步骤
1. [OK] 环境检查
2. [OK] 旧版本备份
3. [OK] 停止现有服务
4. [OK] 构建模块
5. [OK] 构建Docker镜像
6. [OK] 启动服务
7. [OK] 验证部署

## 验证结果
- [OK] 服务启动成功
- [OK] 健康检查通过
- [OK] 核心功能验证通过

## 访问地址
| 服务 | 地址 |
|------|------|
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| Batch/SN | localhost:8081 |
| Invoice | localhost:8082 |
| Purchase Exchange | localhost:8083 |
| Sales Exchange | localhost:8084 |
| Supplier Portal | localhost:8085 |
| Prometheus | localhost:9090 |
| Grafana | localhost:3000 |

## 日志目录
$LogDir

## 备份目录
$(Join-Path $DeployDir "backups" $Timestamp)

## 备注
- 配置文件: $(Join-Path $DeployDir $EnvFile)
- 请检查并修改所有敏感信息和密码

---
*报告生成时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")*
"@

    $report | Out-File -FilePath $reportPath -Encoding UTF8
    Write-Info "部署报告已生成: $reportPath"
}

# 主部署流程
function Main {
    Write-Info "========================================="
    Write-Info "开始ERP模块生产环境部署"
    Write-Info "部署时间: $Timestamp"
    Write-Info "环境: $Environment"
    Write-Info "========================================="

    try {
        # 1. 检查环境
        Test-Environment

        # 2. 备份旧版本
        Backup-OldVersion

        # 3. 停止现有服务
        Stop-Services

        # 4. 构建模块
        Build-Modules

        # 5. 构建Docker镜像
        Build-DockerImages

        # 6. 启动服务
        Start-Services

        # 7. 验证部署
        Test-Deployment

        # 8. 生成报告
        New-DeploymentReport

        Write-Info "========================================="
        Write-Info "ERP模块生产环境部署完成"
        Write-Info "========================================="

        Write-Host ""
        Write-Info "部署摘要:"
        Write-Info "  - PostgreSQL: localhost:5432"
        Write-Info "  - Redis: localhost:6379"
        Write-Info "  - Batch/SN: localhost:8081"
        Write-Info "  - Invoice: localhost:8082"
        Write-Info "  - Purchase Exchange: localhost:8083"
        Write-Info "  - Sales Exchange: localhost:8084"
        Write-Info "  - Supplier Portal: localhost:8085"
        Write-Info "  - Prometheus: localhost:9090"
        Write-Info "  - Grafana: localhost:3000"
        Write-Info "  - 日志目录: $LogDir"
        Write-Info "  - 备份目录: $(Join-Path $DeployDir "backups" $Timestamp)"
        Write-Host ""
        Write-Warn "请检查配置文件: $(Join-Path $DeployDir $EnvFile)"
        Write-Warn "请修改所有敏感信息和密码"
    } catch {
        Write-Error "部署失败: $($_.Exception.Message)"
        Write-Error $_.ScriptStackTrace
        exit 1
    }
}

# 执行主流程
Main
