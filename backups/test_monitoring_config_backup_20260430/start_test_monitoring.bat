@echo off
echo ========================================
echo AI-Ready 测试监控告警系统启动脚本
echo ========================================
echo.

REM 检查Docker是否运行
echo 检查Docker服务状态...
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker服务未运行，请启动Docker Desktop
    pause
    exit /b 1
)
echo [成功] Docker服务正常运行

REM 检查Docker Compose是否可用
echo 检查Docker Compose...
docker-compose version >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Docker Compose未找到，尝试使用docker compose...
    docker compose version >nul 2>&1
    if %errorlevel% neq 0 (
        echo [错误] Docker Compose不可用
        pause
        exit /b 1
    )
    set DOCKER_COMPOSE_CMD=docker compose
) else (
    set DOCKER_COMPOSE_CMD=docker-compose
)
echo [成功] Docker Compose可用

REM 设置环境变量
echo 设置环境变量...
set MONITORING_DIR=%~dp0
cd /d "%MONITORING_DIR%"

REM 检查必要的配置文件
echo 检查配置文件...
if not exist "prometheus.yml" (
    echo [错误] 缺少 prometheus.yml 配置文件
    pause
    exit /b 1
)

if not exist "alerts.yml" (
    echo [错误] 缺少 alerts.yml 告警规则文件
    pause
    exit /b 1
)

if not exist "docker-compose.yml" (
    echo [错误] 缺少 docker-compose.yml 配置文件
    pause
    exit /b 1
)

echo [成功] 所有配置文件就绪

REM 创建必要的目录
echo 创建数据目录...
mkdir data 2>nul
mkdir data\prometheus 2>nul
mkdir data\alertmanager 2>nul
mkdir data\grafana 2>nul
mkdir logs 2>nul
mkdir logs\test-monitor 2>nul
mkdir logs\test-data 2>nul
mkdir logs\test-report 2>nul
mkdir logs\alert-notification 2>nul
mkdir config 2>nul
mkdir config\prometheus 2>nul
mkdir config\grafana 2>nul
mkdir config\grafana\dashboards 2>nul
mkdir config\grafana\datasources 2>nul
mkdir config\alertmanager 2>nul
mkdir config\nginx 2>nul
mkdir config\nginx\conf.d 2>nul
mkdir config\notifications 2>nul

REM 创建Grafana数据源配置文件
echo 创建Grafana数据源配置...
echo {
echo   "apiVersion": 1,
echo   "datasources": [
echo     {
echo       "name": "Test-Prometheus",
echo       "type": "prometheus",
echo       "access": "proxy",
echo       "url": "http://prometheus:9091",
echo       "isDefault": true,
echo       "editable": true
echo     }
echo   ]
echo } > config\grafana\datasources\prometheus.yml

REM 创建默认Grafana仪表盘
echo 创建Grafana仪表盘配置...
echo {
echo   "apiVersion": 1,
echo   "providers": [
echo     {
echo       "name": "default",
echo       "orgId": 1,
echo       "folder": "",
echo       "type": "file",
echo       "disableDeletion": false,
echo       "updateIntervalSeconds": 10,
echo       "allowUiUpdates": true,
echo       "options": {
echo         "path": "/etc/grafana/provisioning/dashboards"
echo       }
echo     }
echo   ]
echo } > config\grafana\dashboards\default.yml

REM 创建AlertManager配置
echo 创建AlertManager配置...
echo global:
echo   smtp_smarthost: 'smtp.gmail.com:587'
echo   smtp_from: 'test-monitoring@ai-ready.com'
echo   smtp_auth_username: 'your-email@gmail.com'
echo   smtp_auth_password: 'your-password'
echo   slack_api_url: 'https://hooks.slack.com/services/your/slack/webhook'
echo 
echo route:
echo   group_by: ['alertname', 'severity', 'environment']
echo   group_wait: 30s
echo   group_interval: 5m
echo   repeat_interval: 1h
echo   receiver: 'default-receiver'
echo 
echo receivers:
echo - name: 'default-receiver'
echo   email_configs:
echo   - to: 'team@ai-ready.com'
echo     send_resolved: true
echo   slack_configs:
echo   - channel: '#test-alerts'
echo     send_resolved: true
echo     title: '{{ template "slack.default.title" . }}'
echo     text: '{{ template "slack.default.text" . }}'
echo   webhook_configs:
echo   - url: 'http://alert-notification-service:8080/webhook'
echo     send_resolved: true
echo 
echo inhibit_rules:
echo   - source_match:
echo       severity: 'critical'
echo     target_match:
echo       severity: 'warning'
echo     equal: ['alertname', 'environment']
echo > config\alertmanager\alertmanager.yml

REM 启动测试监控系统
echo 启动测试监控系统...
echo.
echo 正在启动以下服务:
echo 1. 测试监控服务 (端口: 8100)
echo 2. Prometheus (端口: 9091)
echo 3. AlertManager (端口: 9094)
echo 4. Grafana (端口: 3100)
echo 5. 节点导出器 (端口: 9100)
echo 6. cAdvisor (端口: 8080)
echo 7. 测试数据生成器
echo 8. 测试报告服务 (端口: 8200)
echo 9. 告警通知服务
echo.

%DOCKER_COMPOSE_CMD% up -d

if %errorlevel% neq 0 (
    echo [错误] 启动测试监控系统失败
    pause
    exit /b 1
)

echo [成功] 测试监控系统启动完成
echo.

REM 等待服务启动
echo 等待服务启动（30秒）...
timeout /t 30 /nobreak >nul

REM 检查服务状态
echo 检查服务运行状态...
echo.

%DOCKER_COMPOSE_CMD% ps

echo.
echo ========================================
echo 测试监控系统访问地址
echo ========================================
echo 测试监控API:     http://localhost:8100
echo Prometheus:       http://localhost:9091
echo AlertManager:     http://localhost:9094
echo Grafana:          http://localhost:3100
echo 测试报告服务:     http://localhost:8200
echo.
echo Grafana默认登录:
echo 用户名: admin
echo 密码: admin123
echo ========================================
echo.

REM 测试服务连通性
echo 测试服务连通性...
echo.

echo 1. 测试监控服务健康检查...
curl -f http://localhost:8100/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [成功] 测试监控服务正常
) else (
    echo   [警告] 测试监控服务不可达
)

echo 2. Prometheus健康检查...
curl -f http://localhost:9091/-/healthy >nul 2>&1
if %errorlevel% equ 0 (
    echo   [成功] Prometheus正常
) else (
    echo   [警告] Prometheus不可达
)

echo 3. Grafana健康检查...
curl -f http://localhost:3100/api/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [成功] Grafana正常
) else (
    echo   [警告] Grafana不可达
)

echo.
echo ========================================
echo 常用命令
echo ========================================
echo 查看服务日志:      %DOCKER_COMPOSE_CMD% logs -f
echo 停止服务:          %DOCKER_COMPOSE_CMD% down
echo 重启服务:          %DOCKER_COMPOSE_CMD% restart
echo 查看服务状态:      %DOCKER_COMPOSE_CMD% ps
echo 进入容器:          docker exec -it ai-ready-test-monitor bash
echo 生成测试数据:      docker exec test-data-generator python generate_test_data.py
echo 查看测试报告:      http://localhost:8200/report
echo ========================================
echo.

REM 打开浏览器
echo 是否打开浏览器访问监控仪表盘？(Y/N)
set /p open_browser=
if /i "%open_browser%"=="Y" (
    start http://localhost:3100
    start http://localhost:8100
)

echo.
echo 测试监控系统已成功启动！
echo 按任意键退出...
pause >nul