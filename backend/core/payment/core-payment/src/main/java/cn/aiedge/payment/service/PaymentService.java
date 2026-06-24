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
     */
    PageResult<PaymentRequest> pagePaymentRequest(Integer pageNum, Integer pageSize, String bizType, String channel, Integer status);

    /**
     * 查询支付请求详情
     */
    PaymentRequest getPaymentRequest(Long id);

    /**
     * 取消支付请求
     */
    void cancelPayment(Long id);

    /**
     * 处理支付回调
     * @param channel 渠道
     * @param callbackData 回调数据
     * @return 支付记录
     */
    PaymentRecord handleCallback(String channel, String callbackData);

    /**
     * 确认线下支付
     */
    void confirmOfflinePayment(Long id, String channelOrderNo);

    /**
     * 分页查询支付记录
     */
    PageResult<PaymentRecord> pagePaymentRecord(Integer pageNum, Integer pageSize, String channel);

    /**
     * 查询可用渠道
     */
    List<ChannelInfo> getAvailableChannels(BigDecimal amount);

    /**
     * 渠道信息
     */
    record ChannelInfo(String code, String name, BigDecimal minAmount, BigDecimal maxAmount, boolean available) {}
}