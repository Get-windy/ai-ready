@echo off
REM 测试环境服务验证脚本 (Windows批处理版本)
REM 用途：快速检查测试环境服务可用性
REM 执行：verify-test-environment.bat

echo ==========================================
echo     测试环境服务可用性验证脚本
echo ==========================================
echo 启动时间: %date% %time%
echo.

REM 定义服务检查计数器
set /a TOTAL_SERVICES=0
set /a AVAILABLE_SERVICES=0

REM 检查MySQL服务
echo 检查数据库服务...
mysql -h localhost -P 3306 -u root -p"password123" -e "SELECT 1" >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查PostgreSQL服务
echo 检查PostgreSQL服务...
pg_isready -h localhost -p 5432 >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查Redis服务
echo 检查Redis服务...
redis-cli -h localhost -p 6379 ping >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查API网关
echo 检查API网关...
curl -f -s http://localhost:8080/actuator/health >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查用户服务
echo 检查用户服务...
curl -f -s http://localhost:8081/actuator/health >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查订单服务
echo 检查订单服务...
curl -f -s http://localhost:8082/actuator/health >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查库存服务
echo 检查库存服务...
curl -f -s http://localhost:8083/actuator/health >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查Prometheus
echo 检查Prometheus...
curl -f -s http://localhost:9090/-/ready >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

REM 检查Grafana
echo 检查Grafana...
curl -f -s http://localhost:3000/api/health >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 可用
    set /a AVAILABLE_SERVICES+=1
) else (
    echo ❌ 不可用
)
set /a TOTAL_SERVICES+=1

echo.
echo ==========================================
echo 服务可用性统计:
echo 总服务数: %TOTAL_SERVICES%
echo 可用服务数: %AVAILABLE_SERVICES%
set /a AVAILABILITY_PERCENTAGE=AVAILABLE_SERVICES*100/TOTAL_SERVICES 2>nul
echo 可用率: %AVAILABILITY_PERCENTAGE%%%
echo.

if %AVAILABLE_SERVICES% equ %TOTAL_SERVICES% (
    echo 🎉 所有测试环境服务都已就绪！可以开始性能测试。
    echo 预计开始时间: %time:~0,5%
    echo.
    echo 建议执行以下步骤:
    echo 1. 生成测试数据: setup-test-data.sh
    echo 2. 启动性能监控: monitor-performance.sh start
    echo 3. 执行登录测试: jmeter -n -t scripts\Login_Test.jmx -l results\login.jtl
) else (
    echo ⚠️ 部分服务不可用，性能测试无法开始。
    echo.
    echo 服务启动状态:
    echo 可用服务: %AVAILABLE_SERVICES%/%TOTAL_SERVICES%
    echo.
    echo 请通知 devops-engineer 启动缺失的服务。
)

echo ==========================================
echo 验证完成时间: %date% %time%
echo ==========================================

pause