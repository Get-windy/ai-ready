-- =============================================================================
-- 车辆管理（配送 → 人车管理 → 车辆管理，dms_vehicle）金标准增强（PostgreSQL）
--   菜单：80770 `dms:vehicle`（双入口：列表 dms/vehicle + 添加 dms/vehicle/form）
--   文档：《车辆管理开发文档》§3 金标准目标设计 / §5 待完善清单
--
--   为什么加这 3 列（不是预留字段，均由业务规则直接驱动）：
--     1. operating_permit_expire_date —— §3.1「证件到期（保险/年检/**营运证**，带剩余天数与告警色）」
--                                        与 §3.5.1「保险/年检到期前 N 天预警，扩展为统一合规到期视图」
--                                        既有表只有 insurance_expire_date / inspection_expire_date，缺营运证。
--     2. owner_name / owner_phone     —— §3.3「社会车辆/个人自带车辆的『车主』与『配送员』可能不同，需分别记录」。
--
--   不新建绑定流水表：解绑/绑定留痕复用 verification 域既有表 `dms_rider_vehicle_binding`
--   （含 bind_time / handover_time / bind_mileage / handover_mileage / status），遵循「功能/模块不重复开发」。
--
--   幂等：ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS（out-of-order 安全，版本号避让并行会话）。
-- =============================================================================

ALTER TABLE dms_vehicle ADD COLUMN IF NOT EXISTS operating_permit_expire_date DATE;
ALTER TABLE dms_vehicle ADD COLUMN IF NOT EXISTS owner_name VARCHAR(200);
ALTER TABLE dms_vehicle ADD COLUMN IF NOT EXISTS owner_phone VARCHAR(30);

COMMENT ON COLUMN dms_vehicle.operating_permit_expire_date IS '营运证到期日（证件合规提醒：保险/年检/营运证三证统一视图）';
COMMENT ON COLUMN dms_vehicle.owner_name IS '车主姓名（个人自带/租赁车辆，车主可与当班配送员不同）';
COMMENT ON COLUMN dms_vehicle.owner_phone IS '车主联系电话';

-- 车牌号租户内唯一（软删除后允许复用，故用部分唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_vehicle_plate_no
    ON dms_vehicle (tenant_id, plate_no) WHERE deleted = 0;

-- 车辆编码（VH 号段）租户内唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_vehicle_code
    ON dms_vehicle (tenant_id, vehicle_code) WHERE deleted = 0;

-- 列表默认按状态/证件到期筛选与排序
CREATE INDEX IF NOT EXISTS idx_dms_vehicle_status
    ON dms_vehicle (tenant_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_vehicle_insurance_expire
    ON dms_vehicle (tenant_id, insurance_expire_date);
CREATE INDEX IF NOT EXISTS idx_dms_vehicle_inspection_expire
    ON dms_vehicle (tenant_id, inspection_expire_date);
