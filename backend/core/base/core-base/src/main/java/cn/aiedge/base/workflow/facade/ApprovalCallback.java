package cn.aiedge.base.workflow.facade;

/**
 * 审批结果回调 SPI
 *
 * 业务模块按需实现并注册为 Spring Bean；引擎侧审批推进到终态时回调，
 * 由业务方回写单据状态（如采购订单 status 置为已审批/驳回）。
 *
 * 第二期起引擎侧已接线：core-api 在流程实例翻转为 approved/rejected 后，
 * 于事务提交后（无事务时立即）异步遍历所有 ApprovalCallback Bean 广播。
 * 第三期起：实例被管理员终止（terminated）时视同 rejected 回调（comment 带"流程终止"前缀）；
 * 分发前落 workflow_callback_log 补偿日志，回调失败按指数退避自动重试。
 *
 * 实现方约定：
 * 1. 覆盖 {@link #supports(String)} 声明目标 bizType，分发器据此过滤目标回调；
 * 2. 幂等——单据状态不满足流转条件（如已非待审批状态）时应直接忽略返回，
 *    重复回调/并发回调不得产生副作用；
 * 3. 暂时性失败（库存不足、DB 异常等）允许向上抛出——分发器会落 failed 日志
 *    并按 1m/5m/15m/30m/1h 退避自动重试，单个回调失败不影响其他业务回调。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ApprovalCallback {

    /**
     * 是否关注指定业务类型的回调（第三期：分发器按 bizType 过滤目标回调）
     *
     * @param bizType 业务类型（如 purchase_order / sale_order）
     * @return 默认 true（保持第二期的广播语义，由实现内部自行过滤）
     */
    default boolean supports(String bizType) {
        return true;
    }

    /**
     * 审批通过（流程实例到达 approved 终态）
     *
     * @param bizType      业务类型（如 purchase_order / sale_order）
     * @param bizId        业务单据ID
     * @param operatorId   终审操作人ID（引擎实例的最后审批人）
     * @param operatorName 终审操作人姓名
     */
    void onApproved(String bizType, Long bizId, Long operatorId, String operatorName);

    /**
     * 审批拒绝（流程实例到达 rejected 终态；terminated 视同拒绝，comment 带"流程终止"前缀）
     *
     * @param bizType      业务类型
     * @param bizId        业务单据ID
     * @param comment      拒绝意见
     * @param operatorId   终审操作人ID
     * @param operatorName 终审操作人姓名
     */
    void onRejected(String bizType, Long bizId, String comment, Long operatorId, String operatorName);
}
