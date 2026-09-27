-- =====================================================================================
-- 阶段 3 · 批 2 前置：给 `party_tenant.id` 一个**独立号段**
--
-- 【为什么需要】`party_tenant` 建表时 `id BIGINT PRIMARY KEY` 没有默认值，回填期一律复用
-- `biz_party.id`（1:1，稳定可对账）。但**双写**之后情况变了：
-- ⑬ 裁定「同一对主体在同一家店的角色**可以并存**（客户 + 供应商）」⇒ 同一主体会需要
-- **两条边**（SALE + PURCHASE），而 `id` 不能复用同一个值 ⇒ 必须有独立的 id 来源。
--
-- 【号段选择】起始 `9000000000000000001`：
--   · 本仓应用侧雪花 id 约 `2.1e18` ⇒ 与之**不相交**；
--   · PostgreSQL `bigint` 上限 `9.223e18` ⇒ 不溢出（`9.9e18` 那种取法会溢出，故取 9.0e18）。
--
-- 【回填的旧行不受影响】它们仍持有 `biz_party.id`；本迁移只加"新行的默认值"。
--   双写的幂等靠唯一索引 `uk_party_tenant_edge (tenant_id, party_id, direction) WHERE deleted = 0`，
--   与 id 取什么值无关。
-- =====================================================================================

CREATE SEQUENCE IF NOT EXISTS seq_party_tenant START 9000000000000000001;

ALTER TABLE party_tenant ALTER COLUMN id SET DEFAULT nextval('seq_party_tenant');

COMMENT ON SEQUENCE seq_party_tenant IS
  'party_tenant 的 id 号段（独立于应用雪花 id，起点 9.0e18）：双写可能为同一主体建多条边，id 不能复用 biz_party.id。';

-- 自检：默认值必须已挂上，且序列起点落在安全区间
DO $$
DECLARE
    v_def text;
    v_start bigint;
BEGIN
    SELECT column_default INTO v_def FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'party_tenant' AND column_name = 'id';
    IF v_def IS NULL OR v_def NOT LIKE 'nextval%' THEN
        RAISE EXCEPTION 'party_tenant.id 的默认值未挂上序列，实际 %', coalesce(v_def, '(无)');
    END IF;

    SELECT start_value INTO v_start FROM pg_sequences
     WHERE schemaname = 'public' AND sequencename = 'seq_party_tenant';
    IF v_start IS NULL OR v_start < 9000000000000000000 OR v_start > 9223372036854775807 THEN
        RAISE EXCEPTION 'seq_party_tenant 起点 % 不在安全区间（需 ≥9.0e18 且不超 bigint 上限）', v_start;
    END IF;
END $$;
