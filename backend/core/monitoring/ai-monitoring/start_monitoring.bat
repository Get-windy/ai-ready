@echo off
REM AI模型质量监控系统启动脚本
REM 适用于Windows环境

echo ========================================
echo    AI模型质量监控系统 - 启动脚本
echo ========================================
echo.

REM 检查Docker和Docker Compose
echo [1/5] 检查Docker和Docker Compose...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: Docker未安装或未启动
    echo 请先安装Docker Desktop: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 警告: Docker Compose未安装，尝试使用Docker compose...
    docker compose version >nul 2>&1
    if %errorlevel% neq 0 (
        echo 错误: Docker Compose未安装
        echo 请安装Docker Compose或更新Docker Desktop
        pause
        exit /b 1
    )
    set USE_DOCKER_COMPOSE=0
) else (
    set USE_DOCKER_COMPOSE=1
)

REM 检查端口占用
echo [2/5] 检查端口占用情况...
for %%p in (8000 9090 9093 3000) do (
    netstat -ano | findstr ":%%p " >nul
    if %errorlevel% equ 0 (
        echo 警告: 端口 %%p 已被占用
    )
)

REM 创建必要目录
echo [3/5] 创建必要目录...
if not exist logs mkdir logs
if not exist evaluations mkdir evaluations
if not exist config mkdir config

REM 检查配置文件
echo [4/5] 检查配置文件...
if not exist config\prometheus.yml (
    echo 错误: 缺少配置文件 config\prometheus.yml
    pause
    exit /b 1
)

if not exist config\alerts.yml (
    echo 错误: 缺少配置文件 config\alerts.yml
    pause
    exit /b 1
)

if not exist config\alertmanager.yml (
    echo 错误: 缺少配置文件 config\alertmanager.yml
    pause
    exit /b 1
)

REM 启动服务
echo [5/5] 启动监控服务...
echo.

if %USE_DOCKER_COMPOSE% equ 1 (
    docker-compose up -d
) else (
    docker compose up -d
)

if %errorlevel% neq 0 (
    echo 错误: 启动服务失败
    echo 请检查Docker日志: docker-compose logs
    pause
    exit /b 1
)

echo.
echo ========================================
echo          服务启动成功！
echo ========================================
echo.
echo 监控服务已启动，访问地址:
echo.
echo [1] 监控API服务: http://localhost:8000
echo     健康检查: http://localhost:8000/health
echo     监控数据: http://localhost:8000/metrics/json
echo.
echo [2] Prometheus: http://localhost:9090
echo     告警规则: http://localhost:9090/alerts
echo.
echo [3] AlertManager: http://localhost:9093
echo.
echo [4] Grafana仪表盘: http://localhost:3000
echo     用户名: admin
echo     密码: admin123
echo.
echo [5] 数据生成器测试:
echo     python scripts\data_generator.py --check-status
echo     python scripts\data_generator.py --interval 30
echo.
echo ========================================
echo 常用命令:
echo  查看服务状态: docker-compose ps
echo  查看日志: docker-compose logs -f
echo  停止服务: docker-compose down
echo  重启服务: docker-compose restart
echo ========================================
echo.
echo 按任意键打开浏览器查看监控面板...
pause >nul

REM 打开浏览器
start http://localhost:3000
start http://localhost:8000/health
start http://localhost:9090/alerts

echo.
echo 监控系统正在运行，按Ctrl+C停止所有服务...
echo 要停止服务，请运行: docker-compose down
echo.

REM 保持脚本运行，显示服务状态
:status_loop
echo [%time%] 服务状态:
if %USE_DOCKER_COMPOSE% equ 1 (
    docker-compose ps
) else (
    docker compose ps
)
echo.
echo 按Ctrl+C停止监控...
timeout /t 30 /nobreak >nul
goto status_loop