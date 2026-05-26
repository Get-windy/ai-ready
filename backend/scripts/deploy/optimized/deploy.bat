@echo off
REM ============================================================
REM 一键部署脚本 - Windows版 - Sprint 27+1
REM 最后更新: 2026-04-27
REM ============================================================

setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0..
set COMPOSE_FILE=%PROJECT_DIR%\..\infra\docker\docker-compose.optimized.yml
set LOG_DIR=%PROJECT_DIR%\..\logs\deploy
set DEPLOY_TIME=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set DEPLOY_ENV=%1
set ACTION=%2

if "%DEPLOY_ENV%"=="" set DEPLOY_ENV=test
if "%ACTION%"=="" set ACTION=deploy

REM 颜色定义（通过换行符实现）
set RED=1
set GREEN=2
set YELLOW=3

REM 日志函数
call :log_info [INFO] %date% %time% - %~1
goto :eof

:log_info
echo [%date% %time%] %~1
goto :eof

:log_warn
echo [%date% %time%] [WARN] %~1
goto :eof

:log_error
echo [%date% %time%] [ERROR] %~1
goto :eof

REM 依赖检查
call :check_command docker
call :check_command docker-compose
if errorlevel 1 exit /b 1

call :log_info ========== Docker Compose 一键部署脚本 (Windows) ==========
call :log_info 环境: %DEPLOY_ENV%
call :log_info 动作: %ACTION%
call :log_info =================================================================

REM 动作处理
if "%ACTION%"=="deploy" (
    call :deploy
) else if "%ACTION%"=="up" (
    docker-compose -f "%COMPOSE_FILE%" up -d
    call :log_info 服务已启动
) else if "%ACTION%"=="down" (
    docker-compose -f "%COMPOSE_FILE%" down
    call :log_info 服务已停止
) else if "%ACTION%"=="restart" (
    docker-compose -f "%COMPOSE_FILE%" restart
    call :log_info 服务已重启
) else if "%ACTION%"=="status" (
    call :status
) else if "%ACTION%"=="backup" (
    call :backup_data
) else (
    call :usage
    exit /b 1
)

call :log_info =================================================================
call :log_info 操作完成!
exit /b 0

:check_command
where %1 >nul 2>&1
if errorlevel 1 (
    call :log_error %1 未安装，请先安装
    exit /b 1
)
exit /b 0

:usage
echo 使用方法: %~nx0 [environment] [action]
echo.
echo 环境参数: test (默认) | prod
echo 动作参数: deploy (默认) | up | down | restart | status | backup
echo.
echo 示例:
echo   %~nx0                      # 部署到测试环境
echo   %~nx0 prod                 # 部署到生产环境
echo   %~nx0 test restart         # 重启测试环境
exit /b 1

:deploy
call :log_info 开始部署到 %DEPLOY_ENV% 环境...
call :backup_data
call :log_info 停止旧服务...
docker-compose -f "%COMPOSE_FILE%" down 2>nul
call :log_info 拉取最新镜像...
docker-compose -f "%COMPOSE_FILE%" pull
call :log_info 启动新服务...
docker-compose -f "%COMPOSE_FILE%" up -d
call :log_info 等待服务就绪...
timeout /t 10 /nobreak >nul
call :log_info 健康检查...
call :check_health
call :generate_deploy_report
call :log_info 部署完成！
exit /b 0

:backup_data
set BACKUP_DIR=%PROJECT_DIR%\..\backups\%DEPLOY_ENV%\%DEPLOY_TIME%
mkdir "%BACKUP_DIR%" 2>nul
call :log_info 开始备份数据到: %BACKUP_DIR%

REM 备份PostgreSQL
docker-compose -f "%COMPOSE_FILE%" ps -q postgres >nul 2>&1
if errorlevel 1 (
    docker-compose -f "%COMPOSE_FILE%" exec -T postgres pg_dump -U appuser qizhilian > "%BACKUP_DIR%\postgres_%DEPLOY_TIME%.sql" 2>nul
    call :log_info PostgreSQL数据已备份
) else (
    call :log_warn PostgreSQL服务未运行
)

REM 备份Redis
docker-compose -f "%COMPOSE_FILE%" ps -q redis >nul 2>&1
if errorlevel 1 (
    docker-compose -f "%COMPOSE_FILE%" exec -T redis redis-cli BGSAVE >nul 2>&1
    docker-compose -f "%COMPOSE_FILE%" cp redis:/data/dump.rdb "%BACKUP_DIR%\redis_dump.rdb" 2>nul
    call :log_info Redis数据已备份
) else (
    call :log_warn Redis服务未运行
)

call :log_info 备份完成: %BACKUP_DIR%
exit /b 0

:check_health
set MAX_ATTEMPTS=10
set ATTEMPT=0

call :log_info 检查服务健康状态...

:health_check_loop
set /a ATTEMPT+=1
call :log_info 健康检查尝试 !ATTEMPT!/%MAX_ATTEMPTS%...

 Powershell -Command "if ((Invoke-WebRequest -Uri http://localhost:80/health -UseBasicParsing).StatusCode -eq 200) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    call :log_info 后端服务健康检查通过
    exit /b 0
)

timeout /t 5 /nobreak >nul

if !ATTEMPT! LSS %MAX_ATTEMPTS% (
    goto health_check_loop
)

call :log_error 健康检查超时
exit /b 1

:status
call :log_info 服务状态...
docker-compose -f "%COMPOSE_FILE%" ps

call :log_info 系统资源使用情况...
docker system df

call :log_info 磁盘空间...
wmic logicaldisk get size,freespace,caption