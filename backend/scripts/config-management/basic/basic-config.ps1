#================================================================================
# 基础配置管理工具
# 功能：快速创建和验证配置文件
# 版本：1.0.0
# 作者：AI-Ready Team
# 日期：2026-04-27
#================================================================================

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$CONFIG_DIR = Join-Path $SCRIPT_DIR "..\config"
$LOG_DIR = Join-Path $SCRIPT_DIR "..\logs"

# 确保目录存在
function Ensure-Directories {
    if (-not (Test-Path $CONFIG_DIR)) {
        New-Item -ItemType Directory -Path $CONFIG_DIR | Out-Null
    }
    if (-not (Test-Path $LOG_DIR)) {
        New-Item -ItemType Directory -Path $LOG_DIR | Out-Null
    }
}

# 日志记录
function Write-Log {
    param (
        [Parameter(Mandatory = $true)] [string] $Message,
        [ValidateSet('INFO', 'WARN', 'ERROR', 'SUCCESS')] [string] $Level = 'INFO'
    )
    
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $logMessage = "[$timestamp] [$Level] $Message"
    
    Write-Host $logMessage -ForegroundColor (Get-ForegroundColor $Level)
    
    $logFile = Join-Path $LOG_DIR "basic-config-$(Get-Date -Format 'yyyy-MM').log"
    $logMessage | Out-File -FilePath $logFile -Append
}

function Get-ForegroundColor {
    param ([string] $Level)
    switch ($Level) {
        'INFO' { 'White' }
        'WARN' { 'Yellow' }
        'ERROR' { 'Red' }
        'SUCCESS' { 'Green' }
        default { 'White' }
    }
}

# 创建应用配置文件
function New-AppConfig {
    param (
        [ValidateSet('development', 'test', 'production')] [string] $Env = "test",
        [string] $FileName = "appsettings.json"
    )
    
    Ensure-Directories
    
    $config = @{
        "Logging" = @{
            "LogLevel" = @{
                "Default" = if ($Env -eq "production") { "Warning" } else { "Debug" }
                "Microsoft" = "Warning"
            }
        },
        "ConnectionStrings" = @{
            "DefaultConnection" = "Server=localhost;Database=aiready_$Env;User Id=postgres;Password=yourpassword;"
        },
        "Redis" = @{
            "Host" = "localhost"
            "Port" = 6379
        },
        "Jwt" = @{
            "Secret" = "your-secret-key-change-in-production"
            "ExpirationMinutes" = 1440
        }
    }
    
    if ($Env -eq "test") {
        $config.TestSettings = @{
            "EnableDebug" = $true
            "EnableCors" = $true
        }
    }
    
    $json = $config | ConvertTo-Json -Depth 3
    $outputPath = Join-Path $CONFIG_DIR $FileName
    $json | Out-File -FilePath $outputPath -Encoding UTF8
    
    Write-Log "配置文件创建成功: $outputPath" "SUCCESS"
    return $outputPath
}

# 创建Docker环境变量文件
function New-DockerEnv {
    param (
        [ValidateSet('development', 'test', 'production')] [string] $Env = "test",
        [string] $FileName = ".env"
    )
    
    Ensure-Directories
    
    $envValues = switch ($Env) {
        "development" {
@"
# PostgreSQL
POSTGRES_USER=aiready
POSTGRES_PASSWORD=yourpassword
POSTGRES_DB=aiready_dev
POSTGRES_PORT=5432

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=yourpassword

# API
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=development

# Docker
COMPOSE_PROJECT_NAME=aiready-dev
"@ 
        }
        "test" {
@"
# PostgreSQL
POSTGRES_USER=aiready
POSTGRES_PASSWORD=yourpassword
POSTGRES_DB=aiready_test
POSTGRES_PORT=5432

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=yourpassword

# API
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=test

# Docker
COMPOSE_PROJECT_NAME=aiready-test
"@ 
        }
        "production" {
@"
# PostgreSQL
POSTGRES_USER=aiready_prod
POSTGRES_PASSWORD=prod_password_here
POSTGRES_DB=aiready_prod
POSTGRES_PORT=5432

# Redis
REDIS_HOST=redis.internal
REDIS_PORT=6379
REDIS_PASSWORD=prod_redis_password

# API
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# Docker
COMPOSE_PROJECT_NAME=aiready-prod
"@ 
        }
    }
    
    $outputPath = Join-Path $CONFIG_DIR $FileName
    $envValues | Out-File -FilePath $outputPath -Encoding UTF8
    
    Write-Log "Docker环境变量文件创建成功: $outputPath" "SUCCESS"
    return $outputPath
}

# 验证配置文件
function Test-Config {
    param (
        [Parameter(Mandatory = $true)] [string] $FilePath
    )
    
    if (-not (Test-Path $FilePath)) {
        Write-Log "文件不存在: $FilePath" "ERROR"
        return $false
    }
    
    $filename = Split-Path -Leaf $FilePath
    
    switch -Wildcard ($filename) {
        "*.json" {
            try {
                $content = Get-Content $FilePath -Raw -Encoding UTF8
                $json = $content | ConvertFrom-Json -ErrorAction Stop
                Write-Log "JSON配置验证通过: $FilePath" "SUCCESS"
                return $true
            }
            catch {
                Write-Log "JSON配置验证失败: $_" "ERROR"
                return $false
            }
        }
        ".env" {
            $content = Get-Content $FilePath
            $validLines = 0
            $invalidLines = 0
            
            foreach ($line in $content) {
                if ($line -match "^[A-Za-z_][A-Za-z0-9_]*=" -or $line -match "^#" -or [string]::IsNullOrWhiteSpace($line)) {
                    $validLines++
                }
                else {
                    $invalidLines++
                }
            }
            
            if ($invalidLines -eq 0) {
                Write-Log "Env配置验证通过: $FilePath (有效行: $validLines)" "SUCCESS"
                return $true
            }
            else {
                Write-Log "Env配置验证失败: $invalidLines 无效行" "ERROR"
                return $false
            }
        }
        default {
            Write-Log "未知配置类型: $filename" "WARN"
            return $true
        }
    }
}

# 生成配置摘要
function Get-ConfigSummary {
    param (
        [string] $ConfigDir = $CONFIG_DIR
    )
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  配置文件摘要" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    $files = Get-ChildItem -Path $ConfigDir -File
    $totalSize = 0
    
    foreach ($file in $files) {
        $fileSize = $file.Length
        $totalSize += $fileSize
        
        Write-Host "$($file.Name)" -ForegroundColor Green
        Write-Host "  大小: $([math]::Round($fileSize/1KB, 2)) KB"
        Write-Host "  修改时间: $($file.LastWriteTime)"
        Write-Host ""
    }
    
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host "总计: $($files.Count) 个文件, $([math]::Round($totalSize/1KB, 2)) KB" -ForegroundColor Yellow
    Write-Host "========================================`n" -ForegroundColor Cyan
}

# 主函数
function Main {
    param (
        [Parameter(Mandatory = $true)] [string] $Command
    )
    
    Ensure-Directories
    
    switch ($Command) {
        "app-config" {
            $env = $args[0] ?? "test"
            New-AppConfig -Env $env
        }
        "docker-env" {
            $env = $args[0] ?? "test"
            New-DockerEnv -Env $env
        }
        "validate" {
            if (-not $args[0]) {
                Write-Log "请提供配置文件路径" "ERROR"
                return
            }
            Test-Config -FilePath $args[0]
        }
        "summary" {
            $dir = $args[0] ?? $CONFIG_DIR
            Get-ConfigSummary -ConfigDir $dir
        }
        default {
            Write-Host @"
用法: basic-config.ps1 <command>

命令:
  app-config [env]     创建应用配置文件 (development|test|production)
  docker-env [env]     创建Docker环境变量文件 (development|test|production)
  validate <file>      验证配置文件
  summary [dir]        显示配置摘要

示例:
  .\basic-config.ps1 app-config test
  .\basic-config.ps1 docker-env production
  .\basic-config.ps1 validate ./config/appsettings.json
  .\basic-config.ps1 summary
"@ 
        }
    }
}

# 如果直接运行脚本，执行命令行接口
if ($MyInvocation.ScriptName -eq $MyInvocation.Line) {
    Main -Command $args[0]
}
## EOF ##
