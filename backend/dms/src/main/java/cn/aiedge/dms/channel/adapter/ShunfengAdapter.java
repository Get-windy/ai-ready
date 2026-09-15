package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 顺丰同城配送适配器（**未对接占位**）
 *
 * <p>本系统尚未接入顺丰同城开放平台：不发外呼、不返回模拟数据，
 * 统一走 {@link DeliveryAdapter} 的默认「未实现」失败口径（不伪造成功）。</p>
 *
 * <p>接入步骤见 {@link DadaAdapter} 类注释（连通自检 / 运力拉取 / 真实下单三处覆写）。</p>
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
}
