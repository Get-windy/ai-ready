-- 线路主数据（资料 → 配送管理 → 线路）
--
-- 背景：本页原为占位桩页（前端直连 /md/route/page，后端无 Controller → 404）。
--   对标 ql361「资料 → 配送管理 → 线路」= 配送线路档案（线路类型 自配/物流、线路编号、线路名称、
--   物流公司、配送区域子表、备注），是基础资料字典。
-- 红线：与《配送路线单》执行单据严格区分 —— 执行单为 erp_delivery_route + erp_route_point
--   （配送员 / 进度 / 开始·完成·取消），本表只存线路档案本身，严禁混用。
-- 引用方：客户「所属区域」匹配、销售订单/出库单表头「配送线路」、订单处理中心「按线路」维度、DMS 配送任务调度。

-- ─────────────────────────────────────────────
-- 1) 线路主数据
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_route (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    route_code      VARCHAR(64)  NOT NULL,
    route_name      VARCHAR(128) NOT NULL,
    route_self      SMALLINT     NOT NULL DEFAULT 0,
    route_logistics SMALLINT     NOT NULL DEFAULT 0,
    express_name    VARCHAR(128),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE erp_route IS '线路主数据（资料→配送管理→线路；配送线路档案，区别于配送路线单执行单据）';
COMMENT ON COLUMN erp_route.route_code IS '线路编号';
COMMENT ON COLUMN erp_route.route_name IS '线路名称';
COMMENT ON COLUMN erp_route.route_self IS '线路类型-自配 0-否 1-是（对标 usetype_zp）';
COMMENT ON COLUMN erp_route.route_logistics IS '线路类型-物流 0-否 1-是（对标 usetype_wl）';
COMMENT ON COLUMN erp_route.express_name IS '物流公司名称（线路类型含物流时维护）';
COMMENT ON COLUMN erp_route.status IS '显示状态 ENABLED-已启用 DISABLED-已停用';

CREATE INDEX IF NOT EXISTS idx_erp_route_tenant ON erp_route (tenant_id);
CREATE INDEX IF NOT EXISTS idx_erp_route_name ON erp_route (route_name);
-- 编号在同一租户内唯一；软删除后允许复用（partial index，避免「软删单号仍占唯一索引」的老坑）
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_route_code
    ON erp_route (tenant_id, route_code) WHERE deleted = 0;

-- ─────────────────────────────────────────────
-- 2) 配送区域子表（对标表单子表：配送区域类型 gptype + 配送区域编码 gpcode）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_route_area (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    route_id    BIGINT       NOT NULL,
    area_type   VARCHAR(32),
    area_code   VARCHAR(32),
    area_name   VARCHAR(128),
    sort_no     INTEGER      NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT NOW(),
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE erp_route_area IS '线路配送区域子表（线路主数据 erp_route 的明细）';
COMMENT ON COLUMN erp_route_area.area_type IS '配送区域类型 PROVINCE-省 CITY-市 DISTRICT-区县（对标 gptype）';
COMMENT ON COLUMN erp_route_area.area_code IS '配送区域编码（行政区划编码，对标 gpcode）';
COMMENT ON COLUMN erp_route_area.area_name IS '配送区域名称（行政区划名称快照）';

CREATE INDEX IF NOT EXISTS idx_erp_route_area_route ON erp_route_area (route_id);
CREATE INDEX IF NOT EXISTS idx_erp_route_area_tenant ON erp_route_area (tenant_id);
