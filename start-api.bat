@echo off
chcp 65001 >nul
echo ========================================
echo AI-Ready 启动脚本
echo ========================================
echo.

echo [1/3] 检查 Java 环境...
java -version 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found
    pause
    exit /b 1
)
echo [OK] Java ready
echo.

echo [2/3] Check config file...
if exist "application-local.yml" (
    echo [OK] application-local.yml found
) else (
    echo [WARN] application-local.yml not found
)
echo.

echo [3/3] Starting AI-Ready API service...
echo Service URL: http://localhost:8080
echo.

cd core-api\target
java -jar core-api-1.0.0-SNAPSHOT.jar

pause
