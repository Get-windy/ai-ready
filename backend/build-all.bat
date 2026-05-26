@echo off
echo ========================================
echo ERP系统架构验证脚本
echo ========================================
echo.

echo [1/3] 检查目录结构...
echo.

REM 检查模块目录是否存在
set MODULES=batch customer expense finance inventory invoice metrics monitor order purchase sales supplier

for %%m in (%MODULES%) do (
    if exist "erp-modules\%%m" (
        echo   ✓ erp-modules\%%m
        if exist "erp-modules\%%m\src\main\java\cn\aiedge\erp\%%m" (
            echo     ✓ Java包结构正确
        ) else (
            echo     ✗ Java包结构不正确
        )
    ) else (
        echo   ✗ erp-modules\%%m 不存在
    )
)

echo.
echo [2/3] 检查POM文件...
echo.

if exist "pom.xml" (
    echo   ✓ 父POM文件存在
) else (
    echo   ✗ 父POM文件不存在
)

if exist "infrastructure\pom.xml" (
    echo   ✓ 公共模块POM文件存在
) else (
    echo   ✗ 公共模块POM文件不存在
)

echo.
echo [3/3] 检查架构规范...
echo.

echo   ? 架构设计文档位置: ERP_ARCHITECTURE_DESIGN.md
echo   ? 模块数量: 12 个标准模块
echo   ? 技术栈: Spring Boot 3.2.5 + Java 17
echo.

echo ========================================
echo 架构验证完成！
echo 下一步：请团队成员基于新架构开始开发
echo ========================================
pause