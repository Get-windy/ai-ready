-- =============================================================================
-- 签收管理（配送 → 配送跟踪 → 签收管理，dms:sign，菜单 80900）金标准增强（PostgreSQL）
--   文档：《签收管理开发文档》§3.5 后端接口 / §4 实现差异 / §5 待完善
--
--   本次补的列（均由文档 §3.6 业务规范直接驱动，不是预留字段）：
--     1. actual_quantity  —— §3.6.3「部分签收需记录实际签收数量（当前 DmsSign 无数量字段，影响后续按实际数量计费）」
--     2. planned_quantity —— 同上的对照值：提交签收时把任务应签收数量快照下来，台账才能显示「3/5」而非孤立数字
--     3. deviation_thresh —— §3.6.2「偏差阈值应可配置（落《配送参数》）」；快照提交时生效的阈值，便于事后复核为何标记超限
--     4. audit_by_name    —— 审核人姓名快照（台账/导出直接展示，避免再联查 sys_user）
--
--   ⚠️ 偏差阈值配置键：`dms.sign.deviation.threshold`（《配送参数》可改，缺省 100 米）——
--      ConfigService.getInteger 读不到时回落常量，不强制每租户预置行。
--
--   幂等：ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS
-- =============================================================================

ALTER TABLE dms_sign ADD COLUMN IF NOT EXISTS actual_quantity  NUMERIC(18,4);
ALTER TABLE dms_sign ADD COLUMN IF NOT EXISTS planned_quantity NUMERIC(18,4);
ALTER TABLE dms_sign ADD COLUMN IF NOT EXISTS deviation_thresh NUMERIC(10,2);
ALTER TABLE dms_sign ADD COLUMN IF NOT EXISTS audit_by_name    VARCHAR(128);

COMMENT ON COLUMN dms_sign.actual_quantity IS '实际签收数量（部分签收必填，按实际数量计费依据）';
COMMENT ON COLUMN dms_sign.planned_quantity IS '应签收数量快照（提交签收时的任务总量，用于部分签收对比）';
COMMENT ON COLUMN dms_sign.deviation_thresh IS '本次签收生效的定位偏差阈值(米)，快照自配送参数 dms.sign.deviation.threshold';
COMMENT ON COLUMN dms_sign.audit_by_name IS '审核人姓名快照';

-- 台账默认按签收时间倒序 + 常用过滤
CREATE INDEX IF NOT EXISTS idx_dms_sign_task       ON dms_sign (task_id);
CREATE INDEX IF NOT EXISTS idx_dms_sign_tenant_audit ON dms_sign (tenant_id, audit_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_sign_tenant_time  ON dms_sign (tenant_id, sign_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_sign_warning      ON dms_sign (tenant_id, location_warning) WHERE deleted = 0;
