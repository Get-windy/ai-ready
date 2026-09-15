-- ============================================================
-- V9.6.5: 合并顶级「工作流程」(800) 到「设置」(60012) 面板
--
-- 背景：
--   顶级菜单 800「工作流程」与设置下 61206「工作流」存在大量重复：
--   805 流程设计 vs 80610 流程设计，806 流程分析 vs 51303 流程分析。
--   系统一级菜单过多（15个），工作流不应独占一级。
--
-- 解决方案（第一性原理）：
--   工作流对普通用户 = 「我的待办」（运行态）→ 新建「审批」列
--   工作流对管理员 = 「流程设计/监控」（设计态）→ 合并到现有「工作流」列
--   顶级 800 节点删除，重复节点硬删除
-- ============================================================

-- Step 1: 创建新列「审批」(61207) under 设置(60012)
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61207, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '审批', 'mega:set:approval', 0, 610, 1, 1, 'tenant-admin', 0, 0);

-- Step 2: 运行态菜单（用户日常审批）移到「审批」列
UPDATE sys_menu SET parent_id = 61207, sort = 1, update_time = CURRENT_TIMESTAMP WHERE id = 802;  -- 流程实例
UPDATE sys_menu SET parent_id = 61207, sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 803;  -- 我的待办
UPDATE sys_menu SET parent_id = 61207, sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 804;  -- 我的已办

-- Step 3: 设计态菜单合并到「工作流」列（保留现有 80610/51301/51302/51303）
UPDATE sys_menu SET parent_id = 61206, sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 801;  -- 流程定义

-- Step 3.1: 修复从顶级 800 移入的节点的 client_type（原为空）
UPDATE sys_menu SET client_type = 'tenant-admin', update_time = CURRENT_TIMESTAMP
WHERE id IN (801, 802, 803, 804, 8011, 8012, 8013, 8014, 8015, 8031, 8032, 8033, 8034)
  AND (client_type IS NULL OR client_type = '');

-- Step 7: 清理所有 client_type 为空的孤儿节点（父节点已不存在）
DELETE FROM sys_menu WHERE id IN (401, 402, 603, 604, 605, 606, 607, 9021, 9022, 9023);

-- Step 4: 硬删除重复项（805流程设计 vs 80610，806流程分析 vs 51303）
DELETE FROM sys_menu WHERE id IN (805, 806);

-- Step 5: 硬删除顶级节点 800
DELETE FROM sys_menu WHERE id = 800;

-- Step 6: 调整 61206 下其他项排序（给 801 流程定义 让位）
UPDATE sys_menu SET sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 80610;  -- 流程设计
UPDATE sys_menu SET sort = 4, update_time = CURRENT_TIMESTAMP WHERE id = 51301;  -- 流程监控
UPDATE sys_menu SET sort = 5, update_time = CURRENT_TIMESTAMP WHERE id = 51302;  -- 任务管理
UPDATE sys_menu SET sort = 6, update_time = CURRENT_TIMESTAMP WHERE id = 51303;  -- 流程分析
