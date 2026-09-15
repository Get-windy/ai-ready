-- V11.25.0 重建费用域 6 张表（JPA 子轨 cn.aiedge.erp.expense.model）
-- 说明: 早期手工残表（expense_application 46 列且缺 id 主键），与 JPA 实体严重不符，
--       查询报 "字段 ea1_0.id 不存在" 500。6 张表全部 0 行，故 DROP 后按实体定义重建。
-- 实体: ExpenseApplication / ExpenseApproval / ExpenseAttachment / ExpenseItem /
--       ExpensePayment / ExpenseReimbursement，均继承 BaseEntity
--       (id/created_by/created_at/updated_by/updated_at/deleted/version/tenant_id/remark)。
-- 幂等: 脚本以 DROP TABLE IF EXISTS ... CASCADE 开头，可重复执行。

DROP TABLE IF EXISTS expense_approval CASCADE;
DROP TABLE IF EXISTS expense_attachment CASCADE;
DROP TABLE IF EXISTS expense_item CASCADE;
DROP TABLE IF EXISTS expense_payment CASCADE;
DROP TABLE IF EXISTS expense_reimbursement CASCADE;
DROP TABLE IF EXISTS expense_application CASCADE;

-- ============================================================
-- 费用申请主表
-- ============================================================
CREATE TABLE expense_application (
    id                        BIGSERIAL PRIMARY KEY,
    created_by                VARCHAR(50),
    created_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(50),
    updated_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted                   BOOLEAN      NOT NULL DEFAULT FALSE,
    version                   INTEGER      NOT NULL DEFAULT 0,
    tenant_id                 VARCHAR(50),
    remark                    VARCHAR(500),
    application_code          VARCHAR(50)  NOT NULL,
    applicant_id              VARCHAR(50)  NOT NULL,
    applicant_name            VARCHAR(100) NOT NULL,
    department_id             VARCHAR(50),
    department_name           VARCHAR(100),
    apply_date                DATE         NOT NULL,
    expense_type              VARCHAR(20)  NOT NULL,
    expense_type_desc         VARCHAR(100),
    total_amount              NUMERIC(15,2) NOT NULL DEFAULT 0,
    currency                  VARCHAR(3)   NOT NULL DEFAULT 'CNY',
    budget_subject_id         VARCHAR(50),
    budget_subject_name       VARCHAR(200),
    budget_amount             NUMERIC(15,2),
    used_budget_amount        NUMERIC(15,2) DEFAULT 0,
    budget_usage_rate         NUMERIC(5,2)  DEFAULT 0,
    purpose                   VARCHAR(500) NOT NULL,
    description               TEXT,
    status                    VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    status_desc               VARCHAR(100),
    current_approver_id       VARCHAR(50),
    current_approver_name     VARCHAR(100),
    current_approval_level    INTEGER      DEFAULT 0,
    total_approval_level      INTEGER      DEFAULT 1,
    process_instance_id       VARCHAR(100),
    process_definition_id     VARCHAR(100),
    task_id                   VARCHAR(100),
    payment_method            VARCHAR(20),
    payment_account           VARCHAR(100),
    payment_date              DATE,
    payment_voucher_no        VARCHAR(100),
    reimbursement_date        DATE,
    reimbursement_voucher_no  VARCHAR(100),
    attachment_count          INTEGER      DEFAULT 0,
    is_urgent                 BOOLEAN      DEFAULT FALSE,
    urgent_reason             VARCHAR(500),
    expected_completion_date  DATE,
    actual_completion_date    DATE,
    exceed_budget             BOOLEAN      DEFAULT FALSE,
    exceed_amount             NUMERIC(15,2) DEFAULT 0,
    exceed_reason             VARCHAR(500),
    approval_comment          VARCHAR(1000),
    reject_reason             VARCHAR(1000),
    cancel_reason             VARCHAR(1000),
    CONSTRAINT uk_expense_application_code UNIQUE (application_code)
);
CREATE INDEX idx_expense_application_status        ON expense_application(status);
CREATE INDEX idx_expense_application_apply_date    ON expense_application(apply_date);
CREATE INDEX idx_expense_application_applicant_id  ON expense_application(applicant_id);
CREATE INDEX idx_expense_application_department_id ON expense_application(department_id);

-- ============================================================
-- 费用审批记录表
-- ============================================================
CREATE TABLE expense_approval (
    id                       BIGSERIAL PRIMARY KEY,
    created_by               VARCHAR(50),
    created_at               TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(50),
    updated_at               TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    version                  INTEGER      NOT NULL DEFAULT 0,
    tenant_id                VARCHAR(50),
    remark                   VARCHAR(500),
    application_id           VARCHAR(50)  NOT NULL,
    approval_level           INTEGER      NOT NULL,
    approver_id              VARCHAR(50)  NOT NULL,
    approver_name            VARCHAR(100) NOT NULL,
    approver_department_id   VARCHAR(50),
    approver_department_name VARCHAR(100),
    approval_action          VARCHAR(20)  NOT NULL,
    approval_comment         VARCHAR(1000),
    approval_time            TIMESTAMP    NOT NULL,
    previous_status          VARCHAR(30),
    current_status           VARCHAR(30)
);
CREATE INDEX idx_expense_approval_application_id ON expense_approval(application_id);
CREATE INDEX idx_expense_approval_approver_id    ON expense_approval(approver_id);
CREATE INDEX idx_expense_approval_approval_time  ON expense_approval(approval_time);

-- ============================================================
-- 费用附件表
-- ============================================================
CREATE TABLE expense_attachment (
    id                 BIGSERIAL PRIMARY KEY,
    created_by         VARCHAR(50),
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    version            INTEGER      NOT NULL DEFAULT 0,
    tenant_id          VARCHAR(50),
    remark             VARCHAR(500),
    application_id     VARCHAR(50)  NOT NULL,
    file_name          VARCHAR(200) NOT NULL,
    original_file_name VARCHAR(200) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size          BIGINT,
    file_type          VARCHAR(100),
    file_extension     VARCHAR(20),
    file_md5           VARCHAR(32),
    attachment_type    VARCHAR(20),
    sort_order         INTEGER      DEFAULT 0,
    upload_time        TIMESTAMP    NOT NULL,
    uploader_id        VARCHAR(50),
    uploader_name      VARCHAR(100),
    is_valid           BOOLEAN      DEFAULT TRUE
);
CREATE INDEX idx_expense_attachment_application_id ON expense_attachment(application_id);
CREATE INDEX idx_expense_attachment_file_type      ON expense_attachment(file_type);

-- ============================================================
-- 费用明细项表
-- ============================================================
CREATE TABLE expense_item (
    id                     BIGSERIAL PRIMARY KEY,
    created_by             VARCHAR(50),
    created_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by             VARCHAR(50),
    updated_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    version                INTEGER      NOT NULL DEFAULT 0,
    tenant_id              VARCHAR(50),
    remark                 VARCHAR(500),
    expense_application_id BIGINT       NOT NULL,
    item_name              VARCHAR(200) NOT NULL,
    description            VARCHAR(500),
    expense_date           DATE         NOT NULL,
    amount                 NUMERIC(15,2) NOT NULL,
    quantity               NUMERIC(10,2) DEFAULT 1,
    unit_price             NUMERIC(15,2),
    unit                   VARCHAR(20),
    vendor_name            VARCHAR(200),
    tax_rate               NUMERIC(5,2)  DEFAULT 0,
    tax_amount             NUMERIC(15,2) DEFAULT 0,
    total_amount_with_tax  NUMERIC(15,2),
    has_invoice            BOOLEAN      DEFAULT FALSE,
    invoice_number         VARCHAR(100),
    invoice_date           DATE,
    payment_method         VARCHAR(20),
    account_code           VARCHAR(50),
    budget_code            VARCHAR(50),
    project_code           VARCHAR(50),
    cost_center            VARCHAR(50),
    is_personal            BOOLEAN      DEFAULT FALSE,
    is_reimbursable        BOOLEAN      DEFAULT TRUE,
    receipt_required       BOOLEAN      DEFAULT TRUE,
    receipt_attached       BOOLEAN      DEFAULT FALSE,
    attachment_id          VARCHAR(100),
    is_verified            BOOLEAN      DEFAULT FALSE,
    verified_by            VARCHAR(50),
    verified_date          DATE,
    verification_comment   VARCHAR(500),
    sequence_number        INTEGER      DEFAULT 0
);
CREATE INDEX idx_expense_item_application_id ON expense_item(expense_application_id);

-- ============================================================
-- 费用支付表
-- ============================================================
CREATE TABLE expense_payment (
    id                  BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(50),
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50),
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    version             INTEGER      NOT NULL DEFAULT 0,
    tenant_id           VARCHAR(50),
    remark              VARCHAR(500),
    application_id      BIGINT       NOT NULL,
    amount              NUMERIC(15,2) NOT NULL DEFAULT 0,
    payment_method      VARCHAR(20),
    payment_status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    payment_date        DATE,
    payer_id            VARCHAR(50),
    payer_name          VARCHAR(100),
    voucher_no          VARCHAR(100),
    bank_transaction_no VARCHAR(100)
);
CREATE INDEX idx_expense_payment_application_id ON expense_payment(application_id);
CREATE INDEX idx_expense_payment_status         ON expense_payment(payment_status);
CREATE INDEX idx_expense_payment_payer_id       ON expense_payment(payer_id);

-- ============================================================
-- 费用报销表
-- ============================================================
CREATE TABLE expense_reimbursement (
    id                 BIGSERIAL PRIMARY KEY,
    created_by         VARCHAR(50),
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    version            INTEGER      NOT NULL DEFAULT 0,
    tenant_id          VARCHAR(50),
    remark             VARCHAR(500),
    application_id     BIGINT       NOT NULL,
    amount             NUMERIC(15,2) NOT NULL DEFAULT 0,
    reimbursement_type VARCHAR(50),
    status             VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    bank_account       VARCHAR(100),
    bank_name          VARCHAR(200),
    applicant_id       VARCHAR(50)  NOT NULL,
    applicant_name     VARCHAR(100) NOT NULL,
    department_id      VARCHAR(50),
    department_name    VARCHAR(100),
    apply_date         DATE,
    voucher_no         VARCHAR(100),
    payment_method     VARCHAR(20)
);
CREATE INDEX idx_expense_reimbursement_application_id ON expense_reimbursement(application_id);
CREATE INDEX idx_expense_reimbursement_applicant_id   ON expense_reimbursement(applicant_id);
CREATE INDEX idx_expense_reimbursement_status         ON expense_reimbursement(status);
