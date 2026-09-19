-- 营销模块 → 商城弹窗广告（80323）/ 加价购（80324）：「创建人 / 制单人」列
-- 口径：与 erp_promotion_activity.creator_name（V11.373.0）同法——写入时快照姓名，
--       避免列表每次跨模块 join sys_user；存量行为 NULL（两表当前均为 0 行，无回填需求）。
ALTER TABLE mall_popup_ad ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);
ALTER TABLE mkt_addon_rule ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);

COMMENT ON COLUMN mall_popup_ad.creator_name IS '创建人姓名（对标「商城弹窗广告」页「创建人」列）';
COMMENT ON COLUMN mkt_addon_rule.creator_name IS '制单人姓名（对标「加价购」页「制单人」列）';
