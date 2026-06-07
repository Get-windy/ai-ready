@echo off
chcp 65001 >nul
title AI-Ready Dev Server
echo ==========================================
echo   AI-Ready 开发服务器
echo ==========================================
echo.
echo 请选择操作:
echo  1. 启动后端 (core-api)
echo  2. 启动所有模块
echo  3. 仅清理 Java 进程
echo  4. 退出
echo.
set /p choice="输入数字 (1-4): "

if "%choice%"=="1" goto start_api
if "%choice%"=="2" goto start_all
if "%choice%"=="3" goto kill_only
if "%choice%"=="4" exit /b

:start_api
echo.
echo 启动后端核心模块...
powershell -ExecutionPolicy Bypass -File "%~dp0dev-server.ps1" -Module "core/api/core-api"
goto end

:start_all
echo.
echo 启动所有模块...
powershell -ExecutionPolicy Bypass -File "%~dp0dev-server.ps1" -All
goto end

:kill_only
echo.
echo 清理 Java 进程...
powershell -ExecutionPolicy Bypass -File "%~dp0dev-server.ps1" -KillOnly
goto end

:end
pause
