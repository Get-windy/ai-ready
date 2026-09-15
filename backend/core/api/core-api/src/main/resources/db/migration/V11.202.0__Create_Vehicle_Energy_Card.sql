-- =====================================================================
-- 补能卡 / 套餐档案（P1）
--
-- 背景（业界 fuel card 管理的三条硬要求）：
--   1) **一卡一车一人**：卡必须绑定到唯一主体（车或人），否则无法稽核串用；
--   2) 额度与有效期可管：套餐（如骑手换电 225 元/月无限次）有额度/有效期，
--      过期卡、停用卡、额度超限都应在补能时被标记；
--   3) 非工作时段加油/换电是典型风险特征，需可配置时段窗口。
--
-- 口径：本表是**档案**（一卡一条）；补能是**流水**（一卡多条）。二者同页不同 Tab，
--   流水在落库时会按卡档案做上述稽核并写 abnormal_flag/reason。
--
-- 方言：PostgreSQL；幂等：IF NOT EXISTS / WHERE 守卫
-- =====================================================================

-- ─────────────────────────────────────────────
-- 1) 补能卡 / 套餐档案（22 列，贴合「单表 ≤ 25 列」规范）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_vehicle_energy_card (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    card_no       VARCHAR(64)  NOT NULL,
    card_name     VARCHAR(128),
    card_type     INT,
    vehicle_id    BIGINT,
    rider_id      BIGINT,
    issuer        VARCHAR(128),
    monthly_fee   NUMERIC(12, 2),
    balance       NUMERIC(12, 2),
    quota         NUMERIC(12, 2),
    used_quota    NUMERIC(12, 2),
    start_date    DATE,
    expire_date   DATE,
    status        INT DEFAULT 1,
    remark        VARCHAR(500),
    deleted       INT DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    version       INT DEFAULT 0
);

COMMENT ON TABLE dms_vehicle_energy_card IS '补能卡/套餐档案（油卡/电卡/换电套餐/充电套餐/加气卡）';
COMMENT ON COLUMN dms_vehicle_energy_card.card_no IS '卡号/套餐号（租户内唯一，一卡一车一人）';
COMMENT ON COLUMN dms_vehicle_energy_card.card_type IS '卡类型：1-油卡 2-电卡 3-换电套餐 4-充电套餐 5-加气卡';
COMMENT ON COLUMN dms_vehicle_energy_card.vehicle_id IS '绑定四轮车ID（与 rider_id 二选一）';
COMMENT ON COLUMN dms_vehicle_energy_card.rider_id IS '绑定骑手ID（与 vehicle_id 二选一）';
COMMENT ON COLUMN dms_vehicle_energy_card.monthly_fee IS '月费（月租套餐；按里程分摊到每公里成本）';
COMMENT ON COLUMN dms_vehicle_energy_card.balance IS '余额（储值卡）';
COMMENT ON COLUMN dms_vehicle_energy_card.quota IS '额度（套餐内可补能数量：L/kWh/次）；为空表示不限量';
COMMENT ON COLUMN dms_vehicle_energy_card.used_quota IS '已用额度（服务端按有效期内该卡补能流水汇总，可重算）';
COMMENT ON COLUMN dms_vehicle_energy_card.status IS '状态：0-停用 1-启用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_energy_card_no
    ON dms_vehicle_energy_card (tenant_id, card_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_energy_card_subject
    ON dms_vehicle_energy_card (tenant_id, vehicle_id, rider_id) WHERE deleted = 0;

-- ─────────────────────────────────────────────
-- 2) 窗口参数：非工作时段补能稽核
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT t.tenant_id, v.config_key, v.config_value, v.config_desc, 'TENANT', 0, now(), now(), 0
FROM (VALUES (0::bigint), (1::bigint)) AS t(tenant_id)
CROSS JOIN (VALUES
    ('energy.workhours', '06:00-23:00', '合规补能时段（HH:mm-HH:mm），落在此窗口外的补能记录标记异常')
) AS v(config_key, config_value, config_desc)
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config d
    WHERE d.tenant_id = t.tenant_id AND d.config_key = v.config_key AND d.deleted = 0
);
