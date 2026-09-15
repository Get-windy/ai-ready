-- ============================================================
-- V11.96.0: 费用审批（费用单多级审批流）建表脚本
--
-- 背景：费用审批 = 对《费用单》(erp_expense_doc) 的多级审批工作台。
--   P0 红线（复用单一口径）：审批复用费用单主表状态机，不另建审批引擎/审批主表；
--     审批结果落回 erp_expense_doc 的审批字段，过程留痕落 erp_expense_approval。
--   1) erp_expense_doc 增审批字段：
--      approval_status 0-未提交 1-审批中 2-审批通过 3-审批驳回；
--      approval_level 当前审批级别 1-部门 2-财务 3-总经理；
--      total_approval_level 审批总级数（默认 3）；
--      current_approver_id/name 当前审批人（审批人隔离口径）；
--      submit_time 提交时间；reject_reason 驳回原因。
--   2) erp_expense_approval 审批记录表：单号/级别/审批人/动作/意见/时间/前后状态，可追溯。
--   3) 记账门控（P1）：审批中/已驳回的费用单禁止记账，审批通过或未提交方可记账。
--   4) 幂等：ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================

-- ── 1. 费用单主表：追加审批字段 ──
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS approval_status INT DEFAULT 0;
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS approval_level INT DEFAULT 0;
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS total_approval_level INT DEFAULT 3;
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS current_approver_id BIGINT;
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS current_approver_name VARCHAR(100);
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS submit_time TIMESTAMP;
ALTER TABLE erp_expense_doc ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500);

COMMENT ON COLUMN erp_expense_doc.approval_status IS '审批状态 0-未提交 1-审批中 2-审批通过 3-审批驳回';
COMMENT ON COLUMN erp_expense_doc.approval_level IS '当前审批级别 1-部门 2-财务 3-总经理';
COMMENT ON COLUMN erp_expense_doc.total_approval_level IS '审批总级数';
COMMENT ON COLUMN erp_expense_doc.current_approver_id IS '当前审批人ID（待办隔离口径）';
COMMENT ON COLUMN erp_expense_doc.current_approver_name IS '当前审批人';
COMMENT ON COLUMN erp_expense_doc.submit_time IS '提交审批时间';
COMMENT ON COLUMN erp_expense_doc.reject_reason IS '驳回原因';

CREATE INDEX IF NOT EXISTS idx_expense_doc_approval_status ON erp_expense_doc(approval_status);
CREATE INDEX IF NOT EXISTS idx_expense_doc_approver ON erp_expense_doc(current_approver_id);

-- ── 2. 费用审批记录表（过程留痕，可经 /records 追溯） ──
CREATE TABLE IF NOT EXISTS erp_expense_approval (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    expense_doc_id BIGINT NOT NULL,                    -- 关联 erp_expense_doc.id
    doc_no VARCHAR(100),                               -- 费用单号 YBFYD-
    approval_level INT,                                -- 本次审批级别 1-部门 2-财务 3-总经理
    approver_id BIGINT,                                -- 审批人ID
    approver_name VARCHAR(100),                        -- 审批人
    approval_action VARCHAR(20),                       -- APPROVE-通过 REJECT-驳回
    approval_comment VARCHAR(500),                     -- 审批意见/驳回原因
    approval_time TIMESTAMP,                           -- 审批时间
    previous_status VARCHAR(30),                       -- 审批前状态(审批状态/级别描述)
    current_status VARCHAR(30),                        -- 审批后状态
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_expense_approval IS '费用审批记录：单号/级别/审批人/动作/意见/时间/前后状态';
COMMENT ON COLUMN erp_expense_approval.expense_doc_id IS '费用单ID';
COMMENT ON COLUMN erp_expense_approval.approval_level IS '审批级别 1-部门 2-财务 3-总经理';
COMMENT ON COLUMN erp_expense_approval.approval_action IS 'APPROVE-通过 REJECT-驳回';
COMMENT ON COLUMN erp_expense_approval.approval_comment IS '审批意见/驳回原因';

CREATE INDEX IF NOT EXISTS idx_expense_approval_doc ON erp_expense_approval(expense_doc_id);
CREATE INDEX IF NOT EXISTS idx_expense_approval_approver ON erp_expense_approval(approver_id);
CREATE INDEX IF NOT EXISTS idx_expense_approval_time ON erp_expense_approval(approval_time);
