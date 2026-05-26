@echo off
REM 测试环境网络连通性验证脚本（Windows版本）
REM 用法: validate-network.bat [all|gateway|dns|ports]

setlocal enabledelayedexpansion

REM 网络配置
set GATEWAYS=10.0.0.1 10.0.1.1 10.0.2.1 10.0.3.1
set DNS_SERVER=10.0.0.2
set DOMAINS=test.ai-ready.local test-api.ai-ready.local test-db.ai-ready.local test-redis.ai-ready.local

REM 颜色定义（Windows CMD不支持ANSI颜色，使用文本标记）
set INFO=[INFO]
set WARN=[WARN]
set ERROR=[ERROR]
set PASS=[PASS]
set FAIL=[FAIL]

REM 设置测试类型
set TEST_TYPE=%1
if "%TEST_TYPE%"=="" set TEST_TYPE=all

echo %INFO% 开始网络验证测试...
echo %INFO% 测试类型: %TEST_TYPE%

REM 测试网关连通性
if "%TEST_TYPE%"=="all" goto test_gateways
if "%TEST_TYPE%"=="gateway" goto test_gateways
goto skip_gateways

:test_gateways
echo %INFO% === 测试网关连通性 ===
set ALL_PASS=true
for %%g in (%GATEWAYS%) do (
    ping -n 2 -w 1000 %%g >nul 2>&1
    if !errorlevel! equ 0 (
        echo %PASS% 网关 %%g 可达
    ) else (
        echo %FAIL% 网关 %%g 不可达
        set ALL_PASS=false
    )
)
if "!ALL_PASS!"=="true" (
    echo %INFO% 所有网关测试通过
) else (
    echo %WARN% 部分网关测试失败
)
if "%TEST_TYPE%"=="gateway" goto end

:skip_gateways

REM 测试DNS解析
if "%TEST_TYPE%"=="all" goto test_dns
if "%TEST_TYPE%"=="dns" goto test_dns
goto skip_dns

:test_dns
echo %INFO% === 测试DNS解析 ===
set ALL_PASS=true
for %%d in (%DOMAINS%) do (
    nslookup %%d %DNS_SERVER% >nul 2>&1
    if !errorlevel! equ 0 (
        echo %PASS% 域名 %%d 解析成功
    ) else (
        echo %FAIL% 域名 %%d 解析失败
        set ALL_PASS=false
    )
)
if "!ALL_PASS!"=="true" (
    echo %INFO% 所有DNS解析测试通过
) else (
    echo %WARN% 部分DNS解析测试失败
)
if "%TEST_TYPE%"=="dns" goto end

:skip_dns

REM 测试服务端口（简化版，Windows需要telnet或PowerShell）
if "%TEST_TYPE%"=="all" goto test_ports
if "%TEST_TYPE%"=="ports" goto test_ports
goto skip_ports

:test_ports
echo %INFO% === 测试服务端口 ===
echo %WARN% Windows端口测试需要telnet或PowerShell，跳过详细测试...
echo %INFO% 请手动测试以下端口：
echo   - Web服务: 10.0.1.100:80
echo   - 用户服务: 10.0.2.10:8080
echo   - 订单服务: 10.0.2.20:8081
echo   - PostgreSQL: 10.0.3.10:5432
echo   - Redis: 10.0.3.20:6379
echo   - RabbitMQ: 10.0.3.30:5672
echo   - Grafana: 10.0.0.10:3000
echo   - Prometheus: 10.0.0.11:9090
if "%TEST_TYPE%"=="ports" goto end

:skip_ports

REM 生成测试报告
if "%TEST_TYPE%"=="all" goto generate_report
goto skip_report

:generate_report
echo %INFO% === 生成测试报告 ===
set REPORT_FILE=network-validation-report-%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%.md

(
    echo # 网络验证报告
    echo.
    echo ## 测试信息
    echo - **测试时间**: %date% %time%
    echo - **测试环境**: AI-Ready测试环境
    echo - **测试脚本版本**: 1.0 (Windows)
    echo.
    echo ## 测试结果
    echo.
    echo ### 网关连通性
) > %REPORT_FILE%

for %%g in (%GATEWAYS%) do (
    ping -n 2 -w 1000 %%g >nul 2>&1
    if !errorlevel! equ 0 (
        echo - ✅ %%g: 正常 >> %REPORT_FILE%
    ) else (
        echo - ❌ %%g: 异常 >> %REPORT_FILE%
    )
)

(
    echo.
    echo ### DNS解析
) >> %REPORT_FILE%

for %%d in (%DOMAINS%) do (
    nslookup %%d %DNS_SERVER% >nul 2>&1
    if !errorlevel! equ 0 (
        echo - ✅ %%d: 解析成功 >> %REPORT_FILE%
    ) else (
        echo - ❌ %%d: 解析失败 >> %REPORT_FILE%
    )
)

(
    echo.
    echo ### 服务端口
    echo - ℹ️ Windows端口测试需要手动验证
    echo.
    echo ## 总结
    echo - **测试完成时间**: %date% %time%
    echo - **建议**: 请根据测试结果修复异常项
    echo.
    echo ---
    echo **生成脚本**: validate-network.bat
) >> %REPORT_FILE%

echo %INFO% 测试报告已生成: %REPORT_FILE%

:skip_report

:end
echo %INFO% 网络验证测试完成
endlocal