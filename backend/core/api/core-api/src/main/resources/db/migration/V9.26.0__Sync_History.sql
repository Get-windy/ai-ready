-- =====================================================
-- V9.26.0: 外链同步 — 同步历史记录表
-- =====================================================

CREATE TABLE IF NOT EXISTS sync_history (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    config_id       BIGINT NOT NULL,               -- 关联 sync_data_source.id
    config_name     VARCHAR(200),                   -- 冗余存储配置名称
    source_type     VARCHAR(50),                    -- 外部系统类型
    sync_direction  VARCHAR(20) DEFAULT 'inbound',  -- 同步方向
    sync_type       VARCHAR(20) NOT NULL,           -- full=全量, incremental=增量
    bill_type       VARCHAR(50),                    -- 单据类型
    status          VARCHAR(20) NOT NULL DEFAULT 'running', -- running/success/failed/partial
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,                         -- 耗时毫秒
    records_total   INT DEFAULT 0,                  -- 总记录数
    records_synced  INT DEFAULT 0,                  -- 成功同步数
    records_created INT DEFAULT 0,                  -- 新建数
    records_updated INT DEFAULT 0,                  -- 更新数
    records_skipped INT DEFAULT 0,                  -- 跳过数
    records_failed  INT DEFAULT 0,                  -- 失败数
    error_message   TEXT,                           -- 错误信息
    error_detail    TEXT,                           -- 详细错误日志
    trigger_type    VARCHAR(20) DEFAULT 'manual',   -- manual=手动, cron=定时
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_history_config ON sync_history(config_id);
CREATE INDEX IF NOT EXISTS idx_sync_history_tenant ON sync_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sync_history_status ON sync_history(status);
CREATE INDEX IF NOT EXISTS idx_sync_history_time ON sync_history(start_time DESC);

COMMENT ON TABLE sync_history IS '同步历史记录';
COMMENT ON COLUMN sync_history.config_id IS '关联的同步配置ID';
COMMENT ON COLUMN sync_history.sync_type IS '同步类型: full=全量, incremental=增量';
COMMENT ON COLUMN sync_history.status IS '状态: running=执行中, success=成功, failed=失败, partial=部分成功';
COMMENT ON COLUMN sync_history.trigger_type IS '触发方式: manual=手动, cron=定时任务';
