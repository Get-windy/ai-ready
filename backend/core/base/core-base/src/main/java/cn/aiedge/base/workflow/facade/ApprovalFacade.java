package cn.aiedge.base.workflow.facade;

import java.util.Map;

/**
 * 审批门面（业务单据接入工作流引擎的统一入口）
 *
 * 定位：core-base 定义接口，core-api 的 WorkflowApprovalFacadeImpl 提供实现。
 * 业务模块（erp-purchase / erp-sales 等）只依赖本接口，通过
 * ObjectProvider&lt;ApprovalFacade&gt; 可选注入——无引擎实现时行为与接入前完全一致。
 *
 * 第二期起：引擎实例推进到终态后经 {@link ApprovalCallback} 回写单据状态；
 * startApproval 会先取消同 businessType+businessId 的在途实例再发起新实例（防重）。
 * 第三期起（影子转正式）：单据审批端点经 {@link #findInFlightInstance} 探测在途实例，
 * 存在时经 {@link #driveApproval} 由引擎驱动审批（终态仍由回调回写单据），
 * 不存在（历史单据/引擎不可用）时回退为单据服务自管状态翻转。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ApprovalFacade {

    /**
     * 发起审批流程实例
     *
     * @param processCode   流程编码（workflow_definition.process_code，如 order_approval）
     * @param bizType       业务类型（如 purchase_order / sale_order）
     * @param bizId         业务单据ID
     * @param bizNo         业务单据编号（会并入 businessData 供表单/条件节点使用）
     * @param businessData  业务数据（单据金额、往来单位等，供条件节点 SpEL 求值）
     * @param applicantId   申请人ID
     * @param applicantName 申请人姓名（引擎内部也会按 applicantId 解析，可传 null）
     * @return 流程实例ID；无可用的引擎实现或发起失败时返回 null（调用方不得因此阻断业务）
     */
    String startApproval(String processCode, String bizType, Long bizId, String bizNo,
                         Map<String, Object> businessData, Long applicantId, String applicantName);

    /**
     * 查询流程实例状态
     *
     * @param instanceId 流程实例ID
     * @return 状态字符串（approving/approved/rejected/withdrawn/cancelled/suspended/terminated），
     *         实例不存在或查询失败时返回 null
     */
    String getInstanceStatus(String instanceId);

    /**
     * 查询业务单据的在途审批实例（状态 approving/suspended，取最新一条）
     *
     * @param bizType 业务类型（如 purchase_order / sale_order）
     * @param bizId   业务单据ID
     * @return 在途实例信息（instanceId + 当前待办任务ID）；
     *         无在途实例或查询失败（降级为无引擎）时返回 null
     */
    InFlightInstance findInFlightInstance(String bizType, Long bizId);

    /**
     * 驱动在途实例审批（任务级语义，等同引擎的 approveTask）。
     * 引擎实例到达终态后由 {@link ApprovalCallback} 回调回写单据状态——
     * 返回 true 后调用方不得再直接翻转单据状态，避免重复回写。
     *
     * @param instance   在途实例（{@link #findInFlightInstance} 的返回值）
     * @param action     审批动作：approve / reject
     * @param operatorId 操作人ID（审批人）
     * @param comment    审批意见（驳回原因等）
     * @return 引擎受理成功返回 true；受理失败（实例已终态/任务已处理/引擎异常）
     *         返回 false，调用方可回退为直接翻转逻辑
     */
    boolean driveApproval(InFlightInstance instance, String action, Long operatorId, String comment);

    /**
     * 在途审批实例信息（{@link #findInFlightInstance} 的返回载体）
     */
    class InFlightInstance {

        private final String instanceId;
        private final String currentTaskId;

        public InFlightInstance(String instanceId, String currentTaskId) {
            this.instanceId = instanceId;
            this.currentTaskId = currentTaskId;
        }

        /**
         * 流程实例ID（workflow_instance.id 的字符串形式）
         */
        public String getInstanceId() {
            return instanceId;
        }

        /**
         * 当前待办任务ID（workflow_task.id 的字符串形式）；
         * 可能为 null——无待办任务时 driveApproval 退化为实例级审批
         */
        public String getCurrentTaskId() {
            return currentTaskId;
        }
    }
}
