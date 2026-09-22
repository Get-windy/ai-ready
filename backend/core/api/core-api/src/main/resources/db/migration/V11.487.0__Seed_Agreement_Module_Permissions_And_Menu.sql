-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 阶段 A 落地四件套（模块名册 / 前缀映射 / 权限码族 / 菜单）（2026-09-22）
--
-- 【为什么四件必须同批落库】本仓铁律（DOMAIN-MODEL §12.4 第 4 条）：
--   注解里引用的权限码必须**已在库中**，否则除超管外所有账号运行时一律 **403** ——
--   不报编译错、不报启动错，只有在真机上点才会发现。漏任一件的已知症状：
--     · 缺「模块名册」⇒ "给某租户开协议模块"在数据上无从表达；
--     · 缺「前缀映射」⇒ 模块门认不出这个码族（entitlement 判定落空）；
--     · 缺「权限码」  ⇒ 非超管全 403；
--     · 缺「菜单」    ⇒ 页面进不去（菜单 tenant_id 写错 ⇒ 整页 404，本仓实踩过）。
--
-- 【裁定⑤：平台协议写权限隔离并映射到「系统」】
--   `agreement:`          → 模块 `agreement`（**租户级**，租户可开关）
--   `agreement:platform:` → 模块 `system`  （平台级，按 V11.455.0 只开给系统租户）
--   归属口径 = **最长前缀优先、同长取 sort 小**（ModuleEntitlementService#loadRules 与
--   tools/verify-module-mapping.cjs 同口径）⇒ `agreement:platform:term-option:manage`
--   落到 `system` 而不是 `agreement`。本文件第 6 节在**真库上**断言了这一点。
--   连带结论：平台协议的字典维护**天然只有平台侧能用**，不必另造机制（裁定⑤原话）。
--
-- 【id 号段 —— 实测而非猜】权限码 **111121~111130**、角色关联 **9610102~9610111**：
--   · 该域（core-api）槽位是 slot=11（权限码 111000 起、角色关联 9610000 起，见 V11.464.0）。
--   · 实测（本机 devdb，2026-09-22）：
--       SELECT id FROM sys_permission       WHERE id BETWEEN 111120 AND 111200 → 仅 111120
--       SELECT id FROM sys_role_permission  WHERE id BETWEEN 9610090 AND 9610200 → 0 行
--     ⇒ 取 111121~111130 与 9610102~9610111：紧邻同域既有码、不与任何已用 id 相撞。
--   · sys_permission.id **无序列默认值**、permission_code **无唯一约束**
--     ⇒ 必须显式给 id，幂等靠 WHERE NOT EXISTS（不能靠数据库约束）。
--
-- 【菜单挂载点（2026-09-22 用户裁定）】**不新建一级菜单**，而是按"租户级 / 平台级"**分挂两棵树**：
--   · 租户级协议 → 「设置」（id **60012**, client_type='tenant-admin'）→ 新增二级分组「协议契约」
--   · 平台级协议 → 「系统」（id **60013**, client_type='system-admin'）→ 新增二级分组「协议契约」
--   ⚠️ 这两条不是重复挂载：`SysMenuServiceImpl#getAllMenusForSystemAdmin` 查的是
--      `client_type IN ('tenant-admin','system-admin')` ⇒ **超管同时看到两棵树；普通租户只看 tenant-admin 那棵**。
--      即 `client_type` 本身就是"平台 / 租户"的分隔机制，比单靠 `agreement:platform:*` 权限码更硬
--      —— 两道**独立**的门（树的分流 + 码的归属）。
--   与「协议是独立 Maven 模块」不矛盾：**模块（权益/代码边界）≠ 菜单挂载位置（导航边界）**。
--
-- 【菜单 id 号段】四个号全部**实测为空**，且各自落在既有的分层号段里：
--   · 61208 —— 设置树分组段 612xx（现有 max 61207）；
--   · 81016 —— 设置树叶子段 81xxx（现有 max 81015）；
--   · 61308 —— 系统树分组段 613xx（现有 max 61307）；
--   · 62506 / 62507 —— 系统树叶子段 62xxx（现有 max 62505）。
--
-- 【sys_menu 字段口径】逐列对照「设置 → 打印管理（61205）」及其叶子（80930 / 81000~81003）实测值：
--   menu_type 目录=0、叶子=1 / visible=1 / status=1 / deleted=0 / is_external=0 / is_cache=1
--   display_group=0 / display_mode=0 / menu_level=0
--   ⚠️ tenant_id 必须是 **0** —— 写错 ⇒ 不进菜单树 ⇒ 整页 404（本仓实踩过）。
--   ⚠️ 目录层（menu_type=0）的 `path` 必须是 **NULL**：前端 transformMenuToRoutes 以
--      `parentPath` 拼接，真实 URL 由**叶子**的 path 决定（既有目录层 path 全为 NULL）。
--   ⚠️ `menu_level` 必须是 **0**：普通租户会话下 SysMenuServiceImpl 只返回 menu_level=0 的菜单。
--   ⚠️ 目录层 menu_code 的命名**随树而不同**：「设置」树是 `mega:set:*`、「系统」树是 `mega:sys:*`
--      （**不是** `mega:system:*` —— 实测 61301~61307 全部是 `mega:sys:` 前缀）。
--
-- 【幂等】全部 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 模块名册：sys_module 增「协议」（租户级，sort_order 接现有最大值 13 之后 = 14）──
-- ⚠️ **id 必须显式给**：实测 `sys_module_id_seq` 与数据**错位** ——
--    `SELECT nextval('sys_module_id_seq')` 返回 13，而 id=13（系统）已存在 ⇒ 依赖序列默认值
--    插入会直接撞 `sys_module_pkey`（本次落地实测踩到）。取 max(id)+1 才是安全的。
INSERT INTO sys_module (id, module_name, module_code, version, description, status, icon,
                        sort_order, tenant_id, create_time, update_time, deleted)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_module),
       '协议', 'agreement', '1.0.0',
       '协议：平台↔租户（服务/入驻）、租户↔租户（代销/购销框架）、租户↔消费者（单方承诺）。'
       '含条款字典、版本快照、双方确认（双签）。平台协议写权限用 agreement:platform:* 隔离，归「系统」模块。',
       1, 'file-text', 14, 0, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_module WHERE module_code = 'agreement');

-- 顺手把错位的序列对齐到当前最大值（幂等）：不修的话，任何"按序列插入 sys_module"的代码
-- 都会继续撞主键；对齐后 nextval 才真正可用。
SELECT setval('sys_module_id_seq', (SELECT MAX(id) FROM sys_module));

-- ── 2. 存量租户不降级：给已有开通记录的租户一并补开「协议」 ──
-- 「协议」是**租户级**模块（裁定⑤），业务租户与系统租户都用得上（平台协议要平台侧发起）。
-- 口径同 V11.454.0 的"现状不降级"回填：只对**当前已有模块开通记录**的租户补行，
-- 不给从未开通过任何模块的租户凭空造开通记录。
INSERT INTO sys_tenant_module (id, tenant_id, deleted, create_time, update_time,
                               module_code, module_name, purchase_type, status)
SELECT (SELECT COALESCE(max(id), 9000000000000000000) FROM sys_tenant_module)
           + row_number() OVER (ORDER BY t.tenant_id),
       t.tenant_id, 0, now(), now(), 'agreement', '协议', 'permanent', 0
FROM (SELECT DISTINCT tenant_id FROM sys_tenant_module WHERE deleted = 0) t
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_module m
                  WHERE m.tenant_id = t.tenant_id AND m.module_code = 'agreement' AND m.deleted = 0);

-- ── 3. 「模块 → 权限码前缀」映射（裁定⑤：两条，长短不同，最长优先）──
INSERT INTO sys_module_permission (id, tenant_id, deleted, module_code, permission_prefix,
                                   sort, remark, create_time, update_time)
SELECT v.id, 0, 0, v.module, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
 (52, 'agreement', 'agreement:',          170, '协议模块（租户级）：主档/版本/条款/双签 + 租户侧字典查看'),
 (53, 'system',    'agreement:platform:', 171, '平台协议写权限：条款字典维护等。最长前缀优先 ⇒ 该前缀落到「系统」（只开给系统租户，见 V11.455.0）')
) AS v(id, module, prefix, sort, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_module_permission p
                  WHERE p.permission_prefix = v.prefix AND p.deleted = 0);

-- ── 4. 权限码 10 条（不多不少）+ 显式授予超管角色 ──
-- 逐列对齐同域既有码口径：tenant_id=0 / parent_id=0 / deleted=0 / permission_type=3 / visible=1 / status=0
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (111121, '协议列表查询',       'agreement:list',                        '/api/agreement/page',                        'GET',    1300),
 (111122, '协议详情查看',       'agreement:view',                        '/api/agreement/{id}',                        'GET',    1301),
 (111123, '协议新建',           'agreement:create',                      '/api/agreement',                             'POST',   1302),
 (111124, '协议草稿编辑',       'agreement:update',                      '/api/agreement/{id}',                        'PUT',    1303),
 (111125, '协议草稿删除',       'agreement:delete',                      '/api/agreement/{id}',                        'DELETE', 1304),
 (111126, '协议发起变更',       'agreement:version:create',              '/api/agreement/{id}/versions',               'POST',   1305),
 (111127, '协议本方确认（双签）', 'agreement:version:confirm',             '/api/agreement/version/{versionId}/confirm', 'POST',   1306),
 (111128, '协议置为生效',       'agreement:version:activate',            '/api/agreement/version/{versionId}/activate','POST',   1307),
 (111129, '条款字典查看',       'agreement:term-option:list',            '/api/agreement/term-options/grouped',        'GET',    1308),
 (111130, '平台条款字典维护',   'agreement:platform:term-option:manage', '/api/agreement/term-options',                'POST',   1309)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 必须关联超管角色（role_id = 1 = SUPER_ADMIN / tenant_id = 1），否则平台管理员自己运行时也会被拒。
-- JOIN 用 MIN(id) 取每个码的主记录：permission_code 无唯一约束，历史重复码会让同一 rp_id 产出多行而撞主键。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT v.rp_id, 1, p.id, 1, now()
FROM (VALUES
 (9610102, 'agreement:list'),
 (9610103, 'agreement:view'),
 (9610104, 'agreement:create'),
 (9610105, 'agreement:update'),
 (9610106, 'agreement:delete'),
 (9610107, 'agreement:version:create'),
 (9610108, 'agreement:version:confirm'),
 (9610109, 'agreement:version:activate'),
 (9610110, 'agreement:term-option:list'),
 (9610111, 'agreement:platform:term-option:manage')
) AS v(rp_id, code)
JOIN (SELECT permission_code, MIN(id) AS id FROM sys_permission GROUP BY permission_code) p
  ON p.permission_code = v.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- ── 5. 菜单：分挂两棵树（不新建一级菜单）──
--   设置(60012, tenant-admin) → 协议契约 → 协议列表
--   系统(60013, system-admin) → 协议契约 → 平台协议 / 条款字典维护
-- 每个分组下都有叶子：MegaMenuPanel 的 `visibleColumns = 子项.filter(col => col.children?.length)`
-- —— **只有带子项的层级才成列**，空分组会渲染成空列/点不到。
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, link_icon, display_mode, list_path,
                      tag_label, menu_level)
SELECT v.id, 0, v.parent_id, 0, now(), now(),
       v.menu_name, v.menu_code, v.menu_type, v.path, v.component, NULL, NULL,
       NULL, v.sort, 0, 1, 1, 1, v.client_type,
       v.remark, NULL, 0, NULL, 0, NULL,
       NULL, 0
FROM (VALUES
 (61208, 60012, '协议契约', 'mega:set:agreement', 0, NULL, NULL, 620, 'tenant-admin',
  '「设置」下的二级分组（租户级协议：租户↔租户 / 租户↔消费者）。目录层 path 必须 NULL，真实 URL 由叶子决定。'),
 (81016, 61208, '协议列表', 'agreement', 1, 'agreement', 'views/agreement/index.vue', 1, 'tenant-admin',
  '租户级协议列表。menu_code=agreement ⇒ 持有任一 agreement:* 码的角色可见（派生口径，见 MenuPermissionDeriver）。'),
 (61308, 60013, '协议契约', 'mega:sys:agreement', 0, NULL, NULL, 800, 'system-admin',
  '「系统」下的二级分组（平台级协议：平台↔租户）。client_type=system-admin ⇒ 只有超管能看到这棵树，普通租户的菜单树里根本没有它。'),
 (62506, 61308, '平台协议', 'agreement:view', 1, 'agreement/platform', 'views/agreement/index.vue', 100, 'system-admin',
  '平台↔租户协议列表。复用协议列表页（同组件两条路径），页面按路由默认过滤 agreementType=PLATFORM_SERVICE。'),
 (62507, 61308, '条款字典维护', 'agreement:platform:term-option', 1, 'agreement/term-option/index', 'views/agreement/term-option/index.vue', 200, 'system-admin',
  '平台侧条款字典维护（平台只定义选项、不设默认值）。路径加 /index 是为避开前端常驻隐藏路由 /agreement/term-option（同组件，避免重复路由）。')
) AS v(id, parent_id, menu_name, menu_code, menu_type, path, component, sort, client_type, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = v.id);

-- ── 6. 自检（任一断言不成立则整个迁移回滚，不留半成品）──
DO $$
DECLARE
    n_module   int;
    n_module_row int;
    n_codes    int;
    n_dup      int;
    n_granted  int;
    n_menu     int;
    n_menu_bad int;
    v_owner    text;
BEGIN
    -- 6.1 模块名册 + 存量租户开通
    SELECT count(*) INTO n_module FROM sys_module WHERE module_code = 'agreement' AND deleted = 0;
    IF n_module <> 1 THEN
        RAISE EXCEPTION 'sys_module 应存在 1 条 agreement，实际 %', n_module;
    END IF;
    -- 每个"已有模块开通记录"的租户都必须有 agreement 一行（不降级）
    SELECT count(*) INTO n_module_row FROM (
        SELECT DISTINCT tenant_id FROM sys_tenant_module WHERE deleted = 0) t
     WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_module m
                       WHERE m.tenant_id = t.tenant_id AND m.module_code = 'agreement' AND m.deleted = 0);
    IF n_module_row <> 0 THEN
        RAISE EXCEPTION '有 % 个已开通模块的租户没有 agreement 模块行（存量降级）', n_module_row;
    END IF;

    -- 6.2 本批权限码 10 条、不重复、全部已授超管
    -- ⚠️ 断言必须**按本批 id 区间**，不能按 `permission_code LIKE 'agreement:%'` 数总数：
    --    后续批次（V11.489.0）还会往同一码族里加码，按前缀计数会让本文件在"单独重放"时
    --    看到 20 条而误判失败、把整个迁移回滚。V11.489.0 用的是同一口径。
    SELECT count(*) INTO n_codes FROM sys_permission
     WHERE deleted = 0 AND id BETWEEN 111121 AND 111130;
    IF n_codes <> 10 THEN
        RAISE EXCEPTION '本批权限码应为 10 条（id 111121~111130），实际 % 条', n_codes;
    END IF;
    SELECT count(*) INTO n_dup FROM (
        SELECT permission_code FROM sys_permission WHERE id BETWEEN 111121 AND 111130
         GROUP BY permission_code HAVING count(*) > 1) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION '本批权限码存在重复行（permission_code 无唯一约束，需人工清理）';
    END IF;
    SELECT count(*) INTO n_granted FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE rp.role_id = 1 AND p.id BETWEEN 111121 AND 111130;
    IF n_granted <> 10 THEN
        RAISE EXCEPTION '本批权限码应已授予超管 10 条，实际 % 条', n_granted;
    END IF;

    -- 6.3 前缀归属（裁定⑤核心）：**真库上按最长前缀优先实测**，不采信注释
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:platform:term-option:manage' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'system' THEN
        RAISE EXCEPTION 'agreement:platform:* 应归属 system，实测归属 %（须为最长前缀优先）', v_owner;
    END IF;
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:list' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'agreement' THEN
        RAISE EXCEPTION 'agreement:* 应归属 agreement，实测归属 %', v_owner;
    END IF;

    -- 6.4 菜单 5 行；字段口径；**分挂在正确的树上**；父节点必须真实存在且挂点正确
    SELECT count(*) INTO n_menu FROM sys_menu
     WHERE id IN (61208, 81016, 61308, 62506, 62507) AND deleted = 0;
    IF n_menu <> 5 THEN
        RAISE EXCEPTION '协议菜单应有 5 行，实际 % 行', n_menu;
    END IF;
    SELECT count(*) INTO n_menu_bad FROM sys_menu
     WHERE id IN (61208, 81016, 61308, 62506, 62507)
       AND (tenant_id <> 0 OR menu_level <> 0 OR status <> 1 OR visible <> 1
            OR (menu_type = 1 AND (path IS NULL OR component IS NULL))
            OR (menu_type = 0 AND path IS NOT NULL));
    IF n_menu_bad > 0 THEN
        RAISE EXCEPTION '协议菜单有 % 行的字段口径不符（tenant_id/menu_level/path/component 必须对齐样板）', n_menu_bad;
    END IF;
    -- 分树断言：挂错树 = 要么租户看不到自己要用的页，要么平台页泄给租户
    SELECT count(*) INTO n_menu_bad FROM sys_menu
     WHERE id IN (61208, 81016, 61308, 62506, 62507)
       AND client_type <> (CASE WHEN id IN (61208, 81016) THEN 'tenant-admin' ELSE 'system-admin' END);
    IF n_menu_bad > 0 THEN
        RAISE EXCEPTION '协议菜单有 % 行挂错客户端类型（设置树必须 tenant-admin、系统树必须 system-admin）', n_menu_bad;
    END IF;
    -- 父节点必须存在（否则成孤儿菜单，不进菜单树 —— 本仓实踩过）
    SELECT count(*) INTO n_menu_bad FROM sys_menu c
     WHERE c.id IN (61208, 81016, 61308, 62506, 62507)
       AND NOT EXISTS (SELECT 1 FROM sys_menu p WHERE p.id = c.parent_id AND p.deleted = 0);
    IF n_menu_bad > 0 THEN
        RAISE EXCEPTION '协议菜单有 % 行的父节点不存在（会成孤儿菜单）', n_menu_bad;
    END IF;
    -- 两个挂点必须真的是「设置」(60012) / 「系统」(60013)，且叶子挂在各自分组下
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 61208 AND parent_id = 60012 AND deleted = 0) THEN
        RAISE EXCEPTION '「设置 → 协议契约」必须挂在 60012（设置）下';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 61308 AND parent_id = 60013 AND deleted = 0) THEN
        RAISE EXCEPTION '「系统 → 协议契约」必须挂在 60013（系统）下';
    END IF;

    -- 6.5 号段（V11.486.0）仍在，避免本文件被单独重放后编号能力丢失
    IF NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'AGREEMENT') THEN
        RAISE EXCEPTION 'biz_number_sequence 缺少 AGREEMENT 号段配置';
    END IF;

    RAISE NOTICE 'V11.487.0 自检通过：模块名册 + 前缀映射（最长前缀已实测）+ 10 个权限码（已授超管）+ 4 行菜单';
END $$;
