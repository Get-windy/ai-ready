-- V8.3.0: Partner 表单增强字段
-- 新增：头像、默认经手人、会员信息、经营系列/面积

-- 头像（客户/供应商/物流等通用）
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS partner_avatar VARCHAR(500);

-- 默认经手人（关联系统用户/员工）
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS default_handler_id BIGINT;
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS default_handler_name VARCHAR(100);

-- 会员信息（客户专用）
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS member_card_no VARCHAR(100);
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS member_name VARCHAR(200);
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS initial_points INTEGER DEFAULT 0;

-- 经营系列/面积（供应商/物流等）
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS operating_series VARCHAR(200);
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS operating_area DECIMAL(10,2);
