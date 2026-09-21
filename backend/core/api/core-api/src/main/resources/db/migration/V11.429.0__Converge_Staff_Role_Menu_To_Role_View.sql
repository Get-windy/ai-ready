-- =============================================================================
-- HR 模块 · 职员管理：「岗位权限」菜单改指角色页 + 撤销重复入口
-- V11.429.0 · 2026-09-19
--
-- 【依据】2026-09-19 登录对标系统 22stable.ql361.com 实抓「资料 → 职员权限 → 岗位权限」页，
--   其真实形态 = 岗位列表 + 行级「设置权限」→「角色权限设置」面板（左侧域树 +
--   右侧「功能名称 × 查看/打印/增加/删除/修改/导出」权限矩阵）。
--   即：ql361 的「岗位」就是权限载体，页面核心价值是「给岗位配权限」。
--
-- 【我方语义对齐】本系统权限载体是 sys_role（角色），不是 sys_position（岗位主数据）。
--   · `views/system/position/index.vue`（1601 行）只有岗位主数据 CRUD + position:* 按钮码，
--     全页无 role / permission / menu 引用 —— 点进去配不了任何权限，名实不符。
--   · `views/system/role/index.vue` 才是「角色 + 权限/菜单/单据类型权限」的载体，
--     api/role.ts 已具备 getPermissions / assignPermissions（入参为 permissionId 数组）。
--   → 故把 80531「岗位权限」的 component 改指角色页，由该页承担「租户级权限配置」职责。
--
-- 【为什么同时撤销 80533「权限配置」】
--   V11.422.0 为 views/system/role/index.vue 单独开了 80533 入口。80531 改指同一组件后，
--   同一页面会出现两个菜单入口（违反「功能/模块不重复开发」纪律），故软删 80533。
--   撤销后本分组恢复为与 ql361 一致的三个页面：职员部门 / 岗位权限 / 全部操作员。
--
-- 【字段值口径】不新增任何行，只 UPDATE 既有行，字段取值沿用 V11.422.0 的实测口径。
--
-- 【本迁移刻意不做的事】
--   · 不改 80531 的 path（保留 `md/staff-role`）—— 沿用 V11.422.0 的原则，避免既有书签/外链失效；
--     页面的 path 只影响 URL，不影响组件加载。
--   · 不改 menu_name / menu_code —— 「岗位权限」这个名字在改指角色页之后反而名副其实了。
--   · 不动 80530（职员部门）、80532（全部操作员）。
--
-- 【幂等】两条 UPDATE 都带 `deleted = 0` 条件，重复执行结果一致（UPDATE 是幂等的；
--   80533 软删后 deleted=1，再次执行不会命中，同样幂等）。
--
-- 【回滚】
--   UPDATE sys_menu SET component = 'views/system/position/index.vue', deleted = 0 WHERE id = 80531;
--   UPDATE sys_menu SET deleted = 0 WHERE id = 80533;
--
-- 迁移版本号：落地前实测该目录最大为 V11.428.0，故取 V11.429.0。
-- =============================================================================

-- ── ① 80531「岗位权限」改指角色页（角色 = 权限载体，页内提供权限矩阵） ──────────────
UPDATE sys_menu
SET component   = 'views/system/role/index.vue',
    update_time = now()
WHERE id = 80531
  AND deleted = 0;

-- ── ② 撤销 V11.422.0 新增的 80533「权限配置」（与 80531 指向同一页面，属重复入口） ──
UPDATE sys_menu
SET deleted     = 1,
    update_time = now()
WHERE id = 80533
  AND deleted = 0;
