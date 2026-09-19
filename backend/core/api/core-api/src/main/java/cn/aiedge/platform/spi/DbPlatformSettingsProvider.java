package cn.aiedge.platform.spi;

import cn.aiedge.base.spi.PlatformMailSettingsProvider;
import cn.aiedge.base.spi.PlatformSecuritySettingsProvider;
import cn.aiedge.base.spi.PlatformSmsSettingsProvider;
import cn.aiedge.platform.mapper.MailConfigMapper;
import cn.aiedge.platform.mapper.SecurityPolicyMapper;
import cn.aiedge.platform.mapper.SmsConfigMapper;
import cn.aiedge.platform.model.MailConfig;
import cn.aiedge.platform.model.SecurityPolicy;
import cn.aiedge.platform.model.SmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 把「平台设置」页维护的邮件/短信配置，暴露给 `core-notification` 的发信链路。
 *
 * <p>这是 {@link PlatformMailSettingsProvider} / {@link PlatformSmsSettingsProvider}
 * 在 `core-api` 侧的唯一实现（SPI 接口在 `core-base`，见那里的注释说明依赖方向）。
 *
 * <p><b>本类承载的行为变更</b>：改造前 `EmailChannel` 只读 `spring.mail.*`、
 * `SmsChannel` 只读 `notification.sms.*`，而本仓库 yml 里**这两个键都不存在** →
 * 两个渠道恒不可用、页面保存的配置毫无作用。接入本类后，**页面保存的配置成为第一优先数据源**，
 * 且**保存即热生效**（每次发信实时查库，不做缓存 —— 配置是低频读、单行查询，不值得为它引入缓存失效问题）。
 *
 * <p><b>容错约定</b>：所有方法**不抛异常**，任何异常都降级为 {@code null}
 * （调用方回退到 yml 或判定不可用）。理由：把「读配置失败」放大成「发通知失败」是更糟的失败模式。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbPlatformSettingsProvider
        implements PlatformMailSettingsProvider, PlatformSmsSettingsProvider,
                   PlatformSecuritySettingsProvider {

    private final MailConfigMapper mailConfigMapper;
    private final SmsConfigMapper smsConfigMapper;
    private final SecurityPolicyMapper securityPolicyMapper;

    @Override
    public PlatformMailSettings currentMailSettings() {
        try {
            MailConfig c = mailConfigMapper.selectEffective();
            if (c == null) {
                return null;
            }
            return new PlatformMailSettings(
                    Boolean.TRUE.equals(c.isEnabled()),
                    c.getHost(),
                    c.getPort(),
                    c.getUsername(),
                    c.getPassword(),
                    c.getFromAddress(),
                    c.getEncryption());
        } catch (Exception e) {
            // 读不到 ≠ 不能发信：返回 null 让调用方回退，不把异常抛进发信链路
            log.warn("读取平台邮件配置失败，将回退到 application yml 的 spring.mail.*", e);
            return null;
        }
    }

    @Override
    public PlatformSmsSettings currentSmsSettings() {
        try {
            SmsConfig c = smsConfigMapper.selectEffective();
            if (c == null) {
                return null;
            }
            return new PlatformSmsSettings(
                    Boolean.TRUE.equals(c.isEnabled()),
                    c.getProvider(),
                    c.getAccessKey(),
                    c.getAccessSecret(),
                    c.getSignName());
        } catch (Exception e) {
            log.warn("读取平台短信配置失败，将回退到 application yml 的 notification.sms.*", e);
            return null;
        }
    }

    /**
     * 安全策略：只暴露**已接入消费方**的字段（密码长度与字符类别、密码有效期）。
     *
     * <p>其余字段（登录锁定阈值/时长、会话超时、IP 白名单、单设备登录、限流、审计保留天数）
     * **刻意不在这里返回** —— 它们目前仍无消费方，暴露出去会让调用方误以为已生效。
     * 等真正接线时再逐个加进来。
     */
    @Override
    public PlatformSecuritySettings currentSecuritySettings() {
        try {
            SecurityPolicy p = securityPolicyMapper.selectEffective();
            if (p == null) {
                return null;
            }
            return new PlatformSecuritySettings(
                    p.getPasswordMinLength(),
                    p.isRequireUpper(),
                    p.isRequireLower(),
                    p.isRequireDigit(),
                    p.isRequireSpecial(),
                    p.getPasswordExpireDays());
        } catch (Exception e) {
            log.warn("读取平台安全策略失败，密码校验将回退到内置默认规则", e);
            return null;
        }
    }
}
