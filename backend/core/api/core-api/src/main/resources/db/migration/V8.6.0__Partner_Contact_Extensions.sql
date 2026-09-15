-- V8.6.0: Add delivery and mall account fields to partner contact
-- Add delivery method, route and mall account fields to erp_partner_contact table

ALTER TABLE erp_partner_contact ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50);
ALTER TABLE erp_partner_contact ADD COLUMN IF NOT EXISTS delivery_route VARCHAR(200);
ALTER TABLE erp_partner_contact ADD COLUMN IF NOT EXISTS open_mall_account INTEGER DEFAULT 0;