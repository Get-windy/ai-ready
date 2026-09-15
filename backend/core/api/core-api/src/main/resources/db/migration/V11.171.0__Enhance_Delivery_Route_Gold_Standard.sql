-- =============================================================================
-- 配送路线单（执行单）金标准增强（PostgreSQL）
--
--   页面：配送 → 配送路线 → 配送路线单（菜单 80700 / menu_code `dms:route-list`）
--        组件 views/dms/route-list/index.vue    接口 /api/delivery/route/*
--   对标：ql361 无「配送」一级菜单 → 本页为本系统自主建模（TMS 多点配送执行单），
--        不臆造对标字段；字段口径见《配送路线单开发文档》。
--
--   现状问题（本轮修复）：
--     ① `erp_delivery_route` 缺业务列 —— 路线编号以外全是空数据（路线名称/车辆/
--        计划日期/实际时长/取消原因都无处存），列表 11 列里 5 列永远为空；
--     ② `erp_route_point` **无 tenant_id 列**且未登记进 IGNORE_TENANT_TABLES，
--        多租户插件会对该表注入 tenant_id 条件 → 任何查询/写入直接 SQL 报错；
--     ③ 点位缺签收人/签收时间/失败原因，无法支撑「多点签收」闭环。
--
--   红线（单一口径）：
--     · 本表是**执行单**（谁跑、跑到哪、开始/完成/取消）；
--       `erp_route` + `erp_route_area` 是**线路档案主数据**（资料 → 配送管理 → 线路）。
--       二者不重复建表、不互相替代；执行单只通过 route_id 引用档案快照。
--     · 配送不改库存。
--
--   幂等：ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS。
-- =============================================================================

-- ① 主表补业务列 -------------------------------------------------------------
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS route_id       BIGINT;        -- 引用 erp_route（线路档案）
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS route_name     VARCHAR(200);  -- 线路名称快照
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS route_type     VARCHAR(20);   -- SELF-自配 / LOGISTICS-物流（快照）
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS vehicle_id     BIGINT;        -- 引用 dms_vehicle
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS vehicle_no     VARCHAR(50);   -- 车牌号快照
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS plan_date      DATE;          -- 计划配送日期
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS actual_duration INTEGER;      -- 实际时长（分钟，完成时按 完成时间-开始时间 计算）
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS failed_points  INTEGER DEFAULT 0; -- 配送失败点位数（按点位状态派生）
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS cancel_time    TIMESTAMP;     -- 取消时间
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS cancel_reason  VARCHAR(500);  -- 取消原因
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS create_by_name VARCHAR(100);  -- 创建人姓名快照（审计列）

COMMENT ON COLUMN erp_delivery_route.route_id IS '线路档案ID（erp_route，主数据引用，非重复表）';
COMMENT ON COLUMN erp_delivery_route.route_type IS '线路类型快照：SELF-自配 / LOGISTICS-物流';
COMMENT ON COLUMN erp_delivery_route.actual_duration IS '实际配送时长（分钟）';
COMMENT ON COLUMN erp_delivery_route.route_code IS '路线编号，号段 PSXL-YYYYMMDD-序号（同租户唯一）';

-- ② 点位明细补列（签收闭环 + 租户隔离） --------------------------------------
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS tenant_id   BIGINT;
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS signee      VARCHAR(100);  -- 签收人
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS sign_time   TIMESTAMP;     -- 签收时间
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS fail_reason VARCHAR(500);  -- 配送失败原因
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;

COMMENT ON COLUMN erp_route_point.signee IS '点位签收人（多点签收，逐点独立）';
COMMENT ON COLUMN erp_route_point.fail_reason IS '配送失败原因（status=FAILED 时填写）';

-- ③ 索引 ---------------------------------------------------------------------
-- 路线编号在租户内唯一（软删除后可复用）
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_delivery_route_code
    ON erp_delivery_route (tenant_id, route_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_erp_delivery_route_tenant_status
    ON erp_delivery_route (tenant_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_erp_delivery_route_plan_date
    ON erp_delivery_route (tenant_id, plan_date);
CREATE INDEX IF NOT EXISTS idx_erp_delivery_route_person
    ON erp_delivery_route (tenant_id, delivery_person_id);
CREATE INDEX IF NOT EXISTS idx_erp_route_point_route
    ON erp_route_point (route_id, point_order);
