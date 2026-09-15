-- 调度任务（配送 → 调度管理 → 调度任务，菜单 80730 / `dms:dispatch-task`）金标准增强（PostgreSQL）
--   文档：《调度任务开发文档》§3.6 工程约束 3「调度审计」/ §3.7 数据模型「建议补充 dms_task_log」
--
--   调度工作台的每一次**人工干预**都必须可追溯（谁、何时、把任务从谁改派给谁、为什么），
--   这是即时配送 TMS 的生产级要求（达达/美团调度台均留调度日志）。本迁移只**新建审计表**，
--   不新增业务列、不改 dms_task 语义（task/sign/settlement/payment/orderpool 均依赖 dms_task）。
--
--   审计写入点：指派 / 改派 / 批量指派 / 取消 / 批量取消 / 标记异常 / 超时升级自动重派 / 自动调度。
--   幂等：IF NOT EXISTS。

CREATE TABLE IF NOT EXISTS dms_task_log (
    id              BIGSERIAL     PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL DEFAULT 0,
    task_id         BIGINT        NOT NULL,
    task_no         VARCHAR(50),
    -- ASSIGN-指派 REASSIGN-改派 UNASSIGN-取消指派 CANCEL-取消 EXCEPTION-标记异常
    -- AUTO_ASSIGN-自动调度 ESCALATE-超时升级重派 BATCH_ASSIGN-批量指派 BATCH_CANCEL-批量取消
    action          VARCHAR(32)   NOT NULL,
    action_text     VARCHAR(50),
    from_rider_id   BIGINT,
    from_rider_name VARCHAR(100),
    to_rider_id     BIGINT,
    to_rider_name   VARCHAR(100),
    reason          VARCHAR(500),
    operator_id     BIGINT,
    operator_name   VARCHAR(100),
    deleted         INTEGER       DEFAULT 0,
    create_time     TIMESTAMP     DEFAULT NOW()
);

COMMENT ON TABLE  dms_task_log                 IS '调度审计日志（指派/改派/取消/异常/超时升级，操作留痕可追溯）';
COMMENT ON COLUMN dms_task_log.action          IS 'ASSIGN/REASSIGN/UNASSIGN/CANCEL/EXCEPTION/AUTO_ASSIGN/ESCALATE/BATCH_ASSIGN/BATCH_CANCEL';
COMMENT ON COLUMN dms_task_log.from_rider_name IS '变更前配送员姓名快照（改派/取消指派时留痕）';
COMMENT ON COLUMN dms_task_log.to_rider_name   IS '变更后配送员姓名快照';
COMMENT ON COLUMN dms_task_log.reason          IS '操作原因（改派/取消/异常时可填，审计追溯用）';
COMMENT ON COLUMN dms_task_log.operator_name   IS '操作人姓名快照（系统自动调度为「系统」）';

CREATE INDEX IF NOT EXISTS idx_dms_task_log_task ON dms_task_log (tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_dms_task_log_time ON dms_task_log (tenant_id, create_time DESC);
