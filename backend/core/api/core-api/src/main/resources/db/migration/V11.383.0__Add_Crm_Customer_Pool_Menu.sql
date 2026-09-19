-- =============================================================================
-- CRM · 新增「客户公海」菜单（2026-09-18）
--
-- 背景：后端 `CustomerPoolController` 已有 10 个端点（放入 / 领取 / 退回 /
--       自动回收 / 过期检查 / 统计），表 `crm_customer_pool`（30 列）也已建好，
--       但**前端无页面、`sys_menu` 无菜单** → 这些能力用户完全不可达。
--       见《CRM模块/README.md》§2.3「无菜单的孤儿实现」与 §7 的 P0 条目。
--
-- 本迁移只补菜单行；页面本体为 `views/crm/customer-pool/index.vue`
-- （已同步登记进 `router/dynamicRoutes.ts` 的 componentMap）。
-- 菜单挂在「客户管理」(60701) 分组下，顺序第 4（客户 / 客户跟进 / 客户分级 / 客户公海）。
--
-- 字段口径与既有 CRM 菜单（V6.21.0）保持一致：
--   display_mode = 0（单入口，无配对标签页）、list_path = NULL、tag_label = NULL。
-- =============================================================================

INSERT INTO sys_menu
  (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code,
   menu_type, path, component, icon, sort, visible, status, client_type,
   display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (80202, 1, 60701, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户公海', 'crm:customer-pool',
   1, 'crm/customer-pool', 'views/crm/customer-pool/index.vue', 'CloudOutlined', 4, 1, 1, 'pc-admin',
   0, NULL, NULL, 0, 0)
ON CONFLICT (id) DO NOTHING;
