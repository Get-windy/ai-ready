package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 美团配送适配器（**未对接占位**）
 *
 * <p>本系统尚未接入美团配送开放平台：不发外呼、不返回模拟数据，
 * 统一走 {@link DeliveryAdapter} 的默认「未实现」失败口径（不伪造成功）。</p>
 *
 * <p>接入步骤见 {@link DadaAdapter} 类注释（连通自检 / 运力拉取 / 真实下单三处覆写）。</p>
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
}
