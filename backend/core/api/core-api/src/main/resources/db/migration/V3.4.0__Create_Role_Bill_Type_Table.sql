-- ============================================================
-- 角色-单据类型权限表
-- 借鉴 ql361 的 bill_type 级权限控制模型
-- 每个角色可以访问哪些单据类型，以及对应的操作级别
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_role_bill_type (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    bill_type VARCHAR(10) NOT NULL,
    permission_level SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT ck_bill_type_level CHECK (permission_level IN (1, 2, 3)),
    created_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_bill_type UNIQUE (role_id, bill_type)
);

COMMENT ON TABLE sys_role_bill_type IS '角色-单据类型权限表';
COMMENT ON COLUMN sys_role_bill_type.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_bill_type.bill_type IS '单据类型代码（601=销售出库, 604=销售订单, 504=采购订单, 801=收款单）';
COMMENT ON COLUMN sys_role_bill_type.permission_level IS '权限级别: 1=查看, 2=编辑, 3=审核';
COMMENT ON COLUMN sys_role_bill_type.created_by IS '创建人';
COMMENT ON COLUMN sys_role_bill_type.create_time IS '创建时间';
COMMENT ON COLUMN sys_role_bill_type.update_time IS '更新时间';
