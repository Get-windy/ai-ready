#!/bin/bash

# Docker测试环境Windows管理脚本 (PowerShell)
# 用于启动、停止、清理和管理测试容器

Write-Host "[INFO] Docker测试环境管理脚本 (Windows)" -ForegroundColor Blue
Write-Host ""

# 脚本目录
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptDir "docker-compose.test-env.yml"

# 检查Docker是否安装
function Check-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Docker未安装，请先安装Docker Desktop" -ForegroundColor Red
        exit 1
    }
    
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Docker守护进程未运行，请启动Docker Desktop" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[SUCCESS] Docker已安装并运行" -ForegroundColor Green
}

# 检查Docker Compose是否安装
function Check-DockerCompose {
    $composeVersion = docker compose version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Docker Compose未安装，请先安装Docker Desktop (内置Compose)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[SUCCESS] Docker Compose已安装: $composeVersion" -ForegroundColor Green
}

# 启动测试容器
function Start-Containers {
    Write-Host "[INFO] 启动Docker测试环境..." -ForegroundColor Blue
    
    if (-not (Test-Path $composeFile)) {
        Write-Host "[ERROR] 未找到docker-compose文件: $composeFile" -ForegroundColor Red
        exit 1
    }
    
    # 启动所有服务
    docker-compose -f $composeFile up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 启动容器失败" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[SUCCESS] 所有测试容器已启动" -ForegroundColor Green
    
    # 检查容器健康状态
    Write-Host "[INFO] 等待容器启动并检查健康状态..." -ForegroundColor Blue
    Start-Sleep -Seconds 10
    
    # 检查每个服务的健康状态
    $services = @("postgresql-test", "redis-test", "rabbitmq-test", "elasticsearch-test", "minio-test")
    foreach ($service in $services) {
        $status = docker ps --filter "name=$service" --format "{{.Status}}"
        if ($status) {
            Write-Host "[INFO] $service: $status" -ForegroundColor Blue
        } else {
            Write-Host "[INFO] $service: 未运行或不存在" -ForegroundColor Yellow
        }
    }
    
    # 显示连接信息
    Show-ConnectionInfo
}

# 停止测试容器
function Stop-Containers {
    Write-Host "[INFO] 停止Docker测试环境..." -ForegroundColor Blue
    docker-compose -f $composeFile stop
    Write-Host "[SUCCESS] 测试容器已停止" -ForegroundColor Green
}

# 重启测试容器
function Restart-Containers {
    Write-Host "[INFO] 重启Docker测试环境..." -ForegroundColor Blue
    docker-compose -f $composeFile restart
    Write-Host "[SUCCESS] 测试容器已重启" -ForegroundColor Green
}

# 停止并删除容器、网络
function Down-Containers {
    Write-Host "[INFO] 停止并删除Docker测试环境..." -ForegroundColor Blue
    docker-compose -f $composeFile down
    Write-Host "[SUCCESS] 测试容器和网络已删除" -ForegroundColor Green
}

# 停止、删除容器、网络和卷（完全清理）
function Clean-All {
    Write-Host "[WARNING] 将删除所有测试容器、网络和卷数据！" -ForegroundColor Yellow
    $confirm = Read-Host "确认操作? (y/N)"
    
    if ($confirm -match "^[Yy]$") {
        Write-Host "[INFO] 完全清理Docker测试环境..." -ForegroundColor Blue
        docker-compose -f $composeFile down -v
        Write-Host "[SUCCESS] 测试环境已完全清理" -ForegroundColor Green
    } else {
        Write-Host "[INFO] 操作已取消" -ForegroundColor Blue
    }
}

# 显示容器状态
function Show-Status {
    Write-Host "[INFO] Docker测试环境状态:" -ForegroundColor Blue
    docker-compose -f $composeFile ps
}

# 查看日志
function Show-Logs {
    param(
        [Parameter(Mandatory=$true)]
        [string]$service
    )
    
    Write-Host "[INFO] 查看服务日志: $service" -ForegroundColor Blue
    docker-compose -f $composeFile logs -f $service
}

# 显示连接信息
function Show-ConnectionInfo {
    Write-Host ""
    Write-Host "[INFO] 测试环境连接信息:" -ForegroundColor Blue
    Write-Host "-----------------------------------"
    Write-Host "PostgreSQL:  localhost:5433"
    Write-Host "  用户: test_user"
    Write-Host "  密码: test_password"
    Write-Host "  数据库: ai_ready_test"
    Write-Host ""
    Write-Host "Redis:       localhost:6380"
    Write-Host "  端口: 6380"
    Write-Host ""
    Write-Host "RabbitMQ:    localhost:5673 (AMQP)"
    Write-Host "             localhost:15673 (Management UI)"
    Write-Host "  用户: test_user"
    Write-Host "  密码: test_password"
    Write-Host ""
    Write-Host "Elasticsearch: localhost:9201"
    Write-Host "  端口: 9201"
    Write-Host "  注意: xpack.security已禁用"
    Write-Host ""
    Write-Host "MinIO:       localhost:9001 (API)"
    Write-Host "             localhost:9002 (Console)"
    Write-Host "  用户: test_user"
    Write-Host "  密码: test_password123"
    Write-Host "-----------------------------------"
}

# 显示帮助信息
function Show-Help {
    Write-Host "Docker测试环境管理脚本 (Windows)"
    Write-Host ""
    Write-Host "用法: .\docker-manage.ps1 [命令]"
    Write-Host ""
    Write-Host "命令:"
    Write-Host "  start        启动所有测试容器"
    Write-Host "  stop         停止所有测试容器"
    Write-Host "  restart      重启所有测试容器"
    Write-Host "  down         停止并删除容器、网络"
    Write-Host "  clean        停止并删除容器、网络和卷"
    Write-Host "  status       显示容器状态"
    Write-Host "  logs <service>  查看日志 (./docker-manage.ps1 logs postgresql-test)"
    Write-Host "  ps           列出容器"
    Write-Host "  help         显示帮助信息"
    Write-Host ""
    Write-Host "例子:"
    Write-Host "  .\docker-manage.ps1 start                # 启动测试环境"
    Write-Host "  .\docker-manage.ps1 logs postgresql-test # 查看PostgreSQL日志"
    Write-Host "  .\docker-manage.ps1 clean                # 清理所有测试环境数据"
}

# 主函数
function Main {
    $command = $args[0]
    
    Check-Docker
    Check-DockerCompose
    
    switch ($command) {
        "start" { Start-Containers }
        "stop" { Stop-Containers }
        "restart" { Restart-Containers }
        "down" { Down-Containers }
        "clean" { Clean-All }
        "status" { Show-Status }
        "logs" { Show-Logs $args[1] }
        "ps" { docker-compose -f $composeFile ps }
        "help", "--help", "-h" { Show-Help }
        default {
            Write-Host "[ERROR] 未知命令: $command" -ForegroundColor Red
            Show-Help
            exit 1
        }
    }
}

# 执行主函数
Main $args
