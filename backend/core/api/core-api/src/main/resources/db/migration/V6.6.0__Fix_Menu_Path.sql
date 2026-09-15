-- ============================================================
-- V6.6.0: 修复菜单路径配置问题
--
-- 问题：部分父子菜单的path字段相同，导致动态路由生成错误
-- 例如：父菜单 path='dashboard'，子菜单 path='dashboard'
--       生成的路由路径变成 'dashboard/dashboard'
--
-- 修复：将子菜单的path改为'index'，使完整路径正确
-- ============================================================

-- 修复工作台菜单
UPDATE sys_menu SET path = 'index' WHERE id = 50011;

-- 修复预算管理菜单
UPDATE sys_menu SET path = 'index' WHERE id = 51001;

-- 修复订单中心菜单
UPDATE sys_menu SET path = 'index' WHERE id = 51201;

-- 修复图表菜单
UPDATE sys_menu SET path = 'index' WHERE id = 51701;