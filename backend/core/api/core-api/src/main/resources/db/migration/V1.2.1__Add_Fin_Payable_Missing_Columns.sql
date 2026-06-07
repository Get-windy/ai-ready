-- V1.2.1: Add missing columns to fin_payable table
-- These columns exist in MyBatis Plus entities but were missing from the database schema.

-- ============================================================================
-- fin_payable
-- ============================================================================
ALTER TABLE IF EXISTS fin_payable
    ADD COLUMN IF NOT EXISTS supplier_name       VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS contract_no         VARCHAR(200) DEFAULT NULL;
