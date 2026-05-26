#================================================================================
# 环境变量管理脚本 - AI-Ready测试环境配置管理
# 功能：环境变量读取、验证、模板生成、备份、同步
# 版本：1.0.0
# 作者：AI-Ready Team
# 日期：2026-04-27
#================================================================================

#================================================================================
# 配置 section
#================================================================================

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$CONFIG_DIR = Join-Path $SCRIPT_DIR "..\config"
$BACKUP_DIR = Join-Path $SCRIPT_DIR "..\backups\env"
$LOG_DIR = Join-Path $SCRIPT_DIR "..\logs"

# 创建必要的目录
function Ensure-Directories {
    if (-not (Test-Path $CONFIG_DIR)) {
        New-Item -ItemType Directory -Path $CONFIG_DIR | Out-Null
    }
    if (-not (Test-Path $BACKUP_DIR)) {
        New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
    }
    if (-not (Test-Path $LOG_DIR)) {
        New-Item -ItemType Directory -Path $LOG_DIR | Out-Null
    }
}

# 日志记录函数
function Write-Log {
    param (
        [Parameter(Mandatory = $true)] [string] $Message,
        [ValidateSet('INFO', 'WARN', 'ERROR', 'SUCCESS')] [string] $Level = 'INFO'
    )
    
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $logMessage = "[$timestamp] [$Level] $Message"
    
    Write-Host $logMessage -ForegroundColor (Get-ForegroundColor $Level)
    
    $logFile = Join-Path $LOG_DIR "env-manager-$(Get-Date -Format 'yyyy-MM').log"
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

#================================================================================
# 核心功能：环境变量读取
#================================================================================

function Get-EnvironmentConfig {
    param (
        [string] $ConfigFile = "environment.json",
        [string] $Env = "development"
    )
    
    $configPath = Join-Path $CONFIG_DIR $ConfigFile
    
    if (-not (Test-Path $configPath)) {
        Write-Log "配置文件不存在: $configPath" "ERROR"
        return $null
    }
    
    try {
        $content = Get-Content $configPath -Raw -Encoding UTF8
        $config = $content | ConvertFrom-Json
        
        Write-Log "成功读取配置文件: $configPath" "SUCCESS"
        return $config
    }
    catch {
        Write-Log "读取配置文件失败: $_" "ERROR"
        return $null
    }
}

function Get-EnvironmentValue {
    param (
        [string] $Key,
        [string] $ConfigFile = "environment.json"
    )
    
    $config = Get-EnvironmentConfig -ConfigFile $ConfigFile
    
    if ($null -eq $config) {
        return $null
    }
    
    # 支持嵌套属性访问 (e.g., "database.host")
    $parts = $Key.Split('.')
    $value = $config
    
    foreach ($part in $parts) {
        if ($value -is [PSCustomObject] -and $value.PSObject.Properties.Name -contains $part) {
            $value = $value.$part
        }
        else {
            Write-Log "配置项不存在: $Key" "WARN"
            return $null
        }
    }
    
    return $value
}

#================================================================================
# 核心功能：环境变量验证
#================================================================================

function Test-EnvironmentConfig {
    param (
        [string] $ConfigFile = "environment.json",
        [ValidateSet('development', 'test', 'production')] [string] $Env = "development"
    )
    
    $config = Get-EnvironmentConfig -ConfigFile $ConfigFile
    if ($null -eq $config) {
        return $false
    }
    
    $isValid = $true
    $requiredFields = @("projectName", "version", "environment")
    
    foreach ($field in $requiredFields) {
        if (-not (Get-Member -inputobject $config -name $field -Membertype Properties)) {
            Write-Log "缺少必需字段: $field" "ERROR"
            $isValid = $false
        }
    }
    
    # 环境特定验证
    switch ($Env) {
        "development" {
            if (-not $config.development) {
                Write-Log "缺少 development 环境配置" "ERROR"
                $isValid = $false
            }
        }
        "test" {
            if (-not $config.test) {
                Write-Log "缺少 test 环境配置" "ERROR"
                $isValid = $false
            }
            # 验证数据库连接配置
            if ($config.test.database -and $config.test.database.connectionString) {
                if ($config.test.database.connectionString -like "*localhost*") {
                    Write-Log "警告: 测试环境数据库使用 localhost" "WARN"
                }
            }
        }
        "production" {
            if (-not $config.production) {
                Write-Log "缺少 production 环境配置" "ERROR"
                $isValid = $false
            }
        }
    }
    
    if ($isValid) {
        Write-Log "配置验证通过: $ConfigFile" "SUCCESS"
    }
    else {
        Write-Log "配置验证失败: $ConfigFile" "ERROR"
    }
    
    return $isValid
}

#================================================================================
# 核心功能：环境变量模板生成
#================================================================================

function New-EnvironmentTemplate {
    param (
        [ValidateSet('development', 'test', 'production')] [string] $Env = "test",
        [string] $OutputFile = "environment.json"
    )
    
    $template = @{
        projectName = "AI-Ready"
        version = "1.0.0"
        environment = $Env
        description = "AI-Ready 测试环境配置模板"
        generatedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    
    # 根据环境添加特定配置
    switch ($Env) {
        "development" {
            $template.development = @{
                database = @{
                    host = "localhost"
                    port = 5432
                    name = "aiready_dev"
                    user = "postgres"
                    password = "YourPassword"
                }
                redis = @{
                    host = "localhost"
                    port = 6379
                    password = ""
                }
                api = @{
                    port = 8080
                    debug = $true
                }
            }
        }
        "test" {
            $template.test = @{
                database = @{
                    host = "localhost"
                    port = 5432
                    name = "aiready_test"
                    user = "postgres"
                    password = "YourPassword"
                }
                redis = @{
                    host = "localhost"
                    port = 6379
                    password = ""
                }
                api = @{
                    port = 8082
                    debug = $true
                }
                security = @{
                    enableCors = $true
                    allowOrigins = @("http://localhost:8080", "http://localhost:3000")
                }
            }
        }
        "production" {
            $template.production = @{
                database = @{
                    host = "prod-dbserver.internal"
                    port = 5432
                    name = "aiready_prod"
                    user = "aiready_admin"
                    password = "********"
                }
                redis = @{
                    host = "prod-redis.internal"
                    port = 6379
                    password = "********"
                }
                api = @{
                    port = 8080
                    debug = $false
                }
                security = @{
                    enableCors = $false
                    allowOrigins = @()
                }
            }
        }
    }
    
    # 输出JSON格式
    $json = $template | ConvertTo-Json -Depth 10
    $outputPath = Join-Path $CONFIG_DIR $OutputFile
    
    $json | Out-File -FilePath $outputPath -Encoding UTF8
    
    Write-Log "环境模板已生成: $outputPath" "SUCCESS"
    return $outputPath
}

#================================================================================
# 核心功能：环境变量备份
#================================================================================

function Backup-EnvironmentConfig {
    param (
        [string] $ConfigFile = "environment.json",
        [string] $BackupName = $null
    )
    
    $configPath = Join-Path $CONFIG_DIR $ConfigFile
    
    if (-not (Test-Path $configPath)) {
        Write-Log "配置文件不存在，无法备份: $configPath" "ERROR"
        return $null
    }
    
    Ensure-Directories
    
    # 生成备份文件名
    if (-not $BackupName) {
        $BackupName = "env-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    }
    
    $backupFile = Join-Path $BACKUP_DIR "$BackupName.json"
    
    # 复制配置文件
    Copy-Item -Path $configPath -Destination $backupFile
    
    Write-Log "配置已备份: $backupFile" "SUCCESS"
    return $backupFile
}

function Restore-EnvironmentConfig {
    param (
        [string] $BackupFile,
        [switch] $BackupCurrent = $false
    )
    
    if (-not (Test-Path $BackupFile)) {
        Write-Log "备份文件不存在: $BackupFile" "ERROR"
        return $false
    }
    
    $configPath = Join-Path $CONFIG_DIR "environment.json"
    
    # 如果需要先备份当前配置
    if ($BackupCurrent.IsPresent -and (Test-Path $configPath)) {
        $currentBackup = Backup-EnvironmentConfig
        Write-Log "当前配置已备份: $currentBackup" "INFO"
    }
    
    # 还原配置
    Copy-Item -Path $BackupFile -Destination $configPath -Force
    
    Write-Log "配置已还原: $BackupFile -> $configPath" "SUCCESS"
    return $true
}

#================================================================================
# 核心功能：环境变量同步
#================================================================================

function Sync-EnvironmentConfig {
    param (
        [string] $SourceEnv = "test",
        [string] $TargetEnv = "development",
        [string] $ConfigFile = "environment.json"
    )
    
    $config = Get-EnvironmentConfig -ConfigFile $ConfigFile
    if ($null -eq $config) {
        return $false
    }
    
    # 构建源和目标配置路径
    $sourceKey = $SourceEnv
    $targetKey = $TargetEnv
    
    # 检查源环境配置是否存在
    $sourceConfig = $config.$sourceKey
    if ($null -eq $sourceConfig) {
        Write-Log "源环境配置不存在: $sourceKey" "ERROR"
        return $false
    }
    
    # 同步配置（保留目标环境的其他设置）
    if (-not $config.PSObject.Properties.Name -contains $targetKey) {
        $config | Add-Member -MemberType NoteProperty -Name $targetKey -Value (Copy-Object $sourceConfig)
    }
    else {
        $config.$targetKey = Copy-Object $sourceConfig
    }
    
    # 保存配置
    $json = $config | ConvertTo-Json -Depth 10
    $configPath = Join-Path $CONFIG_DIR $ConfigFile
    $json | Out-File -FilePath $configPath -Encoding UTF8
    
    Write-Log "配置已从 $sourceKey 同步到 $targetKey" "SUCCESS"
    return $true
}

function Copy-Object {
    param ($Object)
    
    # 深度复制PSObject
    $json = $Object | ConvertTo-Json -Depth 10
    return $json | ConvertFrom-Json
}

#================================================================================
# 辅助功能
#================================================================================

function Show-EnvironmentConfig {
    param (
        [string] $ConfigFile = "environment.json"
    )
    
    $config = Get-EnvironmentConfig -ConfigFile $ConfigFile
    
    if ($null -eq $config) {
        return
    }
    
    # 格式化输出配置
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  环境配置详情" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    Write-Host "项目名称: $($config.projectName)" -ForegroundColor Yellow
    Write-Host "版本号: $($config.version)" -ForegroundColor Yellow
    Write-Host "当前环境: $($config.environment)" -ForegroundColor Yellow
    
    # 显示各环境配置摘要
    if ($config.PSObject.Properties.Name -contains "development") {
        Write-Host "`n开发环境配置:" -ForegroundColor Green
        if ($config.development.database) {
            Write-Host "  Database: $($config.development.database.host):$($config.development.database.port)/$($config.development.database.name)" -ForegroundColor White
        }
    }
    
    if ($config.PSObject.Properties.Name -contains "test") {
        Write-Host "`n测试环境配置:" -ForegroundColor Green
        if ($config.test.database) {
            Write-Host "  Database: $($config.test.database.host):$($config.test.database.port)/$($config.test.database.name)" -ForegroundColor White
        }
        if ($config.test.api) {
            Write-Host "  API Port: $($config.test.api.port)" -ForegroundColor White
        }
    }
    
    if ($config.PSObject.Properties.Name -contains "production") {
        Write-Host "`n生产环境配置:" -ForegroundColor Green
        if ($config.production.database) {
            Write-Host "  Database: $($config.production.database.host):$($config.production.database.port)/$($config.production.database.name)" -ForegroundColor White
        }
    }
    
    Write-Host "`n========================================`n" -ForegroundColor Cyan
}

function Export-EnvironmentSummary {
    param (
        [string] $OutputFile = "env-summary.md",
        [string] $ConfigFile = "environment.json"
    )
    
    $config = Get-EnvironmentConfig -ConfigFile $ConfigFile
    
    if ($null -eq $config) {
        return
    }
    
    $markdown = @"
# 环境配置摘要

- **项目名称**: $($config.projectName)
- **版本号**: $($config.version)
- **生成时间**: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

## 开发环境

"@ 

    if ($config.development) {
        $markdown += @"
### 数据库
- **主机**: $($config.development.database.host ?? '未配置')
- **端口**: $($config.development.database.port ?? '未配置')
- **数据库名**: $($config.development.database.name ?? '未配置')

### Redis
- **主机**: $($config.development.redis.host ?? '未配置')
- **端口**: $($config.development.redis.port ?? '未配置')

### API
- **端口**: $($config.development.api.port ?? '未配置')
- **调试模式**: $($config.development.api.debug ?? '未配置')
"@
    }
    
    $markdown += @"
## 测试环境

"@
    
    if ($config.test) {
        $markdown += @"
### 数据库
- **主机**: $($config.test.database.host ?? '未配置')
- **端口**: $($config.test.database.port ?? '未配置')
- **数据库名**: $($config.test.database.name ?? '未配置')

### Redis
- **主机**: $($config.test.redis.host ?? '未配置')
- **端口**: $($config.test.redis.port ?? '未配置')

### API
- **端口**: $($config.test.api.port ?? '未配置')
- **调试模式**: $($config.test.api.debug ?? '未配置')
"@ 
    }
    
    $markdown | Out-File -FilePath (Join-Path $SCRIPT_DIR $OutputFile) -Encoding UTF8
    Write-Log "配置摘要已导出: $OutputFile" "SUCCESS"
}

#================================================================================
# 命令行接口
#================================================================================

function Main {
    param (
        [Parameter(Mandatory = $true)] [string] $Command,
        [string] $ConfigFile = "environment.json",
        [string] $Env = "test"
    )
    
    Ensure-Directories
    
    switch ($Command) {
        "read" {
            Get-EnvironmentConfig -ConfigFile $ConfigFile
        }
        "get" {
            if (-not $arg) {
                Write-Log "请提供配置项key" "ERROR"
                return
            }
            Get-EnvironmentValue -Key $arg -ConfigFile $ConfigFile
        }
        "validate" {
            Test-EnvironmentConfig -ConfigFile $ConfigFile -Env $Env
        }
        "init" {
            New-EnvironmentTemplate -Env $Env -OutputFile $ConfigFile
        }
        "backup" {
            Backup-EnvironmentConfig -ConfigFile $ConfigFile
        }
        "restore" {
            if (-not $arg) {
                Write-Log "请提供备份文件路径" "ERROR"
                return
            }
            Restore-EnvironmentConfig -BackupFile $arg
        }
        "sync" {
            if (-not $arg) {
                Write-Log "请提供源环境名称" "ERROR"
                return
            }
            Sync-EnvironmentConfig -SourceEnv $arg -TargetEnv $Env
        }
        "show" {
            Show-EnvironmentConfig -ConfigFile $ConfigFile
        }
        "export" {
            Export-EnvironmentSummary -ConfigFile $ConfigFile
        }
        default {
            Write-Log "未知命令: $Command" "ERROR"
            Write-Host @"
用法: env-manager.ps1 <command> [options]

命令:
  read              读取配置文件
  get <key>         获取配置项值
  validate          验证配置文件
  init              初始化配置模板
  backup            备份当前配置
  restore <file>    还原配置
  sync <env>        同步环境配置
  show              显示配置摘要
  export            导出配置摘要为Markdown

示例:
  .\env-manager.ps1 init -Env test
  .\env-manager.ps1 validate
  .\env-manager.ps1 backup
  .\env-manager.ps1 restore .\backups\env\env-backup-20260427-120000.json
  .\env-manager.ps1 sync test
"@ 
        }
    }
}

# 如果直接运行脚本，执行命令行接口
if ($MyInvocation.ScriptName -eq $MyInvocation.Line) {
    Main -Command $args[0] -ConfigFile $args[1] -Env $args[2]
}
## EOF ##
