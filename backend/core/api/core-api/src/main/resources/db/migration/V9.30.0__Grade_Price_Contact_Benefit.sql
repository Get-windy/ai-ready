-- =====================================================
-- V9.30.0: 等级价格表增加联系人受益（回扣/提点）字段
-- 支持两种模式：
--   DISCOUNT = 折扣+提点（公司让利，个人从折扣中获佣）
--   MARKUP   = 基准价+加点（客户加价，加的部分归个人）
-- =====================================================

ALTER TABLE erp_product_grade_price
    ADD COLUMN IF NOT EXISTS contact_partner_id BIGINT,
    ADD COLUMN IF NOT EXISTS benefit_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS benefit_rate NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS benefit_fixed NUMERIC(12, 2);

COMMENT ON COLUMN erp_product_grade_price.contact_partner_id IS '受益联系人ID（关联往来单位联系人），NULL=无回扣';
COMMENT ON COLUMN erp_product_grade_price.benefit_type IS '受益模式: DISCOUNT=折扣+提点, MARKUP=基准价+加点, NULL=无';
COMMENT ON COLUMN erp_product_grade_price.benefit_rate IS '提点/加点比例(%)，如3.00表示3%';
COMMENT ON COLUMN erp_product_grade_price.benefit_fixed IS '固定金额，不为NULL时覆盖benefit_rate计算';

CREATE INDEX IF NOT EXISTS idx_grade_price_contact ON erp_product_grade_price(contact_partner_id);
