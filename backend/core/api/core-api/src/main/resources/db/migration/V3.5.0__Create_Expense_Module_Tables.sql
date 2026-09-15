-- ============================================================
-- V3.5.0 费用管理模块数据库表
-- 表前缀: fee_ 表示费用管理（Expense / Fee Management）
-- 设计原则:
--   1. 复用 MyBatis-Plus 规范字段
--   2. 金额统一使用 DECIMAL(20,2)
--   3. 枚举使用 VARCHAR 存储
--   4. 逻辑删除使用 deleted INTEGER DEFAULT 0
--   5. 每表包含 tenant_id, create_by, create_time, update_by, update_time, remark
-- ============================================================

-- ============================================================
-- 1. 费用申请表 fee_application
-- 保存费用申请单主信息
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_application (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    -- 单据信息
    application_no VARCHAR(50) NOT NULL,
    application_title VARCHAR(200) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    department_id BIGINT,
    department_name VARCHAR(100),

    -- 费用信息
    expense_type VARCHAR(30) NOT NULL,
    -- TRAVEL, OFFICE_SUPPLIES, MEETING, ENTERTAINMENT, TRANSPORTATION,
    -- COMMUNICATION, TRAINING, CONSULTING, ADVERTISING, R_D,
    -- EQUIPMENT, MAINTENANCE, RENTAL, INSURANCE, TAX, OTHER
    expense_type_desc VARCHAR(100),
    total_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',

    -- 预算信息
    budget_amount DECIMAL(20,2) DEFAULT 0.00,
    budget_used DECIMAL(20,2) DEFAULT 0.00,
    budget_usage_rate DECIMAL(5,2) DEFAULT 0.00,
    exceed_budget INTEGER NOT NULL DEFAULT 0,
    exceed_amount DECIMAL(20,2) DEFAULT 0.00,
    exceed_reason VARCHAR(500),

    -- 申请信息
    apply_date DATE NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    description TEXT,
    is_urgent INTEGER NOT NULL DEFAULT 0,
    urgent_reason VARCHAR(500),
    expected_completion_date DATE,
    actual_completion_date DATE,
    attachment_count INTEGER DEFAULT 0,

    -- 审批信息
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- DRAFT, SUBMITTED, APPROVING, APPROVED, REJECTED, CANCELLED, WITHDRAWN
    current_approver_id BIGINT,
    current_approver_name VARCHAR(100),
    current_approval_level INTEGER DEFAULT 0,
    total_approval_level INTEGER DEFAULT 1,
    approval_comment TEXT,
    reject_reason VARCHAR(1000),
    cancel_reason VARCHAR(1000),

    -- 工作流关联
    process_instance_id VARCHAR(100),
    process_definition_id VARCHAR(100),

    -- 报销关联
    reimbursement_status VARCHAR(30) DEFAULT 'NONE',
    -- NONE, PARTIAL, COMPLETED
    reimbursed_amount DECIMAL(20,2) DEFAULT 0.00,
    reimbursement_date DATE,

    -- 支付信息
    payment_status VARCHAR(30) DEFAULT 'NONE',
    -- NONE, PENDING, PARTIAL, COMPLETED
    paid_amount DECIMAL(20,2) DEFAULT 0.00,

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_application IS '费用申请表';
COMMENT ON COLUMN fee_application.application_no IS '申请单号';
COMMENT ON COLUMN fee_application.application_title IS '申请标题';
COMMENT ON COLUMN fee_application.applicant_id IS '申请人ID';
COMMENT ON COLUMN fee_application.applicant_name IS '申请人姓名';
COMMENT ON COLUMN fee_application.department_id IS '部门ID';
COMMENT ON COLUMN fee_application.department_name IS '部门名称';
COMMENT ON COLUMN fee_application.expense_type IS '费用类型';
COMMENT ON COLUMN fee_application.expense_type_desc IS '费用类型描述';
COMMENT ON COLUMN fee_application.total_amount IS '费用总金额';
COMMENT ON COLUMN fee_application.currency IS '币种';
COMMENT ON COLUMN fee_application.budget_amount IS '预算金额';
COMMENT ON COLUMN fee_application.budget_used IS '已使用预算';
COMMENT ON COLUMN fee_application.budget_usage_rate IS '预算使用率(%)';
COMMENT ON COLUMN fee_application.exceed_budget IS '是否超出预算(0=否 1=是)';
COMMENT ON COLUMN fee_application.exceed_amount IS '超出金额';
COMMENT ON COLUMN fee_application.exceed_reason IS '超出原因';
COMMENT ON COLUMN fee_application.apply_date IS '申请日期';
COMMENT ON COLUMN fee_application.purpose IS '费用事由';
COMMENT ON COLUMN fee_application.description IS '详细说明';
COMMENT ON COLUMN fee_application.is_urgent IS '是否紧急(0=否 1=是)';
COMMENT ON COLUMN fee_application.urgent_reason IS '紧急原因';
COMMENT ON COLUMN fee_application.expected_completion_date IS '预计完成日期';
COMMENT ON COLUMN fee_application.actual_completion_date IS '实际完成日期';
COMMENT ON COLUMN fee_application.attachment_count IS '附件数量';
COMMENT ON COLUMN fee_application.status IS '状态(DRAFT/SUBMITTED/APPROVING/APPROVED/REJECTED/CANCELLED/WITHDRAWN)';
COMMENT ON COLUMN fee_application.current_approver_id IS '当前审批人ID';
COMMENT ON COLUMN fee_application.current_approver_name IS '当前审批人姓名';
COMMENT ON COLUMN fee_application.current_approval_level IS '当前审批级别';
COMMENT ON COLUMN fee_application.total_approval_level IS '总审批级别数';
COMMENT ON COLUMN fee_application.approval_comment IS '审批意见';
COMMENT ON COLUMN fee_application.reject_reason IS '拒绝原因';
COMMENT ON COLUMN fee_application.cancel_reason IS '取消原因';
COMMENT ON COLUMN fee_application.process_instance_id IS '工作流实例ID';
COMMENT ON COLUMN fee_application.process_definition_id IS '工作流定义ID';
COMMENT ON COLUMN fee_application.reimbursement_status IS '报销状态(NONE/PARTIAL/COMPLETED)';
COMMENT ON COLUMN fee_application.reimbursed_amount IS '已报销金额';
COMMENT ON COLUMN fee_application.reimbursement_date IS '报销日期';
COMMENT ON COLUMN fee_application.payment_status IS '付款状态(NONE/PENDING/PARTIAL/COMPLETED)';
COMMENT ON COLUMN fee_application.paid_amount IS '已付款金额';

CREATE INDEX IF NOT EXISTS idx_fee_app_no ON fee_application(application_no);
CREATE INDEX IF NOT EXISTS idx_fee_app_applicant ON fee_application(applicant_id);
CREATE INDEX IF NOT EXISTS idx_fee_app_dept ON fee_application(department_id);
CREATE INDEX IF NOT EXISTS idx_fee_app_status ON fee_application(status);
CREATE INDEX IF NOT EXISTS idx_fee_app_type ON fee_application(expense_type);
CREATE INDEX IF NOT EXISTS idx_fee_app_date ON fee_application(apply_date);
CREATE INDEX IF NOT EXISTS idx_fee_app_tenant ON fee_application(tenant_id);

-- ============================================================
-- 2. 费用申请明细表 fee_application_item
-- 保存费用申请的每一条明细行
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_application_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    application_id BIGINT NOT NULL,

    -- 明细信息
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    expense_date DATE NOT NULL,
    expense_type VARCHAR(30),
    expense_type_desc VARCHAR(100),
    amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    quantity DECIMAL(10,2) DEFAULT 1.00,
    unit_price DECIMAL(20,2) DEFAULT 0.00,
    unit VARCHAR(20),
    vendor_name VARCHAR(200),

    -- 税务信息
    tax_rate DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(20,2) DEFAULT 0.00,
    total_amount_with_tax DECIMAL(20,2) DEFAULT 0.00,
    has_invoice INTEGER NOT NULL DEFAULT 0,
    invoice_number VARCHAR(100),
    invoice_date DATE,

    -- 辅助核算
    account_code VARCHAR(50),
    budget_code VARCHAR(50),
    project_code VARCHAR(50),
    cost_center VARCHAR(50),
    is_personal INTEGER NOT NULL DEFAULT 0,
    is_reimbursable INTEGER NOT NULL DEFAULT 1,
    receipt_required INTEGER NOT NULL DEFAULT 1,
    receipt_attached INTEGER NOT NULL DEFAULT 0,

    -- 核验信息
    is_verified INTEGER NOT NULL DEFAULT 0,
    verified_by BIGINT,
    verified_date DATE,
    verification_comment VARCHAR(500),

    sequence_number INTEGER DEFAULT 0,

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_application_item IS '费用申请明细表';
COMMENT ON COLUMN fee_application_item.application_id IS '费用申请ID';
COMMENT ON COLUMN fee_application_item.item_name IS '费用项目名称';
COMMENT ON COLUMN fee_application_item.description IS '费用说明';
COMMENT ON COLUMN fee_application_item.expense_date IS '费用发生日期';
COMMENT ON COLUMN fee_application_item.expense_type IS '费用类型';
COMMENT ON COLUMN fee_application_item.expense_type_desc IS '费用类型描述';
COMMENT ON COLUMN fee_application_item.amount IS '金额(不含税)';
COMMENT ON COLUMN fee_application_item.quantity IS '数量';
COMMENT ON COLUMN fee_application_item.unit_price IS '单价';
COMMENT ON COLUMN fee_application_item.unit IS '单位';
COMMENT ON COLUMN fee_application_item.vendor_name IS '供应商/收款方';
COMMENT ON COLUMN fee_application_item.tax_rate IS '税率(%)';
COMMENT ON COLUMN fee_application_item.tax_amount IS '税额';
COMMENT ON COLUMN fee_application_item.total_amount_with_tax IS '含税总金额';
COMMENT ON COLUMN fee_application_item.has_invoice IS '是否有发票(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.invoice_number IS '发票号码';
COMMENT ON COLUMN fee_application_item.invoice_date IS '发票日期';
COMMENT ON COLUMN fee_application_item.account_code IS '会计科目编码';
COMMENT ON COLUMN fee_application_item.budget_code IS '预算科目编码';
COMMENT ON COLUMN fee_application_item.project_code IS '项目编码';
COMMENT ON COLUMN fee_application_item.cost_center IS '成本中心';
COMMENT ON COLUMN fee_application_item.is_personal IS '是否个人垫付(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.is_reimbursable IS '是否可报销(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.receipt_required IS '是否需要收据(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.receipt_attached IS '收据已附(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.is_verified IS '是否已核验(0=否 1=是)';
COMMENT ON COLUMN fee_application_item.verified_by IS '核验人ID';
COMMENT ON COLUMN fee_application_item.verified_date IS '核验日期';
COMMENT ON COLUMN fee_application_item.verification_comment IS '核验意见';
COMMENT ON COLUMN fee_application_item.sequence_number IS '序号';

CREATE INDEX IF NOT EXISTS idx_fee_app_item_app ON fee_application_item(application_id);
CREATE INDEX IF NOT EXISTS idx_fee_app_item_verified ON fee_application_item(is_verified);
CREATE INDEX IF NOT EXISTS idx_fee_app_item_type ON fee_application_item(expense_type);

-- ============================================================
-- 3. 费用报销单表 fee_reimbursement
-- 保存报销单主信息，可与费用申请关联
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_reimbursement (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    -- 单据信息
    reimbursement_no VARCHAR(50) NOT NULL,
    reimbursement_title VARCHAR(200) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    department_id BIGINT,
    department_name VARCHAR(100),

    -- 关联申请
    application_id BIGINT,
    application_no VARCHAR(50),

    -- 费用信息
    total_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    reimbursement_date DATE NOT NULL,
    purpose VARCHAR(500),
    description TEXT,
    attachment_count INTEGER DEFAULT 0,

    -- 审批信息
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- DRAFT, SUBMITTED, APPROVING, APPROVED, REJECTED, CANCELLED, WITHDRAWN
    current_approver_id BIGINT,
    current_approver_name VARCHAR(100),
    current_approval_level INTEGER DEFAULT 0,
    total_approval_level INTEGER DEFAULT 1,
    approval_comment TEXT,
    reject_reason VARCHAR(1000),
    cancel_reason VARCHAR(1000),

    -- 工作流关联
    process_instance_id VARCHAR(100),
    process_definition_id VARCHAR(100),

    -- 付款信息
    payment_method VARCHAR(30),
    -- CASH, BANK_TRANSFER, CHECK, ALIPAY, WECHAT, CREDIT_CARD, DEBIT_CARD, OTHER
    payment_account VARCHAR(100),
    payment_status VARCHAR(30) DEFAULT 'NONE',
    -- NONE, PENDING, COMPLETED
    paid_amount DECIMAL(20,2) DEFAULT 0.00,
    payment_date DATE,
    payment_voucher_no VARCHAR(100),

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_reimbursement IS '费用报销单表';
COMMENT ON COLUMN fee_reimbursement.reimbursement_no IS '报销单号';
COMMENT ON COLUMN fee_reimbursement.reimbursement_title IS '报销标题';
COMMENT ON COLUMN fee_reimbursement.applicant_id IS '报销人ID';
COMMENT ON COLUMN fee_reimbursement.applicant_name IS '报销人姓名';
COMMENT ON COLUMN fee_reimbursement.department_id IS '部门ID';
COMMENT ON COLUMN fee_reimbursement.department_name IS '部门名称';
COMMENT ON COLUMN fee_reimbursement.application_id IS '关联费用申请ID';
COMMENT ON COLUMN fee_reimbursement.application_no IS '关联申请单号';
COMMENT ON COLUMN fee_reimbursement.total_amount IS '报销总金额';
COMMENT ON COLUMN fee_reimbursement.currency IS '币种';
COMMENT ON COLUMN fee_reimbursement.reimbursement_date IS '报销日期';
COMMENT ON COLUMN fee_reimbursement.purpose IS '报销事由';
COMMENT ON COLUMN fee_reimbursement.description IS '详细说明';
COMMENT ON COLUMN fee_reimbursement.attachment_count IS '附件数量';
COMMENT ON COLUMN fee_reimbursement.status IS '状态(DRAFT/SUBMITTED/APPROVING/APPROVED/REJECTED/CANCELLED/WITHDRAWN)';
COMMENT ON COLUMN fee_reimbursement.current_approver_id IS '当前审批人ID';
COMMENT ON COLUMN fee_reimbursement.current_approver_name IS '当前审批人姓名';
COMMENT ON COLUMN fee_reimbursement.current_approval_level IS '当前审批级别';
COMMENT ON COLUMN fee_reimbursement.total_approval_level IS '总审批级别数';
COMMENT ON COLUMN fee_reimbursement.approval_comment IS '审批意见';
COMMENT ON COLUMN fee_reimbursement.reject_reason IS '拒绝原因';
COMMENT ON COLUMN fee_reimbursement.cancel_reason IS '取消原因';
COMMENT ON COLUMN fee_reimbursement.process_instance_id IS '工作流实例ID';
COMMENT ON COLUMN fee_reimbursement.process_definition_id IS '工作流定义ID';
COMMENT ON COLUMN fee_reimbursement.payment_method IS '支付方式';
COMMENT ON COLUMN fee_reimbursement.payment_account IS '支付账户';
COMMENT ON COLUMN fee_reimbursement.payment_status IS '付款状态(NONE/PENDING/COMPLETED)';
COMMENT ON COLUMN fee_reimbursement.paid_amount IS '已付款金额';
COMMENT ON COLUMN fee_reimbursement.payment_date IS '付款日期';
COMMENT ON COLUMN fee_reimbursement.payment_voucher_no IS '付款凭证号';

CREATE INDEX IF NOT EXISTS idx_fee_reimb_no ON fee_reimbursement(reimbursement_no);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_applicant ON fee_reimbursement(applicant_id);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_dept ON fee_reimbursement(department_id);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_status ON fee_reimbursement(status);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_app_id ON fee_reimbursement(application_id);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_date ON fee_reimbursement(reimbursement_date);

-- ============================================================
-- 4. 费用报销明细表 fee_reimbursement_item
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_reimbursement_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    reimbursement_id BIGINT NOT NULL,
    application_item_id BIGINT,

    -- 明细信息
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    expense_date DATE NOT NULL,
    amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    quantity DECIMAL(10,2) DEFAULT 1.00,
    unit_price DECIMAL(20,2) DEFAULT 0.00,
    unit VARCHAR(20),
    vendor_name VARCHAR(200),

    -- 发票信息
    has_invoice INTEGER NOT NULL DEFAULT 0,
    invoice_number VARCHAR(100),
    invoice_date DATE,
    tax_rate DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(20,2) DEFAULT 0.00,
    total_amount_with_tax DECIMAL(20,2) DEFAULT 0.00,

    -- 辅助核算
    account_code VARCHAR(50),
    budget_code VARCHAR(50),
    project_code VARCHAR(50),
    cost_center VARCHAR(50),

    sequence_number INTEGER DEFAULT 0,

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_reimbursement_item IS '费用报销明细表';
COMMENT ON COLUMN fee_reimbursement_item.reimbursement_id IS '报销单ID';
COMMENT ON COLUMN fee_reimbursement_item.application_item_id IS '关联申请明细ID';
COMMENT ON COLUMN fee_reimbursement_item.item_name IS '费用项目名称';
COMMENT ON COLUMN fee_reimbursement_item.description IS '费用说明';
COMMENT ON COLUMN fee_reimbursement_item.expense_date IS '费用发生日期';
COMMENT ON COLUMN fee_reimbursement_item.amount IS '金额(不含税)';
COMMENT ON COLUMN fee_reimbursement_item.quantity IS '数量';
COMMENT ON COLUMN fee_reimbursement_item.unit_price IS '单价';
COMMENT ON COLUMN fee_reimbursement_item.unit IS '单位';
COMMENT ON COLUMN fee_reimbursement_item.vendor_name IS '供应商/收款方';
COMMENT ON COLUMN fee_reimbursement_item.has_invoice IS '是否有发票(0=否 1=是)';
COMMENT ON COLUMN fee_reimbursement_item.invoice_number IS '发票号码';
COMMENT ON COLUMN fee_reimbursement_item.invoice_date IS '发票日期';
COMMENT ON COLUMN fee_reimbursement_item.tax_rate IS '税率(%)';
COMMENT ON COLUMN fee_reimbursement_item.tax_amount IS '税额';
COMMENT ON COLUMN fee_reimbursement_item.total_amount_with_tax IS '含税总金额';
COMMENT ON COLUMN fee_reimbursement_item.account_code IS '会计科目编码';
COMMENT ON COLUMN fee_reimbursement_item.budget_code IS '预算科目编码';
COMMENT ON COLUMN fee_reimbursement_item.project_code IS '项目编码';
COMMENT ON COLUMN fee_reimbursement_item.cost_center IS '成本中心';
COMMENT ON COLUMN fee_reimbursement_item.sequence_number IS '序号';

CREATE INDEX IF NOT EXISTS idx_fee_reimb_item_reimb ON fee_reimbursement_item(reimbursement_id);
CREATE INDEX IF NOT EXISTS idx_fee_reimb_item_app_item ON fee_reimbursement_item(application_item_id);

-- ============================================================
-- 5. 审批记录表 fee_approval_record
-- 通用审批记录，通过 business_type + business_id 关联
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_approval_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    business_type VARCHAR(30) NOT NULL,
    -- APPLICATION, REIMBURSEMENT
    business_id BIGINT NOT NULL,
    approval_level INTEGER NOT NULL,
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100) NOT NULL,
    approver_department_id BIGINT,
    approver_department_name VARCHAR(100),

    approval_action VARCHAR(20) NOT NULL,
    -- SUBMIT, APPROVE, REJECT, RETURN, TRANSFER, WITHDRAW, CANCEL
    approval_comment VARCHAR(1000),
    approval_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    previous_status VARCHAR(30),
    current_status VARCHAR(30),

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_approval_record IS '审批记录表';
COMMENT ON COLUMN fee_approval_record.business_type IS '业务类型(APPLICATION=费用申请 REIMBURSEMENT=费用报销)';
COMMENT ON COLUMN fee_approval_record.business_id IS '业务单据ID';
COMMENT ON COLUMN fee_approval_record.approval_level IS '审批级别';
COMMENT ON COLUMN fee_approval_record.approver_id IS '审批人ID';
COMMENT ON COLUMN fee_approval_record.approver_name IS '审批人姓名';
COMMENT ON COLUMN fee_approval_record.approver_department_id IS '审批人部门ID';
COMMENT ON COLUMN fee_approval_record.approver_department_name IS '审批人部门名称';
COMMENT ON COLUMN fee_approval_record.approval_action IS '审批动作(SUBMIT/APPROVE/REJECT/RETURN/TRANSFER/WITHDRAW/CANCEL)';
COMMENT ON COLUMN fee_approval_record.approval_comment IS '审批意见';
COMMENT ON COLUMN fee_approval_record.approval_time IS '审批时间';
COMMENT ON COLUMN fee_approval_record.assignee_id IS '转交人ID';
COMMENT ON COLUMN fee_approval_record.assignee_name IS '转交人姓名';
COMMENT ON COLUMN fee_approval_record.previous_status IS '前状态';
COMMENT ON COLUMN fee_approval_record.current_status IS '当前状态';

CREATE INDEX IF NOT EXISTS idx_fee_appr_biz ON fee_approval_record(business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_fee_appr_approver ON fee_approval_record(approver_id);
CREATE INDEX IF NOT EXISTS idx_fee_appr_time ON fee_approval_record(approval_time);

-- ============================================================
-- 6. 付款记录表 fee_payment_record
-- 保存费用申请/报销的付款确认记录
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_payment_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    business_type VARCHAR(30) NOT NULL,
    -- APPLICATION, REIMBURSEMENT
    business_id BIGINT NOT NULL,
    business_no VARCHAR(50),

    payment_no VARCHAR(50) NOT NULL,
    payment_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    payment_method VARCHAR(30),
    -- CASH, BANK_TRANSFER, CHECK, ALIPAY, WECHAT, CREDIT_CARD, DEBIT_CARD, OTHER
    payment_account VARCHAR(100),
    account_name VARCHAR(100),
    payee_name VARCHAR(100),
    payee_account VARCHAR(100),
    payment_date DATE NOT NULL,
    voucher_no VARCHAR(100),
    payer_id BIGINT,
    payer_name VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING, COMPLETED, FAILED, CANCELLED
    fail_reason VARCHAR(500),
    confirm_time TIMESTAMP,
    confirm_user_id BIGINT,
    confirm_user_name VARCHAR(100),

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_payment_record IS '付款记录表';
COMMENT ON COLUMN fee_payment_record.business_type IS '业务类型(APPLICATION=费用申请 REIMBURSEMENT=费用报销)';
COMMENT ON COLUMN fee_payment_record.business_id IS '业务单据ID';
COMMENT ON COLUMN fee_payment_record.business_no IS '业务单据编号';
COMMENT ON COLUMN fee_payment_record.payment_no IS '付款单号';
COMMENT ON COLUMN fee_payment_record.payment_amount IS '付款金额';
COMMENT ON COLUMN fee_payment_record.currency IS '币种';
COMMENT ON COLUMN fee_payment_record.payment_method IS '支付方式';
COMMENT ON COLUMN fee_payment_record.payment_account IS '付款账户';
COMMENT ON COLUMN fee_payment_record.account_name IS '付款账户名称';
COMMENT ON COLUMN fee_payment_record.payee_name IS '收款人名称';
COMMENT ON COLUMN fee_payment_record.payee_account IS '收款人账户';
COMMENT ON COLUMN fee_payment_record.payment_date IS '付款日期';
COMMENT ON COLUMN fee_payment_record.voucher_no IS '凭证号';
COMMENT ON COLUMN fee_payment_record.payer_id IS '付款人ID';
COMMENT ON COLUMN fee_payment_record.payer_name IS '付款人姓名';
COMMENT ON COLUMN fee_payment_record.status IS '状态(PENDING/COMPLETED/FAILED/CANCELLED)';
COMMENT ON COLUMN fee_payment_record.fail_reason IS '失败原因';
COMMENT ON COLUMN fee_payment_record.confirm_time IS '确认时间';
COMMENT ON COLUMN fee_payment_record.confirm_user_id IS '确认人ID';
COMMENT ON COLUMN fee_payment_record.confirm_user_name IS '确认人姓名';

CREATE INDEX IF NOT EXISTS idx_fee_pay_biz ON fee_payment_record(business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_fee_pay_no ON fee_payment_record(payment_no);
CREATE INDEX IF NOT EXISTS idx_fee_pay_status ON fee_payment_record(status);
CREATE INDEX IF NOT EXISTS idx_fee_pay_date ON fee_payment_record(payment_date);

-- ============================================================
-- 7. 费用统计台账表 fee_statistics
-- 按月/部门/费用类型的汇总统计数据
-- ============================================================
CREATE TABLE IF NOT EXISTS fee_statistics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,

    stat_year INTEGER NOT NULL,
    stat_month INTEGER NOT NULL,
    stat_date DATE NOT NULL,

    department_id BIGINT,
    department_name VARCHAR(100),
    expense_type VARCHAR(30),

    apply_count INTEGER DEFAULT 0,
    apply_amount DECIMAL(20,2) DEFAULT 0.00,
    approved_count INTEGER DEFAULT 0,
    approved_amount DECIMAL(20,2) DEFAULT 0.00,
    rejected_count INTEGER DEFAULT 0,
    rejected_amount DECIMAL(20,2) DEFAULT 0.00,

    reimbursement_count INTEGER DEFAULT 0,
    reimbursement_amount DECIMAL(20,2) DEFAULT 0.00,
    paid_count INTEGER DEFAULT 0,
    paid_amount DECIMAL(20,2) DEFAULT 0.00,

    budget_amount DECIMAL(20,2) DEFAULT 0.00,
    budget_usage_rate DECIMAL(5,2) DEFAULT 0.00,

    -- 基础字段
    remark VARCHAR(500),
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);

COMMENT ON TABLE fee_statistics IS '费用统计台账表';
COMMENT ON COLUMN fee_statistics.stat_year IS '统计年份';
COMMENT ON COLUMN fee_statistics.stat_month IS '统计月份';
COMMENT ON COLUMN fee_statistics.stat_date IS '统计日期';
COMMENT ON COLUMN fee_statistics.department_id IS '部门ID(NULL=全部)';
COMMENT ON COLUMN fee_statistics.department_name IS '部门名称';
COMMENT ON COLUMN fee_statistics.expense_type IS '费用类型(NULL=全部)';
COMMENT ON COLUMN fee_statistics.apply_count IS '申请数量';
COMMENT ON COLUMN fee_statistics.apply_amount IS '申请金额';
COMMENT ON COLUMN fee_statistics.approved_count IS '已审批数量';
COMMENT ON COLUMN fee_statistics.approved_amount IS '已审批金额';
COMMENT ON COLUMN fee_statistics.rejected_count IS '已拒绝数量';
COMMENT ON COLUMN fee_statistics.rejected_amount IS '已拒绝金额';
COMMENT ON COLUMN fee_statistics.reimbursement_count IS '报销数量';
COMMENT ON COLUMN fee_statistics.reimbursement_amount IS '报销金额';
COMMENT ON COLUMN fee_statistics.paid_count IS '已付款数量';
COMMENT ON COLUMN fee_statistics.paid_amount IS '已付款金额';
COMMENT ON COLUMN fee_statistics.budget_amount IS '预算金额';
COMMENT ON COLUMN fee_statistics.budget_usage_rate IS '预算使用率(%)';

CREATE INDEX IF NOT EXISTS idx_fee_stat_date ON fee_statistics(stat_year, stat_month);
CREATE INDEX IF NOT EXISTS idx_fee_stat_dept ON fee_statistics(department_id);
CREATE INDEX IF NOT EXISTS idx_fee_stat_type ON fee_statistics(expense_type);
