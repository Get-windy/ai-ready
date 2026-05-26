@echo off
echo ========================================
echo AI-Ready 测试应用快速启动 (Windows)
echo ========================================

REM 检查JAR文件是否存在
if not exist "target\ai-ready-test-app-1.0.0.jar" (
    echo JAR文件不存在，正在构建...
    call build.bat
    if %ERRORLEVEL% neq 0 (
        echo 构建失败，无法启动应用
        pause
        exit /b 1
    )
)

echo 启动应用...
echo 应用将在 http://localhost:8080 启动
echo 按 Ctrl+C 停止应用
echo.

REM 启动应用
java -jar target\ai-ready-test-app-1.0.0.jar