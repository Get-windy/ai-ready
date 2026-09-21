package cn.aiedge.erp.b2b.service;

import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMallMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 商城支付服务
 * 操作 erp_sale_order 表（order_source=2）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallPaymentServiceImpl implements MallPaymentService {

    private final ErpSaleOrderMallMapper erpSaleOrderMapper;

    /** 企业客户商城订单来源值 */
    private static final int ORDER_SOURCE_B2B_MALL = 2;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    /** erp 支付状态 → 商城支付状态 */
    private String toMallPaymentStatus(Integer erpPaymentStatus) {
        if (erpPaymentStatus == null) return "UNPAID";
        switch (erpPaymentStatus) {
            case 2:  return "PAID";
            case 4:  return "REFUNDED";
            default: return "UNPAID";
        }
    }

    /** 从 extInfo 恢复原始商城状态 */
    private String getOriginalMallStatus(String extInfo) {
        if (extInfo != null && extInfo.contains("\"originalMallStatus\"")) {
            try {
                int idx = extInfo.indexOf("\"originalMallStatus\"");
                int valStart = extInfo.indexOf(':', idx) + 2;
                int valEnd = extInfo.indexOf('"', valStart);
                if (valStart > 1 && valEnd > valStart) {
                    return extInfo.substring(valStart, valEnd);
                }
            } catch (Exception e) {
                log.warn("解析 extInfo.originalMallStatus 失败", e);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void createPayment(Map<String, Object> paymentRequest) {
        log.info("创建支付请求: {}", paymentRequest);
        String orderId = paymentRequest.get("orderId") != null ? paymentRequest.get("orderId").toString() : null;
        if (orderId != null) {
            ErpSaleOrderMall order = erpSaleOrderMapper.selectById(orderId);
            if (order != null) {
                log.info("订单 {} 发起支付，金额: {}", order.getOrderNo(), order.getTotalAmount());
            }
        }
    }

    @Override
    public void getPaymentStatus(String id) {
        log.info("查询支付状态: {}", id);
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order != null) {
            log.info("订单 {} 支付状态: {}, 商城订单状态: {}",
                    order.getOrderNo(), toMallPaymentStatus(order.getPaymentStatus()), getOriginalMallStatus(order.getExtInfo()));
        }
    }

    @Override
    @Transactional
    public void handleVerifiedCallback(Long tenantId, PaymentCallbackResult result) {
        log.info("处理已验签支付回调: tenantId={}, orderNo={}, channelOrderNo={}, success={}, amount={}",
                tenantId, result.merchantOrderNo(), result.channelOrderNo(), result.success(), result.paidAmount());

        if (!result.success()) {
            // 渠道明确告知「非成功」（如已关闭/已退款），无需改单；返回成功应答避免网关无谓重推
            log.info("回调为非成功状态，忽略：orderNo={}", result.merchantOrderNo());
            return;
        }
        if (tenantId == null || result.merchantOrderNo() == null || result.merchantOrderNo().isBlank()) {
            throw new IllegalStateException("回调缺少租户或商户订单号");
        }

        // 必须带 tenantId：验签用该租户的凭据通过，故此值可信，用它锁死订单归属。
        // 原实现没有这个条件 —— 任何租户的回调都可能改到别人的单（多租户下等于跨租户改单）。
        ErpSaleOrderMall order = erpSaleOrderMapper.selectOne(
                new LambdaQueryWrapper<ErpSaleOrderMall>()
                        .eq(ErpSaleOrderMall::getOrderNo, result.merchantOrderNo())
                        .eq(ErpSaleOrderMall::getOrderSource, ORDER_SOURCE_B2B_MALL)
                        .eq(ErpSaleOrderMall::getTenantId, tenantId)
        );
        if (order == null) {
            // 抛出去让调用方回「失败应答」：可能是回调先于本地事务提交到达，网关重推即可
            throw new IllegalStateException("订单不存在: " + result.merchantOrderNo() + " (tenant=" + tenantId + ")");
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            log.info("订单已是已支付状态，幂等返回：orderNo={}", result.merchantOrderNo());
            return;
        }
        // 金额校验：防止「低价下单 + 高价/任意金额回调」或改价重放
        if (result.paidAmount() != null && order.getTotalAmount() != null
                && result.paidAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalStateException(String.format(
                    "回调金额与订单金额不符：orderNo=%s, 回调=%s, 订单=%s",
                    result.merchantOrderNo(), result.paidAmount(), order.getTotalAmount()));
        }

        order.setStatus(1);  // 待审批
        order.setPaymentStatus(2); // 已支付
        order.setExtInfo("{\"originalMallStatus\":\"PAID\",\"source\":\"b2b_mall\"}");
        erpSaleOrderMapper.updateById(order);
        log.info("订单 {} 支付回调处理成功", result.merchantOrderNo());
    }
}
