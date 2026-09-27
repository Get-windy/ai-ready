package cn.aiedge.payment.support;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.payment.callback.TenantChannelCredentialReader;
import cn.aiedge.payment.config.PaymentConfigCatalog;
import cn.aiedge.payment.dto.PaymentChannelParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付渠道凭据读取（**按当前会话租户**）。
 *
 * <p><b>与 {@link TenantChannelCredentialReader} 的分工</b>：那个类要求调用方**显式传 tenantId**
 * （回调场景没有会话，租户只能从路径拿）；本类用于**有会话**的场景（下单、查单、关单、退款、
 * 渠道可用性判断），租户取自 {@code SecurityContext}。</p>
 *
 * <p>键名统一走 {@link PaymentConfigCatalog#CHANNEL_KEY_PREFIX}（{@code payment.channel.<渠道码小写>}），
 * 与回调验签器读的是**同一行配置** —— 这条一致性很关键：若下单读的键与验签读的键不同，
 * 会出现「用 A 私钥签名、用 B 公钥验签」，表现成"回调永远验签失败"。</p>
 *
 * <p>解析失败返回 {@code null}（由调用方 fail-closed），**不抛异常**：渠道参数是外部输入，
 * 一个租户配错不该把整个接口打成 500。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelCredentialAccessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TenantChannelCredentialReader credentialReader;
    private final SecurityContext securityContext;

    /**
     * 当前租户的渠道参数；未配置或解析失败返回 null。
     *
     * <p>只保留**带类型**的这一个入口：曾经还有一个只传渠道码的便捷重载，
     * 结果同一个类里出现两种调用方式 —— mock 场景下两个入口各走一套，极易踩坑。</p>
     */
    public <T> T read(String channelCode, Class<T> type) {
        if (channelCode == null || channelCode.isBlank()) {
            return null;
        }
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId == null) {
            log.debug("读取渠道凭据时无当前租户（可能未登录），channel={}", channelCode);
            return null;
        }
        String key = PaymentConfigCatalog.CHANNEL_KEY_PREFIX + channelCode.toLowerCase();
        String raw = credentialReader.read(tenantId, key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(raw, type);
        } catch (Exception e) {
            log.warn("渠道配置解析失败：tenantId={}, key={}, reason={}", tenantId, key, e.getMessage());
            return null;
        }
    }
}
