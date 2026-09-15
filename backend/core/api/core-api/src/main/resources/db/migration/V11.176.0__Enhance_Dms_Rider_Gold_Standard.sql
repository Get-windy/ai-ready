-- 配送员管理（配送 → 人车管理 → 配送员管理）金标准补齐
--
-- 背景（见《配送员管理开发文档》§2、§4）：
--   1) dms_rider 原为「骑手」简表，缺 配送员编号 / 归属（部门·渠道）/ 资质（驾驶证·健康证·有效期）/
--      绩效（准时率·今日单量）/ 结算方式 等金标准字段；
--   2) 前端删除被误接成「状态更新」（假删除），后端已有 DELETE /{id}；
--   3) 类型枚举前后端口径错位（前端 0全职/1兼职/2众包 vs 后端 1企业员工/2众包兼职/3外部平台配送员/4社会车辆司机）。
--      本迁移只补数据结构，枚举口径由代码统一（RiderTypeEnum）。
--
-- 业务口径（两类运力同池可比）：
--   1 企业员工（关联 sys_user.id → user_id / 部门 dept_id+dept_name / 入职 entry_date）
--   2 众包兼职 / 4 社会车辆司机（身份证 / 自带车辆 / 结算方式）
--   3 外部平台配送员（关联 dms_channel.id → channel_id / 平台骑手ID platform_rider_id / 渠道背书有效期 qualification_expire_date）
-- 资质到期 qualification_expire_date 统一承载「证照有效期 / 渠道背书有效期」，用于资质门控与到期提醒。

-- ─────────────────────────────────────────────
-- 1) 补列
-- ─────────────────────────────────────────────
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS rider_no                   VARCHAR(64);
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS dept_id                    BIGINT;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS dept_name                  VARCHAR(128);
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS entry_date                 DATE;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS platform_rider_id          VARCHAR(64);
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS qualification_expire_date  DATE;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS driver_license             VARCHAR(64);
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS health_cert_no             VARCHAR(64);
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS punctual_rate              NUMERIC(5,2) DEFAULT 100.00;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS today_orders               INTEGER      DEFAULT 0;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS settle_method              INTEGER;
ALTER TABLE dms_rider ADD COLUMN IF NOT EXISTS verify_remark              VARCHAR(500);

ALTER TABLE dms_rider ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE dms_rider ALTER COLUMN deleted   SET DEFAULT 0;

COMMENT ON COLUMN dms_rider.rider_no IS '配送员编号（同租户唯一，PSY+4位序号）';
COMMENT ON COLUMN dms_rider.dept_id IS '所属部门ID（企业员工必填）';
COMMENT ON COLUMN dms_rider.dept_name IS '所属部门名称快照';
COMMENT ON COLUMN dms_rider.entry_date IS '入职日期（企业员工）';
COMMENT ON COLUMN dms_rider.platform_rider_id IS '外部平台骑手ID（外部平台配送员）';
COMMENT ON COLUMN dms_rider.qualification_expire_date IS '资质/证照有效期（含外部平台渠道背书有效期），到期不可接单';
COMMENT ON COLUMN dms_rider.driver_license IS '驾驶证号（资质区）';
COMMENT ON COLUMN dms_rider.health_cert_no IS '健康证号（资质区）';
COMMENT ON COLUMN dms_rider.punctual_rate IS '准时率（百分比 0-100）';
COMMENT ON COLUMN dms_rider.today_orders IS '今日完成单量（绩效口径，与 total_orders 同源回写）';
COMMENT ON COLUMN dms_rider.settle_method IS '结算方式：1-按单结算 2-月结 3-时段结算';
COMMENT ON COLUMN dms_rider.verify_remark IS '审核备注（审核通过/拒绝原因）';

-- ─────────────────────────────────────────────
-- 2) 存量数据回填编号（按租户内 id 顺序生成 PSY0001 递增）
-- ─────────────────────────────────────────────
UPDATE dms_rider r
SET rider_no = t.no
FROM (
    SELECT id,
           'PSY' || LPAD(ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id)::text, 4, '0') AS no
    FROM dms_rider
    WHERE rider_no IS NULL
) t
WHERE r.id = t.id AND r.rider_no IS NULL;

-- ─────────────────────────────────────────────
-- 3) 索引（编号同租户唯一；软删除后允许复用）
-- ─────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_rider_no
    ON dms_rider (tenant_id, rider_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_rider_tenant  ON dms_rider (tenant_id);
CREATE INDEX IF NOT EXISTS idx_dms_rider_channel ON dms_rider (channel_id);
CREATE INDEX IF NOT EXISTS idx_dms_rider_phone   ON dms_rider (phone);
CREATE INDEX IF NOT EXISTS idx_dms_rider_user    ON dms_rider (user_id);
