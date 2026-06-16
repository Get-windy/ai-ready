-- =====================================================
-- V6.10.0: 补充 fin_reconciliation 和 crm_customer 缺失字段
-- =====================================================

-- 1. fin_reconciliation 表缺少 reconciliation_type 字段
ALTER TABLE fin_reconciliation ADD COLUMN IF NOT EXISTS reconciliation_type VARCHAR(32);
COMMENT ON COLUMN fin_reconciliation.reconciliation_type IS '对账类型: BANK-银行 CUSTOMER-客户 SUPPLIER-供应商';
CREATE INDEX IF NOT EXISTS idx_reconciliation_type ON fin_reconciliation(tenant_id, reconciliation_type);

-- 2. crm_customer 表缺少 department_name 字段
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS department_name VARCHAR(200);
COMMENT ON COLUMN crm_customer.department_name IS '部门名称';
CREATE INDEX IF NOT EXISTS idx_customer_department_name ON crm_customer(tenant_id, department_name);
