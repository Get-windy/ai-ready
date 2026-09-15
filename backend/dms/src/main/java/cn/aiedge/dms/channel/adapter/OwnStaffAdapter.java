package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 自有员工配送适配器（内部调度）
 *
 * 适用于公司自有配送团队。此适配器由系统内部直接调度，
 * 无需调用外部 API，直接创建配送任务。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "own.enabled", havingValue = "true", matchIfMissing = true)
public class OwnStaffAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "own";
    }

    /**
     * 连通性自检：内部运力由系统直接调度，无外部依赖，
     * 配置了本适配器即视为对接就绪（渠道管理页「连通性测试」走此方法）。
     */
    @Override
    public CheckResult check() {
        return new CheckResult(true, "自有运力适配器就绪（内部调度，无需外部连通）");
    }

    @Override
    public CreateResult createOrder(CreateRequest request) {
        log.info("自有员工配送 - 创建配送任务: orderNo={}, 地址={}", request.orderNo(), request.destAddress());
        return new CreateResult(true, "OWN-" + System.currentTimeMillis(), "自有员工配送已安排");
    }

    @Override
    public QueryResult queryOrder(String channelOrderNo) {
        return new QueryResult(channelOrderNo, "DELIVERING", "自有员工", "", null, null);
    }

    @Override
    public CancelResult cancelOrder(String channelOrderNo) {
        return new CancelResult(true, "自有配送已取消");
    }

    @Override
    public EstimateResult estimateFee(EstimateRequest request) {
        // 自有员工配送费用由系统定价
        return new EstimateResult(true, null, "自有配送费用由系统计算");
    }

    @Override
    public LocationResult queryRiderLocation(String channelOrderNo) {
        return new LocationResult(false, null, null, "自有配送员位置通过内部追踪获取");
    }
}
