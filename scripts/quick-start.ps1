# ============================================================
# 智企连·AI-Ready 一键启动脚本
# ============================================================
# 作者: devops-engineer
# 日期: 2026-03-29
# 用法: .\quick-start.ps1 [-StartBackend] [-StartFrontend]
# ============================================================

# 设置控制台编码为 UTF-8 (解决中文乱码问题)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
if ($Host.Name -eq 'ConsoleHost') {
    try { chcp 65001 > $null } catch {}
}

param(
    [switch]$StartBackend = $true,
    [switch]$StartFrontend = $true,
    [switch]$SkipBuild = $false,
    [switch]$Verbose = $false
)

$projectRoot = "I:\AI-Ready"
$frontendDir = "I:\AI-Ready\smart-admin-web"
$backendDir = "I:\AI-Ready\core-base"

Write-Host "==========================================" -ForegroundColor Green
Write-Host "智企连·AI-Ready 快速启动" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "参数设置:" -ForegroundColor Gray
Write-Host "  - StartBackend: $StartBackend" -ForegroundColor Gray
Write-Host "  - StartFrontend: $StartFrontend" -ForegroundColor Gray
Write-Host "  - SkipBuild: $SkipBuild" -ForegroundColor Gray
Write-Host ""

# ========================================
# 1. 启动数据库服务
# ========================================
Write-Host "[Step 1] 启动数据库服务..." -ForegroundColor Yellow

# PostgreSQL
try {
    $pgContainer = docker ps -a 2>&1 | Select-String "ai-ready-postgres"
    if ($pgContainer) {
        $pgRunning = docker ps 2>&1 | Select-String "ai-ready-postgres"
        if ($pgRunning) {
            Write-Host "   ✅ PostgreSQL 已运行" -ForegroundColor Green
        } else {
            Write-Host "   启动 PostgreSQL 容器..." -ForegroundColor Cyan
            docker start ai-ready-postgres 2>&1 | Out-Null
            Write-Host "   ✅ PostgreSQL 已启动" -ForegroundColor Green
        }
    } else {
        Write-Host "   创建 PostgreSQL 容器..." -ForegroundColor Cyan
        docker run -d `
            --name ai-ready-postgres `
            -e POSTGRES_USER=devuser `
            -e POSTGRES_PASSWORD="Dev@2026#Local" `
            -e POSTGRES_DB=devdb `
            -p 5432:5432 `
            -v ai-ready-pgdata:/var/lib/postgresql/data `
            postgres:14-alpine 2>&1 | Out-Null
        Write-Host "   ✅ PostgreSQL 容器已创建并启动" -ForegroundColor Green
    }
} catch {
    Write-Host "   ⚠️ Docker 不可用，请手动启动 PostgreSQL" -ForegroundColor Yellow
}

# Redis
try {
    $redisContainer = docker ps -a 2>&1 | Select-String "ai-ready-redis"
    if ($redisContainer) {
        $redisRunning = docker ps 2>&1 | Select-String "ai-ready-redis"
        if ($redisRunning) {
            Write-Host "   ✅ Redis 已运行" -ForegroundColor Green
        } else {
            Write-Host "   启动 Redis 容器..." -ForegroundColor Cyan
            docker start ai-ready-redis 2>&1 | Out-Null
            Write-Host "   ✅ Redis 已启动" -ForegroundColor Green
        }
    } else {
        Write-Host "   创建 Redis 容器..." -ForegroundColor Cyan
        docker run -d `
            --name ai-ready-redis `
            -p 6379:6379 `
            -v ai-ready-redisdata:/data `
            redis:7-alpine `
            redis-server --appendonly yes 2>&1 | Out-Null
        Write-Host "   ✅ Redis 容器已创建并启动" -ForegroundColor Green
    }
} catch {
    Write-Host "   ⚠️ Docker 不可用，请手动启动 Redis" -ForegroundColor Yellow
}

Write-Host ""

# ========================================
# 2. 等待服务就绪
# ========================================
Write-Host "[Step 2] 等待服务就绪..." -ForegroundColor Yellow
Write-Host "   等待 5 秒..." -ForegroundColor Gray
Start-Sleep -Seconds 5

# 测试 PostgreSQL
try {
    $pgReady = docker exec ai-ready-postgres pg_isready -U devuser 2>&1
    if ($pgReady -match "accepting connections") {
        Write-Host "   ✅ PostgreSQL 就绪" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ PostgreSQL 未就绪，继续等待..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
} catch {}

# 测试 Redis
try {
    $redisPong = docker exec ai-ready-redis redis-cli ping 2>&1
    if ($redisPong -match "PONG") {
        Write-Host "   ✅ Redis 就绪" -ForegroundColor Green
    }
} catch {}

Write-Host ""

# ========================================
# 3. 构建项目
# ========================================
if (-not $SkipBuild) {
    Write-Host "[Step 3] 构建 Maven 项目..." -ForegroundColor Yellow
    
    if (Test-Path "$projectRoot\pom.xml") {
        Push-Location $projectRoot
        
        Write-Host "   执行: mvn clean install -DskipTests" -ForegroundColor Gray
        $buildOutput = mvn clean install -DskipTests 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✅ Maven 构建成功" -ForegroundColor Green
        } else {
            Write-Host "   ❌ Maven 构建失败" -ForegroundColor Red
            if ($Verbose) {
                Write-Host $buildOutput -ForegroundColor Gray
            }
            Write-Host "   请检查 pom.xml 或运行 scripts\verify-environment.ps1" -ForegroundColor Yellow
        }
        
        Pop-Location
    } else {
        Write-Host "   ⚠️ pom.xml 不存在，跳过构建" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# ========================================
# 4. 启动后端
# ========================================
if ($StartBackend) {
    Write-Host "[Step 4] 启动后端服务..." -ForegroundColor Yellow
    
    if (Test-Path "$backendDir\pom.xml") {
        Write-Host "   启动 Spring Boot 应用..." -ForegroundColor Cyan
        Write-Host "   后端地址: http://localhost:8080" -ForegroundColor Gray
        Write-Host "   API 文档: http://localhost:8080/doc.html" -ForegroundColor Gray
        
        Push-Location $backendDir
        
        # 在新窗口启动后端
        $backendJob = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -WindowStyle Normal
        
        Pop-Location
        
        Write-Host "   ✅ 后端启动中... (PID: $($backendJob.Id))" -ForegroundColor Green
        Write-Host "   等待后端就绪 (约 30 秒)..." -ForegroundColor Gray
        
        # 等待后端启动
        $maxWait = 60
        $waited = 0
        $backendReady = $false
        
        while ($waited -lt $maxWait -and -not $backendReady) {
            Start-Sleep -Seconds 5
            $waited += 5
            
            try {
                $healthCheck = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
                if ($healthCheck.StatusCode -eq 200) {
                    $backendReady = $true
                    Write-Host "   ✅ 后端已就绪！" -ForegroundColor Green
                }
            } catch {}
            
            Write-Host "   已等待 $waited 秒..." -ForegroundColor Gray
        }
        
        if (-not $backendReady) {
            Write-Host "   ⚠️ 后端启动较慢，请稍后手动验证" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ⚠️ 后端模块未配置，跳过启动" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# ========================================
# 5. 启动前端
# ========================================
if ($StartFrontend) {
    Write-Host "[Step 5] 启动前端服务..." -ForegroundColor Yellow
    
    if (Test-Path "$frontendDir\package.json") {
        Write-Host "   检查前端依赖..." -ForegroundColor Cyan
        
        # 检查 node_modules
        if (-not (Test-Path "$frontendDir\node_modules")) {
            Write-Host "   安装前端依赖..." -ForegroundColor Cyan
            Push-Location $frontendDir
            
            # 优先使用 pnpm
            try {
                $pnpmExists = pnpm --version 2>&1
                if ($pnpmExists) {
                    Write-Host "   使用 pnpm 安装..." -ForegroundColor Gray
                    pnpm install 2>&1 | Out-Null
                } else {
                    Write-Host "   使用 npm 安装..." -ForegroundColor Gray
                    npm install 2>&1 | Out-Null
                }
            } catch {
                npm install 2>&1 | Out-Null
            }
            
            Pop-Location
            Write-Host "   ✅ 依赖安装完成" -ForegroundColor Green
        } else {
            Write-Host "   ✅ 依赖已存在" -ForegroundColor Green
        }
        
        Write-Host "   启动 Vite 开发服务器..." -ForegroundColor Cyan
        Write-Host "   前端地址: http://localhost:3000" -ForegroundColor Gray
        
        Push-Location $frontendDir
        
        # 在新窗口启动前端
        $frontendJob = Start-Process -FilePath "npm" -ArgumentList "run", "dev" -PassThru -WindowStyle Normal
        
        Pop-Location
        
        Write-Host "   ✅ 前端启动中... (PID: $($frontendJob.Id))" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ 前端项目未配置，跳过启动" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# ========================================
# 总结
# ========================================
Write-Host "==========================================" -ForegroundColor Green
Write-Host "启动完成！" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

Write-Host "服务地址:" -ForegroundColor Cyan
Write-Host "  📦 PostgreSQL: localhost:5432 (devdb)" -ForegroundColor White
Write-Host "  📦 Redis:      localhost:6379" -ForegroundColor White
if ($StartBackend) {
    Write-Host "  🔧 后端 API:  http://localhost:8080" -ForegroundColor White
    Write-Host "  📖 API 文档:  http://localhost:8080/doc.html" -ForegroundColor White
}
if ($StartFrontend) {
    Write-Host "  🖥️ 前端界面: http://localhost:3000" -ForegroundColor White
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "提示:" -ForegroundColor Cyan
Write-Host "  - 按 Ctrl+C 可停止后台服务" -ForegroundColor Gray
Write-Host "  - 运行 scripts\verify-environment.ps1 检查环境" -ForegroundColor Gray
Write-Host "  - 查看 docs\SETUP_GUIDE.md 获取详细配置说明" -ForegroundColor Gray
Write-Host "==========================================" -ForegroundColor Cyan