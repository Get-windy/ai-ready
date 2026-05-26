@echo off
echo ========================================
echo AI-Ready 测试应用构建脚本 (Windows)
echo ========================================

REM 检查Maven是否安装
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo 错误: Maven未安装，请先安装Maven
    exit /b 1
)

REM 清理旧构建
echo 1. 清理旧构建文件...
if exist target rmdir /s /q target

REM 构建应用
echo 2. 构建应用...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo 错误: Maven构建失败
    exit /b 1
)

REM 检查JAR文件
if not exist "target\ai-ready-test-app-1.0.0.jar" (
    echo 错误: JAR文件未生成
    exit /b 1
)

echo 3. 构建成功！
for /f %%i in ('dir /-c "target\ai-ready-test-app-1.0.0.jar" ^| find "ai-ready-test-app-1.0.0.jar"') do (
    set size=%%i
)
echo    JAR文件: target\ai-ready-test-app-1.0.0.jar
echo    大小: %size% bytes

REM 运行测试
echo 4. 运行应用测试...
java -jar target\ai-ready-test-app-1.0.0.jar --version

echo ========================================
echo 构建完成！
echo 你可以使用以下命令运行应用：
echo   java -jar target\ai-ready-test-app-1.0.0.jar
echo 或者使用Docker：
echo   docker-compose up -d
echo ========================================
pause