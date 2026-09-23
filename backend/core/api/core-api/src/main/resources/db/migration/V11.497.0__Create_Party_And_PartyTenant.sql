-- =====================================================================================
-- 阶段 3 序 2：建 `party` / `party_tenant` 两张空表 + 装唯一约束 + 一照一档回填
-- （方案：docs/PHASE-3-5-MIGRATION-PLAN-v1.md §3.3 方案 B 第 1、2 步）
--
-- 【本批只做"新增 + 装配 + 回填"，不动任何读路径】
--   按方案的"只加不删"策略：这一步之后**随时可退**（表是新增的、读路径未变）。
--
-- 【两张表的层级（别搞反）】
--   · `party`        = **共享层**：`tenant_id` 恒 0（§6.5：0 = 默认共享租户）。
--                       它是"单位是谁"，不属于任何一家店 ⇒ 必须登记进 IGNORE_TENANT_TABLES。
--   · `party_tenant` = **租户维度表**：`tenant_id` = 档案所属租户（视角方），
--                       **不**登记忽略 ⇒ 由租户拦截器按会话租户过滤（这正是 R2 的落点）。
--
-- 【关键语义搬移】`biz_party.tenant_id`（"这条档案归哪个租户"）**不是** party 的属性，
--   而是**关系**的属性 ⇒ 它整体搬到 `party_tenant.tenant_id`，`party.tenant_id` 一律 0。
--   这正是"把两个系统级主体从租户内搬出来"的那一步。
--
-- 【方向（direction）不猜】`party_tenant` 一行 = 一条**有向边**（⑲ / §3.4.2b）。
--   存量方向从**既有数据自己的表述**读出：`roles` 含 CUSTOMER ⇒ SALE（我卖给他=客户）、
--   含 SUPPLIER ⇒ PURCHASE；`roles` 为空时按 `party_type`（2=供应商⇒PURCHASE）。
--   ⚠️ `party_type=3`（承运商）**既不是客户也不是供应商**（§3.2c 第三方服务主体）⇒
--   **不建边**，并在自检里显式断言"它在 party 里、但不在 party_tenant 里"，证明是"不猜"而不是漏了。
-- =====================================================================================

-- ─────────────── 1. party：往来单位主档（共享层，tenant_id 恒 0） ───────────────
CREATE TABLE IF NOT EXISTS party (
    id            BIGINT       PRIMARY KEY,                       -- 应用侧雪花 id；回填期复用 biz_party.id（见下）
    tenant_id     BIGINT       NOT NULL DEFAULT 0,                -- §6.5：共享层恒 0
    party_code    VARCHAR(32),
    unified_code  VARCHAR(32),                                    -- 统一社会信用代码：**主档识别键**
    party_name    VARCHAR(128),
    short_name    VARCHAR(64),
    party_type    INTEGER,                                        -- 1 客户 / 2 供应商 / 3 物流 / 4 其他（沿用 biz_party 口径）
    legal_person  VARCHAR(64),
    status        INTEGER      NOT NULL DEFAULT 1,
    create_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0
);
COMMENT ON TABLE party IS
  '往来单位主档（**共享层**，tenant_id 恒 0；"这条档案归哪个租户"是关系、在 party_tenant）。'
  'unified_code 是主档识别键，唯一约束见 uk_party_unified_code。';

-- 主档识别键唯一：**这是"同一真单位不能在主档里出现两次"的技术保证**（裁定②的连带要求）
CREATE UNIQUE INDEX IF NOT EXISTS uk_party_unified_code
    ON party (unified_code) WHERE deleted = 0 AND unified_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_party_name ON party (party_name) WHERE deleted = 0;

-- ─────────────── 2. party_tenant：R2 贸易档案，一行 = 一条有向边 ───────────────
CREATE TABLE IF NOT EXISTS party_tenant (
    id                  BIGINT       PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,       -- 档案所属租户（**视角方，永远是我**）
    party_id            BIGINT       NOT NULL,       -- 对方主体
    direction           VARCHAR(16)  NOT NULL,       -- SALE 我卖给他(客户) / PURCHASE 我向他买(供应商)
    -- ↓↓ 商务条件全部是「本方向」的，绝不跨方向共享（⑲ 账期不可传递）↓↓
    party_level         VARCHAR(32),
    settlement_type     VARCHAR(32),
    settlement_days     INTEGER,
    fixed_payment_day   INTEGER,
    statement_day       INTEGER,
    credit_limit        NUMERIC(18,2),
    price_track_enabled BOOLEAN,
    handler_id          BIGINT,
    status              INTEGER      NOT NULL DEFAULT 1,
    effective_from      DATE,
    effective_to        DATE,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER      NOT NULL DEFAULT 0
);
COMMENT ON TABLE party_tenant IS
  'R2 贸易档案：**一行 = 一条有向边**（SALE=我的客户 / PURCHASE=我的供应商，⑬角色可并存）。'
  '商务条件（等级/结算/账期/额度）都是**本方向**的，不跨方向共享（⑲ 账期不可传递）。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_party_tenant_edge
    ON party_tenant (tenant_id, party_id, direction) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_party_tenant_party ON party_tenant (party_id) WHERE deleted = 0;

-- ─────────────── 3. 一照一档回填：19 行 biz_party → party + party_tenant ───────────────
-- ⚠️ 回填期 **party.id 复用 biz_party.id**：1:1、稳定、可对账，回滚也只是删这两张表的数据。
--   （将来同一主体出现第二条边时，party_tenant.id 由应用生成新雪花 id，**不复用**。）
INSERT INTO party (id, tenant_id, party_code, unified_code, party_name, party_type, status,
                   create_time, update_time, deleted)
SELECT b.id, 0, b.party_code, b.unified_code, b.party_name, b.party_type, COALESCE(b.status, 1),
       now(), now(), 0
FROM biz_party b
WHERE b.deleted = 0
ON CONFLICT (id) DO NOTHING;

-- 方向：先看 roles（数据自己的表述），roles 为空才回落到 party_type；**两类都不满足的（承运商）不建边**
INSERT INTO party_tenant (id, tenant_id, party_id, direction, party_level,
                          create_time, update_time, deleted)
SELECT b.id, b.tenant_id, b.id,
       CASE WHEN b.roles LIKE '%CUSTOMER%' THEN 'SALE' ELSE 'PURCHASE' END,
       b.party_level, now(), now(), 0
FROM biz_party b
WHERE b.deleted = 0
  AND (b.roles LIKE '%CUSTOMER%' OR b.roles LIKE '%SUPPLIER%' OR b.party_type = 2)
ON CONFLICT (id) DO NOTHING;

-- ─────────────── 4. 自检（任一断言不成立则整个迁移回滚，不留半成品） ───────────────
DO $$
DECLARE
    n_tables   int;
    n_uk       int;
    n_src      int;
    n_party    int;
    n_edge     int;
    n_bad_dir  int;
    n_dup_edge int;
    n_carrier  int;
    n_car_edge int;
    n_shared   int;
BEGIN
    -- 4.1 两张表 + 两个唯一索引都在
    SELECT count(*) INTO n_tables FROM information_schema.tables
     WHERE table_schema = 'public' AND table_name IN ('party', 'party_tenant');
    IF n_tables <> 2 THEN
        RAISE EXCEPTION 'party / party_tenant 应两张都建好，实际 % 张', n_tables;
    END IF;
    SELECT count(*) INTO n_uk FROM pg_indexes
     WHERE schemaname = 'public' AND indexname IN ('uk_party_unified_code', 'uk_party_tenant_edge');
    IF n_uk <> 2 THEN
        RAISE EXCEPTION '两个唯一索引应都在（uk_party_unified_code / uk_party_tenant_edge），实际 % 个', n_uk;
    END IF;

    -- 4.2 一照一档：party 行数必须等于 biz_party 未删行数（不许少搬、也不许多造）
    SELECT count(*) INTO n_src FROM biz_party WHERE deleted = 0;
    SELECT count(*) INTO n_party FROM party WHERE deleted = 0;
    IF n_party <> n_src THEN
        RAISE EXCEPTION 'party 行数 % 与 biz_party 未删行数 % 不一致（一照一档要求相等）', n_party, n_src;
    END IF;

    -- 4.3 party.tenant_id 必须恒为 0（共享层）—— 写进别的值就是"又把主体塞回租户内"
    SELECT count(*) INTO n_shared FROM party WHERE deleted = 0 AND tenant_id <> 0;
    IF n_shared > 0 THEN
        RAISE EXCEPTION 'party 有 % 行 tenant_id 不为 0（共享层恒 0）', n_shared;
    END IF;

    -- 4.4 方向必须是 SALE / PURCHASE 两者之一，且 (tenant_id, party_id, direction) 不重复
    SELECT count(*) INTO n_bad_dir FROM party_tenant
     WHERE deleted = 0 AND direction NOT IN ('SALE', 'PURCHASE');
    IF n_bad_dir > 0 THEN
        RAISE EXCEPTION 'party_tenant 有 % 行 direction 不是 SALE/PURCHASE', n_bad_dir;
    END IF;
    SELECT count(*) INTO n_dup_edge FROM (
        SELECT tenant_id, party_id, direction FROM party_tenant WHERE deleted = 0
         GROUP BY 1, 2, 3 HAVING count(*) > 1) x;
    IF n_dup_edge > 0 THEN
        RAISE EXCEPTION 'party_tenant 有 % 组重复边（唯一索引应已挡住）', n_dup_edge;
    END IF;

    -- 4.5 **"不猜方向"的证据**：承运商（party_type=3）在 party 里有、party_tenant 里没有。
    --     它既不是客户也不是供应商（§3.2c 第三方服务主体），方向未定 ⇒ 宁可不建边，也不编一个。
    SELECT count(*) INTO n_carrier FROM party WHERE deleted = 0 AND party_type = 3;
    SELECT count(*) INTO n_car_edge FROM party_tenant pt
      JOIN party p ON p.id = pt.party_id AND p.deleted = 0
     WHERE pt.deleted = 0 AND p.party_type = 3;
    IF n_car_edge <> 0 THEN
        RAISE EXCEPTION 'party_type=3（第三方服务主体）不应有贸易边，实际 % 条 —— 方向不许猜', n_car_edge;
    END IF;
    RAISE NOTICE '回填完成：party % 行（其中 party_type=3 的 % 行不建贸易边）· party_tenant % 条边',
        n_party, n_carrier, (SELECT count(*) FROM party_tenant WHERE deleted = 0);
END $$;
