#!/usr/bin/env pwsh
# ============================================================
# AI-Ready 开发服务器启动脚本 (PowerShell)
# 特性: 主进程退出时自动清理所有 Java 子进程
# ============================================================
# 用法:
#   .\scripts\dev-server.ps1              # 启动后端
#   .\scripts\dev-server.ps1 -Module user  # 启动指定模块
#   .\scripts\dev-server.ps1 -All          # 启动所有模块
#   .\scripts\dev-server.ps1 -KillOnly     # 仅清理进程
# ============================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

param(
    [string]$Module = "core/api/core-api",
    [switch]$All = $false,
    [switch]$SkipBuild = $false,
    [switch]$KillOnly = $false
)

$BackendDir = "I:\AI-Ready\backend"
$ModulePath = $Module

# ── 记录启动的 PID ──
$StartedPids = @()

# ── 清理函数：杀掉所有本项目启动的 Java 进程 ──
function Cleanup-JavaProcesses {
    Write-Host ""
    Write-Host "🧹 清理 Java 进程中..." -ForegroundColor Yellow

    $javaProcesses = Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object {
        $_.Id -in $StartedPids -or $_.Id -eq 0
    }

    $count = 0
    foreach ($pid in $StartedPids) {
        try {
            $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
            if ($proc) {
                $proc.Kill()
                Write-Host "   ✅ 已终止进程 PID: $pid" -ForegroundColor Green
                $count++
            }
        } catch {
            # 进程可能已经结束了
        }
    }

    # 额外清理：杀掉 core-api 相关的进程
    Get-Process java,javaw -ErrorAction SilentlyContinue | ForEach-Object {
        try {
            $cmdLine = (Get-WmiObject Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine
            if ($cmdLine -match "core-api|ai-ready|spring-boot") {
                $_.Kill()
                Write-Host "   ✅ 已清理残留进程 PID: $($_.Id)" -ForegroundColor Green
                $count++
            }
        } catch {}
    }

    if ($count -eq 0) {
        Write-Host "   ℹ️  没有需要清理的 Java 进程" -ForegroundColor Gray
    } else {
        Write-Host "   ✅ 共清理 $count 个进程" -ForegroundColor Green
    }
}

# ── 仅清理模式 ──
if ($KillOnly) {
    Cleanup-JavaProcesses
    exit 0
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  AI-Ready 开发服务器启动脚本" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# ── 第一步：清理残留进程 ──
Write-Host "[Step 1] 清理残留 Java 进程..." -ForegroundColor Yellow
Cleanup-JavaProcesses
Write-Host ""

# ── 第二步：构建 ──
if (-not $SkipBuild) {
    Write-Host "[Step 2] 编译项目..." -ForegroundColor Yellow
    Push-Location $BackendDir

    $buildJob = Start-Process -FilePath "mvn" -ArgumentList "clean compile -DskipTests -q" -NoNewWindow -Wait -PassThru

    if ($buildJob.ExitCode -ne 0) {
        Write-Host "   ❌ 编译失败，退出码: $($buildJob.ExitCode)" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "   ✅ 编译成功" -ForegroundColor Green
    Pop-Location
    Write-Host ""
}

# ── 第三步：启动后端 ──
Write-Host "[Step 3] 启动后端服务..." -ForegroundColor Yellow

Push-Location $BackendDir

if ($All) {
    # 启动所有模块 - 使用 Maven reactor
    Write-Host "   启动所有模块..." -ForegroundColor Cyan
    $proc = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -NoNewWindow
    $StartedPids += $proc.Id
} else {
    # 启动指定模块
    Write-Host "   启动模块: $ModulePath" -ForegroundColor Cyan
    $proc = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -pl $ModulePath" -PassThru -NoNewWindow
    $StartedPids += $proc.Id
}

Write-Host "   🟢 后端服务启动中 (PID: $($proc.Id))..." -ForegroundColor Green
Write-Host "   按 Ctrl+C 停止服务（将自动清理所有进程）" -ForegroundColor Gray
Write-Host ""

Pop-Location

# ── 注册清理钩子 ──
# PowerShell 7 以上支持 Register-EngineEvent
try {
    $null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action {
        param($event, $subscriber)
        # 在进程退出时也要清理
    } -SupportEvent -ErrorAction SilentlyContinue
} catch {}

# ── 等待进程结束 ──
try {
    $proc.WaitForExit()
} catch {
    # 进程被终止或 Ctrl+C
    Write-Host ""
    Write-Host "⏹️  服务已停止，正在清理..." -ForegroundColor Yellow
}

# ── 最终清理 ──
Cleanup-JavaProcesses

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  服务已停止" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
