package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 美团配送适配器（Stub 实现）
 *
 * 对接美团配送开放平台 API。当前为 Stub 实现，
 * 接入生产环境需配置商户密钥并替换为真实 HTTP 调用。
 *
 * @see <a href="https://peisong.meituan.com">美团配送开放平台</a>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "meituan.enabled", havingValue = "true", matchIfMissing = false)
public class MeituanAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "meituan";
    }

    @Override
    public CreateResult createOrder(CreateRequest request) {
        log.info("[美团Stub] 创建配送单: orderNo={}, 地址={}", request.orderNo(), request.destAddress());
        return new CreateResult(true, "MT-" + System.currentTimeMillis(), "配送单已提交至美团配送");
    }

    @Override
    public QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "DELIVERING", "美团骑手", "", null, null);
    }

    @Override
    public CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(true, "美团配送已取消");
    }

    @Override
    public EstimateResult estimateFee(EstimateRequest request) {
        // 模拟美团配送费：基础8元 + 距离*1.5
        BigDecimal baseFee = new BigDecimal("8.00");
        return new EstimateResult(true, baseFee, "美团配送费预估 " + baseFee + " 元（Stub）");
    }

    @Override
    public LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(true, new BigDecimal("39.9042"), new BigDecimal("116.4074"), "模拟位置（Stub）");
    }
}
