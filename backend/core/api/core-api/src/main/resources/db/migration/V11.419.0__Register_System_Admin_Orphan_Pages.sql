-- =============================================================================
-- 系统模块 · 孤儿页收口：为 3 个「已实现但无任何菜单入口」的平台页补菜单 + 权限码
-- V11.419.0 · 2026-09-19
--
-- 【背景】《系统模块/README.md》§2.5 与 §10.4-7 登记：`frontend/apps/pc-admin/src/views/system/**`
--   与 `views/admin/**` 下有一批页面无 DB 菜单绑定。本轮处置（同名交付物
--   `tool-results/docgen/evidence/系统模块/_孤儿处置结果.md`）：
--     · 幽灵文件 1 个 → 删除（`views/admin/monitor/log/index.vue`，被 componentMap 别名顶掉）
--     · 能力已被绑定且已验收页面覆盖的重复实现 2 个 → 删除
--       （`views/system/tenant` ← 菜单 62001；`views/system/tenant-approval` ← 菜单 62002）
--     · 权限页三份孤儿重复 → 保留最完整的 `views/system/permission/index.vue`，删另外两份
--     · **本迁移负责的第三类：能力无重叠替代、但用户不可达的页面 → 补菜单入口**
--
-- 【本迁移补 3 个菜单】父菜单 61307「系统管理」（tenant_id=0 / client_type='system-admin' /
--   menu_type=0 / path 为空），原仅 1 个子菜单 6130701「菜单管理」：
--     6130702 数据字典   → views/system/dict/index.vue       （1067 行，真实读写 sys_dict_type / sys_dict_item）
--     6130703 系统配置   → views/system/config/index.vue     （ 847 行，真实读写 /api/config → sys_config）
--     6130704 权限配置   → views/system/permission/index.vue （ 888 行，角色-权限分配 + 权限定义 CRUD）
--
--   【为什么这三页没有能力重叠替代】
--     · 数据字典：devdb 实测 `sys_menu.component LIKE '%dict%'` 0 行；全仓其它菜单无字典页。
--     · 系统配置：本模块内（61301~61307 子树）无同能力页。**跨模块部分重叠已知**：
--       设置模块「系统参数」80621（`views/set/sys-params`，tenant-admin）共用同一 `/api/config`，
--       但它是按分组表单编辑既定参数，本页是配置项台账 CRUD（新增任意键 / 删除 / 批量删 / 刷新缓存 / 导出），
--       且 client_type 不同（platform 侧 vs 租户侧），故不构成替代 —— 重叠已如实登记在交付物「未处理项」。
--     · 权限配置：三份孤儿里唯一保留的一份（另两份已删除）。
--
-- 【字段值口径 —— 逐列对照同模块既有行实测值（devdb，2026-09-19）】
--   `sys_menu` 无序列默认值，id 必须显式给。列值取自「61307 的兄弟组下 29 行 system-admin 叶子菜单」
--   的共同写法（样例 62001 租户列表）：
--     parent_id=61307 / tenant_id=0 / client_type='system-admin' / menu_type=1
--     visible=1 / status=1 / deleted=0 / is_external=0 / is_cache=1
--     display_group=0 / display_mode=0 / menu_level=1 / route_name='' / redirect=''
--   两处刻意取值说明：
--     · menu_level 取 **1**：本模块 29 行 system-admin 叶子菜单（menu_type=1）实测全部为 1；
--       同父的 6130701 为 0 是已登记的历史异常（README §2.4），不跟随。
--     · path 取带 `/index` 后缀的 `system/xxx/index`：与 6130701 的 `system/menu/index` 同构，
--       且恰好等于 `router/dynamicRoutes.ts` 的 componentMap 键（无则需另补键，避免归一化歧义）。
--
-- 【id 号段纪律 —— 本仓 9xxxx / 613xxxx 为多会话共用号段，并行会话已多次撞主键致迁移失败】
--   落地前实测（devdb，2026-09-19，与本文件同一分钟内）：
--     SELECT count(*) FROM sys_menu          WHERE id IN (6130702,6130703,6130704)                        → 0
--     SELECT id FROM sys_menu WHERE id BETWEEN 6130690 AND 6130800  → 仅 6130701（6130702~6130704 空闲）
--     SELECT count(*) FROM sys_permission    WHERE id IN (91601,91602) → 0（91501~91558 已被 V11.407.0 占用）
--     SELECT count(*) FROM sys_permission    WHERE permission_code IN
--            ('system:config:query','system:config:create')             → 0
--     SELECT count(*) FROM sys_role_permission WHERE id IN (9169001,9169002) → 0
--   迁移版本号：落地前实测该目录最大为 V11.418.0__Seed_Api_Test_Send_Permission.sql，故取 V11.419.0。
--
-- 【幂等】三张表均**无 (业务列) 唯一约束**，因此：
--   菜单按主键 `ON CONFLICT (id) DO NOTHING`（sys_menu 有 PK sys_menu_pkey，可作冲突目标）；
--   权限码按 `permission_code` 做 NOT EXISTS 守卫；授权按 (role_id, permission_id) 判重。
--   可重复执行；不新增表 / 不新增列 / 不改动既有行。
--
-- 【不做的事（刻意留空，勿擅自补）】
--   · 不写 sys_role_menu / sys_tenant_menu 授权行 —— 与既有 30 个 system-admin 菜单口径一致
--     （实测 sys_role_menu 中仅有 6130701 一行；超管经 SysMenuServiceImpl.getAllMenusForSystemAdmin()
--     的 `client_type IN ('tenant-admin','system-admin')` 分支全量可见，不依赖角色授权）。
--   · 例外：新增的 2 个权限码仍授权给超管角色 role_id=1，口径同 V11.394.0 / V11.407.0 / V11.417.0
--     —— 只为让「角色-权限」清单完整可审计（超管另有 `*` 通配）。
-- =============================================================================

-- ── ① 菜单行：61307 系统管理 下新增 3 个子菜单（sort 200/300/400 接在 6130701 的 100 之后） ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, display_mode, menu_level)
VALUES
    (6130702, 0, 61307, 0, now(), now(),
     '数据字典', 'system:dict', 1, 'system/dict/index', 'views/system/dict/index.vue', '', '',
     'BookOutlined', 200, 0, 1, 1, 1, 'system-admin',
     '', '', 0, 0, 1),
    (6130703, 0, 61307, 0, now(), now(),
     '系统配置', 'system:config', 1, 'system/config/index', 'views/system/config/index.vue', '', '',
     'SettingOutlined', 300, 0, 1, 1, 1, 'system-admin',
     '', '', 0, 0, 1),
    (6130704, 0, 61307, 0, now(), now(),
     '权限配置', 'system:permission', 1, 'system/permission/index', 'views/system/permission/index.vue', '', '',
     'SafetyOutlined', 400, 0, 1, 1, 1, 'system-admin',
     '', '', 0, 0, 1)
ON CONFLICT (id) DO NOTHING;

-- ── ② 权限码：补齐「系统配置」页引用但库中缺失的 2 个码 ────────────────────────────
--   来源：`views/system/config/index.vue:31` 用 `system:config:query`、`:138` 用 `system:config:create`，
--   而 devdb 实测 `sys_permission` 中两者均 0 行（README §3.4 第 1/2 条）→ 非超管用户这两个按钮
--   会被 v-permission 直接隐藏（点不动）。注意后端 `SystemConfigController` 的读端点要求的是
--   `system:config:list`（已存在）、写端点要求 `system:config:update`（已存在），
--   故本迁移只补**前端实际引用**的码，不触碰后端已有码的语义。
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 91601, 1, 0, 0, now(), now(), '系统配置查看', 'system:config:query',  3, NULL, NULL, 940, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:config:query')
UNION ALL
SELECT 91602, 1, 0, 0, now(), now(), '系统配置新增', 'system:config:create', 3, NULL, NULL, 941, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'system:config:create');

-- ── ③ 授权给超级管理员角色（role_id = 1），口径同 V11.394.0 / V11.407.0 / V11.417.0 ──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9169000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('system:config:query', 'system:config:create')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
