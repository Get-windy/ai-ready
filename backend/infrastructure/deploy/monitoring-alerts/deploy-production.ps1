# 监控告警模块生产环境部署脚本 (PowerShell)
# 文件名: deploy-production.ps1
# 版本: v1.0.0
# 创建日期: 2026-04-29
# 描述: 部署监控告警模块到生产环境

param(
    [string]$Environment = "production",
    [switch]$SkipBuild,
    [switch]$SkipBackup,
    [switch]$Force
)

# 脚本目录
$ScriptDir = $PSScriptRoot
$DeployDir = $ScriptDir
$BackendDir = Join-Path (Split-Path $DeployDir -Parent) ".." "backend"

# 日志函数
function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor Cyan
}

function Write-Success($message) {
    Write-Host "[SUCCESS] $message" -ForegroundColor Green
}

function Write-Warning($message) {
    Write-Host "[WARNING] $message" -ForegroundColor Yellow
}

function Write-ErrorCustom($message) {
    Write-Host "[ERROR] $message" -ForegroundColor Red
}

# 检查命令
function Test-Command($command) {
    return $null -ne (Get-Command $command -ErrorAction SilentlyContinue)
}

# 检查Docker
function Test-Docker() {
    Write-Info "检查Docker环境..."
    
    if (-not (Test-Command docker)) {
        Write-ErrorCustom "Docker未安装，请先安装Docker"
        exit 1
    }
    
    if (-not (Test-Command docker-compose)) {
        Write-ErrorCustom "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    }
    
    Write-Success "Docker环境检查通过"
}

# 检查Maven
function Test-Maven() {
    Write-Info "检查Maven环境..."
    
    if (-not (Test-Command mvn)) {
        Write-Warning "Maven未安装，跳过构建步骤"
        return $false
    }
    
    Write-Success "Maven环境检查通过"
    return $true
}

# 检查Java
function Test-Java() {
    Write-Info "检查Java环境..."
    
    if (-not (Test-Command java)) {
        Write-ErrorCustom "Java未安装，请先安装Java 17+"
        exit 1
    }
    
    # 检查Java版本
    $javaVersionOutput = & java -version 2>&1
    $javaVersionLine = $javaVersionOutput | Select-String -Pattern '"(\d+\.\d+).*"' | ForEach-Object { $_.Matches.Groups[1].Value }
    
    if ($javaVersionLine -and $javaVersionLine.Split('.')[0] -lt 17) {
        Write-ErrorCustom "Java版本过低，需要Java 17或更高版本"
        exit 1
    }
    
    Write-Success "Java环境检查通过"
}

# 创建目录
function Create-Directories() {
    Write-Info "创建必要的目录..."
    
    $dirs = @(
        "logs/monitoring-api",
        "nginx/conf.d",
        "nginx/ssl",
        "nginx/html",
        "custom-exporter/config",
        "init-scripts"
    )
    
    foreach ($dir in $dirs) {
        $fullPath = Join-Path $DeployDir $dir
        if (-not (Test-Path $fullPath)) {
            New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        }
    }
    
    Write-Success "目录创建完成"
}

# 构建监控模块
function Build-MonitoringModule() {
    Write-Info "开始构建监控模块..."
    
    $monitoringDir = Join-Path $BackendDir "monitoring"
    
    if (Test-Path $monitoringDir) {
        Push-Location $monitoringDir
        if (Test-Path "pom.xml") {
            Write-Info "构建监控模块..."
            & mvn clean package -DskipTests -Pprod
            Write-Success "监控模块构建完成"
        } else {
            Write-Warning "监控模块缺少pom.xml，跳过构建"
        }
        Pop-Location
    } else {
        Write-Warning "监控模块目录不存在，跳过构建"
    }
}

# 构建Docker镜像
function Build-DockerImages() {
    Write-Info "开始构建Docker镜像..."
    
    # 构建自定义导出器
    $customExporterDockerfile = Join-Path $DeployDir ".." "backend" "monitoring" "src" "main" "docker" "custom-exporter" "Dockerfile"
    if (Test-Path $customExporterDockerfile) {
        Write-Info "构建自定义导出器镜像..."
        & docker build -t ai-ready/custom-exporter:latest -f $customExporterDockerfile (Join-Path $DeployDir ".." "backend" "monitoring")
        Write-Success "自定义导出器镜像构建完成"
    } else {
        Write-Warning "自定义导出器Dockerfile不存在，跳过构建"
    }
    
    # 构建监控API服务
    if (Test-Path $customExporterDockerfile) {
        Write-Info "构建监控API服务镜像..."
        & docker build -t ai-ready/monitoring-api:latest -f $customExporterDockerfile (Join-Path $DeployDir ".." "backend" "monitoring")
        Write-Success "监控API服务镜像构建完成"
    } else {
        Write-Warning "监控API服务Dockerfile不存在，跳过构建"
    }
    
    Write-Success "Docker镜像构建完成"
}

# 备份旧版本
function Backup-OldVersion() {
    Write-Info "备份旧版本..."
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupDir = Join-Path $DeployDir "backups" $timestamp
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
    
    # 备份Docker镜像
    $images = docker images --format "{{.Repository}}:{{.Tag}}"
    
    if ($images -match "ai-ready/custom-exporter") {
        Write-Info "备份自定义导出器镜像"
        docker save "ai-ready/custom-exporter:latest" -o (Join-Path $backupDir "custom-exporter-latest.tar")
    }
    
    if ($images -match "ai-ready/monitoring-api") {
        Write-Info "备份监控API服务镜像"
        docker save "ai-ready/monitoring-api:latest" -o (Join-Path $backupDir "monitoring-api-latest.tar")
    }
    
    Write-Success "备份完成"
}

# 启动服务
function Start-Services() {
    Write-Info "启动监控告警服务..."
    
    # 使用生产环境配置启动
    docker-compose --env-file .env.production up -d
    
    Write-Success "服务启动完成"
}

# 验证部署
function Validate-Deployment() {
    Write-Info "验证部署..."
    
    # 检查容器状态
    $containers = docker-compose --env-file .env.production ps
    if ($containers -match "Up") {
        Write-Success "所有容器正常运行"
    } else {
        Write-ErrorCustom "部分容器未正常运行"
        return $false
    }
    
    # 检查健康端点
    $services = @("prometheus", "alertmanager", "grafana", "monitoring-api", "node-exporter")
    $ports = @(9090, 9093, 3000, 8081, 9100)
    
    for ($i = 0; $i -lt $services.Length; $i++) {
        $service = $services[$i]
        $port = $ports[$i]
        
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$port" -TimeoutSec 10 -UseBasicParsing
            if ($response.StatusCode -eq 200) {
                Write-Success "$service 服务健康检查通过"
            } else {
                Write-Warning "$service 服务健康检查失败 (状态码: $($response.StatusCode))"
            }
        } catch {
            Write-Warning "$service 服务健康检查失败: $($_.Exception.Message)"
        }
    }
    
    Write-Success "部署验证完成"
    return $true
}

# 主函数
function Main() {
    Write-Info "开始监控告警模块生产环境部署..."
    
    # 环境检查
    Test-Docker
    Test-Java
    $mavenAvailable = Test-Maven
    
    # 创建目录
    Create-Directories
    
    # 构建模块（如果Maven可用且未跳过）
    if ($mavenAvailable -and -not $SkipBuild) {
        Build-MonitoringModule
    }
    
    # 构建Docker镜像
    Build-DockerImages
    
    # 备份旧版本（如果未跳过）
    if (-not $SkipBackup) {
        Backup-OldVersion
    }
    
    # 启动服务
    Start-Services
    
    # 验证部署
    $validationResult = Validate-Deployment
    
    if ($validationResult) {
        Write-Success "监控告警模块生产环境部署完成！"
        Write-Info "访问地址:"
        Write-Info "  - Prometheus: http://localhost:9090"
        Write-Info "  - AlertManager: http://localhost:9093"
        Write-Info "  - Grafana: http://localhost:3000 (用户名: admin, 密码: 请查看.env.production文件)"
        Write-Info "  - 监控API: http://localhost:8081"
    } else {
        Write-ErrorCustom "部署验证失败，请检查日志"
        exit 1
    }
}

# 执行主函数
Main