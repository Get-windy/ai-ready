#!/usr/bin/env pwsh
# 包命名验证脚本
# 验证Java包命名是否符合ERP架构规范

param(
    [string]$ProjectPath = ".",
    [switch]$Fix = $false,
    [switch]$Verbose = $false
)

Write-Host "=== ERP包命名验证 ===" -ForegroundColor Cyan
Write-Host "项目路径: $ProjectPath" -ForegroundColor Yellow
Write-Host "修复模式: $Fix" -ForegroundColor Yellow
Write-Host "详细输出: $Verbose" -ForegroundColor Yellow
Write-Host ""

# 定义包命名规范
$packageRules = @{
    "erp-package-prefix" = @{
        Description = "所有ERP模块包必须使用cn.aiedge.erp前缀"
        Pattern = "^cn\.aiedge\.erp\."
        Check = {
            param($javaFile)
            $content = Get-Content $javaFile -Raw
            $packageMatch = [regex]::Match($content, '^\s*package\s+([\w\.]+)\s*;')
            
            if ($packageMatch.Success) {
                $packageName = $packageMatch.Groups[1].Value
                return $packageName -match $this.Pattern
            }
            return $false
        }
        Fix = {
            param($javaFile)
            $relativePath = $javaFile.Replace($ProjectPath, "").TrimStart("\")
            
            # 根据文件路径推断正确的包名
            if ($relativePath -match "backend[\\/]erp[\\/]([^\\/]+)[\\/]src[\\/]main[\\/]java[\\/](.+)\.java$") {
                $moduleName = $matches[1]
                $pathParts = $matches[2].Replace("\", ".")
                $correctPackage = "cn.aiedge.erp.$moduleName.$pathParts"
                $correctPackage = $correctPackage -replace '\.java$', '' -replace '\.$', ''
                
                $content = Get-Content $javaFile -Raw
                $newContent = $content -replace '^\s*package\s+[\w\.]+\s*;', "package $correctPackage;"
                
                Set-Content -Path $javaFile -Value $newContent -Encoding UTF8
                Write-Host "  修复包名: $correctPackage" -ForegroundColor Green
                return $true
            }
            return $false
        }
    }
    
    "controller-package" = @{
        Description = "Controller类必须放在controller包中"
        Pattern = "\.controller\."
        Check = {
            param($javaFile)
            if ($javaFile.Name -match "Controller\.java$") {
                $content = Get-Content $javaFile -Raw
                $packageMatch = [regex]::Match($content, '^\s*package\s+([\w\.]+)\s*;')
                
                if ($packageMatch.Success) {
                    $packageName = $packageMatch.Groups[1].Value
                    return $packageName -match $this.Pattern
                }
            }
            return $true  # 非Controller文件跳过此检查
        }
        Fix = {
            param($javaFile)
            # 移动文件到正确的目录
            $targetDir = $javaFile.DirectoryName -replace '[^\\/]+$', 'controller'
            $targetPath = Join-Path $targetDir $javaFile.Name
            
            if (-not (Test-Path $targetDir)) {
                New-Item -Path $targetDir -ItemType Directory -Force
            }
            
            Move-Item -Path $javaFile.FullName -Destination $targetPath -Force
            
            # 更新包名
            $relativePath = $targetPath.Replace($ProjectPath, "").TrimStart("\")
            if ($relativePath -match "src[\\/]main[\\/]java[\\/](.+)\.java$") {
                $packagePath = $matches[1].Replace("\", ".")
                $packageName = $packagePath -replace '\.java$', ''
                
                $content = Get-Content $targetPath -Raw
                $newContent = $content -replace '^\s*package\s+[\w\.]+\s*;', "package $packageName;"
                
                Set-Content -Path $targetPath -Value $newContent -Encoding UTF8
                Write-Host "  移动Controller到controller包: $packageName" -ForegroundColor Green
                return $true
            }
            return $false
        }
    }
    
    "service-package" = @{
        Description = "Service类必须放在service包中"
        Pattern = "\.service\."
        Check = {
            param($javaFile)
            if ($javaFile.Name -match "(Service|ServiceImpl)\.java$") {
                $content = Get-Content $javaFile -Raw
                $packageMatch = [regex]::Match($content, '^\s*package\s+([\w\.]+)\s*;')
                
                if ($packageMatch.Success) {
                    $packageName = $packageMatch.Groups[1].Value
                    return $packageName -match $this.Pattern
                }
            }
            return $true  # 非Service文件跳过此检查
        }
        Fix = {
            param($javaFile)
            # 移动文件到正确的目录
            $targetDir = $javaFile.DirectoryName -replace '[^\\/]+$', 'service'
            $targetPath = Join-Path $targetDir $javaFile.Name
            
            if (-not (Test-Path $targetDir)) {
                New-Item -Path $targetDir -ItemType Directory -Force
            }
            
            Move-Item -Path $javaFile.FullName -Destination $targetPath -Force
            
            # 更新包名
            $relativePath = $targetPath.Replace($ProjectPath, "").TrimStart("\")
            if ($relativePath -match "src[\\/]main[\\/]java[\\/](.+)\.java$") {
                $packagePath = $matches[1].Replace("\", ".")
                $packageName = $packagePath -replace '\.java$', ''
                
                $content = Get-Content $targetPath -Raw
                $newContent = $content -replace '^\s*package\s+[\w\.]+\s*;', "package $packageName;"
                
                Set-Content -Path $targetPath -Value $newContent -Encoding UTF8
                Write-Host "  移动Service到service包: $packageName" -ForegroundColor Green
                return $true
            }
            return $false
        }
    }
}

# 查找所有Java文件
$javaFiles = @()
$srcDirs = @(
    Join-Path $ProjectPath "backend\erp\*\src\main\java\**\*.java",
    Join-Path $ProjectPath "backend\core\*\src\main\java\**\*.java"
)

foreach ($pattern in $srcDirs) {
    $files = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue
    $javaFiles += $files
}

Write-Host "找到 $($javaFiles.Count) 个Java文件" -ForegroundColor Cyan
Write-Host ""

if ($javaFiles.Count -eq 0) {
    Write-Host "未找到Java文件，跳过包命名验证" -ForegroundColor Yellow
    exit 0
}

# 执行验证
$totalFiles = $javaFiles.Count
$passedFiles = 0
$failedFiles = 0
$fixedFiles = 0
$results = @{}

Write-Host "开始验证包命名规范..." -ForegroundColor Cyan
Write-Host ""

foreach ($ruleName in $packageRules.Keys) {
    $rule = $packageRules[$ruleName]
    $results[$ruleName] = @{
        Description = $rule.Description
        Passed = @()
        Failed = @()
        Fixed = @()
    }
    
    Write-Host "验证规则: $($rule.Description)" -ForegroundColor White
    
    $rulePassed = 0
    $ruleFailed = 0
    $ruleFixed = 0
    
    foreach ($javaFile in $javaFiles) {
        $checkResult = & $rule.Check $javaFile
        
        if ($checkResult) {
            $rulePassed++
            $results[$ruleName].Passed += $javaFile
        } else {
            $ruleFailed++
            $results[$ruleName].Failed += $javaFile
            
            if ($Fix -and $rule.Fix) {
                Write-Host "  修复文件: $($javaFile.Name)" -ForegroundColor Yellow
                $fixResult = & $rule.Fix $javaFile
                if ($fixResult) {
                    $ruleFixed++
                    $fixedFiles++
                    $results[$ruleName].Fixed += $javaFile
                }
            }
        }
    }
    
    Write-Host "  通过: $rulePassed, 失败: $ruleFailed, 修复: $ruleFixed" -ForegroundColor Gray
    Write-Host ""
    
    $passedFiles += $rulePassed
    $failedFiles += $ruleFailed
}

# 生成详细报告
if ($Verbose -or $failedFiles -gt 0) {
    Write-Host "=== 详细验证结果 ===" -ForegroundColor Cyan
    
    foreach ($ruleName in $results.Keys) {
        $result = $results[$ruleName]
        
        if ($result.Failed.Count -gt 0) {
            Write-Host "规则: $($result.Description)" -ForegroundColor White
            Write-Host "失败文件 ($($result.Failed.Count)个):" -ForegroundColor Red
            
            foreach ($file in $result.Failed) {
                Write-Host "  $($file.FullName)" -ForegroundColor Red
            }
            
            if ($result.Fixed.Count -gt 0) {
                Write-Host "已修复文件 ($($result.Fixed.Count)个):" -ForegroundColor Green
                foreach ($file in $result.Fixed) {
                    Write-Host "  $($file.FullName)" -ForegroundColor Green
                }
            }
            
            Write-Host ""
        }
    }
}

# 生成摘要报告
Write-Host "=== 验证报告 ===" -ForegroundColor Cyan
Write-Host "总文件数: $totalFiles" -ForegroundColor White
Write-Host "通过文件: $passedFiles" -ForegroundColor Green
Write-Host "失败文件: $failedFiles" -ForegroundColor Red

if ($Fix) {
    Write-Host "修复文件: $fixedFiles" -ForegroundColor Yellow
}

if ($failedFiles -eq 0) {
    Write-Host "✓ 所有包命名规则验证通过" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ 发现 $failedFiles 个包命名问题" -ForegroundColor Red
    
    if ($Fix) {
        if ($fixedFiles -eq $failedFiles) {
            Write-Host "✓ 所有问题已修复" -ForegroundColor Green
            exit 0
        } else {
            Write-Host "✗ 部分问题修复失败，请手动检查" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "提示: 使用 -Fix 参数自动修复问题" -ForegroundColor Yellow
        Write-Host "提示: 使用 -Verbose 查看详细错误信息" -ForegroundColor Yellow
        exit 1
    }
}