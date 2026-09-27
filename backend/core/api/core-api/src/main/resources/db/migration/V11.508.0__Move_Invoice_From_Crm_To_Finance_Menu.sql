-- 2026-09-26 发票从 CRM 菜单搬回财务模块（见 I:/AI-Ready/CRM_MODULE_AUDIT_20260923.md §2.5 / §5.3）
--
-- 背景：发票**从来就不是 CRM 的能力** —— 后端实现在 `erp-finance` 的 `InvoiceController`
-- （前缀 `/api/erp/invoice`，26 个端点，鉴权用 `invoice:*` 共 6 个码），CRM README §5 与
-- 《发票开发文档》也明确「发票走 ERP 财务模块，不属于 crm 模块」。
-- 但菜单挂在 CRM 顶层（`60007 → 60706 发票管理 → 70350 发票`），前端页面在 `views/crm/invoice/`，
-- 按钮用的却是 7 个 `crm:invoice:*` 码 —— 那 7 个码**后端零消费**、与真正守卫的 `invoice:*`
-- 零交集，且模块权益门按 `invoice:` → `finance` 判定。三者叠加的后果：
--   ① 给了 `crm:invoice:*` 的角色按钮可见、点下去 403；
--   ② 只买 CRM 不买财务的租户，菜单可见但接口被权益门拒绝；
--   ③ 一套页面对应两套权限码，无法做职责分离。
--
-- 本迁移：菜单搬回财务 + 权限码统一到后端真正使用的 `invoice:*`（代码侧已同步改前端 v-permission）。
--
-- ⚠️ 前置已核（2026-09-26）：
--   · 全库只有 CRM 下的发票菜单，**财务模块此前没有发票入口**（`information_schema` 已查：
--     path/component/menu_code 含 invoice 的仅 70350 与 analytics 的 80455 发票统计）⇒ 不是重复实现，是归位；
--   · `sys_role_menu` / `sys_tenant_menu` 对 70350、60706 的引用均为 0 行 ⇒ 可安全物理删除。

-- ── ① 移除 CRM 下的发票菜单（分组 60706 + 叶子 70350）─────────────────────
DELETE FROM sys_role_menu   WHERE menu_id IN (70350, 60706);
DELETE FROM sys_tenant_menu WHERE menu_id IN (70350, 60706);
DELETE FROM sys_menu        WHERE id IN (70350, 60706);

-- ── ② 财务下新增「发票管理」分组 + 「发票」双入口叶子 ─────────────────────
-- 分组挂财务顶级菜单 60006 下，sort=1000 排在已有 9 个分组（100~900）之后。
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, icon, sort,
                      is_external, is_cache, visible, status, client_type,
                      display_group, menu_level, route_name)
SELECT 60610, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       '发票管理', 'mega:fin:invoice', 0, 'FileTextOutlined', 1000,
       0, 0, 1, 1, 'tenant-admin', 0, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 60610);

-- 叶子沿用原 CRM 侧的双入口形态（path=表单页、list_path=列表页、tag=[历史]），
-- 只把域前缀换成 finance；menu_code 用 `invoice` —— 权限码库里有 `invoice:view/...`，
-- 按菜单派生规则（MenuPermissionDeriver 的 ':' 边界前缀集）`invoice` 是它们的前缀 ⇒ 可被授权控制。
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, icon, sort,
                      is_external, is_cache, visible, status, client_type,
                      display_group, menu_level, display_mode, list_path, tag_label, route_name)
SELECT 80152, 0, 60610, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       '发票', 'invoice', 1, 'finance/invoice/form', 'views/finance/invoice/form.vue',
       'FileTextOutlined', 1, 0, 1, 1, 1, 'tenant-admin', 0, 0, 1, 'finance/invoice/index', '历史', 'FinInvoice'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 80152);

-- ── ③ 删除 7 条未被任何后端端点消费的 crm:invoice:* 权限码 ─────────────────
-- 这些码只在前端 v-permission 里出现过（现前端已改用 invoice:*），
-- 删掉可避免「同一功能两套码」继续误导后续授权配置。
DELETE FROM sys_role_permission
 WHERE permission_id IN (SELECT id FROM sys_permission WHERE permission_code LIKE 'crm:invoice:%');
DELETE FROM sys_permission WHERE permission_code LIKE 'crm:invoice:%';

-- ── ④ 自检 ───────────────────────────────────────────────────────────────
-- 1) CRM 下应再无发票菜单：期望 0
--   SELECT count(*) FROM sys_menu WHERE deleted = 0 AND (path LIKE 'crm/invoice%' OR id IN (60706, 70350));
-- 2) 财务下发票菜单齐否：期望 2
--   SELECT count(*) FROM sys_menu WHERE deleted = 0 AND id IN (60610, 80152);
-- 3) 残留 crm:invoice 码：期望 0
--   SELECT count(*) FROM sys_permission WHERE permission_code LIKE 'crm:invoice:%';
