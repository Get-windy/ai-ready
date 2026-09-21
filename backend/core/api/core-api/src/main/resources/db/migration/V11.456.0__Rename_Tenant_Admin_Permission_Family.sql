-- ══════════════════════════════════════════════════════════════════════════════
-- 另立「租户管理」码族 `tenant-admin:`（用户裁定 2026-09-21）
--
-- 起因（是一个**逻辑冲突**，不是命名洁癖）：
--   · 用户裁定：「平台级『系统』模块只开给系统租户」；
--   · 用户口述：「租户内的部门管理员权限由**租户所在系统管理员**给与配置」。
--   而租户内管理"人 / 角色 / 权限"的那批码，前缀全是 `system:user:*` / `system:role:*` /
--   `system:permission:*` / `system:data-scope:*` / `system:field-permission:*` /
--   `system:record-rule:*` / `system:sod-rule:*` —— 按 V11.454.0 的映射全归「系统」模块。
--   ⇒ **收紧之后，业务租户将没有任何码可以管理自己的用户与角色**，
--     而「设置」模块当时的 25 个码（`set:` / `workflow:` / `print:`）一个都不管这些。
--
-- 裁定的修法：**另立租户管理码族**，把这批码从平台级的「系统」域剥出来，归「设置」（租户级）模块。
--   ① 7 个子域从 `system:` 剥离：`system:<fam>:<act>` → `tenant-admin:<fam>:<act>`
--      （user / role / permission / data-scope / field-permission / record-rule / sod-rule）
--   ② 4 个本来就是裸前缀的子域一并并入同一族：`<fam>:<act>` → `tenant-admin:<fam>:<act>`
--      （department / position / permission-template / role-inheritance）
--      并入的理由：留着裸前缀会变成"半新半旧"两种写法，而它们同属"租户内的人与权限"。
--   共 70 条码。
--
-- ⚠️ 为什么这次改名比命名本身重要：本仓铁律是「**码必须先在 sys_permission 里存在，注解才能挂**」。
--   改名必须**跨端一次性做完**（后端注解 + 前端 v-permission + 种子 + 本迁移），
--   只做一半的症状是**所有非超管一律 403**，且不会编译失败、不会启动失败。
--   本次用 `tools/rename-permission-prefix.py` 按"真库读码 → 整码精确匹配"改源码，
--   实测 219 处引用 / 23 个文件，改完复扫**旧码零残留**；本迁移负责 DB 侧，
--   两者在同一轮构建重启中同时生效。
--   ⚠️ 特别记一笔：该脚本第一版是**按前缀**替换的，结果把打印引擎里的 CSS
--      `position:relative;` / `position:absolute;`（4 处）也当成权限码 —— 故改为整码匹配。
--
-- 不改的东西（已核对）：`sys_role_permission` 按 permission_id 关联，与码文本无关；
--   `sys_permission_template.permission_config` 存的是**id 数组**不是码；
--   `sys_record_rule`（0 行）的 perm_* 是**整数**列不是码。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 从「系统」域剥离 7 个子域 ──
UPDATE sys_permission
SET permission_code = 'tenant-admin:' || substring(permission_code from 8)
WHERE deleted = 0
  AND (permission_code LIKE 'system:user:%'
    OR permission_code LIKE 'system:role:%'
    OR permission_code LIKE 'system:permission:%'
    OR permission_code LIKE 'system:data-scope:%'
    OR permission_code LIKE 'system:field-permission:%'
    OR permission_code LIKE 'system:record-rule:%'
    OR permission_code LIKE 'system:sod-rule:%')
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission q
      WHERE q.deleted = 0
        AND q.permission_code = 'tenant-admin:' || substring(sys_permission.permission_code from 8)
  );

-- ── 2. 4 个裸前缀子域并入同一族 ──
UPDATE sys_permission
SET permission_code = 'tenant-admin:' || permission_code
WHERE deleted = 0
  AND (permission_code LIKE 'department:%'
    OR permission_code LIKE 'position:%'
    OR permission_code LIKE 'permission-template:%'
    OR permission_code LIKE 'role-inheritance:%')
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission q
      WHERE q.deleted = 0
        AND q.permission_code = 'tenant-admin:' || sys_permission.permission_code
  );

-- ── 3. 映射表改挂：这批码从「系统」模块划归「设置」模块 ──
DELETE FROM sys_module_permission
WHERE deleted = 0
  AND module_code = 'system'
  AND permission_prefix IN ('data-scope:', 'department:', 'position:',
                            'permission-template:', 'role-inheritance:');

INSERT INTO sys_module_permission (tenant_id, module_code, permission_prefix, sort, remark,
                                   create_time, update_time)
SELECT 0, 'settings', 'tenant-admin:', 140,
       '租户内的人/角色/权限管理：用户、角色、权限、数据范围、字段权限、记录规则、SoD、部门、岗位、权限模板、角色继承（原 system: 域剥离）',
       now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_module_permission p
    WHERE p.deleted = 0 AND p.module_code = 'settings' AND p.permission_prefix = 'tenant-admin:'
);

-- 修正既有那两条的说明，免得后人以为 department/position 还归「系统」
UPDATE sys_module_permission
SET remark = '平台参数、菜单、字典、门户配置、AI Agent、会话、开发者工具、数据导入',
    update_time = now()
WHERE deleted = 0 AND module_code = 'system' AND permission_prefix = 'system:';

-- ── 4. 自检 ──
DO $$
DECLARE
    old_left      int;
    new_count     int;
    dup_codes     int;
    stg_prefix    int;
    sys_prefix    int;
    stale_prefix  int;
    n_modules     int;
    mapping_ok    int;
BEGIN
    -- 4.1 旧写法必须一条不留（含 7 个 system 子域 + 4 个裸族）
    SELECT count(*) INTO old_left
    FROM sys_permission
    WHERE deleted = 0
      AND (permission_code LIKE 'system:user:%'
        OR permission_code LIKE 'system:role:%'
        OR permission_code LIKE 'system:permission:%'
        OR permission_code LIKE 'system:data-scope:%'
        OR permission_code LIKE 'system:field-permission:%'
        OR permission_code LIKE 'system:record-rule:%'
        OR permission_code LIKE 'system:sod-rule:%'
        OR permission_code LIKE 'department:%'
        OR permission_code LIKE 'position:%'
        OR permission_code LIKE 'permission-template:%'
        OR permission_code LIKE 'role-inheritance:%');
    IF old_left > 0 THEN
        RAISE EXCEPTION '改名后仍有 % 条旧写法的码（源码与库会对不上 ⇒ 非超管全 403）', old_left;
    END IF;

    -- 4.2 新码族必须是 70 条
    SELECT count(*) INTO new_count
    FROM sys_permission WHERE deleted = 0 AND permission_code LIKE 'tenant-admin:%';
    IF new_count <> 70 THEN
        RAISE EXCEPTION 'tenant-admin: 码族应为 70 条，实际 % 条', new_count;
    END IF;

    -- 4.3 不得因改名撞出同码多行（E-02 的老毛病）
    SELECT count(*) INTO dup_codes
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup_codes > 0 THEN
        RAISE EXCEPTION '改名后出现 % 组同码多行', dup_codes;
    END IF;

    -- 4.4 映射表：tenant-admin: 必须归「设置」
    SELECT count(*) INTO stg_prefix
    FROM sys_module_permission
    WHERE deleted = 0 AND module_code = 'settings' AND permission_prefix = 'tenant-admin:';
    IF stg_prefix <> 1 THEN
        RAISE EXCEPTION 'tenant-admin: 前缀未挂到「设置」模块';
    END IF;

    -- 4.5 映射表：旧的 5 条「系统」前缀必须已清掉
    SELECT count(*) INTO stale_prefix
    FROM sys_module_permission
    WHERE deleted = 0 AND module_code = 'system'
      AND permission_prefix IN ('data-scope:', 'department:', 'position:',
                                'permission-template:', 'role-inheritance:');
    IF stale_prefix > 0 THEN
        RAISE EXCEPTION '「系统」模块仍残留 % 条已迁走的码前缀', stale_prefix;
    END IF;

    -- 4.6 「系统」模块自身仍要有码（别把平台级码也改没了）
    SELECT count(*) INTO sys_prefix
    FROM sys_permission WHERE deleted = 0 AND permission_code LIKE 'system:%';
    IF sys_prefix < 50 THEN
        RAISE EXCEPTION '「系统」域剩下的码过少（% 条），疑似误改', sys_prefix;
    END IF;

    -- 4.7 模块目录仍是 13 条
    SELECT count(*) INTO n_modules FROM sys_module WHERE deleted = 0;
    IF n_modules <> 13 THEN
        RAISE EXCEPTION '模块目录应为 13 条，实际 % 条', n_modules;
    END IF;

    -- 4.8 全部 70 条新码都必须能被映射表接住（按最长前缀）
    -- ⚠️ 这里**不能**加 `permission_type <> 1`：本族里 `tenant-admin:department:manage` 与
    --    `tenant-admin:position:manage` 是 type=1 的**分组节点**，它们同样要被映射表接住
    --    （否则矩阵里那两棵子树会没有模块归属）。首次跑该迁移就是把这条写成了带类型过滤，
    --    结果 68 <> 70 报错回滚 —— 数据没问题，是自检写错了。
    SELECT count(*) INTO mapping_ok
    FROM sys_permission c
    WHERE c.deleted = 0 AND c.permission_code LIKE 'tenant-admin:%'
      AND EXISTS (SELECT 1 FROM sys_module_permission p
                  WHERE p.deleted = 0 AND c.permission_code LIKE p.permission_prefix || '%');
    IF mapping_ok <> 70 THEN
        RAISE EXCEPTION 'tenant-admin: 码族有 % 条无法归属到模块（应 70）', 70 - mapping_ok;
    END IF;
END $$;
