-- =============================================================================
-- HR 模块 · 职员管理收敛：菜单改指「能力更强的那套实现」+ 补开权限配置入口
-- V11.422.0 · 2026-09-19
--
-- 【依据】《人力资源模块/全部操作员开发文档》§5.5 收敛裁定（P1）：
--   「菜单 80532 的 component 建议直接改指 views/system/user/index.vue，本页废弃并删除。」
--   《岗位权限开发文档》§5.5：「views/system/position/index.vue 能力明显强于本页，
--   建议菜单 80531 的 component 直接改指它，本页废弃；
--   同时给 views/system/role/index.vue 单开一个菜单（这才是产品上真正缺的「权限配置」入口）。」
--   两条在文档中均标注「P1 待办 / 本轮未动 views/system/」，本迁移负责落地。
--
-- 【为什么是改指向，而不是删 views/system/*】
--   该域是「两套实现，且功能弱的那套才挂了菜单」：
--     system/user/index.vue       918 行  ← 保留（本迁移把 80532 指过来）
--     system/position/index.vue  1301 行  ← 保留（本迁移把 80531 指过来）
--     system/role/index.vue       931 行  ← 保留（本迁移新增菜单 80533 给它入口）
--   被废弃的是 md/staff-all 与 md/staff-role 两页（页面文件由前端清理同步删除）。
--
-- 【字段值口径】逐列对照同父（61405「职员管理」）既有行 80530 的实测值：
--   tenant_id=0 / parent_id=61405 / client_type='tenant-admin' / menu_type=1
--   visible=1 / status=1 / deleted=0 / is_external=0 / is_cache=1
--   display_group=0 / display_mode=0 / menu_level=3 / route_name='' / redirect='' / icon=''
--   ⚠️ menu_level 取 3（跟随 80530~80532 的实测值），**不要**沿用 system-admin 域的 1。
--
-- 【本迁移刻意不做的事】
--   · 不动 80530「职员部门」——《职员部门开发文档》§5.5 要求先完成「能力差集比对」
--     （system/department 1138 行 vs md/staff-dept 的列/查询/按钮逐项对照），
--     该前置动作尚未完成，故维持原指向。
--   · 不改任何 path —— 裁定只要求改 component；保留原 path 可让既有 URL（书签）不失效。
--   · 不补权限码 —— 经查 system/{user,position,role}/index.vue 三页均无 v-permission /
--     hasPermission 引用，无权限码依赖。
--
-- 【id 号段纪律】本仓 9xxxx / 613xxxx / 805xx 为多会话共用号段。落地前实测（devdb，2026-09-19）：
--   SELECT id FROM sys_menu WHERE id BETWEEN 80500 AND 80599 ORDER BY id
--     → 80510~80513 / 80520 / 80530~80532 / 80550~80552，**80533 空闲**
--   SELECT count(*) FROM sys_menu WHERE id IN (80533,80534,80535)                      → 0
--   SELECT count(*) FROM sys_menu WHERE menu_code='system:role' OR path='system/role/index' → 0
--   `sys_menu.id` 无序列默认值，必须显式给值。
--   迁移版本号：落地前实测该目录最大为 V11.421.0__Seed_Sys_Params_Remaining_Six_Views.sql，故取 V11.422.0。
--
-- 【幂等】sys_menu 无业务列唯一约束，故 UPDATE 带 `deleted = 0` 条件（重复执行结果一致），
--   INSERT 用 `ON CONFLICT (id) DO NOTHING`。
--
-- 【回滚】
--   UPDATE sys_menu SET component='views/md/staff-all/index.vue'    WHERE id=80532;
--   UPDATE sys_menu SET component='views/md/staff-role/index.vue'   WHERE id=80531;
--   DELETE FROM sys_menu WHERE id=80533;
-- =============================================================================

-- ── ① 80532「全部操作员」改指能力更完整的 system/user/index.vue ───────────────────
UPDATE sys_menu
SET component   = 'views/system/user/index.vue',
    update_time = now()
WHERE id = 80532
  AND deleted = 0;

-- ── ② 80531「岗位权限」改指能力更完整的 system/position/index.vue ─────────────────
UPDATE sys_menu
SET component   = 'views/system/position/index.vue',
    update_time = now()
WHERE id = 80531
  AND deleted = 0;

-- ── ③ 新增 80533「权限配置」→ system/role/index.vue ─────────────────────────────
--    《岗位权限开发文档》§5.5：这是产品上真正缺的「权限配置」入口。
--    path 取 `system/role/index`：与 router/dynamicRoutes.ts 的 componentMap 键一致，
--    避免 getComponent 归一化产生歧义（口径同 V11.419.0）。
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES (80533, 0, 61405, 0, now(), now(),
        '权限配置', 'system:role', 1, 'system/role/index', 'views/system/role/index.vue', '', '',
        '', 4, 0, 1, 1, 1, 'tenant-admin',
        '', '', 0, 0, 3)
ON CONFLICT (id) DO NOTHING;
