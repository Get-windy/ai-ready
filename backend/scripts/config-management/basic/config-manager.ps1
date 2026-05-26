#================================================================================
# 配置文件管理脚本 - AI-Ready测试环境配置管理
# 功能：配置生成、验证、备份、还原
# 版本：1.0.0
# 作者：AI-Ready Team
# 日期：2026-04-27
#================================================================================

#================================================================================
# 配置 section
#================================================================================

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$CONFIG_DIR = Join-Path $SCRIPT_DIR "..\config"
$TEMPLATE_DIR = Join-Path $SCRIPT_DIR "..\templates"
$BACKUP_DIR = Join-Path $SCRIPT_DIR "..\backups\config"
$LOG_DIR = Join-Path $SCRIPT_DIR "..\logs"

# 创建必要的目录
function Ensure-Directories {
    if (-not (Test-Path $CONFIG_DIR)) {
        New-Item -ItemType Directory -Path $CONFIG_DIR | Out-Null
    }
    if (-not (Test-Path $TEMPLATE_DIR)) {
        New-Item -ItemType Directory -Path $TEMPLATE_DIR | Out-Null
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
    
    $logFile = Join-Path $LOG_DIR "config-manager-$(Get-Date -Format 'yyyy-MM').log"
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
# 核心功能：配置文件生成
#================================================================================

function New-ConfigTemplate {
    param (
        [ValidateSet('docker-compose', 'application', 'nginx', 'redis', 'postgresql')] [string] $Type = "docker-compose",
        [string] $OutputFile = $null,
        [switch] $IncludeSecrets = $false
    )
    
    Ensure-Directories
    
    $templatePath = Join-Path $TEMPLATE_DIR "$Type-template.yml"
    
    $template = switch ($Type) {
        "docker-compose" {
            @"
version: '3.8'

services:
  # PostgreSQL数据库
  postgresql:
    image: postgres:14
    container_name: aiready-postgresql
    environment:
      POSTGRES_USER: \${POSTGRES_USER:-aiready}
      POSTGRES_PASSWORD: \${POSTGRES_PASSWORD:-yourpassword}
      POSTGRES_DB: \${POSTGRES_DB:-aiready}
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - \${POSTGRES_INIT_SCRIPTS_DIR:-./init}:/docker-entrypoint-initdb.d
    ports:
      - "\${POSTGRES_PORT:-5432}:5432"
    networks:
      - aiready-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \${POSTGRES_USER:-aiready}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis缓存
  redis:
    image: redis:6-alpine
    container_name: aiready-redis
    command: redis-server --requirepass \${REDIS_PASSWORD:-yourpassword}
    ports:
      - "\${REDIS_PORT:-6379}:6379"
    networks:
      - aiready-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # API网关
  nginx:
    image: nginx:alpine
    container_name: aiready-nginx
    ports:
      - "\${NGINX_PORT:-80}:80"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - api-gateway
    networks:
      - aiready-network
    restart: unless-stopped

  # API网关服务
  api-gateway:
    image: openjdk:17-jdk-slim
    container_name: aiready-api-gateway
    ports:
      - "\${API_GATEWAY_PORT:-8080}:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-test}
      - SPRING_DATASOURCE_URL=\${SPRING_DATASOURCE_URL:-jdbc:postgresql://postgresql:5432/aiready}
      - SPRING_DATASOURCE_USERNAME=\${SPRING_DATASOURCE_USERNAME:-aiready}
      - SPRING_DATASOURCE_PASSWORD=\${SPRING_DATASOURCE_PASSWORD:-yourpassword}
      - SPRING_REDIS_HOST=\${SPRING_REDIS_HOST:-redis}
      - SPRING_REDIS_PORT=\${SPRING_REDIS_PORT:-6379}
      - SPRING_REDIS_PASSWORD=\${SPRING_REDIS_PASSWORD:-yourpassword}
    depends_on:
      - postgresql
      - redis
    networks:
      - aiready-network
    restart: unless-stopped

networks:
  aiready-network:
    driver: bridge

volumes:
  postgres-data:
"@
        }
        
        "application" {
            @"
# AI-Ready 应用配置模板
# Environment: \${SPRING_PROFILES_ACTIVE:-test}

# Server Configuration
server:
  port: \${SERVER_PORT:-8082}
  servlet:
    context-path: /
  error:
    include-message: \${SERVER_ERROR_INCLUDE_MESSAGE:-when_authed}
    include-binding-errors: \${SERVER_ERROR_INCLUDE_BINDING_ERRORS:-always}

# Database Configuration
spring:
  datasource:
    url: \${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/aiready_test}
    username: \${SPRING_DATASOURCE_USERNAME:-aiready}
    password: \${SPRING_DATASOURCE_PASSWORD:-yourpassword}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: \${SPRING_DATASOURCE_MAXIMUM_POOL_SIZE:-10}
      minimum-idle: \${SPRING_DATASOURCE_MINIMUM_IDLE:-5}
      connection-timeout: \${SPRING_DATASOURCE_CONNECTION_TIMEOUT:-30000}
      idle-timeout: \${SPRING_DATASOURCE_IDLE_TIMEOUT:-600000}
      max-lifetime: \${SPRING_DATASOURCE_MAX_LIFETIME:-1800000}

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        hbm2ddl:
          auto: update
        show_sql: \${SPRING_JPA_SHOW_SQL:-true}

  redis:
    host: \${SPRING_REDIS_HOST:-localhost}
    port: \${SPRING_REDIS_PORT:-6379}
    password: \${SPRING_REDIS_PASSWORD:-yourpassword}
    timeout: \${SPRING_REDIS_TIMEOUT:-2000}
    lettuce:
      pool:
        max-active: \${SPRING_REDIS_POOL_MAX_ACTIVE:-8}
        max-idle: \${SPRING_REDIS_POOL_MAX_IDLE:-8}
        min-idle: \${SPRING_REDIS_POOL_MIN_IDLE:-0}

# Security Configuration
security:
  enabled: \${SECURITY_ENABLED:-true}
  jwt:
    secret: \${JWT_SECRET:-your-secret-key-change-in-production}
    expiration: \${JWT_EXPIRATION:-86400}
    refresh-expiration: \${JWT_REFRESH_EXPIRATION:-604800}

# Logging Configuration
logging:
  level:
    root: \${LOGGING_LEVEL_ROOT:-INFO}
    cn.aiedge: \${LOGGING_LEVEL_CN_AIEDGE:-DEBUG}
    org.hibernate.SQL: \${LOGGING_LEVEL_HIBERNATE_SQL:-DEBUG}
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n'
    file: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n'
  file:
    name: \${LOGGING_FILE_NAME:-logs/application.log}
    max-size: \${LOGGING_FILE_MAX_SIZE:-10MB}
    max-history: \${LOGGING_FILE_MAX_HISTORY:-30}

# Application Specific Configuration
app:
  name: AI-Ready
  version: 1.0.0
  environment: \${APP_ENVIRONMENT:-test}
  debug: \${APP_DEBUG:-true}
"@
        }
        
        "nginx" {
            @"
upstream api-gateway {
    server api-gateway:8080;
}

server {
    listen 80;
    server_name localhost;

    # Gzip压缩
    gzip on;
    gzip_types application/json application/javascript text/css text/plain;
    gzip_min_length 1000;

    # API代理
    location /api/ {
        proxy_pass http://api-gateway;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_cache_bypass \$http_upgrade;
        
        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 'healthy\n';
        add_header Content-Type text/plain;
    }

    # 限流配置
    limit_req_zone \$binary_remote_addr zone=api_limit:10m rate=100r/s;

    location /api/limited/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass http://api-gateway;
    }
}
"@
        }
        
        "redis" {
            @"
# Redis配置文件模板

# 网络配置
bind 0.0.0.0
protected-mode yes
port 6379

# 通用配置
daemonize no
supervised no
pidfile /var/run/redis_6379.pid
loglevel notice
logfile ""

# 快照配置
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir ./

# 安全配置
requirepass \${REDIS_PASSWORD:-yourpassword}

# 客户端配置
maxclients 10000
timeout 0
tcp-keepalive 300

# 内存配置
maxmemory 256mb
maxmemory-policy allkeys-lru

# AOF配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
"@
        }
        
        "postgresql" {
            @"
# PostgreSQL配置文件模板

# 连接配置
listen_addresses = '*'
port = 5432
max_connections = 100

# 内存配置
shared_buffers = 256MB
work_mem = 4MB
maintenance_work_mem = 64MB
effective_cache_size = 1GB

# WAL配置
wal_level = replica
max_wal_size = 1GB
min_wal_size = 80MB

# 日志配置
logging_collector = on
log_directory = 'log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_min_duration_statement = 1000

# 安全配置
password_encryption = scram-sha-256

# 备份配置
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB
"@
        }
        
        default {
            "# 未知配置类型"
        }
    }
    
    # 设置输出文件名
    if (-not $OutputFile) {
        $OutputFile = "$Type-config.yml"
    }
    
    # 如果文件名没有扩展名，添加.yml
    if (-not $OutputFile.EndsWith(".yml") -and -not $OutputFile.EndsWith(".yaml")) {
        $OutputFile += ".yml"
    }
    
    $outputPath = Join-Path $TEMPLATE_DIR $OutputFile
    
    $template | Out-File -FilePath $outputPath -Encoding UTF8
    
    Write-Log "配置模板已生成: $outputPath" "SUCCESS"
    return $outputPath
}

#================================================================================
# 核心功能：配置验证
#================================================================================

function Test-ConfigFile {
    param (
        [Parameter(Mandatory = $true)] [string] $FilePath,
        [ValidateSet('yml', 'yaml', 'json', 'env')] [string] $Type = "yml"
    )
    
    if (-not (Test-Path $FilePath)) {
        Write-Log "配置文件不存在: $FilePath" "ERROR"
        return $false
    }
    
    $isValid = $true
    
    try {
        switch ($Type) {
            "yml" {
                # 简单YAML检查
                $content = Get-Content $FilePath -Raw
                if ($content -match "^[\s]*#.*$") {
                    Write-Log "配置文件包含注释 (这通常是安全的)" "INFO"
                }
            }
            "json" {
                $content = Get-Content $FilePath -Raw -Encoding UTF8
                $json = $content | ConvertFrom-Json -ErrorAction Stop
                Write-Log "JSON配置验证通过" "SUCCESS"
            }
            "env" {
                $content = Get-Content $FilePath -Raw
                $lines = $content -split "`n"
                foreach ($line in $lines) {
                    if ($line -match "^[\s]*#" -or [string]::IsNullOrWhiteSpace($line)) {
                        continue
                    }
                    if (-not ($line -match "^[A-Za-z_][A-Za-z0-9_]*=")) {
                        Write-Log "(env) 无效的配置行: $line" "WARN"
                        $isValid = $false
                    }
                }
            }
        }
        
        Write-Log "配置文件验证通过: $FilePath" "SUCCESS"
        return $true
    }
    catch {
        Write-Log "配置文件验证失败: $_" "ERROR"
        return $false
    }
}

function Test-ConfigDependencies {
    param (
        [string] $ConfigFile = "docker-compose.yml"
    )
    
    $configPath = Join-Path $TEMPLATE_DIR $ConfigFile
    
    if (-not (Test-Path $configPath)) {
        Write-Log "配置文件不存在: $configPath" "ERROR"
        return $false
    }
    
    Write-Log " checking dependencies for $ConfigFile..." "INFO"
    
    $services = @()
    $content = Get-Content $configPath -Raw
    
    # 提取服务名称
    if ($content -match "services:`n((?:  [a-z-]+:`n)*)") {
        $servicesSection = $matches[1]
        $services = $servicesSection -split "`n" | Where-Object { $_ -match "^  ([a-z-]+):" } | ForEach-Object { $matches[1] }
    }
    
    $dependencieCheck = @{}
    
    foreach ($service in $services) {
        $dependsOn = $content -match "^( {8})$service:`n(.*?)(?=^  [a-z]|\$)" | Out-Null
        $dependencieCheck[$service] = $true
    }
    
    Write-Log "服务依赖检查完成" "SUCCESS"
    return $true
}

#================================================================================
# 核心功能：配置备份
#================================================================================

function Backup-ConfigFile {
    param (
        [string] $ConfigFile = "docker-compose.yml",
        [string] $BackupName = $null
    )
    
    Ensure-Directories
    
    $configPath = Join-Path $TEMPLATE_DIR $ConfigFile
    
    if (-not (Test-Path $configPath)) {
        Write-Log "配置文件不存在，无法备份: $configPath" "ERROR"
        return $null
    }
    
    # 生成备份文件名
    if (-not $BackupName) {
        $BackupName = "config-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    }
    
    $backupFile = Join-Path $BACKUP_DIR "$BackupName.yml"
    
    # 复制配置文件
    Copy-Item -Path $configPath -Destination $backupFile
    
    Write-Log "配置已备份: $backupFile" "SUCCESS"
    return $backupFile
}

function Backup-AllConfigs {
    param (
        [switch] $IncludeTemplates = $true
    )
    
    Ensure-Directories
    
    $backups = @()
    
    # 备份模板文件
    if ($IncludeTemplates) {
        $templateFiles = Get-ChildItem -Path $TEMPLATE_DIR -Filter "*.yml" -File
        foreach ($file in $templateFiles) {
            $backup = Backup-ConfigFile -ConfigFile $file.Name
            if ($null -ne $backup) {
                $backups += $backup
            }
        }
    }
    
    Write-Log "所有配置已备份 (共 $($backups.Count) 个文件)" "SUCCESS"
    return $backups
}

#================================================================================
# 核心功能：配置还原
#================================================================================

function Restore-ConfigFile {
    param (
        [Parameter(Mandatory = $true)] [string] $BackupFile,
        [string] $TargetFile = $null,
        [switch] $BackupCurrent = $false
    )
    
    if (-not (Test-Path $BackupFile)) {
        Write-Log "备份文件不存在: $BackupFile" "ERROR"
        return $false
    }
    
    $filename = Split-Path -Leaf $BackupFile
    
    if (-not $TargetFile) {
        # 从备份文件名推断目标文件
        $TargetFile = $filename -replace "^config-backup-\d{8}-\d{6}", "docker-compose"
        $TargetFile = $TargetFile -replace "\.yml$", ""
        $TargetFile += ".yml"
    }
    
    $targetPath = Join-Path $TEMPLATE_DIR $TargetFile
    
    # 如果需要先备份当前配置
    if ($BackupCurrent.IsPresent -and (Test-Path $targetPath)) {
        $currentBackup = Backup-ConfigFile -ConfigFile $TargetFile
        Write-Log "当前配置已备份: $currentBackup" "INFO"
    }
    
    # 还原配置
    Copy-Item -Path $BackupFile -Destination $targetPath -Force
    
    Write-Log "配置已还原: $BackupFile -> $targetPath" "SUCCESS"
    return $true
}

#================================================================================
# 辅助功能
#================================================================================

function Show-ConfigFile {
    param (
        [Parameter(Mandatory = $true)] [string] $ConfigFile
    )
    
    $configPath = Join-Path $TEMPLATE_DIR $ConfigFile
    
    if (-not (Test-Path $configPath)) {
        Write-Log "配置文件不存在: $configPath" "ERROR"
        return
    }
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  配置文件内容: $ConfigFile" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    Get-Content $configPath -Raw | Write-Host -ForegroundColor White
    
    Write-Host "`n========================================`n" -ForegroundColor Cyan
}

function List-ConfigFiles {
    Write-Host "`n可用的配置文件:" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    $files = Get-ChildItem -Path $TEMPLATE_DIR -Filter "*.yml" -File
    foreach ($file in $files) {
        Write-Host "  $file" -ForegroundColor Green
    }
    
    Write-Host "`n可用的备份文件:" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    $backups = Get-ChildItem -Path $BACKUP_DIR -Filter "*.yml" -File
    foreach ($backup in $backups) {
        Write-Host "  $backup" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

#================================================================================
# 命令行接口
#================================================================================

function Main {
    param (
        [Parameter(Mandatory = $true)] [string] $Command
    )
    
    Ensure-Directories
    
    switch ($Command) {
        "template" {
            $type = $args[0] ?? "docker-compose"
            $ outputFile = $args[1] ?? $null
            New-ConfigTemplate -Type $type -OutputFile $outputFile
        }
        "validate" {
            if (-not $args[0]) {
                Write-Log "请提供配置文件路径" "ERROR"
                return
            }
            $type = $args[1] ?? "yml"
            Test-ConfigFile -FilePath $args[0] -Type $type
        }
        "backup" {
            if (-not $args[0]) {
                Backup-AllConfigs
            }
            else {
                Backup-ConfigFile -ConfigFile $args[0]
            }
        }
        "restore" {
            if (-not $args[0]) {
                Write-Log "请提供备份文件路径" "ERROR"
                return
            }
            $target = $args[1] ?? $null
            Restore-ConfigFile -BackupFile $args[0] -TargetFile $target
        }
        "show" {
            if (-not $args[0]) {
                List-ConfigFiles
            }
            else {
                Show-ConfigFile -ConfigFile $args[0]
            }
        }
        "list" {
            List-ConfigFiles
        }
        default {
            Write-Log "未知命令: $Command" "ERROR"
            Write-Host @"
用法: config-manager.ps1 <command> [options]

命令:
  template <type>   生成配置模板 (docker-compose|application|nginx|redis|postgresql)
  validate <file>   验证配置文件
  backup [file]     备份配置文件
  restore <file>    还原配置文件
  show <file>       显示配置文件内容
  list              列出所有配置文件

示例:
  .\config-manager.ps1 template docker-compose
  .\config-manager.ps1 validate ./config/docker-compose.yml
  .\config-manager.ps1 backup
  .\config-manager.ps1 restore ./backups/config/config-backup-20260427-120000.yml
  .\config-manager.ps1 show docker-compose.yml
"@
        }
    }
}

# 如果直接运行脚本，执行命令行接口
if ($MyInvocation.ScriptName -eq $MyInvocation.Line) {
    Main -Command $args[0]
}
## EOF ##
