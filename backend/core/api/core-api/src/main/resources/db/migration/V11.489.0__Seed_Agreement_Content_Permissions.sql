-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 内容层权限码（2026-09-22）
--
-- 【为什么本文件与 V11.488.0 必须成对出现】本仓铁律（DOMAIN-MODEL §12.4 第 4 条）：
--   注解里引用的权限码必须**已在库中存在**，否则除超管外所有账号运行时一律 **403** ——
--   不报编译错、不报启动错，只有在真机上点才会发现。
--   本文件给内容层 8 个端点 + 模板 6 个端点补码，并**同批**补
--   `sys_module_permission` 前缀映射、授超管，三件缺一件就等于没落地。
--
-- 【裁定⑤ 的继续：平台侧码归「系统」模块】
--   `agreement:`          → 模块 `agreement`（**租户级**，租户可开关）
--   `agreement:platform:` → 模块 `system`  （平台级，按 V11.455.0 只开给系统租户）
--   归属口径 = **最长前缀优先、同长取 sort 小**（ModuleEntitlementService#loadRules
--   与 tools/verify-module-mapping.cjs 同口径）⇒ 本文件新增的
--   `agreement:platform:template:manage` / `agreement:platform:template:read`
--   落到 `system` 而不是 `agreement`。本文件末尾在**真库上**断言了这一点。
--   连带结论：**平台合规抽查读**（用户 2026-09-22 明确要求的能力）不必新造机制，
--   靠码的归属天然只有平台侧拿得到（§13.9）。
--
-- 【id 号段 —— 实测而非猜】权限码 **111131~111140**、角色关联 **9610112~9610121**：
--   · 该域（core-api）槽位是 slot=11（权限码 111000 起，见 V11.464.0）；
--   · V11.487.0 已占 111121~111130 与 9610102~9610111（协议地基的 10 条码）⇒ **从 111131 起**，
--     与那批错开，不依赖"谁先谁后"，也不会撞车。
--   · 实测（本机 devdb，2026-09-22）：
--       SELECT id FROM sys_permission      WHERE id BETWEEN 111121 AND 111200 → 0 行
--       SELECT id FROM sys_role_permission WHERE id BETWEEN 9610100 AND 9610200 → 0 行
--     ⇒ 取 111131~111140 / 9610112~9610121：紧邻同域既有码（111120 = system:tenant-module:remove）、
--       不与任何已用 id 相撞。文件开头的 DO $$ 会**先**再断言一次空闲，撞了就整体回滚。
--   · sys_permission.id **无序列默认值**、permission_code **无唯一约束**
--     ⇒ 必须显式给 id，幂等靠 WHERE NOT EXISTS（不能靠数据库约束）。
--
-- 【本期只补码，不补菜单】内容层（设定/文字/履约方式）住在既有「协议列表」页里，
--   模板管理页的菜单挂载随前端任务一起做（§12.3 的菜单挂载点不变：
--   租户级挂「设置 → 协议契约」、平台级挂「系统 → 协议契约」）。
--
-- 【幂等】全部 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 0. 前置自检：这 20 个 id 号段必须没有被**别人**占（本批自己的行不算冲突，保证可重复执行）──
DO $$
DECLARE
    v_used text;
    v_rp   text;
BEGIN
    SELECT string_agg(id || '=' || permission_code, ', ') INTO v_used
      FROM sys_permission
     WHERE id BETWEEN 111131 AND 111140
       AND permission_code NOT IN (
           'agreement:content:view', 'agreement:content:edit', 'agreement:setting-def:list',
           'agreement:template:list', 'agreement:template:create', 'agreement:template:update',
           'agreement:template:delete', 'agreement:template:apply',
           'agreement:platform:template:manage', 'agreement:platform:template:read');
    IF v_used IS NOT NULL THEN
        RAISE EXCEPTION '权限码 id 号段 111131~111140 已被别的码占用（%），请改用实测空闲号段', v_used;
    END IF;

    SELECT string_agg(rp.id || '=role' || rp.role_id || '/perm' || rp.permission_id, ', ') INTO v_rp
      FROM sys_role_permission rp
     WHERE rp.id BETWEEN 9610112 AND 9610121
       AND (rp.role_id <> 1 OR rp.permission_id NOT IN (
               SELECT p.id FROM sys_permission p WHERE p.permission_code IN (
                   'agreement:content:view', 'agreement:content:edit', 'agreement:setting-def:list',
                   'agreement:template:list', 'agreement:template:create', 'agreement:template:update',
                   'agreement:template:delete', 'agreement:template:apply',
                   'agreement:platform:template:manage', 'agreement:platform:template:read')));
    IF v_rp IS NOT NULL THEN
        RAISE EXCEPTION '角色关联 id 号段 9610112~9610121 已被别的关联占用（%），请改用实测空闲号段', v_rp;
    END IF;
END $$;

-- ── 1. 「模块 → 权限码前缀」映射（幂等：V11.487.0 已建则本段为空操作）──
-- 为什么在这里再写一遍：本文件的自检要在**真库上**验证"平台码归系统模块"，
-- 若 487.0 因故未落地，本文件单独重放时也应当把前缀补上而不是直接报错。
INSERT INTO sys_module_permission (id, tenant_id, deleted, module_code, permission_prefix,
                                   sort, remark, create_time, update_time)
SELECT v.id, 0, 0, v.module, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
 (52, 'agreement', 'agreement:',          170, '协议模块（租户级）：主档/版本/条款/双签 + 内容层 + 租户侧模板'),
 (53, 'system',    'agreement:platform:', 171, '平台协议写权限（含平台模板管理与合规抽查读）。最长前缀优先 ⇒ 落到「系统」')
) AS v(id, module, prefix, sort, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_module_permission p
                  WHERE p.permission_prefix = v.prefix AND p.deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_module_permission p2 WHERE p2.id = v.id);

-- ── 2. 权限码 10 条（不多不少）──
-- 逐列对齐同域既有码口径：tenant_id=0 / parent_id=0 / deleted=0 / permission_type=3 / visible=1 / status=0
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (111131, '协议内容查看',       'agreement:content:view',                 '/api/agreement/content/{versionId}',   'GET',    1400),
 (111132, '协议内容编辑',       'agreement:content:edit',                 '/api/agreement/content/{versionId}',   'PUT',    1401),
 (111133, '设定字段元数据查看', 'agreement:setting-def:list',             '/api/agreement/content/setting-defs',  'GET',    1402),
 (111134, '契约模板查看',       'agreement:template:list',                '/api/agreement/templates/page',        'GET',    1403),
 (111135, '契约模板新建',       'agreement:template:create',              '/api/agreement/templates',             'POST',   1404),
 (111136, '契约模板修改',       'agreement:template:update',              '/api/agreement/templates/{id}',        'PUT',    1405),
 (111137, '契约模板删除',       'agreement:template:delete',              '/api/agreement/templates/{id}',        'DELETE', 1406),
 (111138, '从模板发起契约',     'agreement:template:apply',               '/api/agreement/templates/{id}/apply',  'POST',   1407),
 (111139, '平台模板管理',       'agreement:platform:template:manage',     '/api/agreement/templates',             'POST',   1408),
 (111140, '平台模板合规抽查读', 'agreement:platform:template:read',       '/api/agreement/templates/page',        'GET',    1409)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- ── 3. 授予超管角色（role_id = 1 = SUPER_ADMIN / tenant_id = 1）──
-- 不授的话平台管理员自己运行时也会被拒（本仓对"注解有码、角色无码"的处置纪律）。
-- JOIN 用 MIN(id) 取每个码的主记录：permission_code 无唯一约束，历史重复码会让同一 rp_id 产出多行而撞主键。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT v.rp_id, 1, p.id, 1, now()
FROM (VALUES
 (9610112, 'agreement:content:view'),
 (9610113, 'agreement:content:edit'),
 (9610114, 'agreement:setting-def:list'),
 (9610115, 'agreement:template:list'),
 (9610116, 'agreement:template:create'),
 (9610117, 'agreement:template:update'),
 (9610118, 'agreement:template:delete'),
 (9610119, 'agreement:template:apply'),
 (9610120, 'agreement:platform:template:manage'),
 (9610121, 'agreement:platform:template:read')
) AS v(rp_id, code)
JOIN (SELECT permission_code, MIN(id) AS id FROM sys_permission GROUP BY permission_code) p
  ON p.permission_code = v.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- ── 4. 自检（任一断言不成立则整个迁移回滚，不留半成品）──
DO $$
DECLARE
    n_codes    int;
    n_dup      int;
    n_granted  int;
    v_owner    text;
    n_missing  int;
BEGIN
    -- 4.1 内容层 10 个码都在库里（**注解引用的码必须已存在**，否则非超管全 403）
    SELECT count(*) INTO n_missing FROM (VALUES
        ('agreement:content:view'), ('agreement:content:edit'), ('agreement:setting-def:list'),
        ('agreement:template:list'), ('agreement:template:create'), ('agreement:template:update'),
        ('agreement:template:delete'), ('agreement:template:apply'),
        ('agreement:platform:template:manage'), ('agreement:platform:template:read')
    ) AS e(code)
     WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = e.code AND p.deleted = 0);
    IF n_missing <> 0 THEN
        RAISE EXCEPTION '内容层有 % 个权限码未落库', n_missing;
    END IF;

    -- 4.2 本批 10 个码各自唯一（permission_code 无唯一约束，重复行会让鉴权行为不可预期）
    SELECT count(*) INTO n_dup FROM (
        SELECT permission_code FROM sys_permission
         WHERE permission_code IN ('agreement:content:view','agreement:content:edit','agreement:setting-def:list',
                                   'agreement:template:list','agreement:template:create','agreement:template:update',
                                   'agreement:template:delete','agreement:template:apply',
                                   'agreement:platform:template:manage','agreement:platform:template:read')
         GROUP BY permission_code HAVING count(*) > 1) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION '本批有 % 个权限码存在重复行（permission_code 无唯一约束，需人工清理）', n_dup;
    END IF;
    SELECT count(*) INTO n_codes FROM sys_permission
     WHERE id BETWEEN 111131 AND 111140 AND deleted = 0;
    IF n_codes <> 10 THEN
        RAISE EXCEPTION '本批权限码应为 10 条（id 111131~111140），实际 % 条', n_codes;
    END IF;

    -- 4.3 全部已授超管（漏授 = 平台管理员自己点不动）
    SELECT count(*) INTO n_granted FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE rp.role_id = 1 AND p.id BETWEEN 111131 AND 111140;
    IF n_granted <> 10 THEN
        RAISE EXCEPTION '本批权限码应已授予超管 10 条，实际 % 条', n_granted;
    END IF;

    -- 4.4 前缀归属（裁定⑤核心）：**真库上按最长前缀优先实测**，不采信注释
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:platform:template:read' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'system' THEN
        RAISE EXCEPTION 'agreement:platform:template:read 应归属 system（平台合规抽查读只开给系统租户），实测归属 %', v_owner;
    END IF;
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:content:edit' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'agreement' THEN
        RAISE EXCEPTION 'agreement:content:edit 应归属 agreement（租户级模块），实测归属 %', v_owner;
    END IF;

    -- 4.5 地基那批码（V11.487.0）仍在，避免本文件被单独重放后把模块门弄丢
    IF NOT EXISTS (SELECT 1 FROM sys_module_permission
                    WHERE permission_prefix = 'agreement:' AND deleted = 0) THEN
        RAISE EXCEPTION '缺少 agreement: 前缀映射（模块门认不出这个码族）';
    END IF;

    RAISE NOTICE 'V11.489.0 自检通过：内容层 10 个权限码（已授超管）+ 前缀映射（最长前缀已实测：平台码归 system）';
END $$;
