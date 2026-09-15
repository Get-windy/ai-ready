-- =====================================================================
-- 实名认证（KYC / 资质）台账
--
-- 背景：菜单「配送 → 人车管理 → 实名认证」(80840) 当前只有「人车核验」，
--       缺骑手身份认证 / 证照管理 / 背景审查 / 外部平台背书能力。
-- 口径：dms_rider_verification  主台账（一人一条，骑手维度）
--       dms_rider_certificate   证照明细（驾驶证 / 行驶证 / 健康证 / 从业资格证…）
-- 与 dms_rider.verify_status 的关系：本表为审核留痕的唯一事实来源，
--       审核通过/驳回后回写 dms_rider.verify_status（不新建重复状态列）。
-- 方言：PostgreSQL
-- =====================================================================

-- 1. 实名认证主台账（25 列，严格贴合「单表 ≤ 25 列」规范）
CREATE TABLE IF NOT EXISTS dms_rider_verification (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT,
    rider_id             BIGINT       NOT NULL,
    rider_name           VARCHAR(64),
    rider_type           INT,
    channel_id           BIGINT,
    channel_name         VARCHAR(128),
    real_name            VARCHAR(64),
    id_card_no           VARCHAR(32),
    id_card_urls         TEXT,
    endorse_org          VARCHAR(128),
    endorse_result       INT,
    endorse_expire_date  DATE,
    verify_status        INT          DEFAULT 0,
    audit_by             BIGINT,
    audit_time           TIMESTAMP,
    audit_remark         VARCHAR(500),
    effective_time       TIMESTAMP,
    remark               VARCHAR(500),
    deleted              INT          DEFAULT 0,
    create_time          TIMESTAMP,
    update_time          TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    version              INT          DEFAULT 0
);

COMMENT ON TABLE dms_rider_verification IS '骑手实名认证（KYC）台账';
COMMENT ON COLUMN dms_rider_verification.rider_type IS '配送员类型：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机';
COMMENT ON COLUMN dms_rider_verification.id_card_no IS '身份证号（脱敏存储：前 3 后 4，不落原文）';
COMMENT ON COLUMN dms_rider_verification.id_card_urls IS '身份证件照 URL（JSON：{"front":"...","back":"..."}）';
COMMENT ON COLUMN dms_rider_verification.endorse_org IS '背书渠道（外部平台）/ 背景审查机构';
COMMENT ON COLUMN dms_rider_verification.endorse_result IS '背书/背景审查结论：1-通过 0-未通过';
COMMENT ON COLUMN dms_rider_verification.endorse_expire_date IS '背书/资质有效期（外部平台由渠道方提供）';
COMMENT ON COLUMN dms_rider_verification.verify_status IS '认证状态：0-待提交 1-待审核 2-已通过 3-已驳回 4-已过期';
COMMENT ON COLUMN dms_rider_verification.effective_time IS '生效时间（审核通过时写入，审计留痕）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_rider_verification_rider
    ON dms_rider_verification (tenant_id, rider_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_rider_verification_status
    ON dms_rider_verification (tenant_id, verify_status);

-- 2. 证照明细（17 列）
CREATE TABLE IF NOT EXISTS dms_rider_certificate (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT,
    verification_id  BIGINT       NOT NULL,
    rider_id         BIGINT,
    cert_type        INT,
    cert_no          VARCHAR(64),
    issue_date       DATE,
    expire_date      DATE,
    cert_url         TEXT,
    verify_status    INT          DEFAULT 0,
    remark           VARCHAR(500),
    deleted          INT          DEFAULT 0,
    create_time      TIMESTAMP,
    update_time      TIMESTAMP,
    create_by        BIGINT,
    update_by        BIGINT,
    version          INT          DEFAULT 0
);

COMMENT ON TABLE dms_rider_certificate IS '骑手证照明细（驾驶证/行驶证/健康证/从业资格证/其他）';
COMMENT ON COLUMN dms_rider_certificate.cert_type IS '证照类型：1-驾驶证 2-行驶证 3-健康证 4-从业资格证 5-其他';
COMMENT ON COLUMN dms_rider_certificate.verify_status IS '证照状态：0-待核验 1-有效 2-已过期 3-无效';

CREATE INDEX IF NOT EXISTS idx_dms_rider_certificate_verification
    ON dms_rider_certificate (verification_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_rider_certificate_expire
    ON dms_rider_certificate (tenant_id, expire_date) WHERE deleted = 0;

-- 3. 补齐「交车/巡检」凭证（定位文本）：现状仅里程+经纬度，缺可读位置与巡检定位
ALTER TABLE dms_rider_vehicle_binding ADD COLUMN IF NOT EXISTS handover_location VARCHAR(200);
COMMENT ON COLUMN dms_rider_vehicle_binding.handover_location IS '交车地点（文本，人工可读，配合 handover_lat/lng）';

ALTER TABLE dms_vehicle_inspection ADD COLUMN IF NOT EXISTS inspection_location VARCHAR(200);
COMMENT ON COLUMN dms_vehicle_inspection.inspection_location IS '巡检地点（文本，配合照片凭证）';

-- 4. 核验/证照相关的租户参数（支撑定时任务真实调度，全部可配置）
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT t.tenant_id, v.config_key, v.config_value, v.config_desc, 'TENANT', 0, now(), now(), 0
FROM (VALUES (0::bigint), (1::bigint)) AS t(tenant_id)
CROSS JOIN (VALUES
    ('verification.position.threshold.meters',  '500', '人车位置核验偏差阈值（米），超出即记为异常核验'),
    ('verification.binding.max.hours',          '12',  '人车绑定最长时长（小时），超出产生「绑定超时」预警'),
    ('verification.stay.threshold.minutes',     '60',  '异常滞留判定时长（分钟），期间移动范围 <50 米即判定滞留'),
    ('verification.separation.consecutive',     '3',   '人车分离判定：连续 N 次核验异常即产生预警'),
    ('kyc.cert.expire.warn.days',               '30',  '证照/资质到期提前提醒天数'),
    ('verification.eligibility.enforce',        'false', '是否强制「无资质不接单」：true=指派/抢单前校验实名认证与证照有效期（存量配送员补齐 KYC 前保持 false）'),
    ('verification.departure.inspection.required', 'true', '是否强制「出车前检查」：true=未做检查（或检查不通过）禁止出车绑定'),
    ('verification.return.inspection.required',    'true', '是否强制「收车后检查」：true=未做检查禁止交车；检查异常自动转《车辆维护》并置车辆维修中')
) AS v(config_key, config_value, config_desc)
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config d
    WHERE d.tenant_id = t.tenant_id AND d.config_key = v.config_key AND d.deleted = 0
);
