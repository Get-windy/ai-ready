#!/usr/bin/env pwsh
# 目录结构验证脚本
# 验证ERP模块目录结构是否符合规范

param(
    [string]$ProjectPath = ".",
    [switch]$Fix = $false,
    [switch]$Verbose = $false
)

Write-Host "=== ERP目录结构验证 ===" -ForegroundColor Cyan
Write-Host "项目路径: $ProjectPath" -ForegroundColor Yellow
Write-Host "修复模式: $Fix" -ForegroundColor Yellow
Write-Host "详细输出: $Verbose" -ForegroundColor Yellow
Write-Host ""

# 定义规范
$rules = @{
    "erp-modules-in-erp-folder" = @{
        Description = "所有ERP模块必须放在erp/目录下"
        Check = {
            $backendPath = Join-Path $ProjectPath "backend"
            if (-not (Test-Path $backendPath)) {
                Write-Host "错误: backend目录不存在" -ForegroundColor Red
                return $false
            }
            
            $invalidModules = @()
            $erpPath = Join-Path $backendPath "erp"
            
            # 检查backend根目录下是否有erp-开头的目录
            Get-ChildItem -Path $backendPath -Directory | ForEach-Object {
                if ($_.Name -match "^erp-") {
                    $invalidModules += $_.Name
                }
            }
            
            if ($invalidModules.Count -gt 0) {
                Write-Host "发现不符合规范的ERP模块:" -ForegroundColor Red
                $invalidModules | ForEach-Object {
                    Write-Host "  $_" -ForegroundColor Red
                }
                return $false
            }
            
            return $true
        }
        Fix = {
            $backendPath = Join-Path $ProjectPath "backend"
            $erpPath = Join-Path $backendPath "erp"
            
            # 创建erp目录（如果不存在）
            if (-not (Test-Path $erpPath)) {
                New-Item -Path $erpPath -ItemType Directory -Force
                Write-Host "创建erp目录: $erpPath" -ForegroundColor Green
            }
            
            # 移动erp-开头的模块到erp目录
            Get-ChildItem -Path $backendPath -Directory | ForEach-Object {
                if ($_.Name -match "^erp-") {
                    $sourcePath = $_.FullName
                    $destPath = Join-Path $erpPath $_.Name
                    
                    Write-Host "移动模块: $($_.Name) 到 erp/目录" -ForegroundColor Yellow
                    Move-Item -Path $sourcePath -Destination $destPath -Force
                }
            }
        }
    }
    
    "module-has-correct-structure" = @{
        Description = "ERP模块必须包含标准的Maven目录结构"
        Check = {
            $erpPath = Join-Path (Join-Path $ProjectPath "backend") "erp"
            if (-not (Test-Path $erpPath)) {
                return $true  # 如果没有erp目录，跳过此检查
            }
            
            $incorrectModules = @()
            Get-ChildItem -Path $erpPath -Directory | ForEach-Object {
                $modulePath = $_.FullName
                $expectedDirs = @("src/main/java", "src/main/resources", "src/test/java")
                
                foreach ($dir in $expectedDirs) {
                    $checkPath = Join-Path $modulePath $dir
                    if (-not (Test-Path $checkPath)) {
                        $incorrectModules += @{
                            Module = $_.Name
                            Missing = $dir
                        }
                    }
                }
            }
            
            if ($incorrectModules.Count -gt 0) {
                Write-Host "发现目录结构不完整的模块:" -ForegroundColor Red
                $incorrectModules | ForEach-Object {
                    Write-Host "  $($_.Module) 缺少: $($_.Missing)" -ForegroundColor Red
                }
                return $false
            }
            
            return $true
        }
        Fix = {
            $erpPath = Join-Path (Join-Path $ProjectPath "backend") "erp"
            Get-ChildItem -Path $erpPath -Directory | ForEach-Object {
                $modulePath = $_.FullName
                $requiredDirs = @(
                    "src/main/java/cn/aiedge/erp",
                    "src/main/resources",
                    "src/test/java/cn/aiedge/erp",
                    "src/test/resources"
                )
                
                foreach ($dir in $requiredDirs) {
                    $fullPath = Join-Path $modulePath $dir
                    if (-not (Test-Path $fullPath)) {
                        New-Item -Path $fullPath -ItemType Directory -Force
                        Write-Host "创建目录: $fullPath" -ForegroundColor Green
                    }
                }
            }
        }
    }
    
    "infrastructure-testing-exists" = @{
        Description = "测试框架必须放在infrastructure/testing/目录下"
        Check = {
            $testingPath = Join-Path (Join-Path (Join-Path $ProjectPath "backend") "infrastructure") "testing"
            return (Test-Path $testingPath)
        }
        Fix = {
            $testingPath = Join-Path (Join-Path (Join-Path $ProjectPath "backend") "infrastructure") "testing"
            New-Item -Path $testingPath -ItemType Directory -Force
            Write-Host "创建测试框架目录: $testingPath" -ForegroundColor Green
        }
    }
}

# 执行验证
$totalRules = $rules.Count
$passedRules = 0
$failedRules = 0
$fixedRules = 0

Write-Host "开始验证 $totalRules 条目录结构规则..." -ForegroundColor Cyan
Write-Host ""

foreach ($ruleName in $rules.Keys) {
    $rule = $rules[$ruleName]
    
    Write-Host "验证: $($rule.Description)" -ForegroundColor White
    if ($Verbose) {
        Write-Host "规则: $ruleName" -ForegroundColor Gray
    }
    
    try {
        $result = & $rule.Check
        
        if ($result) {
            Write-Host "  ✓ 通过" -ForegroundColor Green
            $passedRules++
        } else {
            Write-Host "  ✗ 失败" -ForegroundColor Red
            $failedRules++
            
            if ($Fix) {
                Write-Host "  尝试修复..." -ForegroundColor Yellow
                try {
                    & $rule.Fix
                    Write-Host "  ✓ 修复完成" -ForegroundColor Green
                    $fixedRules++
                } catch {
                    Write-Host "  ✗ 修复失败: $_" -ForegroundColor Red
                }
            }
        }
    } catch {
        Write-Host "  ✗ 验证失败: $_" -ForegroundColor Red
        $failedRules++
    }
    
    Write-Host ""
}

# 生成报告
Write-Host "=== 验证报告 ===" -ForegroundColor Cyan
Write-Host "总规则数: $totalRules" -ForegroundColor White
Write-Host "通过规则: $passedRules" -ForegroundColor Green
Write-Host "失败规则: $failedRules" -ForegroundColor Red

if ($Fix) {
    Write-Host "修复规则: $fixedRules" -ForegroundColor Yellow
}

if ($failedRules -eq 0) {
    Write-Host "✓ 所有目录结构规则验证通过" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ 发现 $failedRules 个目录结构问题" -ForegroundColor Red
    
    if ($Fix) {
        if ($fixedRules -eq $failedRules) {
            Write-Host "✓ 所有问题已修复" -ForegroundColor Green
            exit 0
        } else {
            Write-Host "✗ 部分问题修复失败，请手动检查" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "提示: 使用 -Fix 参数自动修复问题" -ForegroundColor Yellow
        exit 1
    }
}