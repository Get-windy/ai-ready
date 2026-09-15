-- ============================================================
-- V6.25.0 菜单重组调整：收货业务和仓库发货移回仓储模块
--
-- 变更说明：
--   1. 仓库发货(60309)从配发收移回仓储，恢复原名"发货作业"
--   2. 收货业务(60403)从配发收移到仓储
--   目的：将仓库相关操作集中在仓储模块
-- ============================================================

-- 移动 60309 (仓库发货) 回仓储模块，恢复原名"发货作业"
UPDATE sys_menu SET
    parent_id = 60003,
    menu_name = '发货作业',
    menu_code = 'mega:wh:shipping',
    sort = 800,
    update_time = CURRENT_TIMESTAMP
WHERE id = 60309;

-- 移动 60403 (收货业务) 到仓储模块
UPDATE sys_menu SET
    parent_id = 60003,
    menu_name = '收货业务',
    menu_code = 'mega:wh:receive',
    sort = 900,
    update_time = CURRENT_TIMESTAMP
WHERE id = 60403;

-- ============================================================
-- 验证结果
-- ============================================================
SELECT '仓储模块下的列（应包含收货业务和发货作业）：' AS info;

SELECT
    id,
    menu_name,
    menu_code,
    sort,
    parent_id
FROM sys_menu
WHERE parent_id = 60003 AND menu_type = 0 AND deleted = 0
ORDER BY sort;

SELECT '配发收模块下的列（应不再包含收货业务和仓库发货）：' AS info;

SELECT
    id,
    menu_name,
    menu_code,
    sort
FROM sys_menu
WHERE parent_id = 60004 AND menu_type = 0 AND deleted = 0
ORDER BY sort;
