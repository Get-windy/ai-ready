-- ============================================================
-- V6.20.0: Create Report Schedule Tables
-- 替换 ReportScheduleServiceImpl 内存存储
-- ============================================================

-- 1. 报表定时调度表
CREATE TABLE IF NOT EXISTS report_schedule (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(100) NOT NULL,
    report_name VARCHAR(200),
    schedule_cron VARCHAR(100) NOT NULL,
    email_recipients TEXT,
    export_format VARCHAR(20) DEFAULT 'excel',
    parameters TEXT,
    status VARCHAR(20) DEFAULT 'STOPPED',
    enabled INT DEFAULT 1,
    last_execute_time TIMESTAMP,
    next_execute_time TIMESTAMP,
    execute_count INT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE report_schedule IS '报表定时调度表';
COMMENT ON COLUMN report_schedule.report_id IS '关联报表ID';
COMMENT ON COLUMN report_schedule.report_name IS '报表名称';
COMMENT ON COLUMN report_schedule.schedule_cron IS 'Cron表达式';
COMMENT ON COLUMN report_schedule.email_recipients IS '邮件接收人（逗号分隔）';
COMMENT ON COLUMN report_schedule.export_format IS '导出格式: excel/pdf/word/csv';
COMMENT ON COLUMN report_schedule.parameters IS '报表参数（JSON）';
COMMENT ON COLUMN report_schedule.status IS '任务状态: RUNNING/PAUSED/STOPPED';
COMMENT ON COLUMN report_schedule.enabled IS '是否启用: 0-禁用 1-启用';

CREATE INDEX IF NOT EXISTS idx_report_schedule_tenant ON report_schedule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_schedule_report ON report_schedule(report_id);

-- 2. 报表调度执行日志表
CREATE TABLE IF NOT EXISTS report_schedule_log (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    report_id VARCHAR(100),
    execute_status VARCHAR(20) DEFAULT 'RUNNING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    execute_time BIGINT,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE report_schedule_log IS '报表调度执行日志表';
COMMENT ON COLUMN report_schedule_log.schedule_id IS '调度任务ID';
COMMENT ON COLUMN report_schedule_log.execute_status IS '执行状态: RUNNING/SUCCESS/FAILURE';
COMMENT ON COLUMN report_schedule_log.execute_time IS '执行耗时（毫秒）';

CREATE INDEX IF NOT EXISTS idx_report_schedule_log_schedule ON report_schedule_log(schedule_id);
CREATE INDEX IF NOT EXISTS idx_report_schedule_log_status ON report_schedule_log(execute_status);
