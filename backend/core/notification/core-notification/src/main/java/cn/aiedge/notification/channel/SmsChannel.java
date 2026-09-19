package cn.aiedge.notification.channel;

import cn.aiedge.base.spi.PlatformSmsSettingsProvider;
import cn.aiedge.base.spi.PlatformSmsSettingsProvider.PlatformSmsSettings;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 短信渠道。
 *
 * <p><b>配置来源（2026-09-19 接线）</b>，按优先级：
 * <ol>
 *   <li><b>平台级数据库配置</b> {@code sys_sms_config}（「系统 → 平台设置 → 短信配置」菜单 62503）
 *       经 {@link PlatformSmsSettingsProvider} 读取；</li>
 *   <li><b>application yml 的 {@code notification.sms.*}</b> 作为兜底。</li>
 * </ol>
 *
 * <p><b>🔴 改造前有两个严重问题，本类一并修掉</b>：
 * <ol>
 *   <li><b>谎报成功</b>：原实现不走任何短信服务商，直接
 *       {@code String externalId = "SMS_" + System.currentTimeMillis();} 然后
 *       {@code return SendResult.ok(externalId)} —— 消息被标记为"已发送"，
 *       而真实世界什么都没发生。这比"发失败"更糟：失败会被重试，谎报成功会让消息永久消失。</li>
 *   <li><b>渠道恒不可用</b>：{@code enabled} 只读 {@code notification.sms.enabled}，
 *       而该键在本仓库 yml 中**从未声明** → 恒 false → 渠道永远不可用，
 *       页面上保存的短信配置毫无作用。</li>
 * </ol>
 *
 * <p><b>现在的诚实边界</b>：本系统**未集成任何短信服务商 SDK**，因此无法真正投递。
 * 本类现在会**明确失败**并给出可执行的原因，交由上游的重试/失败链路处理；
 * 配置完整性校验通过后，也只在日志中标注"配置就绪但未接入投递实现"，
 * **绝不伪造 externalId**。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsChannel implements NotificationChannel {

    @Value("${notification.sms.enabled:false}")
    private boolean ymlEnabled;

    @Value("${notification.sms.provider:}")
    private String ymlProvider;

    /** 平台级配置读取口（core-api 提供实现）；裁剪部署下可能缺失 */
    private final ObjectProvider<PlatformSmsSettingsProvider> platformSmsSettingsProvider;

    @Override
    public SendResult send(NotificationRecord record) {
        PlatformSmsSettings settings = resolve();
        if (settings == null) {
            return SendResult.fail("短信服务未配置（可在「系统 → 平台设置 → 短信配置」维护，"
                    + "或在 application yml 配置 notification.sms.*）");
        }

        String phone = record.getReceiverAddress();
        if (phone == null || phone.isEmpty()) {
            return SendResult.fail("手机号为空");
        }

        String content = record.getContent();
        log.warn("[短信] 配置已就绪（provider={}），但本系统未集成任何短信服务商 SDK，"
                        + "无法真正投递。消息 recordId={}，接收号码={}，内容长度={}。"
                        + "此消息**不会**被标记为已发送，交由上游重试/失败链路处理。",
                settings.provider(), record.getId(), maskPhone(phone),
                content == null ? 0 : content.length());

        // 不再伪造 externalId、不再谎报成功
        return SendResult.fail("短信投递未实现：本系统未集成短信服务商 SDK（provider="
                + settings.provider() + "），消息未发送");
    }

    @Override
    public String getChannelType() {
        return NotificationTemplate.TYPE_SMS;
    }

    @Override
    public boolean isAvailable() {
        return resolve() != null;
    }

    /**
     * 解析当前生效的短信配置。
     *
     * @return 配置；两个来源都不可用时返回 {@code null}
     */
    private PlatformSmsSettings resolve() {
        PlatformSmsSettingsProvider provider = platformSmsSettingsProvider.getIfAvailable();
        PlatformSmsSettings settings = provider == null ? null : provider.currentSmsSettings();
        if (settings != null && settings.usable()) {
            return settings;
        }
        // yml 兜底：只有 provider 非空且 enabled 才算"配置了就绪"
        if (ymlEnabled && ymlProvider != null && !ymlProvider.isBlank()) {
            return new PlatformSmsSettings(true, ymlProvider, null, null, null);
        }
        return null;
    }

    /**
     * 手机号脱敏。注意：本方法只用于**日志**，
     * 不得用脱敏值做任何业务判断（如查重、频控计数）。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
