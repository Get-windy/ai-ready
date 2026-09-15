-- ============================================================================
-- 退款管理 · 处理时间落库
-- 背景：开发文档《退款管理开发文档》待完善 #5 —— 详情抽屉需展示「审批人 / 审批备注 / 处理时间」。
--       审批人（approver_id）与审批备注（approve_remark）列已存在，处理时间缺失。
-- 说明：新增列 refund_request.process_time，审批（批准/拒绝）时写入；回调成功仍写 refunded_time。
-- ============================================================================

ALTER TABLE refund_request
    ADD COLUMN IF NOT EXISTS process_time TIMESTAMP;

COMMENT ON COLUMN refund_request.process_time IS '处理时间（审批完成时间）';
