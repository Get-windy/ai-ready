package cn.aiedge.dms.channel.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 达达配送适配器（**未对接占位**）
 *
 * <p>本系统尚未接入达达开放平台：本适配器不发任何外呼、不返回任何模拟数据，
 * 统一走 {@link DeliveryAdapter} 的默认「未实现」失败口径（不伪造成功）。</p>
 *
 * <p>接入步骤（页面与接口无需改动）：</p>
 * <ol>
 *   <li>实现 {@code check()}（鉴权探活）与 {@code fetchRiders()}（运力拉取）——渠道页「连通性测试」「同步运力」即生效；</li>
 *   <li>用渠道对接配置（{@code dms_channel.config_json} 的 appKey/appSecret/网关/回调地址，服务端解密后取用）发起真实 HTTP；</li>
 *   <li>覆写 {@code createOrder/queryOrder/cancelOrder/estimateFee/queryRiderLocation}——渠道派单即走真实下单。</li>
 * </ol>
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
}
