-- ══════════════════════════════════════════════════════════════════════════════
-- 平台-MODULE-01 遗留①：租户模块「开通 / 停用」两个平台侧写端点的权限码种子
-- （2026-09-22）
--
-- 【为什么必须有这个迁移】新写的两个端点带 @SaCheckPermission
--   · POST   /api/tenant-module/assign  → system:tenant-module:assign
--   · DELETE /api/tenant-module/remove  → system:tenant-module:remove
--   本仓铁律：**注解里引用的权限码必须已在库里存在**，否则除超管（`*` 通配）外
--   所有账号在运行时一律 403 —— 不报编译错、不报启动错，只有在真机上点才会发现。
--   同一个坑在本域已经踩过一次：`TenantModuleController` 原 `/list` 标注的
--   `system:tenant:query` 在库里实测 0 行（见该控制器头部注释）。
--
-- 【id 号段 —— 实测而非猜】权限码 **111119 / 111120**，角色关联 **9610100 / 9610101**：
--   · 该域（core-api）的槽位是 slot=11（权限码 111000 起、角色关联 9610000 起，
--     见 V11.464.0 头部）。111000~111117 是本批 VALUES 的声明范围，
--     **实际落库只到 111114**（111115~111117 的码已在库、被 NOT EXISTS 跳过）。
--   · 实测（本机 devdb，2026-09-22）：
--       SELECT id FROM sys_permission WHERE id BETWEEN 111109 AND 111400 ORDER BY id
--       → 111109,111110,111111,111112,111114（其余空闲）
--       SELECT id FROM sys_role_permission WHERE id BETWEEN 9610090 AND 9610200 ORDER BY id
--       → 0 行（该段空闲）
--     ⇒ 取 111119/111120 与 9610100/9610101：既紧邻同域既有码、又不与任何已用 id 相撞。
--   · sys_permission.id **没有序列默认值**、permission_code **没有唯一约束**
--     ⇒ 必须显式给 id，且幂等靠下面的 WHERE NOT EXISTS（不能靠数据库约束）。
--
-- 【模块归属：不新增 sys_module_permission 映射行，依据如下】
--   entitlement 门的归属口径是「sys_module_permission 的 permission_prefix 最长前缀优先、
--   同长取 sort 小」（ModuleEntitlementService#loadRules 与 tools/verify-module-mapping.cjs 同口径）。
--   实测该表已有 id=26 的行：module_code='system', permission_prefix='system:', sort=130。
--   `system:tenant-module:assign` / `system:tenant-module:remove` 都以 `system:` 为前缀，
--   且不存在长度同为 7 的其它前缀能与它并列（无歧义）⇒ **已被现有映射接住，本迁移不新增映射行**。
--   语义上也自洽：这两个码是平台级「模块授权」动作，本就属于「系统」模块
--   （而「系统」模块按 V11.455.0 只开给系统租户，故只有平台侧能调用，与 assertPlatformAdmin 一致）。
--   ⇒ 改完 tools/verify-module-mapping.cjs 的核心断言（在役码 100% 有归属）仍然成立。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission（role_id = 1 = SUPER_ADMIN /
--   tenant_id = 1），不关联则平台管理员自己在运行时也会被拒。写法照 V11.459.0 / V11.464.0。
--
-- 【幂等】两段 INSERT 都是 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 权限码本体 ──
-- 逐列对齐同域既有码 system:tenant-module:view（id=111096）的口径：
-- tenant_id=0 / parent_id=0 / deleted=0 / permission_type=3 / visible=1 / status=0（0=启用）。
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (111119, '平台与系统tenant-module开通', 'system:tenant-module:assign', '/api/tenant-module/assign', 'POST',   1297),
 (111120, '平台与系统tenant-module停用', 'system:tenant-module:remove', '/api/tenant-module/remove', 'DELETE', 1298)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- ── 2. 显式授予超管角色（role_id = 1）──
-- JOIN 用 MIN(id) 取每个码的主记录：permission_code 无唯一约束，若历史上有重复码，
-- 直接 JOIN 会为同一 rp_id 产出多行而撞主键 —— 这里取最早一条，避免迁移失败。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT v.rp_id, 1, p.id, 1, now()
FROM (VALUES
 (9610100, 'system:tenant-module:assign'),
 (9610101, 'system:tenant-module:remove')
) AS v(rp_id, code)
JOIN (SELECT permission_code, MIN(id) AS id FROM sys_permission GROUP BY permission_code) p
  ON p.permission_code = v.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- ── 3. 自检（任一断言不成立则整个迁移失败回滚，不留半成品）──
DO $$
DECLARE
    n_codes     int;
    n_granted   int;
    n_prefix    int;
    n_dup       int;
BEGIN
    -- 3.1 两个码必须存在且各只有一条
    SELECT count(*) INTO n_codes FROM sys_permission
     WHERE permission_code IN ('system:tenant-module:assign', 'system:tenant-module:remove')
       AND deleted = 0;
    IF n_codes <> 2 THEN
        RAISE EXCEPTION '新权限码应存在 2 条，实际 % 条', n_codes;
    END IF;

    SELECT count(*) INTO n_dup FROM (
        SELECT permission_code FROM sys_permission
         WHERE permission_code IN ('system:tenant-module:assign', 'system:tenant-module:remove')
         GROUP BY permission_code HAVING count(*) > 1
    ) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION '权限码存在重复行（permission_code 无唯一约束，需人工清理）';
    END IF;

    -- 3.2 必须已授予超管角色（否则平台管理员自己也会 403）
    SELECT count(*) INTO n_granted
      FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE rp.role_id = 1
       AND p.permission_code IN ('system:tenant-module:assign', 'system:tenant-module:remove');
    IF n_granted <> 2 THEN
        RAISE EXCEPTION '新权限码应已授予超管角色 2 条，实际 % 条', n_granted;
    END IF;

    -- 3.3 模块归属：必须有前缀能接住这两个码（当前是 sys_module_permission 的 'system:' 行）。
    --     这里只断言「存在 length >= 7 的前缀命中」，不断言唯一命中方式，
    --     完整的长前缀口径由 tools/verify-module-mapping.cjs 在真库上验证。
    SELECT count(*) INTO n_prefix FROM sys_module_permission
     WHERE deleted = 0
       AND permission_prefix IS NOT NULL
       AND 'system:tenant-module:assign' LIKE permission_prefix || '%';
    IF n_prefix < 1 THEN
        RAISE EXCEPTION 'system:tenant-module:assign 无模块前缀归属 —— 需在 sys_module_permission 补映射行';
    END IF;

    RAISE NOTICE 'V11.485.0 自检通过：新增 2 个权限码并已授予超管角色，模块前缀归属已覆盖';
END $$;
