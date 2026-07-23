package cn.aiedge.workflow.event;

/**
 * 审批流程实例到达终态（approved / rejected）事件
 *
 * 由 WorkflowServiceImpl 在实例状态翻转成功并落库后发布；
 * 经 @TransactionalEventListener(phase = AFTER_COMMIT) 在事务提交后
 * （无事务场景经 fallbackExecution 立即）异步分发给各业务模块的
 * ApprovalCallback 实现，回写单据状态。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public record ApprovalCompletedEvent(
        /** 流程实例ID（字符串，同 workflow_instance.id） */
        String instanceId,
        /** 业务类型（如 purchase_order / sale_order），可能为 null（纯流程实例） */
        String businessType,
        /** 业务单据ID，可能为 null */
        Long businessId,
        /** 终态：approved / rejected / terminated（第三期：实例被管理员终止，视同 rejected 回调） */
        String result,
        /** 终审操作人ID */
        Long operatorId,
        /** 终审操作人姓名 */
        String operatorName,
        /** 审批意见（拒绝原因等） */
        String comment,
        /** 实例租户ID（回调线程无 Sa-Token 会话，用于设置临时租户上下文） */
        Long tenantId) {

    public boolean isApproved() {
        return "approved".equals(result);
    }
}
