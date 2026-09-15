-- ============================================================
-- V6.19.0: Create Alert Rule Table
-- 创建告警规则表，替换内存存储
-- ============================================================

-- 1. 告警规则表
CREATE TABLE IF NOT EXISTS sys_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(200) NOT NULL,
    rule_code VARCHAR(100),
    metric_name VARCHAR(100),
    operator VARCHAR(10),
    threshold DOUBLE PRECISION,
    duration INT DEFAULT 60,
    severity VARCHAR(20) DEFAULT 'warning',
    notify_type VARCHAR(50),
    notify_targets VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE sys_alert_rule IS '告警规则表';
COMMENT ON COLUMN sys_alert_rule.rule_name IS '规则名称';
COMMENT ON COLUMN sys_alert_rule.rule_code IS '规则编码';
COMMENT ON COLUMN sys_alert_rule.metric_name IS '监控指标';
COMMENT ON COLUMN sys_alert_rule.operator IS '操作符: >/</>=/<=/==/!=';
COMMENT ON COLUMN sys_alert_rule.threshold IS '阈值';
COMMENT ON COLUMN sys_alert_rule.duration IS '持续时间（秒）';
COMMENT ON COLUMN sys_alert_rule.severity IS '告警级别: info/warning/critical';
COMMENT ON COLUMN sys_alert_rule.notify_type IS '通知方式: email/sms/webhook';
COMMENT ON COLUMN sys_alert_rule.notify_targets IS '通知接收人';

-- 2. 告警历史记录表
CREATE TABLE IF NOT EXISTS sys_alert_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT,
    rule_name VARCHAR(200),
    metric_name VARCHAR(100),
    metric_value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    operator VARCHAR(10),
    severity VARCHAR(20),
    message TEXT,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    resolution TEXT,
    tenant_id BIGINT DEFAULT 1,
    alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_alert_history IS '告警历史记录表';
COMMENT ON COLUMN sys_alert_history.rule_id IS '关联规则ID';
COMMENT ON COLUMN sys_alert_history.metric_value IS '触发值';
COMMENT ON COLUMN sys_alert_history.severity IS '告警级别';
COMMENT ON COLUMN sys_alert_history.acknowledged IS '是否已确认';
COMMENT ON COLUMN sys_alert_history.resolved IS '是否已解决';

CREATE INDEX IF NOT EXISTS idx_alert_history_rule ON sys_alert_history(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_time ON sys_alert_history(alert_time);
CREATE INDEX IF NOT EXISTS idx_alert_history_severity ON sys_alert_history(severity);

-- 种子数据：示例告警规则
INSERT INTO sys_alert_rule (rule_name, rule_code, metric_name, operator, threshold, duration, severity, notify_type, notify_targets, enabled) VALUES
('CPU使用率告警', 'CPU_ALERT', 'cpu_usage', '>', 90.0, 60, 'critical', 'email', 'admin@example.com', TRUE),
('内存使用率告警', 'MEM_ALERT', 'memory_usage', '>', 85.0, 120, 'warning', 'email', 'admin@example.com', TRUE),
('磁盘使用率告警', 'DISK_ALERT', 'disk_usage', '>', 90.0, 300, 'critical', 'email', 'admin@example.com', TRUE),
('API响应时间告警', 'API_LATENCY', 'api_latency', '>', 2000.0, 60, 'warning', 'sms', '13800138000', FALSE);
