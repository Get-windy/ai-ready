# Sprint 27+1 测试环境配置验证脚本

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   Sprint 27+1 测试环境配置验证" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$errors = @()
$warnings = @()
$success = @()

# 1. 检查Docker Compose文件
Write-Host "`n[1/8] 验证Docker Compose文件..." -ForegroundColor Yellow
$composeFile = "docker-compose-sprint-27-1.yml"
if (Test-Path $composeFile) {
    $size = (Get-Item $composeFile).Length
    if ($size -gt 1000) {
        $success += "✅ Docker Compose文件存在且有效 ($size bytes)"
    } else {
        $errors += "❌ Docker Compose文件太小，可能有问题"
    }
} else {
    $errors += "❌ Docker Compose文件不存在: $composeFile"
}

# 2. 检查启动脚本
Write-Host "`n[2/8] 验证启动脚本..." -ForegroundColor Yellow
$startScript = "start-sprint-27-1.ps1"
if (Test-Path $startScript) {
    $success += "✅ 启动脚本存在"
} else {
    $warnings += "⚠️  启动脚本不存在，但可以手动启动"
}

# 3. 检查目录结构
Write-Host "`n[3/8] 验证目录结构..." -ForegroundColor Yellow
$requiredDirs = @(
    "init-scripts/sprint/main",
    "init-scripts/sprint/inventory",
    "init-scripts/sprint/finance", 
    "init-scripts/sprint/ai",
    "prometheus-sprint",
    "grafana-sprint",
    "postgres-exporter",
    "performance-test-config"
)

foreach ($dir in $requiredDirs) {
    if (Test-Path $dir) {
        $success += "✅ 目录存在: $dir"
    } else {
        $warnings += "⚠️  目录不存在: $dir (将在启动时创建)"
    }
}

# 4. 检查配置文件
Write-Host "`n[4/8] 验证配置文件..." -ForegroundColor Yellow
$requiredFiles = @(
    "prometheus-sprint/prometheus.yml",
    "postgres-exporter/queries.yaml",
    "performance-test-config/test_config.yaml"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        $size = (Get-Item $file).Length
        if ($size -gt 100) {
            $success += "✅ 配置文件存在: $file ($size bytes)"
        } else {
            $warnings += "⚠️  配置文件太小: $file ($size bytes)"
        }
    } else {
        $errors += "❌ 配置文件不存在: $file"
    }
}

# 5. 检查性能测试代码
Write-Host "`n[5/8] 验证性能测试代码..." -ForegroundColor Yellow
$testFiles = @(
    "..\..\..\qa\performance\database\performance_test_runner.py",
    "..\..\..\qa\performance\database\performance_test_utils.py",
    "..\..\..\qa\performance\database\performance_monitor.py",
    "..\..\..\qa\performance\database\Dockerfile.performance",
    "..\..\..\qa\performance\database\requirements.txt",
    "..\..\..\qa\performance\database\DEPLOYMENT.md"
)

foreach ($file in $testFiles) {
    if (Test-Path $file) {
        $size = (Get-Item $file).Length
        $success += "✅ 测试文件存在: $(Split-Path $file -Leaf) ($size bytes)"
    } else {
        $errors += "❌ 测试文件不存在: $file"
    }
}

# 6. 检查Docker配置
Write-Host "`n[6/8] 验证Docker配置..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    if ($LASTEXITCODE -eq 0) {
        $success += "✅ Docker已安装: $dockerVersion"
    } else {
        $errors += "❌ Docker未正确安装"
    }
} catch {
    $errors += "❌ Docker未安装"
}

# 7. 检查端口占用
Write-Host "`n[7/8] 检查端口占用..." -ForegroundColor Yellow
$ports = @(5432, 5433, 5434, 5435, 8080, 8081, 8082, 8083, 9090, 3000, 8000)

foreach ($port in $ports) {
    $process = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($process) {
        $processName = (Get-Process -Id $process.OwningProcess -ErrorAction SilentlyContinue).ProcessName
        $warnings += "⚠️  端口 $port 被占用: $processName"
    } else {
        $success += "✅ 端口 $port 可用"
    }
}

# 8. 检查系统资源
Write-Host "`n[8/8] 检查系统资源..." -ForegroundColor Yellow
try {
    $memory = Get-CimInstance Win32_OperatingSystem
    $freeMemoryGB = [math]::Round($memory.FreePhysicalMemory / 1MB, 2)
    $totalMemoryGB = [math]::Round($memory.TotalVisibleMemorySize / 1MB, 2)
    
    if ($freeMemoryGB -gt 4) {
        $success += "✅ 内存充足: $freeMemoryGB GB 可用 / $totalMemoryGB GB 总量"
    } else {
        $warnings += "⚠️  内存可能不足: $freeMemoryGB GB 可用 / $totalMemoryGB GB 总量"
    }
} catch {
    $warnings += "⚠️  无法获取内存信息"
}

# 显示验证结果
Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "          验证结果汇总" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($errors.Count -gt 0) {
    Write-Host "`n❌ 发现错误 ($($errors.Count) 个):" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "  $error" -ForegroundColor Red
    }
} else {
    Write-Host "`n✅ 未发现严重错误" -ForegroundColor Green
}

if ($warnings.Count -gt 0) {
    Write-Host "`n⚠️  发现警告 ($($warnings.Count) 个):" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "  $warning" -ForegroundColor Yellow
    }
}

if ($success.Count -gt 0) {
    Write-Host "`n✅ 验证通过项 ($($success.Count) 个):" -ForegroundColor Green
    foreach ($item in $success) {
        Write-Host "  $item" -ForegroundColor Green
    }
}

# 总结和建议
Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "          建议行动" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($errors.Count -eq 0) {
    Write-Host "`n✅ 环境配置基本正确，可以启动测试环境" -ForegroundColor Green
    Write-Host "`n建议步骤:" -ForegroundColor White
    Write-Host "1. 运行启动脚本: .\start-sprint-27-1.ps1" -ForegroundColor Gray
    Write-Host "2. 验证服务状态: docker-compose -f docker-compose-sprint-27-1.yml ps" -ForegroundColor Gray
    Write-Host "3. 运行性能测试: 参考 DEPLOYMENT.md 文档" -ForegroundColor Gray
} else {
    Write-Host "`n❌ 存在配置错误，需要先修复" -ForegroundColor Red
    Write-Host "`n建议步骤:" -ForegroundColor White
    Write-Host "1. 修复上述错误" -ForegroundColor Gray
    Write-Host "2. 重新运行验证脚本" -ForegroundColor Gray
    Write-Host "3. 确认所有检查通过后再启动环境" -ForegroundColor Gray
}

if ($warnings.Count -gt 0) {
    Write-Host "`n⚠️  注意: 存在警告，建议解决这些问题以获得最佳测试效果" -ForegroundColor Yellow
}

# 显示预估资源需求
Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "          资源需求预估" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

Write-Host "`n最低配置要求:" -ForegroundColor White
Write-Host "- CPU: 4核心" -ForegroundColor Gray
Write-Host "- 内存: 8 GB" -ForegroundColor Gray
Write-Host "- 磁盘: 20 GB 可用空间" -ForegroundColor Gray
Write-Host "- Docker: 4.20+ 版本" -ForegroundColor Gray

Write-Host "`n推荐配置:" -ForegroundColor White
Write-Host "- CPU: 8核心" -ForegroundColor Gray
Write-Host "- 内存: 16 GB" -ForegroundColor Gray
Write-Host "- 磁盘: 50 GB 可用空间" -ForegroundColor Gray
Write-Host "- 网络: 稳定的互联网连接" -ForegroundColor Gray

Write-Host "`n预计启动时间:" -ForegroundColor White
Write-Host "- 首次启动: 5-10 分钟 (下载镜像)" -ForegroundColor Gray
Write-Host "- 后续启动: 1-2 分钟" -ForegroundColor Gray
Write-Host "- 数据库初始化: 30-60 秒" -ForegroundColor Gray

Write-Host "`n测试时间预估:" -ForegroundColor White
Write-Host "- 性能分析: 2-5 分钟" -ForegroundColor Gray
Write-Host "- 性能测试: 5-10 分钟" -ForegroundColor Gray
Write-Host "- 压力测试: 10-30 分钟" -ForegroundColor Gray
Write-Host "- 完整测试套件: 20-45 分钟" -ForegroundColor Gray

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "          验证完成" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($errors.Count -eq 0) {
    Write-Host "`n🚀 可以开始启动测试环境！" -ForegroundColor Green
} else {
    Write-Host "`n🛑 需要先修复错误才能继续" -ForegroundColor Red
    exit 1
}