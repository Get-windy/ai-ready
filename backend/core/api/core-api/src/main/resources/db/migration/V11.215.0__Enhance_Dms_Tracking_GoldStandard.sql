-- =============================================================================
-- 配送跟踪（配送 → 配送跟踪 → 配送跟踪，dms:tracking，菜单 80870）金标准增强（PostgreSQL）
--   文档：《配送跟踪开发文档》§3.2 列配置 / §3.4 后端接口 / §3.5 生产级实践
--
--   本次变更：
--     1. accuracy —— §3.2「定位精度 accuracy（若有）」；无此列则精度信息只能丢在 remark 里
--     2. address  —— §3.2「地址（逆地理编码）」；由上报端带回（服务端逆编码需外呼地图服务，
--                    见《路线规划》，故只存不查，避免给位置上报链路引入外部依赖）
--     3. 时序索引 —— §3.5.1「轨迹表按天分区/定期归档，明细查询走时间索引，避免全表扫描」
--                    （dms_tracking 是全库增长最快的表）：按 租户+配送员+时间、租户+任务、租户+时间 建索引
--
--   ⚠️ 位置上报接口 `POST /api/dms/tracking/report` 的 source 字典见 TrackingSourceEnum
--      （1-APP 2-后台 3-渠道回传），两端共用同一套取值。
--   幂等：ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS
-- =============================================================================

ALTER TABLE dms_tracking ADD COLUMN IF NOT EXISTS accuracy NUMERIC(10,2);
ALTER TABLE dms_tracking ADD COLUMN IF NOT EXISTS address  VARCHAR(255);

COMMENT ON COLUMN dms_tracking.accuracy IS '定位精度(米)：越大越不可信，用于轨迹可信度与对账';
COMMENT ON COLUMN dms_tracking.address  IS '上报位置地址（上报端带回；服务端逆地理编码待接《路线规划》）';
COMMENT ON COLUMN dms_tracking.source   IS '来源：1-APP上报 2-后台补录 3-渠道回传';

-- 明细台账默认按上报时间倒序 + 常用过滤（rider / task / 时间）
CREATE INDEX IF NOT EXISTS idx_dms_tracking_rider_time
    ON dms_tracking (tenant_id, rider_id, report_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_tracking_task_time
    ON dms_tracking (tenant_id, task_id, report_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_tracking_time
    ON dms_tracking (tenant_id, report_time DESC) WHERE deleted = 0;
