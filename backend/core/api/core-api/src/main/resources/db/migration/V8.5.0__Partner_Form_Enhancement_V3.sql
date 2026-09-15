-- V8.5.0: Partner form enhancement V3
-- Add opening_prepaid, contact region/detail_address

ALTER TABLE erp_partner ADD COLUMN IF NOT EXISTS opening_prepaid DECIMAL(20,2) DEFAULT 0;

ALTER TABLE erp_partner_contact ADD COLUMN IF NOT EXISTS region VARCHAR(100);
ALTER TABLE erp_partner_contact ADD COLUMN IF NOT EXISTS detail_address VARCHAR(500);
