-- ============================================================
-- V6.18.0: Create Audit Log Table
-- 创建审计日志表，替换内存存储
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    audit_type VARCHAR(50),
    module VARCHAR(100),
    action VARCHAR(200),
    target_type VARCHAR(100),
    target_id VARCHAR(100),
    target_name VARCHAR(200),
    user_id BIGINT,
    username VARCHAR(100),
    oper_ip VARCHAR(50),
    oper_location VARCHAR(200),
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(20),
    error_msg TEXT,
    request_method VARCHAR(20),
    request_url VARCHAR(500),
    request_params TEXT,
    response_data TEXT,
    duration BIGINT,
    data_snapshot TEXT,
    before_data TEXT,
    after_data TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_audit_log IS '审计日志表';
COMMENT ON COLUMN sys_audit_log.audit_type IS '审计类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/EXPORT/IMPORT/ACCESS';
COMMENT ON COLUMN sys_audit_log.module IS '操作模块';
COMMENT ON COLUMN sys_audit_log.action IS '操作动作';
COMMENT ON COLUMN sys_audit_log.result IS '操作结果: SUCCESS/FAILURE';
COMMENT ON COLUMN sys_audit_log.duration IS '耗时（毫秒）';

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant ON sys_audit_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_type ON sys_audit_log(audit_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_module ON sys_audit_log(module);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON sys_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_oper_time ON sys_audit_log(oper_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_target ON sys_audit_log(target_type, target_id);
