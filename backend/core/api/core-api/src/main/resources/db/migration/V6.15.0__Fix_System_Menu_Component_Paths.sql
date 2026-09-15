-- ============================================================
-- V6.15.0: 修复系统级菜单元件路径
--
-- 问题：V6.13.0 种子数据中所有系统级菜单的 component 字段
-- 均设置为 'views/common/placeholder/index.vue'（占位组件），
-- 导致点击系统菜单时显示"功能开发中"页面。
--
-- 实际情况：V6.14.0 已为所有系统级菜单创建了完整的前端页面，
-- 需将 component 更新为指向真实页面路径，以匹配
-- dynamicRoutes.ts 中 componentMap 的 key。
--
-- 修复范围：
--   系统模块（60013）下所有叶子菜单（ID 62001-62505）
--   代码生成（62401）确认不开发，删除该菜单项
-- ============================================================

-- 6.1 租户管理列（61301）
UPDATE sys_menu SET component = 'views/admin/tenant/list',        update_time = CURRENT_TIMESTAMP WHERE id = 62001;
UPDATE sys_menu SET component = 'views/admin/tenant/approval',    update_time = CURRENT_TIMESTAMP WHERE id = 62002;
UPDATE sys_menu SET component = 'views/admin/tenant/package',     update_time = CURRENT_TIMESTAMP WHERE id = 62003;
UPDATE sys_menu SET component = 'views/admin/tenant/module-auth', update_time = CURRENT_TIMESTAMP WHERE id = 62004;
UPDATE sys_menu SET component = 'views/admin/tenant/quota',       update_time = CURRENT_TIMESTAMP WHERE id = 62005;

-- 6.2 模块管理列（61302）
UPDATE sys_menu SET component = 'views/admin/module/list',    update_time = CURRENT_TIMESTAMP WHERE id = 62101;
UPDATE sys_menu SET component = 'views/admin/module/version', update_time = CURRENT_TIMESTAMP WHERE id = 62102;
UPDATE sys_menu SET component = 'views/admin/module/release', update_time = CURRENT_TIMESTAMP WHERE id = 62103;
UPDATE sys_menu SET component = 'views/admin/module/usage',   update_time = CURRENT_TIMESTAMP WHERE id = 62104;

-- 6.3 系统监控列（61303）
-- 注意：62204 原值为 'views/system/log/index.vue'，改为 'views/admin/monitor/log'
-- 以匹配 componentMap key 'admin/monitor/log'
UPDATE sys_menu SET component = 'views/admin/monitor/health',      update_time = CURRENT_TIMESTAMP WHERE id = 62201;
UPDATE sys_menu SET component = 'views/admin/monitor/performance', update_time = CURRENT_TIMESTAMP WHERE id = 62202;
UPDATE sys_menu SET component = 'views/admin/monitor/api',         update_time = CURRENT_TIMESTAMP WHERE id = 62203;
UPDATE sys_menu SET component = 'views/admin/monitor/log',         update_time = CURRENT_TIMESTAMP WHERE id = 62204;
UPDATE sys_menu SET component = 'views/admin/monitor/audit',       update_time = CURRENT_TIMESTAMP WHERE id = 62205;
UPDATE sys_menu SET component = 'views/admin/monitor/cache',       update_time = CURRENT_TIMESTAMP WHERE id = 62206;

-- 6.4 数据管理列（61304）
UPDATE sys_menu SET component = 'views/admin/data/connection', update_time = CURRENT_TIMESTAMP WHERE id = 62301;
UPDATE sys_menu SET component = 'views/admin/data/slow-query', update_time = CURRENT_TIMESTAMP WHERE id = 62302;
UPDATE sys_menu SET component = 'views/admin/data/backup',     update_time = CURRENT_TIMESTAMP WHERE id = 62303;
UPDATE sys_menu SET component = 'views/admin/data/sync',       update_time = CURRENT_TIMESTAMP WHERE id = 62304;
UPDATE sys_menu SET component = 'views/admin/data/cleanup',    update_time = CURRENT_TIMESTAMP WHERE id = 62305;

-- 6.5 开发工具列（61305）
-- 62401（代码生成）确认不开发，删除菜单项
DELETE FROM sys_menu WHERE id = 62401;
UPDATE sys_menu SET component = 'views/admin/dev/template',  update_time = CURRENT_TIMESTAMP WHERE id = 62402;
UPDATE sys_menu SET component = 'views/admin/dev/api-doc',   update_time = CURRENT_TIMESTAMP WHERE id = 62403;
UPDATE sys_menu SET component = 'views/admin/dev/api-test',  update_time = CURRENT_TIMESTAMP WHERE id = 62404;
UPDATE sys_menu SET component = 'views/admin/dev/scheduler', update_time = CURRENT_TIMESTAMP WHERE id = 62405;

-- 6.6 平台设置列（61306）
UPDATE sys_menu SET component = 'views/admin/platform/params',   update_time = CURRENT_TIMESTAMP WHERE id = 62501;
UPDATE sys_menu SET component = 'views/admin/platform/mail',     update_time = CURRENT_TIMESTAMP WHERE id = 62502;
UPDATE sys_menu SET component = 'views/admin/platform/sms',      update_time = CURRENT_TIMESTAMP WHERE id = 62503;
UPDATE sys_menu SET component = 'views/admin/platform/storage',  update_time = CURRENT_TIMESTAMP WHERE id = 62504;
UPDATE sys_menu SET component = 'views/admin/platform/security', update_time = CURRENT_TIMESTAMP WHERE id = 62505;
