@echo off
REM 简单的网络连通性测试脚本
echo === 网络连通性测试 ===
echo.

echo 1. 测试网关连通性：
echo   测试 10.0.0.1...
ping -n 2 -w 1000 10.0.0.1 >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 网关 10.0.0.1 可达
) else (
    echo   ✗ 网关 10.0.0.1 不可达
)

echo   测试 10.0.1.1...
ping -n 2 -w 1000 10.0.1.1 >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 网关 10.0.1.1 可达
) else (
    echo   ✗ 网关 10.0.1.1 不可达
)

echo   测试 10.0.2.1...
ping -n 2 -w 1000 10.0.2.1 >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 网关 10.0.2.1 可达
) else (
    echo   ✗ 网关 10.0.2.1 不可达
)

echo   测试 10.0.3.1...
ping -n 2 -w 1000 10.0.3.1 >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 网关 10.0.3.1 可达
) else (
    echo   ✗ 网关 10.0.3.1 不可达
)

echo.
echo 2. 测试DNS解析：
echo   测试 google.com...
nslookup google.com >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ google.com 解析成功
) else (
    echo   ✗ google.com 解析失败
)

echo   测试 baidu.com...
nslookup baidu.com >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ baidu.com 解析成功
) else (
    echo   ✗ baidu.com 解析失败
)

echo   测试 localhost...
nslookup localhost >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ localhost 解析成功
) else (
    echo   ✗ localhost 解析失败
)

echo.
echo 3. 测试本地服务端口：
echo   测试本地端口 80 (HTTP)...
netstat -an | findstr ":80" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 端口 80 有服务监听
) else (
    echo   ✗ 端口 80 无服务监听
)

echo   测试本地端口 443 (HTTPS)...
netstat -an | findstr ":443" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ 端口 443 有服务监听
) else (
    echo   ✗ 端口 443 无服务监听
)

echo.
echo 4. 网络信息：
echo   IP配置：
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr "IPv4"') do (
    set ip=%%a
    echo   - IPv4: !ip!
)

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr "默认网关"') do (
    set gateway=%%a
    echo   - 默认网关: !gateway!
)

echo.
echo === 网络测试完成 ===
pause