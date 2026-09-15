-- 配置变更审计（《配送参数》/《配送配置》配置中心）
--
-- 背景：《路线规划开发文档》§5 待完善 3「配置变更审计表 dms_config_history」——
--   配置（尤其地图 Key、派单阈值等）被谁在何时改成什么，需要留痕并支持回滚。
-- 安全口径：**敏感键（Key/密钥/令牌/密码）的历史值一律存掩码**（与接口返回同口径），
--   审计可查「什么时候被谁改过」，但不会把明文密钥二次泄露到审计表/页面。
-- 回滚：按历史记录把旧值写回（敏感键仅能回滚到「掩码对应的旧值」不可复原 → 故敏感键的回滚
--   仅支持「回滚到某一历史版本的时间点」= 记录一条新的审计（值不可逆），实际做法：
--   敏感键回滚时使用历史记录里的 **旧明文**（只在同一次请求的会话内可用）——因此本表额外保留
--   old_value_cipher 列用于回滚，该列仅服务端使用、任何接口都不返回。

CREATE TABLE IF NOT EXISTS dms_config_history (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    config_key       VARCHAR(500) NOT NULL,
    old_value        VARCHAR(1000),
    new_value        VARCHAR(1000),
    old_value_cipher VARCHAR(1000),
    new_value_cipher VARCHAR(1000),
    secret           SMALLINT     NOT NULL DEFAULT 0,
    change_type      VARCHAR(20)  NOT NULL,
    operator_id      BIGINT,
    operator_name    VARCHAR(128),
    client_ip        VARCHAR(64),
    change_time      TIMESTAMP    DEFAULT NOW(),
    remark           VARCHAR(500)
);

COMMENT ON TABLE dms_config_history IS '配置变更审计（敏感键的 old_value/new_value 为掩码；*_cipher 为服务端回滚用明文，不对接口暴露）';
COMMENT ON COLUMN dms_config_history.old_value IS '变更前值（敏感键为掩码）';
COMMENT ON COLUMN dms_config_history.new_value IS '变更后值（敏感键为掩码）';
COMMENT ON COLUMN dms_config_history.old_value_cipher IS '变更前明文（仅敏感键落库，服务端回滚用，不对外返回）';
COMMENT ON COLUMN dms_config_history.new_value_cipher IS '变更后明文（同上）';
COMMENT ON COLUMN dms_config_history.change_type IS '变更类型 CREATE-新增 UPDATE-修改 CLEAR-清除 ROLLBACK-回滚';
COMMENT ON COLUMN dms_config_history.operator_id IS '操作人用户ID';
COMMENT ON COLUMN dms_config_history.operator_name IS '操作人用户名（快照）';
COMMENT ON COLUMN dms_config_history.client_ip IS '客户端IP（审计溯源）';

CREATE INDEX IF NOT EXISTS idx_dms_config_history_key
    ON dms_config_history (tenant_id, config_key, change_time DESC);

-- ─────────────────────────────────────────────
-- 附：地图「前端 JS SDK」Key（可选，用于真实底图）
--   与 Web 服务 Key（map.amap.api-key）是**两把不同的 Key**：
--     · Web 服务 Key：服务端调高德 REST API（规划/编码），绝不外泄；
--     · JS SDK Key：给浏览器加载高德底图，设计上即公开、需在高德控制台按域名限制。
--   未配置时前端回退自研矢量画布（功能不受影响）。
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, version, create_time, update_time)
VALUES (0, 'map.amap.js-key', '', '高德地图 JS SDK Key（前端底图用，公开且需按域名限制；与 Web 服务 Key 不同）。推荐环境变量 AMAP_JS_KEY', 'TENANT', 0, 0, NOW(), NOW())
ON CONFLICT DO NOTHING;
