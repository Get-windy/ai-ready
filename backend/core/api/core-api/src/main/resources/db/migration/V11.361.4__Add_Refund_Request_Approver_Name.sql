-- ============================================================================
-- 退款管理 · 审批人姓名落库
-- 背景：《退款管理开发文档》待完善 #6 —— 详情抽屉需展示「审批人」，原表仅有 approver_id
--       （前端 detailData.approverName || detailData.approverId，无姓名时退化为数字ID）。
-- 说明：新增列 refund_request.approver_name，审批（批准/拒绝）时由 RefundServiceImpl
--       取当前登录用户（StpUtil）并关联 sys_user.real_name / nickname / username 回写。
-- 幂等：IF NOT EXISTS，可重复执行。
-- ============================================================================

ALTER TABLE refund_request
    ADD COLUMN IF NOT EXISTS approver_name VARCHAR(100);

COMMENT ON COLUMN refund_request.approver_name IS '审批人姓名（审批时按 approver_id 关联 sys_user 回写）';
