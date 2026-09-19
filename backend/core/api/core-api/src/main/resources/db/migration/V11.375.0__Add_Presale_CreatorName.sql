-- 营销模块 → 商城营销 → 商城预售（80322）：「商品预售」Tab「创建人」列
-- 口径同 V11.374.0：写入时快照姓名，列表不再跨模块 join sys_user。
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);

COMMENT ON COLUMN mkt_presale.creator_name IS '创建人姓名（对标「商品预售」Tab「创建人」列）';
