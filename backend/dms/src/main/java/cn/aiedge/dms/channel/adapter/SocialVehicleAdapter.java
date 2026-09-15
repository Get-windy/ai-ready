package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 社会车辆/出租车配送适配器（**未对接占位**）
 *
 * <p>适用于应急场景调用社会运力；本系统尚未对接第三方出行平台或自建运力池调度，
 * 因此不发外呼、不返回模拟数据，统一走 {@link DeliveryAdapter} 的默认「未实现」失败口径。</p>
 *
 * <p>接入步骤见 {@link DadaAdapter} 类注释（连通自检 / 运力拉取 / 真实下单三处覆写）。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "dms.channel", name = "taxi.enabled", havingValue = "true", matchIfMissing = false)
public class SocialVehicleAdapter implements DeliveryAdapter {

    @Override
    public String getChannelCode() {
        return "taxi";
    }
}
