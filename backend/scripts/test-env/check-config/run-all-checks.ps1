#Requires -Version 5.1
<#
.SYNOPSIS
    一键运行所有配置检查
.DESCRIPTION
    依次运行数据库、中间件、应用、服务、端口和连通性检查，生成综合报告
#>

param(
    [string]$ReportDir = "I:\AI-Ready\scripts\test-env\check-config\reports",
    [switch]$JsonReport,
    [switch]$Quiet
)

$ErrorActionPreference = "Continue"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 创建报告目录
if (!(Test-Path $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$reportFile = Join-Path $ReportDir "check-report-$timestamp.json"

$allResults = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    overallStatus = "PASS"
    summary = @{
        total = 0
        passed = 0
        failed = 0
        warnings = 0
    }
    categories = @()
}

$scripts = @(
    @{Name="数据库"; Script="check-database.ps1"},
    @{Name="中间件"; Script="check-middleware.ps1"},
    @{Name="应用配置"; Script="check-application.ps1"},
    @{Name="服务状态"; Script="check-services.ps1"},
    @{Name="端口监听"; Script="check-ports.ps1"},
    @{Name="连通性"; Script="check-connectivity.ps1"}
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AI-Ready 测试环境配置检查" -ForegroundColor Cyan
Write-Host "  开始时间: $($allResults.timestamp)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

foreach ($item in $scripts) {
    $scriptPath = Join-Path $scriptDir $item.Script
    if (Test-Path $scriptPath) {
        Write-Host "`n>>> 正在检查: $($item.Name) ..." -ForegroundColor Yellow
        try {
            $categoryReport = Join-Path $ReportDir "$($item.Name)-$timestamp.json"
            & $scriptPath -ReportPath $categoryReport | Out-String | ForEach-Object {
                if (-not $Quiet) { Write-Host $_ }
            }
            
            # 读取报告
            if (Test-Path $categoryReport) {
                $report = Get-Content $categoryReport -Raw | ConvertFrom-Json
                $allResults.categories += $report
                
                $passed = ($report.checks | Where-Object { $_.status -eq "PASS" }).Count
                $failed = ($report.checks | Where-Object { $_.status -eq "FAIL" }).Count
                $warnings = ($report.checks | Where-Object { $_.status -eq "WARN" }).Count
                
                $allResults.summary.total += $report.checks.Count
                $allResults.summary.passed += $passed
                $allResults.summary.failed += $failed
                $allResults.summary.warnings += $warnings
                
                if ($report.status -eq "FAIL") {
                    $allResults.overallStatus = "FAIL"
                }
                
                Write-Host "  结果: $passed 通过, $failed 失败, $warnings 警告" -ForegroundColor Gray
            }
        } catch {
            Write-Host "  错误: $_" -ForegroundColor Red
            $allResults.overallStatus = "FAIL"
        }
    } else {
        Write-Host "  跳过: $($item.Script) 不存在" -ForegroundColor Yellow
    }
}

# 输出汇总
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  检查汇总" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  总检查项: $($allResults.summary.total)" -ForegroundColor White
Write-Host "  通过: $($allResults.summary.passed)" -ForegroundColor Green
Write-Host "  失败: $($allResults.summary.failed)" -ForegroundColor Red
Write-Host "  警告: $($allResults.summary.warnings)" -ForegroundColor Yellow
Write-Host "  总体状态: $($allResults.overallStatus)" -ForegroundColor $(if ($allResults.overallStatus -eq "PASS") { "Green" } else { "Red" })
Write-Host "========================================" -ForegroundColor Cyan

# 保存综合报告
$allResults | ConvertTo-Json -Depth 4 | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "`n综合报告已保存: $reportFile" -ForegroundColor Cyan

# 如果有失败，列出详细错误
if ($allResults.summary.failed -gt 0) {
    Write-Host "`n失败的检查项:" -ForegroundColor Red
    foreach ($cat in $allResults.categories) {
        $fails = $cat.checks | Where-Object { $_.status -eq "FAIL" }
        foreach ($fail in $fails) {
            Write-Host "  [$($cat.category)] $($fail.name): $($fail.message)" -ForegroundColor Red
        }
    }
}

# 返回退出码
if ($allResults.overallStatus -eq "FAIL") {
    exit 1
} else {
    exit 0
}
