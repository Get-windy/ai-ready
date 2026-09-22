-- =====================================================================================
-- 商城「本店启用/停用」下沉到关联表（DOMAIN-MODEL §11.3 阶段 1 遗留项）
--
-- 【为什么要这一列】原来的"停用/启用"写在 `shop_user.status` 上，而 `shop_user` 已按
--   2026-09-22 裁定**升为系统级身份**（tenant_id 恒 0）⇒ 租户管理员点一次"停用"，
--   会把这个顾客在**所有店**一起停掉（A 店的运营动作影响到了 B 店）。
--   按模型：**「他能不能进这家店」是逐租户的关系**，就该落在关联表上。
--
-- 【为什么不复用 status（0待审/1正常/2已拒绝/3已解除）】
--   `status` 表达的是**准入审核**（申请→通过/拒绝），"停用"是**准入之后的运营动作** ——
--   两者是两个维度：停用一个已通过的顾客，他的审核记录仍然是"已通过"。
--   挤进同一个列会让"这一列到底在说什么"变模糊（本仓最容易踩、也最贵的一类错：
--   `status` 字段逐表语义不同，见既有教训）。故**另立一列**，两者正交。
--
-- 【两个"停用"的分工（不许混）】
--   · `shop_user.status` = **平台级账号开关**（封号），影响该顾客在所有店；
--   · `shop_user_tenant.enabled` = **本店启用/停用**，由本店管理员操作，只影响本店。
--   登录时**两道都要过**（平台没封 + 本店没停）。
-- =====================================================================================

ALTER TABLE shop_user_tenant ADD COLUMN IF NOT EXISTS enabled INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN shop_user_tenant.enabled IS
  '本店启用状态：1=启用（可登录本店）0=停用（本店管理员操作，**只影响本店**）。'
  '与 status（准入审核：0待审/1正常/2已拒绝/3已解除）是两个正交维度 —— 停用一个已通过的顾客，其审核记录仍是"已通过"。'
  '另见 shop_user.status：那是**平台级**账号开关，影响该顾客在所有店。';

-- 存量行：本表此前 0 行（2026-09-22 真库核实），新加列由 DEFAULT 1 回填为"启用"，
-- 语义上等于"没有被任何店停过"，与"存量不回改"的口径一致，无需额外 UPDATE。

-- ─────────────── 自检（任一断言不成立则整个迁移回滚） ───────────────
DO $$
DECLARE
    n_col     int;
    n_notnull int;
    n_null    int;
    n_default text;
BEGIN
    -- 1. 列必须在（且恰好一列）
    SELECT count(*) INTO n_col FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'shop_user_tenant' AND column_name = 'enabled';
    IF n_col <> 1 THEN
        RAISE EXCEPTION 'shop_user_tenant.enabled 应有且仅有 1 列，实际 %', n_col;
    END IF;

    -- 2. 必须 NOT NULL（三态会让"没说清"变成一种新状态）
    SELECT count(*) INTO n_notnull FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'shop_user_tenant'
       AND column_name = 'enabled' AND is_nullable = 'NO';
    IF n_notnull <> 1 THEN
        RAISE EXCEPTION 'shop_user_tenant.enabled 必须是 NOT NULL';
    END IF;

    -- 3. 默认值必须是 1（启用）——「默认停用」会让所有存量顾客进不了店
    SELECT column_default INTO n_default FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'shop_user_tenant' AND column_name = 'enabled';
    IF n_default IS DISTINCT FROM '1' THEN
        RAISE EXCEPTION 'shop_user_tenant.enabled 的默认值应为 1（启用），实际 %', coalesce(n_default, '(无)');
    END IF;

    -- 4. 存量行不得有 NULL（有 NULL 说明回填没做完）
    SELECT count(*) INTO n_null FROM shop_user_tenant WHERE enabled IS NULL;
    IF n_null > 0 THEN
        RAISE EXCEPTION 'shop_user_tenant 有 % 行 enabled 为 NULL', n_null;
    END IF;
END $$;
