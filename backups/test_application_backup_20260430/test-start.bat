@echo off
echo 测试AI-Ready测试应用启动...
echo.

REM 检查JAR文件
if not exist "target\ai-ready-test-app-1.0.0.jar" (
    echo 错误: JAR文件不存在
    echo 正在构建应用...
    call mvn clean package -DskipTests
    if %ERRORLEVEL% neq 0 (
        echo 构建失败
        pause
        exit /b 1
    )
)

echo JAR文件大小: 
for /f %%i in ('dir /-c "target\ai-ready-test-app-1.0.0.jar" ^| find "ai-ready-test-app-1.0.0.jar"') do (
    echo    %%i bytes
)

echo.
echo 启动应用测试（5秒后停止）...
echo 应用将在 http://localhost:8080 启动
echo.

REM 在后台启动应用
start "AI-Ready Test App" cmd /c "java -jar target\ai-ready-test-app-1.0.0.jar"

REM 等待应用启动
timeout /t 5 /nobreak >nul

echo.
echo 测试应用健康检查...
curl -s http://localhost:8080/actuator/health
echo.
echo.

echo 测试API端点...
curl -s http://localhost:8080/
echo.
echo.

curl -s http://localhost:8080/api/v1/users
echo.
echo.

echo 按任意键停止应用...
pause >nul

REM 停止应用
taskkill /F /FI "WINDOWTITLE eq AI-Ready Test App" >nul 2>nul

echo 应用已停止
echo 测试完成！