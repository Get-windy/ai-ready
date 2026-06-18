package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 达达配送适配器（Stub 实现）
 *
 * 对接达达开放平台 API。当前为 Stub 实现，
 * 接入生产环境需配置商户密钥并替换为真实 HTTP 调用。
 *
 * @see <a href="https://open.imdada.cn">达达开放平台</a>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "dada.enabled", havingValue = "true", matchIfMissing = false)
public class DadaAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "dada";
    }

    @Override
    public CreateResult createOrder(CreateRequest request) {
        log.info("[达达Stub] 创建配送单: orderNo={}, 地址={}", request.orderNo(), request.destAddress());
        return new CreateResult(true, "DD-" + System.currentTimeMillis(), "配送单已提交至达达配送");
    }

    @Override
    public QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "WAITING_PICKUP", "达达骑手", "", null, null);
    }

    @Override
    public CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(true, "达达配送已取消");
    }

    @Override
    public EstimateResult estimateFee(EstimateRequest request) {
        BigDecimal baseFee = new BigDecimal("7.00");
        return new EstimateResult(true, baseFee, "达达配送费预估 " + baseFee + " 元（Stub）");
    }

    @Override
    public LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(true, new BigDecimal("39.9142"), new BigDecimal("116.3974"), "模拟位置（Stub）");
    }
}
