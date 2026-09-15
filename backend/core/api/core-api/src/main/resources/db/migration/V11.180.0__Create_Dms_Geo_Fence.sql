-- 电子围栏档案（配送 → 配送路线 → 路线规划 → 围栏管理）
--
-- 背景：《路线规划开发文档》P0/P1 —— 原围栏校验仅支持「圆心 + 半径」直角近似，
--   且无围栏档案，无法把区域绑定到线路/渠道；本表落地多边形围栏与围栏 CRUD。
-- 口径：几何模型
--   · CIRCLE   —— center_lat / center_lng / radius_meters
--   · POLYGON  —— polygon_points（"lng,lat;lng,lat;…"，与地图厂商 polyline 同构）
--   判定在服务端用 GeoUtils（Haversine 圆判定 + 射线法多边形判定），不依赖地图 Key。
-- 坐标体系：统一 GCJ-02（入库前转换，见 GeoUtils.toGcj02）。
-- 绑定关系：biz_type + biz_id 指向线路档案（ROUTE，erp_route）/ 渠道（CHANNEL，dms_channel）/
--   仓储区域（WAREHOUSE）等，不建外键（跨模块弱引用，允许被绑定对象先于围栏存在）。

CREATE TABLE IF NOT EXISTS dms_geo_fence (
    id             BIGSERIAL     PRIMARY KEY,
    tenant_id      BIGINT        NOT NULL DEFAULT 0,
    fence_code     VARCHAR(64),
    fence_name     VARCHAR(128)  NOT NULL,
    fence_type     VARCHAR(20)   NOT NULL,
    center_lat     NUMERIC(12,7),
    center_lng     NUMERIC(12,7),
    radius_meters  NUMERIC(12,2),
    polygon_points TEXT,
    biz_type       VARCHAR(32),
    biz_id         VARCHAR(64),
    biz_name       VARCHAR(128),
    status         VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    remark         VARCHAR(500),
    create_by      BIGINT,
    create_time    TIMESTAMP     DEFAULT NOW(),
    update_by      BIGINT,
    update_time    TIMESTAMP     DEFAULT NOW(),
    deleted        SMALLINT      NOT NULL DEFAULT 0,
    version        INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE dms_geo_fence IS '电子围栏档案（支持圆形/多边形；路线规划的围栏校验数据源）';
COMMENT ON COLUMN dms_geo_fence.fence_code IS '围栏编码（同租户内唯一，软删后可复用）';
COMMENT ON COLUMN dms_geo_fence.fence_name IS '围栏名称';
COMMENT ON COLUMN dms_geo_fence.fence_type IS '围栏类型 CIRCLE-圆形 POLYGON-多边形';
COMMENT ON COLUMN dms_geo_fence.center_lat IS '圆心纬度（CIRCLE 必填，GCJ-02）';
COMMENT ON COLUMN dms_geo_fence.center_lng IS '圆心经度（CIRCLE 必填，GCJ-02）';
COMMENT ON COLUMN dms_geo_fence.radius_meters IS '半径（米，CIRCLE 必填）';
COMMENT ON COLUMN dms_geo_fence.polygon_points IS '多边形顶点 "lng,lat;lng,lat;…"（POLYGON 必填，至少 3 个顶点）';
COMMENT ON COLUMN dms_geo_fence.biz_type IS '绑定业务类型 ROUTE-线路档案 CHANNEL-运力渠道 WAREHOUSE-仓库区域 OTHER-其它';
COMMENT ON COLUMN dms_geo_fence.biz_id IS '绑定业务对象ID（弱引用，不建外键）';
COMMENT ON COLUMN dms_geo_fence.biz_name IS '绑定业务对象名称快照';
COMMENT ON COLUMN dms_geo_fence.status IS '状态 ENABLED-启用 DISABLED-停用';

CREATE INDEX IF NOT EXISTS idx_dms_geo_fence_tenant ON dms_geo_fence (tenant_id);
CREATE INDEX IF NOT EXISTS idx_dms_geo_fence_biz ON dms_geo_fence (biz_type, biz_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_geo_fence_code
    ON dms_geo_fence (tenant_id, fence_code) WHERE deleted = 0 AND fence_code IS NOT NULL;
