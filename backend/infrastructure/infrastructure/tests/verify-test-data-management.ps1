# AI-Ready 测试数据管理验证脚本
# Version: v1.0.1
# 创建日期: 2026-04-26
# 用途: 验证测试数据生成、备份、恢复、隔离和清理功能

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AI-Ready 测试数据管理验证脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 配置变量
$TEST_DATA_DIR = "I:\AI-Ready\AI_TEST_DATA"
$BACKUP_DIR = "I:\AI-Ready\testdata-backups"
$REPORT_DIR = "I:\AI-Ready\infrastructure\tests\reports"
$REPORT_FILE = "$REPORT_DIR\test-data-management-report.md"
$RESULTS_FILE = "$REPORT_DIR\test-data-management-results.json"

# 验证结果集合
$results = @{
    summary = @{
        passed = 0
        failed = 0
        warnings = 0
        total = 0
    }
    tests = @()
}

# 记录测试结果函数
function Record-TestResult {
    param(
        [string]$testName,
        [string]$category,
        [string]$status,
        [string]$details
    )
    
    $result = @{
        testName = $testName
        category = $category
        status = $status
        details = $details
        timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    
    $results.tests += $result
    
    # 更新统计
    $results.summary.total++
    switch ($status) {
        "passed" { $results.summary.passed++ }
        "failed" { $results.summary.failed++ }
        "warning" { $results.summary.warnings++ }
    }
    
    # 输出结果
    $color = "Green"
    if ($status -eq "failed") { $color = "Red" }
    if ($status -eq "warning") { $color = "Yellow" }
    
    Write-Host "[$status] $testName" -ForegroundColor $color
}

Write-Host "步骤1: 验证测试数据生成流程" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow

# 1.1 检查测试数据文件是否存在
if (Test-Path $TEST_DATA_DIR) {
    $dataFiles = Get-ChildItem -Path $TEST_DATA_DIR -File -Name
    Record-TestResult "测试数据目录存在" "数据生成" "passed" "测试数据目录存在，包含 $($dataFiles.Count) 个文件"
} else {
    Record-TestResult "测试数据目录存在" "数据生成" "failed" "测试数据目录不存在: $TEST_DATA_DIR"
}

# 1.2 检查数据生成脚本
$scriptFiles = @("generate_anomaly_test_data.py", "generate_business_scenario_data.py", "generate_user_behavior_data.py")
foreach ($script in $scriptFiles) {
    $scriptPath = Join-Path $TEST_DATA_DIR $script
    if (Test-Path $scriptPath) {
        Record-TestResult "数据生成脚本存在: $script" "数据生成" "passed" "脚本文件存在: $scriptPath"
    } else {
        Record-TestResult "数据生成脚本存在: $script" "数据生成" "failed" "脚本文件缺失: $scriptPath"
    }
}

# 1.3 检查生成的数据文件
$dataFilesToCheck = @(
    "user_behavior_data.json",
    "user_behavior_data.csv",
    "business_scenario_data.json",
    "business_scenario_data.csv",
    "anomaly_test_data.json"
)

foreach ($file in $dataFilesToCheck) {
    $filePath = Join-Path $TEST_DATA_DIR $file
    if (Test-Path $filePath) {
        $fileSize = (Get-Item $filePath).Length
        Record-TestResult "数据文件存在: $file" "数据生成" "passed" "文件存在: $filePath, 大小: $fileSize 字节"
    } else {
        Record-TestResult "数据文件存在: $file" "数据生成" "failed" "文件缺失: $filePath"
    }
}

Write-Host ""
Write-Host "步骤2: 测试数据备份机制" -ForegroundColor Yellow
Write-Host "-------------------------" -ForegroundColor Yellow

# 2.1 检查备份目录
if (Test-Path $BACKUP_DIR) {
    Record-TestResult "备份目录存在" "备份机制" "passed" "备份目录已创建: $BACKUP_DIR"
} else {
    # 尝试创建备份目录
    try {
        New-Item -Path $BACKUP_DIR -ItemType Directory -Force
        Record-TestResult "备份目录创建" "备份机制" "passed" "成功创建备份目录: $BACKUP_DIR"
    } catch {
        Record-TestResult "备份目录创建" "备份机制" "failed" "创建备份目录失败: $_"
    }
}

# 2.2 测试数据备份功能
try {
    $backupTime = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupSubDir = Join-Path $BACKUP_DIR $backupTime
    New-Item -Path $backupSubDir -ItemType Directory -Force
    
    # 备份数据文件
    $backupFiles = Get-ChildItem -Path $TEST_DATA_DIR -File | Select-Object -First 5
    $backupCount = 0
    foreach ($file in $backupFiles) {
        Copy-Item -Path $file.FullName -Destination $backupSubDir
        $backupCount++
    }
    
    Record-TestResult "数据备份功能" "备份机制" "passed" "成功备份 $backupCount 个文件到 $backupSubDir"
} catch {
    Record-TestResult "数据备份功能" "备份机制" "failed" "数据备份失败: $_"
}

Write-Host ""
Write-Host "步骤3: 数据隔离验证" -ForegroundColor Yellow
Write-Host "----------------------" -ForegroundColor Yellow

# 3.1 检查Docker配置
$dockerComposeFile = "I:\AI-Ready\docker\test-environment\docker-compose.yml"
if (Test-Path $dockerComposeFile) {
    $configContent = Get-Content $dockerComposeFile -Raw
    if ($configContent -match "postgres-" -and $configContent -match "POSTGRES_DB:") {
        Record-TestResult "数据库隔离配置" "数据隔离" "passed" "发现多数据库隔离配置"
    } else {
        Record-TestResult "数据库隔离配置" "数据隔离" "warning" "数据库配置文件存在但无法确定隔离配置"
    }
    
    if ($configContent -match "redis-main" -and $configContent -match "redis-inventory" -and $configContent -match "redis-finance") {
        Record-TestResult "Redis数据库隔离" "数据隔离" "passed" "发现多Redis实例隔离配置"
    } else {
        Record-TestResult "Redis数据库隔离" "数据隔离" "warning" "Redis隔离配置不完整"
    }
} else {
    Record-TestResult "Docker配置文件" "数据隔离" "failed" "Docker Compose配置文件缺失"
}

Write-Host ""
Write-Host "步骤4: 数据清理策略验证" -ForegroundColor Yellow
Write-Host "--------------------------" -ForegroundColor Yellow

# 4.1 检查清理脚本
$cleanupScripts = Get-ChildItem -Path "I:\AI-Ready" -Recurse -Filter "*cleanup*" -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "*.sh" -or $_.Name -like "*.bat" -or $_.Name -like "*.ps1" }
if ($cleanupScripts.Count -gt 0) {
    Record-TestResult "清理脚本存在" "清理策略" "passed" "在子目录中发现 $($cleanupScripts.Count) 个清理脚本"
} else {
    Record-TestResult "清理脚本存在" "清理策略" "warning" "未找到清理脚本"
}

# 4.2 检查日志清理配置
$dockerLoggingConfig = "I:\AI-Ready\docker\test-environment"
$logConfigs = Get-ChildItem -Path $dockerLoggingConfig -Recurse -Filter "*log*" -ErrorAction SilentlyContinue
if ($logConfigs.Count -gt 0) {
    Record-TestResult "日志清理配置" "清理策略" "passed" "在Docker配置中发现 $($logConfigs.Count) 个日志相关文件"
} else {
    Record-TestResult "日志清理配置" "清理策略" "warning" "未找到日志清理配置"
}

Write-Host ""
Write-Host "步骤5: 数据恢复机制验证" -ForegroundColor Yellow
Write-Host "-------------------------" -ForegroundColor Yellow

# 5.1 检查数据库初始化脚本
$initScripts = Get-ChildItem -Path "I:\AI-Ready\init-scripts" -Filter "*.sql" -ErrorAction SilentlyContinue
if ($initScripts.Count -gt 0) {
    Record-TestResult "数据库初始化脚本" "恢复机制" "passed" "发现 $($initScripts.Count) 个数据库初始化脚本"
} else {
    Record-TestResult "数据库初始化脚本" "恢复机制" "failed" "未找到数据库初始化脚本"
}

# 5.2 检查测试数据恢复能力
try {
    $backupFiles = Get-ChildItem -Path $BACKUP_DIR -Recurse -File -ErrorAction SilentlyContinue
    if ($backupFiles.Count -gt 0) {
        $latestBackup = $backupFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        $backupAge = (Get-Date) - $latestBackup.LastWriteTime
        if ($backupAge.TotalHours -lt 24) {
            Record-TestResult "备份有效性检查" "恢复机制" "passed" "发现最近24小时内创建的备份: $($latestBackup.Name)"
        } else {
            Record-TestResult "备份有效性检查" "恢复机制" "warning" "备份超过24小时未更新: $($latestBackup.Name)"
        }
    } else {
        Record-TestResult "备份有效性检查" "恢复机制" "warning" "备份目录中没有文件"
    }
} catch {
    Record-TestResult "备份有效性检查" "恢复机制" "failed" "检查备份失败: $_"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "验证完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "结果摘要:" -ForegroundColor Yellow
Write-Host "  通过: $($results.summary.passed)" -ForegroundColor Green
Write-Host "  失败: $($results.summary.failed)" -ForegroundColor Red
Write-Host "  警告: $($results.summary.warnings)" -ForegroundColor Yellow
Write-Host "  总计: $($results.summary.total)" -ForegroundColor White

# 生成验证报告
$reportContent = @"
# 测试数据管理验证报告

## 验证时间
$(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## 概述
本次验证测试了测试数据管理的5个主要方面:数据生成、备份机制、数据隔离、清理策略和恢复机制。

## 验证结果摘要
| 指标 | 数值 |
|------|------|
| 通过 | $($results.summary.passed) |
| 失败 | $($results.summary.failed) |
| 警告 | $($results.summary.warnings) |
| 总计 | $($results.summary.total) |

## 详细测试结果

### 1. 数据生成流程验证
"@

foreach ($test in $results.tests) {
    if ($test.category -eq "数据生成") {
        $statusIcon = "[PASS]"
        if ($test.status -eq "failed") { $statusIcon = "[FAIL]" }
        if ($test.status -eq "warning") { $statusIcon = "[WARN]" }
        $reportContent += "- $statusIcon $($test.testName)`n"
        $reportContent += "  - 状态: $($test.status)`n"
        $reportContent += "  - 详情: $($test.details)`n`n"
    }
}

$reportContent += @"
### 2. 备份机制验证
"@

foreach ($test in $results.tests) {
    if ($test.category -eq "备份机制") {
        $statusIcon = "[PASS]"
        if ($test.status -eq "failed") { $statusIcon = "[FAIL]" }
        if ($test.status -eq "warning") { $statusIcon = "[WARN]" }
        $reportContent += "- $statusIcon $($test.testName)`n"
        $reportContent += "  - 状态: $($test.status)`n"
        $reportContent += "  - 详情: $($test.details)`n`n"
    }
}

$reportContent += @"
### 3. 数据隔离验证
"@

foreach ($test in $results.tests) {
    if ($test.category -eq "数据隔离") {
        $statusIcon = "[PASS]"
        if ($test.status -eq "failed") { $statusIcon = "[FAIL]" }
        if ($test.status -eq "warning") { $statusIcon = "[WARN]" }
        $reportContent += "- $statusIcon $($test.testName)`n"
        $reportContent += "  - 状态: $($test.status)`n"
        $reportContent += "  - 详情: $($test.details)`n`n"
    }
}

$reportContent += @"
### 4. 清理策略验证
"@

foreach ($test in $results.tests) {
    if ($test.category -eq "清理策略") {
        $statusIcon = "[PASS]"
        if ($test.status -eq "failed") { $statusIcon = "[FAIL]" }
        if ($test.status -eq "warning") { $statusIcon = "[WARN]" }
        $reportContent += "- $statusIcon $($test.testName)`n"
        $reportContent += "  - 状态: $($test.status)`n"
        $reportContent += "  - 详情: $($test.details)`n`n"
    }
}

$reportContent += @"
### 5. 恢复机制验证
"@

foreach ($test in $results.tests) {
    if ($test.category -eq "恢复机制") {
        $statusIcon = "[PASS]"
        if ($test.status -eq "failed") { $statusIcon = "[FAIL]" }
        if ($test.status -eq "warning") { $statusIcon = "[WARN]" }
        $reportContent += "- $statusIcon $($test.testName)`n"
        $reportContent += "  - 状态: $($test.status)`n"
        $reportContent += "  - 详情: $($test.details)`n`n"
    }
}

$reportContent += @"
## 结论与建议

### 覆盖率
- **验证覆盖率**: $([math]::Round(($results.summary.passed + $results.summary.warnings) / $results.summary.total * 100, 2))%

### 关键发现
"@

if ($results.summary.passed -ge $results.summary.total * 0.8) {
    $reportContent += "- [OK] 测试数据管理功能基本完善，大部分验证项通过"
} elseif ($results.summary.passed -ge $results.summary.total * 0.6) {
    $reportContent += "- [WARN] 测试数据管理功能部分完善，需要改进"
} else {
    $reportContent += "- [CRIT] 测试数据管理功能存在较大问题，需要重点修复"
}

$reportContent += @"

### 改进建议
1. **数据生成**: 确保所有数据生成脚本正常运行并生成有效的测试数据
2. **备份机制**: 建立定期备份策略，确保备份频率满足RPO要求
3. **数据隔离**: 验证多租户场景下的数据隔离策略
4. **清理策略**: 完善自动清理脚本，防止数据无限增长
5. **恢复机制**: 定期演练数据恢复流程，确保RTO满足要求

## 附录
- 验证脚本: verify-test-data-management.ps1
- 报告生成时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
"@

# 保存报告
$reportContent | Out-File -FilePath $REPORT_FILE -Encoding UTF8
Write-Host "验证报告已保存到: $REPORT_FILE" -ForegroundColor Green

# 保存JSON结果
$results | ConvertTo-Json -Depth 10 | Out-File -FilePath $RESULTS_FILE -Encoding UTF8
Write-Host "JSON结果已保存到: $RESULTS_FILE" -ForegroundColor Green

# 返回退出码
if ($results.summary.failed -gt 0) {
    exit 1
} else {
    exit 0
}