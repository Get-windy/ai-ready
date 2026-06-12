package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 社会车辆/出租车配送适配器（Stub 实现）
 *
 * 适用于应急场景下调用社会运力。当前为 Stub 实现，
 * 生产环境需对接第三方出行平台或自建运力调度系统。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "taxi.enabled", havingValue = "true", matchIfMissing = false)
public class SocialVehicleAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "taxi";
    }

    @Override
    public CreateResult createOrder(CreateRequest request) {
        log.info("[社会车辆Stub] 创建配送单: orderNo={}, 地址={}", request.orderNo(), request.destAddress());
        return new CreateResult(true, "TAXI-" + System.currentTimeMillis(), "配送需求已发布至社会运力池");
    }

    @Override
    public QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "DRIVER_MATCHING", "社会车辆司机", "", null, null);
    }

    @Override
    public CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(true, "社会车辆配送已取消");
    }

    @Override
    public EstimateResult estimateFee(EstimateRequest request) {
        BigDecimal baseFee = new BigDecimal("12.00");
        return new EstimateResult(true, baseFee, "社会车辆配送费预估 " + baseFee + " 元（Stub）");
    }

    @Override
    public LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(true, new BigDecimal("39.8942"), new BigDecimal("116.4374"), "模拟位置（Stub）");
    }
}
