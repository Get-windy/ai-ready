-- =============================================================================
-- 智能调度（配送 → 调度管理 → 智能调度，菜单 80850 / `dms:dispatch`）区域分包（AREA）落地（PostgreSQL）
--   文档：《智能调度开发文档》§3.4 派单策略「区域分包 —— 按区域/线路绑定固定配送员，工程约束=线路档案（erp_route）引用」
--         §7 遗留「区域分包（AREA）：当前仅作策略枚举与偏好权重」
--
--   口径：
--   · 线路**档案**（`erp_route`，资料 → 配送管理 → 线路）为单一主数据，DMS 侧**只读引用**，
--     不新建线路表；本表只存「线路档案 × 配送员」的绑定关系（含编号/名称快照，避免列表联查）。
--   · 一个线路可绑定多名配送员（按 priority 优先级），一名配送员可服务多条线路。
--   · 区域分包策略下：命中绑定的配送员优先（严格模式 `dms.dispatch.area.strict=true` 时未绑定直接不可派）。
--
--   幂等：CREATE TABLE/INDEX IF NOT EXISTS + ON CONFLICT DO UPDATE
-- =============================================================================

-- ─────────────────────────────────────────────
-- 1) 线路-配送员绑定（区域分包）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_route_rider (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    route_id    BIGINT       NOT NULL,
    route_code  VARCHAR(64),
    route_name  VARCHAR(128) NOT NULL,
    rider_id    BIGINT       NOT NULL,
    rider_name  VARCHAR(64)  NOT NULL,
    priority    INTEGER      NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT NOW(),
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE dms_route_rider IS '线路-配送员绑定（智能调度「区域分包」策略；线路主数据见 erp_route，DMS 只读引用）';
COMMENT ON COLUMN dms_route_rider.route_id IS '线路档案ID（erp_route.id）';
COMMENT ON COLUMN dms_route_rider.route_code IS '线路编号快照（erp_route.route_code）';
COMMENT ON COLUMN dms_route_rider.route_name IS '线路名称快照（erp_route.route_name）';
COMMENT ON COLUMN dms_route_rider.rider_id IS '配送员ID（dms_rider.id）';
COMMENT ON COLUMN dms_route_rider.rider_name IS '配送员姓名快照（dms_rider.real_name）';
COMMENT ON COLUMN dms_route_rider.priority IS '优先级：数值越小越优先（0=默认），用于一个线路绑定多人时的排序';
COMMENT ON COLUMN dms_route_rider.status IS '状态 1-启用 0-停用（停用不参与区域分包命中）';

CREATE INDEX IF NOT EXISTS idx_dms_route_rider_route ON dms_route_rider (route_id);
CREATE INDEX IF NOT EXISTS idx_dms_route_rider_rider ON dms_route_rider (rider_id);
CREATE INDEX IF NOT EXISTS idx_dms_route_rider_tenant ON dms_route_rider (tenant_id);
-- 同一租户内「线路 × 配送员」不重复绑定；软删后可重新绑定（partial index）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_route_rider
    ON dms_route_rider (tenant_id, route_id, rider_id) WHERE deleted = 0;

-- ─────────────────────────────────────────────
-- 2) 区域分包约束开关（配置中心；智能调度读取，配送参数页可改）
--    strict=true  严格分包：任务线路未绑定该配送员 → 直接不可派
--    strict=false 偏好分包（默认）：命中绑定的配送员加权重，未绑定仍可参与
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'dms.dispatch.area.strict', 'false', '区域分包严格模式：true=未绑定线路的配送员不可派；false=绑定者优先', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_desc = EXCLUDED.config_desc,
      update_time = CURRENT_TIMESTAMP;

-- 同步下发到系统租户(1)，与既有配置双行口径一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0 AND deleted = 0 AND config_key = 'dms.dispatch.area.strict'
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_desc = EXCLUDED.config_desc,
      update_time = CURRENT_TIMESTAMP;
