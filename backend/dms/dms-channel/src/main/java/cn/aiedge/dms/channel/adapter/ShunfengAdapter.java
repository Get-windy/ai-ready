package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 顺丰同城配送适配器（Stub 实现）
 *
 * 对接顺丰同城开放平台 API。当前为 Stub 实现，
 * 接入生产环境需配置商户密钥并替换为真实 HTTP 调用。
 *
 * @see <a href="https://city.sf-express.com">顺丰同城开放平台</a>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "shunfeng.enabled", havingValue = "true", matchIfMissing = false)
public class ShunfengAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "shunfeng";
    }

    @Override
    public CreateResult createOrder(CreateRequest request) {
        log.info("[顺丰同城Stub] 创建配送单: orderNo={}, 地址={}", request.orderNo(), request.destAddress());
        return new CreateResult(true, "SF-" + System.currentTimeMillis(), "配送单已提交至顺丰同城");
    }

    @Override
    public QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "ACCEPTED", "顺丰骑手", "", null, null);
    }

    @Override
    public CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(true, "顺丰同城配送已取消");
    }

    @Override
    public EstimateResult estimateFee(EstimateRequest request) {
        BigDecimal baseFee = new BigDecimal("10.00");
        return new EstimateResult(true, baseFee, "顺丰同城配送费预估 " + baseFee + " 元（Stub）");
    }

    @Override
    public LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(true, new BigDecimal("39.9242"), new BigDecimal("116.4174"), "模拟位置（Stub）");
    }
}
