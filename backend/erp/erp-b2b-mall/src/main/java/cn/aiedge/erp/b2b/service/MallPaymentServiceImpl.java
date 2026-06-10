package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.mapper.MallOrderMapper;
import cn.aiedge.erp.b2b.model.MallOrder;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallPaymentServiceImpl implements MallPaymentService {

    private final MallOrderMapper mallOrderMapper;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    @Transactional
    public void createPayment(Map<String, Object> paymentRequest) {
        log.info("创建支付请求: {}", paymentRequest);
        // 记录支付请求到支付记录表（简化处理）
        // 生产环境应调用微信支付/支付宝SDK创建预支付订单
        String orderId = paymentRequest.get("orderId") != null ? paymentRequest.get("orderId").toString() : null;
        if (orderId != null) {
            MallOrder order = mallOrderMapper.selectById(orderId);
            if (order != null) {
                log.info("订单 {} 发起支付，金额: {}", order.getOrderNo(), order.getPayAmount());
            }
        }
    }

    @Override
    public void getPaymentStatus(String id) {
        log.info("查询支付状态: {}", id);
        // 简化处理：直接查询订单支付状态
        MallOrder order = mallOrderMapper.selectById(id);
        if (order != null) {
            log.info("订单 {} 支付状态: {}, 订单状态: {}",
                    order.getOrderNo(), order.getPaymentStatus(), order.getOrderStatus());
        }
    }

    @Override
    @Transactional
    public void handleCallback(Map<String, Object> callbackData) {
        log.info("处理支付回调: {}", callbackData);
        // 验证回调签名
        String outTradeNo = callbackData.get("out_trade_no") != null ? callbackData.get("out_trade_no").toString() : null;
        String tradeStatus = callbackData.get("trade_status") != null ? callbackData.get("trade_status").toString() : null;

        if (outTradeNo != null && "SUCCESS".equals(tradeStatus)) {
            // 查询订单并更新支付状态
            MallOrder order = mallOrderMapper.selectOne(
                    new LambdaQueryWrapper<MallOrder>()
                            .eq(MallOrder::getOrderNo, outTradeNo)
            );
            if (order != null && "UNPAID".equals(order.getPaymentStatus())) {
                order.setOrderStatus("PAID");
                order.setPaymentStatus("PAID");
                mallOrderMapper.updateById(order);
                log.info("订单 {} 支付回调处理成功", outTradeNo);
            }
        }
    }
}
