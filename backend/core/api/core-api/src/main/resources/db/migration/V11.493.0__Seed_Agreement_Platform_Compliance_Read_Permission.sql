-- =====================================================================================
-- 协议「平台合规抽查读」独立权限码（DOMAIN-MODEL §13.9 / §8.2 待办）
--
-- 【为什么必须有独立码】用户 2026-09-22 明确：「平台**合规抽查读权限需要有**（避免非法交易）」。
--   但在此之前，协议的**所有**读端点都复用**租户级** `agreement:view`
--   ⇒ 平台侧要么读不到（`AgreementVisibility` 只放行两端租户 —— 这是对的），
--     要么只能把租户级码授给平台（那等于把"读别人协议"的能力散给所有租户管理员，**更糟**）。
--
-- 【为什么它天然只有平台侧能用】前缀 `agreement:platform:` 在 `sys_module_permission`
--   (id=53) 已映射到模块 `system`（最长前缀优先）；而按 V11.455.0，「系统」模块**只开给系统租户**
--   ⇒ 这道能力与平台协议写权限一样，天然只有平台侧能拿。**不另造机制。**
--
-- 【与 `agreement:platform:template:read` 的区别（别混）】
--   · `template:read` 读的是**平台模板**（契约模板，本来就不是租户的商业秘密）；
--   · 本码读的是**协议正本**（任意两个租户之间签的商业契约）—— 敏感级别完全不同，
--     因此**单独一个码**，且调用处必须 `@OperLog` **留痕**（谁在什么时候抽查了哪一份）。
--
-- 【口径】只读，不新增任何写能力；不新增菜单（平台合规抽查是平台侧的稽核动作，
--   不走业务导航；按本仓纪律改 sys_menu 需另行批准）。
-- =====================================================================================

-- ─────────────── 1. 权限码 ───────────────
-- 逐列对齐同域既有码口径：tenant_id=0 / parent_id=0 / deleted=0 / permission_type=3 / visible=1 / status=0
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT 111147, 0, 0, 0, now(), now(), '协议平台合规抽查读', 'agreement:platform:compliance:read', 3,
       '/api/agreement/platform/compliance/page', 'GET', 1506, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p
                   WHERE p.permission_code = 'agreement:platform:compliance:read');


-- ─────────────── 2. 授予超管角色（role_id = 1 = SUPER_ADMIN / tenant_id = 1） ───────────────
-- 不授的话平台管理员自己运行时也会被拒（本仓对"注解有码、角色无码"的处置纪律）。
-- JOIN 用 MIN(id) 取主记录：permission_code 无唯一约束，历史重复码会让同一 rp_id 产出多行而撞主键。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9610128, 1, p.id, 1, now()
FROM (SELECT MIN(id) AS id FROM sys_permission
       WHERE permission_code = 'agreement:platform:compliance:read') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                   WHERE rp.role_id = 1 AND rp.permission_id = p.id);


-- ─────────────── 3. 自检（任一断言不成立则整个迁移回滚，不留半成品） ───────────────
DO $$
DECLARE
    n_code     int;
    n_granted  int;
    v_owner    text;
    n_dup      int;
BEGIN
    -- 3.1 码必须在库里（**注解引用的码必须已存在**，否则非超管全 403，且不报编译/启动错）
    SELECT count(*) INTO n_code FROM sys_permission
     WHERE permission_code = 'agreement:platform:compliance:read' AND deleted = 0;
    IF n_code <> 1 THEN
        RAISE EXCEPTION '平台合规抽查读码应有且仅有 1 条，实际 %', n_code;
    END IF;

    -- 3.2 超管必须已授（否则平台管理员自己都读不了）
    SELECT count(*) INTO n_granted FROM sys_role_permission rp
      JOIN sys_permission p ON p.id = rp.permission_id
     WHERE rp.role_id = 1 AND p.permission_code = 'agreement:platform:compliance:read';
    IF n_granted < 1 THEN
        RAISE EXCEPTION '平台合规抽查读码未授予超管角色，平台侧将无法使用';
    END IF;

    -- 3.3 模块归属必须是 system（最长前缀优先）—— 这是"天然只有平台侧能用"的技术依据
    SELECT m.module_code INTO v_owner
      FROM sys_module_permission m
      JOIN sys_permission p ON p.deleted = 0
       AND 'agreement:platform:compliance:read' LIKE m.permission_prefix || '%'
     WHERE m.deleted = 0
     ORDER BY length(m.permission_prefix) DESC, m.sort LIMIT 1;
    IF v_owner IS DISTINCT FROM 'system' THEN
        RAISE EXCEPTION '平台合规抽查读码应归属 system，实测归属 %（最长前缀优先被破坏了）', coalesce(v_owner, '(无归属)');
    END IF;

    -- 3.4 同码不得重复（重复码会让"授了哪一条"变得不确定）
    SELECT count(*) INTO n_dup FROM sys_permission
     WHERE permission_code = 'agreement:platform:compliance:read' AND deleted = 0;
    IF n_dup <> 1 THEN
        RAISE EXCEPTION '平台合规抽查读码出现 % 条同码记录', n_dup;
    END IF;
END $$;
