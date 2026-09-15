package cn.aiedge.payment.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;

import java.math.BigDecimal;

/**
 * 退款服务接口
 */
public interface RefundService {

    /**
     * 创建退款请求
     * @param paymentId 支付请求ID
     * @param amount 退款金额
     * @param reason 退款原因
     * @return 退款请求
     */
    RefundRequest createRefund(Long paymentId, BigDecimal amount, String reason);

    /**
     * 分页查询退款请求
     *
     * @param refundNo  退款单号 / 支付请求ID（模糊，可空）
     * @param startTime 退款（申请）日期起（可空）
     * @param endTime   退款（申请）日期止（可空）
     * @param channel   渠道（按支付记录渠道过滤，可空）
     */
    PageResult<RefundRequest> pageRefundRequest(Integer pageNum, Integer pageSize, Integer status, String refundNo,
                                                String startTime, String endTime, String channel);

    /**
     * 退款请求统计（后端聚合：待审批/已批准/已拒绝 笔数 + 退款金额合计）
     *
     * @return {total, pendingCount, approvedCount, rejectedCount, totalAmount}
     */
    java.util.Map<String, Object> statRefundRequest(String channel);

    /**
     * 查询退款请求详情
     */
    RefundRequest getRefundRequest(Long id);

    /**
     * 审批退款
     * @param id 退款请求ID
     * @param approved 是否批准
     * @param remark 审批备注
     */
    void approveRefund(Long id, boolean approved, String remark);

    /**
     * 处理退款回调
     * @param channel 渠道
     * @param callbackData 回调数据
     * @return 退款记录
     */
    RefundRecord handleCallback(String channel, String callbackData);

    /**
     * 分页查询退款记录
     */
    PageResult<RefundRecord> pageRefundRecord(Integer pageNum, Integer pageSize, String channel);
}