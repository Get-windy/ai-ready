-- V1.2.0: Add missing columns to supplier-related tables
-- These columns exist in MyBatis Plus entities but were missing from the database schema.

-- ============================================================================
-- erp_supplier
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier
    ADD COLUMN IF NOT EXISTS supplier_type        INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS enterprise_nature     INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS credit_code           VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS business_license      VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS legal_person          VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS registered_capital    DOUBLE PRECISION DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS establishment_date    TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS business_scope        TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS company_address       VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS postal_code           VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS website               VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS invoice_type          INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS payment_method        INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS payment_period        INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS transport_method      INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS cooperation_status    INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS supplier_level        VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS comprehensive_score   DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS certification_status  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS portal_status         INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS portal_account_id     VARCHAR(200) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version               INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS category_tags         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS location_info         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS attachment_info       TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS extend_info           TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_performance
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_performance
    ADD COLUMN IF NOT EXISTS supplier_code           VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluation_type         INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluation_date         TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluator_id            VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluator_name          VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS price_score             DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS technology_score        DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS response_score          DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS compliance_score        DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS comprehensive_score     DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS performance_level        VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS level_change            INTEGER DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS quality_deductions       TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS delivery_deductions      TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS service_deductions       TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS deductions               DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS strengths               TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS improvement_suggestions  TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluation_conclusion    TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS attachment_info          TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS status                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS remark                  TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version                 INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info             TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_level
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_level
    ADD COLUMN IF NOT EXISTS level_description    TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS level_icon           VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS level_color          VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS evaluation_period    VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS benefit_count        INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_discount_rate    DECIMAL(10,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS priority_level       INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_active            BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS sort_order           INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_time         TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_by           VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_time         TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_by           VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version              INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted              INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info          TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_notification
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_notification
    ADD COLUMN IF NOT EXISTS notification_no         VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS supplier_code           VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS supplier_name           VARCHAR(200) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS notification_title      VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS notification_content    TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS notification_subtype    VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS business_id             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS business_no             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS notification_priority   INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notification_channel    INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notification_status     INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sender_id               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sender_name             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sender_department       VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS receiver_id             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS receiver_name           VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS receiver_email          VARCHAR(200) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS receiver_phone          VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS planned_send_time       TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS actual_send_time        TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS process_time            TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS process_result          INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS process_comment         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS validity_period         INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS require_receipt         BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_receipt             BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS receipt_time            TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS receipt_content         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS template_id             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS template_params         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS send_log                TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS update_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version                 INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted                 INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info             TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_benefit
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_benefit
    ADD COLUMN IF NOT EXISTS benefit_code             VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS benefit_subtype          VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS benefit_description      TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS benefit_unit             VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS applicable_level_codes   TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS applicable_supplier_types TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS usage_condition_type     VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS usage_condition_value    DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS usage_limit_type         VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS usage_limit_value        DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS exchange_points          DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auto_issue               BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS issue_time_type          VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS issue_time_expression    VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS is_active                BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS sort_order               INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS effective_start_time     TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS effective_end_time       TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info              TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_points_record
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_points_record
    ADD COLUMN IF NOT EXISTS supplier_code            VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS rule_code                VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS record_type              VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS points_amount            DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS record_description       TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS related_business_type    VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS related_business_id      VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS related_business_code    VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS business_time            TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS record_date              TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS effective_time           TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS expire_time              TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS current_balance          DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS before_level_code        VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS after_level_code         VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS operator_id              VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS operator_name            VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS approval_status          VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS approver_id              VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS approver_name            VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS approval_time            TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS approval_comment         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info              TEXT DEFAULT NULL;

-- ============================================================================
-- erp_supplier_points_rule
-- ============================================================================
ALTER TABLE IF EXISTS erp_supplier_points_rule
    ADD COLUMN IF NOT EXISTS rule_description         TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS rule_type                VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS calculation_method        VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS base_points              DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS points_factor            DECIMAL(10,4) DEFAULT 1,
    ADD COLUMN IF NOT EXISTS trigger_condition_type   VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS trigger_condition_value  DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applicable_level_type    VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS applicable_level_codes   TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS applicable_supplier_type VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS applicable_supplier_types TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS effective_start_time     TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS effective_end_time       TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS daily_limit              DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS monthly_limit            DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS yearly_limit             DECIMAL(20,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_active                BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS priority                 INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_time             TIMESTAMP DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS updated_by               VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS version                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted                  INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extend_info              TEXT DEFAULT NULL;
