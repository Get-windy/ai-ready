@echo off
REM erp-purchase模块测试脚本
echo ========================================
echo erp-purchase采购询价/报价功能测试
echo ========================================
echo.

cd /d I:\AI-Ready\backend\erp\erp-purchase

echo 1. 清理项目...
call mvn clean -q
if %errorlevel% neq 0 (
    echo [错误] Maven清理失败
    exit /b 1
)

echo 2. 编译项目...
call mvn compile -q
if %errorlevel% neq 0 (
    echo [错误] 编译失败
    exit /b 1
)

echo 3. 运行单元测试...
call mvn test
if %errorlevel% neq 0 (
    echo [警告] 部分测试失败
)

echo 4. 生成测试报告...
call mvn surefire-report:report -q
if %errorlevel% neq 0 (
    echo [警告] 测试报告生成失败
)

echo.
echo ========================================
echo 测试完成
echo ========================================
echo.

REM 检查测试结果文件
if exist "target\surefire-reports" (
    echo 测试报告位置: target\surefire-reports\
    dir /b target\surefire-reports\*.txt
) else (
    echo [错误] 测试报告目录不存在
)

pause