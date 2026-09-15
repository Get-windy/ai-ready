-- 多渠道订单接入架构表
-- V8.14.0__Multi_Channel_Integration_Tables.sql

-- 外部平台渠道配置表
CREATE TABLE IF NOT EXISTS external_channel_config (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel_code VARCHAR(50) NOT NULL, -- TAOBAO, JD, PDD, DOUYIN, WECHAT_MINI, SELF_MALL, POS, ERP_API
    channel_name VARCHAR(100) NOT NULL,
    channel_type VARCHAR(20) NOT NULL, -- ECOMMERCE, SOCIAL, SELF, ERP
    api_endpoint VARCHAR(200),
    app_id VARCHAR(100),
    app_secret VARCHAR(200),
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    token_expire_time TIMESTAMP,
    sync_enabled INT DEFAULT 1,
    sync_interval INT DEFAULT 30, -- 同步间隔(分钟)
    last_sync_time TIMESTAMP,
    config_json TEXT, -- 平台特定配置JSON
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE external_channel_config IS '外部平台渠道配置表';
COMMENT ON COLUMN external_channel_config.channel_type IS '渠道类型: ECOMMERCE电商, SOCIAL社交电商, SELF自有, ERP对接';

-- 外部订单原始数据表（用于审计和重试）
CREATE TABLE IF NOT EXISTS external_order_raw (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel_code VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(100) NOT NULL, -- 外部平台订单号
    raw_data TEXT NOT NULL, -- 原始JSON数据
    receive_time TIMESTAMP NOT NULL,
    process_status INT DEFAULT 0, -- 0待处理, 1已转换, 2已入库, 3失败
    internal_order_id BIGINT, -- 内部订单ID
    error_msg VARCHAR(500),
    retry_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE external_order_raw IS '外部订单原始数据表';
COMMENT ON COLUMN external_order_raw.process_status IS '处理状态: 0待处理, 1已转换, 2已入库, 3失败';

-- 库存同步记录表
CREATE TABLE IF NOT EXISTS inventory_sync_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel_code VARCHAR(50) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(100),
    internal_qty INT, -- 内部库存数量
    external_qty INT, -- 外部平台库存数量
    sync_qty INT, -- 同步数量
    sync_type VARCHAR(20), -- PUSH推送, PULL拉取, QUERY查询
    sync_time TIMESTAMP NOT NULL,
    sync_status INT DEFAULT 0, -- 0待同步, 1成功, 2失败
    error_msg VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE inventory_sync_record IS '库存同步记录表';
COMMENT ON COLUMN inventory_sync_record.sync_type IS '同步类型: PUSH推送到平台, PULL从平台拉取, QUERY平台查询';

-- API访问日志表（用于限流监控）
CREATE TABLE IF NOT EXISTS api_access_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel_code VARCHAR(50),
    api_path VARCHAR(200) NOT NULL,
    request_method VARCHAR(10),
    request_params TEXT,
    response_code INT,
    response_time INT, -- 响应时间(ms)
    ip_address VARCHAR(50),
    user_agent VARCHAR(200),
    access_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE api_access_log IS 'API访问日志表';

-- 库存查询缓存表（高频查询优化）
CREATE TABLE IF NOT EXISTS inventory_query_cache (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(100),
    warehouse_id BIGINT,
    available_qty INT NOT NULL,
    frozen_qty INT DEFAULT 0,
    cache_time TIMESTAMP NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    version INT DEFAULT 0, -- 版本号用于乐观锁
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE inventory_query_cache IS '库存查询缓存表';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_channel_config_code ON external_channel_config(channel_code);
CREATE INDEX IF NOT EXISTS idx_external_order_raw_channel ON external_order_raw(channel_code, external_order_id);
CREATE INDEX IF NOT EXISTS idx_external_order_raw_status ON external_order_raw(process_status);
CREATE INDEX IF NOT EXISTS idx_inventory_sync_product ON inventory_sync_record(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sync_time ON inventory_sync_record(sync_time);
CREATE INDEX IF NOT EXISTS idx_api_log_path ON api_access_log(api_path);
CREATE INDEX IF NOT EXISTS idx_api_log_time ON api_access_log(access_time);
CREATE INDEX IF NOT EXISTS idx_inventory_cache_product ON inventory_query_cache(product_id, sku_code);

-- 菜单数据
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, component, icon, sort, visible)
VALUES
(604, 600, '渠道配置', 'trade-channel-config', 1, 'views/trade/channel/config', NULL, 4, 1),
(605, 600, '外部订单', 'trade-external-order', 1, 'views/trade/external-order/list', NULL, 5, 1),
(606, 600, '库存同步', 'trade-inventory-sync', 1, 'views/trade/inventory-sync/list', NULL, 6, 1),
(607, 600, 'API监控', 'trade-api-monitor', 1, 'views/trade/api-monitor/list', NULL, 7, 1)
ON CONFLICT (id) DO NOTHING;