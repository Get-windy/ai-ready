-- =============================================================================
-- 车辆维保 · 维保厂商升级为「往来单位档案引用 + 名称快照」（PostgreSQL）
--
-- 背景（生产级口径，2026-09-13）：
--   原实现 `maint_vendor` 是纯文本，与成熟车队/资产管理系统（SAP PM 工单供应商、
--   用友/金蝶车辆管理的维修厂商档案）不一致，导致：
--     1) 同一厂商多种写法（"XX修理厂" / "XX汽修" / 带空格）→ 服务商成本分析失真；
--     2) 无法按厂商稳定过滤（只能名称模糊）、无法与应付/发票/质保索赔对账；
--     3) 厂商改名后历史记录无稳定锚点。
--
--   落地方式（不破坏既有数据、不建重复主数据）：
--     · 新增 `vendor_id` 引用 `biz_party.id`（往来单位**唯一口径**，不另建厂商表）；
--     · `maint_vendor` 保留为**名称快照**（导出/列表/打印零改动，历史数据仍然可读）；
--     · 可空：未建档的路边快修允许仅填名称（自由填写兜底），据实标注 vendor_id 为空。
--
--   幂等：ADD COLUMN / CREATE INDEX 均带 IF NOT EXISTS，可重复执行（版本号跳号避让并行会话）。
-- =============================================================================

ALTER TABLE dms_vehicle_maintenance ADD COLUMN IF NOT EXISTS vendor_id BIGINT;

COMMENT ON COLUMN dms_vehicle_maintenance.vendor_id IS '维保厂商往来单位ID（biz_party.id，供应商/其他往来单位）；为空表示未建档厂商，仅以 maint_vendor 名称快照记账';

-- 按厂商统计/过滤（服务商成本分析、厂商维保履历）
CREATE INDEX IF NOT EXISTS idx_dms_vehicle_maintenance_vendor
    ON dms_vehicle_maintenance (tenant_id, vendor_id) WHERE deleted = 0;
