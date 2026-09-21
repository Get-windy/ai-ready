package cn.aiedge.payment.callback;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付渠道凭据的**按租户严格读取**。
 *
 * <h2>为什么不能用 {@code SysConfigService}</h2>
 * {@code cn.aiedge.base.service.impl.SysConfigServiceImpl} 里写着：
 * <pre>
 *   // 当前租户ID（简化实现）
 *   private static final Long CURRENT_TENANT_ID = 1L;
 * </pre>
 * 它的 {@code getValue/getConfig/...} **一律读租户 1** ——
 * 既不认会话租户，也不认 {@code MyBatisPlusConfig.setTempTenantId}。
 *
 * <p>用它读支付凭据的后果：**所有租户的回调都会拿租户 1 的商户号与密钥去验签**。
 * 这既让「每个租户自选渠道、平台提供接口」的设计失效，
 * 也意味着租户 A 的凭据会被用于租户 B 的回调 —— 正是「凭据只能本租户自用」要禁止的事。</p>
 *
 * <h2>口径</h2>
 * <ul>
 *   <li>**只读本租户**：不做「本租户没有就回落平台行」的兜底 ——
 *       凭据是身份，不是可继承的默认值；回落等于把平台商户号借给所有租户用。</li>
 *   <li>**显式传租户**：不依赖任何 ThreadLocal / 会话上下文，
 *       因为回调是无会话请求，租户来自 URL 路径（签名验证后可信）。</li>
 *   <li>读不到就返回 {@code null} ⇒ 调用方的 {@code isConfigured} 为 false ⇒ 渠道不生效。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantChannelCredentialReader {

    private final SysProjectConfigMapper configMapper;

    /**
     * 读取指定租户的配置值。
     *
     * @param tenantId  租户 ID（必填；为 null 直接返回 null，**绝不猜租户**）
     * @param configKey 配置键（如 {@code payment.channel.wechat}）
     * @return 配置值；不存在返回 {@code null}
     */
    public String read(Long tenantId, String configKey) {
        if (tenantId == null || configKey == null || configKey.isBlank()) {
            return null;
        }
        try {
            LambdaQueryWrapper<SysProjectConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysProjectConfig::getTenantId, tenantId)
                   .eq(SysProjectConfig::getConfigKey, configKey)
                   .eq(SysProjectConfig::getStatus, 0)
                   .eq(SysProjectConfig::getDeleted, 0)
                   .last("LIMIT 1");
            SysProjectConfig row = configMapper.selectOne(wrapper);
            return row == null ? null : row.getConfigValue();
        } catch (Exception e) {
            log.warn("读取租户支付渠道配置失败：tenantId={}, key={}, reason={}", tenantId, configKey, e.getMessage());
            return null;
        }
    }
}
