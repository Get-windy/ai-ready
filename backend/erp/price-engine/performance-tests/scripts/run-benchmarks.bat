@echo off
REM 价格策略模块性能基准测试执行脚本（Windows版本）
REM 自动运行性能基准测试

setlocal enabledelayedexpansion

REM 配置参数
set BASE_DIR=%~dp0..
set JMETER_DIR=%BASE_DIR%\jmeter
set JMH_DIR=%BASE_DIR%\jmh
set REPORTS_DIR=%BASE_DIR%\reports
set SCRIPTS_DIR=%BASE_DIR%\scripts
set DATA_DIR=%BASE_DIR%\test-data

REM 创建目录
if not exist "%REPORTS_DIR%" mkdir "%REPORTS_DIR%"
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"

echo.
echo ==============================================
echo    ERP价格策略模块性能基准测试
echo ==============================================
echo.

REM 检查Java
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java未安装，请先安装Java 11+
    goto :end
) else (
    echo [INFO] Java已安装
)

REM 检查Maven
where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven未安装，JMH基准测试需要Maven
    goto :end
) else (
    echo [INFO] Maven已安装
)

REM 检查JMeter
where jmeter >nul 2>nul
if errorlevel 1 (
    echo [WARNING] JMeter未安装，将跳过API负载测试
    set SKIP_JMETER=true
) else (
    echo [INFO] JMeter已安装
    set SKIP_JMETER=false
)

REM 检查Python
where python >nul 2>nul
if errorlevel 1 (
    where python3 >nul 2>nul
    if errorlevel 1 (
        echo [WARNING] Python未安装，将跳过数据生成
        set SKIP_PYTHON=true
    ) else (
        set SKIP_PYTHON=false
    )
) else (
    set SKIP_PYTHON=false
)

echo.
echo [INFO] 准备测试环境...
echo.

REM 生成测试数据
if "%SKIP_PYTHON%"=="false" (
    echo [INFO] 生成测试数据...
    cd /d "%SCRIPTS_DIR%"
    python setup-test-data.py --size 1000 --type strategy --output "%DATA_DIR%\strategies.json"
    python setup-test-data.py --size 500 --type request --output "%DATA_DIR%\requests.json"
    echo [SUCCESS] 测试数据生成完成
) else (
    echo [WARNING] 跳过数据生成
)

echo.
echo [INFO] 运行性能基准测试...
echo.

REM 运行JMeter测试
if "%SKIP_JMETER%"=="false" (
    echo [INFO] 开始JMeter API负载测试...
    
    set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
    set TIMESTAMP=%TIMESTAMP: =0%
    
    if exist "%JMETER_DIR%\api-load-test.jmx" (
        jmeter -n -t "%JMETER_DIR%\api-load-test.jmx" -l "%REPORTS_DIR%\jmeter_results_%TIMESTAMP%.jtl" -e -o "%REPORTS_DIR%\jmeter_report_%TIMESTAMP%"
        
        if errorlevel 0 (
            echo [SUCCESS] JMeter测试完成
            echo 报告位置: %REPORTS_DIR%\jmeter_report_%TIMESTAMP%
        ) else (
            echo [ERROR] JMeter测试失败
        )
    ) else (
        echo [ERROR] JMeter测试文件不存在: %JMETER_DIR%\api-load-test.jmx
    )
) else (
    echo [WARNING] 跳过JMeter测试
)

REM 运行JMH基准测试
echo.
echo [INFO] 开始JMH微基准测试...
cd /d "%BASE_DIR%"

REM 检查是否是Maven项目
if not exist "pom.xml" (
    echo [ERROR] 当前目录不是Maven项目
    goto :end
)

REM 编译项目
echo [INFO] 编译项目...
call mvn clean compile -q >nul 2>nul

if errorlevel 0 (
    echo [SUCCESS] 项目编译成功
) else (
    echo [ERROR] 项目编译失败
    goto :end
)

REM 运行基准测试
echo [INFO] 运行规则引擎基准测试...
call mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.RuleEngineBenchmark" -Dexec.classpathScope="compile" -q

echo [INFO] 运行优化算法基准测试...
call mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.OptimizationBenchmark" -Dexec.classpathScope="compile" -q

echo [INFO] 运行缓存基准测试...
call mvn exec:java -Dexec.mainClass="cn.aiedge.erp.price.engine.performance.CacheBenchmark" -Dexec.classpathScope="compile" -q

echo.
echo [SUCCESS] JMH基准测试完成

REM 生成报告
echo.
echo [INFO] 生成性能测试综合报告...

set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set REPORT_FILE=%REPORTS_DIR%\performance_report_%TIMESTAMP%.md

(
echo # ERP价格策略模块性能基准测试报告
echo.
echo ## 测试概述
echo - 测试时间: %date% %time%
echo - 测试环境: Windows
echo - Java版本: 
echo.
echo ## 测试目标
echo 建立价格策略模块的性能基准，为后续性能优化提供量化依据。
echo.
echo ## 测试范围
echo 1. API层性能测试
echo 2. 规则引擎性能测试  
echo 3. 优化算法性能测试
echo 4. 缓存层性能测试
echo.
echo ## 测试结果摘要
echo.
echo | 测试类型 | 状态 | 结果文件 |
echo |---------|------|----------|
) > "%REPORT_FILE%"

if "%SKIP_JMETER%"=="false" (
    echo | JMeter API测试 | 已完成 | %REPORTS_DIR%\jmeter_report_%TIMESTAMP% | >> "%REPORT_FILE%"
) else (
    echo | JMeter API测试 | 已跳过 | 需要安装JMeter | >> "%REPORT_FILE%"
)

echo | JMH规则引擎测试 | 已完成 | 控制台输出 | >> "%REPORT_FILE%"
echo | JMH优化算法测试 | 已完成 | 控制台输出 | >> "%REPORT_FILE%"
echo | JMH缓存测试 | 已完成 | 控制台输出 | >> "%REPORT_FILE%"

(
echo.
echo ## 性能基准指标
echo.
echo | 指标类别 | 指标名称 | 基准值 | 单位 | 说明 |
echo |---------|---------|--------|------|------|
echo | API | 策略查询响应时间 | ^< 100 | ms | P95响应时间 |
echo | API | 策略创建响应时间 | ^< 200 | ms | P95响应时间 |
echo | API | 吞吐量 | ^> 500 | TPS | 每秒处理事务数 |
echo | 规则引擎 | 单规则执行时间 | ^< 5 | ms | 平均执行时间 |
echo | 规则引擎 | 并发规则执行吞吐量 | ^> 1000 | TPS | 每秒处理规则数 |
echo | 优化算法 | 1000条数据优化时间 | ^< 1000 | ms | 优化计算时间 |
echo | 缓存 | 读取延迟 | ^< 1 | ms | 平均读取时间 |
echo | 缓存 | 写入延迟 | ^< 2 | ms | 平均写入时间 |
echo.
echo ## 后续步骤
echo.
echo 1. 查看详细的JMeter测试报告
echo 2. 分析JMH基准测试输出
echo 3. 根据实际测试数据更新基准指标
echo 4. 定期运行性能测试进行监控
echo.
echo ---
echo *本报告由自动化测试脚本生成，测试结果需根据实际运行数据进行填充。*
) >> "%REPORT_FILE%"

echo [SUCCESS] 报告生成完成: %REPORT_FILE%

:end
echo.
echo ==============================================
echo    性能基准测试执行完成!
echo ==============================================
echo.
echo 重要文件位置:
echo   1. 性能测试计划: %BASE_DIR%\..\docs\performance\price-strategy-benchmark-plan.md
echo   2. JMeter测试配置: %JMETER_DIR%\api-load-test.jmx
echo   3. JMH基准测试代码: %JMH_DIR%\
echo   4. 测试数据脚本: %SCRIPTS_DIR%\setup-test-data.py
echo   5. 测试报告目录: %REPORTS_DIR%\
echo.
pause