@echo off
echo ========== 数据库备份脚本验证开始 ==========
echo 验证时间: %date% %time%
echo.

echo 检查关键文件:
echo.

set "FILES_EXIST=1"

if exist "I:\AI-Ready\qa\database\database-backup-recovery-drill-plan.md" (
    echo ✅ 备份恢复演练计划
    echo    路径: I:\AI-Ready\qa\database\database-backup-recovery-drill-plan.md
) else (
    echo ❌ 备份恢复演练计划
    echo    路径: I:\AI-Ready\qa\database\database-backup-recovery-drill-plan.md
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

if exist "I:\AI-Ready\scripts\backup\postgresql\backup_postgresql.sh" (
    echo ✅ PostgreSQL备份脚本
    echo    路径: I:\AI-Ready\scripts\backup\postgresql\backup_postgresql.sh
) else (
    echo ❌ PostgreSQL备份脚本
    echo    路径: I:\AI-Ready\scripts\backup\postgresql\backup_postgresql.sh
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

if exist "I:\AI-Ready\scripts\backup\postgresql\restore_postgresql.sh" (
    echo ✅ PostgreSQL恢复脚本
    echo    路径: I:\AI-Ready\scripts\backup\postgresql\restore_postgresql.sh
) else (
    echo ❌ PostgreSQL恢复脚本
    echo    路径: I:\AI-Ready\scripts\backup\postgresql\restore_postgresql.sh
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

if exist "I:\AI-Ready\scripts\backup\redis\backup_redis.sh" (
    echo ✅ Redis备份脚本
    echo    路径: I:\AI-Ready\scripts\backup\redis\backup_redis.sh
) else (
    echo ❌ Redis备份脚本
    echo    路径: I:\AI-Ready\scripts\backup\redis\backup_redis.sh
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

if exist "I:\AI-Ready\scripts\backup\redis\restore_redis.sh" (
    echo ✅ Redis恢复脚本
    echo    路径: I:\AI-Ready\scripts\backup\redis\restore_redis.sh
) else (
    echo ❌ Redis恢复脚本
    echo    路径: I:\AI-Ready\scripts\backup\redis\restore_redis.sh
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

if exist "I:\AI-Ready\qa\database\execute-backup-recovery-drill.sh" (
    echo ✅ 演练执行脚本
    echo    路径: I:\AI-Ready\qa\database\execute-backup-recovery-drill.sh
) else (
    echo ❌ 演练执行脚本
    echo    路径: I:\AI-Ready\qa\database\execute-backup-recovery-drill.sh
    echo    状态: 文件不存在
    set "FILES_EXIST=0"
)

echo.
echo ========== 目录结构验证 ==========
echo.

if exist "I:\AI-Ready\qa\database" (
    echo ✅ QA数据库目录
    echo    路径: I:\AI-Ready\qa\database
) else (
    echo ⚠️ QA数据库目录
    echo    路径: I:\AI-Ready\qa\database
    echo    状态: 目录不存在
)

if exist "I:\AI-Ready\scripts\backup\postgresql" (
    echo ✅ PostgreSQL备份脚本目录
    echo    路径: I:\AI-Ready\scripts\backup\postgresql
) else (
    echo ⚠️ PostgreSQL备份脚本目录
    echo    路径: I:\AI-Ready\scripts\backup\postgresql
    echo    状态: 目录不存在
)

if exist "I:\AI-Ready\scripts\backup\redis" (
    echo ✅ Redis备份脚本目录
    echo    路径: I:\AI-Ready\scripts\backup\redis
) else (
    echo ⚠️ Redis备份脚本目录
    echo    路径: I:\AI-Ready\scripts\backup\redis
    echo    状态: 目录不存在
)

echo.
echo ========== 验证报告 ==========
echo.

if "%FILES_EXIST%"=="1" (
    echo ✅ 所有关键文件检查通过
    echo.
    echo 📋 已完成的交付物:
    echo    1. 数据库备份恢复演练计划文档
    echo    2. PostgreSQL备份脚本 (backup_postgresql.sh)
    echo    3. PostgreSQL恢复脚本 (restore_postgresql.sh)
    echo    4. Redis备份脚本 (backup_redis.sh)
    echo    5. Redis恢复脚本 (restore_redis.sh)
    echo    6. 演练执行脚本 (execute-backup-recovery-drill.sh)
    echo    7. 目录结构 (scripts/backup/, qa/database/)
    echo.
    echo 📅 后续步骤建议:
    echo    1. 在实际测试环境部署数据库
    echo    2. 执行完整的备份恢复演练
    echo    3. 验证备份文件的完整性和可恢复性
    echo    4. 配置定时备份任务
    echo    5. 建立备份监控和告警机制
) else (
    echo ❌ 部分文件缺失，请检查创建过程
    echo    建议重新执行文件创建步骤
)

echo.
echo ========== 验证完成 ==========
echo 验证时间: %date% %time%
pause