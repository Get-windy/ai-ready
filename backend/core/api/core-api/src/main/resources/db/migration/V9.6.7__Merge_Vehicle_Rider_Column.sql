-- ============================================================
-- V9.6.7: 合并配送下「车辆管理」和「骑手管理」列为「人车管理」
--
-- 背景：
--   60502(车辆管理) 和 60503(骑手管理) 各只有2个叶子，列太窄，合并为一列。
--   各自的双入口叶子(50504/50503)已具备 display_mode=1 + tag_label='添加'，
--   点击进列表，[添加]按钮进新增表单。列表叶子(80710/80720)冗余，硬删除。
--
-- 变更：
--   1. 60502 重命名为「人车管理」
--   2. 50503(骑手管理) 移到 60502 下
--   3. 硬删除 60503(原骑手管理列头)、80710(车辆列表)、80720(骑手列表)
-- ============================================================

-- Step 1: 重命名列头 60502
UPDATE sys_menu
SET menu_name = '人车管理', menu_code = 'mega:dms:people-vehicle', update_time = CURRENT_TIMESTAMP
WHERE id = 60502;

-- Step 2: 将骑手管理叶子(50503)移到人车管理列，sort=2
UPDATE sys_menu
SET parent_id = 60502, sort = 2, update_time = CURRENT_TIMESTAMP
WHERE id = 50503;

-- Step 3: 调整车辆管理叶子(50504)排序为 sort=1
UPDATE sys_menu SET sort = 1, update_time = CURRENT_TIMESTAMP WHERE id = 50504;

-- Step 4: 硬删除冗余节点
DELETE FROM sys_menu WHERE id IN (60503, 80710, 80720);

-- Step 5: 调整配送下其他列排序（填补 60503 删除后的空隙）
UPDATE sys_menu SET sort = 300, update_time = CURRENT_TIMESTAMP WHERE id = 60402;  -- 物流配送
UPDATE sys_menu SET sort = 400, update_time = CURRENT_TIMESTAMP WHERE id = 60505;  -- 配送跟踪
UPDATE sys_menu SET sort = 500, update_time = CURRENT_TIMESTAMP WHERE id = 60506;  -- 配送配置
UPDATE sys_menu SET sort = 600, update_time = CURRENT_TIMESTAMP WHERE id = 61506;  -- API监控

-- Step 6: 验证
-- SELECT id, menu_name, parent_id, sort, display_mode, tag_label
-- FROM sys_menu WHERE parent_id = 60502 AND deleted = 0 ORDER BY sort;
