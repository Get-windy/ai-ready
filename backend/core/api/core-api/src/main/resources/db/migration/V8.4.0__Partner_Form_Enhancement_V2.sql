-- V8.4.0: Partner 表单增强字段 V2
-- 新增：公司全称、公司邮箱、所属行业、来源渠道

-- 公司全称（区别于 partner_name 业务名称，用于税务/合同等）
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS company_full_name VARCHAR(200);

-- 公司邮箱
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS company_email VARCHAR(200);

-- 所属行业
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS industry VARCHAR(100);

-- 来源渠道
ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS source_channel VARCHAR(50);
