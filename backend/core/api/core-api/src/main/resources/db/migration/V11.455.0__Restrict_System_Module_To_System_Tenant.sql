-- ══════════════════════════════════════════════════════════════════════════════
-- 平台级「系统」模块**只开给系统租户**（用户裁定 2026-09-21）
--
-- 裁定原话：「平台级『系统』模块只开给系统租户（"系统模块权限由超管授权给系统租户的用户"）。
--           设置模块才是租户级的管理设置模块」。
--
-- 背景：上一个迁移（V11.454.0）为了"不替业务做减法"，把 13 个模块**全量**补给了既有租户
-- （含平台级的 `system`），并在迁移末尾显式登记了这个待裁定项。现在口径已定，按裁定收紧。
--
-- 两层语义据此定型：
--   · **系统（system）= 平台级** → 只属于系统租户（tenant_id = 1）；平台超管把其中的权限
--     授予系统租户所属的用户与部门管理员。
--   · **设置（settings）= 租户级** → 每个租户的管理设置（企业信息、期初、打印配置、
--     账套重建、工作流、系统任务）。
--   · 其余 11 个是业务模块 → 由平台超管/系统租户决定是否授权给某个租户；
--     一旦授权，该租户的系统管理员即拥有租户内最高权限，并可配置本租户的部门管理员。
--
-- ⚠️ 本迁移**只动开通记录（entitlement），不动权限码、不动鉴权注解** ——
--    `TenantModuleService.hasModuleAccess()` 目前仍是零调用方（后端尚不按模块拦截），
--    所以这一收紧**不改变任何接口今天的可达性**，只是把"名册"改对。
--
-- ⚠️ 遗留问题（**不猜，登记在此**）：租户内的用户/角色/权限管理码目前是 `system:user:*` /
--    `system:role:*` / `system:permission:*` / `system:data-scope:*` 等，**前缀都是 `system:`**，
--    按 V11.454.0 的映射它们全归「系统」模块 ⇒ 收紧之后，**业务租户将再也没有任何码可以
--    管理自己的用户与角色**（「设置」模块现有的 25 个码是 set:/workflow:/print:，不含用户/角色）。
--    这需要产品口径：是把"租户管理员"那批码重新划归「设置」，还是另有安排。
--    **在裁定之前不动映射表** —— 否则 entitlement 门一上，租户 2 会连配本租户角色都做不到。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 回收非系统租户的「系统」模块开通记录（软删，可回滚）──
UPDATE sys_tenant_module
SET deleted = 1, update_time = now()
WHERE deleted = 0
  AND module_code = 'system'
  AND tenant_id <> 1;

-- ── 2. 系统租户（tenant_id = 1）必须保留「系统」模块；缺则补上 ──
INSERT INTO sys_tenant_module (id, tenant_id, module_code, module_name, purchase_type,
                               expire_time, status, create_time, update_time, deleted)
SELECT 9000000000000000999::BIGINT, 1, 'system', '系统', 'permanent', NULL, 0, now(), now(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_tenant_module tm
    WHERE tm.tenant_id = 1 AND tm.module_code = 'system' AND tm.deleted = 0
);

-- ── 3. 自检 ──
DO $$
DECLARE
    sys_tenants   text;
    other_sys     int;
    n_platform    int;
    n_modules     int;
    t2_settings   int;
BEGIN
    -- 3.1 「系统」模块的开通租户必须**恰好只有**系统租户 1
    SELECT string_agg(DISTINCT tenant_id::text, ',' ORDER BY tenant_id::text)
      INTO sys_tenants
      FROM sys_tenant_module
     WHERE deleted = 0 AND module_code = 'system';
    IF sys_tenants IS DISTINCT FROM '1' THEN
        RAISE EXCEPTION '「系统」模块应只开给系统租户(1)，实际开通租户 = %', COALESCE(sys_tenants, '(无)');
    END IF;

    -- 3.2 反向断言：非系统租户不得有「系统」模块
    SELECT count(*) INTO other_sys
      FROM sys_tenant_module
     WHERE deleted = 0 AND module_code = 'system' AND tenant_id <> 1;
    IF other_sys > 0 THEN
        RAISE EXCEPTION '仍有 % 个非系统租户持有「系统」模块', other_sys;
    END IF;

    -- 3.3 「系统」模块本身必须还在模块目录里（别把模块和开通记录搞混）
    SELECT count(*) INTO n_platform FROM sys_module WHERE deleted = 0 AND module_code = 'system';
    IF n_platform <> 1 THEN
        RAISE EXCEPTION 'sys_module 里的「系统」模块缺失（回收的是开通记录，不是模块本身）';
    END IF;

    -- 3.4 模块目录仍为 13 条（本迁移不该动目录）
    SELECT count(*) INTO n_modules FROM sys_module WHERE deleted = 0;
    IF n_modules <> 13 THEN
        RAISE EXCEPTION '模块目录应为 13 条，实际 % 条', n_modules;
    END IF;

    -- 3.5 租户级「设置」模块必须给到业务租户（否则它连管理设置都没有）
    SELECT count(*) INTO t2_settings
      FROM sys_tenant_module
     WHERE deleted = 0 AND tenant_id = 2 AND module_code = 'settings';
    IF t2_settings <> 1 THEN
        RAISE EXCEPTION '租户 2 缺「设置」模块 —— 它是租户级的管理设置模块，必须保留';
    END IF;
END $$;
