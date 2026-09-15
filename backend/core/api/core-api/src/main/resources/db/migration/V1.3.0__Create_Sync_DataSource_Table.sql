-- ============================================================
-- 同步数据源配置表
-- 租户管理员在此配置要绑定的外部系统（来肯云商等）
-- ============================================================

CREATE TABLE IF NOT EXISTS sync_data_source (
    id BIGSERIAL PRIMARY KEY,

    -- 租户隔离
    tenant_id BIGINT NOT NULL DEFAULT 1,

    -- 外部系统标识（下拉列表的值）
    -- ql361: 来肯云商
    source_type VARCHAR(50) NOT NULL,

    -- 自定义显示名称（租户可修改）
    display_name VARCHAR(200),

    -- 认证信息
    source_username VARCHAR(200) NOT NULL,
    source_password VARCHAR(500) NOT NULL,  -- 加密存储
    base_url VARCHAR(500) DEFAULT 'https://www.ql361.com',

    -- 同步配置
    sync_mode VARCHAR(20) NOT NULL DEFAULT 'incremental',  -- full | incremental
    sync_cron VARCHAR(100) DEFAULT '*/30 * * * *',         -- cron 表达式
    heartbeat_interval INT DEFAULT 300,                    -- 心跳间隔（秒）

    -- 同步范围（JSON 数组）
    -- 如 ["601","604","504","801"]
    bill_types TEXT DEFAULT '["601","604","504","801"]',

    -- 状态
    status INT NOT NULL DEFAULT 1,  -- 0=禁用, 1=启用
    last_sync_time TIMESTAMP,
    remark VARCHAR(500),

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INT DEFAULT 0,

    -- 每个租户每种外部系统只能有一个配置
    CONSTRAINT uk_tenant_source UNIQUE (tenant_id, source_type)
);

-- 索引
CREATE INDEX idx_sync_ds_tenant ON sync_data_source (tenant_id);
CREATE INDEX idx_sync_ds_status ON sync_data_source (status);

COMMENT ON TABLE sync_data_source IS '同步数据源配置 - 租户绑定外部系统账号';
COMMENT ON COLUMN sync_data_source.source_type IS '外部系统类型: ql361=来肯云商';
COMMENT ON COLUMN sync_data_source.sync_mode IS '同步方式: full=全量, incremental=增量';
COMMENT ON COLUMN sync_data_source.sync_cron IS '同步频率 cron 表达式';
COMMENT ON COLUMN sync_data_source.heartbeat_interval IS '心跳检测间隔（秒）';
COMMENT ON COLUMN sync_data_source.bill_types IS '同步单据类型列表 (JSON)';
