-- V1.2.2: Add missing columns to fin_payable table (fix)
-- V1.2.1 may have been skipped or the column still missing; re-apply.
-- We also add the column for the erp_finance schema if it uses a separate table.

DO $$
BEGIN
    -- fin_payable (core-api module)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'fin_payable' AND column_name = 'supplier_name') THEN
        ALTER TABLE fin_payable ADD COLUMN supplier_name VARCHAR(500) DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'fin_payable' AND column_name = 'contract_id') THEN
        ALTER TABLE fin_payable ADD COLUMN contract_id BIGINT DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'fin_payable' AND column_name = 'contract_no') THEN
        ALTER TABLE fin_payable ADD COLUMN contract_no VARCHAR(200) DEFAULT NULL;
    END IF;
END $$;
