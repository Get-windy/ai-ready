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
     */
    PageResult<RefundRequest> pageRefundRequest(Integer pageNum, Integer pageSize, Integer status);

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