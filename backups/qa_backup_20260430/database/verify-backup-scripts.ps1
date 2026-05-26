# 数据库备份脚本验证脚本
# 版本: 1.0
# 创建日期: 2026-04-28

Write-Host "========== 数据库备份脚本验证开始 ==========" -ForegroundColor Cyan
Write-Host "验证时间: $(Get-Date)" -ForegroundColor Yellow
Write-Host ""

# 检查关键文件
$filesToCheck = @(
    @{Path = "I:\AI-Ready\qa\database\database-backup-recovery-drill-plan.md"; Description = "备份恢复演练计划"},
    @{Path = "I:\AI-Ready\scripts\backup\postgresql\backup_postgresql.sh"; Description = "PostgreSQL备份脚本"},
    @{Path = "I:\AI-Ready\scripts\backup\postgresql\restore_postgresql.sh"; Description = "PostgreSQL恢复脚本"},
    @{Path = "I:\AI-Ready\scripts\backup\redis\backup_redis.sh"; Description = "Redis备份脚本"},
    @{Path = "I:\AI-Ready\scripts\backup\redis\restore_redis.sh"; Description = "Redis恢复脚本"},
    @{Path = "I:\AI-Ready\qa\database\execute-backup-recovery-drill.sh"; Description = "演练执行脚本"}
)

$allFilesExist = $true
foreach ($file in $filesToCheck) {
    if (Test-Path $file.Path) {
        $fileSize = (Get-Item $file.Path).Length
        Write-Host "✅ $($file.Description)" -ForegroundColor Green
        Write-Host "   路径: $($file.Path)" -ForegroundColor Gray
        Write-Host "   大小: $($fileSize) 字节" -ForegroundColor Gray
    } else {
        Write-Host "❌ $($file.Description)" -ForegroundColor Red
        Write-Host "   路径: $($file.Path)" -ForegroundColor Gray
        Write-Host "   状态: 文件不存在" -ForegroundColor Red
        $allFilesExist = $false
    }
    Write-Host ""
}

# 检查目录结构
Write-Host "========== 目录结构验证 ==========" -ForegroundColor Cyan

$dirsToCheck = @(
    @{Path = "I:\AI-Ready\qa\database"; Description = "QA数据库目录"},
    @{Path = "I:\AI-Ready\scripts\backup\postgresql"; Description = "PostgreSQL备份脚本目录"},
    @{Path = "I:\AI-Ready\scripts\backup\redis"; Description = "Redis备份脚本目录"},
    @{Path = "I:\AI-Ready\qa\database\logs"; Description = "演练日志目录"},
    @{Path = "I:\AI-Ready\qa\database\reports"; Description = "演练报告目录"}
)

foreach ($dir in $dirsToCheck) {
    if (Test-Path $dir.Path) {
        $itemCount = (Get-ChildItem $dir.Path -File | Measure-Object).Count
        Write-Host "✅ $($dir.Description)" -ForegroundColor Green
        Write-Host "   路径: $($dir.Path)" -ForegroundColor Gray
        Write-Host "   文件数量: $itemCount" -ForegroundColor Gray
    } else {
        Write-Host "⚠️ $($dir.Description)" -ForegroundColor Yellow
        Write-Host "   路径: $($dir.Path)" -ForegroundColor Gray
        Write-Host "   状态: 目录不存在" -ForegroundColor Yellow
    }
    Write-Host ""
}

# 检查文件内容
Write-Host "========== 文件内容抽样检查 ==========" -ForegroundColor Cyan

$sampleFiles = @(
    @{Path = "I:\AI-Ready\qa\database\database-backup-recovery-drill-plan.md"; Lines = 5},
    @{Path = "I:\AI-Ready\scripts\backup\postgresql\backup_postgresql.sh"; Lines = 10},
    @{Path = "I:\AI-Ready\scripts\backup\redis\backup_redis.sh"; Lines = 10}
)

foreach ($sample in $sampleFiles) {
    if (Test-Path $sample.Path) {
        Write-Host "📄 $(Split-Path $sample.Path -Leaf)" -ForegroundColor Blue
        try {
            $content = Get-Content $sample.Path -TotalCount $sample.Lines
            foreach ($line in $content) {
                Write-Host "   $line" -ForegroundColor Gray
            }
        } catch {
            Write-Host "   无法读取文件内容" -ForegroundColor Red
        }
        Write-Host ""
    }
}

# 生成验证报告
Write-Host "========== 验证报告 ==========" -ForegroundColor Cyan

if ($allFilesExist) {
    Write-Host "✅ 所有关键文件检查通过" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 已完成的交付物:" -ForegroundColor Blue
    Write-Host "   1. 数据库备份恢复演练计划文档" -ForegroundColor Gray
    Write-Host "   2. PostgreSQL备份脚本 (backup_postgresql.sh)" -ForegroundColor Gray
    Write-Host "   3. PostgreSQL恢复脚本 (restore_postgresql.sh)" -ForegroundColor Gray
    Write-Host "   4. Redis备份脚本 (backup_redis.sh)" -ForegroundColor Gray
    Write-Host "   5. Redis恢复脚本 (restore_redis.sh)" -ForegroundColor Gray
    Write-Host "   6. 演练执行脚本 (execute-backup-recovery-drill.sh)" -ForegroundColor Gray
    Write-Host "   7. 目录结构 (scripts/backup/, qa/database/)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "📅 后续步骤建议:" -ForegroundColor Yellow
    Write-Host "   1. 在实际测试环境部署数据库" -ForegroundColor Gray
    Write-Host "   2. 执行完整的备份恢复演练" -ForegroundColor Gray
    Write-Host "   3. 验证备份文件的完整性和可恢复性" -ForegroundColor Gray
    Write-Host "   4. 配置定时备份任务" -ForegroundColor Gray
    Write-Host "   5. 建立备份监控和告警机制" -ForegroundColor Gray
} else {
    Write-Host "❌ 部分文件缺失，请检查创建过程" -ForegroundColor Red
    Write-Host "   建议重新执行文件创建步骤" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========== 验证完成 ==========" -ForegroundColor Cyan
Write-Host "验证时间: $(Get-Date)" -ForegroundColor Yellow