-- ============================================================
-- V6.14.0: Complete Admin Management Tables
-- 创建所有缺失的管理后台数据库表
-- ============================================================

-- 1. 系统参数配置表 (for platform/params)
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGSERIAL PRIMARY KEY,
    param_name VARCHAR(100) NOT NULL,
    param_key VARCHAR(100) NOT NULL UNIQUE,
    param_value TEXT,
    config_type VARCHAR(50) DEFAULT 'system',
    config_group VARCHAR(50) DEFAULT 'basic',
    value_type VARCHAR(20) DEFAULT 'string',
    builtin BOOLEAN DEFAULT FALSE,
    remark VARCHAR(500),
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_config IS '系统参数配置表';
COMMENT ON COLUMN sys_config.param_name IS '参数名称';
COMMENT ON COLUMN sys_config.param_key IS '参数键（唯一）';
COMMENT ON COLUMN sys_config.param_value IS '参数值';
COMMENT ON COLUMN sys_config.config_type IS '配置类型: system/business/security/notification/integration';
COMMENT ON COLUMN sys_config.config_group IS '配置分组: basic/login/password/session/upload/email/sms';
COMMENT ON COLUMN sys_config.value_type IS '值类型: string/number/boolean/json';
COMMENT ON COLUMN sys_config.builtin IS '是否系统内置';
COMMENT ON COLUMN sys_config.tenant_id IS '租户ID(0=全局)';

-- 2. 邮件配置表 (for platform/mail)
CREATE TABLE IF NOT EXISTS sys_mail_config (
    id BIGSERIAL PRIMARY KEY,
    host VARCHAR(200) NOT NULL,
    port INT DEFAULT 465,
    encryption VARCHAR(10) DEFAULT 'ssl',
    username VARCHAR(200),
    password VARCHAR(500),
    from_address VARCHAR(200),
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_mail_config IS '邮件/SMTP配置表';
COMMENT ON COLUMN sys_mail_config.host IS 'SMTP服务器地址';
COMMENT ON COLUMN sys_mail_config.port IS '端口号';
COMMENT ON COLUMN sys_mail_config.encryption IS '加密方式: none/ssl/tls';
COMMENT ON COLUMN sys_mail_config.from_address IS '发件人地址';

-- 3. 短信配置表 (for platform/sms)
CREATE TABLE IF NOT EXISTS sys_sms_config (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    access_key VARCHAR(200),
    access_secret VARCHAR(500),
    sign_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_sms_config IS '短信服务配置表';
COMMENT ON COLUMN sys_sms_config.provider IS '服务商: aliyun/tencent/huawei/qiniu';

-- 4. 存储配置表 (for platform/storage)
CREATE TABLE IF NOT EXISTS sys_storage_config (
    id BIGSERIAL PRIMARY KEY,
    storage_type VARCHAR(20) NOT NULL DEFAULT 'local',
    local_path VARCHAR(500) DEFAULT '/data/files',
    local_url_prefix VARCHAR(200) DEFAULT '/uploads',
    endpoint VARCHAR(500),
    bucket VARCHAR(200),
    access_key VARCHAR(200),
    access_secret VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_storage_config IS '文件存储配置表';
COMMENT ON COLUMN sys_storage_config.storage_type IS '存储方式: local/aliyun/tencent/qiniu/minio';

-- 5. 安全策略配置表 (for platform/security)
CREATE TABLE IF NOT EXISTS sys_security_policy (
    id BIGSERIAL PRIMARY KEY,
    lock_threshold INT DEFAULT 5,
    lock_duration INT DEFAULT 30,
    captcha_enabled BOOLEAN DEFAULT TRUE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    password_min_length INT DEFAULT 8,
    require_upper BOOLEAN DEFAULT TRUE,
    require_lower BOOLEAN DEFAULT TRUE,
    require_digit BOOLEAN DEFAULT TRUE,
    require_special BOOLEAN DEFAULT FALSE,
    password_expire_days INT DEFAULT 90,
    session_timeout INT DEFAULT 60,
    single_device BOOLEAN DEFAULT TRUE,
    ip_whitelist TEXT,
    rate_limit INT DEFAULT 1000,
    audit_retention_days INT DEFAULT 180,
    log_sensitive_ops BOOLEAN DEFAULT TRUE,
    log_login BOOLEAN DEFAULT TRUE,
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50)
);
COMMENT ON TABLE sys_security_policy IS '安全策略配置表';

-- 6. 定时任务表 (for dev/scheduler - 兼容现有Java实体 @TableName("scheduled_task"))
CREATE TABLE IF NOT EXISTS scheduled_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_desc VARCHAR(500),
    task_type VARCHAR(20) DEFAULT 'CRON',
    cron_expression VARCHAR(100),
    execute_class VARCHAR(300),
    execute_method VARCHAR(100),
    execute_params TEXT,
    status VARCHAR(20) DEFAULT 'STOPPED',
    retry_count INT DEFAULT 0,
    retry_interval INT DEFAULT 30,
    timeout INT DEFAULT 300,
    enabled INT DEFAULT 1,
    last_execute_time TIMESTAMP,
    next_execute_time TIMESTAMP,
    execute_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE scheduled_task IS '定时任务表';

-- 7. 定时任务执行日志表 (for dev/scheduler - 兼容现有Java实体 @TableName("scheduled_task_log"))
CREATE TABLE IF NOT EXISTS scheduled_task_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    task_name VARCHAR(100),
    execute_status VARCHAR(20) DEFAULT 'RUNNING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    execute_time BIGINT,
    execute_result TEXT,
    error_message TEXT,
    exception_stack TEXT,
    execute_params TEXT,
    retry_times INT DEFAULT 0,
    execute_node VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE scheduled_task_log IS '定时任务执行日志表';
CREATE INDEX IF NOT EXISTS idx_task_log_task_id ON scheduled_task_log(task_id);
CREATE INDEX IF NOT EXISTS idx_task_log_status ON scheduled_task_log(execute_status);

-- 8. 模块管理表 (for module/list)
CREATE TABLE IF NOT EXISTS sys_module (
    id BIGSERIAL PRIMARY KEY,
    module_name VARCHAR(100) NOT NULL,
    module_code VARCHAR(50) NOT NULL UNIQUE,
    version VARCHAR(20),
    description VARCHAR(500),
    status INT DEFAULT 1,
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_module IS '模块管理表';
COMMENT ON COLUMN sys_module.status IS '状态: 1=启用 0=停用';

-- 9. 模块版本表 (for module/version + module/release)
CREATE TABLE IF NOT EXISTS sys_module_version (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL,
    module_name VARCHAR(100),
    version VARCHAR(20) NOT NULL,
    changelog TEXT,
    release_status VARCHAR(20) DEFAULT 'draft',
    publisher VARCHAR(50),
    release_time TIMESTAMP,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_module_version IS '模块版本表';
COMMENT ON COLUMN sys_module_version.release_status IS '发布状态: draft/beta/released';
CREATE INDEX IF NOT EXISTS idx_module_version_module_id ON sys_module_version(module_id);

-- 10. 数据源连接表 (for data/connection)
CREATE TABLE IF NOT EXISTS sys_data_source (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(50) NOT NULL,
    host VARCHAR(200) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(500),
    status VARCHAR(20) DEFAULT 'disconnected',
    description VARCHAR(500),
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_data_source IS '数据源连接表';
COMMENT ON COLUMN sys_data_source.db_type IS '数据库类型: MySQL/PostgreSQL/Oracle/SQLServer';
COMMENT ON COLUMN sys_data_source.status IS '连接状态: connected/disconnected';

-- 11. 备份记录表 (for data/backup)
CREATE TABLE IF NOT EXISTS sys_backup_record (
    id BIGSERIAL PRIMARY KEY,
    data_source_id BIGINT,
    backup_name VARCHAR(200),
    backup_type VARCHAR(20) DEFAULT 'full',
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message TEXT,
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50)
);
COMMENT ON TABLE sys_backup_record IS '备份记录表';
COMMENT ON COLUMN sys_backup_record.backup_type IS '备份类型: full/incremental';
COMMENT ON COLUMN sys_backup_record.status IS '状态: pending/running/success/failed';

-- 12. 同步任务表 (for data/sync)
CREATE TABLE IF NOT EXISTS sys_sync_task (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT,
    target_id BIGINT,
    task_name VARCHAR(200),
    sync_type VARCHAR(20) DEFAULT 'full',
    cron_expression VARCHAR(100),
    status VARCHAR(20) DEFAULT 'stopped',
    last_sync_time TIMESTAMP,
    next_sync_time TIMESTAMP,
    description VARCHAR(500),
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_sync_task IS '数据同步任务表';
COMMENT ON COLUMN sys_sync_task.sync_type IS '同步类型: full/incremental';

-- 13. 数据清理规则表 (for data/cleanup)
CREATE TABLE IF NOT EXISTS sys_data_cleanup_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(200) NOT NULL,
    target_table VARCHAR(100) NOT NULL,
    condition_column VARCHAR(100),
    retention_days INT DEFAULT 90,
    cron_expression VARCHAR(100),
    status INT DEFAULT 1,
    description VARCHAR(500),
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted INT DEFAULT 0
);
COMMENT ON TABLE sys_data_cleanup_rule IS '数据清理规则表';

-- 14. 慢查询记录表 (for data/slow-query)
CREATE TABLE IF NOT EXISTS sys_slow_query (
    id BIGSERIAL PRIMARY KEY,
    data_source_id BIGINT,
    query_text TEXT,
    query_time_ms BIGINT,
    lock_time_ms BIGINT DEFAULT 0,
    rows_examined INT DEFAULT 0,
    rows_sent INT DEFAULT 0,
    query_time TIMESTAMP,
    database_name VARCHAR(100),
    user_name VARCHAR(100),
    host_info VARCHAR(200),
    tenant_id BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_slow_query IS '慢查询记录表';
CREATE INDEX IF NOT EXISTS idx_slow_query_ds_id ON sys_slow_query(data_source_id);
CREATE INDEX IF NOT EXISTS idx_slow_query_time ON sys_slow_query(query_time);

-- ============================================================
-- 种子数据
-- ============================================================

-- sys_config 初始参数
INSERT INTO sys_config (param_name, param_key, param_value, config_type, config_group, builtin, remark, sort_order) VALUES
('系统名称', 'system.title', 'AI-Ready 企业管理平台', 'system', 'basic', TRUE, '系统全局显示名称', 1),
('系统Logo', 'system.logo', '/logo.png', 'system', 'basic', TRUE, '系统Logo图片路径', 2),
('默认密码', 'system.defaultPassword', '123456', 'system', 'basic', TRUE, '新用户默认密码', 3),
('密码策略', 'system.passwordPolicy', 'minLength=8,requireUpper=1,requireLower=1,requireDigit=1', 'security', 'password', TRUE, '密码复杂度要求', 4),
('会话超时', 'system.sessionTimeout', '3600', 'security', 'session', TRUE, '登录会话超时时间（秒）', 5),
('上传文件大小限制', 'system.uploadMaxSize', '104857600', 'system', 'upload', TRUE, '单文件上传大小限制（字节）', 6),
('允许上传文件类型', 'system.uploadAllowedTypes', 'jpg,png,gif,pdf,doc,docx,xls,xlsx', 'system', 'upload', FALSE, '上传文件扩展名白名单', 7),
('登录失败锁定次数', 'system.loginLockCount', '5', 'security', 'login', FALSE, '登录失败N次后锁定账户', 8)
ON CONFLICT (param_key) DO NOTHING;

-- sys_security_policy 初始策略
INSERT INTO sys_security_policy (lock_threshold, lock_duration, captcha_enabled, two_factor_enabled, password_min_length, require_upper, require_lower, require_digit, require_special, password_expire_days, session_timeout, single_device, rate_limit, audit_retention_days, log_sensitive_ops, log_login)
VALUES (5, 30, TRUE, FALSE, 8, TRUE, TRUE, TRUE, FALSE, 90, 60, TRUE, 1000, 180, TRUE, TRUE);

-- sys_mail_config 初始配置
INSERT INTO sys_mail_config (host, port, encryption, username, password, from_address, enabled)
VALUES ('smtp.example.com', 465, 'ssl', 'noreply@example.com', '', 'noreply@example.com', FALSE);

-- sys_sms_config 初始配置
INSERT INTO sys_sms_config (provider, access_key, access_secret, sign_name, enabled)
VALUES ('aliyun', '', '', 'AI-Ready', FALSE);

-- sys_storage_config 初始配置
INSERT INTO sys_storage_config (storage_type, local_path, local_url_prefix, enabled)
VALUES ('local', '/data/files', '/uploads', TRUE);

-- sys_module 初始模块
INSERT INTO sys_module (module_name, module_code, version, description, status, sort_order) VALUES
('销售管理', 'sale', '2.1.0', '销售订单、出库、退货全流程管理', 1, 1),
('采购管理', 'purchase', '2.0.0', '采购订单、入库、换货全流程管理', 1, 2),
('仓储管理', 'warehouse', '1.5.0', '库存管理、盘点、调拨', 1, 3),
('财务管理', 'finance', '2.3.0', '应收应付、凭证、报表', 1, 4),
('客户关系', 'crm', '1.8.0', '客户管理、线索、商机', 1, 5),
('营销管理', 'marketing', '1.0.0', '营销活动、优惠券', 0, 6)
ON CONFLICT (module_code) DO NOTHING;

-- sys_module_version 初始模块版本
INSERT INTO sys_module_version (module_id, module_name, version, changelog, release_status, publisher, release_time) VALUES
(1, '销售管理', '2.1.0', '新增批量导入功能，修复已知Bug', 'released', '管理员', '2026-06-10 14:00:00'),
(1, '销售管理', '2.2.0-beta', '优化报表加载速度，新增图表分析', 'beta', '管理员', '2026-06-15 10:00:00'),
(2, '采购管理', '2.0.0', '重构采购流程，支持多仓库', 'released', '管理员', '2026-05-20 09:30:00'),
(3, '仓储管理', '1.5.0', '新增WMS作业模块', 'released', '管理员', '2026-06-01 11:00:00'),
(4, '财务管理', '2.3.0', '新增费用管理模块', 'released', '管理员', '2026-06-05 16:00:00'),
(5, '客户关系', '1.8.0', '优化客户导入功能', 'draft', '管理员', NULL);

-- scheduled_task 初始任务
INSERT INTO scheduled_task (task_name, task_desc, task_type, cron_expression, status, enabled, execute_count, success_count, fail_count) VALUES
('数据备份', '每日凌晨自动备份数据库', 'CRON', '0 0 3 * * ?', 'RUNNING', 1, 100, 98, 2),
('日志清理', '每日凌晨清理过期日志', 'CRON', '0 0 4 * * ?', 'RUNNING', 1, 100, 100, 0),
('数据同步', '每5分钟同步业务数据', 'CRON', '0 */5 * * * ?', 'RUNNING', 1, 5000, 4990, 10),
('缓存刷新', '每2小时刷新系统缓存', 'CRON', '0 0 */2 * * ?', 'STOPPED', 0, 200, 198, 2),
('统计报表', '每日凌晨生成统计报表', 'CRON', '0 30 1 * * ?', 'RUNNING', 1, 100, 97, 3),
('告警检查', '每分钟检查系统告警', 'CRON', '0 */1 * * * ?', 'RUNNING', 1, 10000, 9980, 20);
