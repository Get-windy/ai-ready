-- =============================================================================
-- 配送路线单能力升级：围栏归集 + 地图规划 + 催单提前 + ETA 通知（PostgreSQL）
--
--   页面：配送 → 配送路线 → 配送路线单（80700 / dms:route-list）
--   边界（研判结论）：
--     · 地理能力（规划/编码/距离/坐标转换/围栏几何/围栏档案）只在 `dms/route` 实现一次，
--       本模块**只消费不自建**（本轮起不再走本地 AmapService 主链路）；
--     · 围栏**档案**归《路线规划》维护，执行单只做**绑定引用 + 业务判定**；
--     · 执行单 = erp_delivery_route + erp_route_point；线路档案 = erp_route（红线不变）。
--
--   本轮新增能力：
--     ① 围栏绑定：路线单绑定一个电子围栏（dms_geo_fence）+ 是否参与自动归集；
--     ② 自动归集：新销售订单/销售出库单的客户配送坐标落入围栏 → 自动加入该路线单；
--        （同时提供**手动添加围栏外单据**的入口，见接口 add-points）
--     ③ 地图规划：调 /api/dms/route 能力对点位重排，回填里程/时长/分段距离；
--     ④ 催单提前：把某客户点位移到指定序号，其余点位可自动重排；
--     ⑤ ETA：按剩余点位估算各点到达时间，并落「待发送通知」台账（通道后接，先保留能力）。
--
--   幂等：ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS。
-- =============================================================================

-- ① 路线单绑定围栏 -----------------------------------------------------------
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS fence_id     BIGINT;       -- 引用 dms_geo_fence
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS fence_name   VARCHAR(200); -- 围栏名称快照
ALTER TABLE erp_delivery_route ADD COLUMN IF NOT EXISTS auto_collect INTEGER DEFAULT 1; -- 参与自动归集 1/0

COMMENT ON COLUMN erp_delivery_route.fence_id IS '绑定的电子围栏ID（dms_geo_fence，档案在《路线规划》维护）';
COMMENT ON COLUMN erp_delivery_route.auto_collect IS '是否参与「围栏自动归集」1=参与 0=仅手工维护';

-- ② 点位补 ETA / 催单标记 ----------------------------------------------------
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS eta_time   TIMESTAMP;             -- 预估到达时间
ALTER TABLE erp_route_point ADD COLUMN IF NOT EXISTS expedited  INTEGER DEFAULT 0;     -- 是否被催单提前过

COMMENT ON COLUMN erp_route_point.eta_time IS '预估到达时间（按剩余点位 + 地图能力估算，非承诺值）';
COMMENT ON COLUMN erp_route_point.expedited IS '是否被催单提前：1=是（用于运营复盘）';

-- ③ 客户配送坐标（归集判定与地图规划的数据基础）-----------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS latitude  NUMERIC(10, 7);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);

COMMENT ON COLUMN biz_party.latitude IS '配送坐标-纬度（GCJ-02，客户主数据；围栏归集与路线规划用）';
COMMENT ON COLUMN biz_party.longitude IS '配送坐标-经度（GCJ-02，客户主数据）';

-- ④ ETA 通知待发送台账（通道未接入，先落库保留能力）-------------------------
CREATE TABLE IF NOT EXISTS erp_delivery_eta_notify (
    id              BIGSERIAL,
    tenant_id       BIGINT,
    route_id        BIGINT,
    route_code      VARCHAR(50),
    point_id        BIGINT,
    point_seq       INTEGER,
    customer_name   VARCHAR(200),
    customer_phone  VARCHAR(30),
    address         VARCHAR(500),
    eta_time        TIMESTAMP,
    channel         VARCHAR(20) DEFAULT 'SMS',   -- SMS / WECHAT / APP / MANUAL
    content         VARCHAR(1000),
    status          VARCHAR(20) DEFAULT 'PENDING', -- PENDING-待发送 SENT-已发送 FAILED-失败 CANCELLED-已取消
    retry_count     INTEGER DEFAULT 0,
    error_msg       VARCHAR(500),
    sent_time       TIMESTAMP,
    create_by_name  VARCHAR(100),
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE erp_delivery_eta_notify IS '配送 ETA 通知台账：ETA 能力已具备，短信/推送通道后接（当前仅落库为待发送）';

-- ⑤ 索引 ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_erp_delivery_route_fence
    ON erp_delivery_route (tenant_id, fence_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_erp_route_point_eta
    ON erp_route_point (route_id, eta_time);
CREATE INDEX IF NOT EXISTS idx_erp_eta_notify_route
    ON erp_delivery_eta_notify (tenant_id, route_id, status);
