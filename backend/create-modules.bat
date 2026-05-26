@echo off
echo 开始创建ERP模块标准化目录结构...
echo ========================================

REM 模块列表
set MODULES=batch customer expense finance inventory invoice metrics monitor order purchase sales supplier

REM 基础目录
set BASE_PATH=I:\AI-Ready\backend\erp-modules

for %%m in (%MODULES%) do (
    echo 处理模块: %%m
    set MODULE_PATH=%BASE_PATH%\%%m
    
    if exist "%MODULE_PATH%" (
        echo   ✓ 模块目录已存在: %%m
        
        REM 创建标准目录结构
        mkdir "%MODULE_PATH%\src\main\java\cn\aiedge\erp\%%m\controller" 2>nul
        mkdir "%MODULE_PATH%\src\main\java\cn\aiedge\erp\%%m\service" 2>nul
        mkdir "%MODULE_PATH%\src\main\java\cn\aiedge\erp\%%m\repository" 2>nul
        mkdir "%MODULE_PATH%\src\main\java\cn\aiedge\erp\%%m\model" 2>nul
        mkdir "%MODULE_PATH%\src\main\java\cn\aiedge\erp\%%m\config" 2>nul
        mkdir "%MODULE_PATH%\src\test\java\cn\aiedge\erp\%%m" 2>nul
        mkdir "%MODULE_PATH%\docs" 2>nul
        
        echo   + 目录结构创建完成
        echo.
    ) else (
        echo   ✗ 模块目录不存在: %%m
    )
)

echo ========================================
echo ERP模块标准化目录结构创建完成！
echo 总计处理模块: 12 个
pause