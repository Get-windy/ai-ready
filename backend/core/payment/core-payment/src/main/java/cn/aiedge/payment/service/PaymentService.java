package cn.aiedge.payment.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付请求
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param bizNo 业务单号
     * @param amount 支付金额
     * @param channel 支付渠道
     * @return 支付请求
     */
    PaymentRequest createPayment(String bizType, Long bizId, String bizNo, BigDecimal amount, String channel);

    /**
     * 分页查询支付请求
     *
     * @param bizNo     业务单号（模糊，可空）
     * @param startTime 创建时间起（可空，yyyy-MM-dd / yyyy-MM-dd HH:mm:ss）
     * @param endTime   创建时间止（可空）
     */
    PageResult<PaymentRequest> pagePaymentRequest(Integer pageNum, Integer pageSize, String bizType, String bizNo,
                                                  String channel, Integer status, String startTime, String endTime,
                                                  String payerName);

    /**
     * 查询支付请求详情
     */
    PaymentRequest getPaymentRequest(Long id);

    /**
     * 取消支付请求
     */
    void cancelPayment(Long id);

    /**
     * 处理**已通过验签**的支付回调结果（F-07，2026-09-26）。
     *
     * <p>⚠️ 入参是 {@link cn.aiedge.base.payment.PaymentCallbackResult} —— 即
     * {@code PaymentCallbackVerifier#verify} 的产物，**不是**原始报文。
     * 这样"改单"这件事在类型上就只能发生在验签之后：调用方拿不到原始报文，
     * 也就无法绕过验签去改单（原先的 {@code handleCallback(channel, callbackData)}
     * 正是这个漏洞 —— 渠道实现的桩无条件 {@code setStatus(2)}，任何登录用户
     * POST 一下就能把支付单置为已支付，见审计 P0-2）。</p>
     */
    PaymentRecord handleVerifiedCallback(Long tenantId, String channel, cn.aiedge.base.payment.PaymentCallbackResult result);

    /**
     * 确认线下支付
     */
    void confirmOfflinePayment(Long id, String channelOrderNo);

    /**
     * 分页查询支付记录
     *
     * @param channelOrderNo 渠道订单号（模糊，可空）
     * @param status         状态（可空）
     * @param startTime      回调时间起（可空）
     * @param endTime        回调时间止（可空）
     */
    PageResult<PaymentRecord> pagePaymentRecord(Integer pageNum, Integer pageSize, String channel, Integer status,
                                                String channelOrderNo, String startTime, String endTime);

    /**
     * 支付记录统计（后端聚合：成功/失败笔数 + 成功金额合计）
     *
     * @return {total, successCount, failedCount, successAmount}
     */
    java.util.Map<String, Object> statPaymentRecord(String channel);

    /**
     * 支付请求统计（后端聚合：待支付/成功/失败笔数 + 累计金额）
     *
     * @return {pendingCount, successCount, failedCount, totalAmount}
     */
    java.util.Map<String, Object> statPaymentRequest(String channel);

    /**
     * 查询可用渠道
     */
    List<ChannelInfo> getAvailableChannels(BigDecimal amount);

    /**
     * 渠道信息
     */
    record ChannelInfo(String code, String name, BigDecimal minAmount, BigDecimal maxAmount, boolean available) {}
}