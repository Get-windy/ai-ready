# ============================================================
# 智企连·AI-Ready 环境验证脚本
# ============================================================
# 作者: devops-engineer
# 日期: 2026-03-29
# 用法: .\verify-environment.ps1
# ============================================================

Write-Host "==========================================" -ForegroundColor Green
Write-Host "智企连·AI-Ready 环境验证" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

$allPassed = $true
$warnings = @()

# ========================================
# 1. 检查 Java 17
# ========================================
Write-Host "1. 检查 Java 17..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1
    $javaVersion = $javaOutput | Select-String "version \"17" -SimpleMatch
    if ($javaVersion) {
        $versionMatch = $javaOutput[0] -replace 'openjdk version "|"', ''
        Write-Host "   ✅ Java $versionMatch 已安装" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Java 版本不正确，需要 JDK 17+" -ForegroundColor Red
        Write-Host "      当前输出: $javaOutput" -ForegroundColor Gray
        $allPassed = $false
    }
    
    # 检查 JAVA_HOME
    $javaHome = $env:JAVA_HOME
    if ($javaHome) {
        Write-Host "   ✅ JAVA_HOME = $javaHome" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ JAVA_HOME 未设置" -ForegroundColor Yellow
        $warnings += "建议设置 JAVA_HOME 环境变量"
    }
} catch {
    Write-Host "   ❌ Java 未安装" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""

# ========================================
# 2. 检查 Maven
# ========================================
Write-Host "2. 检查 Maven..." -ForegroundColor Yellow
try {
    $mvnOutput = mvn -version 2>&1
    $mvnVersion = $mvnOutput | Select-String "Apache Maven" -SimpleMatch
    if ($mvnVersion) {
        $versionStr = ($mvnVersion -replace "Apache Maven ", "" -split " ")[0]
        Write-Host "   ✅ Apache Maven $versionStr 已安装" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Maven 未正确安装" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "   ❌ Maven 未安装" -ForegroundColor Red
    Write-Host "      下载地址: https://maven.apache.org/download.cgi" -ForegroundColor Gray
    $allPassed = $false
}

Write-Host ""

# ========================================
# 3. 检查 Node.js
# ========================================
Write-Host "3. 检查 Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($nodeVersion -match "v(\d+)") {
        $majorVersion = [int]$matches[1]
        if ($majorVersion -ge 18) {
            Write-Host "   ✅ Node.js $nodeVersion 已安装" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ Node.js 版本过低 ($nodeVersion)，建议升级到 18+" -ForegroundColor Yellow
            $warnings += "Node.js 版本建议升级到 18+"
        }
    }
} catch {
    Write-Host "   ❌ Node.js 未安装" -ForegroundColor Red
    Write-Host "      下载地址: https://nodejs.org/" -ForegroundColor Gray
    $allPassed = $false
}

Write-Host ""

# ========================================
# 4. 检查 npm/pnpm
# ========================================
Write-Host "4. 检查 npm/pnpm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    if ($npmVersion) {
        Write-Host "   ✅ npm $npmVersion 已安装" -ForegroundColor Green
    }
    
    $pnpmVersion = pnpm --version 2>&1
    if ($pnpmVersion) {
        Write-Host "   ✅ pnpm $pnpmVersion 已安装" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ pnpm 未安装 (可选，推荐用于前端项目)" -ForegroundColor Yellow
        $warnings += "可安装 pnpm: npm install -g pnpm"
    }
} catch {
    Write-Host "   ❌ npm 未安装" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""

# ========================================
# 5. 检查 Git
# ========================================
Write-Host "5. 检查 Git..." -ForegroundColor Yellow
try {
    $gitVersion = git --version 2>&1
    if ($gitVersion) {
        $versionStr = $gitVersion -replace "git version ", ""
        Write-Host "   ✅ Git $versionStr 已安装" -ForegroundColor Green
        
        # 检查 Git 配置
        $gitUser = git config --global user.name 2>&1
        $gitEmail = git config --global user.email 2>&1
        if ($gitUser -and $gitEmail) {
            Write-Host "   ✅ Git 用户配置: $gitUser ($gitEmail)" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ Git 用户信息未配置" -ForegroundColor Yellow
            $warnings += "建议配置 Git: git config --global user.name 'Your Name'"
        }
    }
} catch {
    Write-Host "   ⚠️ Git 未安装 (可选)" -ForegroundColor Yellow
    $warnings += "Git 未安装，无法进行版本控制"
}

Write-Host ""

# ========================================
# 6. 检查 Docker
# ========================================
Write-Host "6. 检查 Docker..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version 2>&1
    if ($dockerVersion) {
        Write-Host "   ✅ Docker 已安装: $dockerVersion" -ForegroundColor Green
        
        # 检查 Docker 是否运行
        $dockerRunning = docker ps 2>&1
        if ($dockerRunning -notmatch "error") {
            Write-Host "   ✅ Docker 服务运行中" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ Docker 服务未运行，请启动 Docker Desktop" -ForegroundColor Yellow
            $warnings += "Docker 服务未运行"
        }
    }
} catch {
    Write-Host "   ⚠️ Docker 未安装 (数据库服务需要)" -ForegroundColor Yellow
    $warnings += "Docker 未安装，需手动安装 PostgreSQL/Redis"
}

Write-Host ""

# ========================================
# 7. 检查 PostgreSQL
# ========================================
Write-Host "7. 检查 PostgreSQL..." -ForegroundColor Yellow
try {
    $pgContainer = docker ps 2>&1 | Select-String "ai-ready-postgres"
    if ($pgContainer) {
        Write-Host "   ✅ PostgreSQL Docker 容器运行中" -ForegroundColor Green
        
        # 测试连接
        $pgTest = docker exec ai-ready-postgres pg_isready -U devuser -d devdb 2>&1
        if ($pgTest -match "accepting connections") {
            Write-Host "   ✅ PostgreSQL 接受连接" -ForegroundColor Green
        }
    } else {
        # 检查是否有停止的容器
        $pgStopped = docker ps -a 2>&1 | Select-String "ai-ready-postgres"
        if ($pgStopped) {
            Write-Host "   ⚠️ PostgreSQL 容器已停止" -ForegroundColor Yellow
            Write-Host "      启动命令: docker start ai-ready-postgres" -ForegroundColor Gray
            $warnings += "PostgreSQL 容器已停止"
        } else {
            Write-Host "   ⚠️ PostgreSQL 容器未创建" -ForegroundColor Yellow
            Write-Host "      创建命令见: docs/SETUP_GUIDE.md 第 2.5 步" -ForegroundColor Gray
            $warnings += "PostgreSQL 需要配置"
        }
    }
} catch {
    Write-Host "   ⚠️ 无法检查 PostgreSQL (Docker 不可用)" -ForegroundColor Yellow
    $warnings += "请手动验证 PostgreSQL: localhost:5432"
}

Write-Host ""

# ========================================
# 8. 检查 Redis
# ========================================
Write-Host "8. 检查 Redis..." -ForegroundColor Yellow
try {
    $redisContainer = docker ps 2>&1 | Select-String "ai-ready-redis"
    if ($redisContainer) {
        Write-Host "   ✅ Redis Docker 容器运行中" -ForegroundColor Green
        
        # 测试连接
        $redisTest = docker exec ai-ready-redis redis-cli ping 2>&1
        if ($redisTest -match "PONG") {
            Write-Host "   ✅ Redis 响应正常 (PONG)" -ForegroundColor Green
        }
    } else {
        $redisStopped = docker ps -a 2>&1 | Select-String "ai-ready-redis"
        if ($redisStopped) {
            Write-Host "   ⚠️ Redis 容器已停止" -ForegroundColor Yellow
            Write-Host "      启动命令: docker start ai-ready-redis" -ForegroundColor Gray
            $warnings += "Redis 容器已停止"
        } else {
            Write-Host "   ⚠️ Redis 容器未创建" -ForegroundColor Yellow
            Write-Host "      创建命令见: docs/SETUP_GUIDE.md 第 2.5 步" -ForegroundColor Gray
            $warnings += "Redis 需要配置"
        }
    }
} catch {
    Write-Host "   ⚠️ 无法检查 Redis (Docker 不可用)" -ForegroundColor Yellow
    $warnings += "请手动验证 Redis: localhost:6379"
}

Write-Host ""

# ========================================
# 9. 检查项目结构
# ========================================
Write-Host "9. 检查项目结构..." -ForegroundColor Yellow
$projectRoot = "I:\AI-Ready"
$requiredDirs = @(
    "core-base",
    "core-common",
    "core-api",
    "smart-admin-web",
    "docs",
    "scripts"
)

foreach ($dir in $requiredDirs) {
    $path = Join-Path $projectRoot $dir
    if (Test-Path $path) {
        Write-Host "   ✅ $dir 目录存在" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ $dir 目录不存在" -ForegroundColor Yellow
        $warnings += "$dir 目录不存在"
    }
}

Write-Host ""

# ========================================
# 10. 检查关键文件
# ========================================
Write-Host "10. 检查关键配置文件..." -ForegroundColor Yellow
$keyFiles = @(
    "pom.xml",
    "README.md",
    "docs/SETUP_GUIDE.md",
    "smart-admin-web/package.json",
    "smart-admin-web/vite.config.ts"
)

foreach ($file in $keyFiles) {
    $path = Join-Path $projectRoot $file
    if (Test-Path $path) {
        Write-Host "   ✅ $file 存在" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ $file 不存在" -ForegroundColor Yellow
    }
}

Write-Host ""

# ========================================
# 总结
# ========================================
Write-Host "==========================================" -ForegroundColor Green
Write-Host "环境验证结果" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

if ($allPassed) {
    Write-Host "✅ 核心环境验证通过！" -ForegroundColor Green
} else {
    Write-Host "❌ 部分核心环境缺失，请先完成安装" -ForegroundColor Red
}

if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Host "⚠️ 以下项目需要关注:" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "   - $warning" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "环境配置指南: docs/SETUP_GUIDE.md" -ForegroundColor Cyan
Write-Host "一键启动脚本: scripts\quick-start.ps1" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan