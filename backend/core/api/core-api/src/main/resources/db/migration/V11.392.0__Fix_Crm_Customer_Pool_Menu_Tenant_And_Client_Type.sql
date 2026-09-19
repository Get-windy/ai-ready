-- =============================================================================
-- CRM · 订正「客户公海」菜单行的 tenant_id 与 client_type（2026-09-18）
--
-- 【缺陷】`V11.383.0` 插入菜单 `80202 客户公海` 时，字段值照抄了
--   `V6.21.0__Complete_Mega_Menu_Leaf_Items.sql` 的**历史写法**：
--       tenant_id = 1、client_type = 'pc-admin'
--   但 CRM 现有全部菜单（`60007` / `60701` / `80200` / `80201` / `70303`…）在真库里
--   实际是：
--       tenant_id = 0、client_type = 'tenant-admin'
--   —— 说明 V6.21.0 的写法早已被其后的迁移订正过，而新迁移不该照抄旧模板。
--
-- 【后果（真机实测）】`GET /api/menu/tree` 走 `SysMenuServiceImpl.getMenuTree` →
--   `listAllMenus(0L)`，**只取 tenant_id = 0 的菜单**（菜单是系统级全局资源）。
--   于是 80202 虽在 `sys_menu` 表里（肉眼可查），却**不进菜单树** →
--   前端不生成 `crm/customer-pool` 路由 → 直接访问该 URL 落到全局 404 页
--   （E2E UI 节实测：`客户公海` 页截图为「404 抱歉，您访问的页面不存在」）。
--
-- 【修法】把该行订正为与同组菜单完全一致的取值。补一个**新迁移**而不是改
--   `V11.383.0` 的正文 —— 后者已被 Flyway 应用，改正文会让 checksum 失配、
--   validate 失败、后端起不来（见《CRM模块/README.md》§3.2 的迁移纪律）。
-- =============================================================================

UPDATE sys_menu
   SET tenant_id   = 0,
       client_type = 'tenant-admin',
       display_mode = 0,
       menu_level   = 3,
       is_external  = 0,
       is_cache     = 1
 WHERE id = 80202;

-- 同时把「未来干净重建」时也会踩到的源头改掉：V11.383.0 的 INSERT 值保持原样，
-- 由本迁移在链尾统一订正（迁移不可变，这是代价最小且可复现的做法）。
