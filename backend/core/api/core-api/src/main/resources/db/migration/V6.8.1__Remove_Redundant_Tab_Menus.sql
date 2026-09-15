-- ============================================================
-- V6.8.0: 删除冗余的 tab 聚合页菜单项
--
-- 背景：/purchase、/sale、/stock 三个 tab 聚合页的前端文件已被删除，
-- 这些页面将 7 个不同业务实体塞入一个页面，违反企业系统
-- "一个页面一个业务意图"的原则。对应的独立页面已在 /erp/ 路径下存在。
--
-- 删除的菜单项：
--   50102 - 销售订单 (sale → views/sale/index.vue) 已删除
--   50104 - 销售出库 (stock → views/stock/index.vue) 已删除
--   50201 - 采购订单 (purchase → views/purchase/index.vue) 已删除
--
-- 修复的菜单项：
--   50204 - 采购退货 原来 path='purchase' component='views/purchase/index.vue'
--          这是复制粘贴 bug，实际应与 erp/purchase-return 对应
-- ============================================================

-- 1. 删除冗余的 tab 聚合页菜单项
DELETE FROM sys_menu WHERE id IN (50102, 50104, 50201);

-- 2. 清理 sys_role_menu 中对应的孤立关联
DELETE FROM sys_role_menu WHERE menu_id IN (50102, 50104, 50201);

-- 3. 修复 50204 (采购退货) 的路径和组件 — 原来错误地指向 purchase tab 页
UPDATE sys_menu
SET path = 'erp/purchase-return',
    component = 'views/erp/purchase-return/index.vue',
    menu_code = 'purchase:return'
WHERE id = 50204 AND deleted = 0;

-- 4. 使 erp/sale 和 erp/purchase 菜单可见（原为隐藏，因为之前有 tab 页作为主入口）
UPDATE sys_menu SET visible = 1 WHERE id = 50103 AND deleted = 0;
UPDATE sys_menu SET visible = 1 WHERE id = 50202 AND deleted = 0;

-- ============================================================
-- 验证
-- ============================================================
SELECT id, parent_id, menu_name, path, component, visible
FROM sys_menu
WHERE id IN (50102, 50103, 50104, 50201, 50202, 50204)
ORDER BY id;
