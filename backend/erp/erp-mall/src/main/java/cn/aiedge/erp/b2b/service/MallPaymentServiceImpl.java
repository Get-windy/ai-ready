package cn.aiedge.erp.b2b.service;

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
    public void handleCallback(Map<String, Object> callbackData) {
        log.info("处理支付回调: {}", callbackData);
        String outTradeNo = callbackData.get("out_trade_no") != null ? callbackData.get("out_trade_no").toString() : null;
        String tradeStatus = callbackData.get("trade_status") != null ? callbackData.get("trade_status").toString() : null;

        if (outTradeNo != null && "SUCCESS".equals(tradeStatus)) {
            // 查询 erp_sale_order 并更新支付状态
            ErpSaleOrderMall order = erpSaleOrderMapper.selectOne(
                    new LambdaQueryWrapper<ErpSaleOrderMall>()
                            .eq(ErpSaleOrderMall::getOrderNo, outTradeNo)
                            .eq(ErpSaleOrderMall::getOrderSource, ORDER_SOURCE_B2B_MALL)
            );
            if (order != null && order.getPaymentStatus() != null && order.getPaymentStatus() == 0) {
                order.setStatus(1);  // 待审批
                order.setPaymentStatus(2); // 已支付
                order.setExtInfo("{\"originalMallStatus\":\"PAID\",\"source\":\"b2b_mall\"}");
                erpSaleOrderMapper.updateById(order);
                log.info("订单 {} 支付回调处理成功", outTradeNo);
            }
        }
    }
}
