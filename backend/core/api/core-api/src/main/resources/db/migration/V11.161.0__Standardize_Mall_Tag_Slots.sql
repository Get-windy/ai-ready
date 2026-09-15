-- ============================================================================
-- V11.161.0  商品标签标准化（标准槽位 TAG_N + 用户自定义昵称）
--
-- 背景：对标 ql361「资料 → 商品辅助资料 → 商品标签」是**用户自建的标签字典**
--       （对标账号建了 20 条，名字是「早餐面点 / 酒席宴席 / …」这类**自定义昵称**）。
--       此前我方实现把 20 个昵称硬编码进前端（MALL_TAGS / MALL_INFO_TAGS），
--       既不是通用默认值、改昵称也不生效，且商品侧按**标签名**存储（改名即失联）。
--
-- 口径（与「价格等级」一致）：
--   标准槽位 = erp_mall_tag.tag_code（TAG_1..TAG_20），默认昵称「标签N」；
--   tag_name 为用户可改的昵称；商品侧 erp_product.mall_tags 改存**槽位编码**。
-- ============================================================================

ALTER TABLE erp_mall_tag ADD COLUMN IF NOT EXISTS tag_code varchar(32);

COMMENT ON COLUMN erp_mall_tag.tag_code IS '标签标准槽位编码 TAG_1..TAG_20；显示名 tag_name 为用户自定义昵称';

-- ── 1) 现有标签按 sort_order/id 顺序补标准槽位 ───────────────────────────────
WITH numbered AS (
    SELECT id,
           row_number() OVER (PARTITION BY tenant_id ORDER BY sort_order NULLS FIRST, id) AS rn
    FROM erp_mall_tag
    WHERE deleted = 0 AND (tag_code IS NULL OR tag_code = '')
)
UPDATE erp_mall_tag t
SET tag_code = 'TAG_' || n.rn
FROM numbered n
WHERE t.id = n.id;

-- ── 2) 每个租户补足到 20 个标准槽位（昵称默认「标签N」） ─────────────────────
INSERT INTO erp_mall_tag (id, tenant_id, tag_code, tag_name, sort_order, status, deleted, create_time, update_time)
SELECT (extract(epoch FROM clock_timestamp()) * 1000000)::bigint + gs.n,
       tt.tid,
       'TAG_' || gs.n,
       '标签' || gs.n,
       gs.n - 1,
       1, 0, now(), now()
FROM (SELECT DISTINCT tenant_id AS tid FROM erp_mall_tag WHERE deleted = 0) tt
CROSS JOIN generate_series(1, 20) AS gs(n)
WHERE NOT EXISTS (
    SELECT 1 FROM erp_mall_tag x
    WHERE x.tenant_id = tt.tid AND x.tag_code = 'TAG_' || gs.n AND x.deleted = 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mall_tag_tenant_code
    ON erp_mall_tag (tenant_id, tag_code) WHERE deleted = 0;

-- ── 3) 商品侧：mall_tags 由「标签名串」规范为「槽位编码串」 ──────────────────
UPDATE erp_product p
SET mall_tags = (
    SELECT string_agg(COALESCE(t.tag_code, btrim(s.v)), ',' ORDER BY s.ord)
    FROM unnest(string_to_array(p.mall_tags, ',')) WITH ORDINALITY AS s(v, ord)
    LEFT JOIN erp_mall_tag t
           ON t.tenant_id = p.tenant_id AND t.tag_name = btrim(s.v) AND t.deleted = 0
    WHERE btrim(s.v) <> ''
)
WHERE p.mall_tags IS NOT NULL AND p.mall_tags <> '' AND p.deleted = 0;
