-- ══════════════════════════════════════════════════════════════════════════════
-- 补漏：把两条**裸前缀**的 `data-scope:{view,set}` 也并入 `tenant-admin:` 码族
--
-- 背景（是上一个迁移 V11.456.0 的漏项，不是新决定）：
--   「数据权限」这一个子域里居然混着**两种写法**：
--     · `system:data-scope:{assign,delete,list}` —— 配 `SysDataScopeController`（角色数据权限）
--     · 裸 `data-scope:{view,set}`               —— 配 `UserDataScopeController`（用户数据权限）
--   V11.456.0 只搬了 `system:` 那半，而这半留在裸前缀上；同批又把映射表里的
--   `system` → `data-scope:` 前缀行删掉了 ⇒ 这 2 条码**变成"无模块归属"**。
--
--   暴露方式值得记一笔：不是靠人肉复查，而是靠 `tools/verify-module-mapping.cjs` 里那条
--   **"在役权限码必须 100% 能被某个前缀接住"** 的断言 —— 它直接报了
--   `未归属 2 条：["data-scope:view","data-scope:set"]`。断言写得够狠，补漏就不用靠运气。
--
-- 这两条码是**真在用的**（`UserDataScopeController` 的 `@SaCheckPermission` 引用，
-- 不是僵尸码），故不能删，只能跟着改名 —— 源码侧已由
-- `tools/rename-permission-prefix.py`（BARE 列表已补 `data-scope`）同步改好。
-- ══════════════════════════════════════════════════════════════════════════════

UPDATE sys_permission
SET permission_code = 'tenant-admin:' || permission_code
WHERE deleted = 0
  AND permission_code IN ('data-scope:view', 'data-scope:set')
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission q
      WHERE q.deleted = 0 AND q.permission_code = 'tenant-admin:' || sys_permission.permission_code
  );

-- ── 自检 ──
DO $$
DECLARE
    bare_left   int;
    fam_count   int;
    unmapped    int;
    dup_codes   int;
BEGIN
    -- 1. 裸写法必须清零
    SELECT count(*) INTO bare_left
    FROM sys_permission
    WHERE deleted = 0 AND (permission_code LIKE 'data-scope:%'
                        OR permission_code LIKE 'department:%'
                        OR permission_code LIKE 'position:%'
                        OR permission_code LIKE 'permission-template:%'
                        OR permission_code LIKE 'role-inheritance:%');
    IF bare_left > 0 THEN
        RAISE EXCEPTION '仍有 % 条裸前缀码未并入 tenant-admin: 族', bare_left;
    END IF;

    -- 2. 码族规模：原 70 条 + 本次 2 条 = 72
    SELECT count(*) INTO fam_count
    FROM sys_permission WHERE deleted = 0 AND permission_code LIKE 'tenant-admin:%';
    IF fam_count <> 72 THEN
        RAISE EXCEPTION 'tenant-admin: 码族应为 72 条，实际 % 条', fam_count;
    END IF;

    -- 3. ★关键★：全库在役码仍然 100% 有模块归属（这条断言就是当初抓出漏项的那条）
    SELECT count(*) INTO unmapped
    FROM sys_permission c
    WHERE c.deleted = 0 AND c.permission_type <> 1
      AND NOT EXISTS (SELECT 1 FROM sys_module_permission p
                      WHERE p.deleted = 0 AND c.permission_code LIKE p.permission_prefix || '%');
    IF unmapped > 0 THEN
        RAISE EXCEPTION '仍有 % 条在役码无模块归属（模块→码 映射有洞）', unmapped;
    END IF;

    -- 4. 同码多行
    SELECT count(*) INTO dup_codes
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup_codes > 0 THEN
        RAISE EXCEPTION '出现 % 组同码多行', dup_codes;
    END IF;
END $$;
