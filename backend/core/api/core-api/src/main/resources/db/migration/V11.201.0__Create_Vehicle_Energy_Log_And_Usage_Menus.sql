-- =====================================================================
-- 车辆补能（加油 / 充电 / 加气 / 换电）流水 + 用车菜单拆分
--
-- 背景与研判结论（2026-09-13）：
--   1) 补能是**一车/一人多条的从属流水**，没有独立单据生命周期 → 做成
--      「用车管理」页内的一个 Tab，不占独立菜单（与《车辆维护》这类有维保号、
--      有费用/供应商/下次保养推算的**单据**区分开）。
--   2) 归属模型「车/人二选一」：四轮车挂 vehicle_id、骑手两轮换电挂 rider_id，
--      一套表覆盖两类主体，二者至少填一个。
--   3) 菜单拆分为「用车管理」（用车登记/车辆巡检/车辆补能/核验预警）+「人员核验」
--      （实名认证/证照核验）——命名与能力各自对齐；`dms:verification` 这个语义
--      应归「人员核验」，故让给新菜单，原菜单改用 `dms:vehicle-usage`。
--
-- 方言：PostgreSQL；幂等：IF NOT EXISTS / WHERE 守卫，可重复执行
-- =====================================================================

-- ─────────────────────────────────────────────
-- 1) 补能流水（25 列，贴合「单表 ≤ 25 列」规范）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_vehicle_energy_log (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    vehicle_id          BIGINT,
    rider_id            BIGINT,
    energy_type         INT,
    pay_mode            INT,
    quantity            NUMERIC(12, 2),
    unit_price          NUMERIC(12, 4),
    amount_yuan         NUMERIC(12, 2),
    odometer            INTEGER,
    mileage_since_last  INTEGER,
    unit_cost           NUMERIC(12, 4),
    station             VARCHAR(200),
    occurred_at         TIMESTAMP,
    card_no             VARCHAR(64),
    voucher_url         VARCHAR(500),
    abnormal_flag       INT DEFAULT 0,
    abnormal_reason     VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INT DEFAULT 0,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    version             INT DEFAULT 0
);

COMMENT ON TABLE dms_vehicle_energy_log IS '车辆/骑手补能流水（加油/充电/加气/换电）';
COMMENT ON COLUMN dms_vehicle_energy_log.vehicle_id IS '四轮车ID（与 rider_id 二选一）';
COMMENT ON COLUMN dms_vehicle_energy_log.rider_id IS '骑手ID（两轮车换电/充电，与 vehicle_id 二选一）';
COMMENT ON COLUMN dms_vehicle_energy_log.energy_type IS '补能类型：1-汽油 2-柴油 3-充电 4-换电 5-加气';
COMMENT ON COLUMN dms_vehicle_energy_log.pay_mode IS '支付方式：1-现金 2-油卡 3-电卡 4-月租套餐 5-平台代扣';
COMMENT ON COLUMN dms_vehicle_energy_log.quantity IS '数量：L（油/气）/ kWh（充电）/ 次（换电）';
COMMENT ON COLUMN dms_vehicle_energy_log.odometer IS '本次仪表里程(km)，四轮车必填以便算区间成本';
COMMENT ON COLUMN dms_vehicle_energy_log.mileage_since_last IS '区间里程(km)＝本次里程 − 上次同主体补能里程（自动计算）';
COMMENT ON COLUMN dms_vehicle_energy_log.unit_cost IS '每公里成本（自动＝金额 ÷ 区间里程）';
COMMENT ON COLUMN dms_vehicle_energy_log.card_no IS '卡号 / 月租套餐号（一卡一车一人的锚点，P1 卡与套餐管理用）';
COMMENT ON COLUMN dms_vehicle_energy_log.abnormal_flag IS '异常标记：0-正常 1-异常';
COMMENT ON COLUMN dms_vehicle_energy_log.abnormal_reason IS '异常原因（里程倒挂 / 百公里油耗超阈值 / 金额非法）';

CREATE INDEX IF NOT EXISTS idx_energy_log_vehicle
    ON dms_vehicle_energy_log (tenant_id, vehicle_id, occurred_at DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_energy_log_rider
    ON dms_vehicle_energy_log (tenant_id, rider_id, occurred_at DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_energy_log_type
    ON dms_vehicle_energy_log (tenant_id, energy_type, occurred_at DESC) WHERE deleted = 0;

-- 补能相关参数（异常判定阈值，租户可配）
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT t.tenant_id, v.config_key, v.config_value, v.config_desc, 'TENANT', 0, now(), now(), 0
FROM (VALUES (0::bigint), (1::bigint)) AS t(tenant_id)
CROSS JOIN (VALUES
    ('energy.consumption.max.per100km', '30', '燃油车百公里油耗上限(L/100km)，超出标记补能异常'),
    ('energy.unit.cost.max',           '5',  '每公里成本上限(元/km)，超出标记补能异常')
) AS v(config_key, config_value, config_desc)
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config d
    WHERE d.tenant_id = t.tenant_id AND d.config_key = v.config_key AND d.deleted = 0
);

-- ─────────────────────────────────────────────
-- 2) 菜单：80840 由「实名认证」改为「用车管理」（4 Tab：用车登记/车辆巡检/车辆补能/核验预警）
--    守卫 menu_code='dms:verification' 保证可重复执行且不覆盖人工改动
-- ─────────────────────────────────────────────
UPDATE sys_menu
SET menu_name   = '用车管理',
    menu_code   = 'dms:vehicle-usage',
    path        = 'dms/vehicle/usage',
    component   = 'views/dms/vehicle/usage/index.vue',
    icon        = 'CarOutlined',
    sort        = 4,
    update_time = now()
WHERE id = 80840
  AND deleted = 0
  AND menu_code = 'dms:verification';

-- ─────────────────────────────────────────────
-- 3) 菜单：新增 80847「人员核验」（2 Tab：实名认证 / 证照核验）
--    `dms:verification` 语义归人员核验，故承接原 code 与 path
-- ─────────────────────────────────────────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, route_name, redirect,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      remark, biz_flow_tag, display_group, link_icon, display_mode,
                      list_path, tag_label, menu_level)
SELECT 80847, 0, 60502, 0, now(), now(),
       '人员核验', 'dms:verification', 1, 'dms/verification', 'views/dms/verification/index.vue', NULL, NULL,
       'SafetyCertificateOutlined', 5, 0, 1, 1, 1, 'tenant-admin',
       '配送员实名认证与证照核验（人的合规；车况检查见「用车管理」）', NULL, 0, NULL, 0,
       NULL, NULL, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 80847);
