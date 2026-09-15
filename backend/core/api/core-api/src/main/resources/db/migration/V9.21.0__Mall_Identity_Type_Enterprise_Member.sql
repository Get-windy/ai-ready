-- ============================================================
-- V9.21.0 订货商城身份体系改造
-- 统一登录入口 + 身份自动识别
-- shop_user.user_type: ENTERPRISE(企业客户) / MEMBER(个人会员)
-- ============================================================

-- 1. shop_user 增加身份类型
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'MEMBER';
COMMENT ON COLUMN shop_user.user_type IS '用户身份类型：ENTERPRISE=企业客户(走B2B销售订单) MEMBER=个人会员(走零售/商城订单)';

-- 2. erp_customer_id 改名为 party_id，统一指向 biz_party.id
--    保持向后兼容：erp_customer_id 保留为冗余字段（不删除，只加注释）
COMMENT ON COLUMN shop_user.erp_customer_id IS '已废弃，使用 party_id';
COMMENT ON COLUMN shop_user.erp_partner_id IS '已废弃，使用 party_id';
ALTER TABLE shop_user ADD COLUMN IF NOT EXISTS party_id BIGINT;
COMMENT ON COLUMN shop_user.party_id IS '关联 biz_party.id，统一身份标识';
CREATE INDEX IF NOT EXISTS idx_shop_user_party ON shop_user(party_id);
CREATE INDEX IF NOT EXISTS idx_shop_user_user_type ON shop_user(user_type);

-- 3. 回填已有数据的 user_type
--    有 company_name 的视为企业客户，其余视为个人会员
UPDATE shop_user
SET user_type = CASE
    WHEN company_name IS NOT NULL AND company_name != '' THEN 'ENTERPRISE'
    ELSE 'MEMBER'
END
WHERE user_type IS NULL;

-- 4. 回填 party_id：优先用 erp_partner_id，其次用 erp_customer_id
UPDATE shop_user
SET party_id = COALESCE(erp_partner_id, erp_customer_id)
WHERE party_id IS NULL AND (erp_partner_id IS NOT NULL OR erp_customer_id IS NOT NULL);
