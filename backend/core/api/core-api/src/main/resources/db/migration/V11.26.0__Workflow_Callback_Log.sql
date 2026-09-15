-- V11.26.0 审批回调补偿日志表（工作流三期：回调失败补偿）
-- 用途: ApprovalCallbackDispatcher 分发前落 pending 日志，分发结束按结果落 success/failed；
--       failed 记录由 @Scheduled(fixedDelay=60000) 任务按指数退避(1m/5m/15m/30m/1h)自动重发，
--       达到 max_retry 记 final-failed 停止重试，可经
--       POST /api/workflow/callback-log/{id}/retry 人工重试（重置重试周期并立即分发）。
-- 说明: id 由应用侧雪花算法生成（同 workflow_instance），无 DB 序列；
--       tenant_id 允许为空（引擎内置流程/无租户上下文场景），重试线程按行内 tenant_id
--       设置临时租户上下文后再分发。
-- 幂等: CREATE TABLE/INDEX IF NOT EXISTS，可重复执行。

CREATE TABLE IF NOT EXISTS workflow_callback_log (
    id               BIGINT       NOT NULL PRIMARY KEY,
    tenant_id        BIGINT,
    instance_id      BIGINT,
    biz_type         VARCHAR(100) NOT NULL,
    biz_id           BIGINT,
    result           VARCHAR(20)  NOT NULL,
    operator_id      BIGINT,
    operator_name    VARCHAR(100),
    comment          TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'pending',
    error            TEXT,
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    max_retry        INTEGER      NOT NULL DEFAULT 5,
    next_retry_time  TIMESTAMP,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  workflow_callback_log IS '审批回调补偿日志（工作流三期）';
COMMENT ON COLUMN workflow_callback_log.result          IS '终态: approved/rejected/terminated';
COMMENT ON COLUMN workflow_callback_log.status          IS '补偿状态: pending/success/failed/final-failed';
COMMENT ON COLUMN workflow_callback_log.retry_count     IS '已重试次数（首次分发失败不计）';
COMMENT ON COLUMN workflow_callback_log.max_retry       IS '最大重试次数，达到后记 final-failed';
COMMENT ON COLUMN workflow_callback_log.next_retry_time IS '下次重试时间（指数退避 1m/5m/15m/30m/1h）';

-- 补偿任务捞取：status='failed' AND retry_count<max_retry AND next_retry_time<=now
CREATE INDEX IF NOT EXISTS idx_wcl_status_retry ON workflow_callback_log (status, next_retry_time);
CREATE INDEX IF NOT EXISTS idx_wcl_biz          ON workflow_callback_log (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_wcl_instance     ON workflow_callback_log (instance_id);
