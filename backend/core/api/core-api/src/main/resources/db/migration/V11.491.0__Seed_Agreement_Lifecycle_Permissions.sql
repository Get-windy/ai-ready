-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 成立过程与终止的权限码（2026-09-22）
--
-- 【为什么本文件与 V11.490.0 必须成对出现】本仓铁律（DOMAIN-MODEL §12.4 第 4 条）：
--   注解里引用的权限码必须**已在库中存在**，否则除超管外所有账号运行时一律 **403** ——
--   不报编译错、不报启动错，只有在真机上点才会发现。
--   本文件给「唯一送达 / 协商反要约 / 签署 / 终止」的 6 个写端点补码，
--   并**同批**补 `sys_module_permission` 前缀映射、授超管，三件缺一件就等于没落地。
--
-- 【本批 6 条码（可增不可乱造）】
--   agreement:invite:create      发起唯一送达（生成邀请 token 与短链/邀请码）
--   agreement:invite:revoke      撤回邀请
--   agreement:invite:accept      受邀方领取并查看
--   agreement:sign               签署（自然人代表主体，比"确认"更强的一步）
--   agreement:terminate:request  发起终止
--   agreement:terminate:confirm  对方确认终止 / 提异议
--   其余读端点**复用**既有 `agreement:view`（少造码 = 少一处漏授；读的口径本来就一样）。
--
-- 【前缀归属沿用裁定⑤】`agreement:` → 模块 `agreement`（租户级，租户可开关）。
--   最长前缀优先口径见 `ModuleEntitlementService#loadRules`；
--   本批没有新增 `agreement:platform:` 码，因此平台码仍只有 V11.487.0/V11.489.0 那三条。
--
-- 【id 号段 —— 实测而非猜】权限码 **111141~111146**、角色关联 **9610122~9610127**：
--   · 该域（core-api）槽位是 slot=11（权限码 111000 起，见 V11.464.0）；
--   · V11.487.0 已占 111121~111130、V11.489.0 已占 111131~111140 ⇒ **从 111141 起**，
--     与那两批错开，不依赖"谁先谁后"，也不会撞车。
--   · 实测（本机 devdb，2026-09-22）：
--       SELECT id FROM sys_permission      WHERE id BETWEEN 111141 AND 111160 → 0 行
--       SELECT id FROM sys_role_permission WHERE id BETWEEN 9610122 AND 9610140 → 0 行
--       （注意：`sys_role_permission` 现存 1866 行的 id 是雪花值，961xxxx 段整体空闲，
--         这也是 V11.487.0/V11.489.0 当初选 9610xxx 的同一口径。）
--     ⇒ 取 111141~111146 / 9610122~9610127：紧邻同域既有码（111140 = agreement:platform:template:read），
--       不与任何已用 id 相撞。文件开头的 DO $$ 会**先**再断言一次空闲，撞了就整体回滚。
--   · sys_permission.id **无序列默认值**、permission_code **无唯一约束**
--     ⇒ 必须显式给 id，幂等靠 WHERE NOT EXISTS（不能靠数据库约束）。
--
-- 【⚠️ 自检的计数断言必须**按本批 id 区间**】不能按 `permission_code LIKE 'agreement:%'` 数总数：
--   后续批次还会往同一码族加码，按前缀计数会让本文件在"单独重放"时误判并回滚
--   —— 这个坑已在 V11.487.0 上踩过并修正，V11.489.0 与本文用同一口径（id BETWEEN）。
--
-- 【本期只补码，不补菜单】邀请、签署、终止、版本 diff 都住在既有「协议列表 / 协议详情」页里，
--   页面入口由协议详情页的按钮承担，**不新增 sys_menu 行**（菜单由用户统一处理）。
--
-- 【幂等】全部 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 0. 前置自检：这 12 个 id 号段必须没有被**别人**占（本批自己的行不算冲突，保证可重复执行）──
DO $$
DECLARE
    v_used text;
    v_rp   text;
BEGIN
    SELECT string_agg(id || '=' || permission_code, ', ') INTO v_used
      FROM sys_permission
     WHERE id BETWEEN 111141 AND 111146
       AND permission_code NOT IN (
           'agreement:invite:create', 'agreement:invite:revoke', 'agreement:invite:accept',
           'agreement:sign', 'agreement:terminate:request', 'agreement:terminate:confirm');
    IF v_used IS NOT NULL THEN
        RAISE EXCEPTION '权限码 id 号段 111141~111146 已被别的码占用（%），请改用实测空闲号段', v_used;
    END IF;

    SELECT string_agg(rp.id || '=role' || rp.role_id || '/perm' || rp.permission_id, ', ') INTO v_rp
      FROM sys_role_permission rp
     WHERE rp.id BETWEEN 9610122 AND 9610127
       AND (rp.role_id <> 1 OR rp.permission_id NOT IN (
               SELECT p.id FROM sys_permission p WHERE p.permission_code IN (
                   'agreement:invite:create', 'agreement:invite:revoke', 'agreement:invite:accept',
                   'agreement:sign', 'agreement:terminate:request', 'agreement:terminate:confirm')));
    IF v_rp IS NOT NULL THEN
        RAISE EXCEPTION '角色关联 id 号段 9610122~9610127 已被别的关联占用（%），请改用实测空闲号段', v_rp;
    END IF;
END $$;

-- ── 1. 「模块 → 权限码前缀」映射（幂等：V11.487.0 已建则本段为空操作）──
-- 为什么在这里再写一遍：本文件的自检要在**真库上**验证"码族归协议模块"，
-- 若 487.0 因故未落地，本文件单独重放时也应当把前缀补上而不是直接报错。
INSERT INTO sys_module_permission (id, tenant_id, deleted, module_code, permission_prefix,
                                   sort, remark, create_time, update_time)
SELECT v.id, 0, 0, v.module, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
 (52, 'agreement', 'agreement:',          170, '协议模块（租户级）：主档/版本/条款/双签 + 内容层 + 模板 + 唯一送达/签署/终止'),
 (53, 'system',    'agreement:platform:', 171, '平台协议写权限（含平台模板管理与合规抽查读）。最长前缀优先 ⇒ 落到「系统」')
) AS v(id, module, prefix, sort, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_module_permission p
                  WHERE p.permission_prefix = v.prefix AND p.deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_module_permission p2 WHERE p2.id = v.id);

-- ── 2. 权限码 6 条（不多不少）──
-- 逐列对齐同域既有码口径：tenant_id=0 / parent_id=0 / deleted=0 / permission_type=3 / visible=1 / status=0
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (111141, '协议发起唯一送达',   'agreement:invite:create',      '/api/agreement/lifecycle/{agreementId}/invites',       'POST', 1500),
 (111142, '协议撤回邀请',       'agreement:invite:revoke',      '/api/agreement/lifecycle/invites/{inviteId}/revoke',   'POST', 1501),
 (111143, '协议受邀方领取',     'agreement:invite:accept',      '/api/agreement/lifecycle/invites/open',                'POST', 1502),
 (111144, '协议签署',           'agreement:sign',               '/api/agreement/lifecycle/version/{versionId}/sign',    'POST', 1503),
 (111145, '协议发起终止',       'agreement:terminate:request',  '/api/agreement/lifecycle/{agreementId}/terminations',  'POST', 1504),
 (111146, '协议终止确认与异议', 'agreement:terminate:confirm',  '/api/agreement/lifecycle/terminations/{id}/confirm',   'POST', 1505)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- ── 3. 授予超管角色（role_id = 1 = SUPER_ADMIN / tenant_id = 1）──
-- 不授的话平台管理员自己运行时也会被拒（本仓对"注解有码、角色无码"的处置纪律）。
-- JOIN 用 MIN(id) 取每个码的主记录：permission_code 无唯一约束，历史重复码会让同一 rp_id 产出多行而撞主键。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT v.rp_id, 1, p.id, 1, now()
FROM (VALUES
 (9610122, 'agreement:invite:create'),
 (9610123, 'agreement:invite:revoke'),
 (9610124, 'agreement:invite:accept'),
 (9610125, 'agreement:sign'),
 (9610126, 'agreement:terminate:request'),
 (9610127, 'agreement:terminate:confirm')
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
    n_tables   int;
BEGIN
    -- 4.1 六条码全部在库里（**注解引用的码必须已存在**，否则非超管全 403）
    SELECT count(*) INTO n_missing FROM (VALUES
        ('agreement:invite:create'), ('agreement:invite:revoke'), ('agreement:invite:accept'),
        ('agreement:sign'), ('agreement:terminate:request'), ('agreement:terminate:confirm')
    ) AS e(code)
     WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = e.code AND p.deleted = 0);
    IF n_missing <> 0 THEN
        RAISE EXCEPTION '成立过程/终止有 % 个权限码未落库', n_missing;
    END IF;

    -- 4.2 本批 6 条各自唯一（permission_code 无唯一约束，重复行会让鉴权行为不可预期）
    SELECT count(*) INTO n_dup FROM (
        SELECT permission_code FROM sys_permission
         WHERE permission_code IN ('agreement:invite:create','agreement:invite:revoke','agreement:invite:accept',
                                   'agreement:sign','agreement:terminate:request','agreement:terminate:confirm')
         GROUP BY permission_code HAVING count(*) > 1) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION '本批有 % 个权限码存在重复行（permission_code 无唯一约束，需人工清理）', n_dup;
    END IF;
    -- ⚠️ 计数**按本批 id 区间**，不按 permission_code LIKE 'agreement:%'（后续批次还会加码，见文件头）
    SELECT count(*) INTO n_codes FROM sys_permission
     WHERE id BETWEEN 111141 AND 111146 AND deleted = 0;
    IF n_codes <> 6 THEN
        RAISE EXCEPTION '本批权限码应为 6 条（id 111141~111146），实际 % 条', n_codes;
    END IF;

    -- 4.3 全部已授超管（漏授 = 平台管理员自己点不动）
    SELECT count(*) INTO n_granted FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE rp.role_id = 1 AND p.id BETWEEN 111141 AND 111146;
    IF n_granted <> 6 THEN
        RAISE EXCEPTION '本批权限码应已授予超管 6 条，实际 % 条', n_granted;
    END IF;

    -- 4.4 前缀归属（裁定⑤核心）：**真库上按最长前缀优先实测**，不采信注释
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:terminate:request' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'agreement' THEN
        RAISE EXCEPTION 'agreement:terminate:request 应归属 agreement（租户级模块），实测归属 %', v_owner;
    END IF;
    -- 平台码的归属**不许**被本批带歪
    SELECT p.module_code INTO v_owner FROM sys_module_permission p
     WHERE p.deleted = 0 AND 'agreement:platform:template:manage' LIKE p.permission_prefix || '%'
     ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'system' THEN
        RAISE EXCEPTION 'agreement:platform:template:manage 应归属 system，实测归属 %（最长前缀优先被破坏了）', v_owner;
    END IF;

    -- 4.5 前置表仍在（V11.490.0 建的三张表），避免本文件被单独重放后码与表对不上
    SELECT count(*) INTO n_tables FROM information_schema.tables
     WHERE table_schema = 'public'
       AND table_name IN ('agreement_invite', 'agreement_signature', 'agreement_termination');
    IF n_tables <> 3 THEN
        RAISE EXCEPTION '成立过程/终止的三张表不齐（实际 % 张）—— 码与表必须同批落地', n_tables;
    END IF;

    RAISE NOTICE 'V11.491.0 自检通过：6 个权限码（已授超管）+ 前缀映射（最长前缀已实测：租户码归 agreement、平台码仍归 system）';
END $$;
