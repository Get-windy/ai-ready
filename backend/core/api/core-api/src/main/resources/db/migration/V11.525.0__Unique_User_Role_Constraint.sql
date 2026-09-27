-- 给 sys_user_role 补唯一约束 (user_id, role_id)，并清理由此暴露的重复数据。
--
-- ── 背景 ──────────────────────────────────────────────────────────────────
-- 2026-09-27 权限模型调研发现：sys_user_role 只有 PRIMARY KEY(id)，
-- **没有 (user_id, role_id) 唯一约束** ⇒ 同一用户可被重复授予同一角色。
-- 实测已存在脏数据：用户 e2e_route_doc 挂了 **2 条 SUPER_ADMIN**
-- （id=2099000000000001201 与 2099000000000001291）。
--
-- 影响：权限取并集虽不会因此放大权限，但会污染「用户角色数」统计与审计口径，
--      且让「一人多角色」与「重复授同一角色」在数据上无法区分。
--
-- 本迁移与「一人多角色」并不冲突：唯一约束是 (user_id, role_id) 组合，
-- 一个用户挂**不同**角色仍完全允许（这正是方案 B 里「副总 = 业务经理 + 财务经理」的表达方式）。
--
-- 幂等：清重用「保留最小 id」的写法；加约束前先判断是否已存在。
--
-- 对应文档：PERMISSION_MODEL_RESEARCH_20260927.md 第四节「两个真实缺陷」之①

-- ① 清理重复：同一 (user_id, role_id) 只保留 id 最小的一行
DELETE FROM sys_user_role a
USING sys_user_role b
WHERE a.user_id = b.user_id
  AND a.role_id = b.role_id
  AND a.id > b.id;

-- ② 补唯一约束（幂等：已存在则跳过）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_sys_user_role_user_role'
          AND conrelid = 'sys_user_role'::regclass
    ) THEN
        ALTER TABLE sys_user_role
            ADD CONSTRAINT uk_sys_user_role_user_role UNIQUE (user_id, role_id);
    END IF;
END $$;
